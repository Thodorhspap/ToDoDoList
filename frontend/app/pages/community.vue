<template>
  <div class="page-container">

    <div class="board-container">
      <!-- ΑΡΙΣΤΕΡΗ ΣΤΗΛΗ: νέο task στον τοίχο -->
      <div class="left-sidebar">
        <div class="add-task-widget">
          <h3 class="widget-title">Post a Task</h3>
          <p class="widget-hint">Anyone can complete what you post here, except you.</p>
          <form @submit.prevent="postTask" class="add-task-form">
            <input v-model="newTaskTitle" type="text" placeholder="Task Title" maxlength="255" required />
            <textarea v-model="newDescription" placeholder="Description (optional)..." rows="3"></textarea>
            <div class="form-row">
              <select v-model="newPriority">
                <option value="">No priority</option>
                <option v-for="(p, key) in PRIORITIES" :key="key" :value="key">{{ p.icon }} {{ p.label }}</option>
              </select>
              <input v-model="newCategory" type="text" placeholder="Category" maxlength="30" />
            </div>
            <p v-if="message" class="form-msg" :class="{ error: isError }">{{ message }}</p>
            <button type="submit" class="add-btn" :disabled="posting">
              {{ posting ? 'Posting...' : 'Post to the Wall 🌍' }}
            </button>
          </form>
        </div>
      </div>

      <!-- ΚΕΝΤΡΙΚΟ BOARD: ο τοίχος -->
      <div class="board-card">
        <div class="board-header">
          <h1 class="title">Community <span class="accent-word">Tasks</span></h1>
          <p class="subtitle">Tasks anyone can pick up. First one to finish it gets the credit.</p>
        </div>

        <p v-if="wallMessage" class="form-msg error wall-msg">{{ wallMessage }}</p>

        <div v-if="wall.length > 0" class="tasks-scroll">
          <!-- Ανοιχτά tasks -->
          <h3 class="section-title">📌 Open ({{ openTasks.length }})</h3>
          <TransitionGroup name="fade" tag="div" class="tasks-list">
            <div
              v-for="task in openTasks"
              :key="task.id"
              class="task-item"
              :class="{ completed: completing.has(task.id), mine: task.postedBy === currentUser }"
            >
              <label
                v-if="task.postedBy !== currentUser"
                class="checkbox-container"
                title="I did it! Mark as completed"
              >
                <input type="checkbox" :checked="completing.has(task.id)" @change="completeTask(task, $event)" />
                <span class="checkmark"></span>
              </label>
              <span v-else class="own-mark" title="Your post: someone else has to complete it">🌍</span>

              <div class="task-content">
                <span class="task-title">{{ task.text || 'Χωρίς Τίτλο' }}</span>
                <p v-if="task.description" class="task-desc">{{ task.description }}</p>
                <div class="task-meta">
                  <span v-if="task.priority" class="priority-pill" :class="task.priority.toLowerCase()">
                    {{ PRIORITIES[task.priority]?.icon }} {{ PRIORITIES[task.priority]?.label }}
                  </span>
                  <span v-if="task.category" class="category-tag">#{{ task.category }}</span>
                </div>
              </div>

              <div class="task-side">
                <span class="task-author">
                  {{ task.postedBy === currentUser ? '📌 you' : `📌 ${task.postedBy}` }}
                </span>
                <span class="posted-date">{{ formatDate(task.createdAt) }}</span>
              </div>
            </div>
          </TransitionGroup>
          <p v-if="openTasks.length === 0" class="section-empty">Nothing open right now. Post something! 🎉</p>

          <!-- Ολοκληρωμένα (τελευταίες 7 μέρες) -->
          <template v-if="doneTasks.length > 0">
            <h3 class="section-title">✅ Completed (last 7 days)</h3>
            <TransitionGroup name="fade" tag="div" class="tasks-list">
              <div v-for="task in doneTasks" :key="task.id" class="task-item completed">
                <span class="own-mark">✅</span>
                <div class="task-content">
                  <span class="task-title">{{ task.text || 'Χωρίς Τίτλο' }}</span>
                  <span class="done-by">
                    ✅ Completed by {{ task.completedBy === currentUser ? 'you' : task.completedBy }}
                    · {{ formatDate(task.completedAt) }}
                  </span>
                </div>
                <div class="task-side">
                  <span class="task-author">
                    {{ task.postedBy === currentUser ? '📌 you' : `📌 ${task.postedBy}` }}
                  </span>
                </div>
              </div>
            </TransitionGroup>
          </template>
        </div>

        <div v-else class="empty-state">
          <p>The wall is empty. Be the first to post a task!</p>
        </div>
      </div>

      <!-- ΔΕΞΙΑ ΣΤΗΛΗ: τα tasks που ανέβασα -->
      <div class="sent-panel">
        <h3 class="widget-title">📌 My Posts <span class="count">{{ myPosts.length }}</span></h3>
        <div v-if="myPosts.length > 0" class="mini-list">
          <div
            v-for="task in myPosts"
            :key="task.id"
            class="mini-row"
            :class="{ done: task.completedBy }"
          >
            <span class="mini-status">{{ task.completedBy ? '✅' : '⏳' }}</span>
            <div class="mini-info">
              <span class="mini-title">
                {{ task.text || 'Χωρίς Τίτλο' }}
                <span v-if="task.completionSeen === false" class="new-pill">NEW</span>
              </span>
              <span class="mini-to">
                <template v-if="task.completedBy">done by {{ task.completedBy }} · {{ formatDate(task.completedAt) }}</template>
                <template v-else>waiting for someone</template>
              </span>
            </div>
            <button v-if="!task.completedBy" class="mini-cancel" title="Remove from the wall" @click="deleteTask(task.id)">✕</button>
          </div>
        </div>
        <p v-else class="section-empty">You haven't posted anything yet.</p>
      </div>
    </div>
  </div>
</template>

<script setup>
// Σελίδα με το πλαϊνό μενού (layouts/app.vue)
definePageMeta({ layout: 'app' })

import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { refreshNotifications } from '~/composables/useNotifications'
import { PRIORITIES, formatDate } from '~/composables/taskMeta'
import { celebrateTask } from '~/composables/useConfetti'
import { getCurrentUser } from '~/composables/useAuth'

const API = 'http://localhost:8080/api/community'

const router = useRouter()

const currentUser = ref('')
const wall = ref([])

const newTaskTitle = ref('')
const newDescription = ref('')
const newPriority = ref('')
const newCategory = ref('')

const message = ref('')
const isError = ref(false)
const posting = ref(false)
const wallMessage = ref('')

// Tasks που μόλις τσεκάραμε (μένουν λίγο στα ανοιχτά για το animation)
const completing = ref(new Set())

const byCompletedDesc = (a, b) => (b.completedAt || '').localeCompare(a.completedAt || '')

const openTasks = computed(() => wall.value.filter(t => !t.completedBy || completing.value.has(t.id)))
const doneTasks = computed(() =>
  wall.value.filter(t => t.completedBy && !completing.value.has(t.id)).sort(byCompletedDesc)
)

// Δικά μου: πρώτα όσα περιμένουν, μετά τα ολοκληρωμένα
const myPosts = computed(() => {
  const mine = wall.value.filter(t => t.postedBy === currentUser.value)
  return [...mine.filter(t => !t.completedBy), ...mine.filter(t => t.completedBy).sort(byCompletedDesc)]
})

let wallTimer = null
let wallMessageTimer = null

onMounted(() => {
  const savedUser = getCurrentUser()
  if (!savedUser) {
    router.push('/login')
    return
  }
  currentUser.value = savedUser

  loadWall()
  // Κάθε 5 δευτερόλεπτα: νέα posts και ολοκληρώσεις από άλλους
  wallTimer = setInterval(() => {
    if (!document.hidden) loadWall()
  }, 5000)
})

onUnmounted(() => {
  clearInterval(wallTimer)
  clearTimeout(wallMessageTimer)
})

const loadWall = async () => {
  try {
    const data = await $fetch(API)
    if (!Array.isArray(data)) return
    // Παίρνουμε την έκδοση του server, εκτός από tasks που τσεκάρουμε αυτή τη στιγμή.
    // Το "NEW" στα δικά μου μένει όσο είμαστε σε αυτή τη σελίδα.
    const existing = new Map(wall.value.map(task => [task.id, task]))
    wall.value = data.map(task => {
      const old = existing.get(task.id)
      if (!old) return task
      if (completing.value.has(task.id)) return old
      return { ...task, completionSeen: old.completionSeen === false ? false : task.completionSeen }
    })

    // Κάποιος ολοκλήρωσε δικό μου task: το είδαμε, οπότε σβήνει η ειδοποίηση
    if (data.some(task => task.postedBy === currentUser.value && task.completionSeen === false)) {
      await $fetch(`${API}/completions-seen/${encodeURIComponent(currentUser.value)}`, { method: 'POST' })
      refreshNotifications()
    }
  } catch (error) {
    console.error('❌ Error fetching community tasks:', error)
  }
}

const postTask = async () => {
  if (newTaskTitle.value.trim() === '') return

  message.value = ''
  posting.value = true
  try {
    const saved = await $fetch(API, {
      method: 'POST',
      body: {
        text: newTaskTitle.value,
        description: newDescription.value,
        priority: newPriority.value || null,
        category: newCategory.value.trim() || null,
        postedBy: currentUser.value
      }
    })

    wall.value.unshift(saved)
    newTaskTitle.value = ''
    newDescription.value = ''
    newPriority.value = ''
    newCategory.value = ''
    isError.value = false
    message.value = 'Posted to the wall!'
  } catch (error) {
    console.error('❌ Error posting task:', error)
    isError.value = true
    message.value = error?.data?.message || 'Could not post the task. Is the server running?'
  } finally {
    posting.value = false
  }
}

const showWallMessage = (text) => {
  wallMessage.value = text
  clearTimeout(wallMessageTimer)
  wallMessageTimer = setTimeout(() => { wallMessage.value = '' }, 4000)
}

// Ολοκλήρωση task κάποιου άλλου: γιορτή, γραμμή πάνω του και μετά πάει στα Completed.
// Αν κάποιος άλλος πρόλαβε πρώτος, ο server απαντά 409.
const completeTask = async (task, event) => {
  completing.value.add(task.id)
  try {
    const saved = await $fetch(`${API}/${task.id}/complete`, {
      method: 'PUT',
      body: { username: currentUser.value }
    })
    celebrateTask(event?.target)
    setTimeout(() => {
      Object.assign(task, saved)
      completing.value.delete(task.id)
    }, 400)
  } catch (error) {
    console.error('❌ Error completing community task:', error)
    completing.value.delete(task.id)
    if (event?.target) event.target.checked = false
    showWallMessage(error?.data?.message || 'Could not complete the task. Is the server running?')
    loadWall()
  }
}

const deleteTask = async (id) => {
  try {
    await $fetch(`${API}/${id}`, {
      method: 'DELETE',
      query: { username: currentUser.value }
    })
    wall.value = wall.value.filter(task => task.id !== id)
  } catch (error) {
    console.error('❌ Error deleting community task:', error)
    showWallMessage(error?.data?.message || 'Could not remove the task.')
    loadWall()
  }
}
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Poppins:wght@400;500;600;800&display=swap');

.page-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  padding: 1.5rem;
  box-sizing: border-box;
  position: relative;
  font-family: 'Poppins', sans-serif;
  overflow: hidden;
  gap: 1rem;
}

/* Container για τις στήλες από κάτω */
.board-container {
  display: flex;
  align-items: stretch;
  gap: 2rem;
  flex: 1;
  box-sizing: border-box;
  position: relative;
  overflow: hidden;
  z-index: 1;
  min-height: 0;
}

.left-sidebar {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  width: 360px;
  flex-shrink: 0;
  z-index: 2;
  min-height: 0;
}

.add-task-widget {
  flex-shrink: 0;
}

/* Μικρό panel: tasks που έστειλα */
.sent-panel {
  width: 300px;
  flex-shrink: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: rgba(28, 25, 23, 0.65);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(var(--accent-rgb), 0.2);
  padding: 1.2rem 1.5rem;
  border-radius: 16px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3);
  box-sizing: border-box;
}

.sent-panel .widget-title {
  margin-bottom: 0.8rem;
  font-size: 1.05rem;
}

.count {
  font-size: 0.75rem;
  color: var(--accent);
  background: rgba(var(--accent-rgb), 0.15);
  border-radius: 10px;
  padding: 1px 8px;
  margin-left: 4px;
  vertical-align: middle;
}

.mini-list {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding-right: 6px;
}

.mini-list::-webkit-scrollbar {
  width: 6px;
}

.mini-list::-webkit-scrollbar-thumb {
  background: rgba(var(--accent-rgb), 0.4);
  border-radius: 3px;
}

.mini-row {
  display: flex;
  align-items: center;
  gap: 10px;
  background: rgba(0, 0, 0, 0.25);
  border: 1px solid rgba(var(--accent-rgb), 0.15);
  border-radius: 10px;
  padding: 0.5rem 0.7rem;
}

.mini-row.done {
  opacity: 0.75;
}

.mini-status {
  font-size: 0.9rem;
  flex-shrink: 0;
}

.mini-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.mini-title {
  color: #ffffff;
  font-size: 0.9rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mini-to {
  color: #a8a29e;
  font-size: 0.75rem;
}

.mini-cancel {
  background: transparent;
  border: 1px solid rgba(248, 113, 113, 0.4);
  color: #f87171;
  border-radius: 6px;
  width: 24px;
  height: 24px;
  font-size: 0.7rem;
  cursor: pointer;
  flex-shrink: 0;
  transition: all 0.2s;
}

.mini-cancel:hover {
  background: #f87171;
  color: #1c1917;
}

.add-task-widget {
  position: relative;
  background: rgba(28, 25, 23, 0.65);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(var(--accent-rgb), 0.2);
  padding: 1.5rem;
  border-radius: 16px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3);
}

.widget-title {
  margin: 0 0 1rem 0;
  color: #ffffff;
  font-size: 1.2rem;
  font-weight: 600;
}

.add-task-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.add-task-form input, .add-task-form textarea {
  background: rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(var(--accent-rgb), 0.3);
  border-radius: 10px;
  padding: 0.85rem;
  color: white;
  font-size: 0.95rem;
  transition: all 0.3s;
  box-sizing: border-box;
  font-family: inherit;
}

.add-task-form textarea {
  resize: vertical;
}

.add-task-form select {
  background-color: rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(var(--accent-rgb), 0.3);
  border-radius: 10px;
  padding: 0.85rem 2.5rem 0.85rem 0.85rem;
  color: white;
  font-size: 0.95rem;
  font-family: inherit;
  cursor: pointer;
  color-scheme: dark;
}

.add-task-form select:focus {
  outline: none;
  background-color: rgba(0, 0, 0, 0.5);
  border-color: var(--accent);
  box-shadow: 0 0 15px rgba(var(--accent-rgb), 0.3);
}

.add-task-form input:focus, .add-task-form textarea:focus {
  outline: none;
  border-color: var(--accent);
  background: rgba(0, 0, 0, 0.5);
  box-shadow: 0 0 15px rgba(var(--accent-rgb), 0.3);
}

.form-msg {
  margin: 0;
  color: #4ade80;
  font-size: 0.9rem;
}

.form-msg.error {
  color: #f87171;
}

.add-btn {
  background: transparent;
  color: var(--accent);
  border: 2px solid var(--accent);
  border-radius: 10px;
  padding: 0.85rem;
  font-size: 1rem;
  cursor: pointer;
  font-weight: bold;
  transition: all 0.3s;
  margin-top: 5px;
  font-family: 'Poppins', sans-serif;
}

.add-btn:hover {
  background: var(--accent);
  color: #1c1917;
  box-shadow: 0 0 20px rgba(var(--accent-rgb), 0.7);
}

.add-btn:disabled {
  opacity: 0.6;
  cursor: wait;
}

/* Δεξί Board */
.board-card {
  position: relative;
  z-index: 1;
  background: rgba(28, 25, 23, 0.65);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(var(--accent-rgb), 0.2);
  padding: 2.5rem;
  border-radius: 20px;
  flex: 1;
  height: 100%;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.5), 0 0 20px rgba(var(--accent-rgb), 0.1);
  min-height: 0;
}

.board-header {
  margin-bottom: 1.5rem;
  border-bottom: 1px solid rgba(var(--accent-rgb), 0.2);
  padding-bottom: 1rem;
  flex-shrink: 0;
}

.title {
  margin: 0;
  font-size: 2.2rem;
  color: #ffffff;
  font-weight: 800;
  text-shadow: 0 0 10px rgba(var(--accent-rgb), 0.5);
}

.accent-word {
  color: var(--accent);
  text-shadow: 0 0 10px rgba(var(--accent-rgb), 0.6);
}

.subtitle {
  margin: 0.5rem 0 0;
  color: #a8a29e;
}

/* Αποκλειστικό Scroll Bar για τα Tasks */
.tasks-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  flex: 1;
  overflow-y: auto;
  padding-right: 15px;
  min-height: 0;
}

.tasks-list::-webkit-scrollbar {
  width: 8px;
}

.tasks-list::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.2);
  border-radius: 4px;
}

.tasks-list::-webkit-scrollbar-thumb {
  background: rgba(var(--accent-rgb), 0.4);
  border-radius: 4px;
}

.task-item {
  display: flex;
  align-items: flex-start;
  background: rgba(0, 0, 0, 0.25);
  padding: 1.2rem;
  border-radius: 12px;
  border: 1px solid rgba(var(--accent-rgb), 0.15);
  transition: all 0.3s;
}

.task-item:hover {
  background: rgba(0, 0, 0, 0.4);
  border-color: rgba(var(--accent-rgb), 0.4);
}

.task-content {
  flex: 1;
  margin-left: 15px;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.task-title {
  color: white;
  font-size: 1.1rem;
  font-weight: 500;
}

.task-desc {
  margin: 0;
  color: #a8a29e;
  font-size: 0.9rem;
  line-height: 1.4;
}

.task-side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
  margin-left: 10px;
}

.task-author {
  font-size: 0.8rem;
  color: var(--accent);
  background: rgba(var(--accent-rgb), 0.15);
  border: 1px solid rgba(var(--accent-rgb), 0.3);
  padding: 4px 10px;
  border-radius: 12px;
}


.task-item.completed .task-title, .task-item.completed .task-desc {
  text-decoration: line-through;
  color: #57534e;
}

/* Checkbox ολοκλήρωσης (ίδιο με το board) */
.checkbox-container {
  display: block;
  position: relative;
  cursor: pointer;
  width: 22px;
  height: 22px;
  margin-top: 2px;
  flex-shrink: 0;
}

.checkbox-container input {
  position: absolute;
  opacity: 0;
  cursor: pointer;
  height: 0;
  width: 0;
}

.checkmark {
  position: absolute;
  top: 0;
  left: 0;
  height: 22px;
  width: 22px;
  box-sizing: border-box;
  background-color: transparent;
  border: 2px solid var(--accent);
  border-radius: 6px;
  transition: all 0.2s;
}

.checkbox-container input:checked ~ .checkmark {
  background-color: var(--accent);
  box-shadow: 0 0 10px var(--accent);
}

.checkmark:after {
  content: "";
  position: absolute;
  display: none;
  left: 6px;
  top: 2px;
  width: 5px;
  height: 10px;
  border: solid #1c1917;
  border-width: 0 2px 2px 0;
  transform: rotate(45deg);
}

.checkbox-container input:checked ~ .checkmark:after {
  display: block;
}

/* Ενότητες: Waiting / Completed */
.tasks-scroll {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
  padding-right: 15px;
}

.tasks-scroll::-webkit-scrollbar {
  width: 8px;
}

.tasks-scroll::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.2);
  border-radius: 4px;
}

.tasks-scroll::-webkit-scrollbar-thumb {
  background: rgba(var(--accent-rgb), 0.4);
  border-radius: 4px;
}

.tasks-scroll .tasks-list {
  overflow: visible;
  padding-right: 0;
  flex: none;
}

.section-title {
  margin: 0 0 0.8rem;
  color: var(--accent);
  font-size: 0.85rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.section-title:not(:first-child) {
  margin-top: 1.8rem;
}

.section-empty {
  margin: 0;
  color: #a8a29e;
  font-style: italic;
  font-size: 0.95rem;
}

.form-row {
  display: flex;
  gap: 10px;
}

.form-row > * {
  flex: 1;
  min-width: 0;
}

.task-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  margin-top: 5px;
}

.priority-pill, .category-tag {
  font-size: 0.75rem;
  padding: 3px 8px;
  border-radius: 6px;
}

.priority-pill.high { color: #f87171; background: rgba(239, 68, 68, 0.12); border: 1px solid rgba(239, 68, 68, 0.4); }
.priority-pill.medium { color: #fbbf24; background: rgba(251, 191, 36, 0.12); border: 1px solid rgba(251, 191, 36, 0.4); }
.priority-pill.low { color: #4ade80; background: rgba(74, 222, 128, 0.12); border: 1px solid rgba(74, 222, 128, 0.4); }

.category-tag {
  color: var(--accent);
  background: rgba(var(--accent-rgb), 0.12);
  border: 1px solid rgba(var(--accent-rgb), 0.35);
}


.done-by {
  color: #4ade80;
  font-size: 0.85rem;
}

.new-pill {
  display: inline-block;
  margin-left: 8px;
  padding: 1px 8px;
  border-radius: 8px;
  background: #ef4444;
  color: #ffffff;
  font-size: 0.65rem;
  font-weight: 700;
  letter-spacing: 1px;
  vertical-align: middle;
  box-shadow: 0 0 10px rgba(239, 68, 68, 0.6);
}

.fade-enter-active, .fade-leave-active {
  transition: all 0.4s ease;
}

.fade-enter-from, .fade-leave-to {
  opacity: 0;
  transform: translateX(30px);
}

/* Κεντραρισμένο μήνυμα όταν η λίστα είναι άδεια */
.empty-state {
  display: flex;
  flex: 1;
  justify-content: center;
  align-items: center;
  color: #a8a29e;
  font-style: italic;
  font-size: 1.1rem;
  text-align: center;
}

/* === Community Tasks === */
.widget-hint {
  margin: -0.5rem 0 1rem;
  color: #a8a29e;
  font-size: 0.85rem;
}

.wall-msg {
  margin-bottom: 1rem;
  flex-shrink: 0;
}

/* Στη θέση του checkbox στα δικά μου / ολοκληρωμένα tasks */
.own-mark {
  width: 22px;
  margin-top: 2px;
  flex-shrink: 0;
  text-align: center;
  font-size: 1rem;
}

.task-item.mine {
  border-style: dashed;
}

.posted-date {
  color: #a8a29e;
  font-size: 0.75rem;
}
</style>

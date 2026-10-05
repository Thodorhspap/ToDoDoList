<template>
  <div class="page-container">

    <ParticlesBg />

    <!-- Λεπτό οριζόντιο board κορυφής -->
    <div class="top-settings-bar">
      <button class="bar-btn" @click="router.push('/drop')">
        ← Back to Board
      </button>
    </div>

    <div class="board-container">
      <!-- ΑΡΙΣΤΕΡΗ ΣΤΗΛΗ -->
      <div class="left-sidebar">
        <div class="add-task-widget">
          <h3 class="widget-title">Send a Task</h3>
          <!-- Tasks στέλνονται μόνο σε φίλους -->
          <div v-if="friendsLoaded && friends.length === 0" class="no-friends">
            <p>You can only send tasks to your friends, and you don't have any yet.</p>
            <button class="add-btn" @click="router.push('/friends')">👥 Find Friends</button>
          </div>
          <form v-else @submit.prevent="sendTask" class="add-task-form">
            <select v-model="recipient" required>
              <option value="" disabled>Send to a friend...</option>
              <option v-for="name in friends" :key="name" :value="name">{{ name }}</option>
            </select>
            <input v-model="newTaskTitle" type="text" placeholder="Task Title" required />
            <textarea v-model="newDescription" placeholder="Description (optional)..." rows="3"></textarea>
            <div class="form-row">
              <select v-model="newPriority">
                <option value="">No priority</option>
                <option v-for="(p, key) in PRIORITIES" :key="key" :value="key">{{ p.icon }} {{ p.label }}</option>
              </select>
              <input v-model="newCategory" type="text" placeholder="Category" maxlength="30" />
            </div>
            <div class="date-container">
              <label for="dueDate">Due Date (optional):</label>
              <input id="dueDate" v-model="newDueDate" type="date" />
            </div>
            <p v-if="message" class="form-msg" :class="{ error: isError }">{{ message }}</p>
            <button type="submit" class="add-btn" :disabled="sending">
              {{ sending ? 'Sending...' : 'Send Task 📨' }}
            </button>
          </form>
        </div>
      </div>

      <!-- ΔΕΞΙΟ BOARD -->
      <div class="board-card">
        <div class="board-header">
          <h1 class="title">Friends <span class="accent-word">Tasks</span></h1>
          <p class="subtitle">Tasks your friends sent you</p>
        </div>

        <div v-if="receivedTasks.length > 0" class="tasks-scroll">
          <!-- Ανοιχτά tasks από φίλους -->
          <h3 class="section-title">📥 To Do ({{ openReceived.length }})</h3>
          <TransitionGroup name="fade" tag="div" class="tasks-list">
            <div
              v-for="task in openReceived"
              :key="task.id"
              class="task-item"
              :class="{ completed: completing.has(task.id) }"
            >
              <label class="checkbox-container" title="Mark as completed">
                <input type="checkbox" :checked="completing.has(task.id)" @change="completeTask(task, $event)" />
                <span class="checkmark"></span>
              </label>

              <div class="task-content">
                <span class="task-title">
                  {{ task.text || 'Χωρίς Τίτλο' }}
                  <span v-if="task.seen === false" class="new-pill">NEW</span>
                </span>
                <p v-if="task.description" class="task-desc">{{ task.description }}</p>
                <div class="task-meta">
                  <span v-if="task.priority" class="priority-pill" :class="task.priority.toLowerCase()">
                    {{ PRIORITIES[task.priority]?.icon }} {{ PRIORITIES[task.priority]?.label }}
                  </span>
                  <span v-if="task.category" class="category-tag">#{{ task.category }}</span>
                  <span v-if="task.dueDate" class="task-date" :class="dueStatus(task)">{{ dueLabel(task) }}</span>
                </div>
              </div>

              <div class="task-side">
                <span class="task-author">📨 from: {{ task.sentBy }}</span>
              </div>
            </div>
          </TransitionGroup>
          <p v-if="openReceived.length === 0" class="section-empty">All caught up! Nothing left from your friends. 🎉</p>

          <!-- Ολοκληρωμένα (τελευταίες 7 μέρες) -->
          <template v-if="doneReceived.length > 0">
            <h3 class="section-title">✅ Completed (last 7 days)</h3>
            <TransitionGroup name="fade" tag="div" class="tasks-list">
              <div v-for="task in doneReceived" :key="task.id" class="task-item completed">
                <label class="checkbox-container" title="Move back to To Do">
                  <input type="checkbox" checked @change="reopenTask(task)" />
                  <span class="checkmark"></span>
                </label>
                <div class="task-content">
                  <span class="task-title">{{ task.text || 'Χωρίς Τίτλο' }}</span>
                  <span class="done-by">✅ Completed · {{ formatDate(task.completedAt) }}</span>
                </div>
                <div class="task-side">
                  <span class="task-author">📨 from: {{ task.sentBy }}</span>
                </div>
              </div>
            </TransitionGroup>
          </template>
        </div>

        <div v-else class="empty-state">
          <p>No tasks from your friends yet. When a friend sends you one, it shows up here.</p>
        </div>
      </div>

      <!-- ΔΕΞΙΑ ΣΤΗΛΗ: μικρό panel με τα tasks που έστειλα -->
      <div class="sent-panel">
        <h3 class="widget-title">📨 Tasks I Sent <span class="count">{{ sentTasks.length }}</span></h3>
        <div v-if="sentTasks.length > 0" class="mini-list">
          <div
            v-for="task in sentList"
            :key="task.id"
            class="mini-row"
            :class="{ done: task.done }"
          >
            <span class="mini-status">{{ task.done ? '✅' : '⏳' }}</span>
            <div class="mini-info">
              <span class="mini-title">
                {{ task.text || 'Χωρίς Τίτλο' }}
                <span v-if="task.completionSeen === false" class="new-pill">NEW</span>
              </span>
              <span class="mini-to">
                to {{ task.userId }}<template v-if="task.done"> · done {{ formatDate(task.completedAt) }}</template>
              </span>
            </div>
            <button v-if="!task.done" class="mini-cancel" title="Cancel this task" @click="cancelTask(task.id)">✕</button>
          </div>
        </div>
        <p v-else class="section-empty">Nothing sent yet.</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useSettings } from '~/composables/useSettings'
import { refreshNotifications } from '~/composables/useNotifications'
import { PRIORITIES, formatDate, dueStatus, dueLabel } from '~/composables/taskMeta'
import { celebrateTask } from '~/composables/useConfetti'
import { getCurrentUser } from '~/composables/useAuth'

const router = useRouter()
const route = useRoute()
const settings = useSettings()

const currentUser = ref('')
const friends = ref([])
const friendsLoaded = ref(false)
const sentTasks = ref([])
const receivedTasks = ref([])

const recipient = ref('')
const newTaskTitle = ref('')
const newDescription = ref('')
const newDueDate = ref('')
const newPriority = ref('')
const newCategory = ref('')

// Tasks που μόλις τσεκάραμε (μένουν λίγο στα ανοιχτά για το animation)
const completing = ref(new Set())

const byCompletedDesc = (a, b) => (b.completedAt || '').localeCompare(a.completedAt || '')

// Tasks που μου έστειλαν φίλοι: ανοιχτά και όσα ολοκλήρωσα τις τελευταίες 7 μέρες
const openReceived = computed(() => receivedTasks.value.filter(t => !t.done).sort((a, b) => b.id - a.id))
const doneReceived = computed(() => {
  const weekAgo = Date.now() - 7 * 24 * 60 * 60 * 1000
  return receivedTasks.value
    .filter(t => t.done && t.completedAt && new Date(t.completedAt).getTime() > weekAgo)
    .sort(byCompletedDesc)
})

// Tasks που έστειλα: πρώτα όσα περιμένουν, μετά τα ολοκληρωμένα
const sentList = computed(() => [
  ...sentTasks.value.filter(t => !t.done),
  ...sentTasks.value.filter(t => t.done).sort(byCompletedDesc)
])

const message = ref('')
const isError = ref(false)
const sending = ref(false)
let sentTimer = null

onMounted(async () => {
  const savedUser = getCurrentUser()
  if (!savedUser) {
    // Χωρίς σύνδεση δεν στέλνει κανείς tasks
    router.push('/login')
    return
  }
  currentUser.value = savedUser

  loadSentTasks()
  loadReceivedTasks()
  // Κάθε 5 δευτερόλεπτα: νέα tasks από φίλους και ολοκληρώσεις όσων στείλαμε
  sentTimer = setInterval(() => {
    if (document.hidden) return
    loadSentTasks()
    loadReceivedTasks()
  }, 5000)

  try {
    const data = await $fetch(`http://localhost:8080/api/friends/${encodeURIComponent(currentUser.value)}`)
    friends.value = data.friends
    // Από τη συνομιλία έρχεται /send?to=όνομα, το επιλέγουμε αν είναι φίλος
    const to = route.query.to ? String(route.query.to) : ''
    if (friends.value.includes(to)) recipient.value = to
  } catch (error) {
    console.error('❌ Error fetching friends:', error)
  } finally {
    friendsLoaded.value = true
  }
})

onUnmounted(() => {
  clearInterval(sentTimer)
})

const loadSentTasks = async () => {
  try {
    const data = await $fetch(`http://localhost:8080/api/tasks/sent/${encodeURIComponent(currentUser.value)}`)
    if (!Array.isArray(data)) return
    // Παίρνουμε την έκδοση του server, εκτός από tasks που τσεκάρουμε αυτή τη στιγμή.
    // Το "NEW" στα ολοκληρωμένα μένει όσο είμαστε σε αυτή τη σελίδα.
    const existing = new Map(sentTasks.value.map(task => [task.id, task]))
    sentTasks.value = data.map(task => {
      const old = existing.get(task.id)
      if (!old) return task
      if (completing.value.has(task.id)) return old
      return { ...task, completionSeen: old.completionSeen === false ? false : task.completionSeen }
    })

    // Ο φίλος ολοκλήρωσε κάποιο task: το είδαμε, οπότε σβήνει η ειδοποίηση
    if (data.some(task => task.completionSeen === false)) {
      await $fetch(`http://localhost:8080/api/tasks/completions-seen/${encodeURIComponent(currentUser.value)}`, { method: 'POST' })
      refreshNotifications()
    }
  } catch (error) {
    console.error('❌ Error fetching sent tasks:', error)
  }
}

const userPath = () => encodeURIComponent(currentUser.value)

const loadReceivedTasks = async () => {
  try {
    const data = await $fetch(`http://localhost:8080/api/tasks/${userPath()}`)
    if (!Array.isArray(data)) return
    const fromFriends = data.filter(task => task.sentBy)

    // Παίρνουμε την έκδοση του server, εκτός από tasks που τσεκάρουμε αυτή τη στιγμή.
    // Το "NEW" μένει όσο είμαστε σε αυτή τη σελίδα.
    const existing = new Map(receivedTasks.value.map(task => [task.id, task]))
    receivedTasks.value = fromFriends.map(task => {
      const old = existing.get(task.id)
      if (!old) return task
      if (completing.value.has(task.id)) return old
      return { ...task, seen: old.seen === false ? false : task.seen }
    })

    // Νέα tasks από φίλους: τα είδαμε, οπότε σβήνει η ειδοποίηση
    if (fromFriends.some(task => task.seen === false)) {
      await $fetch(`http://localhost:8080/api/tasks/seen/${userPath()}`, { method: 'POST' })
      refreshNotifications()
    }
  } catch (error) {
    console.error('❌ Error fetching received tasks:', error)
  }
}

const sendTask = async () => {
  const to = recipient.value.trim()
  if (!to || newTaskTitle.value.trim() === '') return

  message.value = ''
  sending.value = true
  try {
    const payload = {
      text: newTaskTitle.value,
      description: newDescription.value,
      dueDate: newDueDate.value ? `${newDueDate.value}T00:00:00` : null,
      author: settings.value.defaultAuthor || currentUser.value,
      sentBy: currentUser.value,
      priority: newPriority.value || null,
      category: newCategory.value.trim() || null,
      done: false
    }

    const savedTask = await $fetch(`http://localhost:8080/api/tasks/send/${encodeURIComponent(to)}`, {
      method: 'POST',
      body: payload
    })

    sentTasks.value.unshift(savedTask)
    newTaskTitle.value = ''
    newDescription.value = ''
    newDueDate.value = ''
    newPriority.value = ''
    newCategory.value = ''
    isError.value = false
    message.value = `Task sent to ${to}!`
  } catch (error) {
    console.error('❌ Error sending task:', error)
    isError.value = true
    message.value = error?.data?.message || 'Could not send the task. Is the server running?'
  } finally {
    sending.value = false
  }
}

const setDone = (task, done) =>
  $fetch(`http://localhost:8080/api/tasks/${task.id}/done`, {
    method: 'PUT',
    body: { done, username: currentUser.value }
  })

const applySaved = (saved) => {
  const target = receivedTasks.value.find(t => t.id === saved.id)
  if (target) Object.assign(target, saved)
}

// Ολοκλήρωση task από φίλο: γιορτή, γραμμή πάνω του και μετά πάει στα Completed.
// Ο φίλος που το έστειλε παίρνει ειδοποίηση.
const completeTask = async (task, event) => {
  completing.value.add(task.id)
  celebrateTask(event?.target)
  try {
    const saved = await setDone(task, true)
    setTimeout(() => {
      applySaved(saved)
      completing.value.delete(task.id)
    }, 400)
  } catch (error) {
    console.error('❌ Error completing task:', error)
    completing.value.delete(task.id)
  }
}

// Επαναφορά από τα Completed στα To Do
const reopenTask = async (task) => {
  try {
    applySaved(await setDone(task, false))
  } catch (error) {
    console.error('❌ Error reopening task:', error)
  }
}

const cancelTask = async (id) => {
  try {
    await $fetch(`http://localhost:8080/api/tasks/${id}`, { method: 'DELETE' })
    sentTasks.value = sentTasks.value.filter(task => task.id !== id)
  } catch (error) {
    console.error('❌ Error cancelling task:', error)
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

/* Λεπτό οριζόντιο board κορυφής */
.top-settings-bar {
  position: relative;
  z-index: 2;
  width: 100%;
  background: rgba(28, 25, 23, 0.65);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(var(--accent-rgb), 0.2);
  padding: 0.5rem 1.5rem;
  border-radius: 14px;
  display: flex;
  justify-content: flex-start;
  align-items: center;
  box-sizing: border-box;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3);
  flex-shrink: 0;
}

.bar-btn {
  position: relative;
  background: transparent;
  color: var(--accent);
  border: 1px solid rgba(var(--accent-rgb), 0.5);
  border-radius: 8px;
  padding: 0.35rem 0.9rem;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  font-family: 'Poppins', sans-serif;
  transition: all 0.3s ease;
}

.bar-btn:hover {
  background: var(--accent);
  color: #1c1917;
  box-shadow: 0 0 15px rgba(var(--accent-rgb), 0.6);
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

.no-friends {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.no-friends p {
  margin: 0;
  color: #a8a29e;
  font-size: 0.95rem;
}

.add-task-form input:focus, .add-task-form textarea:focus {
  outline: none;
  border-color: var(--accent);
  background: rgba(0, 0, 0, 0.5);
  box-shadow: 0 0 15px rgba(var(--accent-rgb), 0.3);
}

.date-container {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.date-container label {
  font-size: 0.85rem;
  color: #a8a29e;
  font-weight: 500;
}

.date-container input[type="date"] {
  cursor: pointer;
  color: white;
  color-scheme: dark;
}

.date-container input[type="date"]::-webkit-calendar-picker-indicator {
  cursor: pointer;
  filter: invert(1);
  opacity: 0.7;
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

.task-date {
  display: inline-block;
  margin-top: 5px;
  font-size: 0.8rem;
  color: #fbbf24;
  background: rgba(251, 191, 36, 0.1);
  border: 1px solid rgba(251, 191, 36, 0.3);
  padding: 3px 8px;
  border-radius: 6px;
  align-self: flex-start;
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

.task-item.completed .task-date {
  color: #57534e;
  border-color: #57534e;
  background: transparent;
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

.task-meta .task-date {
  margin-top: 0;
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

/* Λήγει σήμερα / έληξε */
.task-date.today {
  color: #fb923c;
  background: rgba(251, 146, 60, 0.12);
  border-color: rgba(251, 146, 60, 0.5);
}

.task-date.overdue {
  color: #f87171;
  background: rgba(239, 68, 68, 0.15);
  border-color: rgba(239, 68, 68, 0.6);
  font-weight: 600;
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
</style>

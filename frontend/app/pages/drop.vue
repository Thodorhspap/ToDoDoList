<template>
  <div class="page-container">

    <ParticlesBg />

    <!-- Λεπτό οριζόντιο board κορυφής -->
    <div class="top-settings-bar">
      <!-- Ποιος χρήστης είναι συνδεδεμένος (στην αντίθετη πλευρά από τα κουμπιά) -->
      <span v-if="currentUserId" class="current-user">
        User: <b>{{ currentUserId }}</b>
      </span>
      <button class="settings-btn" title="Your friends" @click="router.push('/friends')">
        👥 Friends
        <NotifyBadge :count="notifications.friendRequests + notifications.unreadMessages" />
      </button>
      <button class="settings-btn" title="Tasks you send to your friends" @click="router.push('/send')">
        📨 Friends Tasks
        <NotifyBadge :count="notifications.newTasks + notifications.completedTasks" />
      </button>
      <button class="settings-btn" title="Settings" @click="router.push('/settings')">
        ⚙️ Settings
      </button>
    </div>

    <div class="board-container">
      <!-- ΑΡΙΣΤΕΡΗ ΣΤΗΛΗ -->
      <div class="left-sidebar">
        <div class="add-task-widget">
          <h3 class="widget-title">New Task</h3>
          <form @submit.prevent="addTask" class="add-task-form">
            <div class="form-row">
              <input v-model="newAuthor" type="text" placeholder="Name" required />
              <input v-model="newTaskTitle" type="text" placeholder="Task Title" required />
            </div>
            <textarea v-model="newDescription" placeholder="Description (optional)..." rows="3"></textarea>
            <div class="form-row">
              <select v-model="newPriority">
                <option value="">No priority</option>
                <option v-for="(p, key) in PRIORITIES" :key="key" :value="key">{{ p.icon }} {{ p.label }}</option>
              </select>
              <input v-model="newCategory" type="text" list="categories" placeholder="Category" maxlength="30" />
            </div>
            <div class="date-container">
              <label for="dueDate">Due Date (optional):</label>
              <input id="dueDate" v-model="newDueDate" type="date" />
            </div>
            <button type="submit" class="add-btn">Add Task</button>
          </form>
          <datalist id="categories">
            <option v-for="c in categories" :key="c" :value="c" />
          </datalist>
        </div>

        <div class="fun-counter-widget" @click="incrementCount($event)">
          <div class="counter-glow"></div>
          <span class="counter-label">Global Clicker</span>
          <span class="counter-number">{{ globalCount }}</span>
          <span class="counter-hint">Click to BOOST!</span>
        </div>
      </div>

      <!-- ΔΕΞΙΟ BOARD -->
      <div class="board-card">
        <div class="board-header">
          <h1 class="title">To Do<span class="pink-do">(Do)</span> List</h1>

          <!-- Καρτέλες: ανοιχτά / ολοκληρωμένα -->
          <div class="tabs-row">
            <div class="tabs">
              <button :class="{ active: tab === 'todo' }" @click="tab = 'todo'">To Do ({{ openTasks.length }})</button>
              <button :class="{ active: tab === 'done' }" @click="tab = 'done'">Done ({{ doneTasks.length }})</button>
            </div>
            <span class="week-stats">You finished <b>{{ finishedThisWeek }}</b> {{ finishedThisWeek === 1 ? 'task' : 'tasks' }} this week</span>
          </div>

          <!-- Φίλτρα -->
          <div class="filters">
            <select v-model="filterPriority">
              <option value="">All priorities</option>
              <option v-for="(p, key) in PRIORITIES" :key="key" :value="key">{{ p.icon }} {{ p.label }}</option>
            </select>
            <select v-model="filterCategory">
              <option value="">All categories</option>
              <option v-for="c in categories" :key="c" :value="c">{{ c }}</option>
            </select>
            <button v-if="filterPriority || filterCategory" class="text-btn" @click="filterPriority = ''; filterCategory = ''">Clear filters</button>
          </div>
        </div>

        <!-- ΑΝΟΙΧΤΑ TASKS -->
        <template v-if="tab === 'todo'">
          <TransitionGroup name="fade" tag="div" class="tasks-list" v-if="visibleOpenTasks.length > 0">
            <div
              v-for="task in visibleOpenTasks"
              :key="task.id"
              class="task-item"
              :class="{ completed: completing.has(task.id), ['priority-' + (task.priority || 'NONE').toLowerCase()]: true }"
            >
              <!-- Επεξεργασία -->
              <form v-if="editingId === task.id" class="edit-form" @submit.prevent="saveEdit(task)">
                <input v-model="editForm.text" type="text" placeholder="Task Title" required />
                <textarea v-model="editForm.description" placeholder="Description (optional)..." rows="2"></textarea>
                <div class="form-row">
                  <select v-model="editForm.priority">
                    <option value="">No priority</option>
                    <option v-for="(p, key) in PRIORITIES" :key="key" :value="key">{{ p.icon }} {{ p.label }}</option>
                  </select>
                  <input v-model="editForm.category" type="text" list="categories" placeholder="Category" maxlength="30" />
                  <input v-model="editForm.dueDate" type="date" />
                </div>
                <div class="edit-actions">
                  <button type="submit" class="small-btn">Save</button>
                  <button type="button" class="text-btn" @click="editingId = null">Cancel</button>
                </div>
              </form>

              <template v-else>
                <label class="checkbox-container" title="Mark as completed">
                  <input type="checkbox" :checked="completing.has(task.id)" @change="completeTask(task, $event)" />
                  <span class="checkmark"></span>
                </label>

                <div class="task-content">
                  <span class="task-title">
                    {{ task.title || task.text || 'Χωρίς Τίτλο' }}
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
                  <span v-if="task.author" class="task-author">by: {{ task.author }}</span>
                  <button class="icon-btn" title="Edit task" @click="startEdit(task)">✏️</button>
                </div>
              </template>
            </div>
          </TransitionGroup>

          <div v-else class="empty-state">
            <p v-if="openTasks.length === 0">No tasks yet! Add one from the left.</p>
            <p v-else>No tasks match the filters.</p>
          </div>
        </template>

        <!-- ΟΛΟΚΛΗΡΩΜΕΝΑ TASKS -->
        <template v-else>
          <TransitionGroup name="fade" tag="div" class="tasks-list" v-if="visibleDoneTasks.length > 0">
            <div v-for="task in visibleDoneTasks" :key="task.id" class="task-item completed">
              <label class="checkbox-container" title="Move back to To Do">
                <input type="checkbox" checked @change="restoreTask(task)" />
                <span class="checkmark"></span>
              </label>

              <div class="task-content">
                <span class="task-title">{{ task.title || task.text || 'Χωρίς Τίτλο' }}</span>
                <p v-if="task.description" class="task-desc">{{ task.description }}</p>
                <div class="task-meta">
                  <span v-if="task.priority" class="priority-pill" :class="task.priority.toLowerCase()">
                    {{ PRIORITIES[task.priority]?.icon }} {{ PRIORITIES[task.priority]?.label }}
                  </span>
                  <span v-if="task.category" class="category-tag">#{{ task.category }}</span>
                  <span v-if="task.completedAt" class="done-date">✅ Completed {{ formatDate(task.completedAt) }}</span>
                </div>
              </div>

              <div class="task-side">
                <button class="icon-btn" title="Delete forever" @click="deleteTask(task)">🗑️</button>
              </div>
            </div>
          </TransitionGroup>

          <div v-else class="empty-state">
            <p v-if="doneTasks.length === 0">No completed tasks yet. Tick one off!</p>
            <p v-else>No completed tasks match the filters.</p>
          </div>
        </template>
      </div>
    </div>

    <!-- Μήνυμα αναίρεσης μετά την ολοκλήρωση -->
    <Transition name="toast">
      <div v-if="undoTask" class="undo-toast">
        <span>✅ Task completed</span>
        <button class="undo-btn" @click="undoComplete">Undo</button>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted, onUnmounted } from 'vue'
import { useSettings } from '~/composables/useSettings'
import { useNotifications } from '~/composables/useNotifications'
import { PRIORITIES, priorityRank, formatDate, dueStatus, dueLabel, categoriesOf } from '~/composables/taskMeta'
import { burstConfetti, celebrateTask } from '~/composables/useConfetti'
import { useRouter } from 'vue-router'
import { getCurrentUser } from '~/composables/useAuth'

const API = 'http://localhost:8080/api/tasks'
const UNDO_SECONDS = 5

const router = useRouter()
const settings = useSettings()
const notifications = useNotifications()

const newAuthor = ref('')
const newTaskTitle = ref('')
const newDescription = ref('')
const newDueDate = ref('')
const newPriority = ref('')
const newCategory = ref('')
const globalCount = ref(0)
const tasks = ref([])

const tab = ref('todo')
const filterPriority = ref('')
const filterCategory = ref('')

// Tasks που μόλις τσεκάραμε (μένουν λίγο στη λίστα για το animation)
const completing = ref(new Set())
const completeTimers = new Map()

// Επεξεργασία
const editingId = ref(null)
const editForm = reactive({ text: '', description: '', dueDate: '', priority: '', category: '' })

// Αναίρεση
const undoTask = ref(null)
let undoTimer = null

const currentUserId = ref('')
let countTimer = null
let tasksTimer = null

// Συμπληρώνει αυτόματα το όνομα από τα Settings, αν το πεδίο είναι κενό
watch(() => settings.value.defaultAuthor, (name) => {
  if (name && !newAuthor.value) newAuthor.value = name
}, { immediate: true })

const categories = computed(() => categoriesOf(tasks.value))

const openTasks = computed(() => tasks.value.filter(t => !t.done))
const doneTasks = computed(() => tasks.value.filter(t => t.done))

const matchesFilters = (task) =>
  (!filterPriority.value || task.priority === filterPriority.value) &&
  (!filterCategory.value || task.category === filterCategory.value)

// Ταξινόμηση tasks σύμφωνα με τα Settings
const sortTasks = (list) => {
  switch (settings.value.taskSort) {
    case 'oldest':
      return list.sort((a, b) => a.id - b.id)
    case 'dueDate':
      // Όσα δεν έχουν ημερομηνία πάνε στο τέλος
      return list.sort((a, b) => {
        if (!a.dueDate && !b.dueDate) return b.id - a.id
        if (!a.dueDate) return 1
        if (!b.dueDate) return -1
        return a.dueDate.localeCompare(b.dueDate)
      })
    case 'name':
      return list.sort((a, b) => (a.title || a.text || '').localeCompare(b.title || b.text || ''))
    case 'priority':
      return list.sort((a, b) => priorityRank(a.priority) - priorityRank(b.priority) || b.id - a.id)
    default:
      return list.sort((a, b) => b.id - a.id)
  }
}

const visibleOpenTasks = computed(() => sortTasks(openTasks.value.filter(matchesFilters)))

// Τα ολοκληρωμένα: τα πιο πρόσφατα πρώτα
const visibleDoneTasks = computed(() =>
  doneTasks.value.filter(matchesFilters)
    .sort((a, b) => (b.completedAt || '').localeCompare(a.completedAt || ''))
)

// Πόσα ολοκληρώθηκαν από τη Δευτέρα αυτής της εβδομάδας
const finishedThisWeek = computed(() => {
  const monday = new Date()
  monday.setHours(0, 0, 0, 0)
  monday.setDate(monday.getDate() - ((monday.getDay() + 6) % 7))
  return doneTasks.value.filter(t => t.completedAt && new Date(t.completedAt) >= monday).length
})

onMounted(async () => {
  const savedUser = getCurrentUser()
  if (!savedUser) {
    // Χωρίς σύνδεση δεν βλέπει κανείς το board
    router.push('/login')
    return
  }
  currentUserId.value = savedUser

  loadCount()
  // Κάθε 2 δευτερόλεπτα φέρνουμε την τιμή, ώστε να βλέπουμε και τα κλικ των άλλων χρηστών
  countTimer = setInterval(() => {
    if (!document.hidden) loadCount()
  }, 2000)

  loadTasks()
  // Κάθε 5 δευτερόλεπτα ανανεώνουμε τα tasks (π.χ. αλλαγές από άλλη καρτέλα)
  tasksTimer = setInterval(() => {
    if (!document.hidden) loadTasks()
  }, 5000)
})

onUnmounted(() => {
  clearInterval(countTimer)
  clearInterval(tasksTimer)
  clearTimeout(undoTimer)
})

const userPath = () => encodeURIComponent(currentUserId.value)

const loadTasks = async () => {
  try {
    const data = await $fetch(`${API}/${userPath()}`)
    if (!Array.isArray(data)) return
    // Μόνο τα δικά μου tasks: όσα μου έστειλαν φίλοι εμφανίζονται στο Friends Tasks
    const mine = data.filter(task => !task.sentBy)

    // Παίρνουμε την έκδοση του server, εκτός από tasks που αλλάζουμε αυτή τη στιγμή
    const existing = new Map(tasks.value.map(task => [task.id, task]))
    tasks.value = mine.map(task => {
      const old = existing.get(task.id)
      if (old && (completing.value.has(task.id) || editingId.value === task.id)) return old
      return task
    })
  } catch (error) {
    console.error('❌ Error fetching tasks:', error)
  }
}

const addTask = async () => {
  if (newTaskTitle.value.trim() === '' || newAuthor.value.trim() === '') return

  try {
    const payload = {
      text: newTaskTitle.value,
      description: newDescription.value,
      dueDate: newDueDate.value ? `${newDueDate.value}T00:00:00` : null,
      author: newAuthor.value,
      priority: newPriority.value || null,
      category: newCategory.value.trim() || null,
      done: false
    }

    const savedTask = await $fetch(`${API}/${userPath()}`, {
      method: 'POST',
      body: payload
    })

    tasks.value.unshift(savedTask)
    tab.value = 'todo'
    newTaskTitle.value = ''
    newDescription.value = ''
    newDueDate.value = ''
    newPriority.value = ''
    newCategory.value = ''
  } catch (error) {
    console.error('❌ Error adding task:', error)
    alert('Σφάλμα κατά την αποθήκευση. Δες την κονσόλα (F12).')
  }
}

// Ενημερώνει το task στη λίστα με την απάντηση του server (βρίσκοντάς το με το id,
// γιατί ο αυτόματος έλεγχος κάθε 5 δευτερόλεπτα μπορεί να έχει αντικαταστήσει το αντικείμενο)
const applySaved = (saved) => {
  const target = tasks.value.find(t => t.id === saved.id)
  if (target) Object.assign(target, saved)
}

const setDone = (task, done) =>
  $fetch(`${API}/${task.id}/done`, {
    method: 'PUT',
    body: { done, username: currentUserId.value }
  })

// Ολοκλήρωση: το task διαγράφεται με γραμμή, μετά πάει στην καρτέλα Done
const completeTask = async (task, event) => {
  completing.value.add(task.id)
  celebrateTask(event?.target)
  showUndo(task)
  try {
    const saved = await setDone(task, true)
    completeTimers.set(task.id, setTimeout(() => {
      applySaved(saved)
      completing.value.delete(task.id)
      completeTimers.delete(task.id)
    }, 400))
  } catch (error) {
    console.error('❌ Error completing task:', error)
    completing.value.delete(task.id)
    hideUndo()
  }
}

const showUndo = (task) => {
  clearTimeout(undoTimer)
  undoTask.value = task
  undoTimer = setTimeout(hideUndo, UNDO_SECONDS * 1000)
}

const hideUndo = () => {
  clearTimeout(undoTimer)
  undoTask.value = null
}

const undoComplete = async () => {
  const task = undoTask.value
  if (!task) return
  hideUndo()
  clearTimeout(completeTimers.get(task.id))
  completeTimers.delete(task.id)
  completing.value.delete(task.id)
  await restoreTask(task)
}

// Επαναφορά από το Done στο To Do
const restoreTask = async (task) => {
  try {
    const saved = await setDone(task, false)
    applySaved(saved)
  } catch (error) {
    console.error('❌ Error restoring task:', error)
  }
}

const deleteTask = async (task) => {
  if (!confirm(`Delete "${task.text}" forever?`)) return
  try {
    await $fetch(`${API}/${task.id}`, { method: 'DELETE' })
    tasks.value = tasks.value.filter(t => t.id !== task.id)
  } catch (error) {
    console.error('❌ Error deleting task:', error)
  }
}

const startEdit = (task) => {
  editingId.value = task.id
  editForm.text = task.text || ''
  editForm.description = task.description || ''
  editForm.dueDate = formatDate(task.dueDate)
  editForm.priority = task.priority || ''
  editForm.category = task.category || ''
}

const saveEdit = async (task) => {
  if (!editForm.text.trim()) return
  try {
    const saved = await $fetch(`${API}/${task.id}`, {
      method: 'PUT',
      body: {
        text: editForm.text.trim(),
        description: editForm.description,
        dueDate: editForm.dueDate ? `${editForm.dueDate}T00:00:00` : null,
        priority: editForm.priority || null,
        category: editForm.category.trim() || null
      }
    })
    applySaved(saved)
    editingId.value = null
  } catch (error) {
    console.error('❌ Error editing task:', error)
    alert(error?.data?.message || 'Σφάλμα κατά την αποθήκευση. Δες την κονσόλα (F12).')
  }
}

// Ο μετρητής μόνο ανεβαίνει, οπότε κρατάμε πάντα τη μεγαλύτερη τιμή
// (έτσι γρήγορα κλικ με απαντήσεις εκτός σειράς δεν τον κάνουν να "πηδάει" προς τα πίσω)
const showCount = (count) => {
  if (typeof count === 'number') globalCount.value = Math.max(globalCount.value, count)
}

const loadCount = async () => {
  try {
    const data = await $fetch('http://localhost:8080/api/counter')
    showCount(data.count)
  } catch (error) {
    console.error('❌ Error fetching counter:', error)
  }
}

const incrementCount = async (event) => {
  burstConfetti(event?.currentTarget, 12)
  globalCount.value++ // άμεση ανταπόκριση, μετά παίρνουμε την πραγματική τιμή από τον server
  try {
    const data = await $fetch('http://localhost:8080/api/counter/increment', { method: 'POST' })
    showCount(data.count)
  } catch (error) {
    console.error('❌ Error incrementing counter:', error)
    globalCount.value--
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
  justify-content: flex-end;
  align-items: center;
  gap: 10px;
  box-sizing: border-box;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3);
  flex-shrink: 0;
}

.current-user {
  margin-right: auto;
  color: #a8a29e;
  font-size: 0.9rem;
  font-weight: 500;
}

.current-user b {
  color: var(--accent);
  font-weight: 700;
}

.settings-btn {
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

.settings-btn:hover {
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

.form-row { 
  display: flex; 
  gap: 10px; 
}

.form-row input { 
  flex: 1; 
  min-width: 0; 
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
  transition: 0.2s; 
}

.date-container input[type="date"]::-webkit-calendar-picker-indicator:hover { 
  opacity: 1; 
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

.fun-counter-widget { 
  position: relative; 
  background: rgba(28, 25, 23, 0.65); 
  backdrop-filter: blur(16px); 
  -webkit-backdrop-filter: blur(16px); 
  border: 1px solid rgba(var(--accent-rgb), 0.2); 
  padding: 1.2rem 1.5rem; 
  border-radius: 16px; 
  text-align: center; 
  cursor: pointer; 
  overflow: hidden; 
  transition: all 0.3s ease; 
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3); 
}

.fun-counter-widget:hover { 
  border-color: var(--accent); 
  transform: translateY(-3px); 
  box-shadow: 0 0 20px rgba(var(--accent-rgb), 0.3); 
}

.fun-counter-widget:active { 
  transform: scale(0.97); 
}

.counter-label { 
  display: block; 
  font-size: 0.85rem; 
  color: var(--accent); 
  font-weight: 600; 
  text-transform: uppercase; 
  letter-spacing: 1px; 
  margin-bottom: 0.2rem; 
}

.counter-number { 
  display: block; 
  font-size: 2.3rem; 
  font-weight: 800; 
  color: #ffffff; 
  text-shadow: 0 0 15px rgba(var(--accent-rgb), 0.6); 
  margin-bottom: 0rem; 
}

.counter-hint { 
  display: block; 
  font-size: 0.75rem; 
  color: #a8a29e; 
  margin-top: -2px; 
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

/* Στυλ για το δεύτερο ροζ (Do) */
.pink-do {
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

.tasks-list::-webkit-scrollbar-thumb:hover { 
  background: var(--accent); 
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
  transform: translateX(5px); 
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
  transition: all 0.3s; 
}

.task-desc { 
  margin: 0; 
  color: #a8a29e; 
  font-size: 0.9rem; 
  line-height: 1.4; 
  transition: all 0.3s; 
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
  transition: all 0.3s; 
}

.task-author { 
  font-size: 0.8rem; 
  color: var(--accent); 
  background: rgba(var(--accent-rgb), 0.15); 
  border: 1px solid rgba(var(--accent-rgb), 0.3); 
  padding: 4px 10px; 
  border-radius: 12px; 
  align-self: flex-start; 
  margin-left: 10px; 
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

.checkbox-container { 
  display: block; 
  position: relative; 
  cursor: pointer; 
  width: 22px; 
  height: 22px; 
  margin-top: 2px; 
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
}

.checkbox-container input:checked ~ .checkmark:after { 
  display: block; 
}

.checkbox-container .checkmark:after { 
  left: 6px; 
  top: 2px; 
  width: 5px; 
  height: 10px; 
  border: solid #1c1917; 
  border-width: 0 2px 2px 0; 
  transform: rotate(45deg); 
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
}
/* ===== Καρτέλες, φίλτρα, στατιστικά ===== */
.tabs-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 1rem;
}

.tabs {
  display: flex;
  border: 1px solid rgba(var(--accent-rgb), 0.5);
  border-radius: 10px;
  overflow: hidden;
}

.tabs button {
  background: transparent;
  color: #a8a29e;
  border: none;
  padding: 0.45rem 1rem;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  font-family: 'Poppins', sans-serif;
  transition: all 0.2s;
}

.tabs button.active {
  background: var(--accent);
  color: #1c1917;
}

.week-stats {
  color: #a8a29e;
  font-size: 0.85rem;
}

.week-stats b {
  color: var(--accent);
}

.filters {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 0.8rem;
}

.filters select, .add-task-form select, .edit-form select {
  background-color: rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(var(--accent-rgb), 0.3);
  border-radius: 10px;
  color: white;
  font-size: 0.85rem;
  font-family: inherit;
  cursor: pointer;
  color-scheme: dark;
}

.filters select {
  padding: 0.4rem 2.2rem 0.4rem 0.7rem;
}

.add-task-form select, .edit-form select {
  flex: 1;
  min-width: 0;
  padding: 0.85rem 2.2rem 0.85rem 0.85rem;
  font-size: 0.95rem;
}

.filters select:focus, .add-task-form select:focus, .edit-form select:focus {
  outline: none;
  border-color: var(--accent);
}

.text-btn {
  background: transparent;
  border: none;
  color: #a8a29e;
  font-size: 0.85rem;
  font-family: 'Poppins', sans-serif;
  cursor: pointer;
  padding: 0.2rem 0.4rem;
}

.text-btn:hover {
  color: var(--accent);
  text-decoration: underline;
}

/* ===== Προτεραιότητα, κατηγορία, ημερομηνίες ===== */
.task-item.priority-high { border-left: 3px solid #ef4444; }
.task-item.priority-medium { border-left: 3px solid #fbbf24; }
.task-item.priority-low { border-left: 3px solid #4ade80; }

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

.priority-pill, .category-tag, .done-date {
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

.done-date {
  color: #4ade80;
  background: rgba(74, 222, 128, 0.08);
  border: 1px solid rgba(74, 222, 128, 0.3);
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

.task-item.completed .priority-pill,
.task-item.completed .category-tag {
  opacity: 0.5;
}

/* ===== Δεξιά πλευρά task, επεξεργασία ===== */
.task-side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
  margin-left: 10px;
  flex-shrink: 0;
}

.task-side .task-author {
  margin-left: 0;
}

.icon-btn {
  background: transparent;
  border: 1px solid rgba(var(--accent-rgb), 0.3);
  border-radius: 8px;
  padding: 3px 8px;
  cursor: pointer;
  font-size: 0.85rem;
  opacity: 0.6;
  transition: all 0.2s;
}

.task-item:hover .icon-btn {
  opacity: 1;
}

.icon-btn:hover {
  border-color: var(--accent);
  background: rgba(var(--accent-rgb), 0.15);
}

.edit-form {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.edit-form input, .edit-form textarea {
  background: rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(var(--accent-rgb), 0.3);
  border-radius: 10px;
  padding: 0.7rem;
  color: white;
  font-size: 0.95rem;
  font-family: inherit;
  box-sizing: border-box;
  min-width: 0;
}

.edit-form .form-row input {
  flex: 1;
}

.edit-form input[type="date"] {
  color-scheme: dark;
}

.edit-form input:focus, .edit-form textarea:focus {
  outline: none;
  border-color: var(--accent);
}

.edit-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.small-btn {
  background: transparent;
  color: var(--accent);
  border: 2px solid var(--accent);
  border-radius: 10px;
  padding: 0.4rem 1.2rem;
  font-size: 0.9rem;
  font-weight: bold;
  cursor: pointer;
  font-family: 'Poppins', sans-serif;
  transition: all 0.3s;
}

.small-btn:hover {
  background: var(--accent);
  color: #1c1917;
}

/* ===== Αναίρεση ===== */
.undo-toast {
  position: fixed;
  bottom: 2rem;
  left: 50%;
  transform: translateX(-50%);
  z-index: 20;
  display: flex;
  align-items: center;
  gap: 1rem;
  background: rgba(28, 25, 23, 0.95);
  border: 1px solid rgba(var(--accent-rgb), 0.5);
  border-radius: 12px;
  padding: 0.7rem 1rem 0.7rem 1.3rem;
  color: #ffffff;
  font-size: 0.95rem;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.5), 0 0 20px rgba(var(--accent-rgb), 0.2);
}

.undo-btn {
  background: var(--accent);
  color: #1c1917;
  border: none;
  border-radius: 8px;
  padding: 0.35rem 1rem;
  font-weight: 700;
  cursor: pointer;
  font-family: 'Poppins', sans-serif;
}

.toast-enter-active, .toast-leave-active {
  transition: all 0.3s ease;
}

.toast-enter-from, .toast-leave-to {
  opacity: 0;
  transform: translate(-50%, 20px);
}
</style>

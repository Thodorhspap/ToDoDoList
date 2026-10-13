<template>
  <div class="page-container">

    <div class="board-card">
      <div class="board-header">
        <div class="header-top">
          <h1 class="title">To Do<span class="pink-do">(Do)</span> List</h1>
          <span class="week-stats">You finished <b>{{ finishedThisWeek }}</b> {{ finishedThisWeek === 1 ? 'task' : 'tasks' }} this week</span>
        </div>

        <!-- Γρήγορη προσθήκη: τίτλος + Enter, τα υπόλοιπα στο ⋯ -->
        <form class="quick-add" @submit.prevent="addTask">
          <div class="quick-row">
            <span class="quick-plus">＋</span>
            <input
              ref="quickInput"
              v-model="newTaskTitle"
              class="quick-input"
              type="text"
              placeholder="Add a task... (press Enter)"
              maxlength="255"
            />
            <button
              type="button"
              class="more-btn"
              :class="{ active: showMore }"
              :title="showMore ? 'Hide options' : 'Description, priority, category, due date'"
              @click="showMore = !showMore"
            >⋯</button>
            <button type="submit" class="add-btn" :disabled="!newTaskTitle.trim()">Add</button>
          </div>

          <div v-if="showMore" class="quick-more">
            <textarea v-model="newDescription" placeholder="Description (optional)..." rows="2"></textarea>
            <div class="form-row">
              <select v-model="newPriority" title="Priority">
                <option value="">No priority</option>
                <option v-for="(p, key) in PRIORITIES" :key="key" :value="key">{{ p.label }}</option>
              </select>
              <input v-model="newCategory" type="text" list="categories" placeholder="Category" maxlength="30" />
              <input v-model="newDueDate" type="date" title="Due date (optional)" />
            </div>
          </div>
        </form>
        <datalist id="categories">
          <option v-for="c in categories" :key="c" :value="c" />
        </datalist>

        <!-- Καρτέλες και φίλτρα -->
        <div class="toolbar">
          <div class="tabs">
            <button :class="{ active: tab === 'todo' }" @click="tab = 'todo'">To Do ({{ openTasks.length }})</button>
            <button :class="{ active: tab === 'done' }" @click="tab = 'done'">Done ({{ doneTasks.length }})</button>
          </div>
          <div class="filters">
            <select v-model="filterPriority">
              <option value="">All priorities</option>
              <option v-for="(p, key) in PRIORITIES" :key="key" :value="key">{{ p.label }}</option>
            </select>
            <select v-model="filterCategory">
              <option value="">All categories</option>
              <option v-for="c in categories" :key="c" :value="c">{{ c }}</option>
            </select>
            <button v-if="filterPriority || filterCategory" class="text-btn" @click="filterPriority = ''; filterCategory = ''">Clear filters</button>
          </div>
        </div>
      </div>

      <!-- ΑΝΟΙΧΤΑ TASKS -->
      <template v-if="tab === 'todo'">
        <TransitionGroup name="fade" tag="div" class="tasks-list" v-if="visibleOpenTasks.length > 0">
          <div
            v-for="task in visibleOpenTasks"
            :key="task.id"
            class="task-item"
            :class="{
              completed: completing.has(task.id),
              selected: selectedId === task.id,
              ['priority-' + (task.priority || 'NONE').toLowerCase()]: true
            }"
            title="Open task"
            @click="openTask(task)"
          >
            <label class="checkbox-container" title="Mark as completed" @click.stop>
              <input type="checkbox" :checked="completing.has(task.id)" @change="completeTask(task, $event)" />
              <span class="checkmark"></span>
            </label>

            <div class="task-content">
              <span class="task-title">{{ task.title || task.text || 'Χωρίς Τίτλο' }}</span>
              <p v-if="task.description" class="task-desc">{{ task.description }}</p>
              <div v-if="hasMeta(task)" class="task-meta">
                <span v-if="task.priority" class="priority-pill" :class="task.priority.toLowerCase()">
                  {{ PRIORITIES[task.priority]?.label }}
                </span>
                <span v-if="task.category" class="category-tag">#{{ task.category }}</span>
                <span v-if="task.dueDate" class="task-date" :class="dueStatus(task)">{{ dueLabel(task) }}</span>
                <span v-if="stepCount(task) > 0" class="steps-pill" :class="{ complete: stepsDone(task) === stepCount(task) }">
                  ☑ {{ stepsDone(task) }}/{{ stepCount(task) }}
                  <span class="mini-bar"><span class="mini-fill" :style="{ width: stepsPercent(task) + '%' }"></span></span>
                </span>
              </div>
            </div>

            <span class="open-hint">›</span>
          </div>
        </TransitionGroup>

        <div v-else class="empty-state">
          <p v-if="openTasks.length === 0">No tasks yet! Type one in the box above.</p>
          <p v-else>No tasks match the filters.</p>
        </div>
      </template>

      <!-- ΟΛΟΚΛΗΡΩΜΕΝΑ TASKS -->
      <template v-else>
        <TransitionGroup name="fade" tag="div" class="tasks-list" v-if="visibleDoneTasks.length > 0">
          <div
            v-for="task in visibleDoneTasks"
            :key="task.id"
            class="task-item completed"
            :class="{ selected: selectedId === task.id }"
            title="Open task"
            @click="openTask(task)"
          >
            <label class="checkbox-container" title="Move back to To Do" @click.stop>
              <input type="checkbox" checked @change="restoreTask(task)" />
              <span class="checkmark"></span>
            </label>

            <div class="task-content">
              <span class="task-title">{{ task.title || task.text || 'Χωρίς Τίτλο' }}</span>
              <div class="task-meta">
                <span v-if="task.priority" class="priority-pill" :class="task.priority.toLowerCase()">
                  {{ PRIORITIES[task.priority]?.label }}
                </span>
                <span v-if="task.category" class="category-tag">#{{ task.category }}</span>
                <span v-if="stepCount(task) > 0" class="steps-pill">☑ {{ stepsDone(task) }}/{{ stepCount(task) }}</span>
                <span v-if="task.completedAt" class="done-date">✅ Completed {{ formatDate(task.completedAt) }}</span>
              </div>
            </div>

            <span class="open-hint">›</span>
          </div>
        </TransitionGroup>

        <div v-else class="empty-state">
          <p v-if="doneTasks.length === 0">No completed tasks yet. Tick one off!</p>
          <p v-else>No completed tasks match the filters.</p>
        </div>
      </template>
    </div>

    <!-- Πλαίσιο λεπτομερειών: ανοίγει όταν πατάς ένα task -->
    <Transition name="backdrop">
      <div v-if="selectedTask" class="drawer-backdrop" @click="closeDrawer"></div>
    </Transition>
    <Transition name="drawer">
      <aside v-if="selectedTask" class="task-drawer" :class="['priority-' + (selectedTask.priority || 'NONE').toLowerCase()]">
        <div class="drawer-header">
          <label class="checkbox-container" :title="selectedTask.done ? 'Move back to To Do' : 'Mark as completed'">
            <input type="checkbox" :checked="selectedTask.done || completing.has(selectedTask.id)" @change="toggleDoneFromDrawer($event)" />
            <span class="checkmark"></span>
          </label>
          <span class="drawer-status" :class="{ done: selectedTask.done || completing.has(selectedTask.id) }">
            {{ selectedTask.done || completing.has(selectedTask.id) ? '✅ Completed' : 'To Do' }}
          </span>
          <span class="save-state">{{ saveState }}</span>
          <button class="close-btn" title="Close (Esc)" @click="closeDrawer">✕</button>
        </div>

        <div class="drawer-body">
          <input
            v-model="draft.text"
            class="drawer-title"
            type="text"
            placeholder="Task title"
            maxlength="255"
            @blur="saveDraft"
            @keydown.enter.prevent="$event.target.blur()"
          />

          <label class="field-label" for="drawer-desc">Description</label>
          <textarea id="drawer-desc" v-model="draft.description" rows="4" placeholder="Add more details..." @blur="saveDraft"></textarea>

          <div class="field-grid">
            <div class="field">
              <label class="field-label" for="drawer-priority">Priority</label>
              <select id="drawer-priority" v-model="draft.priority" @change="saveDraft">
                <option value="">No priority</option>
                <option v-for="(p, key) in PRIORITIES" :key="key" :value="key">{{ p.label }}</option>
              </select>
            </div>
            <div class="field">
              <label class="field-label" for="drawer-category">Category</label>
              <input
                id="drawer-category"
                v-model="draft.category"
                type="text"
                list="categories"
                placeholder="None"
                maxlength="30"
                @blur="saveDraft"
                @keydown.enter.prevent="$event.target.blur()"
              />
            </div>
            <div class="field">
              <label class="field-label" for="drawer-due">Due date</label>
              <input id="drawer-due" v-model="draft.dueDate" type="date" @change="saveDraft" />
            </div>
          </div>
          <span v-if="selectedTask.dueDate && !selectedTask.done" class="task-date drawer-due" :class="dueStatus(selectedTask)">{{ dueLabel(selectedTask) }}</span>

          <!-- Βήματα (checklist) -->
          <div class="drawer-section">
            <div class="section-head">
              <span class="field-label">Steps</span>
              <span v-if="stepCount(selectedTask) > 0" class="steps-count" :class="{ complete: stepsDone(selectedTask) === stepCount(selectedTask) }">
                {{ stepsDone(selectedTask) }}/{{ stepCount(selectedTask) }} done
              </span>
            </div>
            <div v-if="stepCount(selectedTask) > 0" class="steps-bar" :class="{ complete: stepsDone(selectedTask) === stepCount(selectedTask) }">
              <span class="steps-fill" :style="{ width: stepsPercent(selectedTask) + '%' }"></span>
            </div>

            <div class="steps-list">
              <div v-for="step in selectedTask.subtasks" :key="step.id" class="step-row" :class="{ done: step.done }">
                <label class="checkbox-container step-check" title="Mark step as done">
                  <input type="checkbox" :checked="step.done" @change="toggleStep(selectedTask, step, $event)" />
                  <span class="checkmark"></span>
                </label>

                <input
                  v-if="editingStepId === step.id"
                  v-model="editingStepText"
                  class="step-edit-input"
                  maxlength="200"
                  @keydown.enter.prevent="saveStepText(selectedTask, step)"
                  @keydown.esc.stop="editingStepId = null"
                  @blur="saveStepText(selectedTask, step)"
                  v-focus
                />
                <span v-else class="step-text" title="Double-click to rename" @dblclick="startStepEdit(step)">{{ step.text }}</span>

                <button type="button" class="step-delete" title="Delete step" @click="deleteStep(selectedTask, step)">✕</button>
              </div>
              <p v-if="stepCount(selectedTask) === 0" class="steps-empty">Break this task into smaller steps.</p>
            </div>

            <form class="step-add" @submit.prevent="addStep(selectedTask)">
              <input v-model="newStepText[selectedTask.id]" type="text" placeholder="Add a step..." maxlength="200" />
              <button type="submit" class="small-btn">Add</button>
            </form>
          </div>

          <p class="drawer-meta">
            Added by <b>{{ selectedTask.author || currentUserId }}</b>
            <template v-if="selectedTask.createdAt"> on {{ formatDate(selectedTask.createdAt) }}</template>
            <template v-if="selectedTask.completedAt"> · Completed {{ formatDate(selectedTask.completedAt) }}</template>
          </p>
        </div>

        <div class="drawer-footer">
          <button class="danger-btn" @click="deleteTask(selectedTask)">🗑️ Delete task</button>
        </div>
      </aside>
    </Transition>

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
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useSettings } from '~/composables/useSettings'
import { PRIORITIES, priorityRank, formatDate, dueStatus, dueLabel, categoriesOf } from '~/composables/taskMeta'
import { burstConfetti, celebrateTask } from '~/composables/useConfetti'
import { useRouter } from 'vue-router'
import { getCurrentUser } from '~/composables/useAuth'

// Σελίδα με το πλαϊνό μενού (layouts/app.vue)
definePageMeta({ layout: 'app' })

const API = 'http://localhost:8080/api/tasks'
const UNDO_SECONDS = 5

const router = useRouter()
const settings = useSettings()

// Γρήγορη προσθήκη
const quickInput = ref(null)
const showMore = ref(false)
const newTaskTitle = ref('')
const newDescription = ref('')
const newDueDate = ref('')
const newPriority = ref('')
const newCategory = ref('')
const tasks = ref([])

const tab = ref('todo')
const filterPriority = ref('')
const filterCategory = ref('')

// Tasks που μόλις τσεκάραμε (μένουν λίγο στη λίστα για το animation)
const completing = ref(new Set())
const completeTimers = new Map()

// Πλαίσιο λεπτομερειών: ποιο task είναι ανοιχτό και τα πεδία που επεξεργάζεται ο χρήστης
const selectedId = ref(null)
const draft = reactive({ text: '', description: '', priority: '', category: '', dueDate: '' })
const saveState = ref('')
let saveStateTimer = null

// Αναίρεση
const undoTask = ref(null)
let undoTimer = null

// Βήματα (checklist): τι γράφει ο χρήστης στο "Add a step" κάθε task
const newStepText = reactive({})
const editingStepId = ref(null)
const editingStepText = ref('')
// Tasks με αλλαγή σε εξέλιξη: ο αυτόματος έλεγχος δεν τα αντικαθιστά μέχρι να απαντήσει ο server
const stepBusy = new Map()   // taskId -> πόσες αλλαγές περιμένουν απάντηση

// Εστιάζει αυτόματα στο πεδίο μετονομασίας βήματος
const vFocus = { mounted: (el) => el.focus() }

const currentUserId = ref('')
let tasksTimer = null

const categories = computed(() => categoriesOf(tasks.value))

const openTasks = computed(() => tasks.value.filter(t => !t.done))
const doneTasks = computed(() => tasks.value.filter(t => t.done))

const selectedTask = computed(() => tasks.value.find(t => t.id === selectedId.value) || null)

const matchesFilters = (task) =>
  (!filterPriority.value || task.priority === filterPriority.value) &&
  (!filterCategory.value || task.category === filterCategory.value)

const hasMeta = (task) => task.priority || task.category || task.dueDate || stepCount(task) > 0

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

  loadTasks()
  // Κάθε 5 δευτερόλεπτα ανανεώνουμε τα tasks (π.χ. αλλαγές από άλλη καρτέλα)
  tasksTimer = setInterval(() => {
    if (!document.hidden) loadTasks()
  }, 5000)

  window.addEventListener('keydown', onKeydown)
})

onUnmounted(() => {
  clearInterval(tasksTimer)
  clearTimeout(undoTimer)
  clearTimeout(saveStateTimer)
  window.removeEventListener('keydown', onKeydown)
})

// Esc κλείνει το πλαίσιο, το "n" πηγαίνει στη γρήγορη προσθήκη
const onKeydown = (event) => {
  if (event.key === 'Escape' && selectedId.value !== null) {
    closeDrawer()
    return
  }
  const typing = ['INPUT', 'TEXTAREA', 'SELECT'].includes(document.activeElement?.tagName)
  if (event.key === 'n' && !typing && !event.ctrlKey && !event.metaKey && !event.altKey) {
    event.preventDefault()
    quickInput.value?.focus()
  }
}

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
      if (old && (completing.value.has(task.id) || stepBusy.has(task.id))) return old
      return task
    })
    // Αν το ανοιχτό task σβήστηκε (π.χ. από άλλη καρτέλα), κλείνουμε το πλαίσιο
    if (selectedId.value !== null && !selectedTask.value) selectedId.value = null
  } catch (error) {
    console.error('❌ Error fetching tasks:', error)
  }
}

const addTask = async () => {
  if (newTaskTitle.value.trim() === '') return

  try {
    const payload = {
      text: newTaskTitle.value.trim(),
      description: newDescription.value,
      dueDate: newDueDate.value ? `${newDueDate.value}T00:00:00` : null,
      // Το όνομα από τα Settings, αλλιώς το username
      author: settings.value.defaultAuthor?.trim() || currentUserId.value,
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
    showMore.value = false
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
    if (selectedId.value === task.id) selectedId.value = null
  } catch (error) {
    console.error('❌ Error deleting task:', error)
  }
}

// ---------------- Πλαίσιο λεπτομερειών ----------------

const fillDraft = (task) => {
  draft.text = task.text || ''
  draft.description = task.description || ''
  draft.priority = task.priority || ''
  draft.category = task.category || ''
  draft.dueDate = formatDate(task.dueDate)
}

const openTask = async (task) => {
  if (selectedId.value === task.id) return
  if (selectedId.value !== null) await saveDraft()
  selectedId.value = task.id
  editingStepId.value = null
  saveState.value = ''
  fillDraft(task)
}

const closeDrawer = async () => {
  await saveDraft()
  selectedId.value = null
  editingStepId.value = null
}

const draftChanged = (task) =>
  draft.text.trim() !== (task.text || '') ||
  draft.description !== (task.description || '') ||
  draft.priority !== (task.priority || '') ||
  draft.category.trim() !== (task.category || '') ||
  draft.dueDate !== formatDate(task.dueDate)

// Αποθηκεύει αυτόματα τις αλλαγές του πλαισίου (όταν φεύγει ο κέρσορας από ένα πεδίο)
const saveDraft = async () => {
  const task = selectedTask.value
  if (!task || !draftChanged(task)) return
  if (!draft.text.trim()) {
    draft.text = task.text || ''   // ο τίτλος δεν μπορεί να μείνει κενός
    return
  }

  stepBusy.set(task.id, (stepBusy.get(task.id) || 0) + 1)
  try {
    const saved = await $fetch(`${API}/${task.id}`, {
      method: 'PUT',
      body: {
        text: draft.text.trim(),
        description: draft.description,
        dueDate: draft.dueDate ? `${draft.dueDate}T00:00:00` : null,
        priority: draft.priority || null,
        category: draft.category.trim() || null
      }
    })
    applySaved(saved)
    showSaveState('Saved ✓')
  } catch (error) {
    console.error('❌ Error editing task:', error)
    showSaveState('Not saved')
    alert(error?.data?.message || 'Σφάλμα κατά την αποθήκευση. Δες την κονσόλα (F12).')
  } finally {
    releaseBusy(task.id)
  }
}

const showSaveState = (text) => {
  clearTimeout(saveStateTimer)
  saveState.value = text
  saveStateTimer = setTimeout(() => { saveState.value = '' }, 2000)
}

const toggleDoneFromDrawer = async (event) => {
  const task = selectedTask.value
  if (!task) return
  await saveDraft()
  if (event.target.checked) await completeTask(task, event)
  else await restoreTask(task)
}

// ---------------- Βήματα (checklist) ----------------

const stepCount = (task) => task.subtasks?.length || 0
const stepsDone = (task) => (task.subtasks || []).filter(s => s.done).length
const stepsPercent = (task) => stepCount(task) ? Math.round(stepsDone(task) / stepCount(task) * 100) : 0

const releaseBusy = (taskId) => {
  const left = stepBusy.get(taskId) - 1
  if (left > 0) stepBusy.set(taskId, left)
  else stepBusy.delete(taskId)
}

// Τρέχει μια αλλαγή βήματος και ενημερώνει το task με την απάντηση του server
const stepRequest = async (task, request) => {
  stepBusy.set(task.id, (stepBusy.get(task.id) || 0) + 1)
  try {
    applySaved(await request())
  } catch (error) {
    console.error('❌ Error saving step:', error)
    alert(error?.data?.message || 'Σφάλμα κατά την αποθήκευση του βήματος.')
    loadTasks()
  } finally {
    releaseBusy(task.id)
  }
}

const addStep = async (task) => {
  const text = (newStepText[task.id] || '').trim()
  if (!text) return
  newStepText[task.id] = ''
  await stepRequest(task, () => $fetch(`${API}/${task.id}/subtasks`, { method: 'POST', body: { text } }))
}

const toggleStep = async (task, step, event) => {
  const done = event.target.checked
  step.done = done   // άμεση αλλαγή στην οθόνη
  // όταν τσεκάρεται και το τελευταίο βήμα, λίγο κομφετί
  if (done && stepsDone(task) === stepCount(task)) burstConfetti(event.target, 14)
  await stepRequest(task, () => $fetch(`${API}/subtasks/${step.id}`, { method: 'PUT', body: { done } }))
}

const startStepEdit = (step) => {
  editingStepId.value = step.id
  editingStepText.value = step.text
}

const saveStepText = async (task, step) => {
  if (editingStepId.value !== step.id) return   // ήδη αποθηκεύτηκε (Enter και μετά blur)
  editingStepId.value = null
  const text = editingStepText.value.trim()
  if (!text || text === step.text) return
  step.text = text
  await stepRequest(task, () => $fetch(`${API}/subtasks/${step.id}`, { method: 'PUT', body: { text } }))
}

const deleteStep = async (task, step) => {
  task.subtasks = task.subtasks.filter(s => s.id !== step.id)
  await stepRequest(task, () => $fetch(`${API}/subtasks/${step.id}`, { method: 'DELETE' }))
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
}

/* ===== Board ===== */
.board-card {
  position: relative;
  z-index: 1;
  background: rgba(28, 25, 23, 0.65);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(var(--accent-rgb), 0.2);
  padding: 2rem 2.2rem;
  border-radius: 20px;
  flex: 1;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.5), 0 0 20px rgba(var(--accent-rgb), 0.1);
  min-height: 0;
}

.board-header {
  margin-bottom: 1.2rem;
  border-bottom: 1px solid rgba(var(--accent-rgb), 0.2);
  padding-bottom: 1rem;
  flex-shrink: 0;
}

.header-top {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 10px;
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

.week-stats {
  color: #a8a29e;
  font-size: 0.85rem;
}

.week-stats b {
  color: var(--accent);
}

/* ===== Γρήγορη προσθήκη ===== */
.quick-add {
  margin-top: 1.2rem;
  background: rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(var(--accent-rgb), 0.3);
  border-radius: 14px;
  padding: 0.4rem 0.4rem 0.4rem 1rem;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.quick-add:focus-within {
  border-color: var(--accent);
  box-shadow: 0 0 15px rgba(var(--accent-rgb), 0.25);
}

.quick-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.quick-plus {
  color: var(--accent);
  font-size: 1.2rem;
  font-weight: 700;
}

.quick-input {
  flex: 1;
  min-width: 0;
  background: transparent;
  border: none;
  color: white;
  font-size: 1rem;
  font-family: inherit;
  padding: 0.55rem 0.2rem;
}

.quick-input:focus {
  outline: none;
}

.more-btn {
  background: transparent;
  color: #a8a29e;
  border: 1px solid transparent;
  border-radius: 8px;
  width: 38px;
  height: 36px;
  font-size: 1.2rem;
  line-height: 1;
  cursor: pointer;
  transition: all 0.2s;
}

.more-btn:hover, .more-btn.active {
  color: var(--accent);
  border-color: rgba(var(--accent-rgb), 0.5);
  background: rgba(var(--accent-rgb), 0.1);
}

.add-btn {
  background: var(--accent);
  color: #1c1917;
  border: none;
  border-radius: 10px;
  padding: 0.55rem 1.3rem;
  font-size: 0.95rem;
  font-weight: 700;
  cursor: pointer;
  font-family: 'Poppins', sans-serif;
  transition: all 0.2s;
}

.add-btn:hover:not(:disabled) {
  box-shadow: 0 0 18px rgba(var(--accent-rgb), 0.6);
}

.add-btn:disabled {
  opacity: 0.35;
  cursor: default;
}

.quick-more {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 0.6rem 0.6rem 0.5rem 0;
}

.form-row {
  display: flex;
  gap: 8px;
}

.quick-more textarea, .quick-more input, .quick-more select {
  background-color: rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(var(--accent-rgb), 0.3);
  border-radius: 10px;
  padding: 0.6rem 0.7rem;
  color: white;
  font-size: 0.9rem;
  font-family: inherit;
  box-sizing: border-box;
  min-width: 0;
  color-scheme: dark;
}

.quick-more textarea {
  resize: vertical;
}

.quick-more .form-row > * {
  flex: 1;
}

.quick-more select {
  padding-right: 2.2rem;
  cursor: pointer;
}

.quick-more :is(textarea, input, select):focus {
  outline: none;
  border-color: var(--accent);
}

/* ===== Καρτέλες, φίλτρα ===== */
.toolbar {
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

.filters {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.filters select, .drawer-body select {
  background-color: rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(var(--accent-rgb), 0.3);
  border-radius: 10px;
  color: white;
  font-size: 0.85rem;
  font-family: inherit;
  cursor: pointer;
  color-scheme: dark;
  padding: 0.4rem 2.2rem 0.4rem 0.7rem;
}

.filters select:focus {
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

/* ===== Λίστα tasks ===== */
.tasks-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
  overflow-y: auto;
  padding-right: 12px;
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
  padding: 0.9rem 1.1rem;
  border-radius: 12px;
  border: 1px solid rgba(var(--accent-rgb), 0.15);
  cursor: pointer;
  transition: all 0.25s;
}

.task-item:hover {
  background: rgba(0, 0, 0, 0.4);
  border-color: rgba(var(--accent-rgb), 0.4);
  transform: translateX(4px);
}

.task-item.selected {
  border-color: var(--accent);
  background: rgba(var(--accent-rgb), 0.08);
  box-shadow: 0 0 14px rgba(var(--accent-rgb), 0.2);
}

.task-content {
  flex: 1;
  min-width: 0;
  margin-left: 14px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.task-title {
  color: white;
  font-size: 1.05rem;
  font-weight: 500;
  transition: all 0.3s;
}

/* μία γραμμή περιγραφής στη λίστα, ολόκληρη στο πλαίσιο */
.task-desc {
  margin: 0;
  color: #a8a29e;
  font-size: 0.85rem;
  line-height: 1.4;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  transition: all 0.3s;
}

.open-hint {
  color: #57534e;
  font-size: 1.4rem;
  line-height: 1;
  margin-left: 10px;
  align-self: center;
  transition: all 0.2s;
}

.task-item:hover .open-hint, .task-item.selected .open-hint {
  color: var(--accent);
  transform: translateX(3px);
}

.task-item.completed .task-title, .task-item.completed .task-desc {
  text-decoration: line-through;
  color: #57534e;
}

.task-item.priority-high { border-left: 3px solid #ef4444; }
.task-item.priority-medium { border-left: 3px solid #fbbf24; }
.task-item.priority-low { border-left: 3px solid #4ade80; }

.task-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  margin-top: 3px;
}

.priority-pill, .category-tag, .done-date, .task-date, .steps-pill {
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

.task-date {
  display: inline-block;
  color: #fbbf24;
  background: rgba(251, 191, 36, 0.1);
  border: 1px solid rgba(251, 191, 36, 0.3);
  align-self: flex-start;
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

.steps-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #d6d3d1;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.15);
}

.steps-pill.complete {
  color: #4ade80;
  border-color: rgba(74, 222, 128, 0.4);
}

.mini-bar {
  width: 40px;
  height: 4px;
  border-radius: 2px;
  background: rgba(255, 255, 255, 0.12);
  overflow: hidden;
}

.mini-fill {
  display: block;
  height: 100%;
  background: var(--accent);
  transition: width 0.3s;
}

.steps-pill.complete .mini-fill {
  background: #4ade80;
}

.task-item.completed :is(.priority-pill, .category-tag, .steps-pill) {
  opacity: 0.5;
}

/* ===== Checkbox ===== */
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
  background-color: transparent;
  border: 2px solid var(--accent);
  border-radius: 6px;
  box-sizing: border-box;
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

/* ===== Πλαίσιο λεπτομερειών ===== */
.drawer-backdrop {
  position: fixed;
  inset: 0;
  z-index: 29;
  background: rgba(0, 0, 0, 0.35);
}

.task-drawer {
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  z-index: 30;
  width: min(460px, 92vw);
  display: flex;
  flex-direction: column;
  background: rgba(28, 25, 23, 0.97);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-left: 1px solid rgba(var(--accent-rgb), 0.35);
  box-shadow: -20px 0 50px rgba(0, 0, 0, 0.5);
  font-family: 'Poppins', sans-serif;
}

.task-drawer.priority-high { border-top: 4px solid #ef4444; }
.task-drawer.priority-medium { border-top: 4px solid #fbbf24; }
.task-drawer.priority-low { border-top: 4px solid #4ade80; }

.drawer-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 1.1rem 1.4rem;
  border-bottom: 1px solid rgba(var(--accent-rgb), 0.2);
}

.drawer-header .checkbox-container {
  margin-top: 0;
}

.drawer-status {
  color: #a8a29e;
  font-size: 0.9rem;
  font-weight: 600;
}

.drawer-status.done {
  color: #4ade80;
}

.save-state {
  margin-left: auto;
  color: #4ade80;
  font-size: 0.8rem;
}

.close-btn {
  background: transparent;
  border: 1px solid rgba(var(--accent-rgb), 0.3);
  color: #d6d3d1;
  border-radius: 8px;
  width: 34px;
  height: 34px;
  cursor: pointer;
  font-size: 0.9rem;
  transition: all 0.2s;
}

.save-state:empty + .close-btn {
  margin-left: auto;
}

.close-btn:hover {
  border-color: var(--accent);
  color: var(--accent);
}

.drawer-body {
  flex: 1;
  overflow-y: auto;
  padding: 1.3rem 1.4rem;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.drawer-title {
  background: transparent;
  border: 1px solid transparent;
  border-radius: 10px;
  padding: 0.4rem 0.5rem;
  margin: 0 -0.5rem 0.4rem;
  color: #ffffff;
  font-size: 1.45rem;
  font-weight: 700;
  font-family: inherit;
}

.drawer-title:hover {
  border-color: rgba(var(--accent-rgb), 0.25);
}

.drawer-title:focus {
  outline: none;
  border-color: var(--accent);
  background: rgba(0, 0, 0, 0.3);
}

.field-label {
  color: #a8a29e;
  font-size: 0.75rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.8px;
}

.drawer-body textarea, .drawer-body .field input {
  background: rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(var(--accent-rgb), 0.25);
  border-radius: 10px;
  padding: 0.6rem 0.7rem;
  color: white;
  font-size: 0.9rem;
  font-family: inherit;
  box-sizing: border-box;
  width: 100%;
  color-scheme: dark;
}

.drawer-body textarea {
  resize: vertical;
  line-height: 1.5;
}

.drawer-body :is(textarea, input, select):focus {
  outline: none;
  border-color: var(--accent);
}

.field-grid {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 10px;
  margin-top: 6px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 5px;
  min-width: 0;
}

.drawer-body .field select {
  width: 100%;
  padding: 0.6rem 2rem 0.6rem 0.6rem;
  font-size: 0.85rem;
}

.drawer-due {
  margin-top: 2px;
}

.drawer-section {
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid rgba(var(--accent-rgb), 0.15);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.steps-count {
  color: #a8a29e;
  font-size: 0.8rem;
  font-weight: 600;
}

.steps-count.complete {
  color: #4ade80;
}

.steps-bar {
  height: 6px;
  border-radius: 3px;
  background: rgba(255, 255, 255, 0.1);
  overflow: hidden;
}

.steps-fill {
  display: block;
  height: 100%;
  background: var(--accent);
  box-shadow: 0 0 8px rgba(var(--accent-rgb), 0.6);
  transition: width 0.3s ease;
}

.steps-bar.complete .steps-fill {
  background: #4ade80;
  box-shadow: 0 0 8px rgba(74, 222, 128, 0.6);
}

.steps-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.steps-empty {
  margin: 0;
  color: #78716c;
  font-size: 0.85rem;
  font-style: italic;
}

.step-row {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 32px;
  padding: 0 4px;
  border-radius: 8px;
}

.step-row:hover {
  background: rgba(255, 255, 255, 0.04);
}

.checkbox-container.step-check {
  width: 18px;
  height: 18px;
  margin-top: 0;
}

.step-check .checkmark {
  width: 18px;
  height: 18px;
  border-radius: 5px;
}

.checkbox-container.step-check .checkmark:after {
  left: 5px;
  top: 1px;
  width: 4px;
  height: 9px;
}

.step-text {
  flex: 1;
  color: #e7e5e4;
  font-size: 0.92rem;
  cursor: text;
  word-break: break-word;
  transition: color 0.2s;
}

.step-row.done .step-text {
  text-decoration: line-through;
  color: #78716c;
}

.step-edit-input {
  flex: 1;
  min-width: 0;
  background: rgba(0, 0, 0, 0.3);
  border: 1px solid var(--accent);
  border-radius: 6px;
  padding: 0.25rem 0.5rem;
  color: white;
  font-size: 0.92rem;
  font-family: inherit;
}

.step-edit-input:focus {
  outline: none;
}

.step-delete {
  background: transparent;
  border: none;
  color: #78716c;
  cursor: pointer;
  font-size: 0.8rem;
  padding: 2px 6px;
  border-radius: 4px;
  opacity: 0;
  transition: all 0.2s;
}

.step-row:hover .step-delete {
  opacity: 1;
}

.step-delete:hover {
  color: #f87171;
  background: rgba(239, 68, 68, 0.12);
}

.step-add {
  display: flex;
  gap: 8px;
  margin-top: 2px;
}

.step-add input {
  flex: 1;
  min-width: 0;
  background: rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(var(--accent-rgb), 0.25);
  border-radius: 8px;
  padding: 0.45rem 0.7rem;
  color: white;
  font-size: 0.88rem;
  font-family: inherit;
}

.step-add input:focus {
  outline: none;
  border-color: var(--accent);
}

.small-btn {
  background: transparent;
  color: var(--accent);
  border: 2px solid var(--accent);
  border-radius: 10px;
  padding: 0.3rem 1rem;
  font-size: 0.85rem;
  font-weight: bold;
  cursor: pointer;
  font-family: 'Poppins', sans-serif;
  transition: all 0.3s;
}

.small-btn:hover {
  background: var(--accent);
  color: #1c1917;
}

.drawer-meta {
  margin: 1.2rem 0 0;
  color: #78716c;
  font-size: 0.8rem;
}

.drawer-meta b {
  color: #a8a29e;
}

.drawer-footer {
  padding: 1rem 1.4rem;
  border-top: 1px solid rgba(var(--accent-rgb), 0.2);
}

.danger-btn {
  background: transparent;
  color: #f87171;
  border: 1px solid rgba(239, 68, 68, 0.5);
  border-radius: 10px;
  padding: 0.5rem 1rem;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  font-family: 'Poppins', sans-serif;
  transition: all 0.2s;
}

.danger-btn:hover {
  background: rgba(239, 68, 68, 0.15);
  border-color: #ef4444;
}

.drawer-enter-active, .drawer-leave-active {
  transition: transform 0.3s ease;
}

.drawer-enter-from, .drawer-leave-to {
  transform: translateX(100%);
}

.backdrop-enter-active, .backdrop-leave-active {
  transition: opacity 0.3s ease;
}

.backdrop-enter-from, .backdrop-leave-to {
  opacity: 0;
}

/* ===== Αναίρεση ===== */
.undo-toast {
  position: fixed;
  bottom: 2rem;
  left: 50%;
  transform: translateX(-50%);
  z-index: 40;
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

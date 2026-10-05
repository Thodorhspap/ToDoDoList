// Κοινά βοηθητικά για τα tasks: προτεραιότητες, ημερομηνίες λήξης, κατηγορίες

export const PRIORITIES = {
  HIGH: { label: 'High', icon: '🔴', rank: 0 },
  MEDIUM: { label: 'Medium', icon: '🟡', rank: 1 },
  LOW: { label: 'Low', icon: '🟢', rank: 2 }
}

export const priorityRank = (priority) => PRIORITIES[priority]?.rank ?? 3

// "2026-10-05T00:00:00" -> "2026-10-05"
export const formatDate = (dateString) => {
  if (!dateString) return ''
  return dateString.includes('T') ? dateString.split('T')[0] : dateString
}

const todayString = () => {
  const now = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`
}

// 'overdue' αν πέρασε η ημερομηνία, 'today' αν λήγει σήμερα, αλλιώς null
export const dueStatus = (task) => {
  if (!task.dueDate || task.done) return null
  const due = formatDate(task.dueDate)
  const today = todayString()
  if (due < today) return 'overdue'
  if (due === today) return 'today'
  return null
}

export const dueLabel = (task) => {
  const status = dueStatus(task)
  if (status === 'overdue') return `⚠️ Overdue: ${formatDate(task.dueDate)}`
  if (status === 'today') return '⏰ Due today'
  return `📅 Due: ${formatDate(task.dueDate)}`
}

// Μοναδικές κατηγορίες από μια λίστα tasks, αλφαβητικά
export const categoriesOf = (tasks) =>
  [...new Set(tasks.map(t => t.category).filter(Boolean))].sort((a, b) => a.localeCompare(b))

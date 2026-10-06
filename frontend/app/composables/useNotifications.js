import { ref } from 'vue'
import { getCurrentUser } from '~/composables/useAuth'

// Ειδοποιήσεις για τα badges: νέα tasks, αιτήματα φιλίας και αδιάβαστα μηνύματα

const EMPTY = () => ({
  newTasks: 0,
  completedTasks: 0,
  communityCompleted: 0,
  friendRequests: 0,
  unreadMessages: 0,
  unreadByFriend: {}
})

// Κοινή κατάσταση για όλες τις σελίδες (γεμίζει μόνο στον browser)
const notifications = ref(EMPTY())

export const useNotifications = () => notifications

// Φέρνει τις ειδοποιήσεις από τον server (αν δεν είμαστε συνδεδεμένοι, τις μηδενίζει)
export const refreshNotifications = async () => {
  const username = getCurrentUser()
  if (!username) {
    notifications.value = EMPTY()
    return
  }

  try {
    notifications.value = await $fetch(`http://localhost:8080/api/notifications/${encodeURIComponent(username)}`)
  } catch (error) {
    console.error('❌ Error fetching notifications:', error)
  }
}

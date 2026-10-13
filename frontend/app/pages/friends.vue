<template>
  <div class="page-container">

    <div class="board-container">
      <!-- ΑΡΙΣΤΕΡΗ ΣΤΗΛΗ -->
      <div class="left-sidebar">
        <!-- Προσθήκη φίλου -->
        <div class="widget">
          <h3 class="widget-title">Add Friend</h3>
          <form class="add-form" @submit.prevent="sendRequest">
            <input
              v-model="newFriend"
              type="text"
              list="friend-suggestions"
              placeholder="Username"
              autocomplete="off"
              required
            />
            <datalist id="friend-suggestions">
              <option v-for="name in suggestions" :key="name" :value="name" />
            </datalist>
            <button type="submit" class="small-btn" :disabled="requesting">Add</button>
          </form>
          <p v-if="addMessage" class="form-msg" :class="{ error: addError }">{{ addMessage }}</p>
        </div>

        <!-- Αιτήματα φιλίας -->
        <div v-if="incoming.length || outgoing.length" class="widget">
          <h3 class="widget-title">Requests <span v-if="incoming.length" class="count alert">{{ incoming.length }}</span></h3>
          <div class="people-list">
            <div v-for="request in incoming" :key="request.id" class="person-row">
              <span class="person-name">{{ request.username }}</span>
              <div class="row-actions">
                <button class="icon-btn accept" title="Accept" @click="acceptRequest(request.id)">✓</button>
                <button class="icon-btn danger" title="Decline" @click="deleteRequest(request.id)">✕</button>
              </div>
            </div>
            <div v-for="request in outgoing" :key="request.id" class="person-row">
              <span class="person-name muted">{{ request.username }} <small>(pending)</small></span>
              <button class="text-btn" @click="deleteRequest(request.id)">Cancel</button>
            </div>
          </div>
        </div>

        <!-- Λίστα φίλων -->
        <div class="widget friends-widget">
          <div class="widget-head">
            <h3 class="widget-title">Friends <span class="count">{{ friends.length }}</span></h3>
            <button v-if="friends.length" class="text-btn" @click="editing = !editing">
              {{ editing ? 'Done' : 'Edit' }}
            </button>
          </div>

          <div v-if="friends.length" class="people-list scroll">
            <div
              v-for="name in friends"
              :key="name"
              class="person-row friend"
              :class="{ active: name === selectedFriend }"
              @click="openChat(name)"
            >
              <span class="person-name">
                <span class="avatar">{{ name.charAt(0).toUpperCase() }}</span>
                {{ name }}
              </span>
              <button v-if="editing" class="text-btn danger-text" @click.stop="removeFriend(name)">Remove</button>
              <span v-else-if="unreadFrom(name)" class="unread">{{ unreadFrom(name) }}</span>
            </div>
          </div>
          <p v-else class="empty-small">No friends yet. Add someone above!</p>
        </div>
      </div>

      <!-- ΔΕΞΙΟ BOARD: συνομιλία -->
      <div class="board-card">
        <template v-if="selectedFriend">
          <div class="board-header chat-header">
            <div>
              <h1 class="title">{{ selectedFriend }}</h1>
              <p class="subtitle">Chat with your friend</p>
            </div>
            <button class="bar-btn" @click="router.push({ path: '/send', query: { to: selectedFriend } })">
              📨 Send Task
            </button>
          </div>

          <div ref="messagesBox" class="messages">
            <div v-if="messages.length === 0" class="empty-state">
              <p>No messages yet. Say hi! 👋</p>
            </div>
            <div
              v-for="message in messages"
              :key="message.id"
              class="bubble-row"
              :class="{ mine: message.sender === currentUser }"
            >
              <div class="bubble">
                <p class="bubble-text">{{ message.text }}</p>
                <span class="bubble-time">{{ formatTime(message.createdAt) }}</span>
              </div>
              <!-- Read receipt κάτω από το τελευταίο δικό μας μήνυμα -->
              <span v-if="message.id === lastMineId" class="receipt" :class="{ seen: message.id <= lastSeenId }">
                {{ message.id <= lastSeenId ? 'Seen ✓✓' : 'Sent ✓' }}
              </span>
            </div>
          </div>

          <form class="chat-form" @submit.prevent="sendMessage">
            <input
              v-model="newMessage"
              type="text"
              placeholder="Type a message..."
              maxlength="1000"
              autocomplete="off"
            />
            <button type="submit" class="small-btn" :disabled="!newMessage.trim()">Send</button>
          </form>
          <p v-if="chatError" class="form-msg error">{{ chatError }}</p>
        </template>

        <div v-else class="empty-state">
          <p>Pick a friend on the left to start chatting.</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
// Σελίδα με το πλαϊνό μενού (layouts/app.vue)
definePageMeta({ layout: 'app' })

import { ref, computed, nextTick, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { refreshNotifications, useNotifications } from '~/composables/useNotifications'
import { getCurrentUser } from '~/composables/useAuth'

const API = 'http://localhost:8080/api/friends'
const router = useRouter()
const notifications = useNotifications()

const currentUser = ref('')
const allUsers = ref([])
const friends = ref([])
const incoming = ref([])
const outgoing = ref([])

const newFriend = ref('')
const addMessage = ref('')
const addError = ref(false)
const requesting = ref(false)
const editing = ref(false)

const selectedFriend = ref('')
const messages = ref([])
const newMessage = ref('')
const chatError = ref('')
const messagesBox = ref(null)
// Μέχρι ποιο δικό μας μήνυμα έχει διαβάσει ο φίλος
const lastSeenId = ref(0)

const lastMineId = computed(() => {
  for (let i = messages.value.length - 1; i >= 0; i--) {
    if (messages.value[i].sender === currentUser.value) return messages.value[i].id
  }
  return null
})

let friendsTimer = null
let messagesTimer = null

// Προτάσεις: όλοι οι χρήστες εκτός από εμάς και όσους είναι ήδη φίλοι ή σε αναμονή
const suggestions = computed(() => {
  const taken = new Set([
    currentUser.value,
    ...friends.value,
    ...incoming.value.map(r => r.username),
    ...outgoing.value.map(r => r.username)
  ])
  return allUsers.value.filter(name => !taken.has(name))
})

// Αδιάβαστα μηνύματα από έναν φίλο (όχι για τη συνομιλία που είναι ήδη ανοιχτή)
const unreadFrom = (name) =>
  name === selectedFriend.value ? 0 : (notifications.value.unreadByFriend?.[name] || 0)

const userPath = () => encodeURIComponent(currentUser.value)

const formatTime = (dateString) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  const isToday = date.toDateString() === new Date().toDateString()
  const time = date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
  return isToday ? time : `${date.toLocaleDateString()} ${time}`
}

onMounted(async () => {
  const savedUser = getCurrentUser()
  if (!savedUser) {
    // Χωρίς σύνδεση δεν βλέπει κανείς φίλους
    router.push('/login')
    return
  }
  currentUser.value = savedUser

  loadFriends()
  // Νέα αιτήματα/φίλοι κάθε 5 δευτερόλεπτα, νέα μηνύματα κάθε 3
  friendsTimer = setInterval(() => {
    if (!document.hidden) loadFriends()
  }, 5000)
  messagesTimer = setInterval(() => {
    if (!document.hidden && selectedFriend.value) loadMessages()
  }, 3000)

  try {
    allUsers.value = await $fetch('http://localhost:8080/api/auth/users')
  } catch (error) {
    console.error('❌ Error fetching users:', error)
  }
})

onUnmounted(() => {
  clearInterval(friendsTimer)
  clearInterval(messagesTimer)
})

const loadFriends = async () => {
  try {
    const data = await $fetch(`${API}/${userPath()}`)
    friends.value = data.friends
    incoming.value = data.incoming
    outgoing.value = data.outgoing

    // Αν ο φίλος της ανοιχτής συνομιλίας μας αφαίρεσε, κλείνουμε τη συνομιλία
    if (selectedFriend.value && !friends.value.includes(selectedFriend.value)) {
      selectedFriend.value = ''
      messages.value = []
    }
    if (friends.value.length === 0) editing.value = false
  } catch (error) {
    console.error('❌ Error fetching friends:', error)
  }
}

const sendRequest = async () => {
  const to = newFriend.value.trim()
  if (!to) return

  addMessage.value = ''
  requesting.value = true
  try {
    const response = await $fetch(`${API}/request`, {
      method: 'POST',
      body: { from: currentUser.value, to }
    })
    newFriend.value = ''
    addError.value = false
    addMessage.value = response.message
    loadFriends()
  } catch (error) {
    addError.value = true
    addMessage.value = error?.data?.message || 'Could not send the request. Is the server running?'
  } finally {
    requesting.value = false
  }
}

const acceptRequest = async (id) => {
  try {
    await $fetch(`${API}/requests/${id}/accept`, {
      method: 'POST',
      body: { username: currentUser.value }
    })
    loadFriends()
    refreshNotifications()
  } catch (error) {
    console.error('❌ Error accepting request:', error)
  }
}

// Απόρριψη αιτήματος που λάβαμε ή ακύρωση αιτήματος που στείλαμε
const deleteRequest = async (id) => {
  try {
    await $fetch(`${API}/requests/${id}`, {
      method: 'DELETE',
      query: { username: currentUser.value }
    })
    loadFriends()
    refreshNotifications()
  } catch (error) {
    console.error('❌ Error removing request:', error)
  }
}

const removeFriend = async (name) => {
  if (!confirm(`Remove ${name} from your friends?`)) return
  try {
    await $fetch(`${API}/${userPath()}/${encodeURIComponent(name)}`, { method: 'DELETE' })
    loadFriends()
  } catch (error) {
    console.error('❌ Error removing friend:', error)
  }
}

const openChat = (name) => {
  if (editing.value || name === selectedFriend.value) return
  selectedFriend.value = name
  messages.value = []
  lastSeenId.value = 0
  newMessage.value = ''
  chatError.value = ''
  loadMessages()
}

const scrollToBottom = async () => {
  await nextTick()
  if (messagesBox.value) messagesBox.value.scrollTop = messagesBox.value.scrollHeight
}

// Προσθέτει μόνο μηνύματα που δεν έχουμε ήδη (για να μη διπλασιάζονται)
const appendMessages = (newOnes) => {
  const known = new Set(messages.value.map(m => m.id))
  const fresh = newOnes.filter(m => !known.has(m.id))
  if (fresh.length === 0) return
  messages.value = [...messages.value, ...fresh].sort((a, b) => a.id - b.id)
  scrollToBottom()
}

const loadMessages = async () => {
  const friend = selectedFriend.value
  const lastId = messages.value.length ? messages.value[messages.value.length - 1].id : 0
  try {
    const data = await $fetch(`${API}/${userPath()}/messages/${encodeURIComponent(friend)}`, {
      query: { afterId: lastId }
    })
    // Αν στο μεταξύ άλλαξε η συνομιλία, αγνοούμε την απάντηση
    if (friend === selectedFriend.value && Array.isArray(data.messages)) {
      appendMessages(data.messages)
      lastSeenId.value = data.lastSeenId || 0
      // Ο server τα σημείωσε ως διαβασμένα, οπότε ενημερώνουμε τα badges
      if (data.messages.some(m => m.sender === friend)) refreshNotifications()
    }
  } catch (error) {
    console.error('❌ Error fetching messages:', error)
  }
}

const sendMessage = async () => {
  const text = newMessage.value.trim()
  const friend = selectedFriend.value
  if (!text || !friend) return

  chatError.value = ''
  newMessage.value = ''
  try {
    const saved = await $fetch(`${API}/${userPath()}/messages/${encodeURIComponent(friend)}`, {
      method: 'POST',
      body: { text }
    })
    if (friend === selectedFriend.value) appendMessages([saved])
  } catch (error) {
    newMessage.value = text
    chatError.value = error?.data?.message || 'Could not send the message. Is the server running?'
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
  flex-shrink: 0;
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

.widget {
  position: relative;
  background: rgba(28, 25, 23, 0.65);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(var(--accent-rgb), 0.2);
  padding: 1.5rem;
  border-radius: 16px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3);
  flex-shrink: 0;
}

/* Η λίστα φίλων παίρνει τον υπόλοιπο χώρο και κάνει scroll */
.friends-widget {
  flex: 1;
  flex-shrink: 1;
  display: flex;
  flex-direction: column;
  min-height: 120px;
}

.widget-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.widget-title {
  margin: 0 0 1rem 0;
  color: #ffffff;
  font-size: 1.2rem;
  font-weight: 600;
}

.widget-head .widget-title {
  margin: 0;
}

.count {
  font-size: 0.8rem;
  color: var(--accent);
  background: rgba(var(--accent-rgb), 0.15);
  border-radius: 10px;
  padding: 1px 8px;
  margin-left: 4px;
  vertical-align: middle;
}

.count.alert {
  color: #ffffff;
  background: #ef4444;
}

/* Αδιάβαστα μηνύματα δίπλα στο όνομα του φίλου */
.unread {
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  box-sizing: border-box;
  border-radius: 10px;
  background: #ef4444;
  color: #ffffff;
  font-size: 0.7rem;
  font-weight: 700;
  line-height: 20px;
  text-align: center;
  box-shadow: 0 0 10px rgba(239, 68, 68, 0.6);
  flex-shrink: 0;
}

.add-form, .chat-form {
  display: flex;
  gap: 10px;
}

.add-form input, .chat-form input {
  flex: 1;
  min-width: 0;
  background: rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(var(--accent-rgb), 0.3);
  border-radius: 10px;
  padding: 0.8rem;
  color: white;
  font-size: 0.95rem;
  transition: all 0.3s;
  box-sizing: border-box;
  font-family: inherit;
}

.add-form input:focus, .chat-form input:focus {
  outline: none;
  border-color: var(--accent);
  background: rgba(0, 0, 0, 0.5);
  box-shadow: 0 0 15px rgba(var(--accent-rgb), 0.3);
}

.small-btn {
  background: transparent;
  color: var(--accent);
  border: 2px solid var(--accent);
  border-radius: 10px;
  padding: 0 1.2rem;
  font-size: 0.95rem;
  cursor: pointer;
  font-weight: bold;
  transition: all 0.3s;
  font-family: 'Poppins', sans-serif;
}

.small-btn:hover:not(:disabled) {
  background: var(--accent);
  color: #1c1917;
  box-shadow: 0 0 20px rgba(var(--accent-rgb), 0.7);
}

.small-btn:disabled {
  opacity: 0.5;
  cursor: default;
}

.form-msg {
  margin: 0.6rem 0 0;
  color: #4ade80;
  font-size: 0.85rem;
}

.form-msg.error {
  color: #f87171;
}

/* Λίστες ατόμων */
.people-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.people-list.scroll {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
  padding-right: 6px;
}

.people-list.scroll::-webkit-scrollbar {
  width: 6px;
}

.people-list.scroll::-webkit-scrollbar-thumb {
  background: rgba(var(--accent-rgb), 0.4);
  border-radius: 3px;
}

.person-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  background: rgba(0, 0, 0, 0.25);
  border: 1px solid rgba(var(--accent-rgb), 0.15);
  border-radius: 10px;
  padding: 0.6rem 0.8rem;
  transition: all 0.2s;
}

.person-row.friend {
  cursor: pointer;
}

.person-row.friend:hover {
  border-color: rgba(var(--accent-rgb), 0.4);
  background: rgba(0, 0, 0, 0.4);
}

.person-row.active {
  border-color: var(--accent);
  background: rgba(var(--accent-rgb), 0.12);
}

.person-name {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #ffffff;
  font-size: 0.95rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.person-name.muted {
  color: #a8a29e;
}

.avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: rgba(var(--accent-rgb), 0.2);
  border: 1px solid rgba(var(--accent-rgb), 0.5);
  color: var(--accent);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.85rem;
  font-weight: 600;
  flex-shrink: 0;
}

.row-actions {
  display: flex;
  gap: 6px;
}

.icon-btn {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  background: transparent;
  cursor: pointer;
  font-weight: bold;
  transition: all 0.2s;
}

.icon-btn.accept {
  color: #4ade80;
  border: 1px solid rgba(74, 222, 128, 0.5);
}

.icon-btn.accept:hover {
  background: #4ade80;
  color: #1c1917;
}

.icon-btn.danger {
  color: #f87171;
  border: 1px solid rgba(248, 113, 113, 0.5);
}

.icon-btn.danger:hover {
  background: #f87171;
  color: #1c1917;
}

.text-btn {
  background: transparent;
  border: none;
  color: #a8a29e;
  font-size: 0.85rem;
  font-family: 'Poppins', sans-serif;
  cursor: pointer;
  padding: 0.2rem 0.4rem;
  transition: color 0.2s;
  flex-shrink: 0;
}

.text-btn:hover {
  color: var(--accent);
  text-decoration: underline;
}

.danger-text {
  color: #f87171;
}

.danger-text:hover {
  color: #f87171;
}

.empty-small {
  margin: 0;
  color: #a8a29e;
  font-style: italic;
  font-size: 0.9rem;
}

/* Δεξί Board: συνομιλία */
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
  min-width: 0;
}

.board-header {
  margin-bottom: 1.5rem;
  border-bottom: 1px solid rgba(var(--accent-rgb), 0.2);
  padding-bottom: 1rem;
  flex-shrink: 0;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
}

.title {
  margin: 0;
  font-size: 2.2rem;
  color: #ffffff;
  font-weight: 800;
  text-shadow: 0 0 10px rgba(var(--accent-rgb), 0.5);
}

.subtitle {
  margin: 0.5rem 0 0;
  color: #a8a29e;
}

.messages {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-right: 10px;
  margin-bottom: 1rem;
}

.messages::-webkit-scrollbar {
  width: 8px;
}

.messages::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.2);
  border-radius: 4px;
}

.messages::-webkit-scrollbar-thumb {
  background: rgba(var(--accent-rgb), 0.4);
  border-radius: 4px;
}

.bubble-row {
  display: flex;
  justify-content: flex-start;
}

.bubble-row.mine {
  justify-content: flex-end;
}

.bubble-row {
  flex-wrap: wrap;
}

.receipt {
  flex-basis: 100%;
  text-align: right;
  margin-top: 2px;
  font-size: 0.7rem;
  color: #a8a29e;
}

.receipt.seen {
  color: var(--accent);
}

.bubble {
  max-width: 70%;
  background: rgba(0, 0, 0, 0.35);
  border: 1px solid rgba(var(--accent-rgb), 0.15);
  border-radius: 14px 14px 14px 4px;
  padding: 0.6rem 0.9rem;
}

.bubble-row.mine .bubble {
  background: rgba(var(--accent-rgb), 0.18);
  border-color: rgba(var(--accent-rgb), 0.4);
  border-radius: 14px 14px 4px 14px;
}

.bubble-text {
  margin: 0;
  color: #ffffff;
  font-size: 0.95rem;
  line-height: 1.4;
  white-space: pre-wrap;
  word-break: break-word;
}

.bubble-time {
  display: block;
  margin-top: 4px;
  font-size: 0.7rem;
  color: #a8a29e;
  text-align: right;
}

/* Κεντραρισμένο μήνυμα όταν δεν υπάρχει περιεχόμενο */
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

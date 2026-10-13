<template>
  <aside class="app-sidebar">
    <NuxtLink to="/drop" class="brand" title="To Do(Do) List">
      <span class="brand-full">To Do<span class="brand-do">(Do)</span></span>
      <span class="brand-short">T<span class="brand-do">D</span></span>
    </NuxtLink>

    <nav class="nav">
      <NuxtLink
        v-for="item in items"
        :key="item.to"
        :to="item.to"
        class="nav-item"
        :class="{ active: route.path === item.to }"
        :title="item.label"
      >
        <span class="nav-icon">{{ item.icon }}</span>
        <span class="nav-label">{{ item.label }}</span>
        <NotifyBadge :count="mounted ? item.badge() : 0" />
      </NuxtLink>
    </nav>

    <!-- Κοινός μετρητής: όλοι οι χρήστες τον ανεβάζουν μαζί -->
    <button class="clicker" title="Global Clicker: click to BOOST!" @click="incrementCount($event)">
      <span class="clicker-label">Global Clicker</span>
      <span class="clicker-number">{{ globalCount }}</span>
      <span class="clicker-hint">Click to BOOST!</span>
    </button>

    <NuxtLink v-if="currentUser" to="/settings" class="user-chip" :title="`Signed in as ${currentUser}`">
      <span class="user-avatar">{{ currentUser.charAt(0).toUpperCase() }}</span>
      <span class="user-name">{{ currentUser }}</span>
    </NuxtLink>
  </aside>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { useNotifications } from '~/composables/useNotifications'
import { getCurrentUser } from '~/composables/useAuth'
import { burstConfetti } from '~/composables/useConfetti'

const route = useRoute()
const notifications = useNotifications()
const currentUser = ref('')
const globalCount = ref(0)
// Τα badges εμφανίζονται μόνο μετά το mount: οι ειδοποιήσεις μπορεί να έρθουν πριν "ζωντανέψει"
// η σελίδα από τον server, και τότε το HTML δεν θα ταίριαζε (hydration mismatch)
const mounted = ref(false)
let countTimer = null

const items = [
  { to: '/drop', icon: '📋', label: 'My Board', badge: () => 0 },
  { to: '/send', icon: '📨', label: 'Friends Tasks', badge: () => notifications.value.newTasks + notifications.value.completedTasks },
  { to: '/friends', icon: '👥', label: 'Friends', badge: () => notifications.value.friendRequests + notifications.value.unreadMessages },
  { to: '/community', icon: '🌍', label: 'Community', badge: () => notifications.value.communityCompleted },
  { to: '/settings', icon: '⚙️', label: 'Settings', badge: () => 0 }
]

onMounted(() => {
  mounted.value = true
  currentUser.value = getCurrentUser() || ''
  loadCount()
  // Κάθε 2 δευτερόλεπτα φέρνουμε την τιμή, ώστε να βλέπουμε και τα κλικ των άλλων χρηστών
  countTimer = setInterval(() => {
    if (!document.hidden) loadCount()
  }, 2000)
})

onUnmounted(() => clearInterval(countTimer))

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
.app-sidebar {
  position: relative;
  z-index: 3;
  width: 230px;
  flex-shrink: 0;
  height: 100%;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  gap: 1rem;
  padding: 1.5rem 1rem;
  background: rgba(28, 25, 23, 0.7);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-right: 1px solid rgba(var(--accent-rgb), 0.2);
  font-family: 'Poppins', sans-serif;
}

.brand {
  display: block;
  padding: 0 0.6rem 0.5rem;
  color: #ffffff;
  font-size: 1.45rem;
  font-weight: 800;
  text-decoration: none;
  text-shadow: 0 0 10px rgba(var(--accent-rgb), 0.5);
  white-space: nowrap;
}

.brand-do {
  color: var(--accent);
}

.brand-short {
  display: none;
}

.nav {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.nav-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0.65rem 0.8rem;
  border-radius: 10px;
  color: #d6d3d1;
  text-decoration: none;
  font-size: 0.95rem;
  font-weight: 500;
  border: 1px solid transparent;
  transition: all 0.2s;
}

.nav-item:hover {
  background: rgba(var(--accent-rgb), 0.1);
  color: #ffffff;
}

.nav-item.active {
  background: rgba(var(--accent-rgb), 0.18);
  border-color: rgba(var(--accent-rgb), 0.5);
  color: #ffffff;
  font-weight: 600;
  box-shadow: 0 0 12px rgba(var(--accent-rgb), 0.2);
}

.nav-icon {
  width: 22px;
  text-align: center;
  font-size: 1.05rem;
}

/* το badge στη δεξιά άκρη της γραμμής αντί για τη γωνία */
.nav-item :deep(.notify-badge) {
  position: static;
  margin-left: auto;
}

.clicker {
  margin-top: auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 0.8rem;
  border-radius: 12px;
  background: rgba(0, 0, 0, 0.25);
  border: 1px solid rgba(var(--accent-rgb), 0.25);
  cursor: pointer;
  font-family: inherit;
  transition: all 0.2s;
}

.clicker:hover {
  border-color: var(--accent);
  box-shadow: 0 0 15px rgba(var(--accent-rgb), 0.3);
}

.clicker:active {
  transform: scale(0.96);
}

.clicker-label {
  color: var(--accent);
  font-size: 0.7rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.clicker-number {
  color: #ffffff;
  font-size: 1.6rem;
  font-weight: 800;
  line-height: 1.2;
  text-shadow: 0 0 12px rgba(var(--accent-rgb), 0.6);
}

.clicker-hint {
  color: #a8a29e;
  font-size: 0.7rem;
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0.5rem 0.6rem;
  border-radius: 10px;
  text-decoration: none;
  color: #e7e5e4;
  font-size: 0.9rem;
  font-weight: 500;
  transition: background 0.2s;
}

.user-chip:hover {
  background: rgba(var(--accent-rgb), 0.1);
}

.user-avatar {
  width: 32px;
  height: 32px;
  flex-shrink: 0;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--accent);
  color: #1c1917;
  font-weight: 800;
}

.user-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* Μικρότερες οθόνες: μόνο εικονίδια */
@media (max-width: 1100px) {
  .app-sidebar {
    width: 76px;
    padding: 1.2rem 0.6rem;
    align-items: center;
  }

  .brand-full, .nav-label, .clicker-label, .clicker-hint, .user-name {
    display: none;
  }

  .brand-short {
    display: inline;
  }

  .brand {
    padding: 0 0 0.5rem;
  }

  .nav-item {
    justify-content: center;
    padding: 0.7rem;
  }

  .nav-item :deep(.notify-badge) {
    position: absolute;
    top: -4px;
    right: -4px;
  }

  .clicker {
    padding: 0.5rem 0.3rem;
    width: 100%;
  }

  .clicker-number {
    font-size: 1rem;
  }
}
</style>

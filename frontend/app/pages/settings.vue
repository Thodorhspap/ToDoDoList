<template>
  <div class="page-container">

    <ParticlesBg />

    <!-- Λεπτό οριζόντιο board κορυφής -->
    <div class="top-settings-bar">
      <button class="bar-btn" @click="router.push('/drop')">
        ← Back to Board
      </button>
    </div>

    <div class="settings-card">
      <div class="settings-header">
        <h1 class="title">Settings</h1>
        <p class="subtitle">Manage your account and preferences</p>
      </div>

      <div class="settings-list">
        <!-- Εμφάνιση -->
        <section class="settings-section">
          <h3 class="section-title">Appearance</h3>

          <div class="setting-row">
            <div class="setting-info">
              <span class="setting-value">Art style</span>
              <span class="setting-label">The overall look of the app</span>
            </div>
            <div class="segmented">
              <button
                v-for="option in artStyleOptions"
                :key="option.value"
                :class="{ active: settings.artStyle === option.value }"
                @click="settings.artStyle = option.value"
              >{{ option.label }}</button>
            </div>
          </div>

          <div class="setting-row">
            <div class="setting-info">
              <span class="setting-value">Accent color</span>
              <span class="setting-label">The main color of the app</span>
            </div>
            <div class="swatches">
              <button
                v-for="(hex, name) in ACCENT_COLORS"
                :key="name"
                class="swatch"
                :class="{ active: settings.accent === name }"
                :style="{ backgroundColor: hex }"
                :title="name"
                @click="settings.accent = name"
              ></button>
            </div>
          </div>

          <div class="setting-row">
            <div class="setting-info">
              <span class="setting-value">Background particles</span>
              <span class="setting-label">Fewer or none helps on slow computers</span>
            </div>
            <div class="segmented">
              <button
                v-for="option in particleOptions"
                :key="option.value"
                :class="{ active: settings.particles === option.value }"
                @click="settings.particles = option.value"
              >{{ option.label }}</button>
            </div>
          </div>

          <div class="setting-row">
            <div class="setting-info">
              <span class="setting-value">Animations</span>
              <span class="setting-label">Transitions, glowing text and moving particles</span>
            </div>
            <label class="toggle">
              <input v-model="settings.animations" type="checkbox" />
              <span class="toggle-slider"></span>
            </label>
          </div>
        </section>

        <!-- Board -->
        <section class="settings-section">
          <h3 class="section-title">Board</h3>

          <div class="setting-row">
            <div class="setting-info">
              <span class="setting-value">Sort tasks by</span>
              <span class="setting-label">Order of the tasks on your board</span>
            </div>
            <select v-model="settings.taskSort" class="field small-field">
              <option value="newest">Newest first</option>
              <option value="oldest">Oldest first</option>
              <option value="dueDate">Due date</option>
              <option value="name">Name (A–Z)</option>
              <option value="priority">Priority (high first)</option>
            </select>
          </div>

          <div class="setting-row">
            <div class="setting-info">
              <span class="setting-value">Default author name</span>
              <span class="setting-label">Filled in automatically when you add a task</span>
            </div>
            <input v-model.trim="settings.defaultAuthor" type="text" class="field small-field" :placeholder="username" />
          </div>
        </section>

        <!-- Λογαριασμός -->
        <section class="settings-section">
          <h3 class="section-title">Account</h3>

          <div class="setting-row">
            <div class="setting-info">
              <span class="setting-label">Logged in as</span>
              <span class="setting-value">{{ username }}</span>
            </div>
            <button class="danger-btn" @click="logout">Log Out</button>
          </div>

          <!-- Αλλαγή ονόματος -->
          <form class="account-form" @submit.prevent="changeUsername">
            <span class="setting-value">Change username</span>
            <div class="form-row">
              <input v-model="usernameForm.newUsername" type="text" class="field" placeholder="New username" required />
              <input v-model="usernameForm.password" type="password" class="field" placeholder="Your password" required />
            </div>
            <p v-if="usernameForm.message" class="form-msg" :class="{ error: usernameForm.error }">{{ usernameForm.message }}</p>
            <button type="submit" class="primary-btn" :disabled="usernameForm.loading">
              {{ usernameForm.loading ? 'Saving...' : 'Change Username' }}
            </button>
          </form>

          <!-- Αλλαγή κωδικού -->
          <form class="account-form" @submit.prevent="changePassword">
            <span class="setting-value">Change password</span>
            <input v-model="passwordForm.currentPassword" type="password" class="field" placeholder="Current password" required />
            <div class="form-row">
              <input v-model="passwordForm.newPassword" type="password" class="field" placeholder="New password" required />
              <input v-model="passwordForm.confirmPassword" type="password" class="field" placeholder="Repeat new password" required />
            </div>
            <p v-if="passwordForm.message" class="form-msg" :class="{ error: passwordForm.error }">{{ passwordForm.message }}</p>
            <button type="submit" class="primary-btn" :disabled="passwordForm.loading">
              {{ passwordForm.loading ? 'Saving...' : 'Change Password' }}
            </button>
          </form>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ACCENT_COLORS, useSettings } from '~/composables/useSettings'
import { getCurrentUser, setCurrentUser, clearCurrentUser } from '~/composables/useAuth'

const router = useRouter()
const settings = useSettings()
const username = ref('')

const artStyleOptions = [
  { value: 'glass', label: 'Glass' },
  { value: 'brutal', label: 'Neo-Brutalism' }
]

const particleOptions = [
  { value: 'normal', label: 'Normal' },
  { value: 'reduced', label: 'Fewer' },
  { value: 'off', label: 'Off' }
]

const usernameForm = reactive({ newUsername: '', password: '', message: '', error: false, loading: false })
const passwordForm = reactive({ currentPassword: '', newPassword: '', confirmPassword: '', message: '', error: false, loading: false })

onMounted(() => {
  const savedUser = getCurrentUser()
  if (!savedUser) {
    // Χωρίς σύνδεση δεν βλέπει κανείς τις ρυθμίσεις
    router.push('/login')
    return
  }
  username.value = savedUser
})

const changeUsername = async () => {
  usernameForm.message = ''
  usernameForm.loading = true
  try {
    const response = await $fetch('http://localhost:8080/api/auth/username', {
      method: 'PUT',
      body: {
        username: username.value,
        password: usernameForm.password,
        newUsername: usernameForm.newUsername.trim()
      }
    })

    // Τα tasks είναι δεμένα με το όνομα, οπότε αποθηκεύουμε το νέο
    setCurrentUser(response.username)
    username.value = response.username
    usernameForm.newUsername = ''
    usernameForm.password = ''
    usernameForm.error = false
    usernameForm.message = response.message
  } catch (error) {
    usernameForm.error = true
    usernameForm.message = error?.data?.message || 'Could not change username. Is the server running?'
  } finally {
    usernameForm.loading = false
  }
}

const changePassword = async () => {
  passwordForm.message = ''
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    passwordForm.error = true
    passwordForm.message = 'Οι νέοι κωδικοί δεν ταιριάζουν'
    return
  }

  passwordForm.loading = true
  try {
    const response = await $fetch('http://localhost:8080/api/auth/password', {
      method: 'PUT',
      body: {
        username: username.value,
        currentPassword: passwordForm.currentPassword,
        newPassword: passwordForm.newPassword
      }
    })

    passwordForm.currentPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    passwordForm.error = false
    passwordForm.message = response.message
  } catch (error) {
    passwordForm.error = true
    passwordForm.message = error?.data?.message || 'Could not change password. Is the server running?'
  } finally {
    passwordForm.loading = false
  }
}

const logout = () => {
  clearCurrentUser()
  router.push('/login')
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

/* Κεντρική κάρτα ρυθμίσεων */
.settings-card {
  position: relative;
  z-index: 1;
  background: rgba(28, 25, 23, 0.65);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(var(--accent-rgb), 0.2);
  padding: 2.5rem;
  border-radius: 20px;
  width: 100%;
  max-width: 720px;
  margin: 0 auto;
  flex: 1;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.5), 0 0 20px rgba(var(--accent-rgb), 0.1);
  min-height: 0;
}

.settings-header {
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

.subtitle {
  margin: 0.5rem 0 0;
  color: #a8a29e;
}

/* Λίστα ρυθμίσεων με δικό της scroll */
.settings-list {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  flex: 1;
  overflow-y: auto;
  padding-right: 10px;
  min-height: 0;
}

.settings-list::-webkit-scrollbar {
  width: 8px;
}

.settings-list::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.2);
  border-radius: 4px;
}

.settings-list::-webkit-scrollbar-thumb {
  background: rgba(var(--accent-rgb), 0.4);
  border-radius: 4px;
}

.settings-section {
  background: rgba(0, 0, 0, 0.25);
  border: 1px solid rgba(var(--accent-rgb), 0.15);
  border-radius: 12px;
  padding: 1.2rem 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.2rem;
}

.section-title {
  margin: 0;
  color: var(--accent);
  font-size: 0.85rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.setting-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
}

.setting-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.setting-label {
  color: #a8a29e;
  font-size: 0.85rem;
}

.setting-value {
  color: #ffffff;
  font-size: 1.05rem;
  font-weight: 500;
}

/* Χρώματα έμφασης */
.swatches {
  display: flex;
  gap: 10px;
  flex-shrink: 0;
}

.swatch {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: 2px solid transparent;
  cursor: pointer;
  transition: all 0.2s;
}

.swatch:hover {
  transform: scale(1.1);
}

.swatch.active {
  border-color: #ffffff;
  box-shadow: 0 0 12px rgba(var(--accent-rgb), 0.8);
}

/* Επιλογή Normal / Fewer / Off */
.segmented {
  display: flex;
  border: 1px solid rgba(var(--accent-rgb), 0.5);
  border-radius: 10px;
  overflow: hidden;
  flex-shrink: 0;
}

.segmented button {
  background: transparent;
  color: #a8a29e;
  border: none;
  padding: 0.45rem 0.9rem;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  font-family: 'Poppins', sans-serif;
  transition: all 0.2s;
}

.segmented button.active {
  background: var(--accent);
  color: #1c1917;
}

/* Διακόπτης on/off */
.toggle {
  position: relative;
  width: 48px;
  height: 26px;
  flex-shrink: 0;
  cursor: pointer;
}

.toggle input {
  opacity: 0;
  width: 0;
  height: 0;
}

.toggle-slider {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  border: 1px solid rgba(var(--accent-rgb), 0.5);
  border-radius: 26px;
  transition: all 0.3s;
}

.toggle-slider::before {
  content: "";
  position: absolute;
  width: 18px;
  height: 18px;
  left: 3px;
  top: 3px;
  background: #a8a29e;
  border-radius: 50%;
  transition: all 0.3s;
}

.toggle input:checked + .toggle-slider {
  background: rgba(var(--accent-rgb), 0.3);
}

.toggle input:checked + .toggle-slider::before {
  transform: translateX(22px);
  background: var(--accent);
}

/* Πεδία φορμών */
.field {
  background: rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(var(--accent-rgb), 0.3);
  border-radius: 10px;
  padding: 0.7rem 0.9rem;
  color: white;
  font-size: 0.95rem;
  font-family: 'Poppins', sans-serif;
  box-sizing: border-box;
  transition: all 0.3s;
  min-width: 0;
}

.field:focus {
  outline: none;
  border-color: var(--accent);
  background-color: rgba(0, 0, 0, 0.5);
  box-shadow: 0 0 15px rgba(var(--accent-rgb), 0.3);
}

.small-field {
  width: 200px;
  flex-shrink: 0;
}

select.field {
  cursor: pointer;
  color-scheme: dark;
  background-image:
    linear-gradient(45deg, transparent 50%, var(--accent) 50%),
    linear-gradient(135deg, var(--accent) 50%, transparent 50%);
  background-position: calc(100% - 20px) 50%, calc(100% - 15px) 50%;
  background-size: 5px 5px, 5px 5px;
  background-repeat: no-repeat;
  padding-right: 2.5rem;
}

.account-form {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-top: 1.2rem;
  border-top: 1px solid rgba(var(--accent-rgb), 0.15);
}

.form-row {
  display: flex;
  gap: 10px;
}

.form-row .field {
  flex: 1;
}

.form-msg {
  margin: 0;
  color: #4ade80;
  font-size: 0.9rem;
}

.form-msg.error {
  color: #f87171;
}

.primary-btn {
  background: transparent;
  color: var(--accent);
  border: 2px solid var(--accent);
  border-radius: 10px;
  padding: 0.6rem 1.2rem;
  font-size: 0.95rem;
  font-weight: 600;
  cursor: pointer;
  font-family: 'Poppins', sans-serif;
  transition: all 0.3s;
  align-self: flex-start;
}

.primary-btn:hover {
  background: var(--accent);
  color: #1c1917;
  box-shadow: 0 0 20px rgba(var(--accent-rgb), 0.6);
}

.primary-btn:disabled {
  opacity: 0.6;
  cursor: wait;
}

.danger-btn {
  background: transparent;
  color: #f87171;
  border: 2px solid #f87171;
  border-radius: 10px;
  padding: 0.6rem 1.2rem;
  font-size: 0.95rem;
  font-weight: 600;
  cursor: pointer;
  font-family: 'Poppins', sans-serif;
  transition: all 0.3s;
  flex-shrink: 0;
}

.danger-btn:hover {
  background: #f87171;
  color: #1c1917;
  box-shadow: 0 0 20px rgba(248, 113, 113, 0.6);
}
</style>

<template>
  <div class="login-container">
    <ParticlesBg />
    
    <div class="login-card">
      <h1 class="title">Create Account</h1>
      <p class="subtitle">Sign up to start setting your tasks</p>
      
      <form @submit.prevent="handleSignIn" class="login-form">
        <div class="input-group">
          <label for="username">Name / Username</label>
          <input 
            id="username"
            v-model="username" 
            type="text" 
            placeholder="Choose a name..." 
            required 
          />
        </div>

        <div class="input-group">
          <label for="password">Password</label>
          <input 
            id="password"
            v-model="password" 
            type="password" 
            placeholder="••••••••" 
            required 
          />
        </div>

        <p v-if="errorMessage" class="error-msg">{{ errorMessage }}</p>

        <button type="submit" class="primary-btn" :disabled="loading">
          {{ loading ? 'Creating...' : 'Sign Up' }}
        </button>
      </form>

      <div class="switch-auth">
        <button class="secondary-link-btn" @click="goToLogin">
          You already have an account? Login here
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { setCurrentUser } from '~/composables/useAuth'

const router = useRouter()
const username = ref('')
const password = ref('')

const errorMessage = ref('')
const loading = ref(false)

const handleSignIn = async () => {
  if (!username.value.trim() || !password.value.trim()) return

  errorMessage.value = ''
  loading.value = true
  try {
    const response = await $fetch('http://localhost:8080/api/auth/register', {
      method: 'POST',
      body: {
        username: username.value.trim(),
        password: password.value
      }
    })

    if (response.success) {
      // Αποθηκεύουμε το όνομα (τα tasks δένονται με αυτό) και το ID του νέου χρήστη
      setCurrentUser(response.username, response.userId)
      router.push('/drop')
    }
  } catch (error) {
    errorMessage.value = error?.data?.message || 'Registration failed. Is the server running?'
    console.error(error)
  } finally {
    loading.value = false
  }
}

const goToLogin = () => {
  router.push('/login')
}

</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Poppins:wght@400;500;600;800&display=swap');

.login-container { min-height: 100vh; display: flex; align-items: center; justify-content: center; position: relative; font-family: 'Poppins', 'Segoe UI', Tahoma, sans-serif; overflow: hidden; padding: 2rem; }
.login-card { position: relative; z-index: 1; background: rgba(28, 25, 23, 0.65); backdrop-filter: blur(16px); -webkit-backdrop-filter: blur(16px); border: 1px solid rgba(var(--accent-rgb), 0.2); padding: 3rem 2.5rem; border-radius: 20px; width: 100%; max-width: 440px; box-shadow: 0 15px 35px rgba(0, 0, 0, 0.5), 0 0 20px rgba(var(--accent-rgb), 0.1); }
.title { margin: 0; font-size: 2.2rem; color: #ffffff; font-weight: 800; text-align: center; text-shadow: 0 0 10px rgba(var(--accent-rgb), 0.5); }
.subtitle { margin: 0.5rem 0 2rem; color: #a8a29e; font-size: 0.95rem; text-align: center; }
.login-form { display: flex; flex-direction: column; gap: 1.5rem; }
.input-group { display: flex; flex-direction: column; gap: 0.5rem; }
.input-group label { color: #d6d3d1; font-size: 0.9rem; font-weight: 500; }
.input-group input { background: rgba(0, 0, 0, 0.3); border: 1px solid rgba(var(--accent-rgb), 0.3); border-radius: 10px; padding: 0.9rem 1rem; color: white; font-size: 1rem; font-family: 'Poppins', sans-serif; transition: all 0.3s ease; }
.input-group input:focus { outline: none; border-color: var(--accent); background: rgba(0, 0, 0, 0.5); box-shadow: 0 0 15px rgba(var(--accent-rgb), 0.3); }
.primary-btn { background-color: transparent; color: var(--accent); border: 2px solid var(--accent); padding: 0.9rem; border-radius: 10px; font-size: 1.1rem; font-weight: 600; cursor: pointer; transition: all 0.3s ease; width: 100%; margin-top: 1rem; box-shadow: 0 0 10px rgba(var(--accent-rgb), 0.1); font-family: 'Poppins', sans-serif; }
.primary-btn:hover { background-color: var(--accent); color: #1c1917; box-shadow: 0 0 20px rgba(var(--accent-rgb), 0.7), 0 0 40px rgba(var(--accent-rgb), 0.3); transform: translateY(-2px); }
.switch-auth { text-align: center; margin-top: 1.5rem; }
.secondary-link-btn { background: transparent; border: none; color: #a8a29e; font-size: 0.85rem; font-family: 'Poppins', sans-serif; cursor: pointer; transition: color 0.2s ease; padding: 0.5rem; }
.secondary-link-btn:hover { color: var(--accent); text-decoration: underline; }
.error-msg { margin: 0; color: #f87171; font-size: 0.9rem; text-align: center; }
.primary-btn:disabled { opacity: 0.6; cursor: wait; }
</style>
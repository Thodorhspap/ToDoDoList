<template>
  <div class="landing-container">
    
    <!-- Τα σωματίδια στο φόντο παραμένουν ενεργά και ορατά -->
    <ParticlesBg />
    
    <!-- Περιεχόμενο (στοιχισμένο αριστερά) -->
    <div class="content" :class="{ 'fade-out': showWelcome }">
      <h1 class="hero-title">
        To Do <span class="blink-do">Do</span> List
      </h1>
      <p class="hero-subtitle">Ξεκινήστε να οργανώνετε τη μέρα σας με στυλ.</p>
      
      <button @click="startJourney" class="lets-go-btn">
        Let's Go
      </button>
    </div>

    <!-- Το κινηματογραφικό animation του Welcome -->
    <Transition name="welcome">
      <div v-if="showWelcome" class="welcome-overlay">
        <h2 class="welcome-text">Welcome...</h2>
      </div>
    </Transition>

  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const showWelcome = ref(false)

const startJourney = () => {
  showWelcome.value = true
  
  setTimeout(() => {
    router.push('/login')
  }, 2000)
}

</script>

<style scoped>
/* Εισαγωγή της Cormorant Garamond και της Poppins από Google Fonts */
@import url('https://fonts.googleapis.com/css2?family=Cormorant+Garamond:wght@600;700&family=Poppins:wght@400;600&display=swap');

.landing-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  padding-left: 8vw;
  background-color: #1c1917; 
  font-family: 'Poppins', 'Segoe UI', Tahoma, sans-serif;
  overflow: hidden;
  position: relative;
}


/* === Αρχικό Περιεχόμενο === */
.content {
  position: relative;
  text-align: left;
  max-width: 600px;
  transition: opacity 0.6s ease, transform 0.6s ease;
  z-index: 1; 
}

.content.fade-out {
  opacity: 0;
  transform: translateX(-50px);
  pointer-events: none;
}

.hero-title {
  font-family: 'Cormorant Garamond', serif;
  font-size: 5.5rem;
  color: #ffffff;
  margin: 0;
  font-weight: 700;
  letter-spacing: -1px;
  line-height: 1.1;
}

/* Το δεύτερο "Do" που σβήνει τελείως (opacity: 0) */
.blink-do {
  color: var(--accent);
  animation: neonBlink 3s infinite ease-in-out;
}

/* Ορισμός του animation ώστε να εξαφανίζεται πλήρως στο 50% */
@keyframes neonBlink {
  0%, 100% {
    opacity: 1;
    text-shadow: 0 0 20px rgba(var(--accent-rgb), 0.8), 0 0 40px rgba(var(--accent-rgb), 0.4);
  }
  50% {
    opacity: 0;
    text-shadow: 0 0 0px rgba(var(--accent-rgb), 0);
  }
}

.hero-subtitle {
  color: #a8a29e;
  font-size: 1.1rem;
  margin: 1.5rem 0 3rem;
}

.lets-go-btn {
  background-color: transparent;
  color: var(--accent);
  border: 2px solid var(--accent);
  padding: 1rem 3.5rem;
  border-radius: 50px;
  font-size: 1.2rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 0 15px rgba(var(--accent-rgb), 0.1);
  font-family: 'Poppins', sans-serif;
}

.lets-go-btn:hover {
  background-color: var(--accent);
  color: #1c1917;
  box-shadow: 0 0 30px rgba(var(--accent-rgb), 0.6);
  transform: scale(1.05);
}

/* === Οθόνη Welcome === */
.welcome-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(28, 25, 23, 0.6); 
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;
}

.welcome-text {
  font-size: 3.5rem;
  color: var(--accent);
  font-weight: 800;
  letter-spacing: 4px;
  text-shadow: 0 0 30px rgba(var(--accent-rgb), 0.8);
  font-family: 'Cormorant Garamond', serif;
}

/* === Animations του Vue Transition === */
.welcome-enter-active {
  transition: all 1s cubic-bezier(0.4, 0, 0.2, 1);
}

.welcome-enter-from {
  opacity: 0;
  transform: scale(1.2);
}

.welcome-enter-to {
  opacity: 1;
  transform: scale(1);
}
</style>
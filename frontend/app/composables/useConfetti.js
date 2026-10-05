import { tsParticles } from '@tsparticles/engine'
import { loadFull } from 'tsparticles'

// Φορτώνει όλα τα σχήματα/plugins μία φορά, για το φόντο και για το κομφετί
let particlesReady = null
export const ensureParticlesLoaded = () => {
  if (!particlesReady) particlesReady = loadFull(tsParticles)
  return particlesReady
}

// Κομφετί που "σκάει" από ένα στοιχείο της σελίδας (π.χ. το checkbox που μόλις τσεκάραμε)

// Έντονα βασικά χρώματα (χωρίς ροζ) για το χάρτινο κομφετί
const CONFETTI_COLORS = ['#ffd23f', '#3b82f6', '#ef4444', '#22c55e', '#111111']

// Ροζ νέον σπίθες για το Glass
const SPARK_COLORS = ['#ff2d95', '#ff2d95', '#ec4899', '#ff7ac6', '#ffffff']
const SPARK_GLOW = '#ff2d95'
const LIFETIME_MS = 3500
let burstId = 0

// Διαβάζουμε τις ρυθμίσεις από το <html>, όπου τις εφαρμόζει το settings plugin
const readTheme = () => {
  const root = document.documentElement
  return {
    animationsOff: root.classList.contains('no-animations'),
    brutal: root.classList.contains('theme-brutal')
  }
}

// Το κέντρο του στοιχείου, σε ποσοστό της οθόνης
const centerOf = (source) => {
  const rect = source?.getBoundingClientRect?.()
  return {
    x: rect ? ((rect.left + rect.width / 2) / window.innerWidth) * 100 : 50,
    y: rect ? ((rect.top + rect.height / 2) / window.innerHeight) * 100 : 50
  }
}

// Δημιουργεί ένα προσωρινό layer πάνω από όλη τη σελίδα, παίζει το εφέ και το σβήνει
const launch = async (options, layerStyle = {}) => {
  await ensureParticlesLoaded()

  const id = `confetti-${++burstId}`
  const layer = document.createElement('div')
  layer.id = id
  Object.assign(layer.style, {
    position: 'fixed',
    inset: '0',
    width: '100vw',
    height: '100vh',
    zIndex: '1000',
    pointerEvents: 'none',
    ...layerStyle
  })
  document.body.appendChild(layer)

  try {
    const container = await tsParticles.load({
      id,
      options: {
        fullScreen: { enable: false },
        background: { color: { value: 'transparent' } },
        detectRetina: true,
        interactivity: { events: { onHover: { enable: false }, onClick: { enable: false } } },
        ...options
      }
    })

    setTimeout(() => {
      container?.destroy()
      layer.remove()
    }, LIFETIME_MS)
  } catch (error) {
    console.error('❌ Confetti error:', error)
    layer.remove()
  }
}

// Χάρτινο κομφετί που πετάγεται προς τα πάνω και πέφτει (Neo-Brutalism, Global Clicker)
export const burstConfetti = async (source, amount = 40) => {
  if (typeof window === 'undefined') return
  const { animationsOff, brutal } = readTheme()
  if (animationsOff) return
  const { x, y } = centerOf(source)

  await launch({
    particles: {
      number: { value: 0 },
      paint: {
        fill: { color: { value: CONFETTI_COLORS } },
        stroke: brutal ? { width: 2, color: { value: '#111111' } } : { width: 0 }
      },
      shape: {
        type: ['square', 'square', 'polygon', 'circle'],
        options: { polygon: { sides: 3 } }
      },
      size: { value: { min: 5, max: 10 } },
      opacity: { value: 1 },
      rotate: {
        value: { min: 0, max: 360 },
        direction: 'random',
        animation: { enable: true, speed: 40 }
      },
      wobble: { enable: true, distance: 15, speed: { min: -10, max: 10 } },
      move: {
        enable: true,
        direction: 'top',
        angle: { value: 70, offset: 0 },
        speed: { min: 18, max: 35 },
        decay: 0.06,
        gravity: { enable: true, acceleration: 35 },
        outModes: { default: 'destroy', top: 'none' }
      },
      life: { count: 1, duration: { value: 3, sync: true } }
    },
    emitters: {
      position: { x, y },
      size: { width: 0, height: 0 },
      life: { count: 1, duration: 0.1 },
      rate: { quantity: amount, delay: 0.1 }
    }
  })
}

// Glass: ροζ νέον σπίθες, μικρά γωνιώδη θραύσματα με λάμψη
export const burstSparks = async (source, amount = 50) => {
  if (typeof window === 'undefined') return
  const { animationsOff } = readTheme()
  if (animationsOff) return
  const { x, y } = centerOf(source)

  await launch({
    particles: {
      number: { value: 0 },
      paint: {
        fill: { color: { value: SPARK_COLORS } }
      },
      shape: {
        type: ['square', 'polygon', 'circle'],
        options: { polygon: { sides: 3 } }
      },
      size: {
        value: { min: 2, max: 6 },
        animation: { enable: true, speed: 4, startValue: 'max', destroy: 'min' }
      },
      opacity: {
        // Ξεκινά από το max και σβήνει μέχρι το min (το εύρος χρειάζεται, αλλιώς σβήνει αμέσως)
        value: { min: 0, max: 1 },
        animation: { enable: true, speed: 1.1, startValue: 'max', destroy: 'min' }
      },
      rotate: {
        value: { min: 0, max: 360 },
        direction: 'random',
        animation: { enable: true, speed: 25 }
      },
      move: {
        enable: true,
        direction: 'none',
        speed: { min: 10, max: 28 },
        decay: 0.08,
        gravity: { enable: true, acceleration: 4 },
        outModes: { default: 'destroy' }
      },
      life: { count: 1, duration: { value: { min: 0.8, max: 1.5 } } }
    },
    emitters: {
      position: { x, y },
      size: { width: 0, height: 0 },
      life: { count: 1, duration: 0.1 },
      rate: { quantity: amount, delay: 0.1 }
    }
  }, {
    // Ροζ λάμψη νέον γύρω από κάθε σπίθα
    filter: `drop-shadow(0 0 3px ${SPARK_GLOW}) drop-shadow(0 0 8px ${SPARK_GLOW})`
  })
}

// Γιορτή για ολοκληρωμένο task: σπίθες στο Glass, χάρτινο κομφετί στο Neo-Brutalism
export const celebrateTask = (source) => {
  if (typeof window === 'undefined') return
  return readTheme().brutal ? burstConfetti(source) : burstSparks(source)
}

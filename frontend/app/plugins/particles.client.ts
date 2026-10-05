import { defineNuxtPlugin } from '#app'
import Particles from '@tsparticles/vue3'
import { ensureParticlesLoaded } from '~/composables/useConfetti'

export default defineNuxtPlugin((nuxtApp) => {
  nuxtApp.vueApp.use(Particles, {
    // Το ίδιο φόρτωμα με το κομφετί, ώστε να γίνεται μόνο μία φορά
    init: async () => {
      await ensureParticlesLoaded()
    }
  })
})

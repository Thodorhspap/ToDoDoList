import { refreshNotifications } from '~/composables/useNotifications'

export default defineNuxtPlugin((nuxtApp) => {
  // Κάθε 5 δευτερόλεπτα ελέγχουμε για νέες ειδοποιήσεις, σε όποια σελίδα κι αν είμαστε
  nuxtApp.hook('app:mounted', () => {
    refreshNotifications()
    setInterval(() => {
      if (!document.hidden) refreshNotifications()
    }, 5000)
  })

  // Και αμέσως όταν αλλάζουμε σελίδα, για να μην περιμένουμε
  nuxtApp.hook('page:finish', () => {
    refreshNotifications()
  })
})

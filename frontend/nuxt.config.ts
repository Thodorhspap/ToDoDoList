export default defineNuxtConfig({
  app: {
    pageTransition: { name: 'page', mode: 'out-in' },
    head: {
      bodyAttrs: {
        style: 'background-color: #1c1917;'
      }
    }
  },
  css: ['~/assets/css/theme-brutal.css'],
  compatibilityDate: '2024-04-03',
  devtools: { enabled: true }
})
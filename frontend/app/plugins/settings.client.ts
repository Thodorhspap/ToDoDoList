import { watch } from 'vue'
import { SETTINGS_KEY, applySettingsToPage, readSavedSettings, useSettings } from '~/composables/useSettings'

export default defineNuxtPlugin((nuxtApp) => {
  const saved = readSavedSettings()

  // Χρώμα και animations εφαρμόζονται αμέσως, για να μη "τρεμοπαίξει" το ροζ πριν φορτώσει η σελίδα
  applySettingsToPage(saved)

  // Η κοινή κατάσταση ενημερώνεται μετά το hydration, ώστε η σελίδα από τον server να ταιριάζει
  nuxtApp.hook('app:mounted', () => {
    const settings = useSettings()
    Object.assign(settings.value, saved)

    watch(settings, (value) => {
      try {
        localStorage.setItem(SETTINGS_KEY, JSON.stringify(value))
      } catch (error) {
        console.error('❌ Error saving settings:', error)
      }
      applySettingsToPage(value)
    }, { deep: true })
  })
})

// Ρυθμίσεις εμφάνισης και board, αποθηκευμένες στον browser (localStorage)

export const SETTINGS_KEY = 'cyber_settings'

export const ACCENT_COLORS = {
  pink: '#ec4899',
  blue: '#3b82f6',
  green: '#22c55e',
  purple: '#a855f7'
}

export const DEFAULT_SETTINGS = {
  artStyle: 'glass',       // glass | brutal
  accent: 'pink',          // pink | blue | green | purple
  particles: 'normal',     // normal | reduced | off
  taskSort: 'newest',      // newest | oldest | dueDate | name
  defaultAuthor: '',
  animations: true
}

// Κοινή κατάσταση για όλες τις σελίδες
export const useSettings = () => useState('settings', () => ({ ...DEFAULT_SETTINGS }))

export const accentHex = (accent) => ACCENT_COLORS[accent] || ACCENT_COLORS.pink

export const readSavedSettings = () => {
  try {
    const saved = JSON.parse(localStorage.getItem(SETTINGS_KEY) || '{}')
    return { ...DEFAULT_SETTINGS, ...saved }
  } catch {
    return { ...DEFAULT_SETTINGS }
  }
}

// Εφαρμόζει χρώμα και animations απευθείας στο <html>, ώστε να ισχύουν σε κάθε σελίδα
export const applySettingsToPage = (settings) => {
  const hex = accentHex(settings.accent)
  const r = parseInt(hex.slice(1, 3), 16)
  const g = parseInt(hex.slice(3, 5), 16)
  const b = parseInt(hex.slice(5, 7), 16)

  const root = document.documentElement
  root.style.setProperty('--accent', hex)
  root.style.setProperty('--accent-rgb', `${r}, ${g}, ${b}`)
  root.classList.toggle('no-animations', !settings.animations)
  root.classList.toggle('theme-brutal', settings.artStyle === 'brutal')
}

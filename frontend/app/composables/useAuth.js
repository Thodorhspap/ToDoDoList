// Ποιος χρήστης είναι συνδεδεμένος.
//
// Κάθε καρτέλα θυμάται τον δικό της χρήστη (sessionStorage), ώστε δύο καρτέλες στον ίδιο
// browser να μπορούν να είναι συνδεδεμένες με διαφορετικούς λογαριασμούς χωρίς να "αλλάζουν"
// χρήστη η μία στην άλλη. Το localStorage κρατά την τελευταία σύνδεση, για να ξεκινούν οι
// καινούριες καρτέλες ήδη συνδεδεμένες.

const USER_KEY = 'cyber_user'
const USER_ID_KEY = 'cyber_user_id'

const safe = (fn) => {
  try {
    return fn()
  } catch {
    return null
  }
}

export const getCurrentUser = () => {
  const tabUser = safe(() => sessionStorage.getItem(USER_KEY))
  if (tabUser) return tabUser

  // Νέα καρτέλα: παίρνει την τελευταία σύνδεση και από εδώ και πέρα την κρατά για τον εαυτό της
  const lastUser = safe(() => localStorage.getItem(USER_KEY))
  if (lastUser) {
    safe(() => sessionStorage.setItem(USER_KEY, lastUser))
    const lastId = safe(() => localStorage.getItem(USER_ID_KEY))
    if (lastId) safe(() => sessionStorage.setItem(USER_ID_KEY, lastId))
  }
  return lastUser
}

export const setCurrentUser = (username, userId) => {
  for (const storage of [sessionStorage, localStorage]) {
    safe(() => storage.setItem(USER_KEY, username))
    if (userId !== undefined && userId !== null) safe(() => storage.setItem(USER_ID_KEY, String(userId)))
  }
}

export const clearCurrentUser = () => {
  for (const storage of [sessionStorage, localStorage]) {
    safe(() => storage.removeItem(USER_KEY))
    safe(() => storage.removeItem(USER_ID_KEY))
  }
}

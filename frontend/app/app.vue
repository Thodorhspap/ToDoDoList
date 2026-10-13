<template>
  <div class="app-root">
    <NuxtLayout>
      <NuxtPage />
    </NuxtLayout>
  </div>
</template>

<style>
/* Χρώμα έμφασης (αλλάζει από τα Settings) */
:root {
  --accent: #ec4899;
  --accent-rgb: 236, 72, 153;
}

/* Dropdowns: σκούρα λίστα και βελάκι στο χρώμα έμφασης */
select {
  appearance: none;
  -webkit-appearance: none;
  background-image:
    linear-gradient(45deg, transparent 50%, var(--accent) 50%),
    linear-gradient(135deg, var(--accent) 50%, transparent 50%);
  background-position: calc(100% - 20px) 50%, calc(100% - 15px) 50%;
  background-size: 5px 5px, 5px 5px;
  background-repeat: no-repeat;
}

select option {
  background-color: #292524;
  color: #ffffff;
}

select option:disabled {
  color: #a8a29e;
}

/* Όταν τα animations είναι κλειστά από τα Settings */
html.no-animations *,
html.no-animations *::before,
html.no-animations *::after {
  animation: none !important;
  transition: none !important;
}

/* Ορίζουμε παντού σκούρο φόντο για να μηδενίσουμε το λευκό flash */
html, body, .app-root {
  background-color: #1c1917;
  margin: 0;
  padding: 0;
  width: 100%;
  height: 100%;
}

/* Ομαλή μετάβαση σελίδων (Page Transitions) */
.page-enter-active,
.page-leave-active {
  transition: opacity 0.4s ease, transform 0.4s ease;
}

.page-enter-from {
  opacity: 0;
  transform: translateY(15px);
}

.page-leave-to {
  opacity: 0;
  transform: translateY(-15px);
}
</style>
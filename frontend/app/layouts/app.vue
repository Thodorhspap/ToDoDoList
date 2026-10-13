<template>
  <!-- Κοινό πλαίσιο για τις σελίδες μετά τη σύνδεση: πλαϊνό μενού + η σελίδα δεξιά -->
  <div class="app-shell">
    <ParticlesBg />
    <AppSidebar />
    <main class="app-main">
      <slot />
    </main>
  </div>
</template>

<style scoped>
.app-shell {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

/* χωρίς z-index εδώ, ώστε το πλαίσιο λεπτομερειών (position: fixed) να μπορεί να καλύψει και το πλαϊνό μενού */
.app-main {
  position: relative;
  flex: 1;
  min-width: 0;
  height: 100vh;
}
</style>

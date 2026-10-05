<template>
  <ClientOnly>
    <vue-particles
      v-if="settings.particles !== 'off'"
      id="tsparticles"
      :key="particlesKey"
      :options="particlesOptions"
      class="particles-bg"
    />
  </ClientOnly>
</template>

<script setup>
import { computed } from 'vue'
import { accentHex, useSettings } from '~/composables/useSettings'

const settings = useSettings()

// Αλλάζει όταν αλλάζει κάποια ρύθμιση, ώστε τα σωματίδια να ξαναφορτώνουν με τις νέες επιλογές
const particlesKey = computed(() =>
  `${settings.value.artStyle}-${settings.value.accent}-${settings.value.particles}-${settings.value.animations}`
)

// Glass: λεπτά σωματίδια με γραμμές που ενώνονται
const glassOptions = (color) => ({
  background: { color: { value: "#1c1917" } },
  particles: {
    paint: { fill: { color: { value: color } } },
    links: { color, distance: 150, enable: true, opacity: 0.4, width: 1 },
    move: { enable: settings.value.animations, speed: 1.5 },
    number: { value: settings.value.particles === 'reduced' ? 30 : 80 },
    size: { value: { min: 1, max: 3 } },
  },
  interactivity: {
    events: { onHover: { enable: true, mode: "grab" } },
    modes: { grab: { distance: 200, links: { opacity: 0.8 } } },
  },
})

// Neo-Brutalism: χοντρά χρωματιστά σχήματα με μαύρο περίγραμμα που αιωρούνται και γυρίζουν.
// Το ποντίκι τα σπρώχνει μακριά, και ένα κλικ προσθέτει καινούρια.
const brutalOptions = (color) => ({
  background: { color: { value: "transparent" } },
  particles: {
    paint: {
      fill: { color: { value: [color, "#ffd23f", "#a5f3fc", "#ff6b6b", "#4ade80"] } },
      stroke: { width: 2.5, color: { value: "#111111" } },
    },
    shape: {
      type: ["square", "circle", "star", "polygon"],
      options: { polygon: { sides: 3 }, star: { sides: 5 } },
    },
    number: {
      value: settings.value.particles === 'reduced' ? 10 : 24,
      limit: { mode: "delete", value: 45 },
    },
    size: { value: { min: 10, max: 22 } },
    opacity: { value: 1 },
    rotate: {
      value: { min: 0, max: 360 },
      direction: "random",
      animation: { enable: settings.value.animations, speed: 4 },
    },
    move: {
      enable: settings.value.animations,
      speed: 0.8,
      direction: "none",
      random: true,
      outModes: { default: "out" },
    },
  },
  interactivity: {
    events: {
      onHover: { enable: true, mode: "repulse" },
      onClick: { enable: true, mode: "push" },
    },
    modes: {
      repulse: { distance: 110, duration: 0.4 },
      push: { quantity: 2 },
    },
  },
})

const particlesOptions = computed(() => {
  const color = accentHex(settings.value.accent)
  return settings.value.artStyle === 'brutal' ? brutalOptions(color) : glassOptions(color)
})
</script>

<style>
.particles-bg {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
}
</style>

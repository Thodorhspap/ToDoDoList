<template>
  <span v-if="count > 0" class="notify-badge">{{ count > 99 ? '99+' : count }}</span>
</template>

<script setup>
defineProps({
  count: { type: Number, default: 0 }
})
</script>

<style>
/* Κόκκινο badge στην πάνω δεξιά γωνία του κουμπιού (το κουμπί χρειάζεται position: relative) */
.notify-badge {
  position: absolute;
  top: -8px;
  right: -8px;
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  box-sizing: border-box;
  border-radius: 10px;
  background: #ef4444;
  color: #ffffff;
  font-size: 0.7rem;
  font-weight: 700;
  font-family: 'Poppins', sans-serif;
  line-height: 20px;
  text-align: center;
  box-shadow: 0 0 10px rgba(239, 68, 68, 0.7);
  pointer-events: none;
  animation: badgePop 0.3s ease;
}

@keyframes badgePop {
  from { transform: scale(0); }
  to { transform: scale(1); }
}
</style>

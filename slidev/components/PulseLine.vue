<script setup lang="ts">
withDefaults(defineProps<{ width?: number }>(), { width: 420 });
</script>

<template>
  <svg
    class="tp-pulse-line"
    :width="width"
    height="36"
    viewBox="0 0 420 36"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
    aria-hidden="true"
  >
    <path
      class="tp-pulse-path"
      d="M0 18 H120 L138 18 L150 6 L164 30 L176 12 L186 18 H420"
      stroke="var(--tp-pulse)"
      stroke-width="2.5"
      stroke-linecap="round"
      stroke-linejoin="round"
    />
    <circle
      class="tp-pulse-dot"
      cx="186"
      cy="18"
      r="4"
      fill="var(--tp-pulse)"
    />
  </svg>
</template>

<style scoped>
.tp-pulse-line {
  display: block;
  margin: 0.4rem 0 0.2rem;
  overflow: visible;
}
.tp-pulse-path {
  stroke-dasharray: 520;
  stroke-dashoffset: 520;
  animation: tp-draw 1.4s ease-out forwards;
}
.tp-pulse-dot {
  transform-origin: 186px 18px;
  animation: tp-beat 1.6s ease-in-out 1.2s infinite;
  filter: drop-shadow(0 0 6px var(--tp-pulse));
}
@keyframes tp-draw {
  to {
    stroke-dashoffset: 0;
  }
}
@keyframes tp-beat {
  0%,
  100% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.7);
    opacity: 0.55;
  }
}
@media (prefers-reduced-motion: reduce) {
  .tp-pulse-path {
    animation: none;
    stroke-dashoffset: 0;
  }
  .tp-pulse-dot {
    animation: none;
  }
}
</style>

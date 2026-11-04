<script setup lang="ts">
import AppIcon from './AppIcon.vue'
import { theme, toggleTheme } from '../theme'
</script>

<template>
  <button
    type="button"
    class="icon-button"
    :aria-label="theme === 'dark' ? 'Passer au thème clair' : 'Passer au thème sombre'"
    :title="theme === 'dark' ? 'Thème clair' : 'Thème sombre'"
    @click="toggleTheme"
  >
    <AppIcon :name="theme === 'dark' ? 'sun' : 'moon'" />
  </button>
</template>

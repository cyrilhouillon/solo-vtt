<script setup lang="ts">
/** Icônes dessinées pour Solo VTT : trait de 1,75, même grille de 24. Décoratives : le bouton porte le nom. */
export type IconName = 'home' | 'catalog' | 'map' | 'saved' | 'grid' | 'sun' | 'moon' | 'keys' | 'notebook' | 'combat' | 'lock' | 'unlock' | 'more' | 'chevron-right' | 'chevron-down' | 'image' | 'remove'
defineProps<{ name: IconName }>()
</script>

<template>
  <svg
    class="icon"
    viewBox="0 0 24 24"
    width="22"
    height="22"
    fill="none"
    stroke="currentColor"
    stroke-width="1.75"
    stroke-linecap="round"
    stroke-linejoin="round"
    aria-hidden="true"
    focusable="false"
  >
    <template v-if="name === 'home'">
      <path d="M3.5 11 12 4l8.5 7" />
      <path d="M6 9.5v10h4.5v-5h3v5H18v-10" />
    </template>
    <template v-else-if="name === 'catalog'">
      <path d="M4 5.5C4 4.7 4.7 4 5.5 4H11v16H5.5C4.7 20 4 19.3 4 18.5z" />
      <path d="M20 5.5c0-.8-.7-1.5-1.5-1.5H13v16h5.5c.8 0 1.5-.7 1.5-1.5z" />
      <path d="M6.5 8h2.5M6.5 11h2.5M15 8h2.5M15 11h2.5" />
    </template>
    <template v-else-if="name === 'map'">
      <rect x="3.5" y="3.5" width="17" height="17" rx="1.5" />
      <path d="M3.5 9.2h17M3.5 14.8h17M9.2 3.5v17M14.8 3.5v17" />
    </template>
    <template v-else-if="name === 'saved'">
      <path d="M6.5 3.5h11v17L12 16.5l-5.5 4z" />
      <path d="M9.5 8h5" />
    </template>
    <template v-else-if="name === 'grid'">
      <path d="M8.5 3v18M15.5 3v18M3 8.5h18M3 15.5h18" />
    </template>
    <template v-else-if="name === 'sun'">
      <circle cx="12" cy="12" r="4" />
      <path d="M12 2.5v2M12 19.5v2M2.5 12h2M19.5 12h2M5.3 5.3l1.4 1.4M17.3 17.3l1.4 1.4M5.3 18.7l1.4-1.4M17.3 6.7l1.4-1.4" />
    </template>
    <template v-else-if="name === 'moon'">
      <path d="M19.5 14.5A8 8 0 0 1 9.5 4.5a8 8 0 1 0 10 10z" />
    </template>
    <template v-else-if="name === 'keys'">
      <rect x="2.5" y="6" width="19" height="12" rx="1.5" />
      <path d="M6 9.5h1M9.5 9.5h1M13.5 9.5h1M17 9.5h1M6 12.5h1M17 12.5h1M9.5 12.5h5M7.5 15h9" />
    </template>
    <template v-else-if="name === 'notebook'">
      <path d="M6 3.5h12.5v17H6A1.5 1.5 0 0 1 4.5 19V5A1.5 1.5 0 0 1 6 3.5z" />
      <path d="M4.5 17.5A1.5 1.5 0 0 1 6 16h12.5" />
      <path d="M9 7.5h6M9 10.5h4" />
    </template>
    <template v-else-if="name === 'combat'">
      <path d="M4.5 4.5l10 10M19.5 4.5l-10 10" />
      <path d="M12.5 17.5l5-5M6.5 12.5l5 5" />
      <path d="M16 16l3.5 3.5M8 16l-3.5 3.5" />
    </template>
    <template v-else-if="name === 'lock'">
      <rect x="5" y="10.5" width="14" height="10" rx="1.5" />
      <path d="M8 10.5V7.5a4 4 0 0 1 8 0v3" />
      <path d="M12 14.5v2" />
    </template>
    <template v-else-if="name === 'unlock'">
      <rect x="5" y="10.5" width="14" height="10" rx="1.5" />
      <path d="M8 10.5V7.5a4 4 0 0 1 7.7-1.5" />
      <path d="M12 14.5v2" />
    </template>
    <template v-else-if="name === 'more'">
      <path d="M5.5 12h.01M12 12h.01M18.5 12h.01" stroke-width="3" />
    </template>
    <template v-else-if="name === 'chevron-right'">
      <path d="M9.5 6l6 6-6 6" />
    </template>
    <template v-else-if="name === 'chevron-down'">
      <path d="M6 9.5l6 6 6-6" />
    </template>
    <template v-else-if="name === 'image'">
      <rect x="3.5" y="4.5" width="17" height="15" rx="1.5" />
      <circle cx="9" cy="9.5" r="1.75" />
      <path d="M3.5 17l5-5 4 4 2.5-2.5 5.5 5.5" />
    </template>
    <template v-else>
      <path d="M6 6l12 12M18 6L6 18" />
    </template>
  </svg>
</template>

<style scoped>
.icon {
  display: block;
  flex: none;
}
</style>

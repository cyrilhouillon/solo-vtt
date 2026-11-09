<script setup lang="ts">
import type { RouteLocationNormalizedLoaded } from 'vue-router'

/**
 * Un écran par campagne : passer d'une page du Carnet à l'autre garde le Carnet monté, changer de campagne ou d'écran le
 * remonte, et il relit alors la campagne au serveur.
 */
const screenKey = (route: RouteLocationNormalizedLoaded) =>
  `${String(route.name)}-${route.params.campaignId ?? ''}-${route.params.combatId ?? ''}`
</script>

<template>
  <RouterView v-slot="{ Component, route }">
    <component :is="Component" :key="screenKey(route)" />
  </RouterView>
</template>

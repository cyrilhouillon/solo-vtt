<script setup lang="ts">
import { ref } from 'vue'
import GameScreen from './components/GameScreen.vue'
import HomeScreen from './components/HomeScreen.vue'
import NotebookScreen from './components/NotebookScreen.vue'
import type { CampaignView } from './types'

/** La campagne ouverte ; `null` sur l'accueil, qui est toujours l'écran d'ouverture. */
const campaign = ref<CampaignView | null>(null)
/** L'écran de la campagne : son Carnet d'abord, le combat quand le joueur l'ouvre. */
const screen = ref<'notebook' | 'combat'>('notebook')

function open(opened: CampaignView) {
  campaign.value = opened
  screen.value = 'notebook'
}
</script>

<template>
  <HomeScreen v-if="!campaign" @open="open" />
  <!-- Chaque écran relit la campagne au serveur en se montant : passer de l'un à l'autre ne garde rien en mémoire. -->
  <NotebookScreen
    v-else-if="screen === 'notebook'"
    :key="`notebook-${campaign.id}`"
    :campaign="campaign"
    @home="campaign = null"
    @combat="screen = 'combat'"
  />
  <GameScreen v-else :key="`combat-${campaign.id}`" :campaign="campaign" @home="campaign = null" @notebook="screen = 'notebook'" />
</template>

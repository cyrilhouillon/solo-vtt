<script setup lang="ts">
import { ref } from 'vue'
import GameScreen from './components/GameScreen.vue'
import HomeScreen from './components/HomeScreen.vue'
import type { CampaignView } from './types'

/** La campagne où l'on joue ; `null` sur l'accueil, qui est toujours l'écran d'ouverture. */
const campaign = ref<CampaignView | null>(null)
/** Pourquoi l'on est revenu à l'accueil sans l'avoir demandé. */
const homeMessage = ref<string | null>(null)

function goHome(message?: string) {
  homeMessage.value = message ?? null
  campaign.value = null
}

function open(opened: CampaignView) {
  homeMessage.value = null
  campaign.value = opened
}
</script>

<template>
  <HomeScreen v-if="!campaign" :message="homeMessage" @open="open" />
  <!-- Changer de campagne remonte l'écran de jeu : son état repart de zéro. -->
  <GameScreen v-else :key="campaign.id" :campaign="campaign" @home="goHome" />
</template>

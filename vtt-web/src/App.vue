<script setup lang="ts">
import { ref } from 'vue'
import GameScreen from './components/GameScreen.vue'
import HomeScreen from './components/HomeScreen.vue'
import type { CampaignView } from './types'

/** La campagne où l'on joue ; `null` sur l'accueil, qui est toujours l'écran d'ouverture. */
const campaign = ref<CampaignView | null>(null)
</script>

<template>
  <HomeScreen v-if="!campaign" @open="campaign = $event" />
  <!-- Changer de campagne remonte l'écran de jeu : son état repart de zéro. -->
  <GameScreen v-else :key="campaign.id" :campaign="campaign" @home="campaign = null" />
</template>

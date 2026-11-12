<script setup lang="ts">
import { computed } from 'vue'
import { imageUrl } from '../api'
import type { SceneView } from '../types'

/**
 * La carte d'un combat en petit : son image de fond (le sol de la carte sans image) et une pastille par créature, à sa
 * place ; une créature à terre est barrée. Une image statique, sans Konva : rien ne s'y joue.
 */
const props = defineProps<{ scene: SceneView }>()

/** Le rayon d'une pastille : une demi-case, comme un jeton, en fraction de la largeur de la carte. */
const radius = computed(() => {
  const cell = props.scene.unit === 'FOOT' ? 5 : 1.5
  return (cell / 2) * (100 / props.scene.width)
})
</script>

<template>
  <svg
    class="thumbnail"
    :viewBox="`0 0 100 ${(100 * scene.height) / scene.width}`"
    role="img"
    :aria-label="`Carte du combat : ${scene.tokens.map((t) => t.name).join(', ') || 'aucune créature'}`"
  >
    <rect class="ground" x="0" y="0" width="100" :height="(100 * scene.height) / scene.width" />
    <image
      v-if="scene.imageId"
      :href="imageUrl(scene.imageId)"
      x="0"
      y="0"
      width="100"
      :height="(100 * scene.height) / scene.width"
      preserveAspectRatio="none"
    />
    <g v-for="token in scene.tokens" :key="token.id" :class="['token', token.kind === 'CHARACTER' ? 'character' : 'monster']">
      <circle :cx="(100 * token.x) / scene.width" :cy="(100 * token.y) / scene.width" :r="radius" />
      <line
        v-if="token.hitPoints === 0"
        class="down"
        :x1="(100 * token.x) / scene.width - radius * 0.7"
        :y1="(100 * token.y) / scene.width - radius * 0.7"
        :x2="(100 * token.x) / scene.width + radius * 0.7"
        :y2="(100 * token.y) / scene.width + radius * 0.7"
      />
    </g>
  </svg>
</template>

<style scoped>
.thumbnail {
  display: block;
  width: 100%;
  max-height: 60vh;
  border: 1px solid var(--map-border);
  border-radius: 4px;
}
.ground {
  fill: var(--map-ground);
}
.token circle {
  stroke: var(--token-rim);
  stroke-width: 0.3;
}
.character circle {
  fill: var(--token-character);
}
.monster circle {
  fill: var(--token-monster);
}
.down {
  stroke: var(--map-down);
  stroke-width: 0.5;
  stroke-linecap: round;
}
</style>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { SceneCanvas } from '../map/SceneCanvas'
import { formatLength, type CreatureKind, type SceneView } from '../types'

const props = defineProps<{ scene: SceneView; selected: string[]; placing: CreatureKind | null }>()
const emit = defineEmits<{ backgroundClick: [x: number, y: number, free: boolean]; tokenClick: [tokenId: string] }>()

/** Le canvas est muet pour un lecteur d'écran : la liste « Sur la carte » en est le double accessible. */
const label = computed(() => {
  const { width, height, unitSymbol, tokens } = props.scene
  const n = tokens.length
  return `Carte de ${formatLength(width, unitSymbol)} sur ${formatLength(height, unitSymbol)}, ${n} ${n > 1 ? 'créatures' : 'créature'}`
})

const container = ref<HTMLDivElement | null>(null)
let canvas: SceneCanvas | null = null
let observer: ResizeObserver | null = null

onMounted(() => {
  const el = container.value!
  canvas = new SceneCanvas(el, {
    onBackgroundClick: (x, y, free) => emit('backgroundClick', x, y, free),
    onTokenClick: (id) => emit('tokenClick', id),
  })
  canvas.render({ scene: props.scene, selected: props.selected, placing: props.placing })
  observer = new ResizeObserver(() => canvas?.resize(el.clientWidth, el.clientHeight))
  observer.observe(el)
})

watch(
  () => [props.scene, props.selected, props.placing] as const,
  ([scene, selected, placing]) => canvas?.render({ scene, selected, placing }),
  { deep: true },
)

onBeforeUnmount(() => {
  observer?.disconnect()
  canvas?.destroy()
})
</script>

<template>
  <div ref="container" class="map" :class="{ placing }" role="img" :aria-label="label" />
</template>

<style scoped>
.map {
  width: 100%;
  height: 100%;
  min-height: 0;
}
.map.placing {
  cursor: crosshair;
}
</style>

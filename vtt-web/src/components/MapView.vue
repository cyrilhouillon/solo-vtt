<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { SceneCanvas } from '../map/SceneCanvas'
import type { SceneView } from '../types'

const props = defineProps<{ scene: SceneView; selected: string[]; placing: boolean }>()
const emit = defineEmits<{ backgroundClick: [x: number, y: number]; tokenClick: [tokenId: string] }>()

const container = ref<HTMLDivElement | null>(null)
let canvas: SceneCanvas | null = null
let observer: ResizeObserver | null = null

onMounted(() => {
  const el = container.value!
  canvas = new SceneCanvas(el, {
    onBackgroundClick: (x, y) => emit('backgroundClick', x, y),
    onTokenClick: (id) => emit('tokenClick', id),
  })
  canvas.render({ scene: props.scene, selected: props.selected })
  observer = new ResizeObserver(() => canvas?.resize(el.clientWidth, el.clientHeight))
  observer.observe(el)
})

watch(
  () => [props.scene, props.selected] as const,
  ([scene, selected]) => canvas?.render({ scene, selected }),
  { deep: true },
)

onBeforeUnmount(() => {
  observer?.disconnect()
  canvas?.destroy()
})
</script>

<template>
  <div ref="container" class="map" :class="{ placing }" />
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

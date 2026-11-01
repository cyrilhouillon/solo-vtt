<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { CELL, occupant, snapToCell, type Point } from '../map/grid'
import { SceneCanvas, type Targeting } from '../map/SceneCanvas'
import { formatLength, type CreatureKind, type SceneView } from '../types'

const props = defineProps<{
  scene: SceneView
  selected: string[]
  placing: CreatureKind | null
  targeting: Targeting | null
  /** Thème courant : la carte relit ses couleurs quand il change. */
  theme: string
}>()
const emit = defineEmits<{ backgroundClick: [x: number, y: number, free: boolean]; tokenClick: [tokenId: string] }>()

/** Le canvas est muet pour un lecteur d'écran : la liste « Sur la carte » en est le double accessible. */
const label = computed(() => {
  const { width, height, unitSymbol, tokens } = props.scene
  const n = tokens.length
  const summary = `Carte de ${formatLength(width, unitSymbol)} sur ${formatLength(height, unitSymbol)}, ${n} ${n > 1 ? 'créatures' : 'créature'}`
  if (props.targeting) return `${summary}. Ciblage : choisir la cible dans le tableau « Sur la carte ».`
  return props.placing ? `${summary}. Flèches pour choisir une case, Entrée pour poser.` : summary
})

/** Case visée au clavier pendant le placement. */
const cursor = ref<Point | null>(null)
const cursorAnnouncement = ref('')

const container = ref<HTMLDivElement | null>(null)
let canvas: SceneCanvas | null = null
let observer: ResizeObserver | null = null

function render() {
  canvas?.render({
    scene: props.scene,
    selected: props.selected,
    placing: props.placing,
    cursor: cursor.value,
    targeting: props.targeting,
  })
}

onMounted(() => {
  const el = container.value!
  canvas = new SceneCanvas(el, {
    onBackgroundClick: (x, y, free) => emit('backgroundClick', x, y, free),
    onTokenClick: (id) => emit('tokenClick', id),
  })
  render()
  // Le canvas ne redessine pas seul quand les polices finissent de charger.
  void document.fonts.ready.then(render)
  observer = new ResizeObserver(() => canvas?.resize(el.clientWidth, el.clientHeight))
  observer.observe(el)
})

watch(() => [props.scene, props.selected, props.placing, props.targeting, props.theme, cursor.value], render, { deep: true })
watch(
  () => props.placing,
  (placing) => {
    if (!placing) cursor.value = null
  },
)

function announce(point: Point) {
  const cell = CELL[props.scene.unit]
  const taken = occupant(props.scene, point)
  const where = `Colonne ${Math.floor(point.x / cell) + 1}, ligne ${Math.floor(point.y / cell) + 1}`
  cursorAnnouncement.value = taken ? `${where}, occupée par ${taken.name}` : `${where}, libre`
}

function startCursor() {
  cursor.value = snapToCell(props.scene, { x: props.scene.width / 2, y: props.scene.height / 2 })
  announce(cursor.value)
}

/**
 * Le curseur de case n'apparaît qu'au focus clavier. Au clic, la carte prend aussi le focus :
 * la redessiner à ce moment détruirait le sol sous la souris entre l'appui et le relâchement,
 * et Konva perdrait le clic de pose.
 */
function onFocus(e: FocusEvent) {
  if (!props.placing || cursor.value) return
  if ((e.target as HTMLElement).matches(':focus-visible')) startCursor()
}

const STEPS: Record<string, [number, number]> = {
  ArrowLeft: [-1, 0],
  ArrowRight: [1, 0],
  ArrowUp: [0, -1],
  ArrowDown: [0, 1],
}

function onKeydown(e: KeyboardEvent) {
  if (!props.placing) return
  const step = STEPS[e.key]
  if (!cursor.value) {
    // Focus pris à la souris : la première flèche fait apparaître le curseur.
    if (step) {
      e.preventDefault()
      startCursor()
    }
    return
  }
  if (step) {
    e.preventDefault()
    const cell = CELL[props.scene.unit]
    const { width, height } = props.scene
    const next = {
      x: Math.min(width, Math.max(0, cursor.value.x + step[0] * cell)),
      y: Math.min(height, Math.max(0, cursor.value.y + step[1] * cell)),
    }
    cursor.value = snapToCell(props.scene, next)
    announce(cursor.value)
  } else if (e.key === 'Enter' || e.key === ' ') {
    e.preventDefault()
    emit('backgroundClick', cursor.value.x, cursor.value.y, false)
  }
}

onBeforeUnmount(() => {
  observer?.disconnect()
  canvas?.destroy()
})
</script>

<template>
  <div
    ref="container"
    class="map"
    :class="{ placing, targeting: targeting !== null }"
    :role="placing ? 'application' : 'img'"
    :tabindex="placing ? 0 : -1"
    :aria-label="label"
    @focus="onFocus"
    @blur="cursor = null"
    @keydown="onKeydown"
  />
  <p class="sr-only" aria-live="polite">{{ cursorAnnouncement }}</p>
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
.map:focus-visible {
  outline-offset: -2px;
}
/* Le focus pris à la souris (clic sur la carte) ne laisse pas de contour. */
.map:focus:not(:focus-visible) {
  outline: none;
}
</style>

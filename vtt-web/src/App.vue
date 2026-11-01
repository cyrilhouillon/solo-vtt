<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { api, ApiError } from './api'
import CatalogPanel from './components/CatalogPanel.vue'
import CreatureSheet from './components/CreatureSheet.vue'
import MapView from './components/MapView.vue'
import NewMapForm from './components/NewMapForm.vue'
import { formatLength, type CatalogEntry, type CreatureSheet as Sheet, type DistanceUnit, type SceneView } from './types'

const catalog = ref<CatalogEntry[]>([])
const scene = ref<SceneView | null>(null)
const armed = ref<string | null>(null)
const selected = ref<string[]>([])
const sheet = ref<Sheet | null>(null)
const error = ref<string | null>(null)

/**
 * Mesure d'affichage, calculée sur les positions déjà connues : euclidienne de centre à centre,
 * dans l'unité de la carte. Une règle qui dépend d'une distance (portée, déplacement) la fait
 * calculer par le serveur, jamais par ce résultat.
 */
const measure = computed(() => {
  if (!scene.value || selected.value.length !== 2) return null
  const [a, b] = selected.value.map((id) => scene.value!.tokens.find((t) => t.id === id))
  if (!a || !b) return null
  return { from: a.name, to: b.name, value: Math.hypot(b.x - a.x, b.y - a.y) }
})

async function attempt(action: () => Promise<void>) {
  error.value = null
  try {
    await action()
  } catch (e) {
    error.value = e instanceof Error ? e.message : String(e)
  }
}

onMounted(() =>
  attempt(async () => {
    catalog.value = await api.catalog()
    try {
      scene.value = await api.scene()
    } catch (e) {
      if (!(e instanceof ApiError && e.status === 409)) throw e
    }
  }),
)

function createScene(width: number, height: number, unit: DistanceUnit) {
  attempt(async () => {
    scene.value = await api.createScene(width, height, unit)
    selected.value = []
    sheet.value = null
  })
}

function onBackgroundClick(x: number, y: number) {
  if (!armed.value) {
    selected.value = []
    return
  }
  const entryId = armed.value
  attempt(async () => {
    const token = await api.placeToken(entryId, x, y)
    scene.value?.tokens.push(token)
  })
}

function onTokenClick(tokenId: string) {
  const current = selected.value
  selected.value = current.includes(tokenId)
    ? current.filter((id) => id !== tokenId)
    : [...current, tokenId].slice(-2)
  attempt(async () => {
    const last = selected.value.at(-1)
    sheet.value = last ? await api.sheet(last) : null
  })
}

function onKey(e: KeyboardEvent) {
  if (e.key === 'Escape') armed.value = null
}
onMounted(() => window.addEventListener('keydown', onKey))
onBeforeUnmount(() => window.removeEventListener('keydown', onKey))
</script>

<template>
  <div class="layout">
    <header class="bar">
      <NewMapForm @create="createScene" />
      <p v-if="error" class="error" role="alert">{{ error }}</p>
    </header>

    <aside class="panel catalog-panel">
      <CatalogPanel :entries="catalog" :armed="armed" :disabled="!scene" @arm="armed = $event" />
    </aside>

    <main class="map-area">
      <template v-if="scene">
        <div class="map-frame">
          <MapView
            :scene="scene"
            :selected="selected"
            :placing="armed !== null"
            @background-click="onBackgroundClick"
            @token-click="onTokenClick"
          />
        </div>
        <p class="status" aria-live="polite">
          <template v-if="measure">
            Distance {{ measure.from }} – {{ measure.to }} :
            <strong>{{ formatLength(measure.value, scene.unitSymbol) }}</strong>
          </template>
          <template v-else>
            Carte de {{ formatLength(scene.width, scene.unitSymbol) }} × {{ formatLength(scene.height, scene.unitSymbol) }}.
            Sélectionner deux créatures pour mesurer leur distance.
          </template>
        </p>
      </template>
      <p v-else class="empty">Aucune carte. Créer une carte vide pour préparer la scène.</p>
    </main>

    <aside class="panel sheet-panel">
      <CreatureSheet :sheet="sheet" />
    </aside>
  </div>
</template>

<style scoped>
.layout {
  height: 100vh;
  display: grid;
  grid-template-columns: 15rem 1fr 18rem;
  grid-template-rows: auto 1fr;
  grid-template-areas:
    'bar bar bar'
    'catalog map sheet';
}
.bar {
  grid-area: bar;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 0.75rem 1rem;
  border-bottom: 1px solid var(--border);
  background: var(--surface);
}
.panel {
  padding: 1rem;
  background: var(--surface);
  overflow-y: auto;
}
.catalog-panel {
  grid-area: catalog;
  border-right: 1px solid var(--border);
  display: flex;
  flex-direction: column;
}
.catalog-panel > * {
  flex: 1;
}
.sheet-panel {
  grid-area: sheet;
  border-left: 1px solid var(--border);
}
.map-area {
  grid-area: map;
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
}
.map-frame {
  flex: 1;
  min-height: 0;
}
.status {
  margin: 0;
  padding: 0.6rem 1rem;
  border-top: 1px solid var(--border);
  color: var(--text-muted);
  font-variant-numeric: tabular-nums;
}
.status strong {
  color: var(--text);
}
.empty {
  margin: auto;
  color: var(--text-muted);
}
.error {
  margin: 0;
  color: var(--danger);
}
@media (max-width: 900px) {
  .layout {
    height: auto;
    min-height: 100vh;
    grid-template-columns: 1fr;
    grid-template-rows: auto auto 70vh auto;
    grid-template-areas: 'bar' 'catalog' 'map' 'sheet';
  }
  .catalog-panel,
  .sheet-panel {
    border: 0;
    border-bottom: 1px solid var(--border);
  }
}
</style>

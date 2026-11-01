<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { api, ApiError } from './api'
import CatalogPanel from './components/CatalogPanel.vue'
import CreatureSheet from './components/CreatureSheet.vue'
import MapView from './components/MapView.vue'
import NewMapForm from './components/NewMapForm.vue'
import TokenList from './components/TokenList.vue'
import { CELL, occupant } from './map/grid'
import { formatLength, type CatalogEntry, type CreatureSheet as Sheet, type DistanceUnit, type SceneView } from './types'

const catalog = ref<CatalogEntry[]>([])
const scene = ref<SceneView | null>(null)
const armed = ref<string | null>(null)
const selected = ref<string[]>([])
const sheet = ref<Sheet | null>(null)
const error = ref<string | null>(null)
/** Dernière action, lue par les lecteurs d'écran. */
const announcement = ref('')
/** Jetons posés depuis l'ouverture, du plus ancien au plus récent : Ctrl+Z retire le dernier. */
const placed: string[] = []

const armedEntry = computed(() => catalog.value.find((e) => e.id === armed.value) ?? null)

/**
 * Mesure d'affichage, calculée sur les positions déjà connues : euclidienne de centre à centre,
 * dans l'unité de la carte. Une règle qui dépend d'une distance (portée, déplacement) la fait
 * calculer par le serveur, jamais par ce résultat.
 */
const measure = computed(() => {
  if (!scene.value || selected.value.length !== 2) return null
  const [a, b] = selected.value.map((id) => scene.value!.tokens.find((t) => t.id === id))
  if (!a || !b) return null
  const value = Math.hypot(b.x - a.x, b.y - a.y)
  return { from: a.name, to: b.name, value, cells: value / CELL[scene.value.unit] }
})

const cellFormat = new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 1 })

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
    const created = await api.createScene(width, height, unit)
    scene.value = created
    armed.value = null
    clearSelection()
    placed.length = 0
    announcement.value = `Nouvelle carte de ${formatLength(created.width, created.unitSymbol)} sur ${formatLength(created.height, created.unitSymbol)}.`
  })
}

function onBackgroundClick(x: number, y: number, free: boolean) {
  if (!armed.value) {
    clearSelection()
    return
  }
  const taken = free || !scene.value ? undefined : occupant(scene.value, { x, y })
  if (taken) {
    error.value = `Case occupée par ${taken.name}. Maj + clic pour poser sans caler sur la case.`
    return
  }
  const entryId = armed.value
  attempt(async () => {
    const token = await api.placeToken(entryId, x, y)
    scene.value?.tokens.push(token)
    placed.push(token.id)
    announcement.value = `${token.name} posé.`
  })
}

function toggleSelection(tokenId: string) {
  const current = selected.value
  selected.value = current.includes(tokenId)
    ? current.filter((id) => id !== tokenId)
    : [...current, tokenId].slice(-2)
  showSheet(selected.value.at(-1) ?? null)
}

function clearSelection() {
  selected.value = []
  sheet.value = null
}

function showSheet(tokenId: string | null) {
  attempt(async () => {
    sheet.value = tokenId ? await api.sheet(tokenId) : null
  })
}

function removeTokens(ids: string[]) {
  if (!ids.length) return
  const targets = [...ids]
  attempt(async () => {
    const names: string[] = []
    for (const id of targets) {
      await api.removeToken(id)
      const tokens = scene.value?.tokens ?? []
      const index = tokens.findIndex((t) => t.id === id)
      if (index >= 0) names.push(tokens.splice(index, 1)[0].name)
      const p = placed.indexOf(id)
      if (p >= 0) placed.splice(p, 1)
    }
    selected.value = selected.value.filter((id) => !targets.includes(id))
    if (sheet.value && targets.includes(sheet.value.id)) showSheet(selected.value.at(-1) ?? null)
    announcement.value = `${names.join(' et ')} ${names.length > 1 ? 'retirés' : 'retiré'}.`
  })
}

function undoLastPlacement() {
  const last = placed.at(-1)
  if (last) removeTokens([last])
}

function isTyping(target: EventTarget | null): boolean {
  return (
    target instanceof HTMLElement &&
    (target.isContentEditable || ['INPUT', 'TEXTAREA', 'SELECT'].includes(target.tagName))
  )
}

function onKey(e: KeyboardEvent) {
  if (e.key === 'Escape') {
    // Un pas à la fois : d'abord le placement, puis la sélection.
    if (armed.value) armed.value = null
    else clearSelection()
    error.value = null
    return
  }
  if (isTyping(e.target) || !scene.value) return
  if (e.key === 'Delete' || e.key === 'Backspace') {
    e.preventDefault()
    removeTokens(selected.value)
  } else if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'z') {
    e.preventDefault()
    undoLastPlacement()
  }
}

onMounted(() => window.addEventListener('keydown', onKey))
onBeforeUnmount(() => window.removeEventListener('keydown', onKey))
</script>

<template>
  <div class="layout">
    <header class="bar">
      <NewMapForm :token-count="scene ? scene.tokens.length : null" @create="createScene" />
    </header>

    <aside class="panel catalog-panel">
      <CatalogPanel :entries="catalog" :armed="armed" :disabled="!scene" @arm="armed = $event" />
    </aside>

    <main class="map-area">
      <div v-if="error" class="error" role="alert">
        <p>{{ error }}</p>
        <button type="button" @click="error = null">Fermer</button>
      </div>
      <template v-if="scene">
        <div class="map-frame">
          <MapView
            :scene="scene"
            :selected="selected"
            :placing="armedEntry?.kind ?? null"
            @background-click="onBackgroundClick"
            @token-click="toggleSelection"
          />
        </div>
        <p class="status">
          <template v-if="armedEntry">
            Placement : <strong>{{ armedEntry.name }}</strong>. Cliquer sur une case ; Maj + clic pour poser
            librement, Ctrl+Z pour annuler, Échap pour arrêter.
          </template>
          <template v-else-if="measure">
            Distance {{ measure.from }} – {{ measure.to }} :
            <strong>{{ formatLength(measure.value, scene.unitSymbol) }}</strong>
            ({{ cellFormat.format(measure.cells) }} {{ measure.cells < 2 ? 'case' : 'cases' }})
          </template>
          <template v-else>
            Carte de {{ formatLength(scene.width, scene.unitSymbol) }} ×
            {{ formatLength(scene.height, scene.unitSymbol) }}. Sélectionner deux créatures pour mesurer leur
            distance.
          </template>
        </p>
      </template>
      <p v-else class="empty">Aucune carte. Créer une carte vide pour préparer la scène.</p>
      <p class="sr-only" aria-live="polite">{{ announcement }}</p>
    </main>

    <aside class="panel sheet-panel">
      <TokenList
        v-if="scene"
        :tokens="scene.tokens"
        :selected="selected"
        :shown="sheet?.id ?? null"
        @select="toggleSelection"
        @remove="removeTokens([$event])"
      />
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
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}
.map-area {
  grid-area: map;
  position: relative;
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
/* Par-dessus la carte : l'apparition d'une erreur ne la redimensionne pas. */
.error {
  position: absolute;
  inset: 0 0 auto;
  z-index: 1;
  background: var(--surface);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.5rem 1rem;
  border-bottom: 1px solid var(--danger);
  color: var(--danger);
}
.error p {
  margin: 0;
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

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { api, ApiError } from './api'
import CatalogPanel from './components/CatalogPanel.vue'
import CreatureSheet from './components/CreatureSheet.vue'
import MapView from './components/MapView.vue'
import NewMapForm from './components/NewMapForm.vue'
import TokenList from './components/TokenList.vue'
import { CELL, occupant } from './map/grid'
import {
  formatLength,
  type CatalogEntry,
  type CreatureSheet as Sheet,
  type DistanceUnit,
  type SceneView,
  type TokenView,
} from './types'

const catalog = ref<CatalogEntry[]>([])
const scene = ref<SceneView | null>(null)
const armed = ref<string | null>(null)
const selected = ref<string[]>([])
const sheet = ref<Sheet | null>(null)
const error = ref<string | null>(null)
/** Résultat de la dernière action, affiché dans la barre d'état et lu par les lecteurs d'écran. */
const notice = ref('')
/** Ce que lit le lecteur d'écran : la dernière action, ou la distance mesurée. */
const announcement = ref('')

/** Une étape annulable par Ctrl+Z : une pose, ou un retrait (de quoi reposer les créatures). */
type Step = { type: 'place'; tokenId: string } | { type: 'remove'; tokens: TokenView[] }
/** Étapes depuis l'ouverture de la page, de la plus ancienne à la plus récente. */
const history: Step[] = []
let pendingCount = 0

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

function cellsLabel(cells: number): string {
  return `${cellFormat.format(cells)} ${cells < 2 ? 'case' : 'cases'}`
}

watch(measure, (m) => {
  if (m && scene.value) {
    announcement.value = `Distance ${m.from} – ${m.to} : ${formatLength(m.value, scene.value.unitSymbol)}, ${cellsLabel(m.cells)}.`
  }
})

function tell(message: string) {
  notice.value = message
  announcement.value = message
}

/** La scène du serveur, ou null s'il n'a pas encore de carte (409). */
async function loadScene(): Promise<SceneView | null> {
  try {
    return await api.scene()
  } catch (e) {
    if (e instanceof ApiError && e.status === 409) return null
    throw e
  }
}

/**
 * Un 404 ou un 409 signifie que la carte affichée n'est plus celle du serveur (autre onglet,
 * redémarrage) : on recharge la sienne et on oublie ce qui visait l'ancienne.
 */
async function resync(): Promise<string> {
  const fresh = await loadScene()
  scene.value = fresh
  history.length = 0
  if (!fresh) {
    arm(null)
    clearSelection()
    return "Le serveur n'a plus de carte : en créer une nouvelle."
  }
  const ids = new Set(fresh.tokens.map((t) => t.id))
  selected.value = selected.value.filter((id) => ids.has(id))
  if (sheet.value && !ids.has(sheet.value.id)) sheet.value = null
  return "La carte a changé ailleurs (autre onglet ?) : elle vient d'être rechargée. Refaire l'action si besoin."
}

async function attempt(action: () => Promise<void>) {
  error.value = null
  try {
    await action()
  } catch (e) {
    if (e instanceof ApiError && (e.status === 404 || e.status === 409)) {
      try {
        error.value = await resync()
      } catch (reload) {
        error.value = reload instanceof Error ? reload.message : String(reload)
      }
      return
    }
    error.value = e instanceof Error ? e.message : String(e)
  }
}

onMounted(() =>
  attempt(async () => {
    catalog.value = await api.catalog()
    scene.value = await loadScene()
  }),
)

function createScene(width: number, height: number, unit: DistanceUnit) {
  attempt(async () => {
    const created = await api.createScene(width, height, unit)
    scene.value = created
    armed.value = null
    clearSelection()
    history.length = 0
    tell(`Nouvelle carte de ${formatLength(created.width, created.unitSymbol)} sur ${formatLength(created.height, created.unitSymbol)}.`)
  })
}

async function placeToken(catalogId: string, x: number, y: number): Promise<TokenView> {
  const token = await api.placeToken(catalogId, x, y)
  scene.value?.tokens.push(token)
  return token
}

/** Retire les jetons côté serveur puis à l'écran ; rend ceux qui ont été retirés. */
async function deleteTokens(ids: string[]): Promise<TokenView[]> {
  const removed: TokenView[] = []
  // Un jeton en attente n'existe pas encore côté serveur : il n'y a rien à retirer.
  const known = ids.filter((id) => !scene.value?.tokens.find((t) => t.id === id)?.pending)
  for (const id of known) {
    await api.removeToken(id)
    const tokens = scene.value?.tokens ?? []
    const index = tokens.findIndex((t) => t.id === id)
    if (index >= 0) removed.push(tokens.splice(index, 1)[0])
  }
  selected.value = selected.value.filter((id) => !ids.includes(id))
  if (sheet.value && ids.includes(sheet.value.id)) showSheet(selected.value.at(-1) ?? null)
  return removed
}

function names(tokens: TokenView[]): string {
  return tokens.map((t) => t.name).join(' et ')
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
  const entry = armedEntry.value
  const tokens = scene.value?.tokens
  if (!entry || !tokens) return
  // Le jeton s'affiche tout de suite ; la réponse du serveur le remplace, ou l'efface en cas d'échec.
  const pending: TokenView = {
    id: `en-attente-${++pendingCount}`,
    creatureId: '',
    catalogId: entry.id,
    name: entry.name,
    kind: entry.kind,
    x,
    y,
    pending: true,
  }
  tokens.push(pending)
  const settle = (token?: TokenView) => {
    const index = tokens.findIndex((t) => t.id === pending.id)
    if (index < 0) return
    if (token) tokens.splice(index, 1, token)
    else tokens.splice(index, 1)
  }
  attempt(async () => {
    try {
      const token = await api.placeToken(entry.id, x, y)
      settle(token)
      history.push({ type: 'place', tokenId: token.id })
      tell(`${token.name} posé. Ctrl+Z pour annuler.`)
    } catch (e) {
      settle()
      throw e
    }
  })
}

function removeTokens(ids: string[]) {
  if (!ids.length) return
  const targets = [...ids]
  attempt(async () => {
    const removed = await deleteTokens(targets)
    if (!removed.length) return
    history.push({ type: 'remove', tokens: removed })
    tell(`${names(removed)} ${removed.length > 1 ? 'retirés' : 'retiré'}. Ctrl+Z pour annuler.`)
  })
}

/**
 * Annule la dernière pose ou le dernier retrait. Une créature reposée est recréée depuis
 * le catalogue, à la même position : le serveur lui donne un nouvel id, que les étapes
 * plus anciennes suivent.
 */
function undo() {
  const step = history.pop()
  if (!step) {
    tell('Rien à annuler.')
    return
  }
  attempt(async () => {
    if (step.type === 'place') {
      const removed = await deleteTokens([step.tokenId])
      tell(removed.length ? `Pose de ${names(removed)} annulée.` : 'Rien à annuler.')
      return
    }
    const restored: TokenView[] = []
    for (const old of step.tokens) {
      const token = await placeToken(old.catalogId, old.x, old.y)
      restored.push(token)
      for (const s of history) if (s.type === 'place' && s.tokenId === old.id) s.tokenId = token.id
    }
    tell(`${names(restored)} ${restored.length > 1 ? 'reposés' : 'reposé'}.`)
  })
}

// Un nouveau contexte (créature à poser, sélection) rend le message précédent caduc.
function arm(entryId: string | null) {
  armed.value = entryId
  notice.value = ''
}

function toggleSelection(tokenId: string) {
  if (scene.value?.tokens.find((t) => t.id === tokenId)?.pending) return
  notice.value = ''
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

/** Seule la réponse à la dernière demande s'affiche : des sélections rapides ne se doublent pas. */
let sheetRequest = 0
function showSheet(tokenId: string | null) {
  const request = ++sheetRequest
  attempt(async () => {
    const loaded = tokenId ? await api.sheet(tokenId) : null
    if (request === sheetRequest) sheet.value = loaded
  })
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
    if (armed.value) arm(null)
    else clearSelection()
    notice.value = ''
    error.value = null
    return
  }
  if (isTyping(e.target) || !scene.value) return
  if (e.key === 'Delete' || e.key === 'Backspace') {
    e.preventDefault()
    removeTokens(selected.value)
  } else if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'z') {
    e.preventDefault()
    undo()
  }
}

onMounted(() => window.addEventListener('keydown', onKey))
onBeforeUnmount(() => window.removeEventListener('keydown', onKey))
</script>

<template>
  <div class="layout">
    <header class="bar">
      <h1 class="app-name">Solo VTT</h1>
      <NewMapForm :token-count="scene ? scene.tokens.length : null" @create="createScene" />
    </header>

    <aside class="panel catalog-panel">
      <CatalogPanel :entries="catalog" :armed="armed" :disabled="!scene" @arm="arm" />
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
          <strong v-if="notice" class="notice">{{ notice }}</strong>
          <template v-if="armedEntry">
            Placement : <strong>{{ armedEntry.name }}</strong>. Cliquer sur une case, ou Tab jusqu'à la carte puis
            flèches et Entrée ; Maj + clic pour poser librement, Échap pour arrêter.
          </template>
          <template v-else-if="measure">
            Distance {{ measure.from }} – {{ measure.to }} :
            <strong>{{ formatLength(measure.value, scene.unitSymbol) }}</strong> ({{ cellsLabel(measure.cells) }})
          </template>
          <template v-else-if="!notice">
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
  justify-content: flex-start;
  gap: 0.75rem;
  padding: 0.75rem 1rem;
  border-bottom: 1px solid var(--border);
  background: var(--surface);
}
.app-name {
  margin: 0 1rem 0 0;
  font-size: 1.15rem;
  font-weight: 700;
  letter-spacing: -0.01em;
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
.notice {
  margin-right: 0.75rem;
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

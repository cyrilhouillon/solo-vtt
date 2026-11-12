<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { campaignApi } from '../api'
import AppIcon from './AppIcon.vue'
import ThemeButton from './ThemeButton.vue'
import CampaignColumn from './CampaignColumn.vue'
import CreatureSheet from './CreatureSheet.vue'
import DiceTray from './DiceTray.vue'
import JournalPanel from './JournalPanel.vue'
import MapView from './MapView.vue'
import TokenBoard from './TokenBoard.vue'
import { impactLabel, throwsOf, trayFromJournal } from '../dice/tray'
import { theme } from '../theme'
import {
  formatLength,
  type CampaignView,
  type CombatReplay,
  type PageSummary,
  type ReplayStep,
  type SceneView,
  type TrayResult,
} from '../types'

/**
 * Le rejeu d'un combat terminé, en lecture seule : la même table qu'en direct (carte, tableau, feuilles, journal, dés),
 * un pas par action. Avancer d'un pas rejoue ses dés ; reculer, revenir au début ou aller à la fin pose l'état d'emblée.
 * En lecture, chaque pas part deux secondes après la fin de l'animation du précédent. Rien n'est envoyé au serveur.
 */
const props = defineProps<{ campaign: CampaignView; combatId: string }>()
const router = useRouter()
const api = campaignApi(props.campaign.id)

/** Le délai entre la fin de l'animation d'un pas et le pas suivant, en lecture. */
const PLAY_DELAY_MS = 2000

const replay = ref<CombatReplay | null>(null)
/** L'entrée du Carnet du combat, pour son nom et le retour au Carnet. */
const entry = ref<PageSummary | null>(null)
const error = ref<string | null>(null)
/** Le pas montré : 0 est le départ, l'état juste avant le lancement. */
const index = ref(0)
/** La carte du pas montré ; ses PV baissent à l'arrêt des dés, comme en direct. */
const scene = ref<SceneView | null>(null)
const selected = ref<string[]>([])
/** La sélection vient du joueur : sinon la feuille montre la créature qui joue. */
const chosenByPlayer = ref(false)
const lastResult = ref<TrayResult | null>(null)
const rolling = ref(false)
const playing = ref(false)
const tab = ref<'sheet' | 'target' | 'journal'>('sheet')
const mapView = ref<InstanceType<typeof MapView> | null>(null)
/** Numéro du dernier geste : une animation dépassée par un geste plus récent ne pose plus rien. */
let gesture = 0

const steps = computed<ReplayStep[]>(() => replay.value?.steps ?? [])
const last = computed(() => Math.max(steps.value.length - 1, 0))
const step = computed<ReplayStep | null>(() => steps.value[index.value] ?? null)
const combat = computed(() => step.value?.combat ?? null)
/** Le journal jusqu'au pas montré. */
const journal = computed(() => steps.value.slice(1, index.value + 1).flatMap((s) => s.entries))
const sheetId = computed(() => (chosenByPlayer.value ? selected.value[selected.value.length - 1] : combat.value?.activeId) ?? null)
const sheet = computed(() => step.value?.sheets.find((s) => s.id === sheetId.value) ?? null)
/** La cible de la dernière attaque tranchée jusqu'ici. */
const targetSheet = computed(() => {
  const targetId = [...steps.value.slice(1, index.value + 1)].reverse().find((s) => s.targetId)?.targetId
  return step.value?.sheets.find((s) => s.id === targetId) ?? null
})
const title = computed(() => entry.value?.title ?? 'Combat')

function navigate(target: 'home' | 'notebook' | 'combat') {
  stop()
  if (target === 'home') void router.push({ name: 'home' })
  else if (target === 'combat') void router.push({ name: 'combat', params: { campaignId: props.campaign.id } })
  else if (entry.value) void router.push({ name: 'notebook', params: { campaignId: props.campaign.id, pageId: entry.value.id } })
  else void router.push({ name: 'notebook', params: { campaignId: props.campaign.id } })
}

/** Une copie de la carte du pas : l'animation baisse les PV de la cible sans toucher au rejeu. */
function copyScene(view: SceneView): SceneView {
  return { ...view, tokens: view.tokens.map((t) => ({ ...t })) }
}

/** Pose l'état du pas d'emblée : carte, PV, piste relue du journal ; les dés d'un pas abandonné s'effacent. */
function show(at: number) {
  const id = ++gesture
  mapView.value?.cancelDice()
  rolling.value = false
  index.value = Math.min(Math.max(at, 0), last.value)
  if (step.value) scene.value = copyScene(step.value.scene)
  lastResult.value = trayFromJournal(journal.value)
  return id
}

/** Avance d'un pas, comme en direct : une attaque fait rouler ses dés, puis les PV baissent et l'issue se pose. */
async function forward(): Promise<void> {
  if (index.value >= last.value) return
  const previous = step.value
  const id = show(index.value + 1)
  const current = step.value
  if (!current || current.targetId === null || current.targetHitPoints === null || current.targetMaxHitPoints === null) return
  const target = scene.value?.tokens.find((t) => t.id === current.targetId)
  const before = previous?.scene.tokens.find((t) => t.id === current.targetId)?.hitPoints
  if (target && before !== undefined) target.hitPoints = before
  const tray = trayFromJournal(journal.value, {
    id: current.targetId,
    name: target?.name ?? current.targetId,
    hitPoints: current.targetHitPoints,
    maxHitPoints: current.targetMaxHitPoints,
  })
  lastResult.value = tray
  if (!tray) return
  const thrown = throwsOf(current.entries)
  rolling.value = true
  if (thrown.attack.length) await mapView.value?.rollDice(thrown.attack, [])
  if (thrown.damage.length && id === gesture) await mapView.value?.rollDice([], thrown.damage)
  if (id !== gesture) return
  if (target) target.hitPoints = current.targetHitPoints
  mapView.value?.showImpact(current.targetId, impactLabel(tray))
  rolling.value = false
}

/** Numéro de la lecture en cours : une lecture mise en pause ne repart pas, même relancée pendant son animation. */
let playRun = 0

/** Tout geste du joueur met la lecture en pause. */
function stop() {
  playRun++
  playing.value = false
  if (pause) {
    clearTimeout(pause.timer)
    pause.resolve()
    pause = null
  }
}

let pause: { timer: ReturnType<typeof setTimeout>; resolve: () => void } | null = null
function wait(ms: number): Promise<void> {
  return new Promise((resolve) => {
    pause = { timer: setTimeout(() => ((pause = null), resolve()), ms), resolve }
  })
}

/** Lecture : un pas, puis deux secondes après la fin de son animation, le suivant, jusqu'à la fin ou la pause. */
async function play() {
  if (playing.value) {
    stop()
    return
  }
  const run = ++playRun
  if (index.value >= last.value) show(0)
  playing.value = true
  while (run === playRun && index.value < last.value) {
    await forward()
    if (run !== playRun || index.value >= last.value) break
    await wait(PLAY_DELAY_MS)
  }
  if (run === playRun) playing.value = false
}

function manual(action: () => unknown) {
  stop()
  void action()
}

function onTokenChosen(tokenId: string) {
  chosenByPlayer.value = true
  selected.value = selected.value.includes(tokenId) ? selected.value.filter((id) => id !== tokenId) : [...selected.value, tokenId].slice(-2)
  if (!selected.value.length) chosenByPlayer.value = false
  tab.value = 'sheet'
}

const measure = computed(() => {
  if (!scene.value || selected.value.length !== 2) return null
  const [a, b] = selected.value.map((id) => scene.value!.tokens.find((t) => t.id === id))
  if (!a || !b) return null
  return { from: a.name, to: b.name, value: Math.hypot(b.x - a.x, b.y - a.y) }
})

const TABS = [
  { id: 'sheet', label: 'Feuille' },
  { id: 'target', label: 'Cible' },
  { id: 'journal', label: 'Journal' },
] as const

/** Au clavier : ← et → d'un pas, Début et Fin, Espace pour lire ou mettre en pause ; jamais dans un champ. */
function onKey(e: KeyboardEvent) {
  if (e.ctrlKey || e.metaKey || e.altKey || !replay.value) return
  const target = e.target as HTMLElement | null
  if (target?.closest('input, textarea, [contenteditable="true"], [role="tablist"], table')) return
  const moves: Record<string, () => unknown> = {
    ArrowLeft: () => show(index.value - 1),
    ArrowRight: () => forward(),
    Home: () => show(0),
    End: () => show(last.value),
  }
  if (e.key === ' ' && !(target instanceof HTMLButtonElement)) {
    e.preventDefault()
    void play()
  } else if (e.key in moves) {
    e.preventDefault()
    manual(moves[e.key]!)
  }
}

onMounted(async () => {
  window.addEventListener('keydown', onKey)
  try {
    // Le Carnet ne donne que le nom du combat : sans lui, le rejeu s'ouvre sous le titre « Combat ».
    const [loaded, notebook] = await Promise.all([api.combatReplay(props.combatId), api.notebook().catch(() => null)])
    entry.value = notebook?.pages.find((p) => p.combatId === props.combatId) ?? null
    replay.value = loaded
    // Le rejeu s'ouvre sur l'état final, comme l'entrée du Carnet.
    show(loaded.steps.length - 1)
  } catch (e) {
    error.value = e instanceof Error ? e.message : String(e)
  }
})
onBeforeUnmount(() => {
  stop()
  window.removeEventListener('keydown', onKey)
})
</script>

<template>
  <div class="game-table">
    <CampaignColumn class="column" :campaign-name="campaign.name" current="combat" compact @navigate="navigate">
      <template #foot>
        <ThemeButton />
      </template>
    </CampaignColumn>

    <header class="bar">
      <p class="map-info">
        <strong class="replay-title">{{ title }}</strong> · rejeu
        <template v-if="scene">
          · Carte {{ formatLength(scene.width, scene.unitSymbol) }} × {{ formatLength(scene.height, scene.unitSymbol) }}</template>
      </p>
      <span class="spacer" />
      <p v-if="combat" class="round" aria-live="polite">Round {{ combat.round }}</p>
    </header>

    <main class="table">
      <div v-if="error" class="status error" role="alert">
        <p>{{ error }}</p>
      </div>
      <template v-if="scene">
        <TokenBoard
          :tokens="scene.tokens"
          :selected="selected"
          :attacker="null"
          :target="targetSheet?.id ?? null"
          :ranges="{}"
          :combat="combat"
          readonly
          @select="onTokenChosen"
        />
        <div class="map-frame" :style="{ '--map-ratio': `${scene.width} / ${scene.height}` }">
          <MapView
            ref="mapView"
            :scene="scene"
            :selected="selected"
            :placing="null"
            :targeting="null"
            :active-id="combat?.activeId ?? null"
            :theme="theme"
            :movable="false"
            :movement-limit="null"
            :moving="null"
            :grid-preview="null"
            @token-click="onTokenChosen"
          />
        </div>
        <div class="controls" role="group" aria-label="Rejeu du combat">
          <button type="button" class="icon-button" aria-label="Début du combat" title="Début (Début)" :disabled="index === 0" @click="manual(() => show(0))">
            <AppIcon name="first" />
          </button>
          <button type="button" class="icon-button" aria-label="Pas précédent" title="Pas précédent (←)" :disabled="index === 0" @click="manual(() => show(index - 1))">
            <AppIcon name="previous" />
          </button>
          <button
            type="button"
            class="icon-button play"
            :aria-label="playing ? 'Pause' : 'Lecture'"
            :aria-pressed="playing"
            :title="playing ? 'Pause (Espace)' : 'Lecture : un pas toutes les deux secondes (Espace)'"
            :disabled="!last"
            @click="play"
          >
            <AppIcon :name="playing ? 'pause' : 'play'" />
          </button>
          <button type="button" class="icon-button" aria-label="Pas suivant" title="Pas suivant (→)" :disabled="index >= last" @click="manual(forward)">
            <AppIcon name="next" />
          </button>
          <button type="button" class="icon-button" aria-label="Fin du combat" title="Fin (Fin)" :disabled="index >= last" @click="manual(() => show(last))">
            <AppIcon name="last" />
          </button>
          <span class="position" aria-live="polite">{{ index === 0 ? 'Départ' : `Pas ${index} / ${last}` }}</span>
          <span v-if="measure" class="measure">
            Distance {{ measure.from }} – {{ measure.to }} : <strong>{{ formatLength(measure.value, scene.unitSymbol) }}</strong>
          </span>
        </div>
        <DiceTray :result="lastResult" :rolling="rolling" />
      </template>
      <p v-else-if="!error" class="empty">Chargement du combat…</p>
    </main>

    <aside class="side" aria-label="Créatures">
      <div class="tabs" role="tablist" aria-label="Panneau">
        <button
          v-for="t in TABS"
          :key="t.id"
          type="button"
          role="tab"
          class="tab"
          :aria-selected="tab === t.id"
          @click="tab = t.id"
        >
          {{ t.label }}
        </button>
      </div>
      <div class="tab-panel" role="tabpanel" tabindex="0">
        <CreatureSheet
          v-if="tab === 'sheet'"
          :sheet="sheet"
          :actions="false"
          :movement-left="combat && combat.activeId === sheet?.id ? combat.movementLeft : null"
          empty="Sélectionner une créature sur la carte ou dans le tableau pour voir sa feuille."
        />
        <CreatureSheet
          v-else-if="tab === 'target'"
          :sheet="targetSheet"
          :actions="false"
          empty="Aucune cible pour l'instant : la créature visée par la dernière attaque s'affiche ici."
        />
        <JournalPanel v-else :entries="journal" />
      </div>
    </aside>
  </div>
</template>

<style scoped>
/* La même table qu'en direct (`.game-table`, `style.css`) ; la colonne reste en rail. */
.map-info,
.round {
  margin: 0;
  color: var(--ink-muted);
  font-variant-numeric: tabular-nums;
}
.replay-title,
.round {
  color: var(--ink);
  font-weight: 700;
}
/* Les commandes du rejeu prennent la place de la consigne, sous la carte. */
.controls {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.45rem 1rem;
  background: var(--wood);
  border-top: 1px solid var(--rule);
  color: var(--ink-muted);
  font-variant-numeric: tabular-nums;
}
.controls .play[aria-pressed='true'] {
  background: var(--brass);
  color: var(--on-brass);
}
.position {
  margin-left: 0.6rem;
  color: var(--ink);
  font-weight: 700;
}
.measure {
  margin-left: auto;
}
.measure strong {
  color: var(--ink);
}
.status {
  margin: 0;
  padding: 0.45rem 1rem;
  background: var(--wood);
}
.status.error {
  box-shadow: inset 0 2px 0 var(--danger);
  color: var(--danger);
}
.status p {
  margin: 0;
}
.empty {
  margin: auto;
  padding: 1rem;
  color: var(--map-label);
  font-family: var(--font-title);
  font-size: 1.15rem;
}
</style>

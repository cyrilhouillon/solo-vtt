<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { api, ApiError } from './api'
import AppIcon from './components/AppIcon.vue'
import CatalogPanel from './components/CatalogPanel.vue'
import CombatControl from './components/CombatControl.vue'
import CreatureSheet from './components/CreatureSheet.vue'
import DecisionDialog from './components/DecisionDialog.vue'
import DiceTray, { type TrayResult } from './components/DiceTray.vue'
import JournalPanel from './components/JournalPanel.vue'
import MapView from './components/MapView.vue'
import NewMapForm from './components/NewMapForm.vue'
import TokenBoard from './components/TokenBoard.vue'
import { CELL, occupant } from './map/grid'
import { checkMove, type MoveLimit } from './map/move'
import { rangeVerdict, type RangeVerdict } from './map/range'
import type { Targeting } from './map/SceneCanvas'
import { initialTheme, saveTheme, type Theme } from './theme'
import {
  formatLength,
  type Attack,
  type ActionResult,
  type CatalogEntry,
  type Combat,
  type CreatureSheet as Sheet,
  type Decision,
  type DistanceUnit,
  type JournalEntry,
  type SceneView,
  type TokenView,
} from './types'

const catalog = ref<CatalogEntry[]>([])
const scene = ref<SceneView | null>(null)
const armed = ref<string | null>(null)
const selected = ref<string[]>([])
const sheet = ref<Sheet | null>(null)
const error = ref<string | null>(null)
const journal = ref<JournalEntry[]>([])
/** Tiroir ouvert depuis le rail : catalogue, nouvelle carte, ou aucun. */
const drawer = ref<'catalog' | 'map' | null>(null)
/** Onglet du panneau de droite. */
const tab = ref<'sheet' | 'target' | 'journal'>('sheet')
/** La créature visée par la dernière attaque, pour l'onglet Cible. */
const targetSheet = ref<Sheet | null>(null)
/** Ce que la piste à dés montre : la dernière attaque résolue. */
const lastResult = ref<TrayResult | null>(null)
const theme = ref<Theme>(initialTheme())
/** La question du moteur qui suspend l'action en cours ; tant qu'elle attend, aucune autre action. */
const decision = ref<Decision | null>(null)
/** La réponse est envoyée et pas encore revenue : un second clic ne doit pas en envoyer une autre. */
const answering = ref(false)
/** Le combat en cours, ou `null` hors combat. */
const combat = ref<Combat | null>(null)
/** Lancement ou fin de tour envoyés et pas encore revenus. */
const combatBusy = ref(false)
/** Le jeton déplacé au clavier (touche D), ou `null`. */
const movingId = ref<string | null>(null)
/** Un déplacement est envoyé et pas encore revenu. */
const moveBusy = ref(false)
const mapView = ref<InstanceType<typeof MapView> | null>(null)

/**
 * Le seul jeton limité dans son déplacement : la créature active, quand le serveur donne ce qui lui reste.
 * `movementLeft` à `null` veut dire illimité : le client ne suppose aucune règle.
 */
const movementLimit = computed<MoveLimit | null>(() => {
  const c = combat.value
  if (!c?.activeId || c.movementLeft === null) return null
  const token = scene.value?.tokens.find((t) => t.creatureId === c.activeId)
  return token ? { tokenId: token.id, left: c.movementLeft } : null
})
/** Les jetons se glissent hors pose, ciblage, décision et déplacement en vol. */
const movable = computed(() => !armed.value && !targeting.value && !decision.value && !moveBusy.value)
const movingToken = computed(() => scene.value?.tokens.find((t) => t.id === movingId.value) ?? null)

function toggleTheme() {
  theme.value = theme.value === 'dark' ? 'light' : 'dark'
  saveTheme(theme.value)
}

/**
 * Ce que la piste montre : la dernière attaque tranchée du journal (son annonce, son issue, la réaction et les dégâts).
 * Un tour de combat ne l'efface pas ; la jauge de la cible n'est connue qu'au retour de l'attaque.
 */
function trayFromJournal(target?: TrayResult['target']): TrayResult | null {
  const outcome = [...journal.value].reverse().find((e) => e.highlight === 'OUTCOME')
  if (!outcome) return null
  const entries = journal.value.filter((e) => e.action === outcome.action)
  return {
    title: entries[0]?.text ?? '',
    outcome: outcome.text,
    details: entries.filter((e) => e.highlight === 'REACTION' || e.highlight === 'DAMAGE').map((e) => e.text),
    target,
  }
}

/** Le catalogue s'ouvre tant que la carte est vide ; ensuite, c'est le joueur qui le replie. */
function openDefaultDrawer() {
  drawer.value = !scene.value ? 'map' : scene.value.tokens.length ? null : 'catalog'
}

function toggleDrawer(which: 'catalog' | 'map') {
  drawer.value = drawer.value === which ? null : which
}

/** Entre 901 et 1280 px, tiroir et panneau latéral ne tiennent pas ensemble à côté de la carte. */
const PANELS_SHARE = window.matchMedia('(min-width: 901px) and (max-width: 1280px)')
const panelsShare = ref(PANELS_SHARE.matches)
const syncPanelsShare = (e: MediaQueryListEvent) => (panelsShare.value = e.matches)
/** Le tiroir ouvert replie le panneau latéral en bande d'onglets. */
const sideFolded = computed(() => panelsShare.value && drawer.value !== null)

/** L'attaque dont on choisit la cible, ou `null` hors ciblage. Exclusif du placement. */
const targeting = ref<{ attackerId: string; attackerName: string; attack: Attack } | null>(null)

/** Cercles d'allonge ou de portée autour de l'attaquant ; indicatifs, le serveur tranche. */
const targetingView = computed<Targeting | null>(() => {
  const t = targeting.value
  if (!t) return null
  const { reach, range } = t.attack
  const circles = reach
    ? [{ radius: reach.value, dashed: false }]
    : range
      ? [
          { radius: range.normal, dashed: false },
          { radius: range.longRange, dashed: true },
        ]
      : []
  return { attackerId: t.attackerId, measuredBy: reach ? 'reach' : 'range', circles }
})

/** En ciblage, ce que la distance permet pour chaque créature, montré dans le tableau. */
const rangeVerdicts = computed<Record<string, RangeVerdict>>(() => {
  const view = targetingView.value
  const tokens = scene.value?.tokens ?? []
  const attacker = view && tokens.find((t) => t.id === view.attackerId)
  if (!view || !attacker) return {}
  const verdicts: Record<string, RangeVerdict> = {}
  for (const token of tokens) {
    if (token.id === attacker.id) continue
    const verdict = rangeVerdict(view, Math.hypot(token.x - attacker.x, token.y - attacker.y))
    if (verdict) verdicts[token.id] = verdict
  }
  return verdicts
})
/** Résultat de la dernière action, affiché dans la barre d'état et lu par les lecteurs d'écran. */
const notice = ref('')
/** Ce que lit le lecteur d'écran : la dernière action, ou la distance mesurée. */
const announcement = ref('')

/**
 * La sélection vient du joueur (clic, tableau), ou du combat qui sélectionne la créature qui joue.
 * Suppr ne retire que ce que le joueur a choisi : une touche perdue ne sort pas la créature active du combat.
 */
const chosenByPlayer = ref(false)

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
    targetSheet.value = null
    lastResult.value = null
    decision.value = null
    combat.value = null
    drawer.value = 'map'
    return "Le serveur n'a plus de carte (redémarrage ?). En créer une nouvelle."
  }
  targeting.value = null
  movingId.value = null
  journal.value = await api.journal()
  decision.value = await api.pendingDecision()
  combat.value = await api.combat()
  lastResult.value = trayFromJournal()
  const ids = new Set(fresh.tokens.map((t) => t.id))
  selected.value = selected.value.filter((id) => ids.has(id))
  if (sheet.value && !ids.has(sheet.value.id)) sheet.value = null
  if (targetSheet.value && !ids.has(targetSheet.value.id)) targetSheet.value = null
  if (decision.value) return "Une décision attend une réponse : y répondre avant toute autre action."
  return "La carte a été modifiée hors de cet onglet ; elle vient d'être rechargée. Refaire l'action si besoin."
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
    journal.value = await api.journal()
    decision.value = await api.pendingDecision()
    combat.value = await api.combat()
    lastResult.value = trayFromJournal()
    openDefaultDrawer()
  }),
)

function createScene(width: number, height: number, unit: DistanceUnit) {
  attempt(async () => {
    const created = await api.createScene(width, height, unit)
    scene.value = created
    armed.value = null
    targeting.value = null
    movingId.value = null
    journal.value = []
    lastResult.value = null
    decision.value = null
    combat.value = null
    targetSheet.value = null
    clearSelection()
    history.length = 0
    drawer.value = 'catalog'
    tell(`Nouvelle carte de ${formatLength(created.width, created.unitSymbol)} sur ${formatLength(created.height, created.unitSymbol)}.`)
  })
}

async function placeToken(catalogId: string, x: number, y: number): Promise<TokenView> {
  const token = await api.placeToken(catalogId, x, y)
  scene.value?.tokens.push(token)
  await afterRosterChange()
  return token
}

/**
 * En combat, une créature posée tire son initiative et une créature retirée quitte l'ordre (son départ peut ouvrir
 * le tour suivant, voire « Qui joue ? ») : l'ordre, le journal et la décision se relisent.
 */
async function afterRosterChange() {
  if (!combat.value) return
  combat.value = await api.combat()
  journal.value = await api.journal()
  decision.value = await api.pendingDecision()
}

/** Retire les jetons côté serveur puis à l'écran ; rend ceux qui ont été retirés. */
async function deleteTokens(ids: string[]): Promise<TokenView[]> {
  const removed: TokenView[] = []
  // Un jeton en attente n'existe pas encore côté serveur : il n'y a rien à retirer.
  const known = ids.filter((id) => !scene.value?.tokens.find((t) => t.id === id)?.pending)
  // Ceux que « Qui joue ? » empêche de retirer restent sur la carte, et sélectionnés.
  const kept = [...known]
  for (const id of known) {
    await api.removeToken(id)
    kept.shift()
    const tokens = scene.value?.tokens ?? []
    const index = tokens.findIndex((t) => t.id === id)
    if (index >= 0) removed.push(tokens.splice(index, 1)[0])
    // Retirer la créature active peut poser « Qui joue ? » : le serveur refuserait les retraits suivants.
    if (combat.value && (await api.pendingDecision())) break
  }
  await afterRosterChange()
  const gone = ids.filter((id) => !kept.includes(id))
  selected.value = selected.value.filter((id) => !gone.includes(id))
  if (targeting.value && gone.includes(targeting.value.attackerId)) targeting.value = null
  if (movingId.value && gone.includes(movingId.value)) movingId.value = null
  if (sheet.value && gone.includes(sheet.value.id)) showSheet(selected.value.at(-1) ?? null)
  if (targetSheet.value && gone.includes(targetSheet.value.id)) targetSheet.value = null
  return removed
}

function names(tokens: TokenView[]): string {
  return tokens.map((t) => t.name).join(' et ')
}

function onBackgroundClick(x: number, y: number) {
  // En ciblage, un clic à côté ne vise rien et garde la feuille de l'attaquant.
  if (targeting.value) return
  if (!armed.value) {
    clearSelection()
    return
  }
  const taken = scene.value ? occupant(scene.value, { x, y }) : undefined
  if (taken) {
    error.value = `Trop près de ${taken.name}. Choisir une autre position.`
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
      await afterRosterChange()
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
    // En combat, un retrait ne s'annule pas (voir undo) : ne pas le promettre.
    const undoHint = combat.value ? '' : ' Ctrl+Z pour annuler.'
    tell(`${names(removed)} ${removed.length > 1 ? 'retirés' : 'retiré'}.${undoHint}`)
  })
}

/**
 * Annule la dernière pose ou le dernier retrait. Une créature reposée est recréée depuis
 * le catalogue, à la même position : le serveur lui donne un nouvel id, que les étapes
 * plus anciennes suivent.
 */
function undo() {
  const step = history.at(-1)
  if (!step) {
    tell('Rien à annuler.')
    return
  }
  // Reposée en combat, la créature tirerait une nouvelle initiative : ce ne serait pas annuler son retrait.
  if (step.type === 'remove' && combat.value) {
    const plural = step.tokens.length > 1
    tell(`Impossible de reposer ${names(step.tokens)} en plein combat : ${plural ? 'les créatures tireraient' : 'la créature tirerait'} une nouvelle initiative.`)
    return
  }
  history.pop()
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
  movingId.value = null
  if (entryId) targeting.value = null
  notice.value = ''
}

const board = ref<InstanceType<typeof TokenBoard> | null>(null)
/** Le bouton d'attaque qui a lancé le ciblage : le focus y revient une fois le ciblage fini. */
let attackOrigin: HTMLElement | null = null

/** Fin du ciblage par la résolution ou Échap : le clavier reprend là où l'attaque est partie. */
function endTargeting() {
  targeting.value = null
  if (attackOrigin?.isConnected) attackOrigin.focus()
  attackOrigin = null
}

/**
 * Une attaque de la feuille affichée : passe la carte en ciblage ; un second clic l'arrête.
 * Le focus va au tableau, où le clavier choisit la cible.
 */
function startAttack(attack: Attack) {
  const attacker = sheet.value
  if (!attacker) return
  if (targeting.value?.attackerId === attacker.id && targeting.value.attack.id === attack.id) {
    targeting.value = null
    return
  }
  arm(null)
  error.value = null
  targeting.value = { attackerId: attacker.id, attackerName: attacker.name, attack }
  attackOrigin = document.activeElement instanceof HTMLElement ? document.activeElement : null
  announcement.value = `Ciblage : ${attack.label} de ${attacker.name}. Choisir la cible dans le tableau ; Échap annule.`
  // Le tableau s'ouvre en entier pendant le ciblage : le focus attend son rendu.
  void nextTick(() => board.value?.focusFirstTarget())
}

/** Un clic sur une créature : la cible en ciblage, la sélection sinon. */
function onTokenChosen(tokenId: string) {
  if (targeting.value) attackTarget(tokenId)
  else toggleSelection(tokenId)
}

/** Une attaque est envoyée et pas encore résolue : un second clic ne doit pas en lancer une autre. */
let attacking = false

/** Le serveur résout l'attaque ; un refus (portée…) affiche sa raison et laisse le ciblage actif. */
function attackTarget(targetId: string) {
  const t = targeting.value
  if (!t || attacking || scene.value?.tokens.find((token) => token.id === targetId)?.pending) return
  attacking = true
  void attempt(async () => {
    const result = await api.attack(t.attackerId, t.attack.id, targetId)
    endTargeting()
    await settleAction(result)
  }).finally(() => {
    attacking = false
  })
}

/**
 * Ce qu'une action (attaque, combat), ou sa reprise après une décision, a produit : une nouvelle décision ouvre
 * le dialogue ; sinon l'action est finie (journal, combat, et pour une attaque PV du jeton, piste à dés, feuilles).
 */
async function settleAction(result: ActionResult) {
  journal.value.push(...result.entries)
  decision.value = result.decision
  combat.value = result.combat
  notice.value = ''
  // La barre annonce le tour (aria-live) et le dialogue la décision : seule l'issue d'une attaque reste à dire.
  if (result.decision || result.targetId === null) return
  if (result.targetHitPoints === null || result.targetMaxHitPoints === null) return
  const target = scene.value?.tokens.find((token) => token.id === result.targetId)
  if (target) target.hitPoints = result.targetHitPoints
  const tray = trayFromJournal({
    name: target?.name ?? result.targetId,
    hitPoints: result.targetHitPoints,
    maxHitPoints: result.targetMaxHitPoints,
  })
  lastResult.value = tray
  announcement.value = tray ? [tray.outcome, ...tray.details].join('. ') : 'Attaque résolue.'
  // Une réaction peut changer la feuille de la cible comme de l'attaquant (emplacement, CA), et la CA du tableau.
  if (sheet.value) showSheet(sheet.value.id)
  const fresh = await api.sheet(result.targetId)
  targetSheet.value = fresh
  if (target) target.armorClass = fresh.armorClass
}

function endMoving() {
  movingId.value = null
}

/**
 * Un jeton glissé, ou déplacé au clavier, arrive en (x, y). Le client refuse ce qui est évident (trop près d'une
 * créature, au-delà de ce qui reste) ; le serveur tranche le reste et rend l'entrée du journal en combat.
 */
function moveToken(tokenId: string, x: number, y: number) {
  const view = scene.value
  const token = view?.tokens.find((t) => t.id === tokenId)
  if (!view || !token || token.pending || moveBusy.value) return
  const limit = movementLimit.value
  const check = checkMove(view, token, { x, y }, limit)
  if (check.blocker) {
    error.value = `Trop près de ${check.blocker.name}. Choisir une autre position.`
    return
  }
  if (check.tooFar && limit) {
    error.value = `${token.name} ne peut plus parcourir que ${formatLength(limit.left, view.unitSymbol)} ce tour-ci.`
    return
  }
  moveBusy.value = true
  void attempt(async () => {
    const result = await api.moveToken(tokenId, x, y)
    token.x = x
    token.y = y
    endMoving()
    await settleAction(result)
    tell(result.entries.at(-1)?.text ?? `${token.name} déplacé.`)
  }).finally(() => {
    moveBusy.value = false
  })
}

/** D : déplace au clavier la créature sélectionnée ; un second D annule. */
function startMoving() {
  const id = selected.value.length === 1 ? selected.value[0] : null
  if (!id || scene.value?.tokens.find((t) => t.id === id)?.pending) {
    tell('Sélectionner une seule créature, puis D pour la déplacer.')
    return
  }
  if (movingId.value === id) {
    endMoving()
    return
  }
  movingId.value = id
  error.value = null
  void nextTick(() => mapView.value?.focus())
}

/** Lance le combat ou termine le tour ; un second clic avant la réponse n'envoie rien. */
function combatAction(send: () => Promise<ActionResult>) {
  if (combatBusy.value) return
  combatBusy.value = true
  void attempt(async () => {
    await settleAction(await send())
  }).finally(() => {
    combatBusy.value = false
  })
}

/** Lancer le combat met fin à la pose en cours. */
function startCombat() {
  arm(null)
  combatAction(api.startCombat)
}

/**
 * À chaque changement de tour, la sélection et la feuille passent à la créature qui joue ;
 * pendant « Qui joue ? », au premier candidat.
 */
watch(
  () => (combat.value ? (combat.value.activeId ?? combat.value.candidates[0] ?? null) : null),
  (creatureId) => {
    const token = creatureId ? scene.value?.tokens.find((t) => t.creatureId === creatureId) : undefined
    if (!token || token.pending) return
    selected.value = [token.id]
    chosenByPlayer.value = false
    tab.value = 'sheet'
    showSheet(token.id)
  },
)

/** Le joueur répond à la décision ; l'action suspendue reprend côté serveur. */
function answerDecision(option: string) {
  const pending = decision.value
  if (!pending || answering.value) return
  answering.value = true
  void attempt(async () => {
    await settleAction(await api.answer(pending.id, option))
  }).finally(() => {
    answering.value = false
  })
}

function toggleSelection(tokenId: string) {
  if (scene.value?.tokens.find((t) => t.id === tokenId)?.pending) return
  notice.value = ''
  const current = selected.value
  // Un clic sur la créature que le combat a sélectionnée la choisit, sans la désélectionner.
  if (!chosenByPlayer.value && current.length === 1 && current[0] === tokenId) {
    chosenByPlayer.value = true
    return
  }
  chosenByPlayer.value = true
  selected.value = current.includes(tokenId)
    ? current.filter((id) => id !== tokenId)
    : [...current, tokenId].slice(-2)
  const shown = selected.value.at(-1) ?? null
  if (shown) tab.value = 'sheet'
  showSheet(shown)
}

function clearSelection() {
  selected.value = []
  chosenByPlayer.value = false
  sheet.value = null
}

/** Mode en cours, rappelé en laiton dans la barre. */
const mode = computed(() => {
  if (targeting.value) return `Ciblage · ${targeting.value.attack.label}`
  if (movingToken.value) return `Déplacement · ${movingToken.value.name}`
  if (armedEntry.value) return `Pose · ${armedEntry.value.name}`
  if (measure.value) return 'Mesure'
  return null
})

const TABS = [
  { id: 'sheet', label: 'Feuille' },
  { id: 'target', label: 'Cible' },
  { id: 'journal', label: 'Journal' },
] as const
const tabButtons = ref<HTMLButtonElement[]>([])

/** Onglets au clavier : flèches gauche et droite, Début et Fin. */
function onTabKey(e: KeyboardEvent, index: number) {
  const moves: Record<string, number> = {
    ArrowLeft: index - 1,
    ArrowRight: index + 1,
    ArrowUp: index - 1,
    ArrowDown: index + 1,
    Home: 0,
    End: TABS.length - 1,
  }
  if (!(e.key in moves)) return
  e.preventDefault()
  const next = (moves[e.key] + TABS.length) % TABS.length
  tab.value = TABS[next].id
  tabButtons.value[next]?.focus()
}

/** Choisir un onglet du panneau replié le déplie : le tiroir se ferme. */
function chooseTab(id: (typeof TABS)[number]['id']) {
  tab.value = id
  if (sideFolded.value) drawer.value = null
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
  // Le dialogue modal rend la page inerte, pas l'écouteur de window : rien ne passe tant qu'il attend.
  if (decision.value) return
  if (e.key === 'Escape') {
    // Un pas à la fois : d'abord le ciblage ou le placement, puis la sélection.
    if (targeting.value) endTargeting()
    else if (movingId.value) endMoving()
    else if (armed.value) arm(null)
    else clearSelection()
    notice.value = ''
    error.value = null
    return
  }
  if (isTyping(e.target) || !scene.value) return
  if (e.key === 'Delete' || e.key === 'Backspace') {
    e.preventDefault()
    if (chosenByPlayer.value) removeTokens(selected.value)
    else if (selected.value.length) tell('Cliquer sur la créature pour la choisir, puis Suppr pour la retirer.')
  } else if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'z') {
    e.preventDefault()
    undo()
  } else if (e.ctrlKey || e.metaKey || e.altKey || e.repeat) {
    return
  } else if (e.key.toLowerCase() === 'f' && combat.value?.activeId && !combatBusy.value) {
    e.preventDefault()
    combatAction(api.endTurn)
  } else if (e.key.toLowerCase() === 'd' && !armed.value && !targeting.value) {
    e.preventDefault()
    startMoving()
  } else if (/^[1-9]$/.test(e.key) && !(e.target instanceof HTMLElement && e.target.closest('[role="tablist"]'))) {
    // L'attaque n de la feuille affichée, comme son bouton : fermée si les règles la refusent.
    const attacker = sheet.value
    const attack = attacker?.attacks[Number(e.key) - 1]
    if (!attacker || !attack || tab.value !== 'sheet') return
    e.preventDefault()
    if (attacker.attackRefusal && targeting.value?.attack.id !== attack.id) tell(`${attacker.attackRefusal}.`)
    else startAttack(attack)
  }
}

onMounted(() => {
  window.addEventListener('keydown', onKey)
  PANELS_SHARE.addEventListener('change', syncPanelsShare)
})
onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKey)
  PANELS_SHARE.removeEventListener('change', syncPanelsShare)
})
</script>

<template>
  <div class="layout" :class="{ 'with-drawer': drawer }">
    <header class="bar">
      <h1 class="app-name">Solo VTT</h1>
      <p v-if="scene" class="map-info">
        Carte {{ formatLength(scene.width, scene.unitSymbol) }} × {{ formatLength(scene.height, scene.unitSymbol) }} ·
        {{ scene.tokens.length }} {{ scene.tokens.length > 1 ? 'créatures' : 'créature' }}
      </p>
      <span class="spacer" />
      <CombatControl
        v-if="scene"
        :combat="combat"
        :creature-count="scene.tokens.filter((t) => !t.pending).length"
        :blocked="combatBusy"
        @start="startCombat"
        @end-turn="combatAction(api.endTurn)"
      />
      <strong v-if="mode" class="mode">{{ mode }}</strong>
      <button
        type="button"
        class="icon-button"
        :aria-label="theme === 'dark' ? 'Passer au thème clair' : 'Passer au thème sombre'"
        :title="theme === 'dark' ? 'Thème clair' : 'Thème sombre'"
        @click="toggleTheme"
      >
        <AppIcon :name="theme === 'dark' ? 'sun' : 'moon'" />
      </button>
    </header>

    <nav class="rail" aria-label="Outils">
      <button
        type="button"
        class="rail-button"
        :class="{ open: drawer === 'catalog' }"
        :aria-expanded="drawer === 'catalog'"
        aria-controls="drawer"
        aria-label="Catalogue"
        title="Catalogue"
        @click="toggleDrawer('catalog')"
      >
        <AppIcon name="catalog" />
      </button>
      <button
        type="button"
        class="rail-button"
        :class="{ open: drawer === 'map' }"
        :aria-expanded="drawer === 'map'"
        aria-controls="drawer"
        aria-label="Nouvelle carte"
        title="Nouvelle carte"
        @click="toggleDrawer('map')"
      >
        <AppIcon name="map" />
      </button>
    </nav>

    <aside v-if="drawer" id="drawer" class="drawer">
      <CatalogPanel v-if="drawer === 'catalog'" :entries="catalog" :armed="armed" :disabled="!scene" @arm="arm" />
      <NewMapForm v-else :token-count="scene ? scene.tokens.length : null" @create="createScene" @cancel="drawer = null" />
    </aside>

    <main class="table">
      <div v-if="error" class="error" role="alert">
        <p>{{ error }}</p>
        <button type="button" @click="error = null">Fermer</button>
      </div>
      <template v-if="scene">
        <TokenBoard
          ref="board"
          :tokens="scene.tokens"
          :selected="selected"
          :attacker="targeting?.attackerId ?? null"
          :target="targetSheet?.id ?? null"
          :ranges="rangeVerdicts"
          :combat="combat"
          @select="onTokenChosen"
          @remove="removeTokens([$event])"
        />
        <div class="map-frame" :style="{ '--map-ratio': `${scene.width} / ${scene.height}` }">
          <MapView
            ref="mapView"
            :scene="scene"
            :selected="selected"
            :placing="armedEntry?.kind ?? null"
            :targeting="targetingView"
            :active-id="combat?.activeId ?? null"
            :theme="theme"
            :movable="movable"
            :movement-limit="movementLimit"
            :moving="movingId"
            @background-click="onBackgroundClick"
            @token-click="onTokenChosen"
            @token-move="moveToken"
          />
        </div>
        <p class="status">
          <strong v-if="notice" class="notice">{{ notice }}</strong>
          <template v-if="targeting">
            Ciblage : <strong>{{ targeting.attack.label }}</strong> de {{ targeting.attackerName }}. Cliquer sur la
            cible ou la choisir dans le tableau ; Échap annule.<template v-if="targetingView?.measuredBy === 'range'">
              Hachures : portée longue, avec désavantage.</template>
          </template>
          <template v-else-if="movingToken">
            Déplacement : <strong>{{ movingToken.name }}</strong>. Flèches pour choisir la case, Entrée pour déplacer ;
            Échap annule.
          </template>
          <template v-else-if="armedEntry">
            Pose : <strong>{{ armedEntry.name }}</strong>. Cliquer pour poser ; Maj + clic cale sur la case, Échap
            arrête.
          </template>
          <template v-else-if="measure">
            Distance {{ measure.from }} – {{ measure.to }} :
            <strong>{{ formatLength(measure.value, scene.unitSymbol) }}</strong> ({{ cellsLabel(measure.cells) }})
          </template>
          <template v-else-if="!notice && combat">
            Glisser une créature pour la déplacer (Maj cale sur la case) ; D : déplacer au clavier, 1 à 9 : attaques de la
            feuille, F : fin du tour.
          </template>
          <template v-else-if="!notice">
            Glisser une créature pour la déplacer (Maj cale sur la case). Sélectionner deux créatures pour mesurer leur
            distance ; Suppr retire de la carte les créatures sélectionnées.
          </template>
        </p>
        <DiceTray :result="lastResult" />
      </template>
      <p v-else class="empty">Aucune carte. La créer depuis « Nouvelle carte », dans le rail à gauche.</p>
      <p class="sr-only" aria-live="polite">{{ announcement }}</p>
    </main>

    <DecisionDialog :decision="decision" :answering="answering" @answer="answerDecision" />

    <aside class="side" :class="{ folded: sideFolded }" aria-label="Créatures">
      <div class="tabs" role="tablist" aria-label="Panneau">
        <button
          v-for="(t, index) in TABS"
          :id="`tab-${t.id}`"
          :key="t.id"
          ref="tabButtons"
          type="button"
          role="tab"
          class="tab"
          :aria-selected="tab === t.id"
          :aria-controls="`panel-${t.id}`"
          :tabindex="tab === t.id ? 0 : -1"
          :title="sideFolded ? `Ouvrir ${t.label}` : undefined"
          @click="chooseTab(t.id)"
          @keydown="onTabKey($event, index)"
        >
          {{ t.label }}
        </button>
      </div>
      <div
        v-show="!sideFolded"
        :id="`panel-${tab}`"
        class="tab-panel" role="tabpanel" :aria-labelledby="`tab-${tab}`" tabindex="0">
        <CreatureSheet
          v-if="tab === 'sheet'"
          :sheet="sheet"
          :aiming="targeting && targeting.attackerId === sheet?.id ? targeting.attack.id : null"
          :movement-left="combat && combat.activeId === sheet?.id ? combat.movementLeft : null"
          empty="Sélectionner une créature sur la carte ou dans le tableau pour voir sa feuille."
          @attack="startAttack"
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
.layout {
  height: 100vh;
  display: grid;
  grid-template-columns: 3.5rem 1fr 24rem;
  grid-template-rows: auto minmax(0, 1fr);
  grid-template-areas:
    'bar bar bar'
    'rail table side';
}
.layout.with-drawer {
  grid-template-columns: 3.5rem 17rem 1fr 24rem;
  grid-template-areas:
    'bar bar bar bar'
    'rail drawer table side';
}
.bar {
  grid-area: bar;
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 0.55rem 1rem 0.55rem 1.1rem;
  background: var(--wood-raised);
  border-bottom: 2px solid var(--page);
}
.app-name {
  margin: 0;
  font-family: var(--font-title);
  font-weight: 700;
  font-size: 1.45rem;
  letter-spacing: 0.03em;
  color: var(--ink);
  white-space: nowrap;
}
.map-info {
  margin: 0;
  color: var(--ink-muted);
  font-variant-numeric: tabular-nums;
}
.spacer {
  flex: 1;
}
/* Le mode en cours : l'état vivant, en laiton. */
.mode {
  padding: 0.25rem 0.85rem;
  border-radius: 999px;
  background: var(--brass);
  color: var(--on-brass);
  font-weight: 700;
}
.icon-button {
  display: grid;
  place-items: center;
  width: 2.5rem;
  height: 2.5rem;
  padding: 0;
}
.rail {
  grid-area: rail;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  padding-top: 0.75rem;
  background: var(--wood);
  border-right: 1px solid var(--rule);
}
.rail-button {
  display: grid;
  place-items: center;
  width: 2.6rem;
  height: 2.6rem;
  padding: 0;
  border-color: transparent;
  background: transparent;
  color: var(--ink-muted);
}
.rail-button:hover:not(:disabled) {
  color: var(--ink);
  background: var(--wood-raised);
  border-color: transparent;
}
/* Tiroir ouvert : un état d'interface, pas un mode ; il reste en bois. */
.rail-button.open,
.rail-button.open:hover:not(:disabled) {
  color: var(--ink);
  background: var(--wood-sunken);
  border-color: var(--rule);
}
.drawer {
  grid-area: drawer;
  padding: 1rem 1.1rem;
  overflow-y: auto;
  background: var(--wood);
  border-right: 2px solid var(--page);
}
.table {
  grid-area: table;
  position: relative;
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  background: var(--felt);
}
/* Le plateau est bordé de bois. */
.map-frame {
  flex: 1;
  min-height: 0;
  border: 6px solid var(--wood);
  border-top: 0;
  border-bottom: 0;
}
.status {
  margin: 0;
  padding: 0.45rem 1rem;
  background: var(--wood);
  border-top: 1px solid var(--rule);
  color: var(--ink-muted);
  font-variant-numeric: tabular-nums;
  text-wrap: pretty;
}
.status strong {
  color: var(--ink);
}
.notice {
  margin-right: 0.75rem;
}
.empty {
  margin: auto;
  padding: 1rem;
  color: var(--map-label);
  font-family: var(--font-title);
  font-size: 1.15rem;
  text-align: center;
}
/* Par-dessus la carte : l'apparition d'une erreur ne la redimensionne pas. */
.error {
  position: absolute;
  inset: 0 0 auto;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.5rem 1rem;
  background: var(--wood-raised);
  border-bottom: 2px solid var(--danger);
  color: var(--danger);
  font-weight: 500;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}
.error p {
  margin: 0;
}
.side {
  grid-area: side;
  display: flex;
  flex-direction: column;
  min-height: 0;
  background: var(--wood);
  border-left: 2px solid var(--page);
}
.tabs {
  display: flex;
  gap: 0.25rem;
  padding: 0.6rem 0.9rem 0;
  border-bottom: 1px solid var(--rule);
}
.tab {
  padding: 0.4rem 0.9rem 0.35rem;
  border: 0;
  border-radius: 4px 4px 0 0;
  background: transparent;
  font-family: var(--font-title);
  font-weight: 700;
  font-size: 1.05rem;
  letter-spacing: 0.04em;
  color: var(--ink-muted);
  box-shadow: inset 0 -3px 0 transparent;
}
.tab:hover:not(:disabled) {
  background: var(--wood-raised);
  color: var(--ink);
}
.tab[aria-selected='true'] {
  color: var(--ink);
  box-shadow: inset 0 -3px 0 var(--ink);
}
.tab-panel {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 1rem 1.1rem 1.5rem;
}
.tab-panel:focus-visible {
  outline-offset: -2px;
}
@media (max-width: 1100px) {
  .layout {
    grid-template-columns: 3.5rem 1fr 20rem;
  }
}
/* Tiroir ouvert sous 1280 px : le panneau latéral se replie en bande d'onglets verticaux. */
@media (min-width: 901px) and (max-width: 1280px) {
  .layout.with-drawer {
    grid-template-columns: 3.5rem 17rem 1fr 3rem;
  }
  .side.folded .tabs {
    flex-direction: column;
    align-items: center;
    gap: 0.5rem;
    padding: 0.75rem 0;
    border-bottom: 0;
  }
  .side.folded .tab {
    writing-mode: vertical-rl;
    padding: 0.9rem 0.4rem;
    border-radius: 4px;
  }
  .side.folded .tab[aria-selected='true'] {
    box-shadow: inset 3px 0 0 var(--ink);
  }
}
@media (max-width: 900px) {
  .layout,
  .layout.with-drawer {
    height: auto;
    min-height: 100vh;
    grid-template-columns: 3.5rem 1fr;
    grid-template-rows: auto auto auto auto;
    grid-template-areas:
      'bar bar'
      'rail drawer'
      'table table'
      'side side';
  }
  .rail {
    flex-direction: column;
    padding-bottom: 0.75rem;
    border-bottom: 1px solid var(--rule);
  }
  .layout:not(.with-drawer) .rail {
    grid-column: 1 / -1;
    flex-direction: row;
    padding: 0.4rem 0.75rem;
  }
  .side {
    border-left: 0;
    min-height: 24rem;
  }
  /* La carte prend sa propre forme : pas de bande de feutre vide au-dessus ni en dessous. */
  .map-frame {
    flex: none;
    aspect-ratio: var(--map-ratio, 3 / 2);
    min-height: 14rem;
    max-height: 70svh;
  }
}
</style>

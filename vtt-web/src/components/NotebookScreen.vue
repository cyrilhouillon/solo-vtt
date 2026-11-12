<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { onBeforeRouteLeave, onBeforeRouteUpdate, useRouter } from 'vue-router'
import { ApiError, campaignApi } from '../api'
import AppIcon from './AppIcon.vue'
import ThemeButton from './ThemeButton.vue'
import PageEditor from './PageEditor.vue'
import PageTree from './PageTree.vue'
import MovePageDialog from './MovePageDialog.vue'
import CitingPages from './CitingPages.vue'
import SearchResults from './SearchResults.vue'
import PageTabs from './PageTabs.vue'
import DeletePageDialog from './DeletePageDialog.vue'
import TrashDialog from './TrashDialog.vue'
import CampaignModulesDialog from './CampaignModulesDialog.vue'
import FoldSection from './FoldSection.vue'
import CampaignColumn from './CampaignColumn.vue'
import ResultDialog from './ResultDialog.vue'
import CombatEntryView from './CombatEntryView.vue'
import { copyBlocks } from '../notebook/clipboard'
import { ancestors, branch, visiblePages } from '../notebook/tree'
import { MAX_TITLE_LENGTH } from '../notebook/context'
import { IMAGE_TYPES } from '../images'
import { availableModules, notebookModules } from '../modules/registry'
import type { NotebookContext, NotebookModule } from '../modules/notebookModule'
import type { JSONContent } from '@tiptap/core'
import type { CampaignView, NotebookView, PageDeletion, PageDocument, PageView, SearchResult, TrashedPage } from '../types'

/** `pageId` : la page de l'adresse, `null` sans page. */
const props = defineProps<{ campaign: CampaignView; pageId: string | null }>()
const api = campaignApi(props.campaign.id)
const router = useRouter()

/** Le Carnet, `null` tant qu'il n'est pas lu. */
const notebook = ref<NotebookView | null>(null)
/** La page de l'onglet actif, seule montée dans un éditeur ; `null` sans onglet. */
const page = ref<PageView | null>(null)
/** Les pages ouvertes en onglets, dans l'ordre de la barre. */
const tabs = ref<string[]>([])
const title = ref('')
const trimmed = computed(() => title.value.trim())
const busy = ref(false)
const error = ref<string | null>(null)
const firstTitle = ref<HTMLInputElement | null>(null)
/** La page ouverte s'édite : vrai pour celle qu'on vient de créer, jusqu'à ce qu'on la quitte ; sinon d'un geste. */
const unlocked = ref(false)
/** Les branches dépliées de l'arbre, gardées le temps de l'écran seulement. */
const expanded = ref<ReadonlySet<string>>(new Set())
/** La page dont on choisit la destination, `null` hors du dialogue. */
const moving = ref<string | null>(null)
/** La page dont la suppression attend confirmation, `null` hors du dialogue ; `deletion` : ce qui part avec elle. */
const deleting = ref<string | null>(null)
const deletion = ref<PageDeletion | null>(null)
/** La corbeille ouverte, et ses suppressions (`null` pendant la lecture). */
const trashOpen = ref(false)
const trashEntries = ref<TrashedPage[] | null>(null)
const searchField = ref<HTMLInputElement | null>(null)
const trashButton = ref<HTMLButtonElement | null>(null)
/** Le titre en tête de page, éditable une fois la page déverrouillée. */
const pageTitle = ref('')
/** Les pages qui citent la page ouverte, `null` tant qu'elles ne sont pas lues. */
const citing = ref<string[] | null>(null)
/** L'éditeur de la page ouverte, qui insère les images. */
const editor = ref<InstanceType<typeof PageEditor> | null>(null)
const imageInput = ref<HTMLInputElement | null>(null)
/** Vrai pendant l'envoi d'une image : le bouton Image attend. */
const uploading = ref(false)

/** L'image choisie part au serveur, puis entre dans la page au curseur ; un refus va au bandeau. */
async function chooseImage(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  // Vidé tout de suite : choisir à nouveau le même fichier redéclenche `change`.
  input.value = ''
  if (!file || !editor.value) return
  error.value = null
  uploading.value = true
  try {
    const refusal = await editor.value.insertImage(file)
    if (refusal) error.value = refusal
  } finally {
    uploading.value = false
  }
}

/** La recherche en cours : tant que le champ n'est pas vide, ses résultats remplacent l'arbre. */
const query = ref('')
const searching = computed(() => query.value.trim() !== '')
/** Les derniers résultats reçus et la requête qui les a produits ; `null` avant la première réponse. */
const found = ref<{ query: string; results: SearchResult[] } | null>(null)
/** Attente après la dernière frappe dans le champ de recherche. */
const SEARCH_DELAY_MS = 250
let searchTimer: ReturnType<typeof setTimeout> | undefined
/** Numéro de la dernière recherche lancée : une réponse plus ancienne est ignorée. */
let searchRun = 0

watch(query, () => {
  clearTimeout(searchTimer)
  if (!searching.value) {
    searchRun++
    found.value = null
    return
  }
  searchTimer = setTimeout(() => void search(), SEARCH_DELAY_MS)
})

/** La frappe en attente part d'abord, pour que la recherche la trouve ; si elle échoue, rien ne part. */
async function search() {
  const run = ++searchRun
  const wanted = query.value.trim()
  if (!(await saved()) || run !== searchRun) return
  try {
    const results = await api.searchPages(wanted)
    if (run === searchRun) found.value = { query: wanted, results }
  } catch (e) {
    if (run === searchRun) error.value = `Recherche impossible : ${e instanceof Error ? e.message : String(e)}`
  }
}

function clearSearch() {
  query.value = ''
}

/** Attente avant d'envoyer une frappe : assez pour grouper les touches, assez peu pour ne rien perdre. */
const SAVE_DELAY_MS = 1000
/** Plafond d'un envoi `keepalive` à la fermeture de l'onglet (64 Kio pour tous les envois en cours), avec une marge. */
const KEEPALIVE_LIMIT = 60_000

/** Le dernier contenu pas encore enregistré ; il remplace tout contenu plus ancien en attente. */
let pending: { pageId: string; content: PageDocument } | null = null
let saveTimer: ReturnType<typeof setTimeout> | undefined
/** Les envois se suivent, un à la fois. */
let saving: Promise<void> = Promise.resolve()

/** Ce que le Carnet offre aux panneaux des modules. */
const moduleContext: NotebookContext = {
  campaignId: props.campaign.id,
  writeResult: (blocks) => {
    if (page.value && editor.value) editor.value.insertBlocks(blocks)
    else showResult(blocks)
  },
  openPage: async (pageId) => {
    notebook.value = await api.notebook()
    await open(pageId)
  },
}

/** Le résultat d'un module montré sans page, `null` sans dialogue ; l'état de sa copie, comme `ResultDialog` l'attend. */
const result = ref<JSONContent[] | null>(null)
const copyError = ref<string | null | undefined>(undefined)

/** Montre le résultat et le copie aussitôt : le clic qui l'a tiré est encore récent. */
function showResult(blocks: JSONContent[]) {
  result.value = blocks
  void copyResult()
}

async function copyResult() {
  const blocks = result.value
  if (!blocks) return
  copyError.value = undefined
  const reason = await copyBlocks(blocks, notebook.value?.pages ?? [])
  if (result.value === blocks) copyError.value = reason
}

function openFromResult(pageId: string) {
  result.value = null
  void open(pageId)
}

function onChange(content: PageDocument) {
  if (!page.value) return
  pending = { pageId: page.value.id, content }
  clearTimeout(saveTimer)
  saveTimer = setTimeout(() => void flush(), SAVE_DELAY_MS)
}

/** Envoie le contenu en attente ; en cas d'échec, il reste en attente et repart au changement suivant. */
function flush(): Promise<void> {
  clearTimeout(saveTimer)
  saving = saving.then(async () => {
    const next = pending
    if (!next) return
    pending = null
    try {
      await api.savePageContent(next.pageId, next.content)
    } catch (e) {
      pending ??= next
      error.value = `Page non enregistrée : ${e instanceof Error ? e.message : String(e)}`
    }
  })
  return saving
}

/** Vrai quand plus rien n'attend : on peut quitter la page sans perdre la frappe. */
async function saved(): Promise<boolean> {
  await flush()
  return pending === null
}

function onVisibilityChange() {
  if (document.visibilityState === 'hidden') void flush()
}

/** L'onglet se ferme : le dernier envoi part en `keepalive`, s'il tient sous le plafond. */
function onPageHide() {
  if (!pending || JSON.stringify(pending.content).length > KEEPALIVE_LIMIT) return
  void api.savePageContent(pending.pageId, pending.content, true).catch(() => {})
  pending = null
}

/** Le geste en cours, résolu quand il a fini, réussi ou non. */
let running: Promise<void> = Promise.resolve()

function call(action: () => Promise<void>): Promise<void> {
  busy.value = true
  running = (async () => {
    try {
      await action()
    } catch (e) {
      error.value = e instanceof Error ? e.message : String(e)
    } finally {
      busy.value = false
    }
  })()
  return running
}

function toggle(pageId: string) {
  const next = new Set(expanded.value)
  if (!next.delete(pageId)) next.add(pageId)
  expanded.value = next
}

/** Déplie le chemin jusqu'à la page, pour qu'elle soit en vue dans l'arbre. */
function reveal(pageId: string) {
  const path = ancestors(notebook.value?.pages ?? [], pageId)
  if (path.every((id) => expanded.value.has(id))) return
  expanded.value = new Set([...expanded.value, ...path])
}

/** Pose la page ouverte, déverrouillée ou non, et la montre dans l'arbre. */
function display(opened: PageView, editable: boolean) {
  page.value = opened
  pageTitle.value = opened.title
  unlocked.value = editable
  reveal(opened.id)
}

/** Garde les onglets au serveur, l'actif étant par défaut la page ouverte ; un échec va au bandeau, sans fermer la page. */
async function persistTabs(activeId: string | null = page.value?.id ?? null) {
  try {
    await api.setTabs(tabs.value, activeId)
  } catch (e) {
    error.value = `Onglets non enregistrés : ${e instanceof Error ? e.message : String(e)}`
  }
}

/**
 * Ouvre la page dans son onglet, ajouté en fin de barre s'il n'existe pas, et le garde comme actif, puis lit les pages
 * qui la citent. Relue au serveur à chaque fois : un seul éditeur, celui de l'onglet actif.
 */
async function show(pageId: string) {
  if (!(await saved())) return
  const opened = await api.page(pageId)
  // Vidée avant d'afficher : la liste de la page précédente ne passe jamais sous celle-ci.
  citing.value = null
  if (!tabs.value.includes(pageId)) tabs.value = [...tabs.value, pageId]
  display(opened, false)
  await writeAddress(pageId)
  await persistTabs()
  await readCiting(pageId)
}

/**
 * Vrai quand la page s'est ouverte ; illisible (inconnue, à la corbeille : 404), rien ne change et aucune erreur n'est
 * dite. Toute autre erreur (serveur injoignable…) remonte jusqu'au bandeau.
 */
async function tryShow(pageId: string): Promise<boolean> {
  try {
    await show(pageId)
  } catch (e) {
    if (e instanceof ApiError && e.status === 404) return false
    throw e
  }
  return page.value?.id === pageId
}

/** Vrai pendant une navigation lancée par le Carnet lui-même, qui n'a pas à attendre le geste dont elle fait partie. */
let writing = false

/**
 * L'adresse dit la page ouverte (`null` : aucune). Elle remplace l'entrée en cours, sauf `push` : un geste qui ouvre une
 * page en ajoute une, que Précédent défera.
 */
async function writeAddress(pageId: string | null, push = false) {
  if (pageId === props.pageId) return
  const params = pageId ? { campaignId: props.campaign.id, pageId } : { campaignId: props.campaign.id }
  writing = true
  try {
    await (push ? router.push : router.replace)({ name: 'notebook', params })
  } finally {
    writing = false
  }
}

/** L'adresse d'une page, que portent les lignes de l'arbre et les liens : le clic du milieu l'ouvre dans un nouvel onglet. */
const pageHref = (pageId: string) => router.resolve({ name: 'notebook', params: { campaignId: props.campaign.id, pageId } }).href

/** Avant de changer de page ou d'écran par l'adresse : le geste en cours finit, puis la frappe part ; sinon on reste. */
async function mayLeave(): Promise<boolean> {
  if (writing) return true
  await running
  return saved()
}
onBeforeRouteUpdate(mayLeave)
onBeforeRouteLeave(mayLeave)

/** Précédent, Suivant, ou un geste qui a navigué : la page de l'adresse s'ouvre ; illisible, l'adresse reprend la page ouverte. */
watch(
  () => props.pageId,
  (pageId) => {
    if (pageId === (page.value?.id ?? null)) return
    error.value = null
    void call(async () => {
      if (!pageId) {
        // Une entrée sans page, d'avant le premier onglet : aucune page n'est montée.
        page.value = null
        citing.value = null
        return
      }
      // Page illisible ou erreur (dite au bandeau par `call`) : l'adresse reprend la page ouverte.
      let shown = false
      try {
        shown = await tryShow(pageId)
      } finally {
        if (!shown) await writeAddress(page.value?.id ?? null)
      }
    })
  },
)

const tabBar = ref<InstanceType<typeof PageTabs> | null>(null)

/**
 * Ferme l'onglet ; fermer l'actif attend l'envoi de la frappe, puis active son voisin de droite, sinon de gauche. Fermé
 * au clavier, le focus passe à l'onglet actif, sinon il tomberait hors de la barre avec le bouton retiré.
 */
function closeTab(pageId: string) {
  if (busy.value) return
  error.value = null
  const keepFocus = tabBar.value?.hasFocus() ?? false
  void call(async () => {
    await closeTabs(new Set([pageId]))
    if (!keepFocus) return
    await nextTick()
    tabBar.value?.focusActive()
  })
}

/**
 * Ferme les onglets de ces pages ; si l'actif en est, attend l'envoi de sa frappe, puis active son voisin de droite parmi
 * ceux qui restent, sinon de gauche.
 */
async function closeTabs(pageIds: ReadonlySet<string>) {
  if (!tabs.value.some((id) => pageIds.has(id))) return
  const rest = tabs.value.filter((id) => !pageIds.has(id))
  const activeIndex = page.value ? tabs.value.indexOf(page.value.id) : -1
  if (activeIndex < 0 || !pageIds.has(tabs.value[activeIndex])) {
    tabs.value = rest
    await persistTabs()
    return
  }
  // Une frappe qui n'a pas pu partir garde l'onglet ouvert, avec l'erreur.
  if (!(await saved())) return
  // La place de l'actif parmi les onglets qui restent : son voisin de droite y est, sinon celui de gauche.
  const index = activeIndex - tabs.value.slice(0, activeIndex).filter((id) => pageIds.has(id)).length
  tabs.value = rest
  const next = rest[index] ?? rest[index - 1] ?? null
  if (next) {
    try {
      await show(next)
      return
    } catch (e) {
      error.value = `Page illisible : ${e instanceof Error ? e.message : String(e)}`
    }
  }
  // Sans voisin lisible, aucune page n'est montée ; le voisin reste l'actif gardé, retenté à la prochaine ouverture.
  page.value = null
  citing.value = null
  await writeAddress(null)
  await persistTabs(next)
}

/** Les signets dans un nouvel ordre, gardés au serveur ; un échec va au bandeau, l'ordre reste celui montré. */
function reorderTabs(order: string[]) {
  if (busy.value) return
  tabs.value = order
  void persistTabs()
}

/** Un échec laisse la liste absente, sans fermer la page. */
async function readCiting(pageId: string) {
  try {
    citing.value = (await api.citingPages(pageId)).map((p) => p.id)
  } catch (e) {
    error.value = `Pages qui renvoient ici illisibles : ${e instanceof Error ? e.message : String(e)}`
  }
}

/**
 * Un clic arrive souvent pendant le geste que la sortie du titre vient d'envoyer (renommage) : il attend sa fin au lieu
 * d'être perdu. Un second clic pendant l'attente reste ignoré.
 */
async function open(pageId: string) {
  if (busy.value) await running
  if (busy.value || page.value?.id === pageId) return
  error.value = null
  // L'adresse change d'abord ; sa garde attend la frappe, puis la page s'ouvre (`watch` de `pageId`).
  void router.push({ name: 'notebook', params: { campaignId: props.campaign.id, pageId } })
}

/** Les ids des modules actifs dans la campagne, relus au serveur à chaque montage ; aucun tant qu'ils ne sont pas lus. */
const activeModuleIds = ref<string[]>([])
/** Les modules que le dialogue Modules propose : installés côté client et serveur. */
const offeredModules = ref<NotebookModule[] | null>(null)
/** Les modules du client actifs dans la campagne : seuls eux montrent panneau et section, et préparent le Carnet. */
const activeModules = computed(() => notebookModules.filter((module) => activeModuleIds.value.includes(module.id)))
const activePanelModules = computed(() => activeModules.value.filter((module) => module.panel))
/** Le module actif qui montre la page ouverte, si elle est une de ses entrées ; `null` sinon, ou s'il est inactif. */
const entryModule = computed(() => activeModules.value.find((module) => module.id === page.value?.entryOf && module.entryView) ?? null)
/** L'arbre et le dialogue Déplacer taisent les sections des modules inactifs ; les liens, eux, voient toutes les pages. */
const treePages = computed(() => visiblePages(notebook.value?.pages ?? [], activeModuleIds.value))

/** Le dialogue des modules de la campagne, et la raison d'un refus du serveur, affichée dans le dialogue. */
const modulesOpen = ref(false)
const modulesError = ref<string | null>(null)

function openModules() {
  if (busy.value) return
  modulesError.value = null
  modulesOpen.value = true
}

/**
 * Enregistre les modules choisis : la frappe en attente part d'abord. Un module activé prépare le Carnet (les pages de sa
 * section) avant que l'arbre ne soit relu ; un module désactivé disparaît, ses données restent au serveur.
 */
function saveModules(modules: string[]) {
  modulesError.value = null
  void call(async () => {
    if (!(await saved())) {
      modulesError.value = 'La page ouverte n’est pas enregistrée : modules inchangés.'
      return
    }
    const before = activeModuleIds.value
    try {
      activeModuleIds.value = await api.setModules(modules)
    } catch (e) {
      modulesError.value = e instanceof Error ? e.message : String(e)
      return
    }
    modulesOpen.value = false
    await prepareModules(activeModules.value.filter((module) => !before.includes(module.id)))
    notebook.value = await api.notebook()
  })
}

/** Lit les modules actifs ; un échec va au bandeau, et le Carnet s'ouvre sans module. */
async function readModules() {
  // Les modules installés ne servent qu'au dialogue Modules : leur échec ne retire rien au Carnet.
  void api
    .installedModules()
    .then((installed) => (offeredModules.value = availableModules(installed)))
    // Un message déjà affiché (modules actifs, préparation) passe d'abord.
    .catch((e) => (error.value ??= `Modules installés illisibles (${e instanceof Error ? e.message : String(e)})`))
  try {
    activeModuleIds.value = await api.modules()
  } catch (e) {
    error.value = `Modules illisibles (${e instanceof Error ? e.message : String(e)})`
  }
}

/** Chaque module actif prépare le Carnet avant sa lecture (les pages de sa section) ; un refus va au bandeau, sans bloquer. */
async function prepareModules(modules: readonly NotebookModule[]) {
  const results = await Promise.allSettled(modules.map((module) => module.prepare?.(props.campaign.id)))
  results.forEach((result, i) => {
    if (result.status === 'rejected') {
      const reason = result.reason instanceof Error ? result.reason.message : String(result.reason)
      error.value = `${modules[i].title} : Carnet non préparé (${reason})`
    }
  })
}

onMounted(() => {
  document.addEventListener('visibilitychange', onVisibilityChange)
  window.addEventListener('pagehide', onPageHide)
  void call(async () => {
    await readModules()
    await prepareModules(activeModules.value)
    notebook.value = await api.notebook()
    // Le serveur ne rend que des onglets dont la page existe. Tous fermés, ils le restent.
    const { pages, openPageIds, activePageId } = notebook.value
    tabs.value = openPageIds
    // La page de l'adresse d'abord ; sans page, ou illisible, l'onglet actif comme à l'ouverture de la campagne.
    if (props.pageId && (await tryShow(props.pageId))) return
    const target = activePageId ?? openPageIds[0]
    if (target) await show(target)
    else {
      await writeAddress(null)
      if (!pages.length) {
        await nextTick()
        firstTitle.value?.focus()
      }
    }
  })
})
onBeforeUnmount(() => {
  clearTimeout(searchTimer)
  searchRun++
  document.removeEventListener('visibilitychange', onVisibilityChange)
  window.removeEventListener('pagehide', onPageHide)
  void flush()
})

/** Crée la page, à la racine ou sous `parentId`, et l'ouvre déverrouillée. */
function createPage(newTitle: string, parentId: string | null, done = () => {}) {
  if (!newTitle || busy.value) return
  error.value = null
  call(async () => {
    if (!(await saved())) return
    const created = await api.createPage(newTitle, parentId)
    done()
    notebook.value = await api.notebook()
    tabs.value = [...tabs.value, created.id]
    display(created, true)
    // Une page neuve : rien ne peut encore la citer.
    citing.value = []
    await writeAddress(created.id, true)
    await persistTabs()
  })
}

function create() {
  createPage(trimmed.value, null, () => {
    title.value = ''
    creating.value = false
  })
}

/** Le champ « Nouvelle page » de la colonne, ouvert à la demande. */
const creating = ref(false)
const newTitle = ref<HTMLInputElement | null>(null)

async function toggleCreating() {
  creating.value = !creating.value
  if (!creating.value) return
  await nextTick()
  newTitle.value?.focus()
}

function cancelCreating() {
  creating.value = false
  title.value = ''
}

function rename(pageId: string, newTitle: string) {
  if (busy.value) return
  error.value = null
  call(async () => {
    try {
      await api.renamePage(pageId, newTitle)
      if (page.value?.id === pageId) page.value.title = newTitle
    } finally {
      // Refusé ou non, l'arbre et le titre reprennent ce que garde le serveur.
      notebook.value = await api.notebook()
      if (page.value) pageTitle.value = notebook.value.pages.find((p) => p.id === page.value?.id)?.title ?? page.value.title
    }
  })
}

/** Le titre en tête de page : Entrée ou sortie du champ envoie, s'il a changé et n'est pas vide. */
function submitTitle() {
  if (!page.value) return
  const newTitle = pageTitle.value.trim()
  if (!newTitle || newTitle === page.value.title) {
    pageTitle.value = page.value.title
    return
  }
  rename(page.value.id, newTitle)
}

/** Échap rend le titre d'avant ; la sortie du champ qui suit n'a alors rien à envoyer. */
function cancelTitle(e: KeyboardEvent) {
  if (page.value) pageTitle.value = page.value.title
  ;(e.target as HTMLInputElement).blur()
}

const tree = ref<InstanceType<typeof PageTree> | null>(null)

/** Le dialogue se ferme : le focus revient à la ligne de la page, que l'arbre a pu déplacer. */
async function closeMove() {
  const pageId = moving.value
  moving.value = null
  if (pageId) await tree.value?.focusPage(pageId)
}

/** Le choix du dialogue Déplacer. */
function move(parentId: string | null) {
  if (moving.value) moveTo(moving.value, parentId)
}

/** Range la page sous `parentId` (`null` : la racine), depuis le dialogue ou au bout d'un glisser dans l'arbre. */
function moveTo(pageId: string, parentId: string | null) {
  if (busy.value) return
  error.value = null
  call(async () => {
    try {
      await api.movePage(pageId, parentId)
    } finally {
      notebook.value = await api.notebook()
      reveal(pageId)
      await closeMove()
    }
  })
}

/** La recherche affichée suit l'arbre : une page supprimée ou restaurée y entre ou en sort. */
function refreshSearch() {
  if (searching.value) void search()
}

/** « Supprimer… » d'une ligne : le dialogue s'ouvre, puis dit ce qui part avec la page. */
function requestDelete(pageId: string) {
  if (busy.value) return
  error.value = null
  deleting.value = pageId
  deletion.value = null
  call(async () => {
    try {
      const read = await api.pageDeletion(pageId)
      if (deleting.value === pageId) deletion.value = read
    } catch (e) {
      // Le dialogue modal cacherait l'erreur du bandeau.
      deleting.value = null
      throw e
    }
  })
}

/** Annulée, la suppression rend le focus au bouton de menu de la ligne. */
function cancelDelete() {
  const pageId = deleting.value
  deleting.value = null
  deletion.value = null
  if (pageId) void tree.value?.focusMenu(pageId)
}

/**
 * Met la page et ses sous-pages à la corbeille. La frappe en attente de la page ouverte, si elle en est, part d'abord ;
 * si elle échoue, rien n'est supprimé. Les onglets du lot se ferment.
 */
function confirmDelete() {
  const pageId = deleting.value
  if (!pageId || busy.value) return
  error.value = null
  call(async () => {
    try {
      const pages = notebook.value?.pages ?? []
      const lot = branch(pages, pageId)
      if (page.value && lot.has(page.value.id) && !(await saved())) return
      const parentId = pages.find((p) => p.id === pageId)?.parentId ?? null
      await api.deletePage(pageId)
      deleting.value = null
      deletion.value = null
      // Les onglets du lot se ferment avant la relecture : un échec de celle-ci ne laisse pas ouverte une page supprimée.
      await closeTabs(lot)
      notebook.value = await api.notebook()
      refreshSearch()
      // La ligne a disparu : le focus passe au parent, sinon au champ de recherche, sinon au titre du Carnet vide.
      await nextTick()
      if (parentId) await tree.value?.focusPage(parentId)
      else (searchField.value ?? firstTitle.value)?.focus()
    } finally {
      deleting.value = null
      deletion.value = null
    }
  })
}

/** Un échec ferme la corbeille : son dialogue modal cacherait l'erreur du bandeau. */
function inTrash(action: () => Promise<void>) {
  if (busy.value) return
  error.value = null
  let failed = false
  // Fermée une fois l'appel fini : le bouton « Corbeille », rallumé, peut reprendre le focus.
  void call(async () => {
    try {
      await action()
    } catch (e) {
      failed = true
      throw e
    }
  }).then(() => {
    if (failed) closeTrash()
  })
}

function openTrash() {
  if (busy.value) return
  trashOpen.value = true
  trashEntries.value = null
  inTrash(async () => {
    trashEntries.value = await api.trash()
  })
}

function closeTrash() {
  trashOpen.value = false
  void nextTick(() => trashButton.value?.focus())
}

/** La page revient à sa place, ou à la racine, avec ses sous-pages ; l'arbre se déplie jusqu'à elle. */
function restore(pageId: string) {
  inTrash(async () => {
    await api.restorePage(pageId)
    notebook.value = await api.notebook()
    reveal(pageId)
    refreshSearch()
    trashEntries.value = await api.trash()
  })
}

function emptyTrash() {
  inTrash(async () => {
    await api.emptyTrash()
    trashEntries.value = await api.trash()
  })
}

/**
 * Un fichier lâché hors du texte éditable (marge, arbre, page verrouillée) ne remplace jamais l'application : le
 * navigateur l'ouvrirait. Le texte éditable prend le sien en main avant.
 */
function keepFiles(e: DragEvent) {
  if (!e.dataTransfer?.types.includes('Files')) return
  // Au-dessus du texte éditable, ProseMirror a déjà accepté le dépôt ; ailleurs, le curseur dit qu'on ne dépose rien.
  if (e.type === 'dragover' && !e.defaultPrevented) e.dataTransfer.dropEffect = 'none'
  e.preventDefault()
}

/** Ouvre un combat sur l'écran de combat : rejoué s'il est terminé, en direct sinon (déjà repris par l'entrée). */
function openCombat(combatId: string, ended: boolean) {
  const campaignId = props.campaign.id
  void router.push(ended ? { name: 'combatReplay', params: { campaignId, combatId } } : { name: 'combat', params: { campaignId } })
}

/** La garde de sortie attend la frappe : si elle n'a pas pu partir, on reste à l'écran, avec l'erreur. */
function leave(target: 'home' | 'notebook' | 'combat') {
  if (target === 'notebook') return
  void router.push(target === 'home' ? { name: 'home' } : { name: 'combat', params: { campaignId: props.campaign.id } })
}
</script>

<template>
  <div class="notebook" :class="{ 'with-modules': notebook?.pages.length && activePanelModules.length }" @dragover="keepFiles" @drop="keepFiles">
    <CampaignColumn :campaign-name="campaign.name" current="notebook" @navigate="leave">
      <template v-if="notebook?.pages.length">
        <div class="search">
          <input
            ref="searchField"
            v-model="query"
            type="search"
            placeholder="Chercher dans le Carnet"
            aria-label="Chercher dans le Carnet"
            @keydown.esc.prevent="clearSearch"
          />
          <button v-if="query" type="button" class="icon-button ghost clear" aria-label="Effacer la recherche" title="Effacer la recherche" @click="clearSearch">
            <AppIcon name="remove" />
          </button>
        </div>
        <div class="pages-head">
          <h2 id="pages-title" class="label">Pages</h2>
          <button
            type="button"
            class="icon-button ghost add"
            :aria-expanded="creating"
            aria-label="Nouvelle page"
            title="Nouvelle page à la racine"
            :disabled="busy"
            @click="toggleCreating"
          >
            <AppIcon name="plus" />
          </button>
        </div>
        <form v-if="creating" class="new-page" @submit.prevent="create">
          <input
            ref="newTitle"
            v-model="title"
            type="text"
            :maxlength="MAX_TITLE_LENGTH"
            placeholder="Titre de la page"
            aria-label="Titre de la nouvelle page"
            required
            @keydown.esc.prevent="cancelCreating"
          />
          <button type="submit" class="primary" :disabled="!trimmed || busy">Créer</button>
        </form>
        <div class="tree-area" aria-labelledby="pages-title">
          <SearchResults
            v-if="searching"
            :results="found?.results ?? null"
            :query="found?.query ?? query"
            :pages="notebook.pages"
            @open="open"
          />
          <PageTree
            v-else
            ref="tree"
            :pages="treePages"
            :current-id="page?.id ?? null"
            :expanded="expanded"
            :busy="busy"
            :page-href="pageHref"
            @open="open"
            @toggle="toggle"
            @create="createPage"
            @rename="rename"
            @move="(pageId) => (moving = pageId)"
            @delete="requestDelete"
            @drop="moveTo"
          />
        </div>
      </template>

      <template #foot>
        <button ref="trashButton" type="button" class="foot-button" :disabled="busy" @click="openTrash">
          <AppIcon name="trash" />
          Corbeille
        </button>
        <button type="button" class="foot-button" title="Modules de la campagne" :disabled="busy || !notebook" @click="openModules">
          <AppIcon name="modules" />
          Modules
        </button>
        <ThemeButton />
      </template>
    </CampaignColumn>

    <main class="desk">
      <div v-if="error" class="error" role="alert">
        <p>{{ error }}</p>
        <button type="button" @click="error = null">Fermer</button>
      </div>

      <p v-if="notebook === null" class="loading">{{ error ? 'Carnet illisible.' : 'Lecture du Carnet…' }}</p>

      <section v-else-if="!notebook.pages.length" class="sheet first" aria-labelledby="first-page-title">
        <h2 id="first-page-title" class="page-title">Un Carnet vierge</h2>
        <p class="hint">Sessions, personnages, lieux, fils de l’intrigue : tout ce que vous écrivez pendant la partie vit ici. Commencez par une page.</p>
        <form class="new-page" @submit.prevent="create">
          <input
            ref="firstTitle"
            v-model="title"
            type="text"
            :maxlength="MAX_TITLE_LENGTH"
            placeholder="Session 1"
            aria-label="Titre de la première page"
            required
          />
          <button type="submit" class="primary" :disabled="!trimmed || busy">Créer la page</button>
        </form>
      </section>

      <div v-else class="sheet">
        <PageTabs
          v-if="tabs.length"
          ref="tabBar"
          :tab-ids="tabs"
          :active-id="page?.id ?? null"
          :pages="notebook.pages"
          :busy="busy"
          @activate="open"
          @close="closeTab"
          @reorder="reorderTabs"
        />
        <!-- Une entrée de combat n'a pas de texte : sa carte en petit, qui ouvre le combat. -->
        <article v-if="page?.combatId" :key="page.id" class="leaf" aria-labelledby="page-title">
          <header class="page-head">
            <h2 id="page-title" class="page-title">{{ page.title }}</h2>
          </header>
          <CombatEntryView
            :campaign-id="campaign.id"
            :combat-id="page.combatId"
            @open="openCombat"
            @error="(message) => (error = message)"
          />
          <CitingPages v-if="citing" :page-ids="citing" :pages="notebook.pages" @open="open" />
        </article>
        <!-- Une entrée de module n'a pas de texte : la vue de son module, s'il est actif. -->
        <article v-else-if="page?.entryOf" :key="page.id" class="leaf" aria-labelledby="page-title">
          <header class="page-head">
            <h2 id="page-title" class="page-title">{{ page.title }}</h2>
          </header>
          <component
            :is="entryModule.entryView"
            v-if="entryModule"
            :campaign-id="campaign.id"
            :page-id="page.id"
            @error="(message: string) => (error = message)"
          />
          <p v-else class="hint">Module {{ page.entryOf }} inactif dans cette campagne.</p>
          <CitingPages v-if="citing" :page-ids="citing" :pages="notebook.pages" @open="open" />
        </article>
        <article v-else-if="page" :key="page.id" class="leaf" aria-labelledby="page-title">
          <header class="page-head">
            <h2 id="page-title" class="page-title">
              <input
                v-if="unlocked"
                v-model="pageTitle"
                class="title-field"
                type="text"
                :maxlength="MAX_TITLE_LENGTH"
                aria-label="Titre de la page"
                @keydown.enter.prevent="($event.target as HTMLInputElement).blur()"
                @keydown.esc.prevent="cancelTitle($event)"
                @blur="submitTitle"
              />
              <template v-else>{{ page.title }}</template>
            </h2>
            <span class="page-tools">
              <template v-if="unlocked">
                <button
                  type="button"
                  class="icon-button ghost"
                  aria-label="Insérer une image"
                  title="Insérer une image PNG, JPEG ou WebP au curseur"
                  :disabled="uploading"
                  @click="imageInput?.click()"
                >
                  <AppIcon name="image" />
                </button>
                <input ref="imageInput" type="file" hidden :accept="IMAGE_TYPES.join(',')" @change="chooseImage" />
              </template>
              <button
                type="button"
                class="lock"
                :class="{ writing: unlocked }"
                aria-label="Page verrouillée"
                :aria-pressed="!unlocked"
                :title="unlocked ? 'Verrouiller la page' : 'Déverrouiller la page pour l’écrire'"
                @click="unlocked = !unlocked"
              >
                <AppIcon :name="unlocked ? 'unlock' : 'lock'" />
                <span aria-hidden="true">{{ unlocked ? 'Écriture' : 'Lecture' }}</span>
              </button>
            </span>
          </header>
          <PageEditor
            ref="editor"
            :content="page.content"
            :editable="unlocked"
            :label="`Texte de ${page.title}`"
            :pages="notebook.pages"
            :page-href="pageHref"
            @change="onChange"
            @open="open"
            @refuse="(reason) => (error = reason)"
          />
          <CitingPages v-if="citing" :page-ids="citing" :pages="notebook.pages" @open="open" />
        </article>
        <p v-else-if="!busy" class="hint closed">Aucune page ouverte. Choisir une page dans l'arbre.</p>
      </div>
    </main>

    <aside v-if="notebook?.pages.length && activePanelModules.length" class="modules" aria-label="Modules du Carnet">
      <FoldSection v-for="module in activePanelModules" :key="module.id" :title="module.title" :storage-key="`module.${module.id}`">
        <component :is="module.panel" :context="moduleContext" />
      </FoldSection>
    </aside>

    <MovePageDialog :pages="treePages" :page-id="moving" :busy="busy" @move="move" @close="closeMove" />
    <DeletePageDialog
      :page="notebook?.pages.find((p) => p.id === deleting) ?? null"
      :deletion="deletion"
      :busy="busy"
      @confirm="confirmDelete"
      @close="cancelDelete"
    />
    <CampaignModulesDialog
      :open="modulesOpen"
      :available="offeredModules"
      :active="activeModuleIds"
      :busy="busy"
      :error="modulesError"
      @save="saveModules"
      @close="modulesOpen = false"
    />
    <ResultDialog
      :blocks="result"
      :pages="notebook?.pages ?? []"
      :page-href="pageHref"
      :copy-error="copyError"
      @copy="copyResult"
      @open="openFromResult"
      @close="result = null"
    />
    <TrashDialog :open="trashOpen" :entries="trashEntries" :busy="busy" @restore="restore" @empty="emptyTrash" @close="closeTrash" />
  </div>
</template>

<style scoped>
/*
 * Trois colonnes : la campagne sur toute la hauteur, le bureau où repose la feuille, les modules.
 * Les deux colonnes latérales restent en vue et défilent seules ; seule la feuille fait défiler l'écran.
 */
.notebook {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 17.5rem minmax(0, 1fr);
}
.notebook.with-modules {
  grid-template-columns: 17.5rem minmax(0, 1fr) 21rem;
}

/* — Colonne de la campagne : ce que le Carnet y range — */
.search {
  position: relative;
  display: flex;
}
.search input {
  width: 100%;
  padding-right: 2.2rem;
}
.search .clear {
  position: absolute;
  right: 0.15rem;
  top: 50%;
  transform: translateY(-50%);
  width: 2rem;
  height: 2rem;
}
.search .clear :deep(.icon) {
  width: 16px;
  height: 16px;
}
/* Le bouton Effacer remplace celui du navigateur, absent de Firefox. */
input[type='search']::-webkit-search-cancel-button {
  appearance: none;
}
.pages-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 0.2rem 0 -0.5rem 0.15rem;
}
.label {
  margin: 0;
  font-family: var(--font-title);
  font-weight: 700;
  font-size: 0.95rem;
  letter-spacing: 0.06em;
  color: var(--ink-muted);
}
.pages-head .add {
  width: 2rem;
  height: 2rem;
}
.new-page {
  display: flex;
  gap: 0.4rem;
}
.new-page input {
  flex: 1;
  min-width: 0;
}
.new-page button {
  flex: none;
}
/* L'arbre et les résultats défilent seuls ; la zone « Racine du Carnet » d'un glisser déborde au-dessus, sur « Pages ». */
.tree-area {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
.tree-area > :deep(*) {
  flex: 1;
  min-height: 0;
}
.tree-area > :deep(.results) {
  overflow-y: auto;
}
/* — Le bureau et sa feuille — */
.desk {
  min-width: 0;
  padding: 1.5rem clamp(1rem, 3vw, 2.5rem) 3rem;
}
.desk .error {
  max-width: 54rem;
  margin: 0 auto 1rem;
  border-radius: 6px;
  border: 1px solid var(--danger);
}
.loading {
  margin: 0;
  padding: 3rem 0;
  text-align: center;
  color: var(--ink-muted);
}
.sheet {
  max-width: 54rem;
  min-height: calc(100vh - 4.5rem);
  margin: 0 auto;
  padding: 0 clamp(1.75rem, 7%, 4.5rem) 3rem;
  background: var(--sheet);
  border-radius: 3px 8px 8px 3px;
}
.hint {
  margin: 0;
  color: var(--ink-muted);
}
.hint.closed {
  padding-top: 4rem;
  text-align: center;
  font-style: italic;
}
.leaf {
  padding-top: 1.75rem;
}
.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1.5rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid var(--rule);
}
.page-title {
  margin: 0;
  flex: 1;
  min-width: 0;
  font-family: var(--font-title);
  font-weight: 700;
  font-size: 2.1rem;
  line-height: 1.15;
  letter-spacing: 0.01em;
  color: var(--ink);
  overflow-wrap: anywhere;
  text-wrap: balance;
}
/* Déverrouillé, le titre devient un champ de même police, même taille, même place. */
.page-title .title-field {
  width: 100%;
  margin: -0.15rem 0 -0.15rem -0.4rem;
  padding: 0.1rem 0.35rem;
  font: inherit;
  letter-spacing: inherit;
  color: inherit;
  background: transparent;
  border: 1px solid transparent;
  border-radius: 4px;
}
.page-title .title-field:hover,
.page-title .title-field:focus {
  border-color: var(--control-border);
  background: var(--wood-sunken);
}
.page-tools {
  flex: none;
  display: flex;
  align-items: center;
  gap: 0.25rem;
  margin-top: 0.2rem;
}
/* Le verrou dit l'état en mots : Lecture ou Écriture. */
.lock {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.35rem 0.75rem 0.35rem 0.55rem;
  border-color: var(--rule);
  background: none;
  color: var(--ink-muted);
  font-size: 0.95rem;
}
.lock.writing {
  border-color: var(--control-border);
  background: var(--wood-sunken);
  color: var(--ink);
  font-weight: 700;
}
.lock :deep(.icon) {
  width: 18px;
  height: 18px;
}
.first {
  max-width: 38rem;
  min-height: 0;
  margin-top: 12vh;
  padding: 2.5rem 3rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
  border-radius: 8px;
}
.first .page-title {
  flex: none;
}
.first .hint {
  font-family: var(--font-text);
  font-size: 1.15rem;
  line-height: 1.55;
}

/* — Les modules — */
.modules {
  position: sticky;
  top: 0;
  height: 100vh;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 0.9rem;
  padding: 1.5rem 1.25rem 1.5rem 0;
}

@media (max-width: 1180px) {
  .notebook.with-modules {
    grid-template-columns: 16rem minmax(0, 1fr) 19rem;
  }
}
/* Sous 900px : une colonne ; plus rien n'est collant, l'arbre garde une hauteur raisonnable. */
@media (max-width: 900px) {
  .notebook,
  .notebook.with-modules {
    grid-template-columns: minmax(0, 1fr);
  }
  .modules {
    position: static;
    height: auto;
  }
  .tree-area {
    max-height: 45vh;
  }
  .modules {
    overflow: visible;
    padding: 0 16px 2rem;
  }
  .desk {
    padding: 1rem 16px 2rem;
  }
  .sheet {
    min-height: 0;
    padding-inline: 1.25rem;
  }
  .first {
    margin-top: 1rem;
    padding: 1.5rem 1.25rem;
  }
  .page-title {
    font-size: 1.7rem;
  }
}
</style>

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
import ResultDialog from './ResultDialog.vue'
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
  hasOpenPage: () => page.value !== null && editor.value !== null,
  writeToOpenPage: (blocks) => {
    if (!page.value || !editor.value) return 'Aucune page ouverte : ouvrir une page pour y écrire.'
    editor.value.insertBlocks(blocks)
    return null
  },
  writeResult: (blocks) => {
    if (page.value && editor.value) editor.value.insertBlocks(blocks)
    else showResult(blocks)
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
  createPage(trimmed.value, null, () => (title.value = ''))
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

/** La garde de sortie attend la frappe : si elle n'a pas pu partir, on reste à l'écran, avec l'erreur. */
function leave(target: 'home' | 'combat') {
  void router.push(target === 'home' ? { name: 'home' } : { name: 'combat', params: { campaignId: props.campaign.id } })
}
</script>

<template>
  <div class="notebook" @dragover="keepFiles" @drop="keepFiles">
    <header class="bar">
      <button type="button" class="icon-button" aria-label="Accueil" title="Accueil : changer de campagne" @click="leave('home')">
        <AppIcon name="home" />
      </button>
      <h1 class="app-name">Solo VTT</h1>
      <p class="campaign-name">{{ campaign.name }}</p>
      <span class="spacer" />
      <button type="button" title="Modules de la campagne" :disabled="busy || !notebook" @click="openModules">
        Modules
      </button>
      <button type="button" class="combat-button" title="Écran de combat de la campagne" @click="leave('combat')">
        <AppIcon name="combat" />
        Combat
      </button>
      <ThemeButton />
    </header>
    <div v-if="error" class="error" role="alert">
      <p>{{ error }}</p>
      <button type="button" @click="error = null">Fermer</button>
    </div>

    <p v-if="notebook === null" class="loading">{{ error ? 'Carnet illisible.' : 'Lecture du Carnet…' }}</p>

    <main v-else-if="!notebook.pages.length" class="empty">
      <section aria-labelledby="first-page-title">
        <h2 id="first-page-title" class="panel-title">Carnet</h2>
        <hr class="tapered" />
        <p class="hint">Aucune page. Créer la première page du Carnet.</p>
        <form class="create" @submit.prevent="create">
          <label>
            Titre
            <input ref="firstTitle" v-model="title" type="text" :maxlength="MAX_TITLE_LENGTH" required />
          </label>
          <button type="submit" :disabled="!trimmed || busy">Créer</button>
        </form>
        <button ref="trashButton" type="button" class="trash-button" :disabled="busy" @click="openTrash">
          <AppIcon name="trash" />
          Corbeille
        </button>
      </section>
    </main>

    <div v-else class="content" :class="{ 'with-modules': activePanelModules.length }">
      <nav class="pages" aria-labelledby="pages-title">
        <h2 id="pages-title" class="panel-title">Pages</h2>
        <hr class="tapered" />
        <div class="search">
          <input
            ref="searchField"
            v-model="query"
            type="search"
            placeholder="Chercher"
            aria-label="Chercher dans le Carnet"
            @keydown.esc.prevent="clearSearch"
          />
          <button v-if="query" type="button" class="icon-button" aria-label="Effacer la recherche" title="Effacer la recherche" @click="clearSearch">
            <AppIcon name="remove" />
          </button>
        </div>
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
        <form class="create new-page" @submit.prevent="create">
          <label>
            Nouvelle page
            <input v-model="title" type="text" :maxlength="MAX_TITLE_LENGTH" placeholder="Titre" required />
          </label>
          <button type="submit" :disabled="!trimmed || busy">Créer</button>
        </form>
        <button ref="trashButton" type="button" class="trash-button" :disabled="busy" @click="openTrash">
          <AppIcon name="trash" />
          Corbeille
        </button>
      </nav>

      <main class="page">
        <PageTabs
          v-if="tabs.length"
          ref="tabBar"
          :tab-ids="tabs"
          :active-id="page?.id ?? null"
          :pages="notebook.pages"
          :busy="busy"
          @activate="open"
          @close="closeTab"
        />
        <article v-if="page" :key="page.id" aria-labelledby="page-title">
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
                  class="icon-button"
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
                class="icon-button"
                aria-label="Page verrouillée"
                :aria-pressed="!unlocked"
                :title="unlocked ? 'Verrouiller la page' : 'Déverrouiller la page pour l’écrire'"
                @click="unlocked = !unlocked"
              >
                <AppIcon :name="unlocked ? 'unlock' : 'lock'" />
              </button>
            </span>
          </header>
          <hr class="tapered" />
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
        <p v-else-if="!busy" class="hint">Aucune page ouverte. Choisir une page dans l'arbre.</p>
      </main>
      <aside v-if="activePanelModules.length" class="modules" aria-label="Modules du Carnet">
        <section v-for="module in activePanelModules" :key="module.id" :aria-labelledby="`module-${module.id}`">
          <h2 :id="`module-${module.id}`" class="panel-title">{{ module.title }}</h2>
          <hr class="tapered" />
          <component :is="module.panel" :context="moduleContext" />
        </section>
      </aside>
      <MovePageDialog :pages="treePages" :page-id="moving" :busy="busy" @move="move" @close="closeMove" />
    </div>
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
.notebook {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}
.combat-button {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
}
.loading,
.hint {
  margin: 0;
  color: var(--ink-muted);
}
.loading {
  padding: 2rem 16px;
  text-align: center;
}
.empty {
  flex: 1;
  width: 100%;
  max-width: 34rem;
  box-sizing: border-box;
  margin: 0 auto;
  padding: 2rem 16px;
}
section,
.pages {
  display: flex;
  flex-direction: column;
  gap: 0.9rem;
  padding: 1.1rem 1.25rem 1.25rem;
  background: var(--wood);
  border: 2px solid var(--page);
  border-radius: 6px;
}
section .tapered,
.pages .tapered {
  margin: -0.5rem 0 0;
}
.content {
  flex: 1;
  display: grid;
  grid-template-columns: 17rem minmax(0, 1fr);
  gap: 1.25rem;
  padding: 1.25rem 16px;
  align-items: start;
}
/* Les panneaux des modules, contre la page (qui ne dépasse pas 46rem) ; la colonne n'existe que si un module en montre un. */
.content.with-modules {
  grid-template-columns: 17rem minmax(0, 46rem) 18rem;
}
.modules {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  position: sticky;
  top: 1.25rem;
}
/* L'arbre reste en vue quand la page défile, et défile lui-même au-delà de la hauteur de la fenêtre. */
.pages {
  position: sticky;
  top: 1.25rem;
  max-height: calc(100vh - 2.5rem);
  box-sizing: border-box;
}
.pages .new-page {
  flex: none;
}
.create {
  display: flex;
  align-items: flex-end;
  gap: 0.75rem;
}
.new-page {
  padding-top: 0.75rem;
  border-top: 1px solid var(--rule);
}
/* Au pied du panneau, discret : on y va rarement. */
.trash-button {
  flex: none;
  align-self: flex-start;
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  background: none;
  border-color: transparent;
  color: var(--ink-muted);
}
.trash-button:hover:not(:disabled) {
  color: var(--ink);
}
.trash-button :deep(.icon) {
  width: 18px;
  height: 18px;
}
label {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  color: var(--ink-muted);
}
input[type='text'],
input[type='search'] {
  width: 100%;
  box-sizing: border-box;
}
.search {
  flex: none;
  display: flex;
  align-items: center;
  gap: 0.25rem;
}
/* Le bouton Effacer remplace celui du navigateur, absent de Firefox. */
input[type='search']::-webkit-search-cancel-button {
  appearance: none;
}
.page {
  min-width: 0;
  max-width: 46rem;
}
.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}
.page-tools {
  flex: none;
  display: flex;
  align-items: center;
  gap: 0.25rem;
}
.page-title {
  margin: 0;
  min-width: 0;
  font-family: var(--font-title);
  font-weight: 700;
  font-size: 1.8rem;
  color: var(--ink);
  overflow-wrap: anywhere;
}
/* Déverrouillé, le titre devient un champ de même police, même taille, même place. */
.page-title .title-field {
  width: 100%;
  box-sizing: border-box;
  margin: -0.15rem 0 -0.15rem -0.35rem;
  padding: 0.1rem 0.3rem;
  font: inherit;
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
.page .tapered {
  margin: 0.4rem 0 1rem;
}
@media (max-width: 900px) {
  .content,
  .content.with-modules {
    grid-template-columns: minmax(0, 1fr);
  }
  .pages,
  .modules {
    position: static;
    max-height: none;
  }
}
</style>

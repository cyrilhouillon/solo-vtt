<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { campaignApi } from '../api'
import AppIcon from './AppIcon.vue'
import ThemeButton from './ThemeButton.vue'
import PageEditor from './PageEditor.vue'
import PageTree from './PageTree.vue'
import MovePageDialog from './MovePageDialog.vue'
import CitingPages from './CitingPages.vue'
import SearchResults from './SearchResults.vue'
import { ancestors, buildTree } from '../notebook/tree'
import { MAX_TITLE_LENGTH } from '../notebook/context'
import type { CampaignView, NotebookView, PageDocument, PageView, SearchResult } from '../types'

const props = defineProps<{ campaign: CampaignView }>()
/** Retour à l'accueil, ou passage à l'écran de combat de la campagne. */
const emit = defineEmits<{ home: []; combat: [] }>()
const api = campaignApi(props.campaign.id)

/** Le Carnet, `null` tant qu'il n'est pas lu. */
const notebook = ref<NotebookView | null>(null)
const page = ref<PageView | null>(null)
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
/** Le titre en tête de page, éditable une fois la page déverrouillée. */
const pageTitle = ref('')
/** Les pages qui citent la page ouverte, `null` tant qu'elles ne sont pas lues. */
const citing = ref<string[] | null>(null)

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

/** Ouvre la page et la note comme dernière consultée, puis lit les pages qui la citent. */
async function show(pageId: string) {
  if (!(await saved())) return
  const opened = await api.page(pageId)
  // Vidée avant d'afficher : la liste de la page précédente ne passe jamais sous celle-ci.
  citing.value = null
  display(opened, false)
  await api.setLastPage(pageId)
  await readCiting(pageId)
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
  call(() => show(pageId))
}

onMounted(() => {
  document.addEventListener('visibilitychange', onVisibilityChange)
  window.addEventListener('pagehide', onPageHide)
  void call(async () => {
    notebook.value = await api.notebook()
    const { pages, lastPageId } = notebook.value
    const target = pages.find((p) => p.id === lastPageId) ?? buildTree(pages)[0]?.page
    if (target) await show(target.id)
    else {
      await nextTick()
      firstTitle.value?.focus()
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
    display(created, true)
    // Une page neuve : rien ne peut encore la citer.
    citing.value = []
    await api.setLastPage(created.id)
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

async function leave(target: 'home' | 'combat') {
  // Une frappe qui n'a pas pu partir reste à l'écran, avec l'erreur : on ne quitte pas en la perdant.
  if (!(await saved())) return
  if (target === 'home') emit('home')
  else emit('combat')
}
</script>

<template>
  <div class="notebook">
    <header class="bar">
      <button type="button" class="icon-button" aria-label="Accueil" title="Accueil : changer de campagne" @click="leave('home')">
        <AppIcon name="home" />
      </button>
      <h1 class="app-name">Solo VTT</h1>
      <p class="campaign-name">{{ campaign.name }}</p>
      <span class="spacer" />
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
      </section>
    </main>

    <div v-else class="content">
      <nav class="pages" aria-labelledby="pages-title">
        <h2 id="pages-title" class="panel-title">Pages</h2>
        <hr class="tapered" />
        <div class="search">
          <input
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
          :pages="notebook.pages"
          :current-id="page?.id ?? null"
          :expanded="expanded"
          :busy="busy"
          @open="open"
          @toggle="toggle"
          @create="createPage"
          @rename="rename"
          @move="(pageId) => (moving = pageId)"
          @drop="moveTo"
        />
        <form class="create new-page" @submit.prevent="create">
          <label>
            Nouvelle page
            <input v-model="title" type="text" :maxlength="MAX_TITLE_LENGTH" placeholder="Titre" required />
          </label>
          <button type="submit" :disabled="!trimmed || busy">Créer</button>
        </form>
      </nav>

      <main class="page">
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
          </header>
          <hr class="tapered" />
          <PageEditor
            :content="page.content"
            :editable="unlocked"
            :label="`Texte de ${page.title}`"
            :pages="notebook.pages"
            @change="onChange"
            @open="open"
          />
          <CitingPages v-if="citing" :page-ids="citing" :pages="notebook.pages" @open="open" />
        </article>
      </main>
      <MovePageDialog :pages="notebook.pages" :page-id="moving" :busy="busy" @move="move" @close="closeMove" />
    </div>
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
  .content {
    grid-template-columns: minmax(0, 1fr);
  }
  .pages {
    position: static;
    max-height: none;
  }
}
</style>

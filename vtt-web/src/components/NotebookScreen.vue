<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { campaignApi } from '../api'
import AppIcon from './AppIcon.vue'
import { initialTheme, saveTheme, type Theme } from '../theme'
import type { CampaignView, NotebookView, PageView } from '../types'

const props = defineProps<{ campaign: CampaignView }>()
/** Retour à l'accueil, ou passage à l'écran de combat de la campagne. */
const emit = defineEmits<{ home: []; combat: [] }>()
const api = campaignApi(props.campaign.id)

/** Même limite que le serveur (`NotebookResource.MAX_TITLE_LENGTH`). */
const MAX_TITLE_LENGTH = 120

/** Le Carnet, `null` tant qu'il n'est pas lu. */
const notebook = ref<NotebookView | null>(null)
const page = ref<PageView | null>(null)
const title = ref('')
const trimmed = computed(() => title.value.trim())
const busy = ref(false)
const error = ref<string | null>(null)
const theme = ref<Theme>(initialTheme())
const firstTitle = ref<HTMLInputElement | null>(null)

function toggleTheme() {
  theme.value = theme.value === 'dark' ? 'light' : 'dark'
  saveTheme(theme.value)
}

async function call(action: () => Promise<void>) {
  busy.value = true
  try {
    await action()
  } catch (e) {
    error.value = e instanceof Error ? e.message : String(e)
  } finally {
    busy.value = false
  }
}

/** Ouvre la page et la note comme dernière consultée. */
async function show(pageId: string) {
  page.value = await api.page(pageId)
  await api.setLastPage(pageId)
}

function open(pageId: string) {
  if (busy.value || page.value?.id === pageId) return
  error.value = null
  call(() => show(pageId))
}

onMounted(() =>
  call(async () => {
    notebook.value = await api.notebook()
    const { pages, lastPageId } = notebook.value
    const target = pages.find((p) => p.id === lastPageId) ?? pages[0]
    if (target) await show(target.id)
    else {
      await nextTick()
      firstTitle.value?.focus()
    }
  }),
)

function create() {
  if (!trimmed.value || busy.value) return
  error.value = null
  call(async () => {
    const created = await api.createPage(trimmed.value)
    title.value = ''
    notebook.value = await api.notebook()
    page.value = created
    await api.setLastPage(created.id)
  })
}

function leave(target: 'home' | 'combat') {
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
        <ul class="list">
          <li v-for="entry in notebook.pages" :key="entry.id">
            <button
              type="button"
              class="page-link"
              :aria-current="entry.id === page?.id ? 'page' : undefined"
              @click="open(entry.id)"
            >
              {{ entry.title }}
            </button>
          </li>
        </ul>
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
            <h2 id="page-title" class="page-title">{{ page.title }}</h2>
          </header>
          <hr class="tapered" />
          <div class="page-body" />
        </article>
      </main>
    </div>
  </div>
</template>

<style scoped>
.notebook {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}
.bar {
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
.campaign-name {
  margin: 0;
  min-width: 0;
  max-width: 16rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-family: var(--font-title);
  font-weight: 500;
  font-size: 1.1rem;
  color: var(--ink);
}
.spacer {
  flex: 1;
}
.icon-button {
  display: grid;
  place-items: center;
  width: 2.5rem;
  height: 2.5rem;
  padding: 0;
}
.combat-button {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
}
.error {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.45rem 1rem;
  background: var(--wood-raised);
  border-bottom: 2px solid var(--danger);
  color: var(--danger);
  font-weight: 500;
}
.error p {
  margin: 0;
}
.error button {
  flex: none;
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
.list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}
.page-link {
  width: 100%;
  text-align: left;
  background: none;
  border-color: transparent;
  color: var(--ink-muted);
  overflow-wrap: anywhere;
}
.page-link[aria-current='page'] {
  color: var(--ink);
  background: var(--wood-sunken);
  border-color: var(--rule);
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
input[type='text'] {
  width: 100%;
  box-sizing: border-box;
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
.page .tapered {
  margin: 0.4rem 0 1rem;
}
@media (max-width: 900px) {
  .content {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>

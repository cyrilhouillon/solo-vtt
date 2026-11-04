<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api } from '../api'
import ThemeButton from './ThemeButton.vue'
import type { CampaignView } from '../types'

const emit = defineEmits<{ open: [campaign: CampaignView] }>()

/** Même limite que le serveur (`CampaignResource.MAX_NAME_LENGTH`). */
const MAX_NAME_LENGTH = 80

/** Les campagnes, `null` tant qu'elles ne sont pas lues. */
const campaigns = ref<CampaignView[] | null>(null)
const name = ref('')
const trimmed = computed(() => name.value.trim())
const busy = ref(false)
const error = ref<string | null>(null)

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

onMounted(() =>
  call(async () => {
    campaigns.value = await api.campaigns()
  }),
)

function create() {
  if (!trimmed.value || busy.value) return
  error.value = null
  call(async () => {
    await api.createCampaign(trimmed.value)
    name.value = ''
    campaigns.value = await api.campaigns()
  })
}

</script>

<template>
  <div class="home">
    <header class="bar">
      <h1 class="app-name">Solo VTT</h1>
      <span class="spacer" />
      <ThemeButton />
    </header>
    <div v-if="error" class="error" role="alert">
      <p>{{ error }}</p>
      <button type="button" @click="error = null">Fermer</button>
    </div>

    <main class="content">
      <section aria-labelledby="campaigns-title">
        <h2 id="campaigns-title" class="panel-title">Campagnes</h2>
        <hr class="tapered" />
        <p v-if="campaigns === null" class="hint">{{ error ? 'Campagnes illisibles.' : 'Lecture des campagnes…' }}</p>
        <p v-else-if="!campaigns.length" class="hint">Aucune campagne.</p>
        <ul v-else class="list">
          <li v-for="campaign in campaigns" :key="campaign.id">
            <strong class="name">{{ campaign.name }}</strong>
            <button type="button" :disabled="busy" :aria-label="`Ouvrir ${campaign.name}`" @click="emit('open', campaign)">Ouvrir</button>
          </li>
        </ul>
      </section>

      <section aria-labelledby="new-campaign-title">
        <h2 id="new-campaign-title" class="panel-title">Nouvelle campagne</h2>
        <hr class="tapered" />
        <form class="create" @submit.prevent="create">
          <label>
            Nom
            <input v-model="name" type="text" :maxlength="MAX_NAME_LENGTH" required />
          </label>
          <button type="submit" :disabled="!trimmed || busy">Créer</button>
        </form>
      </section>
    </main>
  </div>
</template>

<style scoped>
.home {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}
.content {
  flex: 1;
  width: 100%;
  max-width: 34rem;
  box-sizing: border-box;
  margin: 0 auto;
  padding: 2rem 16px;
  display: flex;
  flex-direction: column;
  gap: 2rem;
}
section {
  display: flex;
  flex-direction: column;
  gap: 0.9rem;
  padding: 1.1rem 1.25rem 1.25rem;
  background: var(--wood);
  border: 2px solid var(--page);
  border-radius: 6px;
}
section .tapered {
  margin: -0.5rem 0 0;
}
.hint {
  margin: 0;
  color: var(--ink-muted);
}
.list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}
.list li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}
.name {
  min-width: 0;
  color: var(--ink);
  font-size: 1.1rem;
  overflow-wrap: anywhere;
}
.list button {
  flex: none;
}
.create {
  display: flex;
  align-items: flex-end;
  gap: 0.75rem;
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
</style>

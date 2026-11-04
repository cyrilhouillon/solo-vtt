<script setup lang="ts">
import type { SearchResult } from '../types'

/**
 * `results` : les pages trouvées, dans l'ordre du serveur ; `null` pendant la première lecture. L'extrait arrive découpé :
 * le client surligne ce que le serveur a trouvé, sans rien comparer lui-même.
 */
defineProps<{ results: readonly SearchResult[] | null; query: string }>()
const emit = defineEmits<{ open: [pageId: string] }>()
</script>

<template>
  <div class="results" role="region" aria-label="Résultats de la recherche" aria-live="polite">
    <p v-if="results === null" class="count">Recherche…</p>
    <template v-else-if="results.length">
      <p class="count">{{ results.length }} {{ results.length > 1 ? 'pages' : 'page' }}</p>
      <ul>
        <li v-for="r in results" :key="r.id">
          <button type="button" class="result" @click="emit('open', r.id)">
            <span class="result-title">{{ r.title }}</span>
            <span v-if="r.excerpt.length" class="excerpt">
              <template v-for="(f, i) in r.excerpt" :key="i">
                <mark v-if="f.match">{{ f.text }}</mark>
                <template v-else>{{ f.text }}</template>
              </template>
            </span>
          </button>
        </li>
      </ul>
    </template>
    <p v-else class="none">Aucune page ne contient « {{ query.trim() }} ».</p>
  </div>
</template>

<style scoped>
.results {
  min-height: 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}
.count,
.none {
  margin: 0;
  color: var(--ink-muted);
}
.none {
  font-style: italic;
}
ul {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}
.result {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
  padding: 0.35rem 0.4rem;
  font: inherit;
  text-align: left;
  color: var(--ink);
  background: none;
  border: 1px solid transparent;
  border-radius: 4px;
  cursor: pointer;
}
.result:hover {
  background: var(--wood-raised);
}
.result:focus-visible {
  outline: 2px solid var(--control-border);
  outline-offset: 1px;
}
/* Même allure qu'un lien de page. */
.result-title {
  text-decoration: underline;
  text-decoration-color: var(--brass);
  text-decoration-thickness: 2px;
  text-underline-offset: 0.18em;
  overflow-wrap: anywhere;
}
.result:hover .result-title {
  text-decoration-color: var(--ink);
}
.excerpt {
  font-size: 0.9rem;
  line-height: 1.4;
  color: var(--ink-muted);
  overflow-wrap: anywhere;
}
mark {
  padding: 0 0.1em;
  color: var(--on-brass);
  background: var(--brass);
  border-radius: 2px;
}
</style>

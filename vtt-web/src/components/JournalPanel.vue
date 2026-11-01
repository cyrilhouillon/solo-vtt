<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import type { JournalEntry } from '../types'

const props = defineProps<{ entries: JournalEntry[] }>()

/**
 * Une action par groupe : l'annonce en titre, l'issue (dernière ligne) juste dessous,
 * et le détail des règles replié entre les deux.
 */
const actions = computed(() => {
  const groups: { action: number; title: string; lines: string[] }[] = []
  for (const entry of props.entries) {
    const last = groups.at(-1)
    if (last && last.action === entry.action) last.lines.push(entry.text)
    else groups.push({ action: entry.action, title: entry.text, lines: [] })
  }
  return groups.map((g) => ({ ...g, outcome: g.lines.at(-1) ?? null, detail: g.lines.slice(0, -1) }))
})

const list = ref<HTMLElement | null>(null)

// La dernière action reste en vue.
watch(
  () => props.entries.length,
  async () => {
    await nextTick()
    list.value?.lastElementChild?.scrollIntoView({ block: 'end' })
  },
)
</script>

<template>
  <p v-if="!actions.length" class="muted">Aucune action. Lancer une attaque depuis l'onglet Feuille.</p>
  <ol v-else ref="list" class="journal" role="log" aria-label="Journal">
    <li v-for="group in actions" :key="group.action">
      <p class="title">{{ group.title }}</p>
      <p v-if="group.outcome" class="outcome">{{ group.outcome }}</p>
      <details v-if="group.detail.length">
        <summary>Détail des règles</summary>
        <ul>
          <li v-for="(line, index) in group.detail" :key="index">{{ line }}</li>
        </ul>
      </details>
      <hr class="tapered" />
    </li>
  </ol>
</template>

<style scoped>
.journal {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  text-wrap: pretty;
}
.title {
  margin: 0;
  font-weight: 700;
}
.outcome {
  margin: 0.1rem 0 0;
  color: var(--rubric);
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}
details {
  margin-top: 0.2rem;
  color: var(--ink-muted);
}
summary {
  cursor: pointer;
  font-family: var(--font-title);
  font-size: 0.9rem;
  letter-spacing: 0.04em;
  width: fit-content;
}
summary:hover {
  color: var(--ink);
}
ul {
  list-style: none;
  margin: 0.25rem 0 0;
  padding: 0;
  font-size: 0.95rem;
  font-variant-numeric: tabular-nums;
}
.muted {
  margin: 0;
  color: var(--ink-muted);
}
</style>

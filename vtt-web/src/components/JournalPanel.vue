<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import type { JournalEntry } from '../types'

const props = defineProps<{ entries: JournalEntry[] }>()

/**
 * Une action par groupe : l'annonce en titre, ce que le moteur a tranché juste dessous (l'issue d'une attaque, une
 * réaction, les dégâts, dans l'ordre), et le détail des règles replié. Une action sans issue marquée (lancer le combat)
 * montre sa dernière ligne. Une entrée qui ouvre un round coupe le groupe : elle s'affiche en séparateur, à sa place.
 */
type Round = { kind: 'round'; key: string; text: string }
type Group = Round | { kind: 'action'; key: string; action: number; title: string; lines: JournalEntry[] }
type Outcome = { text: string; miss: boolean }
type Item = Round | { kind: 'action'; key: string; title: string; outcomes: Outcome[]; detail: string[] }

function outcomeOf(entry: JournalEntry): Outcome {
  return { text: entry.text, miss: entry.roll?.outcome === 'MISS' }
}

const items = computed(() => {
  const groups: Group[] = []
  props.entries.forEach((entry, index) => {
    const last = groups.at(-1)
    if (entry.round) groups.push({ kind: 'round', key: `r${index}`, text: entry.text })
    else if (last?.kind === 'action' && last.action === entry.action) last.lines.push(entry)
    else groups.push({ kind: 'action', key: `a${index}`, action: entry.action, title: entry.text, lines: [] })
  })
  return groups.map<Item>((g) => {
    if (g.kind === 'round') return g
    const marked = g.lines.filter((l) => l.highlight !== null)
    const outcomes = marked.length ? marked : g.lines.slice(-1)
    const detail = g.lines.filter((l) => !outcomes.includes(l)).map((l) => l.text)
    return { kind: 'action', key: g.key, title: g.title, outcomes: outcomes.map(outcomeOf), detail }
  })
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
  <p v-if="!items.length" class="muted">Aucune action. Lancer le combat depuis la barre, ou une attaque depuis l'onglet Feuille.</p>
  <ol v-else ref="list" class="journal" role="log" aria-label="Journal">
    <li v-for="group in items" :key="group.key">
      <p v-if="group.kind === 'round'" class="round">{{ group.text }}</p>
      <template v-else>
      <p class="title">{{ group.title }}</p>
      <p v-for="(outcome, index) in group.outcomes" :key="index" class="outcome" :class="{ miss: outcome.miss }">{{ outcome.text }}</p>
      <details v-if="group.detail.length">
        <summary>Détail des règles</summary>
        <ul>
          <li v-for="(line, index) in group.detail" :key="index">{{ line }}</li>
        </ul>
      </details>
      <hr class="tapered" />
      </template>
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
/* Un nouveau round : un intertitre en petites capitales, comme ceux de la feuille. */
.round {
  margin: 0.4rem 0 0.15rem;
  font-family: var(--font-title);
  font-size: 0.95rem;
  letter-spacing: 0.08em;
  color: var(--ink-muted);
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
/* Un raté se lit comme sur la piste : encre atténuée, maigre. */
.outcome.miss {
  color: var(--ink-muted);
  font-weight: 500;
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

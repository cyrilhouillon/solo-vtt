<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import type { JournalEntry } from '../types'

const props = defineProps<{ entries: JournalEntry[] }>()

/**
 * Une action par groupe : l'annonce en titre, l'issue (dernière ligne) juste dessous,
 * et le détail des règles replié entre les deux. Une entrée qui ouvre un round coupe le groupe :
 * elle s'affiche en séparateur, à sa place.
 */
type Round = { kind: 'round'; key: string; text: string }
type Group = Round | { kind: 'action'; key: string; action: number; title: string; lines: string[] }
type Item = Round | { kind: 'action'; key: string; title: string; outcome: string | null; detail: string[] }

const items = computed(() => {
  const groups: Group[] = []
  props.entries.forEach((entry, index) => {
    const last = groups.at(-1)
    if (entry.round) groups.push({ kind: 'round', key: `r${index}`, text: entry.text })
    else if (last?.kind === 'action' && last.action === entry.action) last.lines.push(entry.text)
    else groups.push({ kind: 'action', key: `a${index}`, action: entry.action, title: entry.text, lines: [] })
  })
  return groups.map<Item>((g) =>
    g.kind === 'round' ? g : { kind: 'action', key: g.key, title: g.title, outcome: g.lines.at(-1) ?? null, detail: g.lines.slice(0, -1) },
  )
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
      <p v-if="group.outcome" class="outcome">{{ group.outcome }}</p>
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

<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import type { JournalEntry } from '../types'

const props = defineProps<{ entries: JournalEntry[] }>()

/** Une action par groupe : sa première ligne (l'annonce) en titre, le détail dessous. */
const actions = computed(() => {
  const groups: { action: number; title: string; lines: string[] }[] = []
  for (const entry of props.entries) {
    const last = groups.at(-1)
    if (last && last.action === entry.action) last.lines.push(entry.text)
    else groups.push({ action: entry.action, title: entry.text, lines: [] })
  }
  return groups
})

const list = ref<HTMLElement | null>(null)

// La dernière action reste en vue.
watch(
  () => props.entries.length,
  async () => {
    await nextTick()
    list.value?.scrollTo({ top: list.value.scrollHeight })
  },
)
</script>

<template>
  <section class="journal" aria-labelledby="journal-title">
    <h2 id="journal-title">Journal</h2>
    <p v-if="!actions.length" class="muted">Aucune action. Lancer une attaque depuis la feuille d'une créature.</p>
    <ol v-else ref="list" role="log">
      <li v-for="group in actions" :key="group.action">
        <p class="title">{{ group.title }}</p>
        <ul>
          <li v-for="(line, index) in group.lines" :key="index">{{ line }}</li>
        </ul>
      </li>
    </ol>
  </section>
</template>

<style scoped>
.journal {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  min-height: 0;
}
h2 {
  font-size: 1.15rem;
  font-weight: 650;
  letter-spacing: -0.01em;
  margin: 0;
}
ol {
  list-style: none;
  margin: 0;
  padding: 0;
  overflow-y: auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}
ul {
  list-style: none;
  margin: 0.15rem 0 0;
  padding: 0 0 0 0.6rem;
  border-left: 2px solid var(--border);
  font-size: 0.85rem;
  color: var(--text-muted);
  font-variant-numeric: tabular-nums;
}
.title {
  margin: 0;
  font-weight: 600;
}
.muted {
  margin: 0;
  font-size: 0.85rem;
  color: var(--text-muted);
}
</style>

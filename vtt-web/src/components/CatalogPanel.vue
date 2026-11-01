<script setup lang="ts">
import { computed } from 'vue'
import type { CatalogEntry, CreatureKind } from '../types'

const props = defineProps<{ entries: CatalogEntry[]; armed: string | null; disabled: boolean }>()
const emit = defineEmits<{ arm: [entryId: string | null] }>()

const groups = computed(() =>
  (
    [
      ['CHARACTER', 'Personnages'],
      ['MONSTER', 'Monstres'],
    ] as [CreatureKind, string][]
  ).map(([kind, title]) => ({ kind, title, entries: props.entries.filter((e) => e.kind === kind) })),
)

function toggle(id: string) {
  emit('arm', props.armed === id ? null : id)
}
</script>

<template>
  <section class="catalog" aria-labelledby="catalog-title">
    <h2 id="catalog-title">Catalogue SRD</h2>
    <p class="hint">
      {{ disabled ? 'Créer une carte pour poser des créatures.' : 'Choisir une créature, puis cliquer sur la carte.' }}
    </p>
    <div v-for="group in groups" :key="group.kind" class="group">
      <h3>{{ group.title }}</h3>
      <ul>
        <li v-for="entry in group.entries" :key="entry.id">
          <button
            type="button"
            class="entry"
            :class="[group.kind.toLowerCase(), { armed: armed === entry.id }]"
            :aria-pressed="armed === entry.id"
            :disabled="disabled"
            @click="toggle(entry.id)"
          >
            <span class="swatch" aria-hidden="true" />
            {{ entry.name }}
          </button>
        </li>
      </ul>
    </div>
    <p class="attribution">Créatures tirées du SRD 5.2.1 (CC-BY-4.0).</p>
  </section>
</template>

<style scoped>
.catalog {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}
h2 {
  font-size: 1rem;
  margin: 0;
}
h3 {
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--text-muted);
  margin: 0 0 0.35rem;
}
ul {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}
.hint,
.attribution {
  margin: 0;
  font-size: 0.8rem;
  color: var(--text-muted);
}
.attribution {
  margin-top: auto;
}
.entry {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  text-align: left;
  background: transparent;
  border-color: transparent;
}
.entry:hover:not(:disabled) {
  background: var(--surface-hover);
}
.entry.armed {
  background: var(--surface-active);
  border-color: var(--accent);
}
.swatch {
  width: 0.7rem;
  height: 0.7rem;
  border-radius: 50%;
  flex: none;
}
.character .swatch {
  background: var(--token-character);
}
.monster .swatch {
  background: var(--token-monster);
}
</style>

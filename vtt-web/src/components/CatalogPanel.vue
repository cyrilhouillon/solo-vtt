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
    <h2 id="catalog-title" class="panel-title">Catalogue SRD</h2>
    <hr class="tapered" />
    <p class="hint">
      {{ disabled ? 'Créer une carte pour poser des créatures.' : 'Choisir une créature, puis cliquer sur une case de la carte.' }}
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
    <p class="attribution">Créatures tirées du <span class="nowrap">SRD 5.2.1 (CC-BY-4.0)</span>.</p>
  </section>
</template>

<style scoped>
.catalog {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  min-height: 100%;
}
.catalog .tapered {
  margin: -0.5rem 0 0;
}
h3 {
  font-family: var(--font-title);
  font-weight: 500;
  font-size: 0.95rem;
  letter-spacing: 0.05em;
  color: var(--ink-muted);
  margin: 0 0 0.3rem;
}
ul {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}
.hint,
.attribution {
  margin: 0;
  font-size: 0.9rem;
  color: var(--ink-muted);
  text-wrap: pretty;
}
.attribution {
  margin-top: auto;
}
.nowrap {
  white-space: nowrap;
}
.entry {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 0.6rem;
  text-align: left;
  background: transparent;
  border-color: transparent;
}
.entry:hover:not(:disabled) {
  background: var(--wood-raised);
  border-color: transparent;
}
/* Créature armée : l'état vivant, en laiton. */
.entry.armed,
.entry.armed:hover:not(:disabled) {
  background: var(--wood-raised);
  border-color: var(--brass);
  font-weight: 700;
}
.swatch {
  width: 0.8rem;
  height: 0.8rem;
  flex: none;
  border: 1.5px solid var(--token-rim);
}
/* Même forme que sur la carte : disque pour un personnage, carré pour un monstre. */
.character .swatch {
  border-radius: 50%;
  background: var(--token-character);
}
.monster .swatch {
  border-radius: 2px;
  background: var(--token-monster);
}
</style>

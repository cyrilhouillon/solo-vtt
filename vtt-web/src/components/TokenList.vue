<script setup lang="ts">
import { KIND_LABELS, type TokenView } from '../types'

/** Les créatures posées : double accessible de la carte, et seul moyen de les retirer au clavier. */
defineProps<{ tokens: TokenView[]; selected: string[]; shown: string | null }>()
const emit = defineEmits<{ select: [tokenId: string]; remove: [tokenId: string] }>()
</script>

<template>
  <section class="tokens" aria-labelledby="tokens-title">
    <h2 id="tokens-title">Sur la carte</h2>
    <p v-if="!tokens.length" class="muted">Aucune créature posée.</p>
    <ul v-else>
      <li v-for="token in tokens" :key="token.id">
        <button
          type="button"
          class="entry"
          :class="[token.kind.toLowerCase(), { selected: selected.includes(token.id) }]"
          :aria-pressed="selected.includes(token.id)"
          @click="emit('select', token.id)"
        >
          <span class="swatch" aria-hidden="true" />
          <span class="name">{{ token.name }}</span>
          <span class="sr-only">, {{ KIND_LABELS[token.kind] }}</span>
          <span v-if="token.id === shown" class="tag">feuille</span>
        </button>
        <button type="button" class="remove" :aria-label="`Retirer ${token.name}`" @click="emit('remove', token.id)">
          Retirer
        </button>
      </li>
    </ul>
    <p v-if="tokens.length" class="muted">Sélectionner deux créatures pour mesurer ; Suppr retire la sélection.</p>
  </section>
</template>

<style scoped>
.tokens {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}
h2 {
  font-size: 1rem;
  margin: 0;
}
ul {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}
li {
  display: flex;
  gap: 0.25rem;
}
.entry {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  text-align: left;
  background: transparent;
  border-color: transparent;
}
.entry:hover {
  background: var(--surface-hover);
}
.entry.selected {
  background: var(--surface-active);
  border-color: var(--token-selected);
}
.name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.tag {
  margin-left: auto;
  font-size: 0.75rem;
  color: var(--text-muted);
}
.remove {
  font-size: 0.8rem;
  color: var(--text-muted);
  background: transparent;
}
.remove:hover {
  color: var(--danger);
  border-color: var(--danger);
}
.muted {
  margin: 0;
  font-size: 0.8rem;
  color: var(--text-muted);
}
.swatch {
  width: 0.7rem;
  height: 0.7rem;
  flex: none;
}
.character .swatch {
  border-radius: 50%;
  background: var(--token-character);
}
.monster .swatch {
  border-radius: 2px;
  background: var(--token-monster);
}
</style>

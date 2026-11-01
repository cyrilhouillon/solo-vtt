<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'
import { KIND_LABELS, type TokenView } from '../types'

/** Les créatures posées : double accessible de la carte, et seul moyen de les retirer au clavier. */
const props = defineProps<{ tokens: TokenView[]; selected: string[]; shown: string | null }>()
const emit = defineEmits<{ select: [tokenId: string]; remove: [tokenId: string] }>()

const title = ref<HTMLHeadingElement | null>(null)
const entries = ref<HTMLButtonElement[]>([])
/** Rang de la ligne retirée : le focus y reste, sur la créature suivante, une fois le retrait fait. */
let focusAfterRemoval: number | null = null

function remove(tokenId: string, index: number) {
  focusAfterRemoval = index
  emit('remove', tokenId)
}

watch(
  () => props.tokens.length,
  async (length, before) => {
    if (focusAfterRemoval === null || length >= before) return
    const index = Math.min(focusAfterRemoval, length - 1)
    focusAfterRemoval = null
    await nextTick()
    ;(index >= 0 ? entries.value[index] : title.value)?.focus()
  },
)
</script>

<template>
  <section class="tokens" aria-labelledby="tokens-title">
    <h2 id="tokens-title" ref="title" tabindex="-1">Sur la carte</h2>
    <p v-if="!tokens.length" class="muted">Aucune créature posée.</p>
    <ul v-else>
      <li v-for="(token, index) in tokens" :key="token.id">
        <button
          ref="entries"
          type="button"
          class="entry"
          :class="[token.kind.toLowerCase(), { selected: selected.includes(token.id) }]"
          :aria-pressed="selected.includes(token.id)"
          @click="emit('select', token.id)"
        >
          <span class="swatch" aria-hidden="true" />
          <span class="name">{{ token.name }}</span>
          <span class="sr-only">, {{ KIND_LABELS[token.kind] }}</span>
          <span v-if="token.id === shown" class="tag"><span aria-hidden="true">feuille</span><span class="sr-only">, feuille affichée</span></span>
        </button>
        <button type="button" class="remove" :aria-label="`Retirer ${token.name}`" @click="remove(token.id, index)">
          Retirer
        </button>
      </li>
    </ul>
    <p v-if="tokens.length" class="muted">
      Deux créatures au plus : une troisième remplace la plus ancienne. Suppr ou Retour arrière retire la sélection.
    </p>
  </section>
</template>

<style scoped>
.tokens {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}
/* Titre de panneau : nettement au-dessus du texte courant (15px). */
h2 {
  font-size: 1.15rem;
  font-weight: 650;
  letter-spacing: -0.01em;
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
/* Sélection : fond et graisse ; le contour reste réservé au focus clavier. */
.entry.selected {
  background: var(--surface-active);
  font-weight: 600;
}
h2:focus {
  outline: none;
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

<script setup lang="ts">
import { ref, useId, watch } from 'vue'
import AppIcon from './AppIcon.vue'

/**
 * Une section qui se replie sous son titre. `storageKey` garde son état dans ce navigateur seulement : une commodité,
 * relue au montage, qui retombe sur `open` si le stockage est absent ou refusé.
 * `nested` : une sous-section, séparée par un filet, sans cadre (jamais de cadre dans un cadre).
 */
const props = withDefaults(defineProps<{ title: string; storageKey?: string; open?: boolean; nested?: boolean }>(), {
  storageKey: undefined,
  open: true,
  nested: false,
})

const key = props.storageKey ? `solovtt.fold.${props.storageKey}` : null

function readStored(): boolean {
  if (!key) return props.open
  try {
    const stored = localStorage.getItem(key)
    return stored === null ? props.open : stored === '1'
  } catch {
    return props.open
  }
}

const expanded = ref(readStored())
watch(expanded, (value) => {
  if (!key) return
  try {
    localStorage.setItem(key, value ? '1' : '0')
  } catch {
    // Stockage refusé : l'état ne survit pas au rechargement, rien de plus.
  }
})

const bodyId = useId()
</script>

<template>
  <section class="fold" :class="{ nested, expanded }">
    <h3 class="fold-head">
      <button type="button" :aria-expanded="expanded" :aria-controls="bodyId" @click="expanded = !expanded">
        <span class="fold-title">{{ title }}</span>
        <slot name="summary" />
        <AppIcon :name="expanded ? 'chevron-down' : 'chevron-right'" />
      </button>
    </h3>
    <div v-show="expanded" :id="bodyId" class="fold-body">
      <slot />
    </div>
  </section>
</template>

<style scoped>
.fold {
  background: var(--wood);
  border-radius: 6px;
}
.fold.nested {
  background: none;
  border-radius: 0;
  border-top: 1px solid var(--rule);
}
.fold-head {
  margin: 0;
  font: inherit;
}
.fold-head button {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 0.6rem;
  padding: 0.75rem 0.9rem 0.75rem 1.1rem;
  border: 0;
  border-radius: 6px;
  background: none;
  text-align: left;
  color: var(--ink);
}
.fold-head button:hover:not(:disabled) {
  background: var(--wood-raised);
}
.fold-title {
  flex: 1;
  min-width: 0;
  font-family: var(--font-title);
  font-weight: 700;
  font-size: 1.15rem;
  letter-spacing: 0.03em;
}
.nested .fold-head button {
  padding: 0.55rem 0.25rem 0.55rem 0;
  border-radius: 4px;
}
.nested .fold-head button:hover:not(:disabled) {
  background: none;
}
.nested .fold-title {
  font-size: 0.95rem;
  letter-spacing: 0.06em;
  color: var(--ink-muted);
}
.nested .fold-head button:hover .fold-title,
.nested.expanded .fold-title {
  color: var(--ink);
}
.fold-head :deep(.icon) {
  flex: none;
  width: 18px;
  height: 18px;
  color: var(--ink-muted);
}
.fold-body {
  padding: 0 1.1rem 1.1rem;
}
.nested .fold-body {
  padding: 0 0 0.9rem;
}
</style>

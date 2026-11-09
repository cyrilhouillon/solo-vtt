<script setup lang="ts">
import AppIcon from './AppIcon.vue'

/**
 * La colonne de la campagne, commune au Carnet et au combat, sur toute la hauteur : où l'on est (la campagne), où l'on
 * va (Accueil, Carnet, Combat), puis ce que l'écran y range (`default`) et son pied (`foot`).
 * `compact` : repliée en rail d'icônes, pour laisser la place à la carte.
 */
defineProps<{ campaignName: string; current: 'notebook' | 'combat'; compact?: boolean }>()
const emit = defineEmits<{ navigate: [target: 'home' | 'notebook' | 'combat'] }>()

const SCREENS = [
  { id: 'notebook', label: 'Carnet', icon: 'notebook', title: 'Carnet de la campagne' },
  { id: 'combat', label: 'Combat', icon: 'combat', title: 'Écran de combat de la campagne' },
] as const
</script>

<template>
  <nav class="campaign-column" :class="{ compact }" aria-label="Campagne">
    <div class="brand">
      <button type="button" class="icon-button ghost" aria-label="Accueil" title="Accueil : changer de campagne" @click="emit('navigate', 'home')">
        <AppIcon name="home" />
      </button>
      <span v-if="!compact" class="app-name">Solo VTT</span>
    </div>
    <h1 class="campaign" :class="{ 'sr-only': compact }" :title="campaignName">{{ campaignName }}</h1>
    <ul class="screens">
      <li v-for="screen in SCREENS" :key="screen.id">
        <span
          v-if="screen.id === current"
          class="screen current"
          aria-current="page"
          :title="compact ? screen.label : undefined"
        >
          <AppIcon :name="screen.icon" /><span :class="{ 'sr-only': compact }">{{ screen.label }}</span>
        </span>
        <button v-else type="button" class="screen" :title="screen.title" @click="emit('navigate', screen.id)">
          <AppIcon :name="screen.icon" /><span :class="{ 'sr-only': compact }">{{ screen.label }}</span>
        </button>
      </li>
    </ul>
    <slot />
    <div class="foot">
      <slot name="foot" />
    </div>
  </nav>
</template>

<style scoped>
.campaign-column {
  position: sticky;
  top: 0;
  height: 100vh;
  display: flex;
  flex-direction: column;
  gap: 0.9rem;
  min-width: 0;
  padding: 0.75rem 0.9rem 0.6rem;
  background: var(--wood);
  border-right: 2px solid var(--page);
}
.brand {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  margin-left: -0.35rem;
}
.app-name {
  font-size: 1rem;
  letter-spacing: 0.06em;
  color: var(--ink-muted);
}
.campaign {
  margin: 0 0 0 0.15rem;
  font-family: var(--font-title);
  font-weight: 700;
  font-size: 1.45rem;
  line-height: 1.15;
  letter-spacing: 0.02em;
  color: var(--ink);
  overflow-wrap: anywhere;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
/* Carnet et Combat : les deux écrans de la campagne, l'un ou l'autre. */
.screens {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.25rem;
  margin: 0;
  padding: 0.2rem;
  list-style: none;
  background: var(--wood-sunken);
  border-radius: 6px;
}
.screen {
  width: 100%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.45rem;
  padding: 0.4rem 0.5rem;
  border: 0;
  border-radius: 4px;
  background: none;
  color: var(--ink-muted);
  font-weight: 500;
}
.screen.current {
  background: var(--wood-raised);
  color: var(--ink);
  font-weight: 700;
}
button.screen:hover:not(:disabled) {
  color: var(--ink);
  background: var(--wood);
}
.screen :deep(.icon) {
  width: 18px;
  height: 18px;
}
.foot {
  display: flex;
  align-items: center;
  gap: 0.15rem;
  margin: auto -0.4rem 0;
  padding-top: 0.5rem;
  border-top: 1px solid var(--rule);
}
.foot > :slotted(:last-child) {
  margin-left: auto;
}
/* Les boutons sans cadre de la colonne : Accueil, pied. */
.ghost,
.foot :slotted(button) {
  border-color: transparent;
  background: none;
  color: var(--ink-muted);
}
.ghost:hover:not(:disabled),
.foot :slotted(button:hover:not(:disabled)) {
  color: var(--ink);
  border-color: transparent;
  background: var(--wood-raised);
}
.foot :slotted(.foot-button) {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.35rem 0.55rem;
  font-size: 0.95rem;
}
.foot :slotted(.foot-button .icon) {
  width: 18px;
  height: 18px;
}

/* Repliée : un rail d'icônes de 3.5rem, écrans l'un sous l'autre. */
.compact {
  align-items: center;
  gap: 0.6rem;
  padding: 0.75rem 0 0.6rem;
}
.compact .brand {
  margin: 0;
}
.compact .screens {
  grid-template-columns: 1fr;
}
.compact .screen {
  width: 2.4rem;
  height: 2.4rem;
  padding: 0;
}
.compact .foot {
  flex-direction: column;
  margin: auto 0 0;
  padding: 0.5rem 0 0;
  align-self: stretch;
  align-items: center;
}
.compact .foot > :slotted(:last-child) {
  margin-left: 0;
}

/* Sous 900px, la colonne se pose au-dessus de l'écran, sans coller. */
@media (max-width: 900px) {
  .campaign-column {
    position: static;
    height: auto;
    border-right: 0;
    border-bottom: 2px solid var(--page);
  }
  .foot {
    margin-top: 0;
  }
  /* Repliée sur un téléphone : une ligne d'icônes. */
  .compact {
    flex-direction: row;
    flex-wrap: wrap;
    padding: 0.4rem 0.75rem;
  }
  .compact .screens {
    grid-template-columns: 1fr 1fr;
  }
  .compact .foot {
    flex-direction: row;
    margin-left: auto;
    padding: 0;
    border-top: 0;
    align-self: center;
  }
}
</style>

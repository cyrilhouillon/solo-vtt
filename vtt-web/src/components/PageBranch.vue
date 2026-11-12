<script setup lang="ts">
import { inject, nextTick, ref, watch } from 'vue'
import AppIcon from './AppIcon.vue'
import { MAX_TITLE_LENGTH, treeContext } from '../notebook/context'
import type { PageNode } from '../notebook/tree'

/** Une branche de l'arbre du Carnet : ses pages, et, dépliées, leurs sous-pages. */
defineProps<{ nodes: PageNode[]; depth: number }>()
const tree = inject(treeContext)!

/** Le champ ouvert prend le focus et sélectionne son texte. */
const field = ref<HTMLInputElement[]>([])
watch(
  () => tree.edit.value,
  async (edit) => {
    if (!edit) return
    await nextTick()
    field.value[0]?.focus()
    field.value[0]?.select()
  },
)

function onKey(e: KeyboardEvent) {
  if (e.key === 'Enter') {
    e.preventDefault()
    tree.submitEdit()
  } else if (e.key === 'Escape') {
    e.preventDefault()
    tree.cancelEdit()
  }
}

/** Pendant un glisser : la page glissée s'estompe, la ligne visée dit si elle l'accepte. */
function dropClass(pageId: string) {
  const drag = tree.drag.value
  if (!drag) return undefined
  return {
    dragged: drag.pageId === pageId,
    'drop-ok': drag.target === pageId && drag.allowed,
    'drop-refused': drag.target === pageId && !drag.allowed,
  }
}

/** Le menu se pose où l'arbre l'a placé, en coordonnées de la fenêtre. */
function menuStyle() {
  const place = tree.menuPlace.value
  if (!place) return undefined
  const px = (v: number | undefined) => (v === undefined ? 'auto' : `${v}px`)
  return { top: px(place.top), bottom: px(place.bottom), right: px(place.right) }
}

const isEditing = (kind: 'rename' | 'child', pageId: string) =>
  tree.edit.value?.kind === kind && tree.edit.value.pageId === pageId
</script>

<template>
  <ul class="branch" :class="{ root: depth === 0 }">
    <li v-for="node in nodes" :key="node.page.id">
      <div class="row" :class="dropClass(node.page.id)" :data-page-id="node.page.id" :style="{ '--depth': depth }">
        <button
          v-if="node.children.length"
          type="button"
          class="toggle"
          :aria-expanded="tree.expanded.value.has(node.page.id)"
          :aria-label="`${tree.expanded.value.has(node.page.id) ? 'Replier' : 'Déplier'} ${node.page.title}`"
          @click="tree.toggle(node.page.id)"
        >
          <AppIcon :name="tree.expanded.value.has(node.page.id) ? 'chevron-down' : 'chevron-right'" />
        </button>
        <span v-else class="toggle-space" />
        <input
          v-if="isEditing('rename', node.page.id)"
          ref="field"
          v-model="tree.draft.value"
          class="title-field"
          type="text"
          :maxlength="MAX_TITLE_LENGTH"
          :aria-label="`Nouveau titre de ${node.page.title}`"
          @keydown="onKey"
          @blur="tree.submitEdit()"
        />
        <!-- Un lien, pour que le clic du milieu ouvre la page dans un nouvel onglet ; tout autre clic l'ouvre ici. Le glisser
             natif d'un lien gênerait celui de l'arbre, en événements pointeur. -->
        <a
          v-else
          class="page-link"
          :href="tree.pageHref(node.page.id)"
          draggable="false"
          :aria-current="node.page.id === tree.currentId.value ? 'page' : undefined"
          @click.prevent="tree.open(node.page.id)"
        >
          {{ node.page.title }}
        </a>
        <button
          type="button"
          class="more"
          :class="{ shown: tree.menuId.value === node.page.id }"
          :aria-label="`Actions de ${node.page.title}`"
          :aria-expanded="tree.menuId.value === node.page.id"
          :disabled="tree.busy.value"
          @click="tree.toggleMenu(node.page.id)"
        >
          <AppIcon name="more" />
        </button>
        <ul v-if="tree.menuId.value === node.page.id" class="menu" :style="menuStyle()">
          <!-- Une entrée, de combat ou de module, n'a pas de sous-page. -->
          <li v-if="!node.page.combatId && !node.page.entryOf">
            <button type="button" @click="tree.startEdit('child', node.page.id)">Nouvelle sous-page</button>
          </li>
          <li><button type="button" @click="tree.startEdit('rename', node.page.id)">Renommer</button></li>
          <!-- Une racine de section appartient à son module : elle ne se déplace ni ne se supprime. -->
          <template v-if="node.page.section === null">
            <li><button type="button" @click="tree.requestMove(node.page.id)">Déplacer sous…</button></li>
            <li><button type="button" class="danger" @click="tree.requestDelete(node.page.id)">Supprimer…</button></li>
          </template>
        </ul>
      </div>
      <div v-if="isEditing('child', node.page.id)" class="row" :style="{ '--depth': depth + 1 }">
        <span class="toggle-space" />
        <input
          ref="field"
          v-model="tree.draft.value"
          class="title-field"
          type="text"
          :maxlength="MAX_TITLE_LENGTH"
          placeholder="Titre"
          :aria-label="`Titre de la nouvelle sous-page de ${node.page.title}`"
          @keydown="onKey"
          @blur="tree.submitEdit()"
        />
      </div>
      <PageBranch v-if="node.children.length && tree.expanded.value.has(node.page.id)" :nodes="node.children" :depth="depth + 1" />
    </li>
  </ul>
</template>

<style scoped>
.branch {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}
.branch:not(.root) {
  margin-top: 0.25rem;
}
.row {
  position: relative;
  display: flex;
  align-items: center;
  gap: 0.15rem;
  padding-left: calc(var(--depth) * 1.1rem);
}
.toggle,
.toggle-space {
  flex: none;
  width: 1.6rem;
  height: 1.6rem;
}
.toggle,
.more {
  display: grid;
  place-items: center;
  padding: 0;
  background: none;
  border-color: transparent;
  color: var(--ink-muted);
}
.toggle :deep(.icon),
.more :deep(.icon) {
  width: 18px;
  height: 18px;
}
.page-link {
  flex: 1;
  min-width: 0;
  padding: 0.35rem 0.8rem;
  border: 1px solid transparent;
  border-radius: 4px;
  font-weight: 500;
  text-decoration: none;
  color: var(--ink-muted);
  overflow-wrap: anywhere;
}
.page-link:hover {
  background: var(--wood-raised);
  border-color: var(--ink-muted);
}
.page-link[aria-current='page'] {
  color: var(--ink);
  background: var(--wood-sunken);
  border-color: var(--rule);
}
.row.dragged .page-link {
  opacity: 0.5;
}
.row.drop-ok .page-link {
  color: var(--ink);
  background: var(--wood-raised);
  border-color: var(--brass);
}
.row.drop-refused .page-link {
  border-color: var(--danger);
  border-style: dashed;
}
.title-field {
  flex: 1;
  min-width: 0;
  box-sizing: border-box;
  padding: 0.3rem 0.5rem;
  border: 1px solid var(--control-border);
  border-radius: 4px;
  background: var(--wood-sunken);
}
.more {
  flex: none;
  width: 2rem;
  height: 2rem;
  opacity: 0;
}
.row:hover .more,
.more:focus-visible,
.more.shown {
  opacity: 1;
}
/* Au doigt, pas de survol : le bouton reste visible. */
@media (hover: none) {
  .more {
    opacity: 1;
  }
}
/* Fixe : l'arbre défile et couperait un menu posé dedans ; `PageTree` le place sous son bouton ou au-dessus. */
.menu {
  position: fixed;
  z-index: 5;
  list-style: none;
  margin: 0;
  padding: 0.3rem;
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
  background: var(--wood-raised);
  border: 1px solid var(--rule);
  border-radius: 4px;
}
.menu .danger {
  color: var(--danger);
}
.menu button {
  width: 100%;
  text-align: left;
  white-space: nowrap;
  background: none;
  border-color: transparent;
}
</style>

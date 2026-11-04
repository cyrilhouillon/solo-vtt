import type { InjectionKey, Ref } from 'vue'

/** Un champ ouvert dans l'arbre : renommer une page, ou titrer une sous-page à créer sous elle. */
export interface TreeEdit {
  kind: 'rename' | 'child'
  pageId: string
}

/** Ce que chaque branche de l'arbre partage avec `PageTree`, qui tient l'état et parle au Carnet. */
export interface TreeContext {
  currentId: Ref<string | null>
  expanded: Ref<ReadonlySet<string>>
  busy: Ref<boolean>
  /** La ligne dont le menu est ouvert. */
  menuId: Ref<string | null>
  edit: Ref<TreeEdit | null>
  draft: Ref<string>
  open: (pageId: string) => void
  toggle: (pageId: string) => void
  toggleMenu: (pageId: string) => void
  startEdit: (kind: TreeEdit['kind'], pageId: string) => void
  submitEdit: () => void
  cancelEdit: () => void
  requestMove: (pageId: string) => void
}

export const treeContext: InjectionKey<TreeContext> = Symbol('treeContext')

/** Même limite que le serveur (`NotebookResource.MAX_TITLE_LENGTH`). */
export const MAX_TITLE_LENGTH = 120

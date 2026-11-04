import type { PageSummary } from '../types'

/** Une page de l'arbre et ses sous-pages, triées. */
export interface PageNode {
  page: PageSummary
  children: PageNode[]
}

/** Ordre des sœurs : par titre, sans casse ni accents, les nombres par valeur (« Session 2 » avant « Session 10 »). */
const collator = new Intl.Collator('fr', { numeric: true, sensitivity: 'base' })

export function byTitle(a: PageSummary, b: PageSummary): number {
  return collator.compare(a.title, b.title) || a.title.localeCompare(b.title) || a.id.localeCompare(b.id)
}

/**
 * L'arbre tiré de la liste plate du serveur. Le serveur rend à la racine toute page dont le parent manque ; par
 * prudence, une telle page y est aussi posée ici, pour qu'aucune ne disparaisse.
 */
export function buildTree(pages: readonly PageSummary[]): PageNode[] {
  const ids = new Set(pages.map((p) => p.id))
  const children = new Map<string | null, PageSummary[]>()
  for (const page of pages) {
    const parent = page.parentId !== null && ids.has(page.parentId) ? page.parentId : null
    children.set(parent, [...(children.get(parent) ?? []), page])
  }
  const nodes = (parent: string | null): PageNode[] =>
    (children.get(parent) ?? [])
      .sort(byTitle)
      .map((page) => ({ page, children: nodes(page.id) }))
  return nodes(null)
}

/** Les ancêtres de la page, du parent à la racine. */
export function ancestors(pages: readonly PageSummary[], id: string): string[] {
  const parents = new Map(pages.map((p) => [p.id, p.parentId]))
  const result: string[] = []
  let current = parents.get(id) ?? null
  while (current !== null && !result.includes(current) && current !== id) {
    result.push(current)
    current = parents.get(current) ?? null
  }
  return result
}

/** La page et toutes ses sous-pages, à toute profondeur. */
export function branch(pages: readonly PageSummary[], id: string): Set<string> {
  const result = new Set([id])
  let grew = true
  while (grew) {
    grew = false
    for (const page of pages) {
      if (page.parentId !== null && result.has(page.parentId) && !result.has(page.id)) {
        result.add(page.id)
        grew = true
      }
    }
  }
  return result
}

/**
 * Vrai si la page peut aller sous `parentId` (`null` : la racine) : ni sous elle-même, ni sous une de ses sous-pages,
 * ni là où elle est déjà.
 */
export function canMove(pages: readonly PageSummary[], id: string, parentId: string | null): boolean {
  const page = pages.find((p) => p.id === id)
  if (!page || page.parentId === parentId) return false
  return parentId === null || !branch(pages, id).has(parentId)
}

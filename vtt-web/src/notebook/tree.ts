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

/** Les racines ordinaires d'abord, puis les racines de section, rangées par section. */
function byRoot(a: PageSummary, b: PageSummary): number {
  if (a.section === b.section) return byTitle(a, b)
  if (a.section === null) return -1
  if (b.section === null) return 1
  return a.section.localeCompare(b.section)
}

/**
 * L'arbre tiré de la liste plate du serveur : les pages ordinaires, puis celles des sections, par section. Le serveur
 * rend à la racine toute page dont le parent manque ; par prudence, une telle page y est aussi posée ici, pour qu'aucune
 * ne disparaisse.
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
      .sort(parent === null ? byRoot : byTitle)
      .map((page) => ({ page, children: nodes(page.id) }))
  return nodes(null)
}

/** Une section de l'arbre : l'id de son module et ses racines. */
export interface TreeSection {
  id: string
  nodes: PageNode[]
}

/** L'arbre coupé en deux : les branches ordinaires, puis une section par module, dans l'ordre de `buildTree`. */
export function splitSections(nodes: readonly PageNode[]): { ordinary: PageNode[]; sections: TreeSection[] } {
  const ordinary = nodes.filter((node) => node.page.section === null)
  const sections: TreeSection[] = []
  for (const node of nodes) {
    const id = node.page.section
    if (id === null) continue
    const last = sections[sections.length - 1]
    if (last?.id === id) last.nodes.push(node)
    else sections.push({ id, nodes: [node] })
  }
  return { ordinary, sections }
}

/** La section du Carnet que tient le cœur pour les entrées de combat, toujours visible, titrée « Combats ». */
export const COMBATS_SECTION = 'combats'

/**
 * Les pages que montrent l'arbre et le dialogue Déplacer : sans celles des sections dont le module n'est pas actif (la
 * section des combats est du cœur, toujours là). Une page est dans une section si sa racine en est une.
 */
export function visiblePages(pages: readonly PageSummary[], activeModules: readonly string[]): PageSummary[] {
  const byId = new Map(pages.map((p) => [p.id, p]))
  const rootOf = (page: PageSummary): PageSummary => {
    const seen = new Set<string>()
    let current = page
    while (current.parentId !== null && byId.has(current.parentId) && !seen.has(current.id)) {
      seen.add(current.id)
      current = byId.get(current.parentId)!
    }
    return current
  }
  return pages.filter((page) => {
    const section = rootOf(page).section
    return section === null || section === COMBATS_SECTION || activeModules.includes(section)
  })
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
 * ni là où elle est déjà. Une racine de section ne bouge pas ; toute autre page entre et sort d'une section.
 */
export function canMove(pages: readonly PageSummary[], id: string, parentId: string | null): boolean {
  const page = pages.find((p) => p.id === id)
  if (!page || page.section !== null || page.parentId === parentId) return false
  // Une entrée, de combat ou de module, n'a pas de sous-page.
  const parent = parentId === null ? undefined : pages.find((p) => p.id === parentId)
  if (parent && (parent.combatId || parent.entryOf)) return false
  return parentId === null || !branch(pages, id).has(parentId)
}

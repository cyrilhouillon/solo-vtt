import type { PageSummary } from '../types'
import { byTitle } from './tree'

/** Ce qu'affiche un lien : son texte choisi, sinon le titre actuel de sa cible ; `null` pour une cible absente sans texte. */
export function linkText(pages: readonly PageSummary[], pageId: string, label: string | null): string | null {
  return label ?? pages.find((p) => p.id === pageId)?.title ?? null
}

/** Sans casse ni accents : « eve » trouve « Ève ». */
function fold(text: string): string {
  return text.normalize('NFD').replace(/\p{Diacritic}/gu, '').toLowerCase()
}

/** Nombre de pages proposées à la fois. */
const MAX_ITEMS = 8

/** Les pages dont le titre contient la requête, dans l'ordre de l'arbre. */
export function matchingPages(pages: readonly PageSummary[], query: string): PageSummary[] {
  const wanted = fold(query.trim())
  return pages.filter((p) => fold(p.title).includes(wanted)).sort(byTitle).slice(0, MAX_ITEMS)
}

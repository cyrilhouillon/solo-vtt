import type { JSONContent } from '@tiptap/core'
import { request } from '../../src/api'

/** Oui ou non, à chances égales, avec son d100 (oui de 1 à 50). */
export interface YesNo {
  question: string | null
  roll: number
  yes: boolean
}

/** Un d100, de 1 à 100. */
export interface D100 {
  question: string | null
  roll: number
}

/** Une liste du joueur : une sous-page de Listes. */
export interface OracleList {
  id: string
  title: string
}

/**
 * Une ligne tirée : le dé (`roll` sur `sides`, le nombre de lignes écrites), son numéro dans la liste, et l'entrée en
 * nœuds de page tels qu'écrits (un lien reste un lien) et en texte.
 */
export interface Draw {
  listId: string
  list: string
  roll: number
  sides: number
  line: number
  entry: JSONContent[]
  entryText: string
}

/** L'oracle générique d'une campagne. */
export function oracleApi(campaignId: string) {
  const base = `/api/campaigns/${encodeURIComponent(campaignId)}/oracle`
  return {
    yesNo: (question: string) => request<YesNo>(`${base}/yes-no`, { method: 'POST', body: JSON.stringify({ question }) }),
    d100: (question: string) => request<D100>(`${base}/d100`, { method: 'POST', body: JSON.stringify({ question }) }),
    /** Assure la page Listes de la section de l'oracle ; idempotent. */
    prepareLists: () => request<{ listsPageId: string }>(`${base}/lists`, { method: 'POST' }),
    lists: () => request<OracleList[]>(`${base}/lists`),
    draw: (listId: string) => request<Draw>(`${base}/lists/${encodeURIComponent(listId)}/draw`, { method: 'POST' }),
  }
}

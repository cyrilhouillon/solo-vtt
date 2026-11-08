import type { JSONContent } from '@tiptap/core'
import { request } from '../../src/api'

/** Une probabilité, d'« Impossible » à « Certain ». */
export interface Odds {
  code: string
  label: string
}

/** Deux mots tirés dans une table de sens, avec leurs d100 ; `table` est son libellé. */
export interface Meaning {
  table: string
  first: string
  firstRoll: number
  second: string
  secondRoll: number
}

/** Une table de sens importée : Actions, Descriptions, ou une table d'éléments nommée. */
export interface MeaningTable {
  kind: 'ACTIONS' | 'DESCRIPTIONS' | 'ELEMENT'
  element: string | null
  label: string
}

export interface MeaningTables {
  imported: boolean
  tables: MeaningTable[]
}

/**
 * Le tirage dans la liste que vise le focus. `empty` : aucune entrée, le focus devient Contexte actuel ; `full` : une
 * entrée au-delà de la 25e ligne, rien n'est tiré. Sinon la ligne et ses dés (`sectionDie` nul avec une seule section
 * active) ; `blank` : ligne vide, « Choisir » ; sinon l'entrée, en nœuds de page tels qu'écrits et en texte.
 */
export interface ListDraw {
  list: 'THREADS' | 'CHARACTERS'
  listLabel: string
  empty: boolean
  full: boolean
  line: number | null
  sectionDie: number | null
  sectionRoll: number | null
  lineRoll: number | null
  blank: boolean
  entry: JSONContent[] | null
  entryText: string | null
}

export interface RandomEvent {
  focus: string
  focusLabel: string
  focusRoll: number
  /** Deux mots d'action, `null` sans tables de sens importées. */
  meaning: Meaning | null
  /** Le focus qui s'applique : Contexte actuel quand la liste visée est vide. */
  effectiveFocus: string
  effectiveFocusLabel: string
  /** `null` quand le focus ne vise aucune liste. */
  list: ListDraw | null
}

/** La réponse de l'oracle, telle que le serveur la tire. */
export interface FateAnswer {
  question: string | null
  roll: number
  odds: Odds
  chaos: number
  answer: string
  answerLabel: string
  randomEvent: RandomEvent | null
}

/** L'oracle Mythic d'une campagne, servi par le module Mythic du serveur. */
export function mythicApi(campaignId: string) {
  const base = `/api/campaigns/${encodeURIComponent(campaignId)}/mythic`
  return {
    odds: () => request<Odds[]>(`${base}/odds`),
    chaos: async () => (await request<{ chaos: number }>(`${base}/chaos`)).chaos,
    setChaos: async (chaos: number) =>
      (await request<{ chaos: number }>(`${base}/chaos`, { method: 'PUT', body: JSON.stringify({ chaos }) })).chaos,
    ask: (question: string, odds: string) =>
      request<FateAnswer>(`${base}/questions`, { method: 'POST', body: JSON.stringify({ question, odds }) }),
    /** Assure les pages Fils et Personnages de la section Mythic du Carnet ; idempotent. */
    prepareLists: () =>
      request<{ threadsPageId: string; charactersPageId: string }>(`${base}/lists`, { method: 'POST' }),
  }
}

/** Les tables de sens du joueur, communes à ses campagnes. */
export const meaningApi = {
  tables: () => request<MeaningTables>('/api/mythic/meaning-tables'),
  importTables: (text: string) =>
    request<MeaningTables>('/api/mythic/meaning-tables', {
      method: 'PUT',
      body: text,
      headers: { 'Content-Type': 'text/plain; charset=utf-8' },
    }),
  draw: (table: MeaningTable) =>
    request<Meaning>('/api/mythic/meanings', {
      method: 'POST',
      body: JSON.stringify({ table: table.kind, element: table.element }),
    }),
}

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

export interface RandomEvent {
  focus: string
  focusLabel: string
  focusRoll: number
  /** Deux mots d'action, `null` sans tables de sens importées. */
  meaning: Meaning | null
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

import { request } from '../../src/api'

/** Une probabilité, d'« Impossible » à « Certain ». */
export interface Odds {
  code: string
  label: string
}

export interface RandomEvent {
  focus: string
  focusLabel: string
  focusRoll: number
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
  }
}

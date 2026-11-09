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

/** L'oracle générique d'une campagne. */
export function oracleApi(campaignId: string) {
  const base = `/api/campaigns/${encodeURIComponent(campaignId)}/oracle`
  return {
    yesNo: (question: string) => request<YesNo>(`${base}/yes-no`, { method: 'POST', body: JSON.stringify({ question }) }),
    d100: (question: string) => request<D100>(`${base}/d100`, { method: 'POST', body: JSON.stringify({ question }) }),
  }
}

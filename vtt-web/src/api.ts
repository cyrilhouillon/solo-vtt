import type { ActionResult, CatalogEntry, Combat, CreatureSheet, Decision, DistanceUnit, JournalEntry, SceneView, TokenView } from './types'

/** Erreur rendue par le serveur, avec son message lisible. */
export class ApiError extends Error {
  readonly status: number

  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

const UNREACHABLE = 'Serveur injoignable : vérifier que vtt-server est lancé.'

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let response: Response
  try {
    response = await fetch(path, {
      ...init,
      headers: init?.body ? { 'Content-Type': 'application/json' } : undefined,
    })
  } catch {
    throw new ApiError(0, UNREACHABLE)
  }
  if (!response.ok) {
    const body = await response.json().catch(() => null)
    // Sans message, un 502-504 vient du proxy de Vite : vtt-server ne répond pas.
    const fallback = response.status >= 502 && response.status <= 504 ? UNREACHABLE : `Le serveur a échoué (erreur ${response.status}). Réessayer ; si l'erreur persiste, voir la console de vtt-server.`
    const message = body?.message ?? fallback
    throw new ApiError(response.status, message)
  }
  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
}

export const api = {
  catalog: () => request<CatalogEntry[]>('/api/catalog'),

  scene: () => request<SceneView>('/api/scene'),

  createScene: (width: number, height: number, unit: DistanceUnit) =>
    request<SceneView>('/api/scene', { method: 'POST', body: JSON.stringify({ width, height, unit }) }),

  placeToken: (catalogId: string, x: number, y: number) =>
    request<TokenView>('/api/scene/tokens', { method: 'POST', body: JSON.stringify({ catalogId, x, y }) }),

  removeToken: (tokenId: string) =>
    request<void>(`/api/scene/tokens/${encodeURIComponent(tokenId)}`, { method: 'DELETE' }),

  sheet: (creatureId: string) => request<CreatureSheet>(`/api/creatures/${encodeURIComponent(creatureId)}`),

  attack: (attackerId: string, attackId: string, targetId: string) =>
    request<ActionResult>('/api/attacks', { method: 'POST', body: JSON.stringify({ attackerId, attackId, targetId }) }),

  journal: () => request<JournalEntry[]>('/api/journal'),

  /** La décision en attente, ou `null` (204). */
  pendingDecision: async () => (await request<Decision | undefined>('/api/decisions/pending')) ?? null,

  answer: (decisionId: string, option: string) =>
    request<ActionResult>(`/api/decisions/${encodeURIComponent(decisionId)}`, { method: 'POST', body: JSON.stringify({ option }) }),

  /** Le combat en cours, ou `null` (204). */
  combat: async () => (await request<Combat | undefined>('/api/combat')) ?? null,

  startCombat: () => request<ActionResult>('/api/combat', { method: 'POST' }),

  endTurn: () => request<ActionResult>('/api/combat/end-turn', { method: 'POST' }),
}

import type { ActionResult, CatalogEntry, Combat, CreatureSheet, Decision, DistanceUnit, JournalEntry, SavedMapView, SceneView, TokenView } from './types'

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
      // JSON par défaut ; une image part avec son propre type.
      headers: init?.body ? { 'Content-Type': 'application/json', ...(init.headers as Record<string, string> | undefined) } : undefined,
    })
  } catch {
    throw new ApiError(0, UNREACHABLE)
  }
  if (!response.ok) {
    const body = await response.json().catch(() => null)
    // Sans message, un 502-504 vient du proxy de Vite : vtt-server ne répond pas.
    // Le seul corps assez lourd pour dépasser la limite du serveur est une image.
    if (response.status === 413 && !body?.message) throw new ApiError(413, 'Image trop lourde (50 Mo au plus).')
    const fallback = response.status >= 502 && response.status <= 504 ? UNREACHABLE : `Le serveur a échoué (erreur ${response.status}). Réessayer ; si l'erreur persiste, voir la console de vtt-server.`
    const message = body?.message ?? fallback
    throw new ApiError(response.status, message)
  }
  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
}

/** L'adresse d'une image de fond gardée par le serveur. */
export const imageUrl = (id: string) => `/api/images/${encodeURIComponent(id)}`

export const api = {
  catalog: () => request<CatalogEntry[]>('/api/catalog'),

  scene: () => request<SceneView>('/api/scene'),

  /** Garde une image de fond sur le serveur ; rend son empreinte. */
  uploadImage: async (file: File) =>
    (await request<{ id: string }>('/api/images', { method: 'POST', body: file, headers: { 'Content-Type': file.type } })).id,

  /** `imageId` : image de fond déjà envoyée, ou `null` pour une carte vide. */
  createScene: (width: number, height: number, unit: DistanceUnit, imageId: string | null = null) =>
    request<SceneView>('/api/scene', { method: 'POST', body: JSON.stringify({ width, height, unit, imageId }) }),

  /** Affiche ou masque la grille de la carte ; c'est de l'affichage, permis en combat. */
  showGrid: (visible: boolean) => request<SceneView>('/api/scene/grid', { method: 'PUT', body: JSON.stringify({ visible }) }),

  /**
   * Cale la grille sur l'image : la carte prend ces dimensions (cases carrées), ses jetons changent d'échelle avec elle,
   * et la grille part du décalage donné, en fraction de case. Refusé en combat, pendant une décision et sans image.
   */
  alignGrid: (width: number, height: number, gridOffsetX: number, gridOffsetY: number) =>
    request<SceneView>('/api/scene/alignment', { method: 'PUT', body: JSON.stringify({ width, height, gridOffsetX, gridOffsetY }) }),

  /** Les cartes enregistrées, triées par nom. */
  savedMaps: () => request<SavedMapView[]>('/api/maps'),

  /** Enregistre la carte en cours sous ce nom ; une carte du même nom est remplacée. */
  saveMap: (name: string) => request<SavedMapView>('/api/maps', { method: 'POST', body: JSON.stringify({ name }) }),

  /** Remplace la carte en cours par la carte enregistrée, comme une nouvelle carte. */
  loadMap: (name: string) => request<SceneView>('/api/maps/load', { method: 'POST', body: JSON.stringify({ name }) }),

  placeToken: (catalogId: string, x: number, y: number) =>
    request<TokenView>('/api/scene/tokens', { method: 'POST', body: JSON.stringify({ catalogId, x, y }) }),

  /** Déplace un jeton ; en combat, celui de la créature active entre au journal. */
  moveToken: (tokenId: string, x: number, y: number) =>
    request<ActionResult>(`/api/scene/tokens/${encodeURIComponent(tokenId)}/move`, { method: 'POST', body: JSON.stringify({ x, y }) }),

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

  /** Termine le combat : plus de tours ; les créatures restent avec leurs PV. */
  endCombat: () => request<ActionResult>('/api/combat/end', { method: 'POST' }),
}

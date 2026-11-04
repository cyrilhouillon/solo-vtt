import type {
  ActionResult,
  CampaignView,
  CatalogEntry,
  Combat,
  CreatureSheet,
  Decision,
  DistanceUnit,
  JournalEntry,
  NotebookView,
  PageDocument,
  PageView,
  SavedMapView,
  SceneView,
  TokenView,
} from './types'

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

/** Ce qui ne dépend d'aucune campagne : les campagnes elles-mêmes, le catalogue, les images. */
export const api = {
  /** Les campagnes, triées par nom. */
  campaigns: () => request<CampaignView[]>('/api/campaigns'),

  createCampaign: (name: string) => request<CampaignView>('/api/campaigns', { method: 'POST', body: JSON.stringify({ name }) }),

  catalog: () => request<CatalogEntry[]>('/api/catalog'),

  /** Garde une image de fond sur le serveur ; rend son empreinte. */
  uploadImage: async (file: File) =>
    (await request<{ id: string }>('/api/images', { method: 'POST', body: file, headers: { 'Content-Type': file.type } })).id,
}

/** Une campagne : son Carnet, sa carte, son combat, son journal et ses cartes enregistrées, plus ce que donne `api`. */
export function campaignApi(campaignId: string) {
  const base = `/api/campaigns/${encodeURIComponent(campaignId)}`
  return {
    ...api,
    /** Les pages du Carnet et la dernière page consultée. */
    notebook: () => request<NotebookView>(`${base}/notebook`),

    createPage: (title: string) => request<PageView>(`${base}/notebook/pages`, { method: 'POST', body: JSON.stringify({ title }) }),

    page: (pageId: string) => request<PageView>(`${base}/notebook/pages/${encodeURIComponent(pageId)}`),

    /** Garde le document de la page ; `keepalive` laisse partir l'envoi quand l'onglet se ferme (corps de 64 Kio au plus). */
    savePageContent: (pageId: string, content: PageDocument, keepalive = false) =>
      request<void>(`${base}/notebook/pages/${encodeURIComponent(pageId)}/content`, {
        method: 'PUT',
        body: JSON.stringify({ content }),
        keepalive,
      }),

    /** Note la page consultée, que le Carnet rouvre à la prochaine ouverture de la campagne. */
    setLastPage: (pageId: string) => request<void>(`${base}/notebook/last-page`, { method: 'PUT', body: JSON.stringify({ pageId }) }),

    scene: () => request<SceneView>(`${base}/scene`),

    /** `imageId` : image de fond déjà envoyée, ou `null` pour une carte vide. */
    createScene: (width: number, height: number, unit: DistanceUnit, imageId: string | null = null) =>
      request<SceneView>(`${base}/scene`, { method: 'POST', body: JSON.stringify({ width, height, unit, imageId }) }),

    /** Affiche ou masque la grille de la carte ; c'est de l'affichage, permis en combat. */
    showGrid: (visible: boolean) => request<SceneView>(`${base}/scene/grid`, { method: 'PUT', body: JSON.stringify({ visible }) }),

    /**
     * Cale la grille sur l'image : la carte prend ces dimensions (cases carrées), ses jetons changent d'échelle avec elle,
     * et la grille part du décalage donné, en fraction de case. Refusé en combat, pendant une décision et sans image.
     */
    alignGrid: (width: number, height: number, gridOffsetX: number, gridOffsetY: number) =>
      request<SceneView>(`${base}/scene/alignment`, { method: 'PUT', body: JSON.stringify({ width, height, gridOffsetX, gridOffsetY }) }),

    /** Les cartes enregistrées, triées par nom. */
    savedMaps: () => request<SavedMapView[]>(`${base}/maps`),

    /** Enregistre la carte en cours sous ce nom ; une carte du même nom est remplacée. */
    saveMap: (name: string) => request<SavedMapView>(`${base}/maps`, { method: 'POST', body: JSON.stringify({ name }) }),

    /** Remplace la carte en cours par la carte enregistrée, comme une nouvelle carte. */
    loadMap: (name: string) => request<SceneView>(`${base}/maps/load`, { method: 'POST', body: JSON.stringify({ name }) }),

    placeToken: (catalogId: string, x: number, y: number) =>
      request<TokenView>(`${base}/scene/tokens`, { method: 'POST', body: JSON.stringify({ catalogId, x, y }) }),

    /** Déplace un jeton ; en combat, celui de la créature active entre au journal. */
    moveToken: (tokenId: string, x: number, y: number) =>
      request<ActionResult>(`${base}/scene/tokens/${encodeURIComponent(tokenId)}/move`, { method: 'POST', body: JSON.stringify({ x, y }) }),

    removeToken: (tokenId: string) =>
      request<void>(`${base}/scene/tokens/${encodeURIComponent(tokenId)}`, { method: 'DELETE' }),

    sheet: (creatureId: string) => request<CreatureSheet>(`${base}/creatures/${encodeURIComponent(creatureId)}`),

    attack: (attackerId: string, attackId: string, targetId: string) =>
      request<ActionResult>(`${base}/attacks`, { method: 'POST', body: JSON.stringify({ attackerId, attackId, targetId }) }),

    journal: () => request<JournalEntry[]>(`${base}/journal`),

    /** La décision en attente, ou `null` (204). */
    pendingDecision: async () => (await request<Decision | undefined>(`${base}/decisions/pending`)) ?? null,

    answer: (decisionId: string, option: string) =>
      request<ActionResult>(`${base}/decisions/${encodeURIComponent(decisionId)}`, { method: 'POST', body: JSON.stringify({ option }) }),

    /** Le combat en cours, ou `null` (204). */
    combat: async () => (await request<Combat | undefined>(`${base}/combat`)) ?? null,

    startCombat: () => request<ActionResult>(`${base}/combat`, { method: 'POST' }),

    endTurn: () => request<ActionResult>(`${base}/combat/end-turn`, { method: 'POST' }),

    /** Termine le combat : plus de tours ; les créatures restent avec leurs PV. */
    endCombat: () => request<ActionResult>(`${base}/combat/end`, { method: 'POST' }),
  }
}

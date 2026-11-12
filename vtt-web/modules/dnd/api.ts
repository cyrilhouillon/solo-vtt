import { request } from '../../src/api'
import type { Sheet, SheetValues } from './sheet'

/** Les fiches des personnages joueurs d'une campagne. */
export function dndApi(campaignId: string) {
  const base = `/api/campaigns/${encodeURIComponent(campaignId)}/dnd`
  return {
    /** Assure la page « Personnages joueurs » de la section du module ; idempotent. */
    prepareSections: () => request<{ playersPageId: string }>(`${base}/sections`, { method: 'POST' }),
    /** Crée un personnage sous ce nom, avec une fiche vide. */
    create: (title: string) => request<Sheet>(`${base}/sheets`, { method: 'POST', body: JSON.stringify({ title }) }),
    read: (pageId: string) => request<Sheet>(`${base}/sheets/${encodeURIComponent(pageId)}`),
    /** Remplace les valeurs saisies ; `keepalive` laisse partir l'envoi quand l'onglet se ferme (corps de 64 Kio au plus). */
    save: (pageId: string, values: SheetValues, keepalive = false) =>
      request<Sheet>(`${base}/sheets/${encodeURIComponent(pageId)}`, { method: 'PUT', body: JSON.stringify(values), keepalive }),
  }
}

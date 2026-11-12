import { request } from '../../src/api'
import type { Build, ClassOptions, Sheet, SheetValues, SpeciesOptions } from './sheet'

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
    /** Les classes de la création guidée et leurs options. */
    classes: () => request<ClassOptions[]>(`${base}/classes`),
    /** Les espèces de la création guidée et leurs sous-choix. */
    species: () => request<SpeciesOptions[]>(`${base}/species`),
    /** Applique une construction guidée ; la réponse porte les valeurs qui en dérivent. */
    build: (pageId: string, build: Build) =>
      request<Sheet>(`${base}/sheets/${encodeURIComponent(pageId)}/build`, { method: 'PUT', body: JSON.stringify(build) }),
    /** Lance les six jets de caractéristique de la fiche, une seule fois. */
    rollAbilities: (pageId: string) =>
      request<Sheet>(`${base}/sheets/${encodeURIComponent(pageId)}/ability-rolls`, { method: 'POST' }),
  }
}

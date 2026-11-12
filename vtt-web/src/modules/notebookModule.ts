import type { JSONContent } from '@tiptap/core'
import type { Component } from 'vue'

/**
 * Ce que le Carnet offre au panneau d'un module : la campagne, et l'écriture de ses résultats. Un module n'écrit que
 * des nœuds standard (paragraphe, citation, liste, texte) : la recherche, les liens entrants et l'enregistrement les
 * lisent sans rien apprendre.
 */
export interface NotebookContext {
  campaignId: string
  /**
   * Écrit le résultat d'un module, sans jamais le refuser : au curseur de la page ouverte déverrouillée, ou à sa fin si
   * elle est verrouillée, puis l'enregistre comme une frappe ; sans page, il s'affiche dans un dialogue et se copie au
   * presse-papiers, prêt à coller dans une page.
   */
  writeResult: (blocks: JSONContent[]) => void
  /** Relit le Carnet puis ouvre la page, après l'enregistrement en cours : pour ouvrir ce que le module vient de créer. */
  openPage: (pageId: string) => Promise<void>
}

/**
 * Un module du Carnet, déclaré par `vtt-web/modules/<id>/index.ts` (export par défaut). Le cœur le découvre à la
 * construction, sans le nommer ; le retirer retire son panneau.
 */
export interface NotebookModule {
  /** Minuscules, chiffres et tirets : le même id que ses données côté serveur. */
  id: string
  title: string
  /** Le panneau, monté à droite de la page avec la prop `context`. */
  panel?: Component
  /**
   * Prépare le Carnet de la campagne (créer les pages de sa section qui manquent), attendu avant sa première lecture à
   * chaque ouverture : idempotent. Un refus va au bandeau d'erreur, sans empêcher d'ouvrir le Carnet. Le titre du module
   * est celui de sa section dans l'arbre.
   */
  prepare?: (campaignId: string) => Promise<void>
  /**
   * La vue d'une entrée du module (page sans texte ni sous-page, `entryOf` : l'id du module), montrée à la place de
   * l'éditeur, sous le titre de la page, avec les props `campaignId` et `pageId` ; elle émet `error` (un message) pour le
   * bandeau du Carnet.
   */
  entryView?: Component
}

import type { JSONContent } from '@tiptap/core'
import type { Component } from 'vue'

/**
 * Ce que le Carnet offre au panneau d'un module : la campagne, et l'écriture dans la page ouverte. Un module n'écrit que
 * des nœuds standard (paragraphe, citation, liste, texte) : la recherche, les liens entrants et l'enregistrement les
 * lisent sans rien apprendre.
 */
export interface NotebookContext {
  campaignId: string
  /** Vrai si une page est ouverte : sans elle, un module ne peut rien écrire. */
  hasOpenPage: () => boolean
  /**
   * Écrit les blocs au curseur de la page ouverte déverrouillée, ou à sa fin si elle est verrouillée, puis l'enregistre
   * comme une frappe. Rend la raison d'un refus, `null` une fois écrit.
   */
  writeToOpenPage: (blocks: JSONContent[]) => string | null
  /**
   * Écrit le résultat d'un module, sans jamais le refuser : dans la page ouverte comme `writeToOpenPage` ; sans page,
   * il s'affiche dans un dialogue et se copie au presse-papiers, prêt à coller dans une page.
   */
  writeResult: (blocks: JSONContent[]) => void
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
}

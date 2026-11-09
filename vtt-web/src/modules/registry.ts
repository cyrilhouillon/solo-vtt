import type { NotebookModule } from './notebookModule'

/**
 * Les modules du Carnet, découverts à la construction dans `vtt-web/modules/<id>/index.ts` : le cœur ne nomme aucun
 * d'eux. Rangés par id, pour que l'ordre ne dépende pas du disque.
 */
const found = import.meta.glob<{ default: NotebookModule }>('../../modules/*/index.ts', { eager: true })

export const notebookModules: readonly NotebookModule[] = Object.values(found)
  .map((module) => module.default)
  .sort((a, b) => a.id.localeCompare(b.id))

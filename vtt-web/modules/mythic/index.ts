import type { NotebookModule } from '../../src/modules/notebookModule'
import MythicPanel from './MythicPanel.vue'
import { mythicApi } from './api'

/** L'oracle de Mythic GME 2e dans le Carnet, et ses listes Fils et Personnages dans sa section de l'arbre. */
const mythic: NotebookModule = {
  id: 'mythic',
  title: 'Mythic',
  panel: MythicPanel,
  prepare: async (campaignId) => {
    await mythicApi(campaignId).prepareLists()
  },
}

export default mythic

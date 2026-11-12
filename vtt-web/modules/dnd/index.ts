import type { NotebookModule } from '../../src/modules/notebookModule'
import CharacterSheetView from './CharacterSheetView.vue'
import DndPanel from './DndPanel.vue'
import { dndApi } from './api'

/**
 * Le module D&D 2024 : les fiches des personnages joueurs, entrées de la section « Personnages joueurs » du Carnet, que
 * le panneau crée et que la vue de fiche montre et saisit.
 */
const dnd: NotebookModule = {
  id: 'dnd',
  title: 'Personnages joueurs',
  panel: DndPanel,
  entryView: CharacterSheetView,
  prepare: async (campaignId) => {
    await dndApi(campaignId).prepareSections()
  },
}

export default dnd

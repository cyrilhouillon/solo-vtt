import type { NotebookModule } from '../../src/modules/notebookModule'
import MythicPanel from './MythicPanel.vue'

/** L'oracle de Mythic GME 2e dans le Carnet. */
const mythic: NotebookModule = { id: 'mythic', title: 'Mythic', panel: MythicPanel }

export default mythic

import type { NotebookModule } from '../../src/modules/notebookModule'
import OraclePanel from './OraclePanel.vue'
import { oracleApi } from './api'

/**
 * Un oracle générique, sans marque : oui/non à chances égales, d100, et une ligne tirée dans les listes du joueur, les
 * sous-pages de « Listes » dans sa section de l'arbre ; chaque jet s'écrit dans la page ouverte.
 */
const oracle: NotebookModule = {
  id: 'oracle',
  title: 'Oracle',
  panel: OraclePanel,
  prepare: async (campaignId) => {
    await oracleApi(campaignId).prepareLists()
  },
}

export default oracle

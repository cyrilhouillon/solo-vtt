import type { NotebookModule } from '../../src/modules/notebookModule'
import OraclePanel from './OraclePanel.vue'

/** Un oracle générique, sans marque : oui/non à chances égales et d100, écrits dans la page ouverte. */
const oracle: NotebookModule = {
  id: 'oracle',
  title: 'Oracle',
  panel: OraclePanel,
}

export default oracle

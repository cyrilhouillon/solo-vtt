import { generateHTML, generateText, type JSONContent } from '@tiptap/core'
import StarterKit from '@tiptap/starter-kit'
import type { PageSummary } from '../types'
import { PageImage } from './pageImage'
import { PageLink } from './pageLink'

/**
 * Copie des blocs d'une page au presse-papiers : en HTML, qu'une page du Carnet recolle tel quel (citation, gras, lien
 * `[[…]]` par `a[data-page-link]`), et en texte, un bloc par ligne, pour tout autre logiciel. Rend la raison d'un échec,
 * `null` une fois copié.
 */
export async function copyBlocks(blocks: JSONContent[], pages: readonly PageSummary[]): Promise<string | null> {
  const doc: JSONContent = { type: 'doc', content: blocks }
  const extensions = [StarterKit.configure({ link: false }), PageLink.configure({ pages: () => pages }), PageImage]
  try {
    const html = generateHTML(doc, extensions)
    // Une citation est un bloc qui en contient d'autres : le séparateur la précéderait d'une ligne vide.
    const text = generateText(doc, extensions, { blockSeparator: '\n' }).trim()
    if (typeof ClipboardItem === 'undefined') {
      await navigator.clipboard.writeText(text)
      return null
    }
    await navigator.clipboard.write([
      new ClipboardItem({
        'text/html': new Blob([html], { type: 'text/html' }),
        'text/plain': new Blob([text], { type: 'text/plain' }),
      }),
    ])
    return null
  } catch (e) {
    return e instanceof Error ? e.message : String(e)
  }
}

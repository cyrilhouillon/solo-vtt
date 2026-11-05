import { Node } from '@tiptap/core'
import { NodeSelection, Selection } from '@tiptap/pm/state'
import { imageUrl } from '../api'

/**
 * Le format d'une image dans le document d'une page : `{ type: 'pageImage', attrs: { imageId, alt } }`. Seule l'empreinte
 * de l'image est gardée, jamais son adresse : l'image vit dans les images du serveur, communes aux cartes et aux pages.
 */
export const PAGE_IMAGE = 'pageImage'

export interface PageImageAttributes {
  imageId: string
  /** Le nom du fichier d'origine, affiché si l'image ne se lit plus. */
  alt: string
}

declare module '@tiptap/core' {
  interface Commands<ReturnType> {
    pageImage: {
      /** Insère l'image à la sélection, ou à `position`, en un seul pas d'annulation. */
      insertPageImage: (attributes: PageImageAttributes, position?: number) => ReturnType
    }
  }
}

/** Une image de la page : un bloc entre deux paragraphes, atomique, sélectionnable au clic ou aux flèches. */
export const PageImage = Node.create({
  name: PAGE_IMAGE,
  group: 'block',
  atom: true,
  selectable: true,
  draggable: false,

  addAttributes() {
    return {
      imageId: {
        default: null,
        parseHTML: (element) => element.getAttribute('data-image-id'),
        renderHTML: (attributes) => ({ 'data-image-id': attributes.imageId, src: imageUrl(attributes.imageId) }),
      },
      alt: {
        default: '',
        parseHTML: (element) => element.getAttribute('alt') ?? '',
        renderHTML: (attributes) => ({ alt: attributes.alt }),
      },
    }
  },

  parseHTML() {
    return [{ tag: 'img[data-image-id]' }]
  },

  renderHTML({ HTMLAttributes }) {
    return ['img', { ...HTMLAttributes, class: 'page-image' }]
  },

  addCommands() {
    return {
      insertPageImage:
        (attributes, position) =>
        ({ tr, commands }) => {
          const image = { type: PAGE_IMAGE, attrs: attributes }
          const inserted = position === undefined ? commands.insertContent(image) : commands.insertContentAt(position, image)
          // ProseMirror sélectionne l'image posée : l'image suivante la remplacerait. Le curseur passe dans le texte qui
          // suit (ou, à défaut, qui précède), jamais sur une autre image.
          if (inserted && tr.selection instanceof NodeSelection) {
            const after = tr.doc.resolve(tr.selection.to)
            const cursor = Selection.findFrom(after, 1, true) ?? Selection.findFrom(after, -1, true)
            if (cursor) tr.setSelection(cursor)
          }
          return inserted
        },
    }
  },
})

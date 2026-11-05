import { Node } from '@tiptap/core'
import { PluginKey } from '@tiptap/pm/state'
import Suggestion from '@tiptap/suggestion'
import { VueNodeViewRenderer, VueRenderer } from '@tiptap/vue-3'
import PageLinkList from '../components/PageLinkList.vue'
import PageLinkView from '../components/PageLinkView.vue'
import type { PageSummary } from '../types'
import { linkText, matchingPages } from './links'

/**
 * Le format d'un lien dans le document d'une page, que liront aussi les liens entrants et la corbeille :
 * `{ type: 'pageLink', attrs: { pageId, label } }`. Le titre de la cible n'est jamais stocké : il est lu à l'affichage.
 */
export const PAGE_LINK = 'pageLink'

export interface PageLinkOptions {
  /**
   * Les pages du Carnet, relues à chaque geste : le lien en tire le titre actuel de sa cible. Une fonction, pas une `Ref` :
   * Tiptap copie les options en profondeur, ce qui perdrait la valeur d'une `Ref` ; lue dans un `computed`, elle suit.
   */
  pages: () => readonly PageSummary[]
  /** Vrai quand la page est verrouillée, lu de même : le lien n'est atteignable au Tab et ne s'ouvre au clic qu'alors. */
  locked: () => boolean
  /** Ouvre la page visée. */
  onOpen: (pageId: string) => void
  /** L'adresse d'une page : le clic du milieu ouvre la cible dans un nouvel onglet du navigateur. */
  pageHref: (pageId: string) => string
}

const suggestionKey = new PluginKey('pageLinkSuggestion')

/**
 * Un lien vers une page du Carnet : nœud en ligne, atomique, qui ne garde que l'id de sa cible et un texte choisi
 * (`label`, `null` pour le titre). Taper `[[` propose les pages par leur titre.
 */
export const PageLink = Node.create<PageLinkOptions>({
  name: PAGE_LINK,
  group: 'inline',
  inline: true,
  atom: true,
  selectable: true,

  addOptions() {
    return { pages: () => [], locked: () => true, onOpen: () => {}, pageHref: () => '' }
  },

  addAttributes() {
    return {
      pageId: {
        default: null,
        parseHTML: (element) => element.getAttribute('data-page-link'),
        renderHTML: (attributes) => ({ 'data-page-link': attributes.pageId }),
      },
      label: {
        default: null,
        parseHTML: (element) => element.getAttribute('data-label'),
        renderHTML: (attributes) => (attributes.label === null ? {} : { 'data-label': attributes.label }),
      },
    }
  },

  parseHTML() {
    return [{ tag: 'a[data-page-link]' }]
  },

  renderHTML({ node, HTMLAttributes }) {
    return ['a', HTMLAttributes, linkText(this.options.pages(), node.attrs.pageId, node.attrs.label)]
  },

  renderText({ node }) {
    return linkText(this.options.pages(), node.attrs.pageId, node.attrs.label)
  },

  addNodeView() {
    return VueNodeViewRenderer(PageLinkView)
  },

  addProseMirrorPlugins() {
    const pages = this.options.pages
    return [
      Suggestion<PageSummary, PageSummary>({
        editor: this.editor,
        pluginKey: suggestionKey,
        char: '[[',
        allowSpaces: true,
        allowedPrefixes: null,
        items: ({ query }) => matchingPages(pages(), query),
        command: ({ editor, range, props }) => {
          // Une seule commande : un seul pas d'annulation rend le « [[… » tapé.
          editor
            .chain()
            .focus()
            .insertContentAt(range, [
              { type: PAGE_LINK, attrs: { pageId: props.id, label: null } },
              { type: 'text', text: ' ' },
            ])
            .run()
        },
        render: () => {
          let list: VueRenderer | null = null
          let unmount: (() => void) | null = null
          return {
            onStart: (props) => {
              list = new VueRenderer(PageLinkList, { props: { ...props, pages: pages() }, editor: props.editor })
              if (list.element instanceof HTMLElement) unmount = props.mount(list.element)
            },
            onUpdate: (props) => list?.updateProps({ ...props, pages: pages() }),
            onKeyDown: ({ event }) => (list?.ref?.onKeyDown(event) as boolean | undefined) ?? false,
            onExit: () => {
              unmount?.()
              list?.destroy()
              unmount = null
              list = null
            },
          }
        },
      }),
    ]
  },
})

import type { JSONContent } from '@tiptap/core'
import type { D100, Draw, YesNo } from './api'

const paragraph = (...parts: JSONContent[]): JSONContent => ({ type: 'paragraph', content: parts })
const text = (value: string): JSONContent => ({ type: 'text', text: value })
const bold = (value: string): JSONContent => ({ type: 'text', text: value, marks: [{ type: 'bold' }] })

/** La réponse en texte, pour le panneau : « Oui (d100 : 37) ». */
export function yesNoText(answer: YesNo): string {
  return `${answer.yes ? 'Oui' : 'Non'} (d100 : ${answer.roll})`
}

/** La question, s'il y en a une, en premier paragraphe de la citation. */
function questionLine(label: string, question: string | null): JSONContent {
  return paragraph(bold(`${label} : `), text(question ?? 'sans énoncé'))
}

/** La réponse oui/non en nœuds standard, une citation que recherche et enregistrement lisent comme du texte. */
export function yesNoBlocks(answer: YesNo): JSONContent[] {
  return [
    {
      type: 'blockquote',
      content: [
        questionLine('Oracle', answer.question),
        paragraph(text(`d100 : ${answer.roll} → `), bold(answer.yes ? 'Oui' : 'Non')),
      ],
    },
  ]
}

/** Le d100 en citation : la question s'il y en a une, puis le jet. */
export function d100Blocks(roll: D100): JSONContent[] {
  const lines = roll.question
    ? [questionLine('d100', roll.question), paragraph(text('d100 : '), bold(String(roll.roll)))]
    : [paragraph(bold('d100 : '), text(String(roll.roll)))]
  return [{ type: 'blockquote', content: lines }]
}

/** La ligne tirée en texte, pour le panneau : « Lieux : Port ». */
export function drawText(draw: Draw): string {
  return `${draw.list} : ${draw.entryText}`
}

/** La ligne tirée en citation : le nom de la liste, l'entrée telle qu'écrite (un lien reste un lien), puis le dé. */
export function drawBlocks(draw: Draw): JSONContent[] {
  return [
    {
      type: 'blockquote',
      content: [paragraph(bold(`${draw.list} : `), ...draw.entry, text(` (d${draw.sides} : ${draw.roll} → ligne ${draw.line})`))],
    },
  ]
}

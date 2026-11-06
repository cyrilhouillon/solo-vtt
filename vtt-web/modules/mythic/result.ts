import type { JSONContent } from '@tiptap/core'
import type { FateAnswer } from './api'

const paragraph = (...parts: JSONContent[]): JSONContent => ({ type: 'paragraph', content: parts })
const text = (value: string): JSONContent => ({ type: 'text', text: value })
const bold = (value: string): JSONContent => ({ type: 'text', text: value, marks: [{ type: 'bold' }] })

/**
 * La réponse de l'oracle en nœuds standard, une citation : la question et ses conditions, le jet et la réponse, puis
 * l'événement aléatoire s'il y en a un. Recherche et enregistrement la lisent comme du texte.
 */
export function answerBlocks(answer: FateAnswer): JSONContent[] {
  const conditions = `${answer.odds.label}, chaos ${answer.chaos}`
  const lines = [
    paragraph(
      bold('Oracle : '),
      text(answer.question ? `${answer.question} (${conditions})` : `question sans énoncé (${conditions})`),
    ),
    paragraph(text(`d100 : ${answer.roll} → `), bold(answer.answerLabel)),
  ]
  if (answer.randomEvent) lines.push(paragraph(bold('Événement aléatoire : '), text(answer.randomEvent.focusLabel)))
  return [{ type: 'blockquote', content: lines }]
}

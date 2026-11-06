import type { JSONContent } from '@tiptap/core'
import type { FateAnswer, Meaning } from './api'

const paragraph = (...parts: JSONContent[]): JSONContent => ({ type: 'paragraph', content: parts })
const text = (value: string): JSONContent => ({ type: 'text', text: value })
const bold = (value: string): JSONContent => ({ type: 'text', text: value, marks: [{ type: 'bold' }] })
const words = (meaning: Meaning) => `${meaning.first} · ${meaning.second}`

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
  const event = answer.randomEvent
  if (event) {
    const meaning = event.meaning ? ` — ${words(event.meaning)}` : ''
    lines.push(paragraph(bold('Événement aléatoire : '), text(event.focusLabel + meaning)))
  }
  return [{ type: 'blockquote', content: lines }]
}

/** Un sens tiré, en citation : la table et ses deux mots, puis leurs d100. */
export function meaningBlocks(meaning: Meaning): JSONContent[] {
  return [
    {
      type: 'blockquote',
      content: [
        paragraph(bold(`Sens (${meaning.table}) : `), text(words(meaning))),
        paragraph(text(`d100 : ${meaning.firstRoll} / ${meaning.secondRoll}`)),
      ],
    },
  ]
}

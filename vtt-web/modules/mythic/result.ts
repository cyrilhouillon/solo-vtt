import type { JSONContent } from '@tiptap/core'
import type { FateAnswer, ListDraw, Meaning, RandomEvent } from './api'

const paragraph = (...parts: JSONContent[]): JSONContent => ({ type: 'paragraph', content: parts })
const text = (value: string): JSONContent => ({ type: 'text', text: value })
const bold = (value: string): JSONContent => ({ type: 'text', text: value, marks: [{ type: 'bold' }] })
const words = (meaning: Meaning) => `${meaning.first} · ${meaning.second}`

/** Le focus tel qu'il s'applique : « PNJ négatif → Contexte actuel (liste Personnages vide) » quand la liste est vide. */
export function focusText(event: RandomEvent): string {
  if (event.list?.empty) return `${event.focusLabel} → ${event.effectiveFocusLabel} (liste ${event.list.listLabel} vide)`
  return event.focusLabel
}

/** Les dés d'un tirage dans une liste : « d4 : 3, d10 : 1 → ligne 6 ». */
function listDice(draw: ListDraw): string {
  const section = draw.sectionDie ? `d${draw.sectionDie} : ${draw.sectionRoll}, ` : ''
  return `${section}d10 : ${draw.lineRoll} → ligne ${draw.line}`
}

/** Le tirage dans une liste en texte, pour le panneau ; `null` sans tirage à montrer (aucune liste, ou liste vide). */
export function listText(draw: ListDraw | null): string | null {
  if (!draw || draw.empty) return null
  if (draw.full) return `${draw.listLabel} : liste pleine, à nettoyer`
  if (draw.blank || !draw.entryText) return `${draw.listLabel} : Choisir (ligne ${draw.line} vide)`
  return `${draw.listLabel} : ${draw.entryText}`
}

/**
 * Le tirage dans une liste, en paragraphe de page : l'entrée telle qu'écrite dans la liste (un lien reste un lien), ou
 * la règle de Mythic pour une ligne vide ou une liste pleine.
 */
function listParagraph(draw: ListDraw): JSONContent {
  const label = bold(`${draw.listLabel} : `)
  if (draw.full)
    return paragraph(label, text("liste pleine (une entrée au-delà de la 25e ligne) : à nettoyer, rien n'est tiré"))
  if (draw.blank || !draw.entry?.length)
    return paragraph(label, bold('Choisir'), text(` — ligne vide : l'entrée la plus logique, ou relancez (${listDice(draw)})`))
  return paragraph(label, ...draw.entry, text(` (${listDice(draw)})`))
}

/**
 * La réponse de l'oracle en nœuds standard, une citation : la question et ses conditions, le jet et la réponse, puis
 * l'événement aléatoire s'il y en a un, avec l'entrée tirée dans sa liste. Recherche et enregistrement la lisent comme du
 * texte.
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
    lines.push(paragraph(bold('Événement aléatoire : '), text(focusText(event) + meaning)))
    if (event.list && !event.list.empty) lines.push(listParagraph(event.list))
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

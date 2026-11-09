import type { TrayResult } from '../components/DiceTray.vue'
import type { JournalEntry, RolledDice } from '../types'

/**
 * Les dés que des entrées du journal ont tirés, en deux lancers comme à la table : le jet d'attaque (avant l'issue, ou
 * tous tant que l'issue n'est pas tranchée), puis les dégâts (après l'issue).
 */
export function throwsOf(entries: readonly JournalEntry[]): { attack: RolledDice[]; damage: RolledDice[] } {
  const dice = (part: readonly JournalEntry[]) => part.flatMap((e) => (e.dice ? [e.dice] : []))
  const at = entries.findIndex((e) => e.highlight === 'OUTCOME')
  return at < 0 ? { attack: dice(entries), damage: [] } : { attack: dice(entries.slice(0, at)), damage: dice(entries.slice(at + 1)) }
}

/**
 * Ce que la piste montre : la dernière attaque tranchée du journal (son annonce, son issue, la réaction et les dégâts).
 * Un tour de combat ne l'efface pas ; la jauge de la cible n'est connue qu'au retour de l'attaque (`target`).
 */
export function trayFromJournal(journal: readonly JournalEntry[], target?: TrayResult['target']): TrayResult | null {
  const outcome = [...journal].reverse().find((e) => e.highlight === 'OUTCOME' && e.roll)
  if (!outcome?.roll) return null
  const entries = journal.filter((e) => e.action === outcome.action)
  const thrown = throwsOf(entries)
  return {
    action: outcome.action,
    title: entries[0]?.text ?? '',
    roll: outcome.roll,
    damage: entries.flatMap((e) => (e.damage ? [e.damage] : [])),
    reactions: entries.filter((e) => e.highlight === 'REACTION').map((e) => e.text),
    attackDice: thrown.attack,
    damageDice: thrown.damage,
    // Seule une attaque qui vient d'être tranchée (elle a sa cible) fait rouler les dés ; relue du journal, elle est posée.
    live: target !== undefined,
    target,
  }
}

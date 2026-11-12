/**
 * La fiche d'un personnage D&D 2024 côté client : ses valeurs saisies, telles que le serveur les garde, et les formules du
 * SRD 5.2.1 qui en tirent les dérivés, pour les montrer à chaque frappe. Le serveur fait foi à l'enregistrement.
 */

export const ABILITIES = ['STRENGTH', 'DEXTERITY', 'CONSTITUTION', 'INTELLIGENCE', 'WISDOM', 'CHARISMA'] as const
export type Ability = (typeof ABILITIES)[number]

export const ABILITY_LABELS: Record<Ability, { short: string; label: string }> = {
  STRENGTH: { short: 'FOR', label: 'Force' },
  DEXTERITY: { short: 'DEX', label: 'Dextérité' },
  CONSTITUTION: { short: 'CON', label: 'Constitution' },
  INTELLIGENCE: { short: 'INT', label: 'Intelligence' },
  WISDOM: { short: 'SAG', label: 'Sagesse' },
  CHARISMA: { short: 'CHA', label: 'Charisme' },
}

/** Les dix-huit compétences, dans l'ordre alphabétique français, et leur caractéristique. */
export const SKILLS = [
  { id: 'ACROBATICS', label: 'Acrobaties', ability: 'DEXTERITY' },
  { id: 'ARCANA', label: 'Arcanes', ability: 'INTELLIGENCE' },
  { id: 'ATHLETICS', label: 'Athlétisme', ability: 'STRENGTH' },
  { id: 'STEALTH', label: 'Discrétion', ability: 'DEXTERITY' },
  { id: 'ANIMAL_HANDLING', label: 'Dressage', ability: 'WISDOM' },
  { id: 'SLEIGHT_OF_HAND', label: 'Escamotage', ability: 'DEXTERITY' },
  { id: 'HISTORY', label: 'Histoire', ability: 'INTELLIGENCE' },
  { id: 'INTIMIDATION', label: 'Intimidation', ability: 'CHARISMA' },
  { id: 'INSIGHT', label: 'Intuition', ability: 'WISDOM' },
  { id: 'INVESTIGATION', label: 'Investigation', ability: 'INTELLIGENCE' },
  { id: 'MEDICINE', label: 'Médecine', ability: 'WISDOM' },
  { id: 'NATURE', label: 'Nature', ability: 'INTELLIGENCE' },
  { id: 'PERCEPTION', label: 'Perception', ability: 'WISDOM' },
  { id: 'PERSUASION', label: 'Persuasion', ability: 'CHARISMA' },
  { id: 'RELIGION', label: 'Religion', ability: 'INTELLIGENCE' },
  { id: 'PERFORMANCE', label: 'Représentation', ability: 'CHARISMA' },
  { id: 'SURVIVAL', label: 'Survie', ability: 'WISDOM' },
  { id: 'DECEPTION', label: 'Tromperie', ability: 'CHARISMA' },
] as const satisfies readonly { id: string; label: string; ability: Ability }[]
export type Skill = (typeof SKILLS)[number]['id']

/** Aucune, maîtrise (+ bonus de maîtrise) ou expertise (deux fois). */
export type Proficiency = 'NONE' | 'PROFICIENT' | 'EXPERTISE'

export interface SheetAttack {
  name: string
  bonus: number
  damage: string
  damageType: string
  range: string
}

export interface SpellSlots {
  level: number
  max: number
  expended: number
}

/** Les valeurs saisies ; `skills` ne garde que les compétences maîtrisées, `speed` est en mètres. */
export interface SheetValues {
  className: string
  level: number
  species: string
  background: string
  abilities: Record<Ability, number>
  proficiencyBonus: number
  savingThrows: Ability[]
  skills: Partial<Record<Skill, Proficiency>>
  armorClass: number
  hitPointMax: number
  hitPoints: number
  hitDice: string
  speed: number
  initiative: number
  attacks: SheetAttack[]
  features: string
  equipment: string
  proficiencies: string
  languages: string
  spellSlots: SpellSlots[]
}

/** Une fiche lue au serveur : la page qui la montre et ses valeurs (les dérivés, recalculés ici, sont ignorés). */
export interface Sheet {
  pageId: string
  values: SheetValues
}

/** Une valeur numérique saisie : un champ vidé vaut 0, que le serveur refusera s'il est hors bornes. */
export function numberOf(value: unknown): number {
  return typeof value === 'number' && Number.isFinite(value) ? value : 0
}

/** Le modificateur d'une valeur : (valeur − 10) / 2, arrondi à l'inférieur. */
export function modifier(score: number): number {
  return Math.floor((numberOf(score) - 10) / 2)
}

export function savingThrow(values: SheetValues, ability: Ability): number {
  return modifier(values.abilities[ability]) + (values.savingThrows.includes(ability) ? numberOf(values.proficiencyBonus) : 0)
}

const MULTIPLIER: Record<Proficiency, number> = { NONE: 0, PROFICIENT: 1, EXPERTISE: 2 }

export function skillBonus(values: SheetValues, skill: Skill): number {
  const ability = SKILLS.find((s) => s.id === skill)!.ability
  return modifier(values.abilities[ability]) + MULTIPLIER[values.skills[skill] ?? 'NONE'] * numberOf(values.proficiencyBonus)
}

export function passivePerception(values: SheetValues): number {
  return 10 + skillBonus(values, 'PERCEPTION')
}

/** Un bonus signé : « +2 », « −1 », « +0 ». */
export function signed(value: number): string {
  return value < 0 ? `−${-value}` : `+${value}`
}

/** Les valeurs telles qu'elles partent au serveur : nombres assurés, compétences non maîtrisées et emplacements vides omis. */
export function toRequest(values: SheetValues): SheetValues {
  const abilities = Object.fromEntries(ABILITIES.map((a) => [a, numberOf(values.abilities[a])])) as Record<Ability, number>
  const skills = Object.fromEntries(Object.entries(values.skills).filter(([, p]) => p && p !== 'NONE')) as SheetValues['skills']
  return {
    ...values,
    level: numberOf(values.level),
    abilities,
    proficiencyBonus: numberOf(values.proficiencyBonus),
    skills,
    armorClass: numberOf(values.armorClass),
    hitPointMax: numberOf(values.hitPointMax),
    hitPoints: numberOf(values.hitPoints),
    speed: numberOf(values.speed),
    initiative: numberOf(values.initiative),
    attacks: values.attacks.map((a) => ({ ...a, bonus: numberOf(a.bonus) })),
    spellSlots: values.spellSlots
      .map((s) => ({ level: s.level, max: numberOf(s.max), expended: numberOf(s.expended) }))
      .filter((s) => s.max > 0 || s.expended > 0),
  }
}

/** Les emplacements des neuf niveaux de sort, à montrer : ceux de la fiche, les autres à zéro. */
export function allSlots(slots: readonly SpellSlots[]): SpellSlots[] {
  return Array.from({ length: 9 }, (_, i) => slots.find((s) => s.level === i + 1) ?? { level: i + 1, max: 0, expended: 0 })
}

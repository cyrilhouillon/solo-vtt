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

/**
 * Les 13 types de dégâts du SRD 5.2.1, tels que le serveur les nomme, et leur libellé après « dégâts » ; rangés par ordre
 * alphabétique du nom français (acide, contondants, feu…).
 */
export const DAMAGE_TYPES = [
  { id: 'ACID', label: 'd’acide' },
  { id: 'BLUDGEONING', label: 'contondants' },
  { id: 'FIRE', label: 'de feu' },
  { id: 'FORCE', label: 'de force' },
  { id: 'LIGHTNING', label: 'de foudre' },
  { id: 'COLD', label: 'de froid' },
  { id: 'NECROTIC', label: 'nécrotiques' },
  { id: 'PIERCING', label: 'perforants' },
  { id: 'POISON', label: 'de poison' },
  { id: 'PSYCHIC', label: 'psychiques' },
  { id: 'RADIANT', label: 'radiants' },
  { id: 'THUNDER', label: 'de tonnerre' },
  { id: 'SLASHING', label: 'tranchants' },
] as const
export type DamageType = (typeof DAMAGE_TYPES)[number]['id']

/** Les dés de dégâts qu'accepte le serveur. */
export const DIE_SIDES = [4, 6, 8, 10, 12, 20] as const

/**
 * Une attaque de la fiche : bonus d'attaque, dés et bonus de dégâts (0 dé : dégâts fixes), type, allonge et portée en
 * mètres (`null` : sans ; une attaque qui a les deux se lance).
 */
export interface SheetAttack {
  name: string
  bonus: number
  diceCount: number
  dieSides: number
  damageBonus: number
  damageType: DamageType
  reach: number | null
  normalRange: number | null
  longRange: number | null
}

/** Une attaque neuve : 1d6 contondants, au contact. */
export function newAttack(): SheetAttack {
  return { name: '', bonus: 0, diceCount: 1, dieSides: 6, damageBonus: 0, damageType: 'BLUDGEONING', reach: 1.5, normalRange: null, longRange: null }
}

/** « 1d8+3 tranchants · 1,5 m ; lancer 6/18 m », tiré des champs. */
export function attackSummary(attack: SheetAttack): string {
  const count = numberOf(attack.diceCount)
  const bonus = numberOf(attack.damageBonus)
  const extra = bonus === 0 ? '' : bonus > 0 ? `+${bonus}` : String(bonus)
  const damage = count === 0 ? String(bonus) : `${count}d${attack.dieSides}${extra}`
  const type = DAMAGE_TYPES.find((t) => t.id === attack.damageType)?.label ?? ''
  const reach = optionalNumber(attack.reach)
  const normal = optionalNumber(attack.normalRange)
  const long = optionalNumber(attack.longRange)
  const range = normal === null || long === null ? null : `${metres(normal)}/${metres(long)} m`
  const where = reach === null ? (range ?? '') : range === null ? `${metres(reach)} m` : `${metres(reach)} m ; lancer ${range}`
  return [`${damage} ${type}`.trim(), where].filter(Boolean).join(' · ')
}

function metres(value: number): string {
  return String(value).replace('.', ',')
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
  /** La taille en texte (« M (moyenne) »). */
  size: string
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
  speciesTraits: string
  feats: string
  equipment: string
  proficiencies: string
  languages: string
  spellSlots: SpellSlots[]
}

/** Les méthodes de valeurs de caractéristique : valeurs standard, achat de points, dés. */
export type AbilityMethod = 'STANDARD_ARRAY' | 'POINT_BUY' | 'ROLLED'

/** Les choix de la création guidée, par leurs ids ; `masteries` par id d'arme. */
export interface Build {
  characterClass: string
  method: AbilityMethod
  scores: Record<Ability, number>
  skills: Skill[]
  fightingStyle: string
  equipment: string
  masteries: string[]
  /** L'espèce et ses sous-choix, `null` tant qu'aucune n'est choisie. */
  species: SpeciesChoice | null
  /** L'historique et ses choix, `null` tant qu'aucun n'est choisi. */
  background: BackgroundChoice | null
}

/** L'espèce d'une construction et ses sous-choix, par leurs ids ; `null` : à choisir. */
export interface SpeciesChoice {
  species: string
  size: string | null
  option: string | null
  skill: Skill | null
  spellcastingAbility: Ability | null
  /** Le don de Polyvalent (Humain), `null` tant qu'il n'est pas choisi. */
  originFeat: OriginFeatChoice | null
}

/** Un don d'origines et ses sous-choix, par leurs ids ; `null` : à choisir ; `skills` : celles de Doué. */
export interface OriginFeatChoice {
  feat: string
  spellList: string | null
  spellcastingAbility: Ability | null
  skills: Skill[]
}

/** L'historique d'une construction et ses choix, par leurs ids ; `increases` : +2/+1 ou +1/+1/+1. */
export interface BackgroundChoice {
  background: string
  increases: Partial<Record<Ability, number>>
  equipment: string
  gamingSet: string | null
  spellcastingAbility: Ability | null
}

/** Un historique pour la création guidée ; `tool` `null` : une boîte de jeux au choix parmi `gamingSets`. */
export interface BackgroundOptions {
  id: string
  label: string
  abilities: { id: Ability; label: string }[]
  feat: DescribedChoice
  /** La liste de sorts que fixe Initié à la magie, `null` pour un autre don. */
  spellList: Choice | null
  skills: { id: Skill; label: string }[]
  tool: string | null
  gamingSets: Choice[]
  equipments: DescribedChoice[]
}

/** Un don d'origines pour la création guidée ; listes et incantations vides sauf Initié, `skillCount` 0 sauf Doué. */
export interface OriginFeatOptions {
  id: string
  label: string
  text: string
  spellLists: Choice[]
  spellcastingAbilities: { id: Ability; label: string }[]
  skillCount: number
}

/** Une espèce pour la création guidée, telle que le serveur la sert ; `optionLabel` et `skillTrait` `null` sans. */
export interface SpeciesOptions {
  id: string
  label: string
  sizes: Choice[]
  speed: number
  optionLabel: string | null
  options: DescribedChoice[]
  skillTrait: string | null
  skillChoices: { id: Skill; label: string }[]
  /** Les caractéristiques d'incantation au choix, vide si l'espèce n'en choisit pas. */
  spellcastingAbilities: { id: Ability; label: string }[]
  /** L'espèce choisit-elle un don d'origines (Polyvalent de l'Humain). */
  choosesOriginFeat: boolean
  traits: string[]
}

/** Un jet de caractéristique : ses quatre d6 dans l'ordre du tirage, et la somme des trois plus hauts. */
export interface AbilityRoll {
  dice: number[]
  total: number
}

export interface Choice {
  id: string
  label: string
}

export interface DescribedChoice extends Choice {
  text: string
}

export interface WeaponChoice extends Choice {
  mastery: string
}

/** Les options d'une classe pour la création guidée, telles que le serveur les sert ; `pointBuyCosts` par valeur. */
export interface ClassOptions {
  id: string
  label: string
  skillChoices: { id: Skill; label: string }[]
  skillCount: number
  fightingStyles: DescribedChoice[]
  equipments: DescribedChoice[]
  weapons: WeaponChoice[]
  masteryCount: number
  suggestedScores: Record<Ability, number>
  standardScores: number[]
  pointBuyCosts: Record<string, number>
  pointBuyBudget: number
  /** La construction d'où part la classe quand le joueur la choisit. */
  defaultBuild: Build
}

/**
 * Une fiche lue au serveur : la page qui la montre, ses valeurs (les dérivés, recalculés ici, sont ignorés), sa
 * construction guidée et ses jets de caractéristique, `null` s'il n'y en a pas.
 */
export interface Sheet {
  pageId: string
  values: SheetValues
  build: Build | null
  rolls: AbilityRoll[] | null
}

/** Les points dépensés à l'achat de points, `null` si une valeur n'a pas de coût (hors de 8 à 15). */
export function pointBuyCost(scores: Record<Ability, number>, costs: Record<string, number>): number | null {
  let total = 0
  for (const ability of ABILITIES) {
    const cost = costs[String(scores[ability])]
    if (cost === undefined) return null
    total += cost
  }
  return total
}

/** Une valeur numérique saisie : un champ vidé vaut 0, que le serveur refusera s'il est hors bornes. */
export function numberOf(value: unknown): number {
  return typeof value === 'number' && Number.isFinite(value) ? value : 0
}

/** Une valeur numérique facultative : un champ vidé vaut `null` (« sans »). */
export function optionalNumber(value: unknown): number | null {
  return typeof value === 'number' && Number.isFinite(value) ? value : null
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

/**
 * Les valeurs telles qu'elles partent au serveur : nombres assurés ; compétences non maîtrisées, emplacements vides et
 * attaques pas encore nommées (que le serveur refuserait) omis.
 */
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
    attacks: values.attacks
      .filter((a) => a.name.trim())
      .map((a) => ({
        ...a,
        bonus: numberOf(a.bonus),
        diceCount: numberOf(a.diceCount),
        damageBonus: numberOf(a.damageBonus),
        reach: optionalNumber(a.reach),
        normalRange: optionalNumber(a.normalRange),
        longRange: optionalNumber(a.longRange),
      })),
    spellSlots: values.spellSlots
      .map((s) => ({ level: s.level, max: numberOf(s.max), expended: numberOf(s.expended) }))
      .filter((s) => s.max > 0 || s.expended > 0),
  }
}

/** Les emplacements des neuf niveaux de sort, à montrer : ceux de la fiche, les autres à zéro. */
export function allSlots(slots: readonly SpellSlots[]): SpellSlots[] {
  return Array.from({ length: 9 }, (_, i) => slots.find((s) => s.level === i + 1) ?? { level: i + 1, max: 0, expended: 0 })
}

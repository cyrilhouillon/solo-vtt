// Formes JSON de vtt-server (solovtt.server.Dtos).

export type DistanceUnit = 'METRE' | 'FOOT'
export type CreatureKind = 'CHARACTER' | 'MONSTER'

export interface CatalogEntry {
  id: string
  name: string
  kind: CreatureKind
}

export interface TokenView {
  id: string
  creatureId: string
  /** Entrée du catalogue d'origine : de quoi reposer la créature après un retrait. */
  catalogId: string
  name: string
  kind: CreatureKind
  x: number
  y: number
  /** Côté client seulement : affiché dès le clic, en attendant la réponse du serveur. */
  pending?: boolean
}

export interface SceneView {
  width: number
  height: number
  unit: DistanceUnit
  unitSymbol: string
  tokens: TokenView[]
}

export interface Measure {
  value: number
  unitSymbol: string
}

/** Une attaque de la feuille : allonge au corps à corps, portée normale et longue à distance. */
export interface Attack {
  id: string
  label: string
  mode: 'MELEE' | 'RANGED'
  attackBonus: number
  damage: string
  damageType: string
  reach: Measure | null
  range: { normal: number; longRange: number; unitSymbol: string } | null
}

export interface JournalEntry {
  /** Numéro de l'action qui a produit l'entrée : les entrées d'une même attaque le partagent. */
  action: number
  text: string
}

export interface AttackResult {
  entries: JournalEntry[]
  targetId: string
  targetHitPoints: number
  targetMaxHitPoints: number
}

export interface CreatureSheet {
  id: string
  name: string
  kind: CreatureKind
  armorClass: number
  hitPoints: number
  maxHitPoints: number
  speed: Measure
  attacks: Attack[]
  resistances: string[]
  vulnerabilities: string[]
}

export const KIND_LABELS: Record<CreatureKind, string> = {
  CHARACTER: 'Personnage',
  MONSTER: 'Monstre',
}

const numberFormat = new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 1 })

export function formatLength(value: number, unitSymbol: string): string {
  return `${numberFormat.format(value)} ${unitSymbol}`
}

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
  name: string
  kind: CreatureKind
  x: number
  y: number
}

export interface SceneView {
  width: number
  height: number
  unit: DistanceUnit
  unitSymbol: string
  tokens: TokenView[]
}

export interface CreatureSheet {
  id: string
  name: string
  kind: CreatureKind
  armorClass: number
  hitPoints: number
  maxHitPoints: number
  speed: { value: number; unitSymbol: string }
  weapon: { name: string; attackBonus: number; damage: string; damageType: string } | null
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

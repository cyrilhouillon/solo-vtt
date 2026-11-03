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
  /** CA et PV de la créature ; absents tant qu'un jeton attend la réponse du serveur. */
  armorClass?: number
  hitPoints?: number
  maxHitPoints?: number
  /** Côté client seulement : affiché dès le clic, en attendant la réponse du serveur. */
  pending?: boolean
}

export interface SceneView {
  width: number
  height: number
  unit: DistanceUnit
  unitSymbol: string
  /** L'image de fond (`imageUrl`), ou `null` pour une carte sans image. */
  imageId: string | null
  /** La grille s'affiche ; sans effet sur les distances ni le calage. */
  gridVisible: boolean
  /** Décalage de la grille depuis le coin haut gauche de la carte, en fraction de case, dans [0, 1). */
  gridOffsetX: number
  gridOffsetY: number
  /** La carte enregistrée d'où vient la carte, à titre d'information ; `null` pour une nouvelle carte. */
  mapName: string | null
  tokens: TokenView[]
}

/** Une carte enregistrée, prête à charger. */
export interface SavedMapView {
  name: string
  width: number
  height: number
  unitSymbol: string
  imageId: string | null
  creatureCount: number
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
  /** Corps à corps : distance de centre à centre jusqu'où les règles acceptent la cible (allonge + tolérance de contact). */
  reachLimit: Measure | null
  range: { normal: number; longRange: number; unitSymbol: string } | null
}

export interface JournalEntry {
  /** Numéro de l'action qui a produit l'entrée : les entrées d'une même attaque le partagent. */
  action: number
  text: string
  /** L'entrée ouvre un round : le journal la montre en séparateur. */
  round: boolean
  /** Ce que la piste à dés retient d'une attaque : son issue, les dégâts, une réaction ; `null` sinon. */
  highlight: 'OUTCOME' | 'DAMAGE' | 'REACTION' | null
  /** Les faits de l'issue d'une attaque (entrée `OUTCOME`), `null` sinon. */
  roll: AttackRoll | null
  /** Les dégâts subis (entrée `DAMAGE`), `null` sinon. */
  damage: Damage | null
  /** Les dés d'un jet (d20, dés bonus, dés de dégâts), `null` sinon. */
  dice: RolledDice | null
}

/**
 * Des dés tirés par le serveur, dans l'ordre du tirage. `kept` est la valeur gardée d'un d20 (avantage : le meilleur,
 * désavantage : le pire), `null` pour les autres dés.
 */
export interface RolledDice {
  sides: number
  values: number[]
  kept: number | null
}

/** L'issue d'une attaque et le calcul qui la tranche. */
export interface AttackRoll {
  outcome: 'HIT' | 'MISS' | 'CRITICAL'
  total: number
  armorClass: number
}

/** Des dégâts subis ; `type` est le libellé français (« tranchants »). */
export interface Damage {
  amount: number
  type: string
}

/** Une question du moteur au milieu d'une action ; le joueur y répond pour toute créature. */
export interface Decision {
  id: string
  /** La créature qui choisit (réaction), ou `null` quand la question ne revient à personne (« Qui joue ? »). */
  chooserId: string | null
  chooserName: string | null
  prompt: string
  options: string[]
}

/** Un membre d'un rang d'initiative ; `played` s'il a déjà joué ce round. */
export interface TurnMember {
  creatureId: string
  name: string
  played: boolean
}

/** Les créatures qui partagent un rang d'initiative. */
export interface InitiativeGroup {
  initiative: number
  members: TurnMember[]
}

/** Le combat en cours : round, créature qui joue (`null` tant que « Qui joue ? » attend), ordre de jeu. */
export interface Combat {
  round: number
  activeId: string | null
  candidates: string[]
  groups: InitiativeGroup[]
  /**
   * Ce qui reste à parcourir à la créature active, dans l'unité de la carte ; `null` veut dire illimité
   * (aucune créature active, ou système qui ne limite pas le déplacement). Le client n'y ajoute aucune règle.
   */
  movementLeft: number | null
}

/** Ce qu'une action (attaque, combat) ou sa reprise a produit. */
export interface ActionResult {
  entries: JournalEntry[]
  /** La cible et ses PV après coup ; `null` pour une action sans cible (combat). */
  targetId: string | null
  targetHitPoints: number | null
  targetMaxHitPoints: number | null
  /** La décision qui suspend l'action ; `null` quand elle est finie. */
  decision: Decision | null
  /** Le combat après l'action ; `null` hors combat. */
  combat: Combat | null
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
  /** Pourquoi la créature ne peut pas attaquer en ce moment (à terre, pas son tour) ; les règles en décident. */
  attackRefusal: string | null
}

export const KIND_LABELS: Record<CreatureKind, string> = {
  CHARACTER: 'Personnage',
  MONSTER: 'Monstre',
}

const numberFormat = new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 1 })

export function formatLength(value: number, unitSymbol: string): string {
  return `${numberFormat.format(value)} ${unitSymbol}`
}

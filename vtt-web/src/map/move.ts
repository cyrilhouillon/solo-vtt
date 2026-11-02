import { formatLength, type SceneView, type TokenView } from '../types'
import { occupant, type Point } from './grid'

/** Tolérance d'arrondi, comme le serveur : un déplacement pile égal à ce qui reste est permis. */
const EPSILON = 1e-6

/**
 * La limite de déplacement d'un jeton : ce qui lui reste, dans l'unité de la carte. Un jeton sans limite
 * (hors combat, créature qui ne joue pas, système sans vitesse limitée) se déplace librement.
 */
export interface MoveLimit {
  tokenId: string
  left: number
}

/** Ce que dit un déplacement envisagé, avant de l'envoyer. Indicatif : le serveur recalcule et tranche. */
export interface MoveCheck {
  distance: number
  /** Ce qui restera après le déplacement, ou `null` sans limite. */
  leftAfter: number | null
  tooFar: boolean
  /** Le jeton que la créature chevaucherait à l'arrivée. */
  blocker: TokenView | undefined
  allowed: boolean
}

export function checkMove(scene: SceneView, token: TokenView, to: Point, limit: MoveLimit | null): MoveCheck {
  const distance = Math.hypot(to.x - token.x, to.y - token.y)
  const left = limit?.tokenId === token.id ? limit.left : null
  const tooFar = left !== null && distance > left + EPSILON
  const blocker = occupant(scene, to, token.id)
  return { distance, leftAfter: left === null ? null : Math.max(0, left - distance), tooFar, blocker, allowed: !tooFar && !blocker }
}

/** « 4,5 m parcourus · reste 4,5 m », ou ce qui empêche le déplacement. */
export function moveLabel(check: MoveCheck, unitSymbol: string): string {
  const travelled = `${formatLength(check.distance, unitSymbol)} parcourus`
  if (check.blocker) return `Trop près de ${check.blocker.name}`
  if (check.tooFar) return `${travelled} · au-delà de la vitesse restante`
  return check.leftAfter === null ? travelled : `${travelled} · reste ${formatLength(check.leftAfter, unitSymbol)}`
}

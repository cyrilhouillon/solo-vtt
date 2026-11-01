import type { Targeting } from './SceneCanvas'

/** Tolérance d'arrondi, comme le serveur : une cible pile à la limite est à portée. */
const EPSILON = 1e-6

/**
 * Ce que la distance dit d'une cible avant le clic, d'après les cercles du ciblage.
 * Indicatif : le serveur recalcule la distance et fait foi.
 */
export interface RangeVerdict {
  label: string
  /** Faux si le serveur refusera la cible. */
  allowed: boolean
}

export function rangeVerdict(targeting: Targeting, distance: number): RangeVerdict | null {
  const normal = targeting.circles.find((c) => !c.dashed)?.radius
  if (normal === undefined) return null
  if (targeting.measuredBy === 'reach') {
    return distance <= normal + EPSILON
      ? { label: "à l'allonge", allowed: true }
      : { label: "hors d'allonge", allowed: false }
  }
  const long = targeting.circles.find((c) => c.dashed)?.radius ?? normal
  if (distance <= normal + EPSILON) return { label: 'portée normale', allowed: true }
  if (distance <= long + EPSILON) return { label: 'portée longue : désavantage', allowed: true }
  return { label: 'au-delà de la portée longue', allowed: false }
}

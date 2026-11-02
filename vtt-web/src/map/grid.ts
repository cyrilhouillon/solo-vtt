import type { DistanceUnit, SceneView, TokenView } from '../types'

/** Taille d'une case : 1,5 m ou 5 ft, l'emprise d'une créature de taille moyenne. */
export const CELL: Record<DistanceUnit, number> = { METRE: 1.5, FOOT: 5 }

export interface Point {
  x: number
  y: number
}

/** Centre exact de la case qui contient le point ; une case partielle en bord de carte reste dans la carte. */
export function snapToCell(scene: SceneView, point: Point): Point {
  const cell = CELL[scene.unit]
  const centre = (value: number, max: number) => Math.min(max, (Math.floor(Math.min(value, max - 1e-9) / cell) + 0.5) * cell)
  return { x: centre(point.x, scene.width), y: centre(point.y, scene.height) }
}

/** Tolérance d'arrondi : deux jetons calés sur des cases voisines ne se chevauchent pas. */
const EPSILON = 1e-6

/**
 * Le jeton qui chevaucherait une créature posée au point : son centre est à moins d'une case,
 * que les positions soient calées sur la grille ou non. `ignoreId` exclut le jeton qu'on déplace.
 */
export function occupant(scene: SceneView, point: Point, ignoreId?: string): TokenView | undefined {
  const cell = CELL[scene.unit]
  return scene.tokens.find((t) => t.id !== ignoreId && Math.hypot(t.x - point.x, t.y - point.y) < cell - EPSILON)
}

/** Au dixième d'unité : assez précis pour une table, et lisible dans le journal. */
export function round(value: number): number {
  return Math.round(value * 10) / 10
}

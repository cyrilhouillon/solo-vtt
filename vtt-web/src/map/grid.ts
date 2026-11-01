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

/** Le jeton dont le centre est dans la même case que le point, s'il y en a un. */
export function occupant(scene: SceneView, point: Point): TokenView | undefined {
  const cell = CELL[scene.unit]
  const index = (value: number) => Math.floor(value / cell)
  return scene.tokens.find((t) => index(t.x) === index(point.x) && index(t.y) === index(point.y))
}

/** Au dixième d'unité : assez précis pour une table, et lisible dans le journal. */
export function round(value: number): number {
  return Math.round(value * 10) / 10
}

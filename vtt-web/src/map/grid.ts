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

/** Relâché à moins d'une case et quart d'une créature, un jeton libre se colle à son contact. */
const ATTRACTION = 1.25

const distance = (a: Point, b: Point) => Math.hypot(a.x - b.x, a.y - b.y)

/**
 * Aimant au contact, pour une position libre : près d'une ou plusieurs créatures, le point le plus proche où le jeton
 * les touche (exactement une case de centre à centre), sans en chevaucher aucune. Entre deux créatures, il peut toucher
 * les deux à la fois. `accept` écarte les points refusés d'avance (au-delà de la vitesse restante) ; sans point valable,
 * la position reste telle quelle.
 */
export function magnet(scene: SceneView, point: Point, ignoreId?: string, accept: (p: Point) => boolean = () => true): Point {
  const cell = CELL[scene.unit]
  const others = scene.tokens.filter((t) => t.id !== ignoreId)
  const near = others.filter((t) => distance(t, point) < cell * ATTRACTION)
  if (!near.length) return point
  const candidates: Point[] = []
  for (const t of near) {
    const d = distance(t, point)
    candidates.push(d > EPSILON ? { x: t.x + ((point.x - t.x) / d) * cell, y: t.y + ((point.y - t.y) / d) * cell } : { x: t.x + cell, y: t.y })
  }
  // Au contact de deux créatures : les intersections des cercles d'une case autour de chacune.
  for (let i = 0; i < near.length; i++) {
    for (let j = i + 1; j < near.length; j++) {
      const [a, b] = [near[i], near[j]]
      const d = distance(a, b)
      if (d < EPSILON || d > 2 * cell) continue
      const h = Math.sqrt(cell * cell - (d / 2) ** 2)
      const mid = { x: (a.x + b.x) / 2, y: (a.y + b.y) / 2 }
      const n = { x: -(b.y - a.y) / d, y: (b.x - a.x) / d }
      candidates.push({ x: mid.x + n.x * h, y: mid.y + n.y * h }, { x: mid.x - n.x * h, y: mid.y - n.y * h })
    }
  }
  const inside = (p: Point) => p.x >= 0 && p.y >= 0 && p.x <= scene.width && p.y <= scene.height
  const free = (p: Point) => others.every((t) => distance(t, p) >= cell - EPSILON)
  const valid = candidates.filter((p) => inside(p) && free(p) && accept(p))
  if (!valid.length) return point
  return valid.reduce((best, p) => (distance(p, point) < distance(best, point) ? p : best))
}

/** Au dixième d'unité : assez précis pour une table, et lisible dans le journal. */
export function round(value: number): number {
  return Math.round(value * 10) / 10
}

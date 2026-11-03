/**
 * Les formes des dés : sommets, faces planes (polygones orientés vers l'extérieur) et normales, sans dépendance au
 * rendu ni à la physique. Les faces se déduisent des sommets (enveloppe convexe) : il suffit de placer les sommets.
 */

export type Vec3 = [number, number, number]

export interface Face {
  /** Indices des sommets, dans le sens trigonométrique vu de l'extérieur. */
  vertices: number[]
  /** Normale sortante, unitaire. */
  normal: Vec3
}

export interface Shape {
  sides: number
  /** Sommets, rayon de l'enveloppe ramené à 1. */
  vertices: Vec3[]
  faces: Face[]
  /** Le d4 se lit au sommet du haut ; les autres à la face du dessus. */
  readsVertex: boolean
}

const PHI = (1 + Math.sqrt(5)) / 2

const sub = (a: Vec3, b: Vec3): Vec3 => [a[0] - b[0], a[1] - b[1], a[2] - b[2]]
const dot = (a: Vec3, b: Vec3) => a[0] * b[0] + a[1] * b[1] + a[2] * b[2]
export const cross = (a: Vec3, b: Vec3): Vec3 => [a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]]
export const normalize = (a: Vec3): Vec3 => {
  const l = Math.hypot(a[0], a[1], a[2])
  return [a[0] / l, a[1] / l, a[2] / l]
}

/** Toutes les combinaisons de signes d'un sommet : (±a, ±b, ±c), sans doublon pour les zéros. */
function signs(a: number, b: number, c: number): Vec3[] {
  const out: Vec3[] = []
  for (const x of a ? [a, -a] : [0]) for (const y of b ? [b, -b] : [0]) for (const z of c ? [c, -c] : [0]) out.push([x, y, z])
  return out
}

function vertexSet(sides: number): Vec3[] {
  switch (sides) {
    case 4:
      return [[1, 1, 1], [1, -1, -1], [-1, 1, -1], [-1, -1, 1]]
    case 6:
      return signs(1, 1, 1)
    case 8:
      return [...signs(1, 0, 0), ...signs(0, 1, 0), ...signs(0, 0, 1)]
    case 10: {
      // Trapézoèdre pentagonal : deux pointes et un anneau de dix sommets alternés en hauteur. Les faces (cerfs-volants)
      // sont planes quand pointe = anneau × (1 + cos 36°) / (1 − cos 36°).
      const ring = 0.105
      const c = Math.cos(Math.PI / 5)
      const apex = (ring * (1 + c)) / (1 - c)
      const out: Vec3[] = [[0, apex, 0], [0, -apex, 0]]
      for (let k = 0; k < 10; k++) {
        const a = (k * Math.PI) / 5
        out.push([Math.cos(a), k % 2 ? -ring : ring, Math.sin(a)])
      }
      return out
    }
    case 12:
      return [...signs(1, 1, 1), ...signs(0, 1 / PHI, PHI), ...signs(1 / PHI, PHI, 0), ...signs(PHI, 0, 1 / PHI)]
    case 20:
      return [...signs(0, 1, PHI), ...signs(1, PHI, 0), ...signs(PHI, 0, 1)]
    default:
      throw new Error(`Pas de dé à ${sides} faces`)
  }
}

/** Les faces planes de l'enveloppe convexe : chaque plan qui laisse tous les sommets d'un même côté. */
function hullFaces(vertices: Vec3[]): Face[] {
  const eps = 1e-6
  const faces: Face[] = []
  const n = vertices.length
  for (let i = 0; i < n; i++)
    for (let j = i + 1; j < n; j++)
      for (let k = j + 1; k < n; k++) {
        const cr = cross(sub(vertices[j], vertices[i]), sub(vertices[k], vertices[i]))
        if (Math.hypot(...cr) < eps) continue
        let normal = normalize(cr)
        const d = dot(normal, vertices[i])
        const side = vertices.map((v) => dot(normal, v) - d)
        const above = side.some((s) => s > eps)
        const below = side.some((s) => s < -eps)
        if (above && below) continue
        if (above) normal = [-normal[0], -normal[1], -normal[2]]
        if (faces.some((f) => dot(f.normal, normal) > 1 - eps)) continue
        const onPlane = side.flatMap((s, index) => (Math.abs(s) < eps ? [index] : []))
        faces.push({ vertices: ordered(vertices, onPlane, normal), normal })
      }
  return faces
}

/** Les sommets d'une face, rangés dans le sens trigonométrique vu de l'extérieur. */
function ordered(vertices: Vec3[], indices: number[], normal: Vec3): number[] {
  const c = centroid(indices.map((i) => vertices[i]))
  const up = normalize(sub(vertices[indices[0]], c))
  const right = cross(up, normal)
  const angle = (i: number) => {
    const d = sub(vertices[i], c)
    return Math.atan2(dot(d, up), dot(d, right))
  }
  return [...indices].sort((a, b) => angle(a) - angle(b))
}

export function centroid(points: Vec3[]): Vec3 {
  const s = points.reduce<Vec3>((acc, p) => [acc[0] + p[0], acc[1] + p[1], acc[2] + p[2]], [0, 0, 0])
  return [s[0] / points.length, s[1] / points.length, s[2] / points.length]
}

const cache = new Map<number, Shape>()

/** La forme d'un dé à `sides` faces (4, 6, 8, 10, 12 ou 20). */
export function shapeOf(sides: number): Shape {
  let shape = cache.get(sides)
  if (!shape) {
    const raw = vertexSet(sides)
    const radius = Math.max(...raw.map((v) => Math.hypot(...v)))
    const vertices = raw.map((v): Vec3 => [v[0] / radius, v[1] / radius, v[2] / radius])
    const faces = hullFaces(vertices)
    const expected = sides === 4 ? 4 : sides
    if (faces.length !== expected) throw new Error(`d${sides} : ${faces.length} faces au lieu de ${expected}`)
    shape = { sides, vertices, faces, readsVertex: sides === 4 }
    cache.set(sides, shape)
  }
  return shape
}

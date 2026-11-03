import * as CANNON from 'cannon-es'
import * as THREE from 'three'
import type { RolledDice } from '../types'
import { centroid, cross, normalize, shapeOf, type Face, type Shape, type Vec3 } from './shapes'

/**
 * Les dés 3D lancés sur la carte. La physique ne choisit que le mouvement : le lancer est simulé en entier hors écran,
 * puis la face qui finit en haut échange son numéro avec celle qui porte la valeur du serveur, et les images
 * enregistrées sont rejouées. Le dé s'arrête donc naturellement sur la valeur tirée par le serveur, sans saut.
 */

/** Un dé à lancer : ses faces, la valeur du serveur, s'il est le d20 écarté, s'il compte les dégâts. */
interface DieSpec {
  sides: number
  value: number
  dropped: boolean
  damage: boolean
}

/** Un dé en jeu : sa forme, ses numéros (par face, ou par sommet pour le d4), son maillage et ses images. */
interface Die {
  spec: DieSpec
  shape: Shape
  labels: number[]
  mesh: THREE.Mesh
  frames: { p: Vec3; q: [number, number, number, number] }[]
}

/** Rayon d'un dé à l'écran, en pixels : l'unité du monde physique. */
const DIE_PX = 30
const STEP = 1 / 120
/** Une image enregistrée tous les deux pas : 60 images par seconde. */
const STEPS_PER_FRAME = 2
const MAX_FRAMES = 60 * 4
const HOLD_MS = 800
const FADE_MS = 400
const FONT = "'Alegreya SC', Georgia, serif"
const TEXTURE_PX = 256

/** Os et encre de noyer pour le jet d'attaque, rubrique et ivoire pour les dégâts (palette de `style.css`). */
const PALETTE = {
  attack: { body: '#efe6d2', ink: '#2a1a10' },
  damage: { body: '#93231d', ink: '#efe6d2' },
}

const random = (min: number, max: number) => min + Math.random() * (max - min)

function specsOf(rolls: RolledDice[], damage: boolean): DieSpec[] {
  return rolls.flatMap((r) => {
    const keptAt = r.kept === null ? -1 : r.values.indexOf(r.kept)
    return r.values.map((value, i) => ({ sides: r.sides, value, dropped: r.kept !== null && i !== keptAt, damage }))
  })
}

const rotate = (q: THREE.Quaternion, v: Vec3) => new THREE.Vector3(...v).applyQuaternion(q)

export class DiceRoller {
  private readonly renderer: THREE.WebGLRenderer
  private readonly camera = new THREE.PerspectiveCamera(30, 1, 0.1, 1000)
  private width = 1
  private height = 1
  private frame = 0
  /** Numéro du dernier lancer demandé : un lancer dépassé pendant le chargement de la police ne part pas. */
  private generation = 0
  private finish: (() => void) | null = null

  /** Lève une erreur si le navigateur n'a pas WebGL : l'appelant pose alors les valeurs d'emblée. */
  constructor(canvas: HTMLCanvasElement) {
    this.renderer = new THREE.WebGLRenderer({ canvas, alpha: true, antialias: true })
    this.renderer.setPixelRatio(window.devicePixelRatio)
    this.renderer.setClearColor(0x000000, 0)
    this.renderer.shadowMap.enabled = true
    this.renderer.shadowMap.type = THREE.PCFShadowMap
  }

  /** La taille de la carte, en pixels CSS. */
  resize(width: number, height: number): void {
    this.width = Math.max(1, width)
    this.height = Math.max(1, height)
    this.renderer.setSize(this.width, this.height, false)
    this.placeCamera()
  }

  /** Lance les dés ; la promesse se résout quand ils se sont effacés, ou tout de suite si un autre lancer les arrête. */
  async roll(attack: RolledDice[], damage: RolledDice[]): Promise<void> {
    this.cancel()
    const generation = ++this.generation
    const specs = [...specsOf(attack, false), ...specsOf(damage, true)]
    if (!specs.length) return
    await document.fonts.load(`700 64px ${FONT}`).catch(() => undefined)
    if (generation !== this.generation) return
    return new Promise<void>((resolve) => {
      const dice = this.simulate(specs)
      const scene = this.stage(dice)
      this.finish = () => {
        this.finish = null
        cancelAnimationFrame(this.frame)
        this.dispose(scene)
        this.renderer.clear()
        resolve()
      }
      this.play(scene, dice)
    })
  }

  /** Arrête le lancer en cours : sa promesse se résout tout de suite. */
  cancel(): void {
    this.generation++
    this.finish?.()
  }

  destroy(): void {
    this.cancel()
    this.renderer.dispose()
  }

  /** Caméra droite au-dessus du sol, qui voit exactement la carte. */
  private placeCamera(): void {
    const h = this.height / DIE_PX
    this.camera.aspect = this.width / this.height
    this.camera.position.set(0, h / 2 / Math.tan(THREE.MathUtils.degToRad(this.camera.fov / 2)), 0)
    this.camera.up.set(0, 0, -1)
    this.camera.lookAt(0, 0, 0)
    this.camera.updateProjectionMatrix()
  }

  /** Le lancer entier, hors écran : sol, quatre murs aux bords de la carte, dés lancés depuis le bas. */
  private simulate(specs: DieSpec[]): Die[] {
    const w = this.width / DIE_PX
    const h = this.height / DIE_PX
    const world = new CANNON.World({ gravity: new CANNON.Vec3(0, -60, 0), allowSleep: true })
    const felt = new CANNON.Material()
    const bone = new CANNON.Material()
    world.addContactMaterial(new CANNON.ContactMaterial(felt, bone, { friction: 0.35, restitution: 0.35 }))
    world.addContactMaterial(new CANNON.ContactMaterial(bone, bone, { friction: 0.2, restitution: 0.4 }))
    const planes: [Vec3, CANNON.Vec3][] = [
      [[-Math.PI / 2, 0, 0], new CANNON.Vec3(0, 0, 0)],
      [[0, Math.PI / 2, 0], new CANNON.Vec3(-w / 2, 0, 0)],
      [[0, -Math.PI / 2, 0], new CANNON.Vec3(w / 2, 0, 0)],
      [[0, 0, 0], new CANNON.Vec3(0, 0, -h / 2)],
      [[0, Math.PI, 0], new CANNON.Vec3(0, 0, h / 2)],
    ]
    for (const [euler, position] of planes) {
      const plane = new CANNON.Body({ mass: 0, material: felt, shape: new CANNON.Plane(), position })
      plane.quaternion.setFromEuler(...euler)
      world.addBody(plane)
    }

    const bodies = specs.map((spec, i) => {
      const shape = shapeOf(spec.sides)
      const body = new CANNON.Body({
        mass: 1,
        material: bone,
        shape: new CANNON.ConvexPolyhedron({
          vertices: shape.vertices.map((v) => new CANNON.Vec3(...v)),
          faces: shape.faces.map((f) => f.vertices),
        }),
        linearDamping: 0.2,
        angularDamping: 0.3,
        allowSleep: true,
        sleepSpeedLimit: 0.5,
        sleepTimeLimit: 0.25,
      })
      const spread = Math.min(2.6, (w - 3) / Math.max(1, specs.length))
      body.position.set((i - (specs.length - 1) / 2) * spread + random(-0.3, 0.3), random(1.5, 3), h / 2 - 1.5)
      body.quaternion.setFromEuler(random(0, 2 * Math.PI), random(0, 2 * Math.PI), random(0, 2 * Math.PI))
      const speed = Math.min(h * 1.1, 26)
      body.velocity.set(random(-3, 3), random(2, 5), -random(0.75, 1) * speed)
      body.angularVelocity.set(random(-20, 20), random(-20, 20), random(-20, 20))
      world.addBody(body)
      return body
    })

    const dice: Die[] = specs.map((spec) => {
      const shape = shapeOf(spec.sides)
      const labels = (shape.readsVertex ? shape.vertices : shape.faces).map((_, i) => i + 1)
      return { spec, shape, labels, mesh: new THREE.Mesh(), frames: [] }
    })
    for (let f = 0; f < MAX_FRAMES; f++) {
      for (let s = 0; s < STEPS_PER_FRAME; s++) world.step(STEP)
      bodies.forEach((b, i) => {
        dice[i].frames.push({
          p: [b.position.x, b.position.y, b.position.z],
          q: [b.quaternion.x, b.quaternion.y, b.quaternion.z, b.quaternion.w],
        })
      })
      if (bodies.every((b) => b.sleepState === CANNON.Body.SLEEPING)) break
    }

    for (const die of dice) this.relabel(die)
    return dice
  }

  /** La face (ou le sommet du d4) qui finit en haut échange son numéro avec celle qui porte la valeur du serveur. */
  private relabel(die: Die): void {
    const last = die.frames[die.frames.length - 1]
    const q = new THREE.Quaternion(...last.q)
    const heights = die.shape.readsVertex
      ? die.shape.vertices.map((v) => rotate(q, v).y)
      : die.shape.faces.map((f) => rotate(q, f.normal).y)
    const up = heights.indexOf(Math.max(...heights))
    const target = die.labels.indexOf(die.spec.value)
    if (target < 0 || target === up) return
    ;[die.labels[up], die.labels[target]] = [die.labels[target], die.labels[up]]
  }

  /** La scène three : lumière chaude, ombres sur un sol invisible, un maillage par dé. */
  private stage(dice: Die[]): THREE.Scene {
    const scene = new THREE.Scene()
    const w = this.width / DIE_PX
    const h = this.height / DIE_PX
    scene.add(new THREE.HemisphereLight(0xfff3dc, 0x3b2418, 1.6))
    const sun = new THREE.DirectionalLight(0xffe8c0, 2.2)
    sun.position.set(-w * 0.3, 40, -h * 0.4)
    sun.castShadow = true
    sun.shadow.mapSize.set(2048, 2048)
    const reach = Math.max(w, h) / 2 + 2
    Object.assign(sun.shadow.camera, { left: -reach, right: reach, top: reach, bottom: -reach, near: 1, far: 100 })
    sun.shadow.radius = 4
    scene.add(sun)
    const floor = new THREE.Mesh(new THREE.PlaneGeometry(w, h), new THREE.ShadowMaterial({ opacity: 0.35, transparent: true }))
    floor.rotation.x = -Math.PI / 2
    floor.receiveShadow = true
    scene.add(floor)
    for (const die of dice) {
      die.mesh = this.meshOf(die)
      scene.add(die.mesh)
    }
    return scene
  }

  /** Rejoue les images, tient les dés arrêtés, atténue le d20 écarté, puis efface tout. */
  private play(scene: THREE.Scene, dice: Die[]): void {
    const frames = Math.max(...dice.map((d) => d.frames.length))
    const start = performance.now()
    const landedAt = start + (frames * 1000) / 60
    let dimmed = false
    const draw = (now: number) => {
      // L'horodatage d'une image peut précéder `start` : elle montre alors la première image.
      const index = Math.max(0, Math.min(frames - 1, Math.floor(((now - start) * 60) / 1000)))
      for (const die of dice) {
        const f = die.frames[Math.min(index, die.frames.length - 1)]!
        die.mesh.position.set(...f.p)
        die.mesh.quaternion.set(...f.q)
      }
      if (now >= landedAt && !dimmed) {
        dimmed = true
        for (const die of dice) if (die.spec.dropped) this.setOpacity(die.mesh, 0.45)
      }
      const fading = now - landedAt - HOLD_MS
      if (fading > 0) {
        const left = Math.max(0, 1 - fading / FADE_MS)
        scene.traverse((o) => {
          if (o instanceof THREE.Mesh) {
            const die = dice.find((d) => d.mesh === o)
            this.setOpacity(o, left * (die?.spec.dropped ? 0.45 : die ? 1 : 0.35))
          }
        })
      }
      this.renderer.render(scene, this.camera)
      return fading >= FADE_MS
    }
    const tick = (now: number) => {
      let done = true
      try {
        done = draw(now)
      } catch (e) {
        // Une image qui échoue ne doit pas laisser la piste attendre : le lancer se termine.
        console.warn('Dés 3D :', e)
      }
      if (done) this.finish?.()
      else this.frame = requestAnimationFrame(tick)
    }
    this.frame = requestAnimationFrame(tick)
  }

  private setOpacity(mesh: THREE.Mesh, opacity: number): void {
    for (const m of [mesh.material].flat()) {
      m.transparent = true
      m.opacity = opacity
    }
  }

  /** Le maillage d'un dé : une face par groupe, chacune avec sa texture numérotée. */
  private meshOf(die: Die): THREE.Mesh {
    const { shape } = die
    const positions: number[] = []
    const normals: number[] = []
    const uvs: number[] = []
    const geometry = new THREE.BufferGeometry()
    const materials = shape.faces.map((face, index) => {
      const layout = faceLayout(shape, face)
      const start = positions.length / 3
      for (let k = 1; k < face.vertices.length - 1; k++) {
        for (const at of [0, k, k + 1]) {
          positions.push(...shape.vertices[face.vertices[at]])
          normals.push(...face.normal)
          uvs.push(...layout.uv[at])
        }
      }
      geometry.addGroup(start, positions.length / 3 - start, index)
      const texture = new THREE.CanvasTexture(this.drawFace(die, index, layout))
      texture.colorSpace = THREE.SRGBColorSpace
      texture.anisotropy = 4
      return new THREE.MeshStandardMaterial({ map: texture, roughness: 0.45, metalness: 0.05 })
    })
    geometry.setAttribute('position', new THREE.Float32BufferAttribute(positions, 3))
    geometry.setAttribute('normal', new THREE.Float32BufferAttribute(normals, 3))
    geometry.setAttribute('uv', new THREE.Float32BufferAttribute(uvs, 2))
    const mesh = new THREE.Mesh(geometry, materials)
    mesh.castShadow = true
    return mesh
  }

  /** Le numéro d'une face ; le d4 porte aux coins les numéros de ses sommets, tournés vers eux. */
  private drawFace(die: Die, faceIndex: number, layout: FaceLayout): HTMLCanvasElement {
    const canvas = document.createElement('canvas')
    canvas.width = canvas.height = TEXTURE_PX
    const ctx = canvas.getContext('2d')!
    const palette = die.spec.damage ? PALETTE.damage : PALETTE.attack
    ctx.fillStyle = palette.body
    ctx.fillRect(0, 0, TEXTURE_PX, TEXTURE_PX)
    ctx.fillStyle = palette.ink
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    const toCanvas = ([u, v]: [number, number]) => [u * TEXTURE_PX, (1 - v) * TEXTURE_PX] as const
    const write = (n: number, x: number, y: number, angle: number, size: number) => {
      ctx.save()
      ctx.translate(x, y)
      ctx.rotate(angle)
      ctx.font = `700 ${Math.round(size)}px ${FONT}`
      ctx.fillText(String(n), 0, 0)
      if (die.spec.sides >= 10 && (n === 6 || n === 9)) ctx.fillRect(-size * 0.22, size * 0.42, size * 0.44, size * 0.07)
      ctx.restore()
    }
    const [cx, cy] = toCanvas([0.5, 0.5])
    if (die.shape.readsVertex) {
      die.shape.faces[faceIndex].vertices.forEach((vertex, at) => {
        const [x, y] = toCanvas(layout.uv[at])
        const px = cx + (x - cx) * 0.6
        const py = cy + (y - cy) * 0.6
        write(die.labels[vertex], px, py, Math.atan2(x - cx, cy - y), TEXTURE_PX * 0.24)
      })
    } else {
      const size = { 6: 0.5, 8: 0.36, 10: 0.36, 12: 0.38, 20: 0.3 }[die.spec.sides] ?? 0.35
      write(die.labels[faceIndex], cx, cy, 0, TEXTURE_PX * size)
    }
    return canvas
  }

  private dispose(scene: THREE.Scene): void {
    scene.traverse((o) => {
      if (!(o instanceof THREE.Mesh)) return
      o.geometry.dispose()
      for (const m of [o.material].flat()) {
        ;(m as THREE.MeshStandardMaterial).map?.dispose()
        m.dispose()
      }
    })
  }
}

/** Où tombent les sommets d'une face sur sa texture, dans l'ordre de la face. */
interface FaceLayout {
  uv: [number, number][]
}

/**
 * Le repère d'une face : le haut du numéro vers un sommet (triangles, pentagones), vers une arête (d6) ou vers la
 * pointe (d10), à droite le produit vectoriel haut × normale pour que le numéro ne soit pas en miroir.
 */
function faceLayout(shape: Shape, face: Face): FaceLayout {
  const points = face.vertices.map((i) => shape.vertices[i])
  const c = centroid(points)
  let toward: Vec3 = points[0]
  if (shape.sides === 6) toward = centroid([points[0], points[1]])
  if (shape.sides === 10) toward = points.reduce((a, b) => (Math.abs(b[1]) > Math.abs(a[1]) ? b : a))
  const up = normalize([toward[0] - c[0], toward[1] - c[1], toward[2] - c[2]])
  const right = cross(up, face.normal)
  const local = points.map((p) => {
    const d: Vec3 = [p[0] - c[0], p[1] - c[1], p[2] - c[2]]
    return [d[0] * right[0] + d[1] * right[1] + d[2] * right[2], d[0] * up[0] + d[1] * up[1] + d[2] * up[2]] as const
  })
  const scale = Math.max(...local.map(([x, y]) => Math.hypot(x, y))) / 0.48
  return { uv: local.map(([x, y]) => [0.5 + x / scale, 0.5 + y / scale]) }
}

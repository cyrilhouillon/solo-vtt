import Konva from 'konva'
import { formatLength, type CreatureKind, type SceneView, type TokenView } from '../types'
import { CELL, occupant, round, snapToCell, type Point } from './grid'
import { rangeVerdict, type RangeVerdict } from './range'

/** Ce que la carte signale ; les coordonnées sont dans l'unité de la carte. */
export interface SceneCanvasEvents {
  /** `free` : la touche Maj était tenue, la position n'est pas calée sur la case. */
  onBackgroundClick(x: number, y: number, free: boolean): void
  onTokenClick(tokenId: string): void
}

export interface SceneCanvasState {
  scene: SceneView
  selected: string[]
  /** Le type de la créature en cours de placement, ou `null` hors placement. */
  placing: CreatureKind | null
  /** Case visée au clavier pendant le placement, ou `null`. */
  cursor: Point | null
  /** Ciblage en cours, ou `null`. */
  targeting: Targeting | null
}

/**
 * Ciblage d'une attaque : cercles centrés sur l'attaquant, dans l'unité de la carte.
 * Ils sont indicatifs ; c'est le serveur qui accepte ou refuse la cible.
 */
export interface Targeting {
  attackerId: string
  /** Allonge au corps à corps, ou portée à distance (lancer compris). */
  measuredBy: 'reach' | 'range'
  /** Allonge, ou portée normale puis longue (`dashed`). */
  circles: { radius: number; dashed: boolean }[]
}

/** Ce que la distance à l'attaquant permet, pour une créature survolée en ciblage. */
type TargetHint = RangeVerdict & { distance: number }

const MARGIN = 16
const FONT = "'Alegreya Sans', system-ui, sans-serif"

/** Initiales d'un nom, numéro compris : « Gobelin guerrier 2 » donne « GG2 ». */
function initialsOf(name: string): string {
  const words = name.split(/\s+/).filter(Boolean)
  const number = /^\d+$/.test(words.at(-1) ?? '') ? words.pop() : ''
  return words
    .slice(0, 2)
    .map((w) => w[0].toUpperCase())
    .join('') + number
}

const numberFormat = new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 1 })

/**
 * Rendu de la carte avec Konva, indépendant de Vue. Toute conversion entre pixels
 * et unité de la carte passe par {@link toPixelsX}, {@link toPixelsY} et {@link toUnits}.
 */
export class SceneCanvas {
  private readonly stage: Konva.Stage
  private readonly layer = new Konva.Layer()
  /** Aperçu du placement sous le curseur, redessiné sans toucher au reste. */
  private readonly preview = new Konva.Layer({ listening: false })
  private readonly events: SceneCanvasEvents
  private readonly container: HTMLDivElement
  /** Couleurs du thème, relues à chaque dessin : le joueur peut changer de thème en cours de partie. */
  private colors: Record<string, string> = {}
  private state: SceneCanvasState | null = null
  private pixelsPerUnit = 1
  private origin = { x: MARGIN, y: MARGIN }
  private pointerInside = false
  /** Maj change l'aperçu sans attendre que la souris bouge. */
  private readonly onShift = (e: KeyboardEvent) => {
    if (e.key === 'Shift' && this.pointerInside) this.drawPointerPreview(e.type === 'keydown')
  }

  constructor(container: HTMLDivElement, events: SceneCanvasEvents) {
    this.events = events
    this.container = container
    this.stage = new Konva.Stage({ container, width: container.clientWidth, height: container.clientHeight })
    this.stage.add(this.layer, this.preview)
    this.stage.on('mousemove', (e) => {
      this.pointerInside = true
      this.drawPointerPreview(e.evt.shiftKey)
    })
    this.stage.on('mouseleave', () => {
      this.pointerInside = false
      this.clearPreview()
    })
    window.addEventListener('keydown', this.onShift)
    window.addEventListener('keyup', this.onShift)
  }

  resize(width: number, height: number): void {
    this.stage.size({ width, height })
    this.redraw()
  }

  render(state: SceneCanvasState): void {
    this.state = state
    this.redraw()
  }

  destroy(): void {
    window.removeEventListener('keydown', this.onShift)
    window.removeEventListener('keyup', this.onShift)
    this.stage.destroy()
  }

  private toPixelsX(value: number): number {
    return this.origin.x + value * this.pixelsPerUnit
  }

  private toPixelsY(value: number): number {
    return this.origin.y + value * this.pixelsPerUnit
  }

  private toUnits(pixels: { x: number; y: number }): Point {
    return {
      x: (pixels.x - this.origin.x) / this.pixelsPerUnit,
      y: (pixels.y - this.origin.y) / this.pixelsPerUnit,
    }
  }

  private readColors(): void {
    const style = getComputedStyle(this.container)
    const token = (name: string) => style.getPropertyValue(name).trim()
    this.colors = {
      ground: token('--map-ground'),
      grid: token('--map-grid'),
      border: token('--map-border'),
      character: token('--token-character'),
      monster: token('--token-monster'),
      rim: token('--token-rim'),
      live: token('--map-live'),
      down: token('--map-down'),
      label: token('--map-label'),
      labelGround: token('--map-label-ground'),
      danger: token('--danger'),
    }
  }

  private redraw(): void {
    this.readColors()
    this.layer.destroyChildren()
    this.clearPreview()
    if (!this.state) return
    const { scene, selected, placing } = this.state
    const available = { w: this.stage.width() - 2 * MARGIN, h: this.stage.height() - 2 * MARGIN }
    this.pixelsPerUnit = Math.max(0.1, Math.min(available.w / scene.width, available.h / scene.height))
    // Centrée dans la place disponible.
    this.origin = {
      x: Math.round((this.stage.width() - scene.width * this.pixelsPerUnit) / 2),
      y: Math.round((this.stage.height() - scene.height * this.pixelsPerUnit) / 2),
    }

    this.drawGround(scene)
    if (this.state.targeting) this.drawTargeting(scene, this.state.targeting)
    const measure = this.measureLabel(scene, selected)
    // Les noms passent au-dessus de tous les jetons ; ceux des jetons sélectionnés en dernier.
    const names = new Konva.Group({ listening: false })
    const ordered = [...scene.tokens].sort((a, b) => Number(selected.includes(a.id)) - Number(selected.includes(b.id)))
    const verdictOf = this.verdicts(scene, this.state.targeting)
    for (const token of ordered) {
      this.drawToken(scene, token, selected.includes(token.id), placing === null, names, verdictOf(token))
    }
    this.layer.add(names)
    if (measure) this.layer.add(measure)
    this.layer.draw()
    if (this.state.cursor) this.drawPreview(this.state.cursor, false, true)
  }

  /** Position visée par le pointeur, calée sur la case sauf si `free`, ou `null` hors de la carte. */
  private pointerTarget(scene: SceneView, free: boolean): Point | null {
    const pointer = this.stage.getPointerPosition()
    if (!pointer) return null
    const p = this.toUnits(pointer)
    if (p.x < 0 || p.y < 0 || p.x > scene.width || p.y > scene.height) return null
    return free ? { x: round(p.x), y: round(p.y) } : snapToCell(scene, p)
  }

  private drawGround(scene: SceneView): void {
    const ground = new Konva.Rect({
      x: this.toPixelsX(0),
      y: this.toPixelsY(0),
      width: scene.width * this.pixelsPerUnit,
      height: scene.height * this.pixelsPerUnit,
      fill: this.colors.ground,
      stroke: this.colors.border,
      strokeWidth: 1,
    })
    ground.on('click tap', (e) => {
      const free = 'shiftKey' in e.evt && e.evt.shiftKey
      const target = this.pointerTarget(scene, free)
      if (target) this.events.onBackgroundClick(target.x, target.y, free)
    })
    this.layer.add(ground)

    // Au demi-pixel près, pour des traits de 1 px nets et réguliers.
    const crisp = (pixels: number) => Math.round(pixels) + 0.5
    const cell = CELL[scene.unit]
    const lines = new Konva.Group({ listening: false })
    for (let x = cell; x < scene.width; x += cell) {
      const px = crisp(this.toPixelsX(x))
      lines.add(this.gridLine([px, this.toPixelsY(0), px, this.toPixelsY(scene.height)]))
    }
    for (let y = cell; y < scene.height; y += cell) {
      const py = crisp(this.toPixelsY(y))
      lines.add(this.gridLine([this.toPixelsX(0), py, this.toPixelsX(scene.width), py]))
    }
    this.layer.add(lines)
  }

  private drawTargeting(scene: SceneView, targeting: Targeting): void {
    const attacker = scene.tokens.find((t) => t.id === targeting.attackerId)
    if (!attacker) return
    // Une portée plus grande que la carte ne déborde pas sur les marges.
    const clipped = new Konva.Group({
      listening: false,
      clip: {
        x: this.toPixelsX(0),
        y: this.toPixelsY(0),
        width: scene.width * this.pixelsPerUnit,
        height: scene.height * this.pixelsPerUnit,
      },
    })
    this.layer.add(clipped)
    const x = this.toPixelsX(attacker.x)
    const y = this.toPixelsY(attacker.y)
    const normal = targeting.circles.find((c) => !c.dashed)
    const long = targeting.circles.find((c) => c.dashed)
    // Portée longue (désavantage) : bande hachurée, lisible sans la couleur.
    if (normal && long) {
      clipped.add(
        new Konva.Ring({
          x,
          y,
          innerRadius: normal.radius * this.pixelsPerUnit,
          outerRadius: long.radius * this.pixelsPerUnit,
          fillPatternImage: this.hatch() as unknown as HTMLImageElement,
          fillPatternRepeat: 'repeat',
          listening: false,
        }),
      )
    }
    for (const circle of targeting.circles) {
      clipped.add(
        new Konva.Circle({
          x,
          y,
          radius: circle.radius * this.pixelsPerUnit,
          stroke: this.colors.live,
          strokeWidth: 2,
          dash: circle.dashed ? [8, 6] : undefined,
          listening: false,
        }),
      )
    }
  }

  /** Motif de hachures obliques, dans la couleur de l'état vivant. */
  private hatch(): HTMLCanvasElement {
    const tile = document.createElement('canvas')
    tile.width = tile.height = 10
    const ctx = tile.getContext('2d')!
    ctx.strokeStyle = this.colors.live
    ctx.globalAlpha = 0.45
    ctx.lineWidth = 1.5
    ctx.beginPath()
    ctx.moveTo(-1, 11)
    ctx.lineTo(11, -1)
    ctx.stroke()
    return tile
  }

  /**
   * En ciblage, la distance de chaque créature à l'attaquant et ce qu'elle permet ; `null` pour
   * l'attaquant et hors ciblage. Indicatif : la distance qui fait foi est celle du serveur.
   */
  private verdicts(scene: SceneView, targeting: Targeting | null): (token: TokenView) => TargetHint | null {
    const attacker = targeting && scene.tokens.find((t) => t.id === targeting.attackerId)
    if (!targeting || !attacker) return () => null
    return (token) => {
      if (token.id === attacker.id) return null
      const distance = Math.hypot(token.x - attacker.x, token.y - attacker.y)
      const verdict = rangeVerdict(targeting, distance)
      return verdict && { ...verdict, distance }
    }
  }

  private gridLine(points: number[]): Konva.Line {
    return new Konva.Line({ points, stroke: this.colors.grid, strokeWidth: 1 })
  }

  /**
   * Trace la ligne de mesure sous les jetons et rend son étiquette, à poser par-dessus :
   * la distance dans l'unité de la carte, puis en cases.
   */
  private measureLabel(scene: SceneView, selected: string[]): Konva.Label | null {
    if (selected.length !== 2) return null
    const [a, b] = selected.map((id) => scene.tokens.find((t) => t.id === id))
    if (!a || !b) return null
    const points = [this.toPixelsX(a.x), this.toPixelsY(a.y), this.toPixelsX(b.x), this.toPixelsY(b.y)]
    this.layer.add(
      new Konva.Line({ points, stroke: this.colors.live, strokeWidth: 2, dash: [8, 6], listening: false }),
    )
    const distance = Math.hypot(b.x - a.x, b.y - a.y)
    const cells = distance / CELL[scene.unit]
    const label = new Konva.Label({ x: (points[0] + points[2]) / 2, y: (points[1] + points[3]) / 2, listening: false })
    label.add(new Konva.Tag({ fill: this.colors.labelGround, stroke: this.colors.live, strokeWidth: 1, cornerRadius: 4 }))
    label.add(
      new Konva.Text({
        text: `${formatLength(distance, scene.unitSymbol)} · ${numberFormat.format(cells)} ${cells < 2 ? 'case' : 'cases'}`,
        fontSize: 16,
        fontStyle: '700',
        fontFamily: FONT,
        fill: this.colors.label,
        padding: 5,
      }),
    )
    label.offset({ x: label.width() / 2, y: label.height() / 2 })
    // Entre deux jetons proches, l'étiquette les masquerait : on l'écarte de la ligne, vers le haut.
    const dx = points[2] - points[0]
    const dy = points[3] - points[1]
    const length = Math.hypot(dx, dy)
    const radius = this.tokenRadius(scene)
    if (length > 0 && length < label.width() + 2 * radius + 8) {
      let nx = -dy / length
      let ny = dx / length
      if (ny > 0) [nx, ny] = [-nx, -ny]
      const shift = radius + label.height() / 2 + 6
      label.move({ x: nx * shift, y: ny * shift })
    }
    return label
  }

  private tokenRadius(scene: SceneView): number {
    return Math.max(8, (CELL[scene.unit] / 2) * this.pixelsPerUnit * 0.9)
  }

  /** Personnage : disque ; monstre : carré arrondi. La forme double la couleur. */
  private tokenShape(kind: CreatureKind, radius: number, style: Konva.ShapeConfig): Konva.Shape {
    const fill = kind === 'CHARACTER' ? this.colors.character : this.colors.monster
    if (kind === 'CHARACTER') return new Konva.Circle({ radius, fill, ...style })
    const side = radius * 1.8
    return new Konva.Rect({ width: side, height: side, offsetX: side / 2, offsetY: side / 2, cornerRadius: side * 0.2, fill, ...style })
  }

  private drawToken(
    scene: SceneView,
    token: TokenView,
    isSelected: boolean,
    clickable: boolean,
    names: Konva.Group,
    hint: TargetHint | null,
  ): void {
    const radius = this.tokenRadius(scene)
    const x = this.toPixelsX(token.x)
    const y = this.toPixelsY(token.y)
    const down = token.hitPoints === 0
    const reachable = hint?.allowed ?? true
    // En placement, un clic sur un jeton vise la case : il traverse jusqu'au sol.
    // Un jeton en attente du serveur est estompé et ne réagit pas encore ; hors de portée aussi, estompé.
    const opacity = token.pending ? 0.55 : reachable ? 1 : 0.4
    const group = new Konva.Group({ x, y, listening: clickable && !token.pending, opacity })
    group.add(
      this.tokenShape(token.kind, radius, {
        stroke: isSelected ? this.colors.live : this.colors.rim,
        strokeWidth: isSelected ? 3.5 : 2,
        ...(down ? { fill: this.colors.down } : {}),
      }),
    )
    // À terre : imprimé en gris et barré d'une croix.
    if (down) {
      const arm = radius * 0.55
      for (const points of [
        [-arm, -arm, arm, arm],
        [arm, -arm, -arm, arm],
      ]) {
        group.add(new Konva.Line({ points, stroke: this.colors.rim, strokeWidth: 3, lineCap: 'round', listening: false }))
      }
    } else {
      const initials = new Konva.Text({
        text: initialsOf(token.name),
        fontSize: Math.round(Math.max(10, radius * 0.75)),
        fontStyle: '700',
        fontFamily: FONT,
        fill: this.colors.rim,
        listening: false,
      })
      initials.offset({ x: initials.width() / 2, y: initials.height() / 2 })
      group.add(initials)
    }
    // Le nom complet à la demande : sélection ou survol. Les initiales suffisent au repos.
    // En ciblage, la cible envisagée porte aussi sa CA et ses PV : tout ce qui décide du coup est sous le curseur.
    const stats = hint && token.armorClass !== undefined ? ` · CA ${token.armorClass} · ${token.hitPoints}/${token.maxHitPoints} PV` : ''
    const full = this.nameLabel(token.name + stats, radius)
    full.visible(isSelected)
    full.move({ x, y })
    names.add(full)
    // En ciblage, la cible envisagée est cerclée de laiton, et sa distance dit ce qu'elle permet.
    const ring = new Konva.Circle({ radius: radius * 1.35, stroke: this.colors.live, strokeWidth: 3, visible: false, listening: false })
    group.add(ring)
    const rangeLabel = hint && this.rangeLabel(scene, hint, radius)
    if (rangeLabel) {
      rangeLabel.move({ x, y })
      names.add(rangeLabel)
    }
    group.on('click tap', (e) => {
      e.cancelBubble = true
      this.events.onTokenClick(token.id)
    })
    // En ciblage, le viseur n'apparaît que là où un clic lance l'attaque.
    const targeting = this.state?.targeting
    const cursor = !targeting ? 'pointer' : token.id === targeting.attackerId || !reachable ? 'not-allowed' : 'crosshair'
    group.on('mouseenter', () => {
      this.stage.container().style.cursor = cursor
      full.visible(true)
      full.moveToTop()
      ring.visible(hint !== null && reachable)
      rangeLabel?.visible(true)
      rangeLabel?.moveToTop()
      this.layer.batchDraw()
    })
    group.on('mouseleave', () => {
      this.stage.container().style.cursor = ''
      full.visible(isSelected)
      ring.visible(false)
      rangeLabel?.visible(false)
      this.layer.batchDraw()
    })
    this.layer.add(group)
  }

  /** Distance et verdict de portée, au-dessus du jeton survolé ; cerclé de laiton si la cible est permise. */
  private rangeLabel(scene: SceneView, hint: TargetHint, radius: number): Konva.Label {
    const label = new Konva.Label({ y: -radius - 6, listening: false, visible: false })
    label.add(
      new Konva.Tag({
        fill: this.colors.labelGround,
        stroke: hint.allowed ? this.colors.live : undefined,
        strokeWidth: 1,
        cornerRadius: 4,
        pointerDirection: 'down',
        pointerWidth: 10,
        pointerHeight: 5,
      }),
      new Konva.Text({
        text: `${formatLength(hint.distance, scene.unitSymbol)} · ${hint.label}`,
        fontSize: 15,
        fontStyle: '700',
        fontFamily: FONT,
        fill: this.colors.label,
        padding: 4,
        wrap: 'none',
      }),
    )
    return label
  }

  /** Étiquette du nom complet, sous le jeton. */
  private nameLabel(name: string, radius: number): Konva.Label {
    const text = new Konva.Text({
      text: name,
      fontSize: Math.round(Math.min(17, Math.max(14, radius * 0.8))),
      fontStyle: '700',
      fontFamily: FONT,
      fill: this.colors.label,
      padding: 3,
      wrap: 'none',
    })
    const label = new Konva.Label({ y: radius + 4, listening: false })
    label.add(new Konva.Tag({ fill: this.colors.labelGround, cornerRadius: 3 }), text)
    label.offsetX(label.width() / 2)
    return label
  }

  private drawPointerPreview(free: boolean): void {
    if (!this.state?.placing) return this.clearPreview()
    const target = this.pointerTarget(this.state.scene, free)
    if (!target) return this.clearPreview()
    this.drawPreview(target, free, false)
  }

  /**
   * Fantôme de la créature à poser. Sur une case prise, un anneau rouge plein, plus large
   * que le jeton, et un curseur d'interdiction. `framed` encadre la case visée au clavier.
   */
  private drawPreview(target: Point, free: boolean, framed: boolean): void {
    this.clearPreview()
    if (!this.state?.placing) return
    const { scene, placing } = this.state
    const radius = this.tokenRadius(scene)
    const x = this.toPixelsX(target.x)
    const y = this.toPixelsY(target.y)
    const taken = !free && occupant(scene, target) !== undefined
    if (framed) {
      const side = CELL[scene.unit] * this.pixelsPerUnit
      this.preview.add(
        new Konva.Rect({ x: x - side / 2, y: y - side / 2, width: side, height: side, stroke: this.colors.live, strokeWidth: 2 }),
      )
    }
    if (taken) {
      this.preview.add(new Konva.Circle({ x, y, radius: radius * 1.3, stroke: this.colors.danger, strokeWidth: 3 }))
    } else {
      this.preview.add(this.tokenShape(placing, radius, { x, y, opacity: 0.5, stroke: this.colors.live, strokeWidth: 2 }))
    }
    if (!framed) this.stage.container().style.cursor = taken ? 'not-allowed' : ''
    this.preview.batchDraw()
  }

  private clearPreview(): void {
    if (this.state?.placing) this.stage.container().style.cursor = ''
    this.preview.destroyChildren()
    this.preview.batchDraw()
  }
}

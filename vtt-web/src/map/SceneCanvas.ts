import Konva from 'konva'
import { formatLength, type CreatureKind, type SceneView, type TokenView } from '../types'
import { CELL, occupant, round, snapToCell, type Point } from './grid'

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
}

const MARGIN = 16

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
  private readonly colors: Record<string, string>
  private state: SceneCanvasState | null = null
  private pixelsPerUnit = 1
  private origin = { x: MARGIN, y: MARGIN }

  constructor(container: HTMLDivElement, events: SceneCanvasEvents) {
    this.events = events
    this.stage = new Konva.Stage({ container, width: container.clientWidth, height: container.clientHeight })
    this.stage.add(this.layer, this.preview)
    const style = getComputedStyle(container)
    const token = (name: string) => style.getPropertyValue(name).trim()
    this.colors = {
      ground: token('--map-ground'),
      grid: token('--map-grid'),
      border: token('--map-border'),
      character: token('--token-character'),
      monster: token('--token-monster'),
      selected: token('--token-selected'),
      label: token('--map-label'),
      measure: token('--map-measure'),
      danger: token('--danger'),
    }
    this.stage.on('mousemove', (e) => this.drawPreview(e.evt.shiftKey))
    this.stage.on('mouseleave', () => this.clearPreview())
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

  private redraw(): void {
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
    const measure = this.measureLabel(scene, selected)
    for (const token of scene.tokens) this.drawToken(scene, token, selected.includes(token.id), placing === null)
    if (measure) this.layer.add(measure)
    this.layer.draw()
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
      new Konva.Line({ points, stroke: this.colors.measure, strokeWidth: 2, dash: [8, 6], listening: false }),
    )
    const distance = Math.hypot(b.x - a.x, b.y - a.y)
    const cells = distance / CELL[scene.unit]
    const label = new Konva.Label({ x: (points[0] + points[2]) / 2, y: (points[1] + points[3]) / 2, listening: false })
    label.add(new Konva.Tag({ fill: this.colors.ground, stroke: this.colors.measure, strokeWidth: 1, cornerRadius: 4 }))
    label.add(
      new Konva.Text({
        text: `${formatLength(distance, scene.unitSymbol)} · ${numberFormat.format(cells)} ${cells < 2 ? 'case' : 'cases'}`,
        fontSize: 15,
        fontStyle: '600',
        fontFamily: 'system-ui, sans-serif',
        fill: this.colors.measure,
        padding: 5,
      }),
    )
    label.offset({ x: label.width() / 2, y: label.height() / 2 })
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

  private drawToken(scene: SceneView, token: TokenView, isSelected: boolean, clickable: boolean): void {
    const radius = this.tokenRadius(scene)
    // En placement, un clic sur un jeton vise la case : il traverse jusqu'au sol.
    const group = new Konva.Group({ x: this.toPixelsX(token.x), y: this.toPixelsY(token.y), listening: clickable })
    group.add(
      this.tokenShape(token.kind, radius, {
        stroke: isSelected ? this.colors.selected : this.colors.ground,
        strokeWidth: isSelected ? 3 : 1.5,
      }),
    )
    const label = new Konva.Label({ y: radius + 3, listening: false })
    label.add(new Konva.Tag({ fill: this.colors.ground, opacity: 0.85, cornerRadius: 3 }))
    label.add(
      new Konva.Text({
        text: token.name,
        fontSize: Math.round(Math.min(16, Math.max(13, radius * 0.8))),
        fontFamily: 'system-ui, sans-serif',
        fill: this.colors.label,
        padding: 2,
      }),
    )
    label.offsetX(label.width() / 2)
    group.add(label)
    group.on('click tap', (e) => {
      e.cancelBubble = true
      this.events.onTokenClick(token.id)
    })
    group.on('mouseenter', () => (this.stage.container().style.cursor = 'pointer'))
    group.on('mouseleave', () => (this.stage.container().style.cursor = ''))
    this.layer.add(group)
  }

  /** Fantôme de la créature à poser ; contour rouge si la case est déjà prise. */
  private drawPreview(free: boolean): void {
    this.clearPreview()
    if (!this.state?.placing) return
    const { scene, placing } = this.state
    const target = this.pointerTarget(scene, free)
    if (!target) return
    const taken = !free && occupant(scene, target) !== undefined
    const ghost = this.tokenShape(placing, this.tokenRadius(scene), {
      x: this.toPixelsX(target.x),
      y: this.toPixelsY(target.y),
      opacity: taken ? 0.25 : 0.5,
      stroke: taken ? this.colors.danger : this.colors.selected,
      strokeWidth: 2,
      dash: taken ? [4, 3] : undefined,
    })
    this.preview.add(ghost)
    this.preview.batchDraw()
  }

  private clearPreview(): void {
    this.preview.destroyChildren()
    this.preview.batchDraw()
  }
}

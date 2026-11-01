import Konva from 'konva'
import type { SceneView } from '../types'

/** Ce que la carte signale ; les coordonnées sont dans l'unité de la carte. */
export interface SceneCanvasEvents {
  onBackgroundClick(x: number, y: number): void
  onTokenClick(tokenId: string): void
}

export interface SceneCanvasState {
  scene: SceneView
  selected: string[]
}

/** Taille d'une case : 1,5 m ou 5 ft, l'emprise d'une créature de taille moyenne. */
const CELL = { METRE: 1.5, FOOT: 5 } as const
const MARGIN = 16

/**
 * Rendu de la carte avec Konva, indépendant de Vue. Toute conversion entre pixels
 * et unité de la carte passe par {@link toPixels} et {@link toUnits}.
 */
export class SceneCanvas {
  private readonly stage: Konva.Stage
  private readonly layer = new Konva.Layer()
  private readonly events: SceneCanvasEvents
  private readonly colors: Record<string, string>
  private state: SceneCanvasState | null = null
  private pixelsPerUnit = 1

  constructor(container: HTMLDivElement, events: SceneCanvasEvents) {
    this.events = events
    this.stage = new Konva.Stage({ container, width: container.clientWidth, height: container.clientHeight })
    this.stage.add(this.layer)
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
    }
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

  private toPixels(value: number): number {
    return MARGIN + value * this.pixelsPerUnit
  }

  private toUnits(pixels: number): number {
    return (pixels - MARGIN) / this.pixelsPerUnit
  }

  private redraw(): void {
    this.layer.destroyChildren()
    if (!this.state) return
    const { scene, selected } = this.state
    const available = { w: this.stage.width() - 2 * MARGIN, h: this.stage.height() - 2 * MARGIN }
    this.pixelsPerUnit = Math.max(0.1, Math.min(available.w / scene.width, available.h / scene.height))

    this.drawGround(scene)
    this.drawMeasure(scene, selected)
    for (const token of scene.tokens) this.drawToken(scene, token, selected.includes(token.id))
    this.layer.draw()
  }

  private drawGround(scene: SceneView): void {
    const ground = new Konva.Rect({
      x: this.toPixels(0),
      y: this.toPixels(0),
      width: scene.width * this.pixelsPerUnit,
      height: scene.height * this.pixelsPerUnit,
      fill: this.colors.ground,
      stroke: this.colors.border,
      strokeWidth: 1,
    })
    ground.on('click tap', () => {
      const pointer = this.stage.getPointerPosition()
      if (!pointer) return
      const x = clamp(this.toUnits(pointer.x), 0, scene.width)
      const y = clamp(this.toUnits(pointer.y), 0, scene.height)
      this.events.onBackgroundClick(round(x), round(y))
    })
    this.layer.add(ground)

    const cell = CELL[scene.unit]
    const lines = new Konva.Group({ listening: false })
    for (let x = cell; x < scene.width; x += cell) {
      lines.add(this.gridLine([this.toPixels(x), this.toPixels(0), this.toPixels(x), this.toPixels(scene.height)]))
    }
    for (let y = cell; y < scene.height; y += cell) {
      lines.add(this.gridLine([this.toPixels(0), this.toPixels(y), this.toPixels(scene.width), this.toPixels(y)]))
    }
    this.layer.add(lines)
  }

  private gridLine(points: number[]): Konva.Line {
    return new Konva.Line({ points, stroke: this.colors.grid, strokeWidth: 1 })
  }

  private drawMeasure(scene: SceneView, selected: string[]): void {
    if (selected.length !== 2) return
    const [a, b] = selected.map((id) => scene.tokens.find((t) => t.id === id))
    if (!a || !b) return
    this.layer.add(
      new Konva.Line({
        points: [this.toPixels(a.x), this.toPixels(a.y), this.toPixels(b.x), this.toPixels(b.y)],
        stroke: this.colors.measure,
        strokeWidth: 2,
        dash: [8, 6],
        listening: false,
      }),
    )
  }

  private drawToken(scene: SceneView, token: SceneView['tokens'][number], isSelected: boolean): void {
    const radius = Math.max(8, (CELL[scene.unit] / 2) * this.pixelsPerUnit * 0.9)
    const group = new Konva.Group({ x: this.toPixels(token.x), y: this.toPixels(token.y) })
    group.add(
      new Konva.Circle({
        radius,
        fill: token.kind === 'CHARACTER' ? this.colors.character : this.colors.monster,
        stroke: isSelected ? this.colors.selected : this.colors.ground,
        strokeWidth: isSelected ? 3 : 1.5,
      }),
    )
    const label = new Konva.Label({ y: radius + 3, listening: false })
    label.add(new Konva.Tag({ fill: this.colors.ground, opacity: 0.85, cornerRadius: 3 }))
    label.add(
      new Konva.Text({
        text: token.name,
        fontSize: 12,
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
}

function clamp(value: number, min: number, max: number): number {
  return Math.min(max, Math.max(min, value))
}

/** Au dixième d'unité : assez précis pour une table, et lisible dans le journal. */
function round(value: number): number {
  return Math.round(value * 10) / 10
}

import Konva from 'konva'
import { imageUrl } from '../api'
import { formatLength, type CreatureKind, type SceneView, type TokenView } from '../types'
import { CELL, gridOf, magnet, occupant, round, snapToCell, type GridLines, type Point } from './grid'
import { checkMove, moveLabel, snapToLimit, type MoveLimit } from './move'
import { rangeVerdict, type RangeVerdict } from './range'
import { FIT_VIEW, layout, panBy, reveal, sameView, zoomAt, type Frame, type View } from './view'

/** Ce que la carte signale ; les coordonnées sont dans l'unité de la carte. */
export interface SceneCanvasEvents {
  /** `free` : la position n'est pas calée sur la case (Maj non tenue ; Maj cale sur la case). */
  onBackgroundClick(x: number, y: number, free: boolean): void
  onTokenClick(tokenId: string): void
  /** Un jeton glissé est relâché à cette position, libre ou calée (Maj) ; le jeton attend la réponse au départ. */
  onTokenMove(tokenId: string, x: number, y: number): void
  /** Pendant le calage, la carte est glissée de ce déplacement depuis le signal précédent, dans l'unité de la carte. */
  onGridDrag(dx: number, dy: number): void
  /** La vue a changé de zoom (1 : carte entière). */
  onZoom(zoom: number): void
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
  /** La créature dont c'est le tour en combat, ou `null`. */
  activeId: string | null
  /** Les jetons se glissent : faux en pose, en ciblage, pendant une décision ou un déplacement envoyé. */
  movable: boolean
  /** Le seul jeton limité dans son déplacement et ce qui lui reste, ou `null` : tout jeton se déplace librement. */
  movementLimit: MoveLimit | null
  /** Le jeton déplacé au clavier vers `cursor`, ou `null`. */
  moving: string | null
  /**
   * Pendant le calage, la grille d'aperçu (dans l'unité de la carte), dessinée même masquée ; un glisser sur la carte
   * la décale, et le fond ne pose rien. `null` hors calage.
   */
  gridPreview: GridLines | null
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
/** Jauge de PV : écart sous le jeton et épaisseur, en pixels. */
const HEALTH_GAP = 3
const HEALTH_HEIGHT = 4
/** La bulle d'issue reste lisible avant de s'effacer. */
const IMPACT_HOLD_MS = 1200
const REDUCED_MOTION = window.matchMedia('(prefers-reduced-motion: reduce)')
const FONT = "'Alegreya Sans', system-ui, sans-serif"
/** Un appui qui bouge de moins que ces pixels reste un clic ; au-delà, il déplace la vue. */
const PAN_THRESHOLD = 4
/** Pas de zoom des boutons et du clavier. */
const ZOOM_STEP = 1.25
/** La case du curseur clavier reste à cette distance du bord de la vue, en pixels. */
const REVEAL_PAD = 48

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
  /** Bulles d'issue posées sur la cible : sans écoute, et que le redessin ne vide pas ; elles s'effacent seules. */
  private readonly effects = new Konva.Layer({ listening: false })
  private readonly events: SceneCanvasEvents
  private readonly container: HTMLDivElement
  /** Couleurs du thème, relues à chaque dessin : le joueur peut changer de thème en cours de partie. */
  private colors: Record<string, string> = {}
  private state: SceneCanvasState | null = null
  /** L'image de fond chargée, gardée d'un dessin à l'autre ; `ready` faux tant qu'elle charge ou si elle a échoué. */
  private backdrop: { id: string; image: HTMLImageElement; ready: boolean } | null = null
  private pixelsPerUnit = 1
  private origin = { x: MARGIN, y: MARGIN }
  /** Zoom et point regardé ; `FIT_VIEW` montre la carte entière. */
  private view: View = FIT_VIEW
  /** Redessin demandé pour la prochaine image, après un changement de vue. */
  private frameRequest = 0
  /** Pincement à deux doigts en cours : écart et milieu précédents, en pixels du stage. */
  private pinch: { distance: number; x: number; y: number } | null = null
  private pointerInside = false
  /** Maj tenue : la pose et le glisser se calent sur la case. */
  private shiftHeld = false
  /**
   * Créature glissée depuis le catalogue et dernière position du pointeur, dans la fenêtre. Le pointeur est capturé
   * par le catalogue : Konva ne le voit pas, la carte vise donc depuis ces coordonnées.
   */
  private drag: { kind: CreatureKind; clientX: number; clientY: number } | null = null
  /** Dernière position du pointeur pendant un glisser de calage (bouton du milieu), en pixels du stage ; `null` hors glisser. */
  private gridDrag: { x: number; y: number } | null = null
  /**
   * Glisser du fond à la souris : dernière position du pointeur en pixels du stage, et `moved` une fois le seuil passé ;
   * `null` hors glisser.
   */
  private pan: { x: number; y: number; moved: boolean } | null = null
  /** Le dernier appui a déplacé la vue : le clic qui suit ne pose ni ne cible rien. */
  private panned = false
  /** Maj change l'aperçu sans attendre que la souris bouge : tenue, elle cale sur la case. */
  private readonly onShift = (e: KeyboardEvent) => {
    if (e.key !== 'Shift') return
    this.shiftHeld = e.type === 'keydown'
    if (this.drag) this.drawDrag()
    else if (this.pointerInside) this.drawPointerPreview(!this.shiftHeld)
  }

  constructor(container: HTMLDivElement, events: SceneCanvasEvents) {
    this.events = events
    this.container = container
    this.stage = new Konva.Stage({ container, width: container.clientWidth, height: container.clientHeight })
    this.stage.add(this.layer, this.preview, this.effects)
    this.stage.on('mousemove', (e) => {
      this.pointerInside = true
      this.drawPointerPreview(!e.evt.shiftKey)
    })
    this.stage.on('mouseleave', () => {
      this.pointerInside = false
      this.clearPreview()
    })
    window.addEventListener('keydown', this.onShift)
    window.addEventListener('keyup', this.onShift)
    // Bouton du milieu pendant le calage : décale la grille (sans le défilement automatique du navigateur).
    // Bouton gauche sur le fond (sol, image, marges ; pas un jeton qui écoute) : déplace la vue au-delà d'un seuil.
    // Au doigt, un seul doigt fait défiler la page : la vue se déplace à deux doigts.
    this.stage.on('mousedown', (e) => {
      const pointer = this.stage.getPointerPosition()
      this.panned = false
      if (!pointer) return
      if (e.evt.button === 1) {
        e.evt.preventDefault()
        if (this.state?.gridPreview) this.gridDrag = { ...pointer }
        return
      }
      const onBackground = e.target === this.stage || e.target.getParent() === this.layer
      if (e.evt.button === 0 && onBackground) this.pan = { ...pointer, moved: false }
    })
    this.stage.on('mousemove', () => {
      const pointer = this.stage.getPointerPosition()
      if (!pointer) return
      if (this.gridDrag && this.state?.gridPreview) {
        const [dx, dy] = [pointer.x - this.gridDrag.x, pointer.y - this.gridDrag.y]
        this.gridDrag = { ...pointer }
        if (dx || dy) this.events.onGridDrag(dx / this.pixelsPerUnit, dy / this.pixelsPerUnit)
        return
      }
      if (!this.pan) return
      const [dx, dy] = [pointer.x - this.pan.x, pointer.y - this.pan.y]
      if (!this.pan.moved && Math.hypot(dx, dy) < PAN_THRESHOLD) return
      if (!this.pan.moved) this.stage.container().style.cursor = 'grabbing'
      this.pan = { ...pointer, moved: true }
      this.panned = true
      const frame = this.frame()
      if (frame) this.setView(panBy(this.view, dx, dy, frame))
    })
    this.stage.on('mouseup mouseleave', () => {
      this.gridDrag = null
      if (this.pan?.moved) this.stage.container().style.cursor = ''
      this.pan = null
    })
    this.stage.on('wheel', (e) => {
      e.evt.preventDefault()
      const pointer = this.stage.getPointerPosition()
      if (Konva.isDragging() || !pointer) return
      // Pavé tactile : le pincement arrive en molette avec Ctrl, par petits pas.
      const speed = e.evt.ctrlKey ? 0.01 : e.evt.deltaMode === 1 ? 0.05 : 0.002
      this.zoomAround(Math.exp(-e.evt.deltaY * speed), pointer)
    })
    this.stage.on('touchstart touchmove', (e) => {
      const touches = e.evt.touches
      if (e.type === 'touchstart' && touches.length === 1) this.panned = false
      // Pendant le glisser d'un jeton, un redessin le détruirait et validerait un déplacement intermédiaire.
      if (touches.length !== 2 || Konva.isDragging()) return
      // Le toucher qui suit un pincement ne pose ni ne cible rien.
      this.panned = true
      e.evt.preventDefault()
      const box = this.container.getBoundingClientRect()
      const [a, b] = [touches[0], touches[1]]
      const now = {
        distance: Math.hypot(a.clientX - b.clientX, a.clientY - b.clientY),
        x: (a.clientX + b.clientX) / 2 - box.left,
        y: (a.clientY + b.clientY) / 2 - box.top,
      }
      const before = this.pinch
      this.pinch = now
      const frame = this.frame()
      if (!before || !frame) return
      const zoomed = before.distance > 0 ? zoomAt(this.view, now.distance / before.distance, now, frame) : this.view
      this.setView(panBy(zoomed, now.x - before.x, now.y - before.y, frame))
    })
    this.stage.on('touchend touchcancel', (e) => {
      if (e.evt.touches.length < 2) this.pinch = null
    })
  }

  /** Le cadre de la carte actuelle, ou `null` sans carte. */
  private frame(): Frame | null {
    if (!this.state) return null
    const { width, height } = this.state.scene
    return { stage: { width: this.stage.width(), height: this.stage.height() }, map: { width, height }, margin: MARGIN }
  }

  /** Change la vue et redessine à la prochaine image ; les bulles d'issue, posées en pixels, s'effacent. */
  private setView(view: View): void {
    if (sameView(view, this.view)) return
    const zoomChanged = view.zoom !== this.view.zoom
    this.view = view
    this.effects.destroyChildren()
    this.effects.batchDraw()
    if (zoomChanged) this.events.onZoom(view.zoom)
    if (this.frameRequest) return
    this.frameRequest = requestAnimationFrame(() => {
      this.frameRequest = 0
      this.redraw()
    })
  }

  private zoomAround(factor: number, at: { x: number; y: number }): void {
    const frame = this.frame()
    if (frame) this.setView(zoomAt(this.view, factor, at, frame))
  }

  private zoomAroundCenter(factor: number): void {
    if (Konva.isDragging()) return
    this.zoomAround(factor, { x: this.stage.width() / 2, y: this.stage.height() / 2 })
  }

  zoomIn(): void {
    this.zoomAroundCenter(ZOOM_STEP)
  }

  zoomOut(): void {
    this.zoomAroundCenter(1 / ZOOM_STEP)
  }

  /** La carte entière. */
  fit(): void {
    this.setView(FIT_VIEW)
  }

  /** Garde un point de la carte (son unité) en vue, sa case entière et une marge comprises. */
  reveal(point: Point): void {
    const frame = this.frame()
    if (!frame || !this.state) return
    const halfCell = (CELL[this.state.scene.unit] / 2) * layout(this.view, frame).pixelsPerUnit
    this.setView(reveal(this.view, point, halfCell + REVEAL_PAD, frame))
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
    cancelAnimationFrame(this.frameRequest)
    window.removeEventListener('keydown', this.onShift)
    window.removeEventListener('keyup', this.onShift)
    this.stage.destroy()
  }

  /** Une créature du catalogue est glissée au-dessus de la page : son fantôme suit le pointeur sur la carte. */
  dragOver(kind: CreatureKind, clientX: number, clientY: number): void {
    this.drag = { kind, clientX, clientY }
    this.drawDrag()
  }

  /** Position où la créature glissée serait posée, ou `null` hors de la carte : aimantée, ou calée si Maj est tenue. */
  dropTarget(clientX: number, clientY: number): Point | null {
    if (!this.state) return null
    const box = this.container.getBoundingClientRect()
    return this.targetAt(this.state.scene, { x: clientX - box.left, y: clientY - box.top }, !this.shiftHeld)
  }

  /** Fin du glisser, posé ou annulé : le fantôme s'efface. */
  endDrag(): void {
    this.drag = null
    this.clearPreview()
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
      hp: token('--hp'),
      hpLow: token('--hp-low'),
    }
  }

  private redraw(): void {
    this.readColors()
    this.layer.destroyChildren()
    this.clearPreview()
    if (!this.state) return
    const { scene, selected, placing } = this.state
    // Carte entière centrée au zoom 1 ; zoomée, le point regardé au centre, sans laisser entrer un bord.
    const placed = layout(this.view, this.frame()!)
    this.pixelsPerUnit = placed.pixelsPerUnit
    this.origin = placed.origin

    this.drawGround(scene)
    if (this.state.targeting) this.drawTargeting(scene, this.state.targeting)
    const measure = this.measureLabel(scene, selected)
    // Les noms passent au-dessus de tous les jetons ; ceux des jetons sélectionnés en dernier.
    const names = new Konva.Group({ listening: false })
    const ordered = [...scene.tokens].sort((a, b) => Number(selected.includes(a.id)) - Number(selected.includes(b.id)))
    const verdictOf = this.verdicts(scene, this.state.targeting)
    for (const token of ordered) {
      // Pendant le calage, les jetons n'écoutent pas : survol et glisser vont à la carte, qui décale la grille.
      const clickable = placing === null && !this.state.gridPreview
      this.drawToken(scene, token, selected.includes(token.id), token.creatureId === this.state.activeId, clickable, names, verdictOf(token))
    }
    this.layer.add(names)
    if (measure) this.layer.add(measure)
    this.layer.draw()
    if (this.state.cursor) {
      const moved = this.state.moving ? scene.tokens.find((t) => t.id === this.state!.moving) : undefined
      if (moved) this.drawMovePreview(moved, this.state.cursor, true)
      else if (this.state.placing) this.drawPreview(this.state.cursor, true, this.state.placing)
    }
    if (this.drag) this.drawDrag()
  }

  /** Position visée par le pointeur, ou `null` hors de la carte : calée sur la case, ou libre et aimantée au contact. */
  private pointerTarget(scene: SceneView, free: boolean): Point | null {
    const pointer = this.stage.getPointerPosition()
    return pointer ? this.targetAt(scene, pointer, free) : null
  }

  /** Position visée depuis un point du stage, en pixels ; `null` hors de la carte. */
  private targetAt(scene: SceneView, pixels: { x: number; y: number }, free: boolean): Point | null {
    const p = this.toUnits(pixels)
    if (p.x < 0 || p.y < 0 || p.x > scene.width || p.y > scene.height) return null
    return free ? magnet(scene, { x: round(p.x), y: round(p.y) }) : snapToCell(scene, p)
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
      if (this.state?.gridPreview || this.panned) return
      if ('button' in e.evt && e.evt.button !== 0) return
      const free = !('shiftKey' in e.evt && e.evt.shiftKey)
      const target = this.pointerTarget(scene, free)
      if (target) this.events.onBackgroundClick(target.x, target.y, free)
    })
    this.layer.add(ground)

    const backdrop = this.backdropOf(scene)
    if (backdrop) {
      this.layer.add(new Konva.Image({
        image: backdrop,
        x: this.toPixelsX(0),
        y: this.toPixelsY(0),
        width: scene.width * this.pixelsPerUnit,
        height: scene.height * this.pixelsPerUnit,
        listening: false,
      }))
      // Le bord du sol, en partie sous l'image, se redessine par-dessus.
      this.layer.add(new Konva.Rect({
        x: this.toPixelsX(0),
        y: this.toPixelsY(0),
        width: scene.width * this.pixelsPerUnit,
        height: scene.height * this.pixelsPerUnit,
        stroke: this.colors.border,
        strokeWidth: 1,
        listening: false,
      }))
    }
    const preview = this.state?.gridPreview ?? null
    if (!preview && !scene.gridVisible) return

    // Au demi-pixel près, pour des traits de 1 px nets et réguliers.
    const crisp = (pixels: number) => Math.round(pixels) + 0.5
    const { cell, offsetX, offsetY } = preview ?? gridOf(scene)
    // La première ligne de chaque axe est celle du décalage ; une ligne sur le bord se confond avec lui.
    const first = (offset: number) => (offset > 0 ? offset : cell)
    const lines = new Konva.Group({ listening: false })
    for (let x = first(offsetX); x < scene.width; x += cell) {
      const px = crisp(this.toPixelsX(x))
      lines.add(this.gridLine([px, this.toPixelsY(0), px, this.toPixelsY(scene.height)]))
    }
    for (let y = first(offsetY); y < scene.height; y += cell) {
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

  /**
   * L'image de fond prête à dessiner, ou `null` (carte sans image, image qui charge ou introuvable : le sol seul).
   * Elle n'est chargée qu'une fois ; la carte se redessine quand elle arrive.
   */
  private backdropOf(scene: SceneView): HTMLImageElement | null {
    if (!scene.imageId) return null
    if (this.backdrop?.id !== scene.imageId) {
      const image = new Image()
      const backdrop = { id: scene.imageId, image, ready: false }
      image.onload = () => {
        backdrop.ready = true
        if (this.backdrop === backdrop) this.redraw()
      }
      image.src = imageUrl(scene.imageId)
      this.backdrop = backdrop
    }
    return this.backdrop.ready ? this.backdrop.image : null
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
    isActive: boolean,
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
    // À son tour : un anneau laiton en tirets, plus large que l'anneau de cible et distinct du contour de sélection.
    if (isActive) {
      group.add(new Konva.Circle({ radius: radius * 1.55, stroke: this.colors.live, strokeWidth: 3, dash: [7, 5], listening: false }))
    }
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
      this.drawHealth(group, token, radius)
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
    // Le nom complet à la demande : sélection, survol, ou la créature qui joue. Les initiales suffisent au repos.
    // En ciblage, la cible envisagée porte aussi sa CA et ses PV : tout ce qui décide du coup est sous le curseur.
    const stats = hint && token.armorClass !== undefined ? ` · CA ${token.armorClass} · ${token.hitPoints}/${token.maxHitPoints} PV` : ''
    const full = this.nameLabel(token.name + stats, radius)
    full.visible(isSelected || isActive)
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
    const movable = !!this.state?.movable && clickable && !token.pending
    if (movable) this.makeDraggable(scene, token, group, full, isSelected || isActive)
    // En ciblage, le viseur n'apparaît que là où un clic lance l'attaque.
    const targeting = this.state?.targeting
    const cursor = !targeting ? (movable ? 'grab' : 'pointer') : token.id === targeting.attackerId || !reachable ? 'not-allowed' : 'crosshair'
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
      full.visible(isSelected || isActive)
      ring.visible(false)
      rangeLabel?.visible(false)
      this.layer.batchDraw()
    })
    this.layer.add(group)
  }

  /**
   * Le jeton suit le pointeur, librement ou calé sur la case (Maj), sans sortir de la carte ; l'aperçu dit la distance
   * parcourue et ce qui reste. Relâché, il revient au départ et signale la position d'arrivée : l'application la vérifie
   * et le serveur tranche.
   */
  private makeDraggable(scene: SceneView, token: TokenView, group: Konva.Group, name: Konva.Label, named: boolean): void {
    const start = { x: group.x(), y: group.y() }
    const target = (pixels: { x: number; y: number }): Point => {
      const p = this.toUnits(pixels)
      const inside = { x: Math.min(scene.width, Math.max(0, p.x)), y: Math.min(scene.height, Math.max(0, p.y)) }
      if (this.shiftHeld) return snapToCell(scene, inside)
      // Aimanté au contact, sans dépasser ce qui reste à parcourir ; sinon, aimanté au bord de ce qui reste.
      const limit = this.state?.movementLimit ?? null
      const free = { x: round(inside.x), y: round(inside.y) }
      const contact = magnet(scene, free, token.id, (p) => !checkMove(scene, token, p, limit).tooFar)
      return contact !== free ? contact : snapToLimit(scene, token, free, limit)
    }
    group.draggable(true)
    group.dragDistance(4)
    group.dragBoundFunc((pos) => {
      const t = target(pos)
      return { x: this.toPixelsX(t.x), y: this.toPixelsY(t.y) }
    })
    group.on('dragstart', () => {
      group.moveToTop()
      name.visible(false)
      this.stage.container().style.cursor = 'grabbing'
    })
    group.on('dragmove', () => this.drawMovePreview(token, target(group.position()), false))
    group.on('dragend', () => {
      const to = target(group.position())
      group.position(start)
      name.visible(named)
      this.clearPreview()
      this.stage.container().style.cursor = 'grab'
      this.layer.batchDraw()
      if (to.x !== token.x || to.y !== token.y) this.events.onTokenMove(token.id, to.x, to.y)
    })
  }

  /**
   * Aperçu d'un déplacement : le cercle de ce qui reste autour du départ (s'il y a une limite), le trait du départ à
   * l'arrivée et une bulle « parcouru · reste ». Interdit (trop loin, trop près) : trait et anneau `--danger`.
   * `ghost` dessine la créature à l'arrivée (clavier) ; au glisser, c'est le jeton lui-même.
   */
  private drawMovePreview(token: TokenView, to: Point, ghost: boolean): void {
    if (!this.state) return
    const { scene, movementLimit } = this.state
    this.preview.destroyChildren()
    const check = checkMove(scene, token, to, movementLimit)
    const color = check.allowed ? this.colors.live : this.colors.danger
    const radius = this.tokenRadius(scene)
    const from = { x: this.toPixelsX(token.x), y: this.toPixelsY(token.y) }
    const at = { x: this.toPixelsX(to.x), y: this.toPixelsY(to.y) }
    if (movementLimit?.tokenId === token.id) {
      const clipped = new Konva.Group({
        clip: { x: this.toPixelsX(0), y: this.toPixelsY(0), width: scene.width * this.pixelsPerUnit, height: scene.height * this.pixelsPerUnit },
      })
      clipped.add(new Konva.Circle({ ...from, radius: movementLimit.left * this.pixelsPerUnit, stroke: this.colors.live, strokeWidth: 2 }))
      this.preview.add(clipped)
    }
    this.preview.add(new Konva.Line({ points: [from.x, from.y, at.x, at.y], stroke: color, strokeWidth: 2, dash: [8, 6] }))
    if (ghost) this.preview.add(this.tokenShape(token.kind, radius, { ...at, opacity: 0.5, stroke: this.colors.live, strokeWidth: 2 }))
    if (!check.allowed) this.preview.add(new Konva.Circle({ ...at, radius: radius * 1.3, stroke: this.colors.danger, strokeWidth: 3 }))
    const label = new Konva.Label({ ...at, y: at.y - radius - 6 })
    label.add(
      new Konva.Tag({ fill: this.colors.labelGround, stroke: color, strokeWidth: 1, cornerRadius: 4, pointerDirection: 'down', pointerWidth: 10, pointerHeight: 5 }),
      new Konva.Text({ text: moveLabel(check, scene.unitSymbol), fontSize: 15, fontStyle: '700', fontFamily: FONT, fill: this.colors.label, padding: 4, wrap: 'none' }),
    )
    this.preview.add(label)
    this.preview.batchDraw()
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

  /** Jauge de PV sous le jeton, de sa largeur ; ambre à la moitié ou moins, comme le tableau. Rien tant que les PV sont inconnus. */
  private drawHealth(group: Konva.Group, token: TokenView, radius: number): void {
    if (token.hitPoints === undefined || !token.maxHitPoints) return
    const health = Math.max(0, Math.min(1, token.hitPoints / token.maxHitPoints))
    const bar = { x: -radius, y: radius + HEALTH_GAP, height: HEALTH_HEIGHT, cornerRadius: 2, listening: false }
    group.add(new Konva.Rect({ ...bar, width: 2 * radius, fill: 'rgba(0, 0, 0, 0.45)' }))
    group.add(new Konva.Rect({ ...bar, width: 2 * radius * health, fill: health <= 0.5 ? this.colors.hpLow : this.colors.hp }))
  }

  /**
   * L'issue d'une attaque au-dessus de sa cible (« −11 », « Raté ») : la bulle s'élève et s'efface seule, sans capter le
   * pointeur ; sans s'élever si le joueur demande moins d'animations.
   */
  showImpact(tokenId: string, text: string): void {
    const token = this.state?.scene.tokens.find((t) => t.id === tokenId)
    if (!token) return
    const radius = this.tokenRadius(this.state!.scene)
    const x = this.toPixelsX(token.x)
    const y = this.toPixelsY(token.y) - radius - 6
    const label = new Konva.Label({ x, y })
    label.add(
      new Konva.Tag({ fill: this.colors.labelGround, cornerRadius: 4, pointerDirection: 'down', pointerWidth: 10, pointerHeight: 5 }),
      new Konva.Text({ text, fontSize: 20, fontStyle: '700', fontFamily: FONT, fill: this.colors.label, padding: 5, wrap: 'none' }),
    )
    this.effects.add(label)
    this.effects.batchDraw()
    const still = REDUCED_MOTION.matches
    window.setTimeout(() => {
      new Konva.Tween({
        node: label,
        duration: 0.6,
        opacity: 0,
        ...(still ? {} : { y: y - 18 }),
        easing: Konva.Easings.EaseIn,
        onFinish: () => label.destroy(),
      }).play()
    }, IMPACT_HOLD_MS)
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
    // Sous la jauge de PV.
    const label = new Konva.Label({ y: radius + HEALTH_GAP + HEALTH_HEIGHT + 3, listening: false })
    label.add(new Konva.Tag({ fill: this.colors.labelGround, cornerRadius: 3 }), text)
    label.offsetX(label.width() / 2)
    return label
  }

  private drawPointerPreview(free: boolean): void {
    if (this.drag) return
    if (!this.state?.placing) return this.clearPreview()
    const target = this.pointerTarget(this.state.scene, free)
    if (!target) return this.clearPreview()
    this.drawPreview(target, false, this.state.placing)
  }

  private drawDrag(): void {
    if (!this.drag) return
    const target = this.dropTarget(this.drag.clientX, this.drag.clientY)
    if (!target) return this.clearPreview()
    this.drawPreview(target, false, this.drag.kind)
  }

  /**
   * Fantôme de la créature à poser. Sur une case prise, un anneau rouge plein, plus large
   * que le jeton, et un curseur d'interdiction. `framed` encadre la case visée au clavier.
   */
  private drawPreview(target: Point, framed: boolean, placing: CreatureKind): void {
    this.clearPreview()
    if (!this.state) return
    const { scene } = this.state
    const radius = this.tokenRadius(scene)
    const x = this.toPixelsX(target.x)
    const y = this.toPixelsY(target.y)
    const taken = occupant(scene, target) !== undefined
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
    // Au glisser depuis le catalogue, le pointeur est capturé : le curseur est celui du catalogue.
    if (!framed && !this.drag) this.stage.container().style.cursor = taken ? 'not-allowed' : ''
    this.preview.batchDraw()
  }

  private clearPreview(): void {
    if (this.state?.placing) this.stage.container().style.cursor = ''
    this.preview.destroyChildren()
    this.preview.batchDraw()
  }
}

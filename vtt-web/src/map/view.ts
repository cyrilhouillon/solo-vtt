/**
 * Vue de la carte : zoom et point regardé, sans Vue ni Konva. Le centre est en fraction de la carte (0 à 1), si bien que
 * la vue survit à un redimensionnement de la fenêtre et à une mise à l'échelle de la carte (calage de la grille).
 * Elle n'est qu'affichage : ni le serveur ni la partie ne la connaissent.
 */
export interface View {
  /** 1 : la carte entière dans le cadre ; jamais moins. */
  zoom: number
  cx: number
  cy: number
}

export const FIT_VIEW: View = { zoom: 1, cx: 0.5, cy: 0.5 }
export const MAX_ZOOM = 8

export interface Size {
  width: number
  height: number
}

/** Ce que la carte applique : pixels par unité de carte, et position en pixels du coin haut gauche de la carte. */
export interface Layout {
  pixelsPerUnit: number
  origin: { x: number; y: number }
}

/** Le cadre où la carte se dessine, sa marge et les dimensions de la carte dans son unité. */
export interface Frame {
  stage: Size
  map: Size
  margin: number
}

function fitScale({ stage, map, margin }: Frame): number {
  return Math.max(0.1, Math.min((stage.width - 2 * margin) / map.width, (stage.height - 2 * margin) / map.height))
}

const clamp = (value: number, min: number, max: number) => Math.min(max, Math.max(min, value))

/**
 * Origine sur un axe : centrée si la carte tient dans le cadre ; sinon le centre demandé, bornée pour que la carte
 * couvre le cadre (marges comprises) sans qu'un bord n'y entre.
 */
function axisOrigin(stage: number, length: number, center: number, margin: number): number {
  if (length <= stage - 2 * margin) return Math.round((stage - length) / 2)
  return Math.round(clamp(stage / 2 - center * length, stage - margin - length, margin))
}

export function layout(view: View, frame: Frame): Layout {
  const pixelsPerUnit = fitScale(frame) * view.zoom
  const { stage, map, margin } = frame
  return {
    pixelsPerUnit,
    origin: {
      x: axisOrigin(stage.width, map.width * pixelsPerUnit, view.cx, margin),
      y: axisOrigin(stage.height, map.height * pixelsPerUnit, view.cy, margin),
    },
  }
}

/** La vue dont le centre est celui qu'on voit vraiment, une fois les bornes appliquées. */
function settle(view: View, frame: Frame): View {
  const { pixelsPerUnit, origin } = layout(view, frame)
  const { stage, map } = frame
  return {
    zoom: view.zoom,
    cx: (stage.width / 2 - origin.x) / (map.width * pixelsPerUnit),
    cy: (stage.height / 2 - origin.y) / (map.height * pixelsPerUnit),
  }
}

/** Zoome du facteur donné, borné à [1, MAX_ZOOM], en gardant sous `at` (pixels du cadre) le même point de la carte. */
export function zoomAt(view: View, factor: number, at: { x: number; y: number }, frame: Frame): View {
  const before = layout(view, frame)
  const zoom = clamp(view.zoom * factor, 1, MAX_ZOOM)
  if (zoom === view.zoom) return view
  const pixelsPerUnit = fitScale(frame) * zoom
  const unit = { x: (at.x - before.origin.x) / before.pixelsPerUnit, y: (at.y - before.origin.y) / before.pixelsPerUnit }
  const origin = { x: at.x - unit.x * pixelsPerUnit, y: at.y - unit.y * pixelsPerUnit }
  const { stage, map } = frame
  return settle(
    {
      zoom,
      cx: (stage.width / 2 - origin.x) / (map.width * pixelsPerUnit),
      cy: (stage.height / 2 - origin.y) / (map.height * pixelsPerUnit),
    },
    frame,
  )
}

/** Fait glisser la carte de `dx`, `dy` pixels, bornée au cadre. */
export function panBy(view: View, dx: number, dy: number, frame: Frame): View {
  const { pixelsPerUnit } = layout(view, frame)
  const { map } = frame
  return settle({ ...view, cx: view.cx - dx / (map.width * pixelsPerUnit), cy: view.cy - dy / (map.height * pixelsPerUnit) }, frame)
}

/** Décale la vue juste assez pour que le point (unité de la carte) soit à `pad` pixels au moins du bord du cadre. */
export function reveal(view: View, point: { x: number; y: number }, pad: number, frame: Frame): View {
  const { pixelsPerUnit, origin } = layout(view, frame)
  const shift = (pixels: number, length: number) => {
    const room = Math.min(pad, length / 2)
    return pixels < room ? room - pixels : pixels > length - room ? length - room - pixels : 0
  }
  const dx = shift(origin.x + point.x * pixelsPerUnit, frame.stage.width)
  const dy = shift(origin.y + point.y * pixelsPerUnit, frame.stage.height)
  return dx || dy ? panBy(view, dx, dy, frame) : view
}

export function sameView(a: View, b: View): boolean {
  return a.zoom === b.zoom && Math.abs(a.cx - b.cx) < 1e-9 && Math.abs(a.cy - b.cy) < 1e-9
}

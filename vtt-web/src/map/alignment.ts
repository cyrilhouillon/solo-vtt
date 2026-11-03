import type { DistanceUnit, SceneView } from '../types'
import { mapFromImage } from './backdrop'
import { CELL, type GridLines } from './grid'

/** Le calage d'une grille sur l'image, en pixels de l'image : décalage de la première ligne et taille d'une case. */
export interface Alignment {
  offsetX: number
  offsetY: number
  cellPixels: number
}

/** Ce que le serveur reçoit : dimensions de la carte et décalage de la grille, en fraction de case dans [0, 1). */
export interface AlignedMap {
  width: number
  height: number
  gridOffsetX: number
  gridOffsetY: number
}

/** Fraction de case d'un décalage en pixels, ramenée dans [0, 1) : un décalage d'une case ou plus revient au même. */
function fraction(offset: number, cellPixels: number): number {
  const f = (((offset / cellPixels) % 1) + 1) % 1
  return f >= 1 ? 0 : f
}

/**
 * Le calage actuel de la carte, en pixels de son image (de largeur naturelle `imageWidth`). Le décalage se règle au pixel
 * entier : ramené dans une case de taille fractionnaire (40 px dans 32,5 px donnent 7,5 px), il est arrondi au pixel.
 */
export function alignmentOf(scene: SceneView, imageWidth: number): Alignment {
  const cellPixels = imageWidth / (scene.width / CELL[scene.unit])
  return { offsetX: Math.round(scene.gridOffsetX * cellPixels), offsetY: Math.round(scene.gridOffsetY * cellPixels), cellPixels }
}

/** La carte que donne ce calage : dimensions tirées de l'image, décalage en fraction de case. */
export function alignedMap(imageWidth: number, imageHeight: number, unit: DistanceUnit, alignment: Alignment): AlignedMap {
  const { width, height } = mapFromImage(imageWidth, imageHeight, alignment.cellPixels, unit)
  return {
    width,
    height,
    gridOffsetX: fraction(alignment.offsetX, alignment.cellPixels),
    gridOffsetY: fraction(alignment.offsetY, alignment.cellPixels),
  }
}

/** La grille d'aperçu de ce calage, dans l'unité de la carte affichée (avant validation). */
export function previewGrid(scene: SceneView, imageWidth: number, alignment: Alignment): GridLines {
  const unitsPerPixel = scene.width / imageWidth
  const cell = alignment.cellPixels * unitsPerPixel
  return {
    cell,
    offsetX: fraction(alignment.offsetX, alignment.cellPixels) * cell,
    offsetY: fraction(alignment.offsetY, alignment.cellPixels) * cell,
  }
}

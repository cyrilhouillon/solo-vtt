import type { DistanceUnit } from '../types'
import { CELL } from './grid'

/** Une carte tirée d'une image : ses cases et ses dimensions dans l'unité de la carte. */
export interface MapFromImage {
  cellsAcross: number
  cellsDown: number
  width: number
  height: number
}

/**
 * Les dimensions d'une carte dont la grille tombe sur celle de l'image : chaque case de l'image fait
 * `cellPixels` pixels et vaut une case de la carte (1,5 m ou 5 ft). 975 × 975 px à 32,5 px donnent 30 × 30 cases.
 */
export function mapFromImage(pixelWidth: number, pixelHeight: number, cellPixels: number, unit: DistanceUnit): MapFromImage {
  const cellsAcross = pixelWidth / cellPixels
  const cellsDown = pixelHeight / cellPixels
  return { cellsAcross, cellsDown, width: cellsAcross * CELL[unit], height: cellsDown * CELL[unit] }
}

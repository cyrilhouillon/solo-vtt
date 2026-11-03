/** Ce que perd la carte actuelle quand une autre la remplace : « Remplace la carte actuelle{describeLoss(n)}. » */
export function describeLoss(count: number): string {
  if (count === 0) return ' et son journal'
  return count === 1 ? ', sa créature et son journal' : `, ses ${count} créatures et son journal`
}

/** Les types d'image que le serveur accepte (`POST /api/images`), pour une carte comme pour une page du Carnet. */
export const IMAGE_TYPES = ['image/png', 'image/jpeg', 'image/webp']

/**
 * La limite du serveur (`quarkus.http.limits.max-body-size`). Au-delà, il coupe la connexion avant la fin de l'envoi :
 * le navigateur ne voit qu'une erreur réseau, d'où ce refus avant d'envoyer.
 */
export const MAX_IMAGE_BYTES = 50 * 1024 * 1024

/** La raison de refuser ce fichier avant de l'envoyer, ou `null` s'il peut partir. */
export function imageRefusal(file: File): string | null {
  if (!IMAGE_TYPES.includes(file.type)) return 'Choisir une image PNG, JPEG ou WebP.'
  if (file.size > MAX_IMAGE_BYTES) return 'Image trop lourde (50 Mo au plus).'
  return null
}

<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { mapFromImage } from '../map/backdrop'
import { CELL } from '../map/grid'
import { describeLoss } from '../replaceWarning'
import type { DistanceUnit } from '../types'

/** `tokenCount` : nombre de créatures de la carte actuelle, `null` s'il n'y a pas encore de carte. */
const props = defineProps<{ tokenCount: number | null }>()
/** `image` : l'image de fond choisie, à envoyer au serveur, ou `null` pour une carte vide. */
const emit = defineEmits<{ create: [width: number, height: number, unit: DistanceUnit, image: File | null]; cancel: [] }>()

const width = ref(30)
const height = ref(20)
const unit = ref<DistanceUnit>('METRE')
const firstField = ref<HTMLInputElement | null>(null)
const form = ref<HTMLFormElement | null>(null)

/** Dernières dimensions saisies, et leur unité : la référence des conversions. */
let typed = { unit: unit.value, width: width.value, height: height.value }
// Après la mise à jour de v-model, qui écoute le même événement.
function remember() {
  nextTick(() => (typed = { unit: unit.value, width: width.value, height: height.value }))
}

// Changer d'unité garde le même nombre de cases (30 × 20 m donne 100 × 65 ft) ;
// revenir à l'unité saisie rend les valeurs saisies, sans arrondi cumulé.
watch(unit, (to) => {
  const source = typed
  const convert = (value: number) => Math.max(1, Math.round(value / CELL[source.unit])) * CELL[to]
  const [w, h] = to === source.unit ? [source.width, source.height] : [convert(source.width), convert(source.height)]
  width.value = w
  height.value = h
})

// Ouvert depuis le rail sur une carte existante : le focus va au premier champ.
onMounted(() => {
  if (props.tokenCount !== null) firstField.value?.focus()
})

const IMAGE_TYPES = ['image/png', 'image/jpeg', 'image/webp']
/**
 * La limite du serveur (`quarkus.http.limits.max-body-size`). Au-delà, il coupe la connexion avant la fin de l'envoi :
 * le navigateur ne voit qu'une erreur réseau, d'où ce refus avant d'envoyer.
 */
const MAX_IMAGE_BYTES = 50 * 1024 * 1024

/** L'image choisie et sa taille en pixels, lue par le navigateur. */
const image = ref<{ file: File; width: number; height: number } | null>(null)
const imageError = ref<string | null>(null)
const fileInput = ref<HTMLInputElement | null>(null)
/**
 * Cases sur la largeur de l'image et taille d'une case en pixels : saisir l'une calcule l'autre.
 * Le champ en cours de saisie n'est jamais réécrit (une saisie comme « 32, » n'est pas encore un nombre).
 */
const cellsAcross = ref<number | ''>('')
const cellPixels = ref<number | ''>('')
/** Le champ saisi fait foi : l'autre n'est qu'un affichage arrondi (1000 px sur 30 cases : 33,33 px affichés). */
const typedField = ref<'cells' | 'pixels'>('cells')

const fromImage = computed(() => {
  if (!image.value) return null
  const typed = typedField.value === 'cells' ? cellsAcross.value : cellPixels.value
  if (typeof typed !== 'number' || !(typed > 0)) return null
  const pixels = typedField.value === 'cells' ? image.value.width / typed : typed
  return mapFromImage(image.value.width, image.value.height, pixels, unit.value)
})

function readSize(file: File): Promise<{ width: number; height: number }> {
  const url = URL.createObjectURL(file)
  const img = new Image()
  return new Promise<{ width: number; height: number }>((resolve, reject) => {
    img.onload = () => resolve({ width: img.naturalWidth, height: img.naturalHeight })
    img.onerror = () => reject(new Error('illisible'))
    img.src = url
  }).finally(() => URL.revokeObjectURL(url))
}

async function chooseImage(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  imageError.value = null
  if (!file) return
  if (!IMAGE_TYPES.includes(file.type)) {
    imageError.value = 'Choisir une image PNG, JPEG ou WebP.'
    removeImage(false)
    return
  }
  if (file.size > MAX_IMAGE_BYTES) {
    imageError.value = 'Image trop lourde (50 Mo au plus).'
    removeImage(false)
    return
  }
  try {
    const size = await readSize(file)
    image.value = { file, ...size }
    // Les cases saisies avant restent : la taille de case suit la nouvelle image.
    if (typedField.value === 'cells') typeCells()
    else typeCellPixels()
  } catch {
    imageError.value = 'Image illisible.'
    removeImage(false)
  }
}

function removeImage(clearError = true) {
  image.value = null
  if (clearError) imageError.value = null
  if (fileInput.value) fileInput.value.value = ''
}

/** Au centième près pour l'affichage d'un champ calculé. */
const rounded = (value: number) => Math.round(value * 100) / 100
const decimal = new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 2 })

// Après la mise à jour de v-model, qui écoute le même événement.
function typeCells() {
  typedField.value = 'cells'
  nextTick(() => {
    const cells = cellsAcross.value
    cellPixels.value = image.value && typeof cells === 'number' && cells > 0 ? rounded(image.value.width / cells) : ''
  })
}

function typeCellPixels() {
  typedField.value = 'pixels'
  nextTick(() => {
    const pixels = cellPixels.value
    cellsAcross.value = image.value && typeof pixels === 'number' && pixels > 0 ? rounded(image.value.width / pixels) : ''
  })
}

function submit() {
  if (image.value) {
    if (!fromImage.value) return
    emit('create', fromImage.value.width, fromImage.value.height, unit.value, image.value.file)
    return
  }
  emit('create', width.value, height.value, unit.value, null)
}

/**
 * Remplacer une carte peuplée demande un clic explicite : ce bouton n'est pas de type submit,
 * donc Entrée dans un champ ne déclenche rien.
 */
function replace() {
  if (form.value?.reportValidity()) submit()
}

const unitSymbol = (u: DistanceUnit) => (u === 'METRE' ? 'm' : 'ft')
</script>

<template>
  <form ref="form" class="new-map" aria-labelledby="new-map-title" @submit.prevent="submit" @keydown.esc.stop="emit('cancel')">
    <h2 id="new-map-title" class="panel-title">Nouvelle carte</h2>
    <hr class="tapered" />
    <fieldset class="unit">
      <legend>Unité</legend>
      <label><input v-model="unit" type="radio" value="METRE" /> mètres</label>
      <label><input v-model="unit" type="radio" value="FOOT" /> pieds</label>
    </fieldset>
    <div class="image">
      <label>
        Image de fond (facultative)
        <input ref="fileInput" type="file" accept="image/png,image/jpeg,image/webp" @change="chooseImage" />
      </label>
      <p v-if="image" class="image-info">{{ image.file.name }} · {{ image.width }} × {{ image.height }} px</p>
      <p v-if="imageError" class="warning" role="alert">{{ imageError }}</p>
      <button v-if="image" type="button" @click="removeImage()">Retirer l'image</button>
    </div>
    <template v-if="image">
      <div class="size">
        <label>
          Cases en largeur
          <input v-model.number="cellsAcross" @input="typeCells" type="number" min="0.01" step="any" required />
        </label>
        <label>
          Case
          <span class="field">
            <input v-model.number="cellPixels" @input="typeCellPixels" type="number" min="1" step="any" required />
            px
          </span>
        </label>
      </div>
      <p v-if="fromImage" class="image-info" aria-live="polite">
        {{ decimal.format(fromImage.cellsAcross) }} × {{ decimal.format(fromImage.cellsDown) }} cases ·
        {{ decimal.format(fromImage.width) }} × {{ decimal.format(fromImage.height) }} {{ unitSymbol(unit) }}
      </p>
    </template>
    <div v-else class="size">
      <label>
        Largeur
        <span class="field">
          <input ref="firstField" v-model.number="width" @input="remember" type="number" min="1" step="any" required />
          {{ unitSymbol(unit) }}
        </span>
      </label>
      <label>
        Hauteur
        <span class="field">
          <input v-model.number="height" @input="remember" type="number" min="1" step="any" required />
          {{ unitSymbol(unit) }}
        </span>
      </label>
    </div>
    <template v-if="tokenCount !== null">
      <p class="warning" role="note">Remplace la carte actuelle{{ describeLoss(tokenCount) }}.</p>
      <div class="actions">
        <button v-if="tokenCount > 0" type="button" class="danger" @click="replace">Remplacer la carte</button>
        <button v-else type="submit" class="danger">Remplacer la carte</button>
        <button type="button" @click="emit('cancel')">Annuler</button>
      </div>
    </template>
    <div v-else class="actions">
      <button type="submit" class="primary">Créer la carte</button>
    </div>
  </form>
</template>

<style scoped>
.new-map {
  display: flex;
  flex-direction: column;
  gap: 0.9rem;
}
.new-map .tapered {
  margin: -0.5rem 0 0;
}
fieldset {
  border: 0;
  padding: 0;
  margin: 0;
  display: flex;
  gap: 1rem;
}
legend {
  float: left;
  margin-right: 0.25rem;
  font-family: var(--font-title);
  color: var(--ink-muted);
}
.size {
  display: flex;
  gap: 1rem;
}
label {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  color: var(--ink-muted);
}
.unit label {
  flex-direction: row;
  align-items: center;
  gap: 0.35rem;
  color: var(--ink);
}
.field {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  color: var(--ink);
}
input[type='number'] {
  width: 5rem;
}
.image {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 0.4rem;
}
/* Le libellé prend la largeur du tiroir : sans cela, « Aucun fichier choisi » le fait déborder. */
.image label {
  align-self: stretch;
  min-width: 0;
}
.image input[type='file'] {
  max-width: 100%;
  color: var(--ink);
}
/* Le bouton natif prend l'allure des autres boutons. */
.image input[type='file']::file-selector-button {
  margin-right: 0.5rem;
  padding: 0.35rem 0.8rem;
  border: 1px solid var(--control-border);
  border-radius: 4px;
  background: var(--wood-sunken);
  color: var(--ink);
  font: inherit;
  font-weight: 500;
  cursor: pointer;
}
.image input[type='file']:hover::file-selector-button {
  background: var(--wood-raised);
  border-color: var(--ink-muted);
}
.image-info {
  margin: 0;
  color: var(--ink);
  overflow-wrap: anywhere;
}
.warning {
  margin: 0;
  color: var(--danger);
}
.actions {
  display: flex;
  gap: 0.5rem;
}
.danger {
  border-color: var(--danger);
  color: var(--danger);
}
.primary {
  background: var(--wood-raised);
  border-color: var(--ink-muted);
  font-weight: 700;
}
</style>

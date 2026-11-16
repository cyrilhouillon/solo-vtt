<script setup lang="ts">
import { computed, ref } from 'vue'
import { numberOf, type EquipmentChoice, type EquipmentOptions, type Inventory, type Item } from './sheet'

/**
 * L'inventaire d'une fiche : un objet par ligne (nom, quantité, porté ou tenu, retirer), l'ajout d'une arme ou d'une
 * armure du SRD ou d'un objet en texte, et les pièces d'or. Chaque geste émet l'inventaire entier, que la fiche envoie
 * aussitôt : le serveur en tire la CA et les attaques. Quantités, noms et po partent au `change`, pas à la frappe.
 */
const props = defineProps<{ inventory: Inventory; options: EquipmentOptions | null }>()
const emit = defineEmits<{ change: [inventory: Inventory] }>()

/** L'objet choisi à ajouter : `weapon:ID`, `armor:ID`, ou vide pour un objet en texte. */
const choice = ref('')
const newName = ref('')

const weaponLabels = computed(() => new Map((props.options?.weapons ?? []).map((w) => [w.id, w.label])))
const armorLabels = computed(() => new Map((props.options?.armors ?? []).map((a) => [a.id, a.label])))

/** Les choix groupés par catégorie, dans l'ordre du serveur. */
function grouped(choices: EquipmentChoice[]): [string, EquipmentChoice[]][] {
  const groups = new Map<string, EquipmentChoice[]>()
  for (const c of choices) groups.set(c.category, [...(groups.get(c.category) ?? []), c])
  return [...groups]
}

const weaponGroups = computed(() => grouped(props.options?.weapons ?? []))
const armorGroups = computed(() => grouped(props.options?.armors ?? []))

function label(item: Item): string {
  if (item.weapon) return weaponLabels.value.get(item.weapon) ?? item.weapon
  if (item.armor) return armorLabels.value.get(item.armor) ?? item.armor
  return item.name ?? ''
}

/** « Portée » pour une armure, « Tenu » pour un bouclier, « Tenue » pour une arme. */
function equipLabel(item: Item): string {
  if (item.armor === 'SHIELD') return 'Tenu'
  return item.armor ? 'Portée' : 'Tenue'
}

function send(items: Item[], gold = props.inventory.gold) {
  emit('change', { items, gold })
}

function update(index: number, patch: Partial<Item>) {
  send(props.inventory.items.map((item, i) => (i === index ? { ...item, ...patch } : item)))
}

function remove(index: number) {
  send(props.inventory.items.filter((_, i) => i !== index))
}

const canAdd = computed(() => (choice.value ? true : newName.value.trim() !== ''))

function add() {
  if (!canAdd.value) return
  const [kind, id] = choice.value.split(':')
  const item: Item = choice.value
    ? { weapon: kind === 'weapon' ? id : null, armor: kind === 'armor' ? id : null, name: null, quantity: 1, equipped: false }
    : { weapon: null, armor: null, name: newName.value.trim(), quantity: 1, equipped: false }
  send([...props.inventory.items, item])
  choice.value = ''
  newName.value = ''
}
</script>

<template>
  <section class="inventory" aria-labelledby="inventory-title">
    <h3 id="inventory-title">Inventaire</h3>
    <table v-if="inventory.items.length">
      <thead>
        <tr>
          <th scope="col">Objet</th>
          <th scope="col">Qté</th>
          <th scope="col">Équipé</th>
          <th scope="col"><span class="visually-hidden">Retirer</span></th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(item, index) in inventory.items" :key="index">
          <td>
            <input
              v-if="item.name !== null"
              :value="item.name"
              type="text"
              maxlength="80"
              :aria-label="`Nom de l'objet ${index + 1}`"
              @change="update(index, { name: ($event.target as HTMLInputElement).value })"
            />
            <template v-else>
              {{ label(item) }} <span v-if="item.armor && item.armor !== 'SHIELD'" class="muted">(armure)</span>
            </template>
          </td>
          <td>
            <input
              :value="item.quantity"
              type="number"
              min="1"
              class="small"
              :aria-label="`Quantité de ${label(item)}`"
              @change="update(index, { quantity: numberOf(($event.target as HTMLInputElement).value) })"
            />
          </td>
          <td>
            <label v-if="item.name === null" class="check">
              <input
                type="checkbox"
                :checked="item.equipped"
                :aria-label="`${label(item)} : ${equipLabel(item).toLowerCase()}`"
                @change="update(index, { equipped: ($event.target as HTMLInputElement).checked })"
              />
              {{ equipLabel(item) }}
            </label>
          </td>
          <td>
            <button type="button" class="ghost" :aria-label="`Retirer ${label(item)}`" @click="remove(index)">×</button>
          </td>
        </tr>
      </tbody>
    </table>
    <p v-else class="muted">Aucun objet.</p>

    <div class="add">
      <select v-model="choice" aria-label="Arme ou armure à ajouter">
        <option value="">Objet en texte…</option>
        <optgroup v-for="[category, choices] in weaponGroups" :key="category" :label="category">
          <option v-for="w in choices" :key="w.id" :value="`weapon:${w.id}`">{{ w.label }}</option>
        </optgroup>
        <optgroup v-for="[category, choices] in armorGroups" :key="category" :label="category">
          <option v-for="a in choices" :key="a.id" :value="`armor:${a.id}`">{{ a.label }}</option>
        </optgroup>
      </select>
      <input
        v-if="!choice"
        v-model="newName"
        type="text"
        maxlength="80"
        placeholder="Corde de chanvre"
        aria-label="Nom de l'objet à ajouter"
        @keydown.enter.prevent="add"
      />
      <button type="button" :disabled="!canAdd" @click="add">Ajouter</button>
    </div>

    <label class="gold">
      Pièces d'or
      <input
        :value="inventory.gold"
        type="number"
        min="0"
        class="small"
        @change="send(inventory.items, numberOf(($event.target as HTMLInputElement).value))"
      />
      po
    </label>
  </section>
</template>

<style scoped>
h3 {
  margin: 0 0 0.6rem;
  font-family: var(--font-title);
  font-size: 1.15rem;
}
table {
  width: 100%;
  border-collapse: collapse;
  margin-bottom: 0.5rem;
}
th {
  font-size: 0.8rem;
  font-weight: 600;
  text-align: left;
  color: var(--ink-muted);
}
td,
th {
  padding: 0.15rem 0.25rem 0.15rem 0;
}
input,
select {
  font: inherit;
}
input[type='text'] {
  width: 100%;
  min-width: 6rem;
}
input.small {
  width: 4rem;
}
.check {
  display: flex;
  align-items: center;
  gap: 0.3rem;
  font-size: 0.9rem;
  white-space: nowrap;
}
.muted {
  color: var(--ink-muted);
}
.add {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem;
  align-items: center;
  margin-bottom: 0.6rem;
}
.add input {
  flex: 1;
  width: auto;
}
.gold {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.9rem;
}
.visually-hidden {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip-path: inset(50%);
  white-space: nowrap;
}
</style>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { dndApi } from './api'
import {
  ABILITIES,
  ABILITY_LABELS,
  SKILLS,
  allSlots,
  modifier,
  passivePerception,
  savingThrow,
  signed,
  skillBonus,
  toRequest,
  type Ability,
  type Proficiency,
  type SheetValues,
  type Skill,
} from './sheet'

/**
 * La fiche d'un personnage joueur, montrée à la place du texte de sa page. Toute valeur se saisit sur place ; les dérivés
 * (modificateurs, sauvegardes, compétences, Perception passive) suivent la frappe. La fiche s'enregistre seule, une
 * seconde après la dernière frappe, un envoi à la fois ; un refus du serveur s'affiche sur la fiche, la saisie reste à
 * l'écran et repart au changement suivant.
 */
const props = defineProps<{ campaignId: string; pageId: string }>()
const emit = defineEmits<{ error: [message: string] }>()

const SAVE_DELAY_MS = 1000
const api = dndApi(props.campaignId)

const values = ref<SheetValues | null>(null)
/** La raison du dernier refus du serveur, `null` si la fiche est enregistrée ; `'saving'` pendant l'envoi. */
const status = ref<string | 'saving' | 'saved' | null>(null)
let timer: ReturnType<typeof setTimeout> | undefined
let saving: Promise<void> = Promise.resolve()
/** Vrai tant que la saisie à l'écran n'est pas partie. */
let pending = false

const reason = (e: unknown) => (e instanceof Error ? e.message : String(e))

onMounted(async () => {
  try {
    const sheet = await api.read(props.pageId)
    values.value = { ...sheet.values, spellSlots: allSlots(sheet.values.spellSlots) }
  } catch (e) {
    emit('error', `Fiche illisible : ${reason(e)}`)
  }
})

watch(
  values,
  (_, before) => {
    // La première lecture n'est pas une frappe.
    if (!before) return
    pending = true
    clearTimeout(timer)
    timer = setTimeout(() => void save(), SAVE_DELAY_MS)
  },
  { deep: true },
)

/** Envoie la saisie à l'écran, après l'envoi en cours. */
function save(): Promise<void> {
  saving = saving.then(async () => {
    if (!pending || !values.value) return
    pending = false
    const sent = toRequest(JSON.parse(JSON.stringify(values.value)) as SheetValues)
    status.value = 'saving'
    try {
      await api.save(props.pageId, sent)
      status.value = pending ? 'saving' : 'saved'
    } catch (e) {
      status.value = reason(e)
    }
  })
  return saving
}

onBeforeUnmount(() => {
  clearTimeout(timer)
  void save()
})

function proficiency(skill: Skill): Proficiency {
  return values.value?.skills[skill] ?? 'NONE'
}

function setProficiency(skill: Skill, value: Proficiency) {
  if (!values.value) return
  values.value.skills = { ...values.value.skills, [skill]: value }
}

function toggleSave(ability: Ability, checked: boolean) {
  if (!values.value) return
  const others = values.value.savingThrows.filter((a) => a !== ability)
  values.value.savingThrows = checked ? [...others, ability] : others
}

function addAttack() {
  values.value?.attacks.push({ name: '', bonus: 0, damage: '', damageType: '', range: '' })
}

function removeAttack(index: number) {
  values.value?.attacks.splice(index, 1)
}
</script>

<template>
  <div v-if="values" class="character-sheet">
    <p class="save-state" :class="{ refused: status && status !== 'saving' && status !== 'saved' }" role="status">
      <template v-if="status === 'saving'">Enregistrement…</template>
      <template v-else-if="status === 'saved'">Enregistrée</template>
      <template v-else-if="status">Non enregistrée : {{ status }}</template>
    </p>

    <section class="identity" aria-label="Identité">
      <label>Classe <input v-model="values.className" type="text" maxlength="80" /></label>
      <label class="narrow">Niveau <input v-model.number="values.level" type="number" min="1" max="20" /></label>
      <label>Espèce <input v-model="values.species" type="text" maxlength="80" /></label>
      <label>Historique <input v-model="values.background" type="text" maxlength="80" /></label>
    </section>

    <div class="columns">
      <section class="abilities" aria-labelledby="abilities-title">
        <h3 id="abilities-title">Caractéristiques</h3>
        <ul class="ability-list">
          <li v-for="ability in ABILITIES" :key="ability" class="ability">
            <label :title="ABILITY_LABELS[ability].label">
              <span class="short">{{ ABILITY_LABELS[ability].short }}</span>
              <input v-model.number="values.abilities[ability]" type="number" min="1" max="30" :aria-label="ABILITY_LABELS[ability].label" />
            </label>
            <output class="mod">{{ signed(modifier(values.abilities[ability])) }}</output>
          </li>
        </ul>
        <label class="inline">Bonus de maîtrise <input v-model.number="values.proficiencyBonus" type="number" min="0" max="10" /></label>

        <h4>Jets de sauvegarde</h4>
        <ul class="checks">
          <li v-for="ability in ABILITIES" :key="ability">
            <label>
              <input
                type="checkbox"
                :checked="values.savingThrows.includes(ability)"
                @change="toggleSave(ability, ($event.target as HTMLInputElement).checked)"
              />
              {{ ABILITY_LABELS[ability].label }}
            </label>
            <output>{{ signed(savingThrow(values, ability)) }}</output>
          </li>
        </ul>

        <h4>Compétences</h4>
        <ul class="checks">
          <li v-for="skill in SKILLS" :key="skill.id">
            <label>
              <select
                :value="proficiency(skill.id)"
                :aria-label="`Maîtrise de ${skill.label}`"
                @change="setProficiency(skill.id, ($event.target as HTMLSelectElement).value as Proficiency)"
              >
                <option value="NONE">—</option>
                <option value="PROFICIENT">M</option>
                <option value="EXPERTISE">E</option>
              </select>
              {{ skill.label }} <span class="muted">({{ ABILITY_LABELS[skill.ability].short }})</span>
            </label>
            <output>{{ signed(skillBonus(values, skill.id)) }}</output>
          </li>
        </ul>
        <p class="passive">Perception passive <output>{{ passivePerception(values) }}</output></p>
      </section>

      <div class="right">
        <section class="combat" aria-labelledby="combat-title">
          <h3 id="combat-title">Combat</h3>
          <div class="stats">
            <label>CA <input v-model.number="values.armorClass" type="number" min="0" /></label>
            <label>Initiative <input v-model.number="values.initiative" type="number" /></label>
            <label>Vitesse (m) <input v-model.number="values.speed" type="number" min="0" step="0.5" /></label>
            <label>PV actuels <input v-model.number="values.hitPoints" type="number" min="0" /></label>
            <label>PV maximum <input v-model.number="values.hitPointMax" type="number" min="0" /></label>
            <label>Dés de vie <input v-model="values.hitDice" type="text" maxlength="40" placeholder="1d10" /></label>
          </div>

          <h4>Attaques</h4>
          <div v-if="values.attacks.length" class="attacks">
          <table>
            <thead>
              <tr>
                <th scope="col">Nom</th>
                <th scope="col">Bonus</th>
                <th scope="col">Dégâts</th>
                <th scope="col">Type</th>
                <th scope="col">Portée</th>
                <th scope="col"><span class="visually-hidden">Retirer</span></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(attack, index) in values.attacks" :key="index">
                <td><input v-model="attack.name" type="text" class="name" maxlength="80" :aria-label="`Nom de l'attaque ${index + 1}`" /></td>
                <td><input v-model.number="attack.bonus" type="number" class="small" :aria-label="`Bonus de l'attaque ${index + 1}`" /></td>
                <td><input v-model="attack.damage" type="text" maxlength="40" :aria-label="`Dégâts de l'attaque ${index + 1}`" /></td>
                <td><input v-model="attack.damageType" type="text" maxlength="40" :aria-label="`Type de dégâts de l'attaque ${index + 1}`" /></td>
                <td><input v-model="attack.range" type="text" maxlength="40" :aria-label="`Portée de l'attaque ${index + 1}`" /></td>
                <td>
                  <button type="button" class="ghost" :aria-label="`Retirer l'attaque ${index + 1}`" @click="removeAttack(index)">×</button>
                </td>
              </tr>
            </tbody>
          </table>
          </div>
          <button type="button" @click="addAttack">Ajouter une attaque</button>

          <h4>Emplacements de sort</h4>
          <table class="slots">
            <thead>
              <tr>
                <th scope="col">Niveau</th>
                <th scope="col">Maximum</th>
                <th scope="col">Dépensés</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="slot in values.spellSlots" :key="slot.level">
                <th scope="row">{{ slot.level }}</th>
                <td><input v-model.number="slot.max" type="number" min="0" class="small" :aria-label="`Emplacements de niveau ${slot.level}`" /></td>
                <td>
                  <input
                    v-model.number="slot.expended"
                    type="number"
                    min="0"
                    class="small"
                    :aria-label="`Emplacements de niveau ${slot.level} dépensés`"
                  />
                </td>
              </tr>
            </tbody>
          </table>
        </section>

        <section class="texts" aria-label="Textes">
          <label>Capacités <textarea v-model="values.features" rows="4" /></label>
          <label>Équipement <textarea v-model="values.equipment" rows="3" /></label>
          <label>Maîtrises <textarea v-model="values.proficiencies" rows="2" /></label>
          <label>Langues <textarea v-model="values.languages" rows="2" /></label>
        </section>
      </div>
    </div>
  </div>
  <p v-else class="loading">Lecture de la fiche…</p>
</template>

<style scoped>
.character-sheet {
  display: flex;
  flex-direction: column;
  gap: 1.2rem;
  margin-top: 0.6rem;
}
h3 {
  margin: 0 0 0.6rem;
  font-family: var(--font-title);
  font-size: 1.15rem;
}
h4 {
  margin: 1rem 0 0.4rem;
  font-size: 0.95rem;
  color: var(--ink-muted);
}
label {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  font-size: 0.9rem;
}
input,
select,
textarea {
  font: inherit;
}
input[type='text'],
input[type='number'],
textarea {
  width: 100%;
  min-width: 0;
}
output {
  font-variant-numeric: tabular-nums;
}
.muted {
  color: var(--ink-muted);
}
.save-state {
  min-height: 1.2em;
  margin: 0;
  font-size: 0.85rem;
  color: var(--ink-muted);
}
.save-state.refused {
  color: var(--danger);
}
.identity {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(9rem, 1fr));
  gap: 0.7rem;
}
.columns {
  display: grid;
  grid-template-columns: minmax(14rem, 18rem) 1fr;
  gap: 1.4rem;
  align-items: start;
}
@media (max-width: 52rem) {
  .columns {
    grid-template-columns: 1fr;
  }
}
.ability-list {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0.5rem;
  margin: 0 0 0.8rem;
  padding: 0;
  list-style: none;
}
.ability {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.2rem;
  padding: 0.45rem 0.35rem;
  border: 1px solid var(--rule);
  border-radius: 4px;
  background: var(--wood-sunken);
}
.ability label {
  align-items: center;
}
.ability .short {
  font-weight: 700;
  letter-spacing: 0.04em;
}
.ability input {
  width: 3.6rem;
  text-align: center;
}
.mod {
  font-family: var(--font-title);
  font-size: 1.2rem;
  font-weight: 700;
}
.inline {
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
}
.inline input {
  width: 4rem;
}
.checks {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  margin: 0;
  padding: 0;
  list-style: none;
}
.checks li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
}
.checks label {
  flex-direction: row;
  align-items: center;
  gap: 0.4rem;
}
.checks select {
  width: 3.2rem;
}
.passive {
  display: flex;
  justify-content: space-between;
  margin: 0.8rem 0 0;
  font-weight: 700;
}
.right {
  display: flex;
  flex-direction: column;
  gap: 1.2rem;
  min-width: 0;
}
.stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(7rem, 1fr));
  gap: 0.7rem;
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
input.small {
  width: 4rem;
}
.attacks input[type='text'] {
  min-width: 6rem;
}
.attacks input.name {
  min-width: 9rem;
}
.attacks {
  overflow-x: auto;
  margin-bottom: 0.5rem;
}
.attacks table {
  margin: 0;
}
.slots {
  max-width: 20rem;
}
.texts {
  display: flex;
  flex-direction: column;
  gap: 0.7rem;
}
textarea {
  resize: vertical;
}
.visually-hidden {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip-path: inset(50%);
  white-space: nowrap;
}
.loading {
  color: var(--ink-muted);
}
</style>

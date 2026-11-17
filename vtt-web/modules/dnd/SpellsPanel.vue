<script setup lang="ts">
import { computed, ref } from 'vue'
import { ABILITIES, ABILITY_LABELS, signed, type Ability, type Derived, type SheetValues, type Spell } from './sheet'

/**
 * Les sorts d'une fiche : caractéristique d'incantation, DD et attaque de sort (calculés par le serveur, à la dernière
 * réponse), sorts mineurs, grimoire et sorts préparés. Chaque liste se retouche (ajouter un sort de son niveau, retirer
 * par ×) ; chaque geste émet ce qu'il change, que la fiche enregistre comme une frappe.
 */
type SpellValues = Pick<SheetValues, 'spellcastingAbility' | 'cantrips' | 'spellbook' | 'preparedSpells'>
type SpellListKey = 'cantrips' | 'spellbook' | 'preparedSpells'

const props = defineProps<{ values: SheetValues; derived: Derived; spells: Spell[] }>()
const emit = defineEmits<{ change: [patch: Partial<SpellValues>] }>()

const LISTS: { key: SpellListKey; title: string; cantrips: boolean }[] = [
  { key: 'cantrips', title: 'Sorts mineurs', cantrips: true },
  { key: 'spellbook', title: 'Grimoire', cantrips: false },
  { key: 'preparedSpells', title: 'Sorts préparés', cantrips: false },
]

const labels = computed(() => new Map(props.spells.map((s) => [s.id, s])))
/** Le sort choisi à ajouter, par liste. */
const choice = ref<Record<SpellListKey, string>>({ cantrips: '', spellbook: '', preparedSpells: '' })

/** Les sorts qu'une liste peut encore prendre : de son niveau, pas déjà dedans. */
function addable(key: SpellListKey, cantrips: boolean): Spell[] {
  return props.spells.filter((s) => (s.level === 0) === cantrips && !props.values[key].includes(s.id))
}

function label(id: string): string {
  const spell = labels.value.get(id)
  if (!spell) return id
  const marks = [spell.concentration ? 'C' : '', spell.ritual ? 'R' : ''].filter(Boolean).join(', ')
  return marks ? `${spell.label} (${marks})` : spell.label
}

function add(key: SpellListKey) {
  const id = choice.value[key]
  if (!id) return
  emit('change', { [key]: [...props.values[key], id] })
  choice.value[key] = ''
}

function remove(key: SpellListKey, id: string) {
  emit('change', { [key]: props.values[key].filter((s) => s !== id) })
}

function chooseAbility(value: string) {
  emit('change', { spellcastingAbility: (value || null) as Ability | null })
}

const dc = computed(() => props.derived.spellSaveDc)
const attack = computed(() => props.derived.spellAttackBonus)
</script>

<template>
  <section class="spells" aria-labelledby="spells-title">
    <h4 id="spells-title">Sorts</h4>
    <div class="casting">
      <label>
        Incantation
        <select :value="values.spellcastingAbility ?? ''" @change="chooseAbility(($event.target as HTMLSelectElement).value)">
          <option value="">—</option>
          <option v-for="a in ABILITIES" :key="a" :value="a">{{ ABILITY_LABELS[a].label }}</option>
        </select>
      </label>
      <span>DD <strong>{{ dc ?? '—' }}</strong></span>
      <span>Attaque <strong>{{ attack === null ? '—' : signed(attack) }}</strong></span>
    </div>
    <div v-for="list in LISTS" :key="list.key" class="list">
      <span class="title">{{ list.title }}</span>
      <ul>
        <li v-for="id in values[list.key]" :key="id">
          {{ label(id) }}
          <button type="button" class="ghost" :aria-label="`Retirer ${label(id)} de ${list.title}`" @click="remove(list.key, id)">
            ×
          </button>
        </li>
        <li v-if="!values[list.key].length" class="muted">aucun</li>
      </ul>
      <span class="add">
        <select v-model="choice[list.key]" :aria-label="`Sort à ajouter : ${list.title}`">
          <option value="">Ajouter…</option>
          <option v-for="s in addable(list.key, list.cantrips)" :key="s.id" :value="s.id">{{ s.label }}</option>
        </select>
        <button type="button" :disabled="!choice[list.key]" @click="add(list.key)">Ajouter</button>
      </span>
    </div>
  </section>
</template>

<style scoped>
.spells {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}
h4 {
  margin: 0;
}
select,
button {
  font: inherit;
}
.casting {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.5rem 1rem;
  font-size: 0.9rem;
}
.casting label {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}
.list {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 0.3rem 0.6rem;
  font-size: 0.9rem;
}
.title {
  min-width: 7.5rem;
  color: var(--ink-muted);
}
ul {
  display: flex;
  flex-wrap: wrap;
  gap: 0.2rem 0.6rem;
  margin: 0;
  padding: 0;
  list-style: none;
}
li {
  display: inline-flex;
  align-items: baseline;
  gap: 0.15rem;
}
.add {
  display: inline-flex;
  gap: 0.3rem;
}
.muted {
  color: var(--ink-muted);
}
</style>

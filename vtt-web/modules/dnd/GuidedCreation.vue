<script setup lang="ts">
import { computed, onMounted, ref, useId, watch } from 'vue'
import FoldSection from '../../src/components/FoldSection.vue'
import { dndApi } from './api'
import {
  ABILITIES,
  ABILITY_LABELS,
  pointBuyCost,
  type Ability,
  type AbilityMethod,
  type AbilityRoll,
  type Build,
  type ClassOptions,
  type Skill,
} from './sheet'

/**
 * La création guidée d'une fiche : classe, méthode et valeurs de caractéristique, compétences, Style de combat,
 * équipement de départ et Bottes d'arme. Chaque choix émet la construction entière, que la fiche envoie au serveur ;
 * rien n'est gardé ici que les brouillons d'un choix pas encore envoyable (achat de points, dés pas encore lancés).
 * Classe « — » : saisie libre, rien n'est envoyé.
 */
const props = defineProps<{
  campaignId: string
  build: Build | null
  rolls: AbilityRoll[] | null
  /** Vrai pendant un envoi : les choix sont éteints. */
  busy: boolean
  /** La raison du dernier refus du serveur. */
  error: string | null
}>()
const emit = defineEmits<{
  build: [build: Build]
  /** Lancer les dés, puis appliquer la construction que `next` tire des jets. */
  roll: [next: (rolls: AbilityRoll[]) => Build]
}>()

const METHODS: { id: AbilityMethod; label: string }[] = [
  { id: 'STANDARD_ARRAY', label: 'Valeurs standard' },
  { id: 'POINT_BUY', label: 'Achat de points' },
  { id: 'ROLLED', label: 'Dés' },
]

/** Les armes de l'équipement de départ par défaut de chaque classe, en Bottes d'arme d'emblée (SRD 5.2.1). */
const DEFAULT_MASTERIES: Record<string, string[]> = { FIGHTER: ['GREATSWORD', 'FLAIL', 'JAVELIN'] }
const DEFAULT_STYLE = 'DEFENSE'

const options = ref<ClassOptions[]>([])
const loadError = ref<string | null>(null)
/** La classe choisie ; '' : saisie libre. */
const classId = ref(props.build?.characterClass ?? '')
const method = ref<AbilityMethod>(props.build?.method ?? 'STANDARD_ARRAY')
/** Les valeurs à l'écran : celles de la construction, ou le brouillon de l'achat de points. */
const scores = ref<Record<Ability, number>>(scoresOf(props.build))

const id = useId()

onMounted(async () => {
  try {
    options.value = await dndApi(props.campaignId).classes()
  } catch (e) {
    loadError.value = `Classes illisibles : ${e instanceof Error ? e.message : String(e)}`
  }
})

function scoresOf(build: Build | null): Record<Ability, number> {
  return Object.fromEntries(ABILITIES.map((a) => [a, build?.scores[a] ?? 10])) as Record<Ability, number>
}

// La réponse du serveur fait foi : brouillons remis à la construction gardée.
watch(
  () => props.build,
  (build) => {
    if (!build) return
    classId.value = build.characterClass
    method.value = build.method
    scores.value = scoresOf(build)
  },
)

const current = computed(() => options.value.find((o) => o.id === classId.value) ?? null)
/** La construction de la classe choisie, `null` tant que le serveur ne l'a pas rendue. */
const shown = computed(() => (current.value && props.build?.characterClass === classId.value ? props.build : null))

function defaults(o: ClassOptions): Build {
  const known = (DEFAULT_MASTERIES[o.id] ?? []).filter((w) => o.weapons.some((weapon) => weapon.id === w))
  const masteries = [...known, ...o.weapons.map((w) => w.id).filter((w) => !known.includes(w))].slice(0, o.masteryCount)
  return {
    characterClass: o.id,
    method: 'STANDARD_ARRAY',
    scores: { ...o.suggestedScores },
    skills: [],
    fightingStyle: o.fightingStyles.find((s) => s.id === DEFAULT_STYLE)?.id ?? o.fightingStyles[0].id,
    equipment: o.equipments[0].id,
    masteries,
  }
}

function send(change: Partial<Build>) {
  if (!shown.value) return
  emit('build', { ...shown.value, ...change })
}

function chooseClass(value: string) {
  classId.value = value
  const o = current.value
  if (o && props.build?.characterClass !== o.id) emit('build', defaults(o))
}

/** Les totaux des jets, du plus haut au plus bas, sur les caractéristiques rangées par la répartition suggérée. */
function rolledScores(o: ClassOptions, rolls: AbilityRoll[]): Record<Ability, number> {
  const totals = rolls.map((r) => r.total).sort((a, b) => b - a)
  const ranked = [...ABILITIES].sort((a, b) => o.suggestedScores[b] - o.suggestedScores[a])
  return Object.fromEntries(ranked.map((a, i) => [a, totals[i]])) as Record<Ability, number>
}

function chooseMethod(value: AbilityMethod) {
  const o = current.value
  method.value = value
  if (!o) return
  if (value !== 'ROLLED') send({ method: value, scores: { ...o.suggestedScores } })
  else if (props.rolls) send({ method: value, scores: rolledScores(o, props.rolls) })
}

function roll() {
  const o = current.value
  const base = shown.value
  if (!o || !base) return
  emit('roll', (rolls) => ({ ...base, method: 'ROLLED', scores: rolledScores(o, rolls) }))
}

/** Les valeurs d'une liste d'affectation : les valeurs standard ou les totaux des jets. */
const pool = computed(() => {
  const o = current.value
  const values = method.value === 'ROLLED' ? (props.rolls ?? []).map((r) => r.total) : (o?.standardScores ?? [])
  return [...new Set(values)].sort((a, b) => b - a)
})

/** Une valeur déjà prise par une autre caractéristique lui passe la valeur d'avant : aucun doublon. */
function assign(ability: Ability, value: number) {
  const next = { ...scores.value }
  const other = ABILITIES.find((a) => a !== ability && next[a] === value)
  if (other) next[other] = next[ability]
  next[ability] = value
  send({ method: method.value, scores: next })
}

const spent = computed(() => (current.value ? pointBuyCost(scores.value, current.value.pointBuyCosts) : null))
const budget = computed(() => current.value?.pointBuyBudget ?? 0)
const pointBuyRefusal = computed(() => {
  if (method.value !== 'POINT_BUY') return null
  if (spent.value === null) return 'Achat de points : valeurs de 8 à 15'
  if (spent.value > budget.value) return `Achat de points : ${spent.value} points dépensés, ${budget.value} au plus`
  return null
})
const pointBuyChanged = computed(() => ABILITIES.some((a) => scores.value[a] !== shown.value?.scores[a]))

function applyPointBuy() {
  if (pointBuyRefusal.value || !pointBuyChanged.value) return
  send({ method: 'POINT_BUY', scores: { ...scores.value } })
}

/** Le dé écarté d'un jet : le premier des plus bas. */
const dropped = (r: AbilityRoll) => r.dice.indexOf(Math.min(...r.dice))

function toggleSkill(skill: Skill, checked: boolean) {
  const o = current.value
  if (!o || !shown.value) return
  const chosen = new Set(shown.value.skills)
  if (checked) chosen.add(skill)
  else chosen.delete(skill)
  send({ skills: o.skillChoices.map((s) => s.id).filter((s) => chosen.has(s)) })
}

function chooseMastery(index: number, weapon: string) {
  if (!shown.value) return
  const next = [...shown.value.masteries]
  const other = next.indexOf(weapon)
  if (other >= 0) next[other] = next[index]
  next[index] = weapon
  send({ masteries: next })
}

const styleText = computed(() => current.value?.fightingStyles.find((s) => s.id === shown.value?.fightingStyle)?.text)
const reason = computed(() => props.error ?? pointBuyRefusal.value ?? loadError.value)
</script>

<template>
  <FoldSection title="Création guidée" storage-key="dnd.guided">
    <fieldset class="guided" :disabled="busy">
      <label class="row">
        Classe
        <select :value="classId" @change="chooseClass(($event.target as HTMLSelectElement).value)">
          <option value="">— (saisie libre)</option>
          <option v-for="o in options" :key="o.id" :value="o.id">{{ o.label }}</option>
        </select>
      </label>

      <template v-if="current && shown">
        <section class="part" :aria-labelledby="`${id}-abilities`">
          <h4 :id="`${id}-abilities`">Caractéristiques</h4>
          <label class="row">
            Méthode
            <select :value="method" @change="chooseMethod(($event.target as HTMLSelectElement).value as AbilityMethod)">
              <option v-for="m in METHODS" :key="m.id" :value="m.id">{{ m.label }}</option>
            </select>
          </label>

          <template v-if="method === 'POINT_BUY'">
            <ul class="scores">
              <li v-for="ability in ABILITIES" :key="ability">
                <label :title="ABILITY_LABELS[ability].label">
                  <span class="short">{{ ABILITY_LABELS[ability].short }}</span>
                  <input
                    v-model.number="scores[ability]"
                    type="number"
                    min="8"
                    max="15"
                    :aria-label="ABILITY_LABELS[ability].label"
                    @change="applyPointBuy"
                  />
                </label>
              </li>
            </ul>
            <div class="row">
              <span role="status">Points restants {{ spent === null ? '—' : budget - spent }} / {{ budget }}</span>
              <button type="button" :disabled="!!pointBuyRefusal || !pointBuyChanged" @click="applyPointBuy">Appliquer</button>
            </div>
          </template>
          <p v-else-if="method === 'ROLLED' && !rolls" class="row">
            <span class="muted">Six jets de 4d6, les trois plus hauts.</span>
            <button type="button" class="primary" @click="roll">Lancer les dés</button>
          </p>
          <ul v-else class="scores">
            <li v-for="ability in ABILITIES" :key="ability">
              <label :title="ABILITY_LABELS[ability].label">
                <span class="short">{{ ABILITY_LABELS[ability].short }}</span>
                <select
                  :value="scores[ability]"
                  :aria-label="ABILITY_LABELS[ability].label"
                  @change="assign(ability, Number(($event.target as HTMLSelectElement).value))"
                >
                  <option v-for="v in pool" :key="v" :value="v">{{ v }}</option>
                </select>
              </label>
            </li>
          </ul>

          <ol v-if="rolls" class="rolls" aria-label="Jets de caractéristique">
            <li v-for="(r, i) in rolls" :key="i">
              <strong>{{ r.total }}</strong>
              <span class="dice">
                <template v-for="(d, j) in r.dice" :key="j">
                  <s v-if="j === dropped(r)">{{ d }}</s>
                  <span v-else>{{ d }}</span>
                </template>
              </span>
            </li>
          </ol>
        </section>

        <fieldset class="part">
          <legend>Compétences ({{ current.skillCount }})</legend>
          <ul class="skills">
            <li v-for="s in current.skillChoices" :key="s.id">
              <label>
                <input
                  type="checkbox"
                  :checked="shown.skills.includes(s.id)"
                  :disabled="!shown.skills.includes(s.id) && shown.skills.length >= current.skillCount"
                  @change="toggleSkill(s.id, ($event.target as HTMLInputElement).checked)"
                />
                {{ s.label }}
              </label>
            </li>
          </ul>
        </fieldset>

        <div class="part">
          <label class="row">
            Style de combat
            <select :value="shown.fightingStyle" @change="send({ fightingStyle: ($event.target as HTMLSelectElement).value })">
              <option v-for="s in current.fightingStyles" :key="s.id" :value="s.id">{{ s.label }}</option>
            </select>
          </label>
          <p v-if="styleText" class="muted text">{{ styleText }}</p>
        </div>

        <fieldset class="part">
          <legend>Équipement</legend>
          <label v-for="e in current.equipments" :key="e.id" class="option">
            <input type="radio" :name="`${id}-equipment`" :checked="shown.equipment === e.id" @change="send({ equipment: e.id })" />
            <span><strong>{{ e.label }}</strong> : {{ e.text }}</span>
          </label>
        </fieldset>

        <fieldset class="part">
          <legend>Bottes d'arme ({{ current.masteryCount }})</legend>
          <div class="masteries">
            <select
              v-for="(weapon, i) in shown.masteries"
              :key="i"
              :value="weapon"
              :aria-label="`Botte d'arme ${i + 1}`"
              @change="chooseMastery(i, ($event.target as HTMLSelectElement).value)"
            >
              <option v-for="w in current.weapons" :key="w.id" :value="w.id">{{ w.label }} ({{ w.mastery }})</option>
            </select>
          </div>
        </fieldset>
      </template>

      <p v-if="reason" class="refusal" role="alert">{{ reason }}</p>
    </fieldset>
  </FoldSection>
</template>

<style scoped>
fieldset {
  min-width: 0;
  margin: 0;
  padding: 0;
  border: 0;
}
.guided {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
h4,
legend {
  margin: 0 0 0.4rem;
  padding: 0;
  font-size: 0.95rem;
  color: var(--ink-muted);
}
label {
  font-size: 0.9rem;
}
select,
input {
  font: inherit;
}
.row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.6rem;
  margin: 0;
}
.part {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}
.muted {
  color: var(--ink-muted);
}
.text {
  margin: 0;
  font-size: 0.85rem;
}
.scores {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(5.5rem, 1fr));
  gap: 0.5rem;
  margin: 0;
  padding: 0;
  list-style: none;
}
.scores label {
  display: flex;
  align-items: center;
  gap: 0.35rem;
}
.scores input {
  width: 3.6rem;
}
.short {
  font-weight: 700;
  letter-spacing: 0.04em;
}
.rolls {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem 1rem;
  margin: 0;
  padding: 0;
  list-style: none;
  font-variant-numeric: tabular-nums;
}
.rolls li {
  display: flex;
  align-items: baseline;
  gap: 0.35rem;
}
.dice {
  display: inline-flex;
  gap: 0.25rem;
  color: var(--ink-muted);
}
.skills {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(10rem, 1fr));
  gap: 0.2rem 0.8rem;
  margin: 0;
  padding: 0;
  list-style: none;
}
.skills label,
.option {
  display: flex;
  align-items: baseline;
  gap: 0.4rem;
}
.masteries {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}
.refusal {
  margin: 0;
  color: var(--danger);
}
</style>

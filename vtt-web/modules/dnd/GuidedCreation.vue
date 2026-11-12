<script setup lang="ts">
import { computed, onMounted, ref, useId, watch } from 'vue'
import FoldSection from '../../src/components/FoldSection.vue'
import { dndApi } from './api'
import {
  ABILITIES,
  ABILITY_LABELS,
  SKILLS,
  pointBuyCost,
  type Ability,
  type AbilityMethod,
  type AbilityRoll,
  type BackgroundOptions,
  type Build,
  type ClassOptions,
  type OriginFeatChoice,
  type OriginFeatOptions,
  type Skill,
  type SpeciesChoice,
  type SpeciesOptions,
} from './sheet'

/**
 * La création guidée d'une fiche : classe, espèce et ses sous-choix (don de Polyvalent compris), historique et ses choix, méthode et valeurs de caractéristique, compétences,
 * Style de combat, équipement de départ et Bottes d'arme. Chaque choix émet la construction entière, que la fiche envoie au serveur ;
 * rien n'est gardé ici que les brouillons d'un choix pas encore envoyable (achat de points, dés pas encore lancés).
 * Classe « — » : saisie libre, rien n'est envoyé ; proposée tant qu'aucune classe n'est choisie.
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


const options = ref<ClassOptions[]>([])
const speciesList = ref<SpeciesOptions[]>([])
const backgroundList = ref<BackgroundOptions[]>([])
const featList = ref<OriginFeatOptions[]>([])
const loadError = ref<string | null>(null)
/** Change à chaque refus : les listes d'espèce se remontent sur la construction gardée. */
const revision = ref(0)
/** La classe choisie ; '' : saisie libre. */
const classId = ref(props.build?.characterClass ?? '')
const method = ref<AbilityMethod>(props.build?.method ?? 'STANDARD_ARRAY')
/** Les valeurs à l'écran : celles de la construction, ou le brouillon de l'achat de points. */
const scores = ref<Record<Ability, number>>(scoresOf(props.build))

const id = useId()

onMounted(async () => {
  const api = dndApi(props.campaignId)
  const [classes, species, backgrounds, feats] = await Promise.allSettled([
    api.classes(),
    api.species(),
    api.backgrounds(),
    api.originFeats(),
  ])
  const errors: string[] = []
  if (classes.status === 'fulfilled') options.value = classes.value
  else errors.push(`Classes illisibles : ${reasonOf(classes.reason)}`)
  if (species.status === 'fulfilled') speciesList.value = species.value
  else errors.push(`Espèces illisibles : ${reasonOf(species.reason)}`)
  if (backgrounds.status === 'fulfilled') backgroundList.value = backgrounds.value
  else errors.push(`Historiques illisibles : ${reasonOf(backgrounds.reason)}`)
  if (feats.status === 'fulfilled') featList.value = feats.value
  else errors.push(`Dons illisibles : ${reasonOf(feats.reason)}`)
  loadError.value = errors.length ? errors.join(' ; ') : null
})

function reasonOf(e: unknown): string {
  return e instanceof Error ? e.message : String(e)
}

function scoresOf(build: Build | null): Record<Ability, number> {
  return Object.fromEntries(ABILITIES.map((a) => [a, build?.scores[a] ?? 10])) as Record<Ability, number>
}

/** Remet les brouillons (classe, méthode, valeurs) à la construction gardée au serveur. */
function resetDrafts(build: Build | null) {
  classId.value = build?.characterClass ?? ''
  method.value = build?.method ?? 'STANDARD_ARRAY'
  scores.value = scoresOf(build)
}

// La réponse du serveur fait foi : brouillons remis à la construction gardée, et de même après un refus.
watch(
  () => props.build,
  (build) => {
    if (build) resetDrafts(build)
  },
)
watch(
  () => props.error,
  (error) => {
    if (error) {
      resetDrafts(props.build)
      revision.value++
    }
  },
)

const current = computed(() => options.value.find((o) => o.id === classId.value) ?? null)
/** La construction de la classe choisie, `null` tant que le serveur ne l'a pas rendue. */
const shown = computed(() => (current.value && props.build?.characterClass === classId.value ? props.build : null))

/** La construction d'où part la classe, telle que le serveur la propose. */
function defaults(o: ClassOptions): Build {
  return JSON.parse(JSON.stringify(o.defaultBuild)) as Build
}

function send(change: Partial<Build>) {
  if (!shown.value) return
  emit('build', { ...shown.value, ...change })
}

function chooseClass(value: string) {
  classId.value = value
  const o = current.value
  // L'espèce et l'historique ne dépendent pas de la classe : ils la suivent.
  if (o && props.build?.characterClass !== o.id) {
    emit('build', { ...defaults(o), species: props.build?.species ?? null, background: props.build?.background ?? null })
  }
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

/** L'espèce choisie, ses options telles que le serveur les sert ; `null` sans espèce. */
const species = computed(() => speciesList.value.find((s) => s.id === shown.value?.species?.species) ?? null)
const speciesSkill = computed(() => shown.value?.species?.skill ?? null)
const optionText = computed(() => species.value?.options.find((o) => o.id === shown.value?.species?.option)?.text)

/** Une espèce choisie part sans sous-choix : ils restent à choisir. */
function chooseSpecies(id: string) {
  if (!id || id === shown.value?.species?.species) return
  send({ species: { species: id, size: null, option: null, skill: null, spellcastingAbility: null, originFeat: null } })
}

/** Un sous-choix de l'espèce ; '' le remet à choisir. */
function setSpecies<K extends keyof Omit<SpeciesChoice, 'species'>>(key: K, value: string) {
  const current = shown.value?.species
  if (!current) return
  send({ species: { ...current, [key]: value || null } })
}

/** Le don de Polyvalent choisi et ses options ; `null` sans don. */
const versatile = computed(() => shown.value?.species?.originFeat ?? null)
const versatileFeat = computed(() => featList.value.find((f) => f.id === versatile.value?.feat) ?? null)

/** L'historique choisi, ses options telles que le serveur les sert ; `null` sans historique. */
const background = computed(() => backgroundList.value.find((b) => b.id === shown.value?.background?.background) ?? null)
/** Les caractéristiques d'incantation du don de l'historique (Initié à la magie). */
const backgroundSpellcasting = computed(
  () => featList.value.find((f) => f.id === background.value?.feat.id)?.spellcastingAbilities ?? [],
)
const backgroundSkills = computed<Skill[]>(() => background.value?.skills.map((s) => s.id) ?? [])
/** Les compétences de Doué (Polyvalent). */
const featSkills = computed<Skill[]>(() => versatile.value?.skills ?? [])

/** Une compétence que la classe ne peut pas prendre : donnée par l'historique, l'espèce ou Doué. */
function takenForClass(skill: Skill) {
  return backgroundSkills.value.includes(skill) || speciesSkill.value === skill || featSkills.value.includes(skill)
}
/** Une compétence que l'espèce ne peut pas prendre : donnée par la classe, l'historique ou Doué. */
function takenForSpecies(skill: Skill) {
  return !!shown.value?.skills.includes(skill) || backgroundSkills.value.includes(skill) || featSkills.value.includes(skill)
}
/** Une compétence que Doué ne peut pas prendre : donnée par la classe, l'historique ou l'espèce. */
function takenForFeat(skill: Skill) {
  return !!shown.value?.skills.includes(skill) || backgroundSkills.value.includes(skill) || speciesSkill.value === skill
}

/** Le don de l'historique, que Polyvalent ne reprend pas (SRD « Feats ») sauf Doué, répétable. */
function takenFeat(feat: string) {
  return feat === background.value?.feat.id && feat !== 'SKILLED' && feat !== 'MAGIC_INITIATE'
}
/** La liste d'Initié à la magie de l'historique, que Polyvalent ne reprend pas. */
function takenSpellList(list: string) {
  return versatile.value?.feat === background.value?.feat.id && list === background.value?.spellList?.id
}

/** Un don de Polyvalent choisi part sans sous-choix ; '' le remet à choisir. */
function chooseFeat(id: string) {
  const current = shown.value?.species
  if (!current) return
  send({ species: { ...current, originFeat: id ? { feat: id, spellList: null, spellcastingAbility: null, skills: [] } : null } })
}

function setFeat<K extends 'spellList' | 'spellcastingAbility'>(key: K, value: string) {
  const current = shown.value?.species
  if (!current?.originFeat) return
  send({ species: { ...current, originFeat: { ...current.originFeat, [key]: value || null } } })
}

function toggleFeatSkill(skill: Skill, checked: boolean) {
  const current = shown.value?.species
  if (!current?.originFeat) return
  const chosen = new Set(current.originFeat.skills)
  if (checked) chosen.add(skill)
  else chosen.delete(skill)
  send({ species: { ...current, originFeat: { ...current.originFeat, skills: SKILLS.map((s) => s.id).filter((s) => chosen.has(s)) } } })
}

/**
 * Le don de Polyvalent sous un nouvel historique : sans les compétences qu'il donne ; le même don que le sien repasse à
 * choisir, et Initié à la magie perd la même liste.
 */
function versatileAfter(feat: OriginFeatChoice | null, b: BackgroundOptions, given: Set<Skill>): OriginFeatChoice | null {
  if (!feat) return null
  if (feat.feat === b.feat.id && feat.feat !== 'SKILLED' && feat.feat !== 'MAGIC_INITIATE') return null
  const spellList = feat.feat === b.feat.id && feat.spellList === b.spellList?.id ? null : feat.spellList
  return { ...feat, spellList, skills: feat.skills.filter((s) => !given.has(s)) }
}

/**
 * Un historique choisi part avec +1 aux trois et l'équipement A, ses autres choix à faire ; les compétences de classe,
 * d'espèce ou de Doué qu'il donne repassent à choisir.
 */
function chooseBackground(id: string) {
  const build = shown.value
  const b = backgroundList.value.find((o) => o.id === id)
  if (!build || !b || id === build.background?.background) return
  const given = new Set<Skill>(b.skills.map((s) => s.id))
  const increases = Object.fromEntries(b.abilities.map((a) => [a.id, 1]))
  const species = build.species && {
    ...build.species,
    skill: build.species.skill && given.has(build.species.skill) ? null : build.species.skill,
    originFeat: versatileAfter(build.species.originFeat, b, given),
  }
  send({
    skills: build.skills.filter((s) => !given.has(s)),
    species,
    background: { background: id, increases, equipment: 'A', gamingSet: null, spellcastingAbility: null },
  })
}

function setBackground<K extends 'equipment' | 'gamingSet' | 'spellcastingAbility'>(key: K, value: string) {
  const current = shown.value?.background
  if (!current) return
  send({ background: { ...current, [key]: value || null } })
}

/** Augmentations : `'21'` pour +2 et +1, `'111'` pour +1 aux trois. */
const increaseMode = computed(() => (Object.values(shown.value?.background?.increases ?? {}).includes(2) ? '21' : '111'))
const plusTwo = computed(() => ABILITIES.find((a) => shown.value?.background?.increases[a] === 2) ?? '')
const plusOne = computed(() =>
  increaseMode.value === '21' ? (ABILITIES.find((a) => shown.value?.background?.increases[a] === 1) ?? '') : '',
)

function setIncreases(increases: Partial<Record<Ability, number>>) {
  const current = shown.value?.background
  if (current) send({ background: { ...current, increases } })
}

function chooseIncreaseMode(mode: string) {
  const abilities = background.value?.abilities.map((a) => a.id) ?? []
  if (mode === increaseMode.value || abilities.length < 3) return
  if (mode === '111') setIncreases(Object.fromEntries(abilities.map((a) => [a, 1])))
  else setIncreases({ [abilities[0]!]: 2, [abilities[1]!]: 1 })
}

/** Choisit la caractéristique à +2 ou à +1 ; prise par l'autre, elles s'échangent. */
function chooseIncrease(rank: 2 | 1, ability: Ability) {
  let two = plusTwo.value as Ability
  let one = plusOne.value as Ability
  if (rank === 2) {
    if (ability === one) one = two
    two = ability
  } else {
    if (ability === two) two = one
    one = ability
  }
  setIncreases({ [two]: 2, [one]: 1 })
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
          <!-- Une fiche construite reste retouchable à la main : « — » ne se propose qu'avant le premier choix. -->
          <option v-if="!build" value="">— (saisie libre)</option>
          <option v-for="o in options" :key="o.id" :value="o.id">{{ o.label }}</option>
        </select>
      </label>

      <template v-if="current && shown">
        <section :key="revision" class="part" :aria-labelledby="`${id}-species`">
          <h4 :id="`${id}-species`">Espèce</h4>
          <div class="row">
            <label class="row">
              Espèce
              <select :value="shown.species?.species ?? ''" @change="chooseSpecies(($event.target as HTMLSelectElement).value)">
                <!-- Comme la classe : « — » ne se propose qu'avant le premier choix, l'espèce saisie à la main reste. -->
                <option v-if="!shown.species" value="">— (saisie libre)</option>
                <option v-for="s in speciesList" :key="s.id" :value="s.id">{{ s.label }}</option>
              </select>
            </label>
            <template v-if="species && shown.species">
              <label v-if="species.sizes.length > 1" class="row">
                Taille
                <select :value="shown.species.size ?? ''" @change="setSpecies('size', ($event.target as HTMLSelectElement).value)">
                  <option value="">à choisir</option>
                  <option v-for="z in species.sizes" :key="z.id" :value="z.id">{{ z.label }}</option>
                </select>
              </label>
              <span v-else class="muted">Taille {{ species.sizes[0]?.label }}</span>
            </template>
          </div>
          <template v-if="species && shown.species">
            <label v-if="species.optionLabel" class="row">
              {{ species.optionLabel }}
              <select :value="shown.species.option ?? ''" @change="setSpecies('option', ($event.target as HTMLSelectElement).value)">
                <option value="">à choisir</option>
                <option v-for="o in species.options" :key="o.id" :value="o.id">{{ o.label }}</option>
              </select>
            </label>
            <p v-if="optionText" class="muted text">{{ optionText }}</p>
            <div v-if="species.spellcastingAbilities.length || species.skillTrait" class="row">
              <label v-if="species.spellcastingAbilities.length" class="row">
                Incantation
                <select
                  :value="shown.species.spellcastingAbility ?? ''"
                  @change="setSpecies('spellcastingAbility', ($event.target as HTMLSelectElement).value)"
                >
                  <option value="">à choisir</option>
                  <option v-for="a in species.spellcastingAbilities" :key="a.id" :value="a.id">{{ a.label }}</option>
                </select>
              </label>
              <label v-if="species.skillTrait" class="row">
                {{ species.skillTrait }}
                <select :value="shown.species.skill ?? ''" @change="setSpecies('skill', ($event.target as HTMLSelectElement).value)">
                  <option value="">à choisir</option>
                  <!-- Une compétence déjà donnée (classe, historique, Doué) ne se prend pas deux fois. -->
                  <option v-for="k in species.skillChoices" :key="k.id" :value="k.id" :disabled="takenForSpecies(k.id)">
                    {{ k.label }}
                  </option>
                </select>
              </label>
            </div>
            <template v-if="species.choosesOriginFeat">
              <label class="row">
                Polyvalent
                <select :value="versatile?.feat ?? ''" @change="chooseFeat(($event.target as HTMLSelectElement).value)">
                  <option value="">à choisir</option>
                  <!-- Un don ne se prend qu'une fois : celui de l'historique est éteint, sauf Doué et Initié (autre liste). -->
                  <option v-for="f in featList" :key="f.id" :value="f.id" :disabled="takenFeat(f.id)">{{ f.label }}</option>
                </select>
              </label>
              <p v-if="versatileFeat" class="muted text">{{ versatileFeat.text }}</p>
              <div v-if="versatile && versatileFeat?.spellLists.length" class="row">
                <label class="row">
                  Liste de sorts
                  <select :value="versatile.spellList ?? ''" @change="setFeat('spellList', ($event.target as HTMLSelectElement).value)">
                    <option value="">à choisir</option>
                    <option v-for="l in versatileFeat.spellLists" :key="l.id" :value="l.id" :disabled="takenSpellList(l.id)">
                      {{ l.label }}
                    </option>
                  </select>
                </label>
                <label class="row">
                  Incantation
                  <select
                    :value="versatile.spellcastingAbility ?? ''"
                    @change="setFeat('spellcastingAbility', ($event.target as HTMLSelectElement).value)"
                  >
                    <option value="">à choisir</option>
                    <option v-for="a in versatileFeat.spellcastingAbilities" :key="a.id" :value="a.id">{{ a.label }}</option>
                  </select>
                </label>
              </div>
              <fieldset v-if="versatile && versatileFeat?.skillCount" class="part">
                <legend>Compétences de {{ versatileFeat.label }} ({{ versatileFeat.skillCount }})</legend>
                <ul class="skills">
                  <li v-for="k in SKILLS" :key="k.id">
                    <label>
                      <input
                        type="checkbox"
                        :checked="featSkills.includes(k.id)"
                        :disabled="
                          (!featSkills.includes(k.id) && featSkills.length >= versatileFeat.skillCount) || takenForFeat(k.id)
                        "
                        @change="toggleFeatSkill(k.id, ($event.target as HTMLInputElement).checked)"
                      />
                      {{ k.label }}
                    </label>
                  </li>
                </ul>
              </fieldset>
            </template>
          </template>
        </section>

        <section :key="`b${revision}`" class="part" :aria-labelledby="`${id}-background`">
          <h4 :id="`${id}-background`">Historique</h4>
          <label class="row">
            Historique
            <select :value="shown.background?.background ?? ''" @change="chooseBackground(($event.target as HTMLSelectElement).value)">
              <!-- Comme l'espèce : « — » ne se propose qu'avant le premier choix, l'historique saisi à la main reste. -->
              <option v-if="!shown.background" value="">— (saisie libre)</option>
              <option v-for="b in backgroundList" :key="b.id" :value="b.id">{{ b.label }}</option>
            </select>
          </label>
          <template v-if="background && shown.background">
            <div class="row">
              <label class="row">
                Augmentations
                <select :value="increaseMode" @change="chooseIncreaseMode(($event.target as HTMLSelectElement).value)">
                  <option value="21">+2 et +1</option>
                  <option value="111">+1 aux trois</option>
                </select>
              </label>
              <template v-if="increaseMode === '21'">
                <label class="row">
                  +2
                  <select :value="plusTwo" @change="chooseIncrease(2, ($event.target as HTMLSelectElement).value as Ability)">
                    <option v-for="a in background.abilities" :key="a.id" :value="a.id">{{ a.label }}</option>
                  </select>
                </label>
                <label class="row">
                  +1
                  <select :value="plusOne" @change="chooseIncrease(1, ($event.target as HTMLSelectElement).value as Ability)">
                    <option v-for="a in background.abilities" :key="a.id" :value="a.id">{{ a.label }}</option>
                  </select>
                </label>
              </template>
              <span v-else class="muted">{{ background.abilities.map((a) => a.label).join(', ') }}</span>
            </div>
            <p class="muted text">
              Compétences : {{ background.skills.map((k) => k.label).join(', ') }}
              <template v-if="background.tool"> · Outil : {{ background.tool }}</template>
            </p>
            <label v-if="background.gamingSets.length" class="row">
              Boîte de jeux
              <select
                :value="shown.background.gamingSet ?? ''"
                @change="setBackground('gamingSet', ($event.target as HTMLSelectElement).value)"
              >
                <option value="">à choisir</option>
                <option v-for="g in background.gamingSets" :key="g.id" :value="g.id">{{ g.label }}</option>
              </select>
            </label>
            <p class="muted text">
              Don : <strong>{{ background.feat.label }}</strong
              ><template v-if="background.spellList"> ({{ background.spellList.label }})</template> — {{ background.feat.text }}
            </p>
            <label v-if="background.spellList" class="row">
              Incantation
              <select
                :value="shown.background.spellcastingAbility ?? ''"
                @change="setBackground('spellcastingAbility', ($event.target as HTMLSelectElement).value)"
              >
                <option value="">à choisir</option>
                <option v-for="a in backgroundSpellcasting" :key="a.id" :value="a.id">{{ a.label }}</option>
              </select>
            </label>
            <div class="part">
              <label v-for="e in background.equipments" :key="e.id" class="option">
                <input
                  type="radio"
                  :name="`${id}-background-equipment`"
                  :checked="shown.background.equipment === e.id"
                  @change="setBackground('equipment', e.id)"
                />
                <span><strong>{{ e.label }}</strong> : {{ e.text }}</span>
              </label>
            </div>
          </template>
        </section>

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
                  :disabled="
                    (!shown.skills.includes(s.id) && shown.skills.length >= current.skillCount) || takenForClass(s.id)
                  "
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

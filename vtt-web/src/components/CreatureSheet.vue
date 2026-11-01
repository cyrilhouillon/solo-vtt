<script setup lang="ts">
import { formatLength, KIND_LABELS, type Attack, type CreatureSheet } from '../types'

/**
 * Une créature composée comme un bloc de statistiques.
 * `aiming` : l'id de l'attaque de cette feuille en cours de ciblage, ou `null`.
 * `actions` : faux pour une feuille en lecture seule (onglet Cible).
 */
withDefaults(defineProps<{ sheet: CreatureSheet | null; aiming?: string | null; actions?: boolean; empty: string }>(), {
  aiming: null,
  actions: true,
})
const emit = defineEmits<{ attack: [attack: Attack] }>()

const numberFormat = new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 1 })

/** Allonge ou portée, dans l'unité de la carte. */
function distance(attack: Attack): string {
  if (attack.reach) return `allonge ${formatLength(attack.reach.value, attack.reach.unitSymbol)}`
  if (attack.range) {
    const { normal, longRange, unitSymbol } = attack.range
    return `portée ${numberFormat.format(normal)}/${formatLength(longRange, unitSymbol)}`
  }
  return ''
}

function signed(value: number): string {
  return value >= 0 ? `+${value}` : `${value}`
}
</script>

<template>
  <article v-if="sheet" class="block">
    <h3 class="name">{{ sheet.name }}</h3>
    <p class="kind">{{ KIND_LABELS[sheet.kind] }}<template v-if="sheet.hitPoints === 0"> · <strong class="down">à terre</strong></template></p>
    <hr class="tapered" />
    <dl class="stats">
      <div>
        <dt>Classe d'armure</dt>
        <dd>{{ sheet.armorClass }}</dd>
      </div>
      <div>
        <dt>Points de vie</dt>
        <dd>{{ sheet.hitPoints }} / {{ sheet.maxHitPoints }}</dd>
      </div>
      <div>
        <dt>Vitesse</dt>
        <dd>{{ formatLength(sheet.speed.value, sheet.speed.unitSymbol) }}</dd>
      </div>
    </dl>
    <template v-if="sheet.resistances.length || sheet.vulnerabilities.length">
      <hr class="tapered" />
      <p v-if="sheet.resistances.length"><strong class="key">Résistances</strong> {{ sheet.resistances.join(', ') }}</p>
      <p v-if="sheet.vulnerabilities.length"><strong class="key">Vulnérabilités</strong> {{ sheet.vulnerabilities.join(', ') }}</p>
    </template>
    <h4>Attaques</h4>
    <ul v-if="sheet.attacks.length" class="attacks">
      <li v-for="attack in sheet.attacks" :key="attack.id" :class="{ aiming: aiming === attack.id }">
        <p>
          <em class="label">{{ attack.label }}.</em>{{ ' ' }}<span class="key">{{ signed(attack.attackBonus) }}</span> au
          toucher, {{ distance(attack) }}.
          Dégâts <span class="key">{{ attack.damage }}</span> {{ attack.damageType }}.
        </p>
        <button
          v-if="actions"
          type="button"
          :class="{ aiming: aiming === attack.id }"
          :aria-pressed="aiming === attack.id"
          :aria-label="`Attaquer avec ${attack.label}`"
          @click="emit('attack', attack)"
        >
          {{ aiming === attack.id ? 'Annuler' : 'Attaquer' }}
        </button>
      </li>
    </ul>
    <p v-else class="muted">Aucune</p>
  </article>
  <p v-else class="muted">{{ empty }}</p>
</template>

<style scoped>
.block {
  display: flex;
  flex-direction: column;
}
.name {
  margin: 0;
  font-family: var(--font-title);
  font-weight: 700;
  font-size: 1.6rem;
  line-height: 1.1;
  color: var(--rubric);
}
.kind {
  margin: 0.15rem 0 0;
  font-style: italic;
  color: var(--ink-muted);
}
.down {
  font-style: normal;
  color: var(--rubric);
}
p {
  margin: 0;
}
/* Lignes de bloc de statistiques : le libellé, puis le chiffre en rubrique. */
.stats {
  display: flex;
  flex-direction: column;
  gap: 0.1rem;
  margin: 0;
}
.stats div {
  display: flex;
  align-items: baseline;
  gap: 0.5rem;
}
dt {
  font-weight: 700;
}
dd {
  margin: 0;
  font-size: 1.15rem;
  font-weight: 700;
  color: var(--rubric);
  font-variant-numeric: tabular-nums;
}
.key {
  font-weight: 700;
  color: var(--rubric);
  font-variant-numeric: tabular-nums;
}
h4 {
  margin: 1.1rem 0 0.4rem;
  font-family: var(--font-title);
  font-weight: 700;
  font-size: 1.1rem;
  color: var(--rubric);
  border-bottom: 1px solid var(--rubric);
}
.attacks {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}
.attacks li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 0.45rem 0.6rem;
  border-radius: 4px;
  border: 1px solid transparent;
}
.attacks li.aiming {
  border-color: var(--brass);
  background: var(--wood-raised);
}
.label {
  font-weight: 700;
}
.attacks button {
  flex: none;
}
.attacks button.aiming {
  background: var(--brass);
  border-color: var(--brass);
  color: var(--on-brass);
}
.muted {
  color: var(--ink-muted);
}
</style>

<script setup lang="ts">
import { formatLength, KIND_LABELS, type CreatureSheet } from '../types'

defineProps<{ sheet: CreatureSheet | null }>()

function signed(value: number): string {
  return value >= 0 ? `+${value}` : `${value}`
}
</script>

<template>
  <section class="sheet" aria-labelledby="sheet-title">
    <template v-if="sheet">
      <header>
        <h2 id="sheet-title">{{ sheet.name }}</h2>
        <span class="kind">{{ KIND_LABELS[sheet.kind] }}</span>
      </header>
      <dl class="stats">
        <div>
          <dt>CA</dt>
          <dd>{{ sheet.armorClass }}</dd>
        </div>
        <div>
          <dt>PV</dt>
          <dd>{{ sheet.hitPoints }} / {{ sheet.maxHitPoints }}</dd>
        </div>
        <div>
          <dt>Vitesse</dt>
          <dd>{{ formatLength(sheet.speed.value, sheet.speed.unitSymbol) }}</dd>
        </div>
      </dl>
      <h3>Attaque</h3>
      <p v-if="sheet.weapon">
        <strong>{{ sheet.weapon.name }}</strong> : {{ signed(sheet.weapon.attackBonus) }} au toucher,
        {{ sheet.weapon.damage }} dégâts {{ sheet.weapon.damageType }}
      </p>
      <p v-else class="muted">Aucune</p>
      <template v-if="sheet.resistances.length">
        <h3>Résistances</h3>
        <p>Dégâts {{ sheet.resistances.join(', ') }}</p>
      </template>
      <template v-if="sheet.vulnerabilities.length">
        <h3>Vulnérabilités</h3>
        <p>Dégâts {{ sheet.vulnerabilities.join(', ') }}</p>
      </template>
    </template>
    <p v-else class="muted">Sélectionner une créature sur la carte pour voir sa feuille.</p>
  </section>
</template>

<style scoped>
.sheet {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}
header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 0.5rem;
}
h2 {
  font-size: 1.35rem;
  font-weight: 650;
  letter-spacing: -0.01em;
  margin: 0;
}
h3 {
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--text-muted);
  margin: 0.5rem 0 0;
}
p {
  margin: 0;
}
.kind,
.muted {
  color: var(--text-muted);
  font-size: 0.85rem;
}
.stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0.5rem;
  margin: 0.25rem 0 0;
}
.stats div {
  background: var(--surface-hover);
  border-radius: 6px;
  padding: 0.4rem 0.5rem;
}
dt {
  font-size: 0.75rem;
  color: var(--text-muted);
}
dd {
  margin: 0;
  font-size: 1.05rem;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}
</style>

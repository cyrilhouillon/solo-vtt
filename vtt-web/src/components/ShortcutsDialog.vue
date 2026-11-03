<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'

/**
 * La légende des gestes et des touches, ouverte par « ? » ou le bouton de la barre. Modale mais légère : Échap, un clic
 * hors du panneau ou « Fermer » la referment, et le focus revient à ce qui l'avait.
 */
const props = defineProps<{ open: boolean }>()
const emit = defineEmits<{ close: [] }>()

const dialog = ref<HTMLDialogElement | null>(null)
const closeButton = ref<HTMLButtonElement | null>(null)

/** Le dialogue suit `open`, au montage compris : sinon un `open` déjà vrai laisserait un dialogue fermé. */
function sync(open: boolean) {
  const el = dialog.value
  if (!el) return
  if (open && !el.open) {
    el.showModal()
    closeButton.value?.focus()
  } else if (!open && el.open) el.close()
}

watch(() => props.open, sync)
onMounted(() => sync(props.open))

/** Un clic sur le voile (hors du panneau) ferme ; un clic dans le panneau, non. */
function onClick(e: MouseEvent) {
  if (e.target === dialog.value) emit('close')
}

/** Les touches par moment de jeu ; chaque groupe reste court. Un geste ou un mot de liaison n'est pas une touche. */
const WORDS = new Set(['Glisser', 'Deux clics', 'à'])
const GROUPS: { title: string; rows: { keys: string[]; text: string }[] }[] = [
  {
    title: 'Carte',
    rows: [
      { keys: ['Glisser'], text: 'déplacer une créature ; Maj la cale sur la case' },
      { keys: ['D'], text: 'déplacer au clavier : flèches, puis Entrée' },
      { keys: ['+', '−', '0'], text: 'zoom avant, arrière, carte entière (molette aussi)' },
      { keys: ['Échap'], text: 'revenir d’un pas : ciblage, pose, sélection' },
    ],
  },
  {
    title: 'Créatures',
    rows: [
      { keys: ['Entrée'], text: 'sur une entrée du catalogue : la poser au clavier' },
      { keys: ['Deux clics'], text: 'sur deux créatures : mesurer leur distance' },
      { keys: ['Suppr'], text: 'retirer les créatures choisies' },
      { keys: ['Ctrl', 'Z'], text: 'annuler la dernière pose ou le dernier retrait' },
    ],
  },
  {
    title: 'Combat',
    rows: [
      { keys: ['1', 'à', '9'], text: 'attaques de la feuille ouverte' },
      { keys: ['Maj', 'F'], text: 'fin du tour' },
    ],
  },
]
</script>

<template>
  <dialog ref="dialog" class="shortcuts" aria-labelledby="shortcuts-title" @close="emit('close')" @click="onClick">
    <h2 id="shortcuts-title" class="title">Raccourcis</h2>
    <hr class="tapered" />
    <section v-for="group in GROUPS" :key="group.title" class="group">
      <h3>{{ group.title }}</h3>
      <dl>
        <div v-for="row in group.rows" :key="row.text">
          <dt>
            <template v-for="(key, index) in row.keys" :key="index"><span v-if="WORDS.has(key)" class="word">{{ key }}</span><kbd v-else>{{ key }}</kbd></template>
          </dt>
          <dd>{{ row.text }}</dd>
        </div>
      </dl>
    </section>
    <div class="actions">
      <button ref="closeButton" type="button" @click="emit('close')">Fermer</button>
    </div>
  </dialog>
</template>

<style scoped>
/* Même panneau de bois que le dialogue de décision : un objet posé sur la table. */
.shortcuts {
  width: min(30rem, calc(100vw - 2rem));
  padding: 1.1rem 1.25rem 1.25rem;
  border: 1px solid var(--rule);
  border-radius: 4px;
  background: var(--wood);
  color: var(--ink);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}
.shortcuts::backdrop {
  background: rgba(0, 0, 0, 0.45);
}
.title {
  margin: 0;
  font-family: var(--font-title);
  font-weight: 700;
  font-size: 1.35rem;
  line-height: 1.15;
}
.group + .group {
  margin-top: 1rem;
}
h3 {
  margin: 0 0 0.35rem;
  font-family: var(--font-title);
  font-size: 0.95rem;
  letter-spacing: 0.08em;
  color: var(--ink-muted);
}
dl {
  margin: 0;
  display: grid;
  gap: 0.35rem;
}
dl div {
  display: grid;
  grid-template-columns: 7.5rem minmax(0, 1fr);
  gap: 0.75rem;
  align-items: baseline;
}
dt {
  display: flex;
  flex-wrap: wrap;
  gap: 0.25rem;
}
dd {
  margin: 0;
}
/* Un geste (« Glisser ») ou « à » : du texte, pas une touche. */
.word {
  font-style: italic;
  color: var(--ink-muted);
}
/* Une touche : un pavé de bois en creux, comme un champ. */
kbd {
  display: inline-block;
  min-width: 1.6rem;
  padding: 0.05rem 0.4rem;
  border: 1px solid var(--control-border);
  border-bottom-width: 2px;
  border-radius: 4px;
  background: var(--wood-sunken);
  font: inherit;
  font-size: 0.9rem;
  font-weight: 500;
  text-align: center;
  font-variant-numeric: tabular-nums;
}
.actions {
  margin-top: 1.1rem;
  display: flex;
  justify-content: flex-end;
}
</style>

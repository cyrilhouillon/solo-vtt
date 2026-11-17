package dnd.engine.sheet;

import java.util.List;

/** L'état d'une fiche, tiré de son histoire : on rejoue ses événements dans l'ordre. */
public final class SheetHistory {

    private SheetHistory() {
    }

    /**
     * La fiche après ces événements. Une saisie remplace les valeurs et garde construction et jets ; une construction
     * remplace construction et valeurs ; des PV perdus baissent les PV actuels, jamais sous 0 ;
     * un emplacement dépensé s'ajoute aux dépensés de son niveau, jamais au-delà du maximum, et sans emplacement de ce
     * niveau ne change rien.
     *
     * @throws IllegalArgumentException si l'histoire ne commence pas par {@link SheetEvent.SheetCreated}, en contient un
     *     second, ou lance deux fois les dés de caractéristique
     */
    public static SheetState replay(List<SheetEvent> events) {
        if (events.isEmpty() || !(events.getFirst() instanceof SheetEvent.SheetCreated)) {
            throw new IllegalArgumentException("Une fiche commence par sa création");
        }
        SheetState state = null;
        for (SheetEvent event : events) {
            state = switch (event) {
                case SheetEvent.SheetCreated created -> {
                    if (state != null) throw new IllegalArgumentException("Une fiche n'est créée qu'une fois");
                    yield SheetState.blank();
                }
                case SheetEvent.SheetEdited edited -> new SheetState(edited.values(), state.build(), state.builtAbilities(), state.rolls());
                case SheetEvent.SheetBuilt built -> new SheetState(built.values(), built.build(), built.values().abilities(), state.rolls());
                case SheetEvent.HitPointsLost lost -> new SheetState(
                        state.values().withHitPoints(Math.max(0, state.values().hitPoints() - lost.amount())),
                        state.build(), state.builtAbilities(), state.rolls());
                case SheetEvent.SpellSlotExpended expended -> new SheetState(
                        state.values().withSpellSlotExpended(expended.level()),
                        state.build(), state.builtAbilities(), state.rolls());
                case SheetEvent.AbilitiesRolled rolled -> {
                    if (state.rolls() != null) {
                        throw new IllegalArgumentException("Les dés de caractéristique ne se lancent qu'une fois");
                    }
                    yield new SheetState(state.values(), state.build(), state.builtAbilities(), rolled.rolls());
                }
            };
        }
        return state;
    }
}

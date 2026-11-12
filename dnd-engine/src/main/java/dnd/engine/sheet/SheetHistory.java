package dnd.engine.sheet;

import java.util.List;

/** L'état d'une fiche, tiré de son histoire : on rejoue ses événements dans l'ordre. */
public final class SheetHistory {

    private SheetHistory() {
    }

    /**
     * La fiche après ces événements.
     *
     * @throws IllegalArgumentException si l'histoire ne commence pas par {@link SheetEvent.SheetCreated}, ou en contient un
     *     second
     */
    public static CharacterSheet replay(List<SheetEvent> events) {
        if (events.isEmpty() || !(events.getFirst() instanceof SheetEvent.SheetCreated)) {
            throw new IllegalArgumentException("Une fiche commence par sa création");
        }
        CharacterSheet sheet = null;
        for (SheetEvent event : events) {
            sheet = switch (event) {
                case SheetEvent.SheetCreated created -> {
                    if (sheet != null) throw new IllegalArgumentException("Une fiche n'est créée qu'une fois");
                    yield CharacterSheet.blank();
                }
                case SheetEvent.SheetEdited edited -> edited.values();
            };
        }
        return sheet;
    }
}

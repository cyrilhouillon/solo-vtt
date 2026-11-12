package dnd.engine.sheet;

import java.util.List;

/**
 * L'état d'une fiche, pli de son histoire : ses valeurs, sa construction guidée ({@code null} si elle n'en a pas) et ses
 * jets de caractéristique ({@code null} s'ils ne sont pas lancés).
 */
public record SheetState(CharacterSheet values, Build build, List<AbilityRoll> rolls) {

    public SheetState {
        if (values == null) throw new IllegalArgumentException("Une fiche a des valeurs");
        if (rolls != null) rolls = List.copyOf(rolls);
    }

    /** Une fiche qui vient d'être créée. */
    public static SheetState blank() {
        return new SheetState(CharacterSheet.blank(), null, null);
    }
}

package dnd.engine.sheet;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * L'état d'une fiche, pli de son histoire : ses valeurs, sa construction guidée ({@code null} si elle n'en a pas), les
 * valeurs de caractéristique de la fiche quand cette construction a été appliquée ({@code null} sans construction) et ses
 * jets de caractéristique ({@code null} s'ils ne sont pas lancés).
 */
public record SheetState(CharacterSheet values, Build build, Map<Ability, Integer> builtAbilities,
                         List<AbilityRoll> rolls) {

    public SheetState {
        if (values == null) throw new IllegalArgumentException("Une fiche a des valeurs");
        if ((build == null) != (builtAbilities == null)) {
            throw new IllegalArgumentException("Une construction garde les caractéristiques qu'elle a lues");
        }
        if (builtAbilities != null) builtAbilities = Map.copyOf(builtAbilities);
        if (rolls != null) rolls = List.copyOf(rolls);
    }

    /** Une fiche qui vient d'être créée. */
    public static SheetState blank() {
        return new SheetState(CharacterSheet.blank(), null, null, null);
    }

    /**
     * Les valeurs de la fiche une fois {@code next} appliquée : seuls les champs dont la valeur dérivée change depuis la
     * construction précédente sont réécrits. Une caractéristique prend la valeur de {@code next} si elle a changé de
     * construction en construction, sinon garde la fiche ; CA, attaques, initiative et PV suivent les caractéristiques
     * qui en sortent, retouches comprises. Une espèce choisie se change, mais ne se retire pas : la fiche garderait son
     * espèce et ses traits sans les PV qu'elle donne.
     *
     * @throws IllegalArgumentException si {@code next} retire l'espèce de la construction précédente
     */
    public CharacterSheet rebuild(Build next) {
        if (build != null && build.species() != null && next.species() == null) {
            throw new IllegalArgumentException("Espèce : elle se change, mais ne se retire pas");
        }
        GuidedValues before = build == null ? GuidedValues.NONE : GuidedValues.of(build, builtAbilities);
        Map<Ability, Integer> abilities = new EnumMap<>(values.abilities());
        for (Ability ability : Ability.values()) {
            Integer previous = build == null ? null : build.scores().get(ability);
            if (!Objects.equals(previous, next.scores().get(ability))) abilities.put(ability, next.scores().get(ability));
        }
        return GuidedValues.of(next, abilities).rebuild(values, before);
    }
}

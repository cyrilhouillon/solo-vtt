package dnd.engine.sheet;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * L'historique d'une construction guidée et ses choix : augmentations de caractéristique (+2 et +1, ou +1 aux trois, parmi
 * celles de l'historique), équipement A ou B, boîte de jeux (Soldat) et caractéristique d'incantation d'Initié à la magie.
 * Historique, augmentations et équipement sont requis ; boîte de jeux et incantation peuvent rester à choisir.
 */
public record BackgroundChoice(Background background, Map<Ability, Integer> increases, BackgroundEquipment equipment,
                               GamingSet gamingSet, Ability spellcastingAbility) {

    public BackgroundChoice {
        increases = increases == null || increases.isEmpty() ? Map.of() : Collections.unmodifiableMap(new EnumMap<>(increases));
    }

    /** Un historique avec +1 aux trois caractéristiques et l'équipement A, ses autres choix à faire. */
    public static BackgroundChoice of(Background background) {
        Map<Ability, Integer> increases = new EnumMap<>(Ability.class);
        background.abilities().forEach(ability -> increases.put(ability, 1));
        return new BackgroundChoice(background, increases, BackgroundEquipment.A, null, null);
    }

    /** Refuse un choix impossible, avec la raison. */
    public void validate() {
        if (background == null) throw new IllegalArgumentException("Choisir un historique");
        String name = background.label();
        if (increases.values().stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Augmentation inconnue");
        }
        for (Ability ability : increases.keySet()) {
            if (!background.abilities().contains(ability)) {
                throw new IllegalArgumentException("%s : %s n’est pas une caractéristique de l’historique".formatted(name, ability.label()));
            }
        }
        List<Integer> values = increases.values().stream().sorted().toList();
        if (!values.equals(List.of(1, 2)) && !values.equals(List.of(1, 1, 1))) {
            throw new IllegalArgumentException("%s : augmenter une caractéristique de 2 et une autre de 1, ou les trois de 1".formatted(name));
        }
        if (equipment == null) throw new IllegalArgumentException("%s : choisir l’équipement A ou B".formatted(name));
        if (gamingSet != null && !background.choosesGamingSet()) {
            throw new IllegalArgumentException("%s : aucune boîte de jeux à choisir".formatted(name));
        }
        feat().validate(Set.of());
    }

    /** L'augmentation d'une caractéristique, 0 sans. */
    public int increase(Ability ability) {
        return increases.getOrDefault(ability, 0);
    }

    /** Le don d'origines de l'historique, avec sa liste fixée et l'incantation choisie. */
    public OriginFeatChoice feat() {
        return new OriginFeatChoice(background.feat(), background.spellList(), spellcastingAbility, Set.of());
    }

    /** L'outil maîtrisé tel qu'il s'écrit sur la fiche. */
    public String toolText() {
        if (!background.choosesGamingSet()) return background.tool();
        return "boîte de jeux (" + (gamingSet == null ? "à choisir" : gamingSet.label()) + ")";
    }

    /** L'équipement choisi tel qu'il s'écrit sur la fiche. */
    public String equipmentText() {
        return background.equipmentText(equipment);
    }

    /** Les armes de l'équipement choisi : celles de A, aucune avec B. */
    public List<SrdWeapon> weapons() {
        return equipment == BackgroundEquipment.A ? background.weapons() : List.of();
    }
}

package dnd.engine.sheet;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Les neuf espèces du SRD 5.2.1 et leurs traits de niveau 1 (« Character Species »). Les traits de niveau 5 (Vol draconique,
 * Forme de géant) ne sont pas écrits : la fiche est de niveau 1.
 */
public enum Species {
    DRAGONBORN("Drakéide", List.of(CreatureSize.MEDIUM), 9, 18, 0, Set.of(), null, false, "Ascendance draconique",
            List.of("Souffle : remplace une attaque de l’action Attaque ; Cône de 4,50 m ou Ligne de 9 m, sauvegarde de Dextérité "
                            + "(DD 8 + mod. Con + bonus de maîtrise), 1d10 dégâts du type de l’ascendance, moitié en cas de réussite ; "
                            + "bonus de maîtrise fois par Repos long",
                    "Résistance aux dégâts : le type de l’ascendance")),
    DWARF("Nain", List.of(CreatureSize.MEDIUM), 9, 36, 1, Set.of(), null, false, null,
            List.of("Connaissance de la pierre : action Bonus, Perception des vibrations à 18 m pendant 10 minutes, sur la "
                            + "pierre ; bonus de maîtrise fois par Repos long",
                    "Résistance naine : Résistance aux dégâts de poison ; Avantage aux sauvegardes contre l’état Empoisonné",
                    "Ténacité naine : +1 PV maximum, et +1 à chaque niveau")),
    ELF("Elfe", List.of(CreatureSize.MEDIUM), 9, 18, 0,
            EnumSet.of(Skill.INSIGHT, Skill.PERCEPTION, Skill.SURVIVAL), "Sens aiguisés", true, "Lignage elfique",
            List.of("Ascendance féerique : Avantage aux sauvegardes contre l’état Charmé",
                    "Transe : pas de sommeil, la magie ne peut pas vous endormir ; Repos long en 4 heures de transe")),
    GNOME("Gnome", List.of(CreatureSize.SMALL), 9, 18, 0, Set.of(), null, true, "Lignage gnome",
            List.of("Ruse gnome : Avantage aux sauvegardes d’Intelligence, de Sagesse et de Charisme")),
    GOLIATH("Goliath", List.of(CreatureSize.MEDIUM), 10.5, 0, 0, Set.of(), null, false, "Ascendance gigante",
            List.of("Forte carrure : Avantage pour mettre fin à l’état Agrippé ; capacité de charge d’une taille au-dessus")),
    HALFLING("Halfelin", List.of(CreatureSize.SMALL), 9, 0, 0, Set.of(), null, false, null,
            List.of("Agilité halfeline : traverser l’espace d’une créature plus grande, sans s’y arrêter",
                    "Brave : Avantage aux sauvegardes contre l’état Effrayé",
                    "Chance : un 1 au d20 d’un Test d20 se relance, et le nouveau résultat compte",
                    "Discrétion naturelle : Furtivité possible dans l’ombre d’une créature plus grande d’au moins une taille")),
    HUMAN("Humain", List.of(CreatureSize.MEDIUM, CreatureSize.SMALL), 9, 0, 0, EnumSet.allOf(Skill.class), "Compétent",
            false, null,
            List.of("Ingénieux : Inspiration héroïque à chaque Repos long")),
    ORC("Orc", List.of(CreatureSize.MEDIUM), 9, 36, 0, Set.of(), null, false, null,
            List.of("Acharnement : tombé à 0 PV sans être tué, vous restez à 1 PV ; une fois par Repos long",
                    "Poussée d’adrénaline : action Pointe en action Bonus, et autant de PV temporaires que le bonus de maîtrise ; "
                            + "bonus de maîtrise fois par Repos court ou long")),
    TIEFLING("Tieffelin", List.of(CreatureSize.MEDIUM, CreatureSize.SMALL), 9, 18, 0, Set.of(), null, true, "Héritage fiélon",
            List.of("Présence d’outre-monde : sort mineur thaumaturgie, avec la caractéristique d’incantation de l’héritage"));

    private final String label;
    private final List<CreatureSize> sizes;
    private final double speed;
    private final int darkvision;
    private final int hitPointsPerLevel;
    private final Set<Skill> skillChoices;
    private final String skillTrait;
    private final boolean spellcasting;
    private final String optionLabel;
    private final List<String> traits;

    Species(String label, List<CreatureSize> sizes, double speed, int darkvision, int hitPointsPerLevel,
            Set<Skill> skillChoices, String skillTrait, boolean spellcasting, String optionLabel, List<String> traits) {
        this.label = label;
        this.sizes = sizes;
        this.speed = speed;
        this.darkvision = darkvision;
        this.hitPointsPerLevel = hitPointsPerLevel;
        this.skillChoices = Set.copyOf(skillChoices);
        this.skillTrait = skillTrait;
        this.spellcasting = spellcasting;
        this.optionLabel = optionLabel;
        this.traits = traits;
    }

    public String label() {
        return label;
    }

    /** Les tailles possibles, celle que le SRD cite d'abord en premier. */
    public List<CreatureSize> sizes() {
        return sizes;
    }

    /** La vitesse en mètres. */
    public double speed() {
        return speed;
    }

    /** La portée de la Vision dans le noir en mètres, 0 sans. */
    public int darkvision() {
        return darkvision;
    }

    /** Les PV maximum de plus à chaque niveau (Ténacité naine). */
    public int hitPointsPerLevel() {
        return hitPointsPerLevel;
    }

    /** Les compétences parmi lesquelles l'espèce en choisit une, vide si elle n'en choisit pas. */
    public Set<Skill> skillChoices() {
        return skillChoices;
    }

    /** Le trait qui donne la compétence au choix (« Sens aiguisés »), {@code null} sans. */
    public String skillTrait() {
        return skillTrait;
    }

    /** L'espèce choisit-elle sa caractéristique d'incantation (Intelligence, Sagesse ou Charisme). */
    public boolean spellcasting() {
        return spellcasting;
    }

    /** Le nom du trait dont on choisit une option (« Lignage elfique »), {@code null} sans option. */
    public String optionLabel() {
        return optionLabel;
    }

    /** Les traits de niveau 1 qui ne dépendent d'aucun choix, un par ligne (« Nom : résumé »). */
    public List<String> traits() {
        return traits;
    }

    /** L'espèce choisit-elle un don d'origines (Polyvalent de l'Humain). */
    public boolean choosesOriginFeat() {
        return this == HUMAN;
    }

    /** Les options du trait {@link #optionLabel()}, vide sans option. */
    public List<SpeciesOption> options() {
        return Arrays.stream(SpeciesOption.values()).filter(option -> option.species() == this).toList();
    }
}

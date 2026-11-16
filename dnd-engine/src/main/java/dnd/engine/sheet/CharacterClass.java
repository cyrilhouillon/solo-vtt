package dnd.engine.sheet;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import static dnd.engine.sheet.SrdWeapon.Category.MARTIAL_MELEE;
import static dnd.engine.sheet.SrdWeapon.Category.SIMPLE_MELEE;
import static dnd.engine.sheet.SrdWeapon.Category.SIMPLE_RANGED;

/** Les classes que la création guidée connaît, avec leurs traits de base et aptitudes de niveau 1 (SRD 5.2.1). */
public enum CharacterClass {
    FIGHTER("Guerrier", 10, EnumSet.of(Ability.STRENGTH, Ability.CONSTITUTION),
            EnumSet.of(Skill.ACROBATICS, Skill.ATHLETICS, Skill.ANIMAL_HANDLING, Skill.HISTORY, Skill.INSIGHT,
                    Skill.INTIMIDATION, Skill.PERCEPTION, Skill.PERSUASION, Skill.SURVIVAL), 2,
            scores(15, 14, 13, 8, 10, 12),
            "Armes courantes et armes de guerre ; armures légères, intermédiaires et lourdes, boucliers",
            new Traits(true, 3, weapon -> true, 0, null, Set.of(), null, false, false,
                    weapon -> true, EnumSet.allOf(SrdArmor.Category.class))),
    BARBARIAN("Barbare", 12, EnumSet.of(Ability.STRENGTH, Ability.CONSTITUTION),
            EnumSet.of(Skill.ATHLETICS, Skill.ANIMAL_HANDLING, Skill.INTIMIDATION, Skill.NATURE, Skill.PERCEPTION,
                    Skill.SURVIVAL), 2,
            scores(15, 13, 14, 10, 12, 8),
            "Armes courantes et armes de guerre ; armures légères et intermédiaires, boucliers",
            new Traits(false, 2, weapon -> !weapon.category().ranged(), 0, null, Set.of(),
                    Ability.CONSTITUTION, true, false,
                    weapon -> true, EnumSet.of(SrdArmor.Category.LIGHT, SrdArmor.Category.MEDIUM, SrdArmor.Category.SHIELD))),
    MONK("Moine", 8, EnumSet.of(Ability.STRENGTH, Ability.DEXTERITY),
            EnumSet.of(Skill.ACROBATICS, Skill.ATHLETICS, Skill.STEALTH, Skill.HISTORY, Skill.INSIGHT, Skill.RELIGION), 2,
            scores(12, 15, 13, 10, 14, 8),
            "Armes courantes et armes de guerre dotées de la propriété Légère ; aucune armure",
            new Traits(false, 0, weapon -> false, 0, null,
                    EnumSet.of(Tool.Category.ARTISAN, Tool.Category.MUSICAL_INSTRUMENT), Ability.WISDOM, false, true,
                    CharacterClass::monkProficient, Set.of())),
    ROGUE("Roublard", 8, EnumSet.of(Ability.DEXTERITY, Ability.INTELLIGENCE),
            EnumSet.of(Skill.ACROBATICS, Skill.ATHLETICS, Skill.STEALTH, Skill.SLEIGHT_OF_HAND, Skill.INTIMIDATION,
                    Skill.INSIGHT, Skill.INVESTIGATION, Skill.PERCEPTION, Skill.PERSUASION, Skill.DECEPTION), 4,
            scores(12, 15, 13, 14, 10, 8),
            "Armes courantes et armes de guerre dotées de la propriété Finesse ou Légère ; armures légères",
            new Traits(false, 2, CharacterClass::rogueProficient, 2, Tool.THIEVES_TOOLS, Set.of(), null, false, false,
                    CharacterClass::rogueProficient, EnumSet.of(SrdArmor.Category.LIGHT)));

    /**
     * Les aptitudes de niveau 1 qui changent la construction.
     *
     * @param fightingStyle     la classe prend-elle un Style de combat
     * @param masteryCount      le nombre de Bottes d'arme
     * @param masteryWeapons    les armes dont la classe peut prendre la botte
     * @param expertiseCount    le nombre d'Expertises
     * @param tool              l'outil maîtrisé d'office, {@code null} sans
     * @param toolChoices       les catégories où la classe choisit un outil, vides sans choix
     * @param unarmoredDefense  la caractéristique de Défense sans armure (10 + Dex + elle), {@code null} sans
     * @param unarmoredShield   la Défense sans armure tient-elle avec un bouclier
     * @param martialArts       Arts martiaux : armes de Moine et mains nues
     * @param weapons           les armes que la classe maîtrise
     * @param armorTraining     les catégories d'armure où la classe est formée, bouclier compris
     */
    private record Traits(boolean fightingStyle, int masteryCount, Predicate<SrdWeapon> masteryWeapons, int expertiseCount,
                          Tool tool, Set<Tool.Category> toolChoices, Ability unarmoredDefense, boolean unarmoredShield,
                          boolean martialArts, Predicate<SrdWeapon> weapons, Set<SrdArmor.Category> armorTraining) {
    }

    /** Le dé d'Arts martiaux au niveau 1. */
    public static final int MARTIAL_ARTS_DIE = 6;

    private final String label;
    private final int hitDie;
    private final Set<Ability> savingThrows;
    private final Set<Skill> skillChoices;
    private final int skillCount;
    private final Map<Ability, Integer> suggestedScores;
    private final String proficiencies;
    private final Traits traits;

    CharacterClass(String label, int hitDie, Set<Ability> savingThrows, Set<Skill> skillChoices, int skillCount,
                   Map<Ability, Integer> suggestedScores, String proficiencies, Traits traits) {
        this.label = label;
        this.hitDie = hitDie;
        this.savingThrows = Set.copyOf(savingThrows);
        this.skillChoices = Set.copyOf(skillChoices);
        this.skillCount = skillCount;
        this.suggestedScores = Map.copyOf(suggestedScores);
        this.proficiencies = proficiencies;
        this.traits = traits;
    }

    public String label() {
        return label;
    }

    /** Le nombre de faces du dé de vie. */
    public int hitDie() {
        return hitDie;
    }

    public Set<Ability> savingThrows() {
        return savingThrows;
    }

    /** Les compétences parmi lesquelles la classe choisit. */
    public Set<Skill> skillChoices() {
        return skillChoices;
    }

    /** Combien de compétences la classe choisit. */
    public int skillCount() {
        return skillCount;
    }

    /** La répartition des valeurs standard que le SRD suggère pour la classe. */
    public Map<Ability, Integer> suggestedScores() {
        return suggestedScores;
    }

    /** Le nombre de Bottes d'arme au niveau 1. */
    public int masteryCount() {
        return traits.masteryCount();
    }

    /** La classe peut-elle prendre la botte de cette arme. */
    public boolean masters(SrdWeapon weapon) {
        return traits.masteryCount() > 0 && traits.masteryWeapons().test(weapon);
    }

    /** Les armes dont la classe peut prendre la botte, dans l'ordre du SRD. */
    public List<SrdWeapon> masteryWeapons() {
        return Arrays.stream(SrdWeapon.values()).filter(this::masters).toList();
    }

    /** La classe prend-elle un Style de combat au niveau 1. */
    public boolean takesFightingStyle() {
        return traits.fightingStyle();
    }

    /** Le nombre d'Expertises au niveau 1. */
    public int expertiseCount() {
        return traits.expertiseCount();
    }

    /** L'outil maîtrisé d'office, {@code null} sans. */
    public Tool tool() {
        return traits.tool();
    }

    /** Les catégories où la classe choisit l'outil qu'elle maîtrise, vides si elle n'en choisit pas. */
    public Set<Tool.Category> toolChoices() {
        return traits.toolChoices();
    }

    /** Les outils parmi lesquels la classe choisit, dans l'ordre du SRD. */
    public List<Tool> toolOptions() {
        return Arrays.stream(Tool.values()).filter(tool -> traits.toolChoices().contains(tool.category())).toList();
    }

    /** La caractéristique ajoutée par Défense sans armure (10 + Dex + elle), {@code null} sans. */
    public Ability unarmoredDefense() {
        return traits.unarmoredDefense();
    }

    /** La Défense sans armure tient-elle avec un bouclier. */
    public boolean unarmoredDefenseWithShield() {
        return traits.unarmoredShield();
    }

    /** La classe a-t-elle Arts martiaux. */
    public boolean martialArts() {
        return traits.martialArts();
    }

    /** Une arme de Moine : arme courante de corps à corps, ou arme de guerre de corps à corps Légère. */
    public static boolean monkWeapon(SrdWeapon weapon) {
        return weapon.category() == SIMPLE_MELEE || weapon.category() == MARTIAL_MELEE && weapon.has(SrdWeapon.Property.LIGHT);
    }

    /** La classe maîtrise-t-elle cette arme : sinon, pas de bonus de maîtrise à l'attaque (SRD 5.2.1, « Proficiency »). */
    public boolean proficient(SrdWeapon weapon) {
        return traits.weapons().test(weapon);
    }

    /**
     * La classe est-elle formée à cette armure ou au bouclier (SRD 5.2.1, « Armor Training ») : sans formation, un
     * bouclier ne donne pas son bonus de CA.
     */
    public boolean trained(SrdArmor armor) {
        return traits.armorTraining().contains(armor.category());
    }

    /** Les maîtrises d'arme et la formation aux armures, en texte. */
    public String proficiencies() {
        return proficiencies;
    }

    /** Les équipements de départ que la classe propose. */
    public List<StartingEquipment> equipments() {
        return Arrays.stream(StartingEquipment.values()).filter(equipment -> equipment.characterClass() == this).toList();
    }

    /** Le Moine maîtrise les armes courantes et les armes de guerre Légères. */
    private static boolean monkProficient(SrdWeapon weapon) {
        return weapon.category() == SIMPLE_MELEE || weapon.category() == SIMPLE_RANGED || weapon.has(SrdWeapon.Property.LIGHT);
    }

    /** Le Roublard maîtrise les armes courantes et les armes de guerre Finesse ou Légère. */
    private static boolean rogueProficient(SrdWeapon weapon) {
        return weapon.category() == SIMPLE_MELEE || weapon.category() == SIMPLE_RANGED
                || weapon.has(SrdWeapon.Property.FINESSE) || weapon.has(SrdWeapon.Property.LIGHT);
    }

    private static Map<Ability, Integer> scores(int str, int dex, int con, int intel, int wis, int cha) {
        Map<Ability, Integer> scores = new EnumMap<>(Ability.class);
        scores.put(Ability.STRENGTH, str);
        scores.put(Ability.DEXTERITY, dex);
        scores.put(Ability.CONSTITUTION, con);
        scores.put(Ability.INTELLIGENCE, intel);
        scores.put(Ability.WISDOM, wis);
        scores.put(Ability.CHARISMA, cha);
        return scores;
    }
}

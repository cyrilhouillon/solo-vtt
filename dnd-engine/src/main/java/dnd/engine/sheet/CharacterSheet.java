package dnd.engine.sheet;

import dnd.engine.state.CreatureProfile;
import dnd.engine.state.Weapon;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * La fiche d'un personnage D&D 2024 : ce que le joueur y écrit, et ce que les formules du SRD 5.2.1 en tirent
 * (modificateurs, sauvegardes, compétences, Perception passive, CA et attaques). Le nom du personnage n'y est pas : c'est
 * le titre de sa page dans le Carnet.
 *
 * <p>Une compétence absente de {@code skills} n'est pas maîtrisée. Vitesse en mètres. Taille et traits d'espèce sont du
 * texte, comme l'espèce ; les dons aussi. Les textes absents valent {@code ""}. CA et attaques ne sont pas gardées :
 * {@link CombatStats} les calcule de la classe, du Style, des caractéristiques, de l'inventaire, de l'ajustement de CA
 * ({@code armorBonus}) et des autres attaques saisies ({@code otherAttacks}, sorts…).</p>
 *
 * @param characterClass la classe, {@code null} tant qu'elle n'est pas choisie : ni CA ni attaques
 * @param fightingStyle  le Style de combat, seulement pour une classe qui en prend un
 * @param spellcastingAbility la caractéristique d'incantation, {@code null} sans : ni DD ni attaque de sort
 * @param cantrips       les sorts mineurs connus
 * @param spellbook      les sorts du grimoire (Magicien), vide sans grimoire
 * @param preparedSpells les sorts préparés, ceux que le personnage peut lancer ; la fiche ne les exige pas dans le grimoire
 */
public record CharacterSheet(CharacterClass characterClass, FightingStyle fightingStyle, int level, String species,
                             String size, String background,
                             Map<Ability, Integer> abilities, int proficiencyBonus,
                             Set<Ability> savingThrows, Map<Skill, Proficiency> skills,
                             int armorBonus, int hitPointMax, int hitPoints, String hitDice, double speed, int initiative,
                             List<SheetAttack> otherAttacks,
                             String features, String speciesTraits, String feats, Inventory inventory, String proficiencies,
                             String languages,
                             List<SpellSlots> spellSlots,
                             Ability spellcastingAbility, List<Spell> cantrips, List<Spell> spellbook,
                             List<Spell> preparedSpells) {

    public static final int MIN_SCORE = 1;
    public static final int MAX_SCORE = 30;
    public static final int MAX_LEVEL = 20;
    public static final int MAX_PROFICIENCY_BONUS = 10;
    /**
     * Le préfixe de l'id d'une autre attaque dans le profil : elle peut porter le nom d'une attaque calculée (« Dague »
     * saisie, Dague tenue) sans que le moteur les confonde.
     */
    static final String OTHER_ATTACK_ID = "autre:";

    public CharacterSheet {
        species = text(species);
        size = text(size);
        background = text(background);
        hitDice = text(hitDice);
        features = text(features);
        speciesTraits = text(speciesTraits);
        feats = text(feats);
        inventory = inventory == null ? Inventory.EMPTY : inventory;
        if (fightingStyle != null && (characterClass == null || !characterClass.takesFightingStyle())) {
            throw new IllegalArgumentException(characterClass == null ? "Choisir une classe avant un Style de combat"
                    : characterClass.label() + " : aucun Style de combat au niveau 1");
        }
        proficiencies = text(proficiencies);
        languages = text(languages);
        if (level < 1 || level > MAX_LEVEL) throw new IllegalArgumentException("Niveau entre 1 et %d : %d".formatted(MAX_LEVEL, level));
        abilities = abilities == null ? Map.of() : abilities;
        for (Ability ability : Ability.values()) {
            Integer score = abilities.get(ability);
            if (score == null) throw new IllegalArgumentException("Valeur manquante : " + ability.label());
            if (score < MIN_SCORE || score > MAX_SCORE) {
                throw new IllegalArgumentException("%s entre %d et %d : %d".formatted(ability.label(), MIN_SCORE, MAX_SCORE, score));
            }
        }
        abilities = Map.copyOf(new EnumMap<>(abilities));
        if (proficiencyBonus < 0 || proficiencyBonus > MAX_PROFICIENCY_BONUS) {
            throw new IllegalArgumentException("Bonus de maîtrise entre 0 et %d : %d".formatted(MAX_PROFICIENCY_BONUS, proficiencyBonus));
        }
        savingThrows = savingThrows == null || savingThrows.isEmpty() ? Set.of() : Set.copyOf(EnumSet.copyOf(savingThrows));
        skills = skills == null ? Map.of() : Map.copyOf(skills);
        if (hitPointMax < 0) throw new IllegalArgumentException("PV maximum négatifs : " + hitPointMax);
        if (hitPoints < 0) throw new IllegalArgumentException("PV actuels négatifs : " + hitPoints);
        if (!(speed >= 0)) throw new IllegalArgumentException("Vitesse négative : " + speed);
        otherAttacks = otherAttacks == null ? List.of() : List.copyOf(otherAttacks);
        if (otherAttacks.stream().map(SheetAttack::name).distinct().count() != otherAttacks.size()) {
            throw new IllegalArgumentException("Deux attaques portent le même nom");
        }
        spellSlots = spellSlots == null ? List.of() : List.copyOf(spellSlots);
        if (spellSlots.stream().map(SpellSlots::level).distinct().count() != spellSlots.size()) {
            throw new IllegalArgumentException("Un seul jeu d'emplacements par niveau de sort");
        }
        cantrips = spells("Sorts mineurs", cantrips, true);
        spellbook = spells("Grimoire", spellbook, false);
        preparedSpells = spells("Sorts préparés", preparedSpells, false);
    }

    /** Une liste de sorts saisie : sans inconnu ni doublon, de sorts mineurs ou de sorts de niveau 1 et plus. */
    private static List<Spell> spells(String what, List<Spell> spells, boolean cantrips) {
        if (spells == null) return List.of();
        if (spells.stream().anyMatch(Objects::isNull)) throw new IllegalArgumentException(what + " : sort inconnu");
        if (Set.copyOf(spells).size() != spells.size()) throw new IllegalArgumentException(what + " : un sort en double");
        for (Spell spell : spells) {
            if (spell.cantrip() != cantrips) {
                throw new IllegalArgumentException(cantrips ? "%s n’est pas un sort mineur".formatted(spell.label())
                        : "%s : %s est un sort mineur".formatted(what, spell.label()));
            }
        }
        return List.copyOf(spells);
    }

    /** La fiche d'un personnage qui vient d'être créé : niveau 1, toutes les valeurs à 10, bonus de maîtrise +2. */
    public static CharacterSheet blank() {
        Map<Ability, Integer> abilities = new EnumMap<>(Ability.class);
        for (Ability ability : Ability.values()) abilities.put(ability, 10);
        return new CharacterSheet(null, null, 1, "", "", "", abilities, 2, Set.of(), Map.of(),
                0, 0, 0, "", 9, 0, List.of(), "", "", "", Inventory.EMPTY, "", "", List.of(), null, List.of(), List.of(),
                List.of());
    }

    /** Cette fiche, avec ces PV actuels. */
    public CharacterSheet withHitPoints(int hitPoints) {
        return new CharacterSheet(characterClass, fightingStyle, level, species, size, background, abilities,
                proficiencyBonus, savingThrows, skills, armorBonus, hitPointMax, hitPoints, hitDice, speed, initiative,
                otherAttacks, features, speciesTraits, feats, inventory, proficiencies, languages, spellSlots,
                spellcastingAbility, cantrips, spellbook, preparedSpells);
    }

    /**
     * Cette fiche, avec un emplacement de ce niveau dépensé de plus, jamais au-delà du maximum ; inchangée sans
     * emplacement de ce niveau.
     */
    public CharacterSheet withSpellSlotExpended(int slotLevel) {
        List<SpellSlots> slots = spellSlots.stream()
                .map(slot -> slot.level() == slotLevel && slot.expended() < slot.max()
                        ? new SpellSlots(slot.level(), slot.max(), slot.expended() + 1) : slot)
                .toList();
        return new CharacterSheet(characterClass, fightingStyle, level, species, size, background, abilities,
                proficiencyBonus, savingThrows, skills, armorBonus, hitPointMax, hitPoints, hitDice, speed, initiative,
                otherAttacks, features, speciesTraits, feats, inventory, proficiencies, languages, slots,
                spellcastingAbility, cantrips, spellbook, preparedSpells);
    }

    /** Le DD de sauvegarde des sorts : 8 + modificateur d'incantation + bonus de maîtrise ; {@code null} sans incantation. */
    public Integer spellSaveDc() {
        return spellcastingAbility == null ? null : 8 + modifier(spellcastingAbility) + proficiencyBonus;
    }

    /** Le modificateur d'attaque des sorts : modificateur d'incantation + bonus de maîtrise ; {@code null} sans incantation. */
    public Integer spellAttackBonus() {
        return spellcastingAbility == null ? null : modifier(spellcastingAbility) + proficiencyBonus;
    }

    /** Le modificateur d'une caractéristique : (valeur − 10) / 2, arrondi à l'inférieur. */
    public int modifier(Ability ability) {
        return Math.floorDiv(abilities.get(ability) - 10, 2);
    }

    /** Le bonus de sauvegarde : le modificateur, plus le bonus de maîtrise si la sauvegarde est maîtrisée. */
    public int savingThrow(Ability ability) {
        return modifier(ability) + (savingThrows.contains(ability) ? proficiencyBonus : 0);
    }

    /** La maîtrise d'une compétence, {@link Proficiency#NONE} si elle n'est pas notée. */
    public Proficiency proficiency(Skill skill) {
        return skills.getOrDefault(skill, Proficiency.NONE);
    }

    /** Le bonus d'une compétence : le modificateur de sa caractéristique, plus le bonus de maîtrise, deux fois en expertise. */
    public int skill(Skill skill) {
        return modifier(skill.ability()) + proficiency(skill).multiplier() * proficiencyBonus;
    }

    /** Perception passive : 10 + le bonus de Perception. */
    public int passivePerception() {
        return 10 + skill(Skill.PERCEPTION);
    }

    /** La CA, calculée ({@link CombatStats#armorClass}) ; {@code null} sans classe. */
    public Integer armorClass() {
        return characterClass == null ? null : CombatStats.armorClass(this);
    }

    /** Les attaques, calculées puis saisies ({@link CombatStats#attacks}) ; {@code null} sans classe. */
    public List<SheetAttack> attacks() {
        return characterClass == null ? null : CombatStats.attacks(this);
    }

    /**
     * La créature que joue cette fiche, telle qu'elle est maintenant : CA, PV maximum et actuels, vitesse, initiative, une
     * arme par attaque, ses sorts mineurs et préparés (par leur id) et ses emplacements restants (maximum − dépensés).
     *
     * @throws IllegalArgumentException si la fiche ne peut pas jouer (sans classe, PV maximum à 0, PV actuels au-dessus
     *     du maximum)
     */
    public CreatureProfile profile() {
        if (characterClass == null) throw new IllegalArgumentException("Aucune classe : la choisir avant de poser la fiche");
        if (hitPointMax == 0) throw new IllegalArgumentException("PV maximum à 0 : compléter la fiche avant de la poser");
        List<Weapon> weapons = new ArrayList<>(CombatStats.computedAttacks(this).stream().map(SheetAttack::weapon).toList());
        otherAttacks.forEach(attack -> weapons.add(attack.weapon(OTHER_ATTACK_ID + attack.name())));
        Set<String> spells = new LinkedHashSet<>();
        cantrips.forEach(spell -> spells.add(spell.id()));
        preparedSpells.forEach(spell -> spells.add(spell.id()));
        int highest = spellSlots.stream().mapToInt(SpellSlots::level).max().orElse(0);
        List<Integer> remaining = new ArrayList<>();
        for (int slotLevel = 1; slotLevel <= highest; slotLevel++) {
            int at = slotLevel;
            remaining.add(spellSlots.stream().filter(slot -> slot.level() == at)
                    .mapToInt(slot -> slot.max() - slot.expended()).findFirst().orElse(0));
        }
        return new CreatureProfile(armorClass(), hitPointMax, hitPoints, speed, initiative, weapons, Set.of(), spells,
                remaining);
    }

    /** Un texte saisi, sans espaces aux bords ; {@code ""} s'il est absent. */
    static String text(String value) {
        return value == null ? "" : value.strip();
    }
}

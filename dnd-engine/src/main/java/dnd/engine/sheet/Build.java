package dnd.engine.sheet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Les choix de la création guidée d'un personnage : classe, méthode et valeurs de caractéristique, compétences, Style de
 * combat, équipement de départ, Bottes d'arme, espèce, historique, Expertises et outil de classe ({@code null} tant qu'ils
 * ne sont pas choisis). Le record n'impose rien : {@link #validate} dit ce qui est impossible.
 */
public record Build(CharacterClass characterClass, AbilityMethod method, Map<Ability, Integer> scores,
                    Set<Skill> skills, FightingStyle fightingStyle, StartingEquipment equipment,
                    List<SrdWeapon> masteries, SpeciesChoice species, BackgroundChoice background,
                    Set<Skill> expertise, Tool tool) {

    public Build {
        scores = scores == null || scores.isEmpty() ? Map.of() : Collections.unmodifiableMap(new EnumMap<>(scores));
        // Des copies modifiables enveloppées, jamais Set.of() ni List.of() : leur contains(null) lève une
        // NullPointerException, alors que validate y cherche null pour refuser un id inconnu (400).
        skills = Collections.unmodifiableSet(skills == null ? new HashSet<>() : new HashSet<>(skills));
        masteries = Collections.unmodifiableList(masteries == null ? new ArrayList<>() : new ArrayList<>(masteries));
        expertise = Collections.unmodifiableSet(expertise == null ? new HashSet<>() : new HashSet<>(expertise));
    }

    /** Un Guerrier jouable d'emblée : répartition standard suggérée, Défense, équipement A et ses trois armes en Bottes. */
    public static Build fighter() {
        return suggested(CharacterClass.FIGHTER);
    }

    /** Cette construction, avec cette espèce. */
    public Build withSpecies(SpeciesChoice species) {
        return new Build(characterClass, method, scores, skills, fightingStyle, equipment, masteries, species, background,
                expertise, tool);
    }

    /** Cette construction, avec cet historique. */
    public Build withBackground(BackgroundChoice background) {
        return new Build(characterClass, method, scores, skills, fightingStyle, equipment, masteries, species, background,
                expertise, tool);
    }

    /** Cette construction, avec ces Expertises. */
    public Build withExpertise(Set<Skill> expertise) {
        return new Build(characterClass, method, scores, skills, fightingStyle, equipment, masteries, species, background,
                expertise, tool);
    }

    /** Cette construction, avec cet outil de classe. */
    public Build withTool(Tool tool) {
        return new Build(characterClass, method, scores, skills, fightingStyle, equipment, masteries, species, background,
                expertise, tool);
    }

    /** Les valeurs de caractéristique, augmentations de l'historique comprises. */
    public Map<Ability, Integer> abilities() {
        if (background == null || scores.isEmpty()) return scores;
        Map<Ability, Integer> abilities = new EnumMap<>(scores);
        background.increases().forEach((ability, increase) -> abilities.merge(ability, increase, Integer::sum));
        return Collections.unmodifiableMap(abilities);
    }

    /** L'inventaire de départ : l'équipement de la classe, puis celui de l'historique ; leurs pièces d'or additionnées. */
    public Inventory startingInventory() {
        Inventory inventory = equipment.inventory(tool);
        return background == null ? inventory : inventory.plus(background.inventory());
    }

    /** Les compétences que l'historique rend maîtrisées, aucune sans historique. */
    public Set<Skill> backgroundSkills() {
        return background == null || background.background() == null ? Set.of() : background.background().skills();
    }

    /** Les compétences que la construction rend maîtrisées : classe, historique, espèce et don de l'espèce. */
    public Set<Skill> proficientSkills() {
        Set<Skill> proficient = EnumSet.noneOf(Skill.class);
        proficient.addAll(skills);
        proficient.addAll(backgroundSkills());
        if (species != null) proficient.addAll(species.grantedSkills());
        return proficient;
    }

    /**
     * La construction d'où part une classe quand le joueur la choisit : répartition standard suggérée, premier équipement,
     * ses premières armes que la classe peut viser en Bottes, Défense si la classe prend un Style.
     */
    public static Build suggested(CharacterClass characterClass) {
        StartingEquipment equipment = characterClass.equipments().getFirst();
        List<SrdWeapon> masteries = equipment.weapons().stream().filter(characterClass::masters)
                .limit(characterClass.masteryCount()).toList();
        return new Build(characterClass, AbilityMethod.STANDARD_ARRAY, characterClass.suggestedScores(), Set.of(),
                characterClass.takesFightingStyle() ? FightingStyle.DEFENSE : null, equipment, masteries, null, null,
                Set.of(), null);
    }

    /**
     * Refuse une construction impossible, avec la raison.
     *
     * @param rolls les jets de caractéristique de la fiche, s'il y en a
     */
    public void validate(List<AbilityRoll> rolls) {
        if (characterClass == null) throw new IllegalArgumentException("Choisir une classe");
        if (method == null) throw new IllegalArgumentException("Choisir une méthode de caractéristiques");
        method.validate(scores, rolls);
        if (skills.contains(null)) throw new IllegalArgumentException("Compétence inconnue");
        // Aucune méthode ne dépasse 18 : une augmentation de 2 ne porte jamais une valeur au-dessus de 20.
        if (background != null) background.validate();
        if (skills.size() > characterClass.skillCount()) {
            throw new IllegalArgumentException("%s : %d compétences au plus".formatted(characterClass.label(), characterClass.skillCount()));
        }
        for (Skill skill : skills) {
            if (!characterClass.skillChoices().contains(skill)) {
                throw new IllegalArgumentException("%s hors des compétences du %s".formatted(skill.label(), characterClass.label()));
            }
            if (backgroundSkills().contains(skill)) {
                throw new IllegalArgumentException("%s déjà donnée par l’historique".formatted(skill.label()));
            }
        }
        String name = characterClass.label();
        if (characterClass.takesFightingStyle() && fightingStyle == null) {
            throw new IllegalArgumentException("Choisir un Style de combat");
        }
        if (!characterClass.takesFightingStyle() && fightingStyle != null) {
            throw new IllegalArgumentException(name + " : aucun Style de combat au niveau 1");
        }
        if (equipment == null || equipment.characterClass() != characterClass) {
            throw new IllegalArgumentException("Choisir un équipement de départ du " + characterClass.label());
        }
        if (masteries.contains(null)) throw new IllegalArgumentException("Botte d'arme inconnue");
        if (masteries.size() != characterClass.masteryCount() || Set.copyOf(masteries).size() != masteries.size()) {
            throw new IllegalArgumentException("Bottes d'arme : %d types d'arme différents".formatted(characterClass.masteryCount()));
        }
        for (SrdWeapon weapon : masteries) {
            if (!characterClass.masters(weapon)) {
                throw new IllegalArgumentException("%s : pas de Botte d'arme pour %s".formatted(name, weapon.label()));
            }
        }
        if (species != null) species.validate(skills, backgroundSkills());
        if (species != null && species.originFeat() != null && background != null) {
            checkDistinctFeats(background.feat(), species.originFeat());
        }
        checkExpertise();
        checkTool();
    }

    /** Une Expertise porte sur une compétence maîtrisée (SRD 5.2.1, « Expertise »), au plus le nombre de la classe. */
    private void checkExpertise() {
        String name = characterClass.label();
        if (expertise.contains(null)) throw new IllegalArgumentException("Expertise inconnue");
        if (expertise.size() > characterClass.expertiseCount()) {
            throw new IllegalArgumentException(characterClass.expertiseCount() == 0
                    ? name + " : aucune Expertise au niveau 1"
                    : "%s : %d Expertises au plus".formatted(name, characterClass.expertiseCount()));
        }
        Set<Skill> proficient = proficientSkills();
        for (Skill skill : expertise) {
            if (!proficient.contains(skill)) {
                throw new IllegalArgumentException("Expertise : %s n’est pas maîtrisée".formatted(skill.label()));
            }
        }
    }

    /**
     * L'outil de classe se choisit seulement si la classe en choisit un, dans ses catégories, et pas celui que l'historique
     * donne déjà : il ne donnerait aucune maîtrise de plus (comme une compétence déjà donnée par l'historique).
     */
    private void checkTool() {
        if (tool == null) return;
        if (!characterClass.toolChoices().contains(tool.category())) {
            throw new IllegalArgumentException(characterClass.toolChoices().isEmpty()
                    ? characterClass.label() + " : aucun outil à choisir"
                    : "%s : %s hors des outils au choix".formatted(characterClass.label(), tool.label()));
        }
        if (background != null && background.background() != null && background.background().tool() == tool) {
            throw new IllegalArgumentException("%s déjà donné par l’historique".formatted(tool.label()));
        }
    }

    /**
     * Un don ne se prend qu'une fois (SRD 5.2.1, « Feats ») : Polyvalent ne reprend pas celui de l'historique. Doué est
     * répétable ; Initié à la magie aussi, avec une autre liste de sorts.
     */
    private static void checkDistinctFeats(OriginFeatChoice fromBackground, OriginFeatChoice versatile) {
        OriginFeat feat = versatile.feat();
        if (feat != fromBackground.feat() || feat == OriginFeat.SKILLED) return;
        if (feat != OriginFeat.MAGIC_INITIATE) {
            throw new IllegalArgumentException("Polyvalent : %s déjà donné par l’historique".formatted(feat.label()));
        }
        if (versatile.spellList() == fromBackground.spellList()) {
            throw new IllegalArgumentException("Polyvalent : %s (%s) déjà donné par l’historique ; choisir une autre liste"
                    .formatted(feat.label(), fromBackground.spellList().label()));
        }
    }
}

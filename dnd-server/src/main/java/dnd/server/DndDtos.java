package dnd.server;

import dnd.engine.sheet.Ability;
import dnd.engine.sheet.CharacterSheet;
import dnd.engine.sheet.Proficiency;
import dnd.engine.sheet.SheetAttack;
import dnd.engine.sheet.Skill;
import dnd.engine.sheet.SpellSlots;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Ce que les routes du module D&D reçoivent et rendent. */
public final class DndDtos {

    private DndDtos() {
    }

    /** La page « Personnages joueurs », racine de la section du module. */
    public record SectionsDto(String playersPageId) {}

    /** Le nom d'un nouveau personnage : le titre de sa page. */
    public record NewSheetRequest(String title) {}

    /** Une attaque saisie. */
    public record AttackDto(String name, int bonus, String damage, String damageType, String range) {}

    /** Les emplacements d'un niveau de sort. */
    public record SpellSlotsDto(int level, int max, int expended) {}

    /**
     * Les valeurs saisies d'une fiche ; {@code skills} ne liste que les compétences maîtrisées, {@code speed} est en
     * mètres.
     */
    public record SheetValuesDto(String className, int level, String species, String background,
                                 Map<Ability, Integer> abilities, int proficiencyBonus,
                                 Set<Ability> savingThrows, Map<Skill, Proficiency> skills,
                                 int armorClass, int hitPointMax, int hitPoints, String hitDice, double speed, int initiative,
                                 List<AttackDto> attacks,
                                 String features, String equipment, String proficiencies, String languages,
                                 List<SpellSlotsDto> spellSlots) {

        static SheetValuesDto of(CharacterSheet sheet) {
            return new SheetValuesDto(sheet.className(), sheet.level(), sheet.species(), sheet.background(),
                    new EnumMap<>(sheet.abilities()), sheet.proficiencyBonus(), sheet.savingThrows(), sheet.skills(),
                    sheet.armorClass(), sheet.hitPointMax(), sheet.hitPoints(), sheet.hitDice(), sheet.speed(), sheet.initiative(),
                    sheet.attacks().stream().map(a -> new AttackDto(a.name(), a.bonus(), a.damage(), a.damageType(), a.range())).toList(),
                    sheet.features(), sheet.equipment(), sheet.proficiencies(), sheet.languages(),
                    sheet.spellSlots().stream().map(s -> new SpellSlotsDto(s.level(), s.max(), s.expended())).toList());
        }

        /**
         * La fiche de ces valeurs.
         *
         * @throws IllegalArgumentException si une valeur est hors bornes, avec la raison
         */
        CharacterSheet toSheet() {
            return new CharacterSheet(className, level, species, background, abilities, proficiencyBonus,
                    savingThrows, skills == null ? Map.of() : withoutNone(skills),
                    armorClass, hitPointMax, hitPoints, hitDice, speed, initiative,
                    attacks == null ? List.of() : attacks.stream()
                            .map(a -> new SheetAttack(a.name(), a.bonus(), a.damage(), a.damageType(), a.range())).toList(),
                    features, equipment, proficiencies, languages,
                    spellSlots == null ? List.of() : spellSlots.stream()
                            .map(s -> new SpellSlots(s.level(), s.max(), s.expended())).toList());
        }

        private static Map<Skill, Proficiency> withoutNone(Map<Skill, Proficiency> skills) {
            Map<Skill, Proficiency> kept = new EnumMap<>(Skill.class);
            skills.forEach((skill, proficiency) -> {
                if (skill != null && proficiency != null && proficiency != Proficiency.NONE) kept.put(skill, proficiency);
            });
            return kept;
        }
    }

    /** Ce que les formules du SRD tirent des valeurs saisies. */
    public record DerivedDto(Map<Ability, Integer> modifiers, Map<Ability, Integer> savingThrows,
                             Map<Skill, Integer> skills, int passivePerception) {

        static DerivedDto of(CharacterSheet sheet) {
            Map<Ability, Integer> modifiers = new EnumMap<>(Ability.class);
            Map<Ability, Integer> saves = new EnumMap<>(Ability.class);
            for (Ability ability : Ability.values()) {
                modifiers.put(ability, sheet.modifier(ability));
                saves.put(ability, sheet.savingThrow(ability));
            }
            Map<Skill, Integer> skills = new EnumMap<>(Skill.class);
            for (Skill skill : Skill.values()) skills.put(skill, sheet.skill(skill));
            return new DerivedDto(modifiers, saves, skills, sheet.passivePerception());
        }
    }

    /** Une fiche : la page qui la montre, ses valeurs saisies et leurs dérivés. */
    public record SheetDto(String pageId, SheetValuesDto values, DerivedDto derived) {

        static SheetDto of(String pageId, CharacterSheet sheet) {
            return new SheetDto(pageId, SheetValuesDto.of(sheet), DerivedDto.of(sheet));
        }
    }
}

package dnd.engine.sheet;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static dnd.engine.sheet.Item.named;
import static dnd.engine.sheet.Item.weapon;

/** Les quatre historiques du SRD 5.2.1 (« Character Backgrounds »). */
public enum Background {
    ACOLYTE("Acolyte", EnumSet.of(Ability.INTELLIGENCE, Ability.WISDOM, Ability.CHARISMA),
            OriginFeat.MAGIC_INITIATE, SpellList.CLERIC, EnumSet.of(Skill.INSIGHT, Skill.RELIGION), Tool.CALLIGRAPHERS_SUPPLIES, 8,
            named("Matériel de calligraphe", 1), named("Livre (de prières)", 1), named("Symbole sacré", 1),
            named("Parchemin (feuille)", 10), named("Robe", 1)),
    CRIMINAL("Criminel", EnumSet.of(Ability.DEXTERITY, Ability.CONSTITUTION, Ability.INTELLIGENCE),
            OriginFeat.ALERT, null, EnumSet.of(Skill.STEALTH, Skill.SLEIGHT_OF_HAND), Tool.THIEVES_TOOLS, 16,
            weapon(SrdWeapon.DAGGER, 2), named("Outils de voleur", 1), named("Bourse", 2), named("Pied-de-biche", 1),
            named("Tenue de voyage", 1)),
    SAGE("Sage", EnumSet.of(Ability.CONSTITUTION, Ability.INTELLIGENCE, Ability.WISDOM),
            OriginFeat.MAGIC_INITIATE, SpellList.WIZARD, EnumSet.of(Skill.ARCANA, Skill.HISTORY), Tool.CALLIGRAPHERS_SUPPLIES, 8,
            weapon(SrdWeapon.QUARTERSTAFF, 1), named("Matériel de calligraphe", 1), named("Livre (d’histoire)", 1),
            named("Parchemin (feuille)", 8), named("Robe", 1)),
    SOLDIER("Soldat", EnumSet.of(Ability.STRENGTH, Ability.DEXTERITY, Ability.CONSTITUTION),
            OriginFeat.SAVAGE_ATTACKER, null, EnumSet.of(Skill.ATHLETICS, Skill.INTIMIDATION), null, 14,
            weapon(SrdWeapon.SHORTBOW, 1), named("Flèches", 20), named("Carquois", 1), weapon(SrdWeapon.SPEAR, 1),
            named("Trousse de soins", 1), named("Tenue de voyage", 1));

    /** La boîte de jeux du Soldat dans l'option proposée, qui vaut pour toute variante choisie. */
    private static final String SAME_GAMING_SET = "Boîte de jeux (même type)";

    private final String label;
    private final Set<Ability> abilities;
    private final OriginFeat feat;
    private final SpellList spellList;
    private final Set<Skill> skills;
    private final Tool tool;
    private final int gold;
    private final List<Item> items;

    Background(String label, Set<Ability> abilities, OriginFeat feat, SpellList spellList, Set<Skill> skills, Tool tool,
               int gold, Item... items) {
        this.label = label;
        this.abilities = Set.copyOf(abilities);
        this.feat = feat;
        this.spellList = spellList;
        this.skills = Set.copyOf(skills);
        this.tool = tool;
        this.gold = gold;
        this.items = List.of(items);
    }

    public String label() {
        return label;
    }

    /** Les trois caractéristiques que l'historique augmente. */
    public Set<Ability> abilities() {
        return abilities;
    }

    /** Le don d'origines de l'historique. */
    public OriginFeat feat() {
        return feat;
    }

    /** La liste de sorts d'Initié à la magie, {@code null} pour un autre don. */
    public SpellList spellList() {
        return spellList;
    }

    /** Les deux compétences maîtrisées. */
    public Set<Skill> skills() {
        return skills;
    }

    /** L'outil maîtrisé, {@code null} quand il se choisit (boîte de jeux du Soldat). */
    public Tool tool() {
        return tool;
    }

    /** L'historique fait-il choisir une boîte de jeux. */
    public boolean choosesGamingSet() {
        return tool == null;
    }

    /** Les armes de l'équipement A, une fois chacune. */
    public List<SrdWeapon> weapons() {
        return items.stream().map(Item::weapon).filter(w -> w != null).toList();
    }

    /** Les pièces d'or de l'équipement A. */
    public int gold() {
        return gold;
    }

    /**
     * L'inventaire de l'équipement A ou B (50 po).
     *
     * @param gamingSet la boîte de jeux du Soldat, {@code null} : à choisir
     */
    public Inventory inventory(BackgroundEquipment equipment, GamingSet gamingSet) {
        return inventory(equipment, "Boîte de jeux (" + (gamingSet == null ? "à choisir" : gamingSet.label()) + ")");
    }

    /** L'équipement A ou B tel que l'option se propose : la boîte de jeux du Soldat désignée sans être nommée. */
    public String equipmentText(BackgroundEquipment equipment) {
        return inventory(equipment, SAME_GAMING_SET).text();
    }

    private Inventory inventory(BackgroundEquipment equipment, String gamingSet) {
        if (equipment != BackgroundEquipment.A) return new Inventory(List.of(), BackgroundEquipment.GOLD_B);
        List<Item> all = new ArrayList<>(items);
        if (choosesGamingSet()) all.add(Item.named(gamingSet, 1));
        return new Inventory(all, gold);
    }
}

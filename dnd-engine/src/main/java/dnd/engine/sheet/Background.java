package dnd.engine.sheet;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Les quatre historiques du SRD 5.2.1 (« Character Backgrounds »). */
public enum Background {
    ACOLYTE("Acolyte", EnumSet.of(Ability.INTELLIGENCE, Ability.WISDOM, Ability.CHARISMA),
            OriginFeat.MAGIC_INITIATE, SpellList.CLERIC, EnumSet.of(Skill.INSIGHT, Skill.RELIGION), "matériel de calligraphe",
            "Matériel de calligraphe, livre (de prières), symbole sacré, parchemin (10 feuilles), robe", List.of(), 8),
    CRIMINAL("Criminel", EnumSet.of(Ability.DEXTERITY, Ability.CONSTITUTION, Ability.INTELLIGENCE),
            OriginFeat.ALERT, null, EnumSet.of(Skill.STEALTH, Skill.SLEIGHT_OF_HAND), "outils de voleur",
            "2 dagues, outils de voleur, 2 bourses, pied-de-biche, tenue de voyage", List.of(SrdWeapon.DAGGER), 16),
    SAGE("Sage", EnumSet.of(Ability.CONSTITUTION, Ability.INTELLIGENCE, Ability.WISDOM),
            OriginFeat.MAGIC_INITIATE, SpellList.WIZARD, EnumSet.of(Skill.ARCANA, Skill.HISTORY), "matériel de calligraphe",
            "Bâton de combat, matériel de calligraphe, livre (d’histoire), parchemin (8 feuilles), robe",
            List.of(SrdWeapon.QUARTERSTAFF), 8),
    SOLDIER("Soldat", EnumSet.of(Ability.STRENGTH, Ability.DEXTERITY, Ability.CONSTITUTION),
            OriginFeat.SAVAGE_ATTACKER, null, EnumSet.of(Skill.ATHLETICS, Skill.INTIMIDATION), null,
            "Arc court, 20 flèches, carquois, lance, boîte de jeux (même type), trousse de soins, tenue de voyage",
            List.of(SrdWeapon.SHORTBOW, SrdWeapon.SPEAR), 14);

    private final String label;
    private final Set<Ability> abilities;
    private final OriginFeat feat;
    private final SpellList spellList;
    private final Set<Skill> skills;
    private final String tool;
    private final String items;
    private final List<SrdWeapon> weapons;
    private final int gold;

    Background(String label, Set<Ability> abilities, OriginFeat feat, SpellList spellList, Set<Skill> skills, String tool,
               String items, List<SrdWeapon> weapons, int gold) {
        this.label = label;
        this.abilities = Set.copyOf(abilities);
        this.feat = feat;
        this.spellList = spellList;
        this.skills = Set.copyOf(skills);
        this.tool = tool;
        this.items = items;
        this.weapons = weapons;
        this.gold = gold;
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
    public String tool() {
        return tool;
    }

    /** L'historique fait-il choisir une boîte de jeux. */
    public boolean choosesGamingSet() {
        return tool == null;
    }

    /** Les objets de l'équipement A, sans les pièces d'or. */
    public String items() {
        return items;
    }

    /** Les armes de l'équipement A, une fois chacune. */
    public List<SrdWeapon> weapons() {
        return weapons;
    }

    /** Les pièces d'or de l'équipement A. */
    public int gold() {
        return gold;
    }

    /** L'équipement A ou B tel qu'il s'écrit sur la fiche. */
    public String equipmentText(BackgroundEquipment equipment) {
        return equipment == BackgroundEquipment.A ? items + ", " + gold + " po" : BackgroundEquipment.GOLD_B + " po";
    }
}

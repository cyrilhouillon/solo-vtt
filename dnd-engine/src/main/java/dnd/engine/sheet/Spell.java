package dnd.engine.sheet;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static dnd.engine.sheet.Spell.School.ABJURATION;
import static dnd.engine.sheet.Spell.School.CONJURATION;
import static dnd.engine.sheet.Spell.School.DIVINATION;
import static dnd.engine.sheet.Spell.School.ENCHANTMENT;
import static dnd.engine.sheet.Spell.School.EVOCATION;
import static dnd.engine.sheet.Spell.School.ILLUSION;
import static dnd.engine.sheet.Spell.School.NECROMANCY;
import static dnd.engine.sheet.Spell.School.TRANSMUTATION;

/**
 * Les sorts que la fiche connaît (SRD 5.2.1, « Liste des sorts de Magicien ») : sorts mineurs (niveau 0) et sorts du 1er
 * niveau. Seule la liste du Magicien est portée ; les autres listes viendront avec leurs classes. L'id est celui du SRD,
 * que le moteur lit ({@code shield} pour Bouclier, {@link dnd.engine.rules.ShieldReaction#SPELL}).
 */
public enum Spell {
    ACID_SPLASH("acid-splash", "Aspersion acide", 0, EVOCATION, false, false),
    POISON_SPRAY("poison-spray", "Bouffée de poison", 0, NECROMANCY, false, false),
    CHILL_TOUCH("chill-touch", "Contact glacial", 0, NECROMANCY, false, false),
    TRUE_STRIKE("true-strike", "Coup au but", 0, DIVINATION, false, false),
    ELEMENTALISM("elementalism", "Élémentalisme", 0, TRANSMUTATION, false, false),
    MINOR_ILLUSION("minor-illusion", "Illusion mineure", 0, ILLUSION, false, false),
    LIGHT("light", "Lumière", 0, EVOCATION, false, false),
    DANCING_LIGHTS("dancing-lights", "Lumières dansantes", 0, ILLUSION, true, false),
    MAGE_HAND("mage-hand", "Main du mage", 0, CONJURATION, false, false),
    MESSAGE("message", "Message", 0, TRANSMUTATION, false, false),
    SHOCKING_GRASP("shocking-grasp", "Poigne électrique", 0, EVOCATION, false, false),
    PRESTIDIGITATION("prestidigitation", "Prestidigitation", 0, TRANSMUTATION, false, false),
    RAY_OF_FROST("ray-of-frost", "Rayon de givre", 0, EVOCATION, false, false),
    MENDING("mending", "Réparation", 0, TRANSMUTATION, false, false),
    FIRE_BOLT("fire-bolt", "Trait de feu", 0, EVOCATION, false, false),

    ALARM("alarm", "Alarme", 1, ABJURATION, false, true),
    FIND_FAMILIAR("find-familiar", "Appel de familier", 1, CONJURATION, false, true),
    MAGE_ARMOR("mage-armor", "Armure du mage", 1, ABJURATION, false, false),
    SHIELD("shield", "Bouclier", 1, ABJURATION, false, false),
    CHARM_PERSON("charm-person", "Charme-personne", 1, ENCHANTMENT, false, false),
    COMPREHEND_LANGUAGES("comprehend-languages", "Compréhension des langues", 1, DIVINATION, false, true),
    COLOR_SPRAY("color-spray", "Couleurs dansantes", 1, ILLUSION, false, false),
    ICE_KNIFE("ice-knife", "Couteau de glace", 1, CONJURATION, false, false),
    DISGUISE_SELF("disguise-self", "Déguisement", 1, ILLUSION, false, false),
    DETECT_MAGIC("detect-magic", "Détection de la magie", 1, DIVINATION, true, true),
    FLOATING_DISK("floating-disk", "Disque flottant", 1, CONJURATION, false, true),
    FEATHER_FALL("feather-fall", "Feuille morte", 1, TRANSMUTATION, false, false),
    HIDEOUS_LAUGHTER("hideous-laughter", "Fou rire", 1, ENCHANTMENT, true, false),
    GREASE("grease", "Graisse", 1, CONJURATION, false, false),
    LONGSTRIDER("longstrider", "Grande foulée", 1, TRANSMUTATION, false, false),
    IDENTIFY("identify", "Identification", 1, DIVINATION, false, true),
    SILENT_IMAGE("silent-image", "Image silencieuse", 1, ILLUSION, true, false),
    BURNING_HANDS("burning-hands", "Mains brûlantes", 1, EVOCATION, false, false),
    FOG_CLOUD("fog-cloud", "Nappe de brouillard", 1, CONJURATION, true, false),
    CHROMATIC_ORB("chromatic-orb", "Orbe chromatique", 1, EVOCATION, false, false),
    MAGIC_MISSILE("magic-missile", "Projectile magique", 1, EVOCATION, false, false),
    PROTECTION_FROM_EVIL_AND_GOOD("protection-from-evil-and-good", "Protection contre le mal et le bien", 1, ABJURATION, true, false),
    RAY_OF_SICKNESS("ray-of-sickness", "Rayon empoisonné", 1, NECROMANCY, false, false),
    EXPEDITIOUS_RETREAT("expeditious-retreat", "Repli expéditif", 1, TRANSMUTATION, true, false),
    JUMP("jump", "Saut", 1, TRANSMUTATION, false, false),
    UNSEEN_SERVANT("unseen-servant", "Serviteur invisible", 1, CONJURATION, false, true),
    FALSE_LIFE("false-life", "Simulacre de vie", 1, NECROMANCY, false, false),
    SLEEP("sleep", "Sommeil", 1, ENCHANTMENT, true, false),
    ILLUSORY_SCRIPT("illusory-script", "Texte illusoire", 1, ILLUSION, false, true),
    THUNDERWAVE("thunderwave", "Vague tonnante", 1, EVOCATION, false, false);

    /** Les écoles de magie. */
    public enum School {
        ABJURATION("Abjuration"), CONJURATION("Invocation"), DIVINATION("Divination"), ENCHANTMENT("Enchantement"),
        EVOCATION("Évocation"), ILLUSION("Illusion"), NECROMANCY("Nécromancie"), TRANSMUTATION("Transmutation");

        private final String label;

        School(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    private final String id;
    private final String label;
    private final int level;
    private final School school;
    private final boolean concentration;
    private final boolean ritual;
    private final Set<SpellList> lists;

    Spell(String id, String label, int level, School school, boolean concentration, boolean ritual) {
        this.id = id;
        this.label = label;
        this.level = level;
        this.school = school;
        this.concentration = concentration;
        this.ritual = ritual;
        this.lists = EnumSet.of(SpellList.WIZARD);
    }

    /** L'id du SRD, que le moteur lit. */
    public String id() {
        return id;
    }

    public String label() {
        return label;
    }

    /** Le niveau du sort ; 0 pour un sort mineur. */
    public int level() {
        return level;
    }

    public boolean cantrip() {
        return level == 0;
    }

    public School school() {
        return school;
    }

    public boolean concentration() {
        return concentration;
    }

    public boolean ritual() {
        return ritual;
    }

    /** Les listes de classe qui ont ce sort. */
    public Set<SpellList> lists() {
        return lists;
    }

    /** Les sorts d'une liste et d'un niveau, dans l'ordre du SRD. */
    public static List<Spell> of(SpellList list, int level) {
        return Arrays.stream(values()).filter(spell -> spell.level == level && spell.lists.contains(list)).toList();
    }
}

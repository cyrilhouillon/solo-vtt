package dnd.engine.state;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;

/**
 * Une créature. Les méthodes qui modifient l'état (section « Mutations ») ne doivent être
 * appelées QUE depuis {@code GameEvent.apply}, jamais directement par une règle :
 * c'est ce qui garantit que le journal d'événements suffit à reconstruire la partie.
 */
public final class Creature {

    private final String id;
    private final String name;
    private final int baseArmorClass;
    private final int maxHitPoints;
    private final double speed;             // en mètres
    private final List<Weapon> weapons;
    private final Set<DamageType> resistances;
    private final Set<DamageType> vulnerabilities;
    private final Set<Condition> conditions;
    private final Set<String> effects;      // effets actifs, ex. "bless"
    private final Set<String> features;     // capacités, ex. "dueling"
    private final Set<String> spellsKnown;  // ex. "shield"

    private int hitPoints;
    private boolean reactionAvailable;
    private final int[] spellSlots;                      // index = niveau (1 à 9)
    private final Map<String, Integer> armorClassBonuses; // source -> bonus

    private Creature(Builder b) {
        this.id = b.id;
        this.name = b.name;
        this.baseArmorClass = b.armorClass;
        this.maxHitPoints = b.hitPoints;
        this.hitPoints = b.hitPoints;
        this.speed = b.speed;
        this.weapons = List.copyOf(b.weapons);
        this.resistances = Set.copyOf(b.resistances);
        this.vulnerabilities = Set.copyOf(b.vulnerabilities);
        this.conditions = b.conditions.isEmpty() ? EnumSet.noneOf(Condition.class) : EnumSet.copyOf(b.conditions);
        this.effects = new HashSet<>(b.effects);
        this.features = Set.copyOf(b.features);
        this.spellsKnown = Set.copyOf(b.spellsKnown);
        this.reactionAvailable = true;
        this.spellSlots = b.spellSlots.clone();
        this.armorClassBonuses = new LinkedHashMap<>();
    }

    private Creature(Creature o) {
        this.id = o.id;
        this.name = o.name;
        this.baseArmorClass = o.baseArmorClass;
        this.maxHitPoints = o.maxHitPoints;
        this.hitPoints = o.hitPoints;
        this.speed = o.speed;
        this.weapons = o.weapons;
        this.resistances = o.resistances;
        this.vulnerabilities = o.vulnerabilities;
        this.conditions = o.conditions.isEmpty() ? EnumSet.noneOf(Condition.class) : EnumSet.copyOf(o.conditions);
        this.effects = new HashSet<>(o.effects);
        this.features = o.features;
        this.spellsKnown = o.spellsKnown;
        this.reactionAvailable = o.reactionAvailable;
        this.spellSlots = o.spellSlots.clone();
        this.armorClassBonuses = new LinkedHashMap<>(o.armorClassBonuses);
    }

    public Creature copy() {
        return new Creature(this);
    }

    public static Builder builder(String id, String name) {
        return new Builder(id, name);
    }

    // --- Lecture -------------------------------------------------------------

    public String id() { return id; }
    public String name() { return name; }
    public int hitPoints() { return hitPoints; }
    public int maxHitPoints() { return maxHitPoints; }
    /** Vitesse de déplacement, en mètres par tour. */
    public double speed() { return speed; }
    public List<Weapon> weapons() { return weapons; }

    /** Les attaques possibles : celles de corps à corps, puis celles à distance (lancer compris). */
    public List<Attack> attacks() {
        List<Attack> attacks = new ArrayList<>();
        for (Weapon w : weapons) if (w.isMelee()) attacks.add(new Attack(w, Attack.Mode.MELEE));
        for (Weapon w : weapons) if (w.range() != null) attacks.add(new Attack(w, Attack.Mode.RANGED));
        return attacks;
    }
    public boolean reactionAvailable() { return reactionAvailable; }

    public int armorClass() {
        return baseArmorClass + armorClassBonuses.values().stream().mapToInt(Integer::intValue).sum();
    }

    public boolean has(Condition condition) { return conditions.contains(condition); }
    public boolean hasEffect(String effect) { return effects.contains(effect); }
    public boolean hasFeature(String feature) { return features.contains(feature); }
    public boolean knowsSpell(String spell) { return spellsKnown.contains(spell); }
    public boolean isResistantTo(DamageType type) { return resistances.contains(type); }
    public boolean isVulnerableTo(DamageType type) { return vulnerabilities.contains(type); }

    public int spellSlots(int level) { return spellSlots[level]; }

    public OptionalInt lowestAvailableSlot() {
        for (int level = 1; level < spellSlots.length; level++) {
            if (spellSlots[level] > 0) return OptionalInt.of(level);
        }
        return OptionalInt.empty();
    }

    /** Résumé de l'état mutable, utilisé pour comparer deux états (tests de rejeu). */
    public String signature() {
        return "%s[pv=%d, ca=%d, reaction=%s, emplacements=%s, bonusCA=%s]".formatted(
                id, hitPoints, armorClass(), reactionAvailable, Arrays.toString(spellSlots), armorClassBonuses);
    }

    // --- Mutations (réservées à GameEvent.apply) -------------------------------

    public void takeDamage(int amount) {
        hitPoints = Math.max(0, hitPoints - amount);
    }

    public void spendReaction() {
        reactionAvailable = false;
    }

    public void expendSpellSlot(int level) {
        if (spellSlots[level] <= 0) throw new IllegalStateException(name + " n'a plus d'emplacement de niveau " + level);
        spellSlots[level]--;
    }

    public void addArmorClassBonus(String source, int bonus) {
        armorClassBonuses.put(source, bonus);
    }

    public void removeArmorClassBonus(String source) {
        armorClassBonuses.remove(source);
    }

    // --- Builder ------------------------------------------------------------

    public static final class Builder {
        private final String id;
        private final String name;
        private int armorClass = 10;
        private int hitPoints = 1;
        private double speed = 9;
        private final List<Weapon> weapons = new ArrayList<>();
        private final Set<DamageType> resistances = new HashSet<>();
        private final Set<DamageType> vulnerabilities = new HashSet<>();
        private final Set<Condition> conditions = new HashSet<>();
        private final Set<String> effects = new HashSet<>();
        private final Set<String> features = new HashSet<>();
        private final Set<String> spellsKnown = new HashSet<>();
        private final int[] spellSlots = new int[10];

        private Builder(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public Builder armorClass(int ac) { this.armorClass = ac; return this; }
        public Builder hitPoints(int hp) { this.hitPoints = hp; return this; }
        public Builder speed(double metres) { this.speed = metres; return this; }
        /** Ajoute une arme ; une créature peut en avoir plusieurs. */
        public Builder weapon(Weapon w) { weapons.add(w); return this; }
        public Builder resistance(DamageType t) { resistances.add(t); return this; }
        public Builder vulnerability(DamageType t) { vulnerabilities.add(t); return this; }
        public Builder condition(Condition c) { conditions.add(c); return this; }
        public Builder effect(String e) { effects.add(e); return this; }
        public Builder feature(String f) { features.add(f); return this; }
        public Builder knows(String spell) { spellsKnown.add(spell); return this; }
        public Builder spellSlots(int level, int count) { spellSlots[level] = count; return this; }

        public Creature build() {
            return new Creature(this);
        }
    }
}

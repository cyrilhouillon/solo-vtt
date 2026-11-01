package dnd.engine.attack;

import dnd.engine.state.Attack;
import dnd.engine.state.Weapon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * L'état d'UNE attaque en cours, partagé par ses listeners.
 * C'est un brouillon : seul ce qui est émis sous forme d'événement compte pour la partie.
 */
public final class AttackContext {

    public record DiceTerm(String source, int count, int sides) {
        public String notation() {
            return count + "d" + sides;
        }
    }

    private final String attackerId;
    private final String targetId;
    private final Attack attack;
    private final double distance;

    private final List<String> advantageSources = new ArrayList<>();
    private final List<String> disadvantageSources = new ArrayList<>();
    private final List<DiceTerm> attackDice = new ArrayList<>();

    private int natural;
    private int total;
    private boolean hit;
    private boolean critical;
    private int damage;

    /** {@code distance} : de l'attaquant à la cible, en mètres. */
    public AttackContext(String attackerId, String targetId, Attack attack, double distance) {
        this.attackerId = attackerId;
        this.targetId = targetId;
        this.attack = attack;
        this.distance = distance;
    }

    public String attackerId() { return attackerId; }
    public String targetId() { return targetId; }
    public Attack attack() { return attack; }
    public Weapon weapon() { return attack.weapon(); }
    /** De l'attaquant à la cible, en mètres. */
    public double distance() { return distance; }

    // Accumulation : l'ordre d'ajout n'a aucune importance, c'est voulu.
    public void addAdvantage(String source) { advantageSources.add(source); }
    public void addDisadvantage(String source) { disadvantageSources.add(source); }
    public void addAttackDie(DiceTerm term) { attackDice.add(term); }

    public List<String> advantageSources() { return Collections.unmodifiableList(advantageSources); }
    public List<String> disadvantageSources() { return Collections.unmodifiableList(disadvantageSources); }
    public List<DiceTerm> attackDice() { return Collections.unmodifiableList(attackDice); }

    public int natural() { return natural; }
    public void setNatural(int natural) { this.natural = natural; }

    public int total() { return total; }
    public void setTotal(int total) { this.total = total; }

    public boolean hit() { return hit; }
    public void setHit(boolean hit) { this.hit = hit; }

    public boolean critical() { return critical; }
    public void setCritical(boolean critical) { this.critical = critical; }

    public int damage() { return damage; }
    public void setDamage(int damage) { this.damage = damage; }
}

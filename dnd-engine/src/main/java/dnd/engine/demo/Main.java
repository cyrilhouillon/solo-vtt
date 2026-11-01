package dnd.engine.demo;

import dnd.engine.attack.AttackAction;
import dnd.engine.core.Decision;
import dnd.engine.core.Dice;
import dnd.engine.core.Engine;
import dnd.engine.core.Resolution;
import dnd.engine.core.StepResult;
import dnd.engine.events.GameEvent;
import dnd.engine.rules.Bless;
import dnd.engine.rules.DuelingStyle;
import dnd.engine.rules.ShieldReaction;
import dnd.engine.rules.StandardRules;
import dnd.engine.state.Condition;
import dnd.engine.state.Creature;
import dnd.engine.state.DamageType;
import dnd.engine.state.GameState;
import dnd.engine.state.Weapon;

import java.util.Arrays;
import java.util.Scanner;

/**
 * Démo en console.
 *
 * <pre>
 *   java -cp target/classes dnd.engine.demo.Main                 # scénarios automatiques
 *   java -cp target/classes dnd.engine.demo.Main --interactif    # c'est toi qui décides pour la magicienne
 *   java -cp target/classes dnd.engine.demo.Main --seed=42       # autre tirage pour le scénario 1
 * </pre>
 */
public final class Main {

    /** Graines choisies pour que chaque scénario montre ce qu'il doit montrer. */
    private static final long SEED_SCENARIO_1 = 5;
    private static final long SEED_SCENARIO_2 = 7;

    /** Qui répond aux décisions : un joueur humain, une IA, un client réseau… */
    interface Controller {
        String choose(Decision decision);
    }

    public static void main(String[] args) {
        boolean interactive = Arrays.asList(args).contains("--interactif");
        long seed1 = Arrays.stream(args)
                .filter(a -> a.startsWith("--seed="))
                .map(a -> Long.parseLong(a.substring("--seed=".length())))
                .findFirst().orElse(SEED_SCENARIO_1);

        Controller wizard = interactive ? new ConsoleController() : d -> d.options().getFirst();

        title("Scénario 1 : avantage/désavantage, Bénédiction, réaction Bouclier");
        GameState initial = new GameState()
                .add(fighter(true))
                .add(Creature.builder("magicienne", "Ysolde la magicienne")
                        .armorClass(12).hitPoints(14)
                        .condition(Condition.RESTRAINED)
                        .knows(ShieldReaction.SPELL).spellSlots(1, 2)
                        .build());
        play(initial, Dice.seeded(seed1), "guerriere", "magicienne", wizard);

        title("Scénario 2 : ordre des dégâts (Duel +2, puis résistance)");
        GameState initial2 = new GameState()
                .add(fighter(false))
                .add(Creature.builder("epouvantail", "Épouvantail animé")
                        .armorClass(10).hitPoints(30)
                        .resistance(DamageType.SLASHING).vulnerability(DamageType.FIRE)
                        .build());
        play(initial2, Dice.seeded(SEED_SCENARIO_2), "guerriere", "epouvantail", d -> d.options().getFirst());
    }

    static Creature fighter(boolean poisonedAndBlessed) {
        var b = Creature.builder("guerriere", "Brunehilde la guerrière")
                .armorClass(18).hitPoints(28)
                .weapon(new Weapon("Épée longue", 1, 8, DamageType.SLASHING, 5, 3, true))
                .feature(DuelingStyle.FEATURE);
        if (poisonedAndBlessed) {
            b.condition(Condition.POISONED).effect(Bless.EFFECT);
        }
        return b.build();
    }

    /** Boucle de pilotage : soumettre, puis répondre aux décisions jusqu'à ce que tout soit résolu. */
    static void play(GameState initial, Dice dice, String attackerId, String targetId, Controller controller) {
        GameState snapshot = initial.copy();
        Engine engine = new Engine(initial, StandardRules.create(), dice);
        engine.subscribe(event -> print(engine, event));

        Resolution attack = AttackAction.of(attackerId, targetId, engine.state());
        StepResult result = engine.submit(attack);

        while (result instanceof StepResult.AwaitingDecision(Decision decision)) {
            System.out.println("  ? " + decision.prompt());
            String choice = controller.choose(decision);
            System.out.println("  > " + choice);
            result = engine.answer(decision.id(), choice);
        }

        // Démonstration de l'event sourcing : on rejoue le journal sur l'état initial.
        engine.events().forEach(e -> e.apply(snapshot));
        boolean identical = snapshot.signature().equals(engine.state().signature());
        System.out.println();
        System.out.println("  Journal : " + engine.events().size() + " événements. Rejeu sur l'état initial : "
                + (identical ? "état identique ✔" : "DIFFÉRENT ✘"));
    }

    static void print(Engine engine, GameEvent event) {
        String indent = event instanceof GameEvent.RuleApplied ? "      " : "    ";
        System.out.println(indent + event.describe(engine.state()));
    }

    static void title(String text) {
        System.out.println();
        System.out.println("=== " + text + " ===");
    }

    static final class ConsoleController implements Controller {
        private final Scanner in = new Scanner(System.in);

        public String choose(Decision d) {
            for (int i = 0; i < d.options().size(); i++) {
                System.out.println("    " + (i + 1) + ". " + d.options().get(i));
            }
            while (true) {
                System.out.print("  Ton choix : ");
                String line = in.nextLine().trim();
                try {
                    int n = Integer.parseInt(line);
                    if (n >= 1 && n <= d.options().size()) return d.options().get(n - 1);
                } catch (NumberFormatException ignored) {
                    // on redemande
                }
            }
        }
    }
}

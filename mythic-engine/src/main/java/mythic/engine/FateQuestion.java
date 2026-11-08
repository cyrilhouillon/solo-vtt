package mythic.engine;

import solovtt.engine.Dice;

import java.util.function.Function;
import java.util.Optional;

/**
 * Une question oui/non à la table de destin : un d100 donne la réponse ; un double (11, 22… 99, et 100 lu « 00 ») dont
 * le chiffre ne dépasse pas le chaos déclenche un événement aléatoire, dont un second d100 tire le focus. Le « 00 » est
 * un double de 0 : il déclenche toujours. Avec les tables de sens du joueur, deux d100 de plus tirent le sens de
 * l'événement dans les Actions ; sans elles, l'événement n'a pas de sens et aucun dé de plus n'est lancé. Un focus qui
 * vise une liste (Fils, Personnages) y tire ensuite une ligne ; une liste absente est vide.
 */
public final class FateQuestion {

    private FateQuestion() {
    }

    /**
     * @param lists la liste d'une sorte, lue seulement si le focus d'un événement la vise ; une liste absente est
     *              {@link AdventureList#empty vide}
     */
    public static FateAnswer ask(Odds odds, ChaosFactor chaos, Optional<MeaningTables> tables,
                                 Function<AdventureList.Kind, AdventureList> lists, Dice dice) {
        int roll = dice.roll(100);
        Answer answer = FateChart.cell(odds, chaos).answer(roll);
        Optional<RandomEvent> event = Optional.empty();
        if (triggersRandomEvent(roll, chaos)) event = Optional.of(RandomEvent.roll(tables, lists, dice));
        return new FateAnswer(roll, odds, chaos, answer, event);
    }

    static boolean triggersRandomEvent(int roll, ChaosFactor chaos) {
        if (roll == 100) return true;
        int tens = roll / 10;
        int units = roll % 10;
        return tens == units && units <= chaos.value();
    }
}

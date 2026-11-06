package mythic.engine;

import solovtt.engine.Dice;

import java.util.Optional;

/**
 * Une question oui/non à la table de destin : un d100 donne la réponse ; un double (11, 22… 99, et 100 lu « 00 ») dont
 * le chiffre ne dépasse pas le chaos déclenche un événement aléatoire, dont un second d100 tire le focus. Le « 00 » est
 * un double de 0 : il déclenche toujours.
 */
public final class FateQuestion {

    private FateQuestion() {
    }

    public static FateAnswer ask(Odds odds, ChaosFactor chaos, Dice dice) {
        int roll = dice.roll(100);
        Answer answer = FateChart.cell(odds, chaos).answer(roll);
        Optional<RandomEvent> event = Optional.empty();
        if (triggersRandomEvent(roll, chaos)) {
            int focusRoll = dice.roll(100);
            event = Optional.of(new RandomEvent(EventFocus.of(focusRoll), focusRoll));
        }
        return new FateAnswer(roll, odds, chaos, answer, event);
    }

    static boolean triggersRandomEvent(int roll, ChaosFactor chaos) {
        if (roll == 100) return true;
        int tens = roll / 10;
        int units = roll % 10;
        return tens == units && units <= chaos.value();
    }
}

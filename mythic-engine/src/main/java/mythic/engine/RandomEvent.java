package mythic.engine;

/** Un événement aléatoire, déclenché par un double : son focus et le d100 qui l'a tiré. */
public record RandomEvent(EventFocus focus, int focusRoll) {
}

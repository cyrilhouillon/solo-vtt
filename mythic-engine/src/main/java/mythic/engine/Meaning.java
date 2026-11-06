package mythic.engine;

/** Un sens tiré dans une table de Mythic : deux mots, chacun avec le d100 qui l'a tiré. */
public record Meaning(String table, String first, int firstRoll, String second, int secondRoll) {
}

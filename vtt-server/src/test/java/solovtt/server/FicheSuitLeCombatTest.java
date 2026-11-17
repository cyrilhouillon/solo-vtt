package solovtt.server;

import dnd.engine.catalog.CatalogEntry;
import dnd.engine.catalog.PlayerCharacters;
import dnd.engine.events.GameEvent;
import dnd.engine.rules.ShieldReaction;
import org.junit.jupiter.api.Test;
import solovtt.engine.Dice;
import solovtt.scene.DistanceUnit;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Les coups reçus par un personnage joueur posé suivent sa fiche, une seule fois. Carte de 30 × 20 m : Guerrière
 * (1,5 ; 1,5), Gobelin (12 ; 1,5), Magicienne au contact du Gobelin (12 ; 3). Initiative : Guerrière 20, Gobelin 15,
 * Magicienne 2. Le d20 de 10 du Gobelin touche la Magicienne, qui ne réagit pas ; 3 au d6 : 5 dégâts.
 */
class FicheSuitLeCombatTest {

    /** Un envoi à la fiche : l'id du personnage et les événements. */
    private record Envoi(String id, List<GameEvent> events) {}

    private final InMemoryGameStore store = new InMemoryGameStore();
    private final CombatStore combats = CombatStore.inMemory();
    private final List<String> lances = new ArrayList<>();
    private final List<Envoi> envois = new ArrayList<>();
    private final CharacterSheetFollower fiches = (id, events) -> envois.add(new Envoi(id, events));

    private static CatalogEntry entree(String id) {
        return TestCharacters.entry(id);
    }

    private SceneSession session(Dice dice, CharacterSheetFollower suivi) {
        return new SceneSession(dice, store, MapStore.inMemory(), combats, lances::add, suivi);
    }

    /** Le combat lancé, la Guerrière déplacée, son tour fini : le Gobelin joue. */
    private SceneSession combatAuTourDuGobelin(Dice dice, CharacterSheetFollower suivi) {
        var session = session(dice, suivi);
        session.create(30, 20, DistanceUnit.METRE, null);
        session.place(entree("guerriere"), 1.5, 1.5);
        session.place(entree("gobelin"), 12, 1.5);
        session.place(entree("magicienne"), 12, 3);
        session.startCombat();
        session.move("guerriere-1", 6, 1.5);
        session.endTurn();
        return session;
    }

    /** Le Gobelin frappe la Magicienne, qui ne réagit pas. */
    private static SceneSession.ActionOutcome frapperLaMagicienne(SceneSession session) {
        var attaque = session.attack("gobelin-1", "melee:Cimeterre", "magicienne-1");
        return session.answer(attaque.decision().orElseThrow().id(), "Ne rien faire").orElseThrow();
    }

    private static int degats(SceneSession.ActionOutcome outcome) {
        return outcome.entries().stream().filter(e -> e.damage() != null).findFirst().orElseThrow().damage().amount();
    }

    @Test
    void unCoupEnCombat_partUneFois_avecLaCommandeQuiLePorte() {
        var session = combatAuTourDuGobelin(Dice.scripted(20, 15, 2, 10, 3), fiches);

        var attaque = session.attack("gobelin-1", "melee:Cimeterre", "magicienne-1");
        assertTrue(envois.isEmpty(), "Bouclier attend : aucun dégât encore");

        var reponse = session.answer(attaque.decision().orElseThrow().id(), "Ne rien faire").orElseThrow();

        assertEquals(5, degats(reponse));
        assertEquals(List.of(new Envoi("magicienne", List.of(new GameEvent.DamageDealt("magicienne-1", 5,
                dnd.engine.state.DamageType.SLASHING)))), envois);
        assertEquals(List.of(), reponse.warnings());
        assertInstanceOf(SavedGame.SavedCommand.Answer.class, store.game().commands().getLast(), "gardé avant d'être envoyé");
    }

    @Test
    void unCoupHorsCombat_suitAussiLaFiche() {
        var session = session(Dice.scripted(18, 4), fiches);
        session.create(30, 20, DistanceUnit.METRE, null);
        session.place(entree("gobelin"), 12, 1.5);
        session.place(entree("guerriere"), 12, 3);

        var attaque = session.attack("gobelin-1", "melee:Cimeterre", "guerriere-1");

        assertEquals(1, envois.size());
        assertEquals("guerriere", envois.getFirst().id());
        assertEquals(degats(attaque), ((GameEvent.DamageDealt) envois.getFirst().events().getFirst()).amount());
    }

    @Test
    void unCoupRate_unCoupSurUnMonstre_ouUneAttaqueRefusee_nEnvoientRien() {
        var session = session(Dice.scripted(2, 15, 4), fiches);
        session.create(30, 20, DistanceUnit.METRE, null);
        session.place(entree("gobelin"), 12, 1.5);
        session.place(entree("guerriere"), 12, 3);
        session.place(entree("magicienne"), 28, 18);

        session.attack("gobelin-1", "melee:Cimeterre", "guerriere-1");
        session.attack("guerriere-1", "melee:Épée longue", "gobelin-1");
        assertThrows(IllegalArgumentException.class, () -> session.attack("gobelin-1", "melee:Cimeterre", "magicienne-1"));

        assertTrue(envois.isEmpty());
    }

    @Test
    void rouvrirLaPartie_rejouerLeCombat_ouLeLirePasAPas_nEnvoientRien() {
        var session = combatAuTourDuGobelin(Dice.scripted(20, 15, 2, 10, 3), fiches);
        frapperLaMagicienne(session);
        session.endCombat();
        envois.clear();

        session(Dice.scripted(), fiches);
        SceneSession.replayed(store.game());
        CombatReplay.of(combats.game(lances.getFirst()).load().orElseThrow(), lances.getFirst());

        assertTrue(envois.isEmpty());
    }

    @Test
    void nouvelleCarte_puisReprendreLeCombat_nEnvoieQueLesCoupsSuivants() {
        var session = combatAuTourDuGobelin(Dice.scripted(20, 15, 2, 10, 3, 10, 4), fiches);
        frapperLaMagicienne(session);
        envois.clear();

        session.create(10, 10, DistanceUnit.METRE, null);
        session.resume(combats.game(lances.getFirst()).load().orElseThrow());
        assertTrue(envois.isEmpty(), "ni la nouvelle carte ni la reprise ne réémettent le coup");

        var second = frapperLaMagicienne(session);

        assertEquals(1, envois.size());
        assertEquals(List.of(new GameEvent.DamageDealt("magicienne-1", degats(second), dnd.engine.state.DamageType.SLASHING)),
                envois.getFirst().events());
    }

    @Test
    void uneFicheQuiNeSuitPas_laisseLeCoupGarde_etAvertit() {
        CharacterSheetFollower abimee = (id, events) -> {
            throw new IllegalStateException("Fiches de personnage illisibles : modules/dnd.json est abîmé");
        };
        var session = combatAuTourDuGobelin(Dice.scripted(20, 15, 2, 10, 3), abimee);

        var reponse = frapperLaMagicienne(session);

        assertEquals(List.of("Fiche « Magicienne » non mise à jour : Fiches de personnage illisibles : modules/dnd.json est abîmé"),
                reponse.warnings());
        assertEquals(5, degats(reponse));
        assertInstanceOf(SavedGame.SavedCommand.Answer.class, store.game().commands().getLast());
        assertEquals(7 - 5, session.creature("magicienne-1").orElseThrow().hitPoints());
    }

    @Test
    void leCatalogue_adresseLeCoupAuModuleActif_etRefuseSansLui() {
        CampaignStore campagnes = CampaignStore.inMemory();
        String avecDnd = campagnes.create("Avec", List.of("dnd")).id();
        String sansDnd = campagnes.create("Sans", List.of()).id();
        List<String> recus = new ArrayList<>();
        PlayerCharacters source = new PlayerCharacters() {
            public String moduleId() {
                return "dnd";
            }

            public List<Listed> list(String campaignId) {
                return List.of();
            }

            public Optional<Taken> take(String campaignId, String id) {
                return Optional.empty();
            }

            @Override
            public boolean sustained(String campaignId, String id, List<GameEvent> events) {
                if (!id.equals("aria")) return false;
                recus.add(campaignId);
                return true;
            }
        };
        var catalogue = new CampaignCatalog(campagnes, new InstalledModules(List.of("dnd")), List.of(source));

        assertDoesNotThrow(() -> catalogue.sustained(avecDnd, "aria", List.of()));
        assertEquals(List.of(avecDnd), recus);
        var inconnue = assertThrows(IllegalStateException.class, () -> catalogue.sustained(avecDnd, "perdue", List.of()));
        assertEquals("fiche introuvable ou module inactif", inconnue.getMessage());
        assertThrows(IllegalStateException.class, () -> catalogue.sustained(sansDnd, "aria", List.of()));
        assertEquals(List.of(avecDnd), recus, "un module inactif ne reçoit rien");
    }

    @Test
    void unBouclierLance_envoieLEmplacementDepense_etLaRepriseGardeBouclierEtLEmplacementRestant() {
        var session = combatAuTourDuGobelin(Dice.scripted(20, 15, 2, 10), fiches);
        var attaque = session.attack("gobelin-1", "melee:Cimeterre", "magicienne-1");

        var reponse = session.answer(attaque.decision().orElseThrow().id(), ShieldReaction.CAST).orElseThrow();

        assertTrue(reponse.entries().stream().noneMatch(e -> e.damage() != null), "Bouclier fait rater le Gobelin");
        assertEquals(List.of(new Envoi("magicienne", List.of(new GameEvent.SpellSlotExpended("magicienne-1", 1)))), envois);
        envois.clear();

        var reprise = session(Dice.scripted(), fiches);

        assertTrue(envois.isEmpty(), "la reprise ne réémet pas l'emplacement");
        var magicienne = reprise.creature("magicienne-1").orElseThrow();
        assertTrue(magicienne.knowsSpell(ShieldReaction.SPELL));
        assertEquals(1, magicienne.spellSlots(1));
    }
}

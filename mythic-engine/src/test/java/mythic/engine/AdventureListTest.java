package mythic.engine;

import org.junit.jupiter.api.Test;
import solovtt.engine.Dice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static mythic.engine.AdventureList.Kind.CHARACTERS;
import static mythic.engine.AdventureList.Kind.THREADS;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AdventureListTest {

    /** Une liste de {@code n} lignes écrites. */
    private static AdventureList ecrites(AdventureList.Kind kind, int n) {
        return new AdventureList(kind, Collections.nCopies(n, true));
    }

    @Test
    void cinqLignesAuPlus_pasDeDeDeSection_unD10LuParPaires() {
        assertEquals(new ListDraw.Line(CHARACTERS, 3, null, null, 6, false), ecrites(CHARACTERS, 3).draw(Dice.scripted(6)));
        assertEquals(new ListDraw.Line(CHARACTERS, 1, null, null, 1, false), ecrites(CHARACTERS, 5).draw(Dice.scripted(1)));
        assertEquals(new ListDraw.Line(CHARACTERS, 5, null, null, 10, false), ecrites(CHARACTERS, 5).draw(Dice.scripted(10)));
    }

    @Test
    void septLignes_unD4ChoisitLaSection_commeDansLExempleDuLivre() {
        assertEquals(new ListDraw.Line(CHARACTERS, 6, 4, 3, 1, false), ecrites(CHARACTERS, 7).draw(Dice.scripted(3, 1)));
    }

    @Test
    void leDeDeSection_suitLeNombreDeSectionsActives() {
        assertEquals(4, ((ListDraw.Line) ecrites(THREADS, 10).draw(Dice.scripted(1, 1))).sectionDie());
        assertEquals(6, ((ListDraw.Line) ecrites(THREADS, 11).draw(Dice.scripted(1, 1))).sectionDie());
        assertEquals(8, ((ListDraw.Line) ecrites(THREADS, 16).draw(Dice.scripted(1, 1))).sectionDie());
        assertEquals(10, ((ListDraw.Line) ecrites(THREADS, 21).draw(Dice.scripted(1, 1))).sectionDie());
        assertEquals(new ListDraw.Line(THREADS, 25, 10, 10, 10, false), ecrites(THREADS, 25).draw(Dice.scripted(10, 10)));
    }

    @Test
    void uneLigneVideTiree_vautChoisir() {
        var liste = new AdventureList(THREADS, List.of(true, false, true));

        assertEquals(new ListDraw.Line(THREADS, 2, null, null, 4, true), liste.draw(Dice.scripted(4)));
    }

    @Test
    void uneLigneAuDelaDeLaListe_dansUneSectionActive_vautChoisir() {
        assertEquals(new ListDraw.Line(THREADS, 4, null, null, 7, true), ecrites(THREADS, 2).draw(Dice.scripted(7)));
    }

    @Test
    void desLignesVidesEnFin_nActiventPasDeSection() {
        var lignes = new ArrayList<>(Collections.nCopies(5, true));
        lignes.addAll(Collections.nCopies(3, false));

        assertEquals(new ListDraw.Line(CHARACTERS, 5, null, null, 9, false),
                new AdventureList(CHARACTERS, lignes).draw(Dice.scripted(9)));
    }

    @Test
    void uneEntreeRepetee_occupePlusDeLignes_doncPlusDeJets() {
        var liste = new AdventureList(CHARACTERS, List.of(true, true, true));
        // « A », « A », « B » : 1-2 et 3-4 donnent une ligne de « A », 5-6 celle de « B ».
        assertEquals(1, ((ListDraw.Line) liste.draw(Dice.scripted(2))).line());
        assertEquals(2, ((ListDraw.Line) liste.draw(Dice.scripted(3))).line());
        assertEquals(3, ((ListDraw.Line) liste.draw(Dice.scripted(5))).line());
    }

    @Test
    void uneListeVide_neLancePasDeDe() {
        assertEquals(new ListDraw.Empty(THREADS), AdventureList.empty(THREADS).draw(Dice.scripted()));
        assertEquals(new ListDraw.Empty(THREADS), new AdventureList(THREADS, List.of(false, false)).draw(Dice.scripted()));
    }

    @Test
    void uneEntreeAuDelaDeLa25eLigne_rendLaListePleine_sansDe() {
        assertEquals(new ListDraw.Full(CHARACTERS), ecrites(CHARACTERS, 26).draw(Dice.scripted()));
    }

    @Test
    void desLignesVidesAuDelaDe25_neComptentPas() {
        var lignes = new ArrayList<>(Collections.nCopies(25, true));
        lignes.add(false);

        assertEquals(new ListDraw.Line(CHARACTERS, 1, 10, 1, 1, false),
                new AdventureList(CHARACTERS, lignes).draw(Dice.scripted(1, 1)));
    }
}

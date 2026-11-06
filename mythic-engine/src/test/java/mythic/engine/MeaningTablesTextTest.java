package mythic.engine;

import org.junit.jupiter.api.Test;

import java.util.List;

import static mythic.engine.TablesDeTest.colonne;
import static mythic.engine.TablesDeTest.section;
import static mythic.engine.TablesDeTest.texte;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MeaningTablesTextTest {

    private static String refus(String texte) {
        return assertThrows(IllegalArgumentException.class, () -> MeaningTablesText.parse(texte)).getMessage();
    }

    private static final String OBLIGATOIRES = section("Actions 1", "action1") + section("Actions 2", "action2")
            + section("Descriptions 1", "description1") + section("Descriptions 2", "description2");

    @Test
    void unFichierBienForme_donneSesColonnes_etSesElementsDansLOrdre() {
        var tables = MeaningTablesText.parse(texte());

        assertEquals(colonne("action1"), tables.actions1());
        assertEquals(colonne("action2"), tables.actions2());
        assertEquals(colonne("description1"), tables.descriptions1());
        assertEquals(colonne("description2"), tables.descriptions2());
        assertEquals(List.of("Ton de l'aventure", "Lieux"), tables.elementNames());
        assertEquals(colonne("lieu"), tables.elements().get("Lieux"));
    }

    @Test
    void sansElement_lesQuatreColonnesSuffisent() {
        assertEquals(List.of(), MeaningTablesText.parse(OBLIGATOIRES).elementNames());
    }

    @Test
    void bomCrlfLignesVidesEtEspaces_sontTolérés() {
        String sale = "﻿\r\n" + texte().replace("\n", "\r\n\r\n").replace("action1-007", "   action1-007  ")
                .replace("# Actions 2", "#   actions   2 ");

        assertEquals(MeaningTablesText.parse(texte()), MeaningTablesText.parse(sale));
    }

    @Test
    void uneColonneSansSesCentEntrees_estRefusee_avecLaLigneDeSonTitre() {
        String court = OBLIGATOIRES.replace("action2-100\n", "");

        assertEquals("Actions 2 : 99 entrées, 100 attendues (ligne 102)", refus(court));
    }

    @Test
    void uneColonneObligatoireAbsente_estRefusee() {
        String sansDescription2 = OBLIGATOIRES.replace(section("Descriptions 2", "description2"), "");

        assertEquals("Section « # Descriptions 2 » absente", refus(sansDescription2));
    }

    @Test
    void unTitreInconnu_estRefuseAvecSaLigne() {
        assertEquals("Ligne 1 : titre inconnu « # Actions 3 » (Actions 1, Actions 2, Descriptions 1, Descriptions 2 ou Élément : <nom>)",
                refus("# Actions 3\n"));
    }

    @Test
    void duTexteAvantLePremierTitre_estRefuse() {
        assertEquals("Ligne 2 : texte avant le premier titre (« # Actions 1 » attendu)", refus("\n1: Abandon\n" + OBLIGATOIRES));
    }

    @Test
    void unElementEnDouble_sansCasse_estRefuse() {
        String double_ = OBLIGATOIRES + section("Élément : Lieux", "lieu") + section("Element : lieux", "lieu");

        assertEquals("Ligne 506 : élément « lieux » en double", refus(double_));
    }

    @Test
    void unNomDElementVide_estRefuse() {
        assertEquals("Ligne 405 : nom d'élément vide", refus(OBLIGATOIRES + "# Élément :\n"));
    }

    @Test
    void uneColonneEnDouble_estRefusee() {
        assertEquals("Ligne 102 : section « # Actions 1 » en double",
                refus(section("Actions 1", "action1") + section("Actions 1", "action1")));
    }
}

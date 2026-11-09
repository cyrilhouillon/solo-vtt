package oracle.server;

import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.Test;
import solovtt.engine.Dice;
import solovtt.module.ModuleData;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OracleResourceTest {

    static final String STRAHD = "strahd";

    /** Les données des modules d'une campagne, en mémoire. */
    static final class Donnees implements ModuleData {
        final Map<String, String> fichiers = new HashMap<>();

        @Override
        public boolean exists(String campaignId) {
            return campaignId.equals(STRAHD) || campaignId.equals("phandelver");
        }

        @Override
        public Optional<String> read(String campaignId, String moduleId) {
            return Optional.ofNullable(fichiers.get(campaignId + "/" + moduleId));
        }

        @Override
        public void write(String campaignId, String moduleId, String json) {
            fichiers.put(campaignId + "/" + moduleId, json);
        }
    }

    private static OracleResource oracle(int... des) {
        return new OracleResource(new Donnees(), new PagesDeTest(), Dice.scripted(des));
    }

    @Test
    void ouiNon_ouiJusquA50_nonAuDela() {
        assertEquals(new OracleDtos.YesNoDto("Le pont tient ?", 50, true),
                oracle(50).yesNo(STRAHD, new OracleDtos.QuestionRequest("  Le pont tient ?  ")));
        assertEquals(new OracleDtos.YesNoDto(null, 51, false), oracle(51).yesNo(STRAHD, new OracleDtos.QuestionRequest(null)));
        assertEquals(new OracleDtos.YesNoDto(null, 1, true), oracle(1).yesNo(STRAHD, null));
    }

    @Test
    void d100_rendLeJet() {
        assertEquals(new OracleDtos.D100Dto(null, 100), oracle(100).d100(STRAHD, new OracleDtos.QuestionRequest("   ")));
        assertEquals(new OracleDtos.D100Dto("Combien de gardes ?", 37),
                oracle(37).d100(STRAHD, new OracleDtos.QuestionRequest("Combien de gardes ?")));
    }

    @Test
    void uneQuestionTropLongue_400_sansDe() {
        var trop = new OracleDtos.QuestionRequest("x".repeat(501));
        assertThrows(IllegalArgumentException.class, () -> oracle().yesNo(STRAHD, trop));
        assertThrows(IllegalArgumentException.class, () -> oracle().d100(STRAHD, trop));
        assertEquals(500, oracle(2).yesNo(STRAHD, new OracleDtos.QuestionRequest("x".repeat(500))).question().length());
    }

    @Test
    void uneCampagneInconnue_404() {
        assertThrows(NotFoundException.class, () -> oracle(10).yesNo("inconnue", null));
        assertThrows(NotFoundException.class, () -> oracle(10).d100("inconnue", null));
    }
}

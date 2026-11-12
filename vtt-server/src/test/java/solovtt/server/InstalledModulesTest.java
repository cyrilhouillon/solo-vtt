package solovtt.server;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

class InstalledModulesTest {

    @Test
    void unModuleNommeCombats_estRefuse_carLaSectionEstAuCoeur() {
        assertThrows(IllegalArgumentException.class, () -> new InstalledModules(List.of("mythic", "combats")));
    }
}

package mythic.server;

import solovtt.module.CommonModuleData;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Les données communes des modules, en mémoire. */
final class DonneesCommunes implements CommonModuleData {
    final Map<String, String> fichiers = new HashMap<>();

    @Override
    public Optional<String> read(String moduleId) {
        return Optional.ofNullable(fichiers.get(moduleId));
    }

    @Override
    public void write(String moduleId, String json) {
        fichiers.put(moduleId, json);
    }
}

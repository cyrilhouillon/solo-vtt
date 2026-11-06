package solovtt.server.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import solovtt.server.Campaign;
import solovtt.server.CampaignStore;
import solovtt.server.GameStore;
import solovtt.server.MapStore;
import solovtt.server.NotebookStore;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Une campagne par dossier {@code <racine>/<id>/} : {@code campagne.json} (id et nom), {@code partie.json} et
 * {@code cartes.json}, son Carnet dans {@code carnet/} et les données de chaque module dans {@code modules/<module>.json}. Un dossier sans {@code campagne.json} lisible est ignoré,
 * jamais écrasé.
 */
public final class FileCampaignStore implements CampaignStore {

    private static final String CAMPAIGN_FILE = "campagne.json";
    private static final System.Logger LOG = System.getLogger(FileCampaignStore.class.getName());

    private final Path root;
    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    /** Un seul Carnet par campagne, pour que ses écritures se suivent. */
    private final Map<String, NotebookStore> notebooks = new ConcurrentHashMap<>();

    public FileCampaignStore(Path root) {
        this.root = root.toAbsolutePath();
    }

    @Override
    public synchronized List<Campaign> list() {
        if (!Files.isDirectory(root)) return List.of();
        List<Campaign> campaigns = new ArrayList<>();
        try (Stream<Path> dirs = Files.list(root)) {
            for (Path dir : dirs.filter(Files::isDirectory).toList()) {
                Path file = dir.resolve(CAMPAIGN_FILE);
                if (!Files.exists(file)) continue;
                try {
                    Campaign campaign = mapper.readValue(file.toFile(), Campaign.class);
                    if (campaign.id() == null || !campaign.id().equals(dir.getFileName().toString()) || campaign.name() == null) {
                        LOG.log(System.Logger.Level.WARNING, "Campagne ignorée, fiche incohérente : " + file);
                        continue;
                    }
                    campaigns.add(campaign);
                } catch (IOException e) {
                    LOG.log(System.Logger.Level.WARNING, "Campagne ignorée, fiche illisible : " + file, e);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Campagnes illisibles : " + root, e);
        }
        return campaigns.stream().sorted(BY_NAME).toList();
    }

    @Override
    public synchronized Campaign create(String name) {
        var campaign = new Campaign(UUID.randomUUID().toString(), name);
        Path dir = root.resolve(campaign.id());
        try {
            Files.createDirectories(dir);
            Path temporary = dir.resolve(CAMPAIGN_FILE + ".tmp");
            mapper.writeValue(temporary.toFile(), campaign);
            Files.move(temporary, dir.resolve(CAMPAIGN_FILE), StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible de créer la campagne : " + dir, e);
        }
        return campaign;
    }

    @Override
    public GameStore games(String id) {
        return new JsonFileGameStore(dirOf(id).resolve("partie.json"));
    }

    @Override
    public MapStore maps(String id) {
        return new JsonFileMapStore(dirOf(id).resolve("cartes.json"));
    }

    @Override
    public NotebookStore notebook(String id) {
        Path dir = dirOf(id);
        return notebooks.computeIfAbsent(id, key -> new JsonFileNotebookStore(dir.resolve("carnet")));
    }

    @Override
    public synchronized Optional<String> readModuleData(String id, String moduleId) {
        Path file = moduleFile(id, moduleId);
        if (!Files.exists(file)) return Optional.empty();
        try {
            return Optional.of(Files.readString(file));
        } catch (IOException e) {
            throw new UncheckedIOException("Données de module illisibles : " + file, e);
        }
    }

    @Override
    public synchronized void writeModuleData(String id, String moduleId, String json) {
        Path file = moduleFile(id, moduleId);
        try {
            Files.createDirectories(file.getParent());
            Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(temporary, json);
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible d'écrire les données de module : " + file, e);
        }
    }

    private Path moduleFile(String id, String moduleId) {
        return dirOf(id).resolve("modules").resolve(CampaignStore.checkModuleId(moduleId) + ".json");
    }

    /** Le dossier d'une campagne connue : aucun chemin ne se construit depuis un id qui n'en est pas un. */
    private Path dirOf(String id) {
        if (find(id).isEmpty()) throw new IllegalArgumentException("Campagne inconnue : " + id);
        return root.resolve(id);
    }
}

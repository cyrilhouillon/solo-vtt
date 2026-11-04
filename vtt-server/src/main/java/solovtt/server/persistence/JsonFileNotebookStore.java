package solovtt.server.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import solovtt.server.NotebookStore;
import solovtt.server.Page;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Le Carnet d'une campagne, dans un dossier : une fiche par page ({@code <id>.json} : id, titre, contenu en JSON
 * imbriqué) et {@code carnet.json} pour la dernière page consultée. Chaque écriture passe par un fichier voisin et un
 * déplacement atomique. Une fiche illisible est ignorée, jamais écrasée.
 */
public final class JsonFileNotebookStore implements NotebookStore {

    private static final String NOTEBOOK_FILE = "carnet.json";
    private static final String EXTENSION = ".json";
    private static final System.Logger LOG = System.getLogger(JsonFileNotebookStore.class.getName());

    private record PageFile(String id, String title, JsonNode content) {}

    private record NotebookFile(String lastPageId) {}

    private final Path dir;
    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public JsonFileNotebookStore(Path dir) {
        this.dir = dir.toAbsolutePath();
    }

    @Override
    public synchronized List<Page> list() {
        if (!Files.isDirectory(dir)) return List.of();
        List<Page> pages = new ArrayList<>();
        try (Stream<Path> files = Files.list(dir)) {
            for (Path file : files.filter(this::isPageFile).toList()) {
                read(file).ifPresent(pages::add);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Carnet illisible : " + dir, e);
        }
        return pages.stream().sorted(BY_TITLE).toList();
    }

    @Override
    public synchronized Page create(String title) {
        var page = new Page(UUID.randomUUID().toString(), title, null);
        write(page);
        return page;
    }

    @Override
    public synchronized void saveContent(String id, String content) {
        Page page = known(id);
        write(new Page(page.id(), page.title(), content));
    }

    @Override
    public synchronized Optional<String> lastPage() {
        Path file = dir.resolve(NOTEBOOK_FILE);
        if (!Files.exists(file)) return Optional.empty();
        try {
            return Optional.ofNullable(mapper.readValue(file.toFile(), NotebookFile.class).lastPageId());
        } catch (IOException e) {
            LOG.log(System.Logger.Level.WARNING, "Dernière page illisible : " + file, e);
            return Optional.empty();
        }
    }

    @Override
    public synchronized void setLastPage(String id) {
        writeAtomically(dir.resolve(NOTEBOOK_FILE), new NotebookFile(known(id).id()));
    }

    /** Une page lue sur le disque : aucun chemin ne se construit depuis un id qui n'en est pas un. */
    private Page known(String id) {
        return find(id).orElseThrow(() -> new IllegalArgumentException("Page inconnue : " + id));
    }

    private boolean isPageFile(Path file) {
        String name = file.getFileName().toString();
        return Files.isRegularFile(file) && name.endsWith(EXTENSION) && !name.equals(NOTEBOOK_FILE);
    }

    private Optional<Page> read(Path file) {
        try {
            PageFile page = mapper.readValue(file.toFile(), PageFile.class);
            String expected = page.id() + EXTENSION;
            if (page.id() == null || page.title() == null || !expected.equals(file.getFileName().toString())) {
                LOG.log(System.Logger.Level.WARNING, "Page ignorée, fiche incohérente : " + file);
                return Optional.empty();
            }
            String content = page.content() == null || page.content().isNull() ? null : mapper.writer()
                    .without(SerializationFeature.INDENT_OUTPUT).writeValueAsString(page.content());
            return Optional.of(new Page(page.id(), page.title(), content));
        } catch (IOException e) {
            LOG.log(System.Logger.Level.WARNING, "Page ignorée, fiche illisible : " + file, e);
            return Optional.empty();
        }
    }

    private void write(Page page) {
        try {
            JsonNode content = page.content() == null ? null : mapper.readTree(page.content());
            writeAtomically(dir.resolve(page.id() + EXTENSION), new PageFile(page.id(), page.title(), content));
        } catch (IOException e) {
            throw new UncheckedIOException("Contenu de page illisible : " + page.id(), e);
        }
    }

    private void writeAtomically(Path file, Object value) {
        try {
            Files.createDirectories(dir);
            Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
            mapper.writeValue(temporary.toFile(), value);
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible d'écrire le Carnet : " + file, e);
        }
    }
}

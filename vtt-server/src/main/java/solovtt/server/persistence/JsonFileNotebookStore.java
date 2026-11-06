package solovtt.server.persistence;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import solovtt.server.CampaignStore;
import solovtt.server.NotebookStore;
import solovtt.server.Page;
import solovtt.server.PageTree;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Le Carnet d'une campagne, dans un dossier : une fiche par page ({@code <id>.json} : id, titre, parent, section d'une
 * racine de section, contenu en JSON imbriqué) et {@code carnet.json} pour les onglets : {@code openPageIds} dans l'ordre, {@code lastPageId} l'actif (un
 * fichier d'avant les onglets, sans {@code openPageIds}, se lit en un seul onglet). Une page à la corbeille garde sa fiche,
 * marquée {@code deletedWith} (la page dont la suppression l'a emportée) et {@code deletedAt} (ISO-8601, le mapper n'ayant
 * pas de module {@code java.time}) ; une fiche sans ces champs est vivante. Chaque écriture passe par un fichier voisin et
 * un déplacement atomique. Une fiche illisible est ignorée, jamais écrasée ni effacée.
 */
public final class JsonFileNotebookStore implements NotebookStore {

    private static final String NOTEBOOK_FILE = "carnet.json";
    private static final String EXTENSION = ".json";
    private static final System.Logger LOG = System.getLogger(JsonFileNotebookStore.class.getName());

    private record PageFile(String id, String title, String parentId,
                            @JsonInclude(JsonInclude.Include.NON_NULL) String section, JsonNode content,
                            @JsonInclude(JsonInclude.Include.NON_NULL) String deletedWith,
                            @JsonInclude(JsonInclude.Include.NON_NULL) String deletedAt) {}

    private record NotebookFile(String lastPageId, List<String> openPageIds) {}

    /** Une fiche lue : la page, et sa marque de corbeille, {@code null} si elle est vivante. */
    private record Stored(Page page, Trashed trashed) {
        boolean alive() {
            return trashed == null;
        }
    }

    private final Path dir;
    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public JsonFileNotebookStore(Path dir) {
        this.dir = dir.toAbsolutePath();
    }

    @Override
    public synchronized List<Page> list() {
        return stored().stream().filter(Stored::alive).map(Stored::page).sorted(BY_TITLE).toList();
    }

    /** Lit la seule fiche de cette page, vivante : un id qui n'est pas un UUID ne construit aucun chemin. */
    @Override
    public synchronized Optional<Page> find(String id) {
        if (!isPageId(id)) return Optional.empty();
        Path file = dir.resolve(id + EXTENSION);
        return Files.isRegularFile(file) ? read(file).filter(Stored::alive).map(Stored::page) : Optional.empty();
    }

    @Override
    public synchronized Page create(String title, String parentId) {
        if (parentId != null) known(parentId);
        var page = new Page(UUID.randomUUID().toString(), title, parentId, null);
        write(page);
        return page;
    }

    @Override
    public synchronized Page createSectionRoot(String section, String title, String content) {
        var page = new Page(UUID.randomUUID().toString(), title, null, CampaignStore.checkModuleId(section), content);
        write(page);
        return page;
    }

    @Override
    public synchronized void saveContent(String id, String content) {
        write(known(id).withContent(content));
    }

    @Override
    public synchronized void rename(String id, String title) {
        write(known(id).withTitle(title));
    }

    @Override
    public synchronized void move(String id, String parentId) {
        Page page = known(id);
        if (parentId != null) known(parentId);
        NotebookStore.checkMove(list(), page, parentId);
        write(page.withParent(parentId));
    }

    /**
     * La page d'abord : si une écriture échoue ensuite, la suppression est déjà à la corbeille, et la restaurer ramène ce
     * qui en est parti.
     */
    @Override
    public synchronized void delete(String id) {
        Page root = known(id);
        NotebookStore.checkDelete(root);
        List<Page> alive = list();
        Set<String> branch = PageTree.branch(alive, id);
        String deletedAt = Instant.now().toString();
        write(root, id, deletedAt);
        for (Page page : alive) {
            if (!page.id().equals(id) && branch.contains(page.id())) write(page, id, deletedAt);
        }
    }

    @Override
    public synchronized List<TrashEntry> trash() {
        return NotebookStore.trashEntries(stored().stream().filter(s -> !s.alive()).map(Stored::trashed).toList());
    }

    /**
     * La page en dernier : si une écriture échoue avant, la suppression reste à la corbeille, et la restaurer à nouveau
     * ramène le reste.
     */
    @Override
    public synchronized void restore(String id) {
        List<Stored> all = stored();
        Stored root = all.stream()
                .filter(s -> !s.alive() && s.page().id().equals(id) && id.equals(s.trashed().deletedWith()))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Aucune suppression : " + id));
        Set<String> alive = all.stream().filter(Stored::alive).map(s -> s.page().id()).collect(Collectors.toSet());
        String parent = root.page().parentId();
        String parentId = parent != null && alive.contains(parent) ? parent : null;
        for (Stored member : all) {
            if (member.alive() || member == root || !id.equals(member.trashed().deletedWith())) continue;
            write(member.page());
        }
        write(root.page().withParent(parentId));
    }

    @Override
    public synchronized void emptyTrash() {
        for (Stored member : stored()) {
            if (member.alive()) continue;
            try {
                Files.deleteIfExists(dir.resolve(member.page().id() + EXTENSION));
            } catch (IOException e) {
                throw new UncheckedIOException("Impossible de vider la corbeille : " + dir, e);
            }
        }
    }

    @Override
    public synchronized Tabs tabs() {
        Path file = dir.resolve(NOTEBOOK_FILE);
        if (!Files.exists(file)) return Tabs.NONE;
        try {
            NotebookFile read = mapper.readValue(file.toFile(), NotebookFile.class);
            if (read.openPageIds() != null) return new Tabs(read.openPageIds(), read.lastPageId());
            return read.lastPageId() == null ? Tabs.NONE : new Tabs(List.of(read.lastPageId()), read.lastPageId());
        } catch (IOException | RuntimeException e) {
            LOG.log(System.Logger.Level.WARNING, "Onglets illisibles : " + file, e);
            return Tabs.NONE;
        }
    }

    @Override
    public synchronized void setTabs(Tabs tabs) {
        NotebookStore.checkTabs(tabs, id -> find(id).isPresent());
        writeAtomically(dir.resolve(NOTEBOOK_FILE), new NotebookFile(tabs.activeId(), tabs.pageIds()));
    }

    /** Une page vivante lue sur le disque : aucun chemin ne se construit depuis un id qui n'en est pas un. */
    private Page known(String id) {
        return find(id).orElseThrow(() -> new IllegalArgumentException("Page inconnue : " + id));
    }

    /** Un id de page est un UUID sous sa forme canonique, comme {@link #create} le tire. */
    private static boolean isPageId(String id) {
        if (id == null) return false;
        try {
            return UUID.fromString(id).toString().equals(id);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private boolean isPageFile(Path file) {
        String name = file.getFileName().toString();
        return Files.isRegularFile(file) && name.endsWith(EXTENSION) && !name.equals(NOTEBOOK_FILE);
    }

    /** Toutes les fiches lisibles, vivantes ou à la corbeille. */
    private List<Stored> stored() {
        if (!Files.isDirectory(dir)) return List.of();
        List<Stored> pages = new ArrayList<>();
        try (Stream<Path> files = Files.list(dir)) {
            for (Path file : files.filter(this::isPageFile).toList()) {
                read(file).ifPresent(pages::add);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Carnet illisible : " + dir, e);
        }
        return pages;
    }

    private Optional<Stored> read(Path file) {
        try {
            PageFile page = mapper.readValue(file.toFile(), PageFile.class);
            String expected = page.id() + EXTENSION;
            if (page.id() == null || page.title() == null || !expected.equals(file.getFileName().toString())
                    || (page.deletedWith() == null) != (page.deletedAt() == null)) {
                LOG.log(System.Logger.Level.WARNING, "Page ignorée, fiche incohérente : " + file);
                return Optional.empty();
            }
            String content = page.content() == null || page.content().isNull() ? null : mapper.writer()
                    .without(SerializationFeature.INDENT_OUTPUT).writeValueAsString(page.content());
            Page read = new Page(page.id(), page.title(), page.parentId(), page.section(), content);
            Trashed trashed = page.deletedWith() == null ? null
                    : new Trashed(read, page.deletedWith(), Instant.parse(page.deletedAt()));
            return Optional.of(new Stored(read, trashed));
        } catch (IOException | RuntimeException e) {
            LOG.log(System.Logger.Level.WARNING, "Page ignorée, fiche illisible : " + file, e);
            return Optional.empty();
        }
    }

    private void write(Page page) {
        write(page, null, null);
    }

    /** Écrit la fiche, marquée à la corbeille quand {@code deletedWith} n'est pas {@code null}. */
    private void write(Page page, String deletedWith, String deletedAt) {
        try {
            JsonNode content = page.content() == null ? null : mapper.readTree(page.content());
            writeAtomically(dir.resolve(page.id() + EXTENSION),
                    new PageFile(page.id(), page.title(), page.parentId(), page.section(), content, deletedWith, deletedAt));
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

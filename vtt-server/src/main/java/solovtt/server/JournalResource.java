package solovtt.server;

import jakarta.ws.rs.GET;

import java.util.List;

public class JournalResource {

    private final SceneSession session;

    public JournalResource(SceneSession session) {
        this.session = session;
    }

    /** Le journal de la carte en cours, de la plus ancienne entrée à la plus récente. */
    @GET
    public List<Dtos.JournalEntryDto> journal() {
        return session.journal().stream().map(Dtos.JournalEntryDto::of).toList();
    }
}

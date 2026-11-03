package solovtt.server;

/** Une action qui suppose une campagne ouverte, tentée sans campagne ; rendue en 409 comme tout refus d'action. */
public class NoCampaignException extends ActionRefusedException {

    public NoCampaignException() {
        super("Aucune campagne ouverte : en ouvrir une depuis l'accueil");
    }
}

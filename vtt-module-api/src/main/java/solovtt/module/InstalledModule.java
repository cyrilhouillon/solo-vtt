package solovtt.module;

/**
 * Un module installé dans l'application : il se déclare au cœur par un bean qui implémente cette interface, pour que le
 * joueur puisse l'activer ou non dans chaque campagne. Les routes d'un module sous {@code /api/campaigns/{id}/<id du
 * module>/…} sont refusées (404) dans une campagne où il n'est pas actif.
 */
public interface InstalledModule {

    /** L'id du module : minuscules, chiffres et tirets, le même que ses données, sa section du Carnet et ses routes. */
    String id();
}

package solovtt.server;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Où vivent les images de carte, hors de la partie : chacune est gardée une seule fois, sous l'empreinte SHA-256
 * de ses octets, que la partie référence. La session ne connaît que ce port.
 */
public interface ImageStore {

    /** Une image gardée et son type (`image/png`, `image/jpeg` ou `image/webp`). */
    record StoredImage(byte[] bytes, String mediaType) {}

    /** Garde l'image si elle ne l'est pas déjà. */
    String put(byte[] bytes, String mediaType);

    /** L'image gardée sous cette empreinte, vide si aucune. */
    Optional<StoredImage> find(String id);

    /** L'empreinte d'une image : SHA-256 de ses octets, en hexadécimal minuscule. */
    static String idOf(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }

    /** Garde les images en mémoire, le temps du processus. */
    static ImageStore inMemory() {
        Map<String, StoredImage> images = new ConcurrentHashMap<>();
        return new ImageStore() {
            @Override
            public String put(byte[] bytes, String mediaType) {
                String id = idOf(bytes);
                images.putIfAbsent(id, new StoredImage(bytes.clone(), mediaType));
                return id;
            }

            @Override
            public Optional<StoredImage> find(String id) {
                return Optional.ofNullable(images.get(id));
            }
        };
    }
}

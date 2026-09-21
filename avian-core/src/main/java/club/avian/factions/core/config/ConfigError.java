package club.avian.factions.core.config;

/** One startup/reload configuration failure, printed as {@code file → path: message}. */
public record ConfigError(String file, String path, String message) {

    @Override
    public String toString() {
        return file + " → " + path + ": " + message;
    }
}

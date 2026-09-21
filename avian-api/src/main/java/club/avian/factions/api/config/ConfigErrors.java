package club.avian.factions.api.config;

/** Collects invariant failures during validation; every failure names file and field (spec §67). */
public interface ConfigErrors {

    /** Records a failure at {@code path} (dotted, e.g. {@code database.pool-size}). */
    void add(String path, String message);

    /** Records {@code message} (formatted with {@code args}) when {@code condition} is false. */
    default void check(boolean condition, String path, String message, Object... args) {
        if (!condition) {
            add(path, args.length == 0 ? message : String.format(message, args));
        }
    }
}

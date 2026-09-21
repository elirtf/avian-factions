package club.avian.factions.api.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a config field whose change cannot be applied by {@code /avian reload}: the old value is
 * kept and a warning names the field (ADR-0003 rule 7).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface RequiresRestart {
}

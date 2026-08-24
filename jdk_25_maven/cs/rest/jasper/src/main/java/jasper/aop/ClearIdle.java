package jasper.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark REST controllers or methods that should reset the idle timer.
 * Used by the backfill system to detect when the server is idle.
 * Should not be applied to public replication endpoints (pub/).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ClearIdle {
}

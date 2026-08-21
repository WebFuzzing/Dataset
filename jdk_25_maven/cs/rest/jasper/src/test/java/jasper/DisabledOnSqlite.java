package jasper;

import org.springframework.test.context.junit.jupiter.DisabledIf;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@DisabledIf(expression = "#{environment.acceptsProfiles(T(org.springframework.core.env.Profiles).of('sqlite'))}", loadContext = true)
public @interface DisabledOnSqlite {
}

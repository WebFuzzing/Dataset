package jasper;

import java.lang.annotation.*;

import org.springframework.boot.test.context.SpringBootTest;

/**
 * Base composite annotation for integration tests.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(classes = JasperApplication.class)
public @interface IntegrationTest {
}

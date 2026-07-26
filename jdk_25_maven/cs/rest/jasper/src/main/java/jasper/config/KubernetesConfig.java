package jasper.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("kubernetes")
@Configuration
public class KubernetesConfig {

}

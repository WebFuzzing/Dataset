package jasper;

import jasper.config.Props;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.boot.liquibase.autoconfigure.LiquibaseProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.kubernetes.fabric8.discovery.Fabric8CatalogWatchAutoConfiguration;
import org.springframework.cloud.kubernetes.fabric8.discovery.Fabric8DiscoveryClientAutoConfiguration;

@SpringBootApplication(
	exclude = {
		Fabric8DiscoveryClientAutoConfiguration.class,
		Fabric8CatalogWatchAutoConfiguration.class,
		DataRedisAutoConfiguration.class,
	},
	excludeName = "org.springframework.cloud.kubernetes.fabric8.discovery.Fabric8InformerAutoConfiguration"
)
@EnableConfigurationProperties({ LiquibaseProperties.class, Props.class })
@EnableCaching
public class JasperApplication {

	static void main(String[] args) {
		SpringApplication.run(JasperApplication.class, args);
	}

}

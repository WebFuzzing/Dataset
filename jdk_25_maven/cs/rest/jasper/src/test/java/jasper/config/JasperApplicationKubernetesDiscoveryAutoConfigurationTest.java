package jasper.config;

import static org.assertj.core.api.Assertions.assertThat;

import jasper.JasperApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.kubernetes.fabric8.discovery.Fabric8CatalogWatchAutoConfiguration;
import org.springframework.cloud.kubernetes.fabric8.discovery.Fabric8DiscoveryClientAutoConfiguration;
import org.springframework.core.annotation.MergedAnnotations;

class JasperApplicationKubernetesDiscoveryAutoConfigurationTest {

	@Test
	void applicationExcludesKubernetesDiscoveryAutoConfiguration() {
		var application = MergedAnnotations.from(JasperApplication.class).get(SpringBootApplication.class);

		assertThat(application.getClassArray("exclude"))
			.contains(Fabric8DiscoveryClientAutoConfiguration.class, Fabric8CatalogWatchAutoConfiguration.class);
		assertThat(application.getStringArray("excludeName"))
			.contains("org.springframework.cloud.kubernetes.fabric8.discovery.Fabric8InformerAutoConfiguration");
	}
}

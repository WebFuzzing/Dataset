package jasper;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import jasper.config.Props;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.belongToAnyOf;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

class TechnicalStructureTest {

    @Test
    void respectsTechnicalArchitectureLayers() {
        JavaClasses classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("jasper");
        
        // prettier-ignore
        ArchRule rule = layeredArchitecture().consideringAllDependencies()
            .layer("Util").definedBy("jasper.util..")
            .layer("Config").definedBy("jasper.config..")
            .layer("Client").definedBy("jasper.client..")
            .layer("Web").definedBy("jasper.web..")
            .layer("Component").definedBy("jasper.component..")
            .layer("Service").definedBy("jasper.service..")
            .layer("Security").definedBy("jasper.security..")
            .layer("Persistence").definedBy("jasper.repository..")
            .layer("Domain").definedBy("jasper.domain..")
            .layer("Plugin").definedBy("jasper.plugin..")

            .whereLayer("Config").mayOnlyBeAccessedByLayers("Domain", "Web", "Service", "Component", "Security", "Client", "Persistence", "Util")
            .whereLayer("Client").mayOnlyBeAccessedByLayers("Web", "Component", "Service")
            .whereLayer("Web").mayOnlyBeAccessedByLayers("Config")
            .whereLayer("Component").mayOnlyBeAccessedByLayers("Web", "Client", "Service", "Config", "Security", "Domain")
            .whereLayer("Service").mayOnlyBeAccessedByLayers("Web", "Component", "Config", "Security", "Util", "Client", "Domain")
            .whereLayer("Security").mayOnlyBeAccessedByLayers("Config", "Client", "Service", "Component", "Persistence", "Web")
            .whereLayer("Persistence").mayOnlyBeAccessedByLayers("Service", "Component", "Security", "Web", "Config", "Domain")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Persistence", "Plugin", "Client", "Service", "Component", "Security", "Web", "Util", "Config")
            .whereLayer("Plugin").mayOnlyBeAccessedByLayers("Component", "Service", "Web")

            .ignoreDependency(belongToAnyOf(JasperApplication.class), alwaysTrue())
            .ignoreDependency(alwaysTrue(), belongToAnyOf(
                jasper.config.Constants.class,
                Props.class
            ));
            
        rule.check(classes);
    }
}

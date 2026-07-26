package jasper.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("api-docs")
public class OpenApiConfiguration {

    public static final String API_FIRST_PACKAGE = "jasper.web.api";

    @Bean
    public GroupedOpenApi openApi(
        Props props
    ) {
        Props.ApiDocs properties = props.getApiDocs();
        return GroupedOpenApi
            .builder()
			.displayName(properties.getTitle())
            .group("jasper")
            .packagesToScan("jasper.web.rest")
            .pathsToMatch(properties.getDefaultIncludePattern())
			.addOpenApiCustomizer(openApi -> {
				openApi.getInfo().setTitle(properties.getTitle());
				openApi.getInfo().setDescription(properties.getDescription());
				openApi.getInfo().setVersion(properties.getVersion());
				openApi.getInfo().setLicense(properties.getLicense());
				properties.getServers().forEach(openApi::addServersItem);
			})
            .build();
    }
}

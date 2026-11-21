package reciter.swagger;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    servers = {
        // This is the fix: Setting the URL to the root context path forces 
        // the browser to resolve the full URL using the current page's scheme (HTTPS).
        @Server(url = "/", description = "Relative context path (Forces HTTPS)") 
    }
)
public class OpenApiConfig {}

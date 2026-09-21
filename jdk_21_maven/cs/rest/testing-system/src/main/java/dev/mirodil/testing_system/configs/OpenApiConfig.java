package dev.mirodil.testing_system.configs;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@OpenAPIDefinition(
        info = @Info(
                contact = @Contact(
                        name = "Mirodil Kamilov",
                        email = "mirodilkamilov@gmail.com",
                        url = "https://www.linkedin.com/in/mirodilkamilov"
                ),
                title = "Testing System REST API Documentation",
                description = """
                        Welcome to the Testing System REST API! This backend application facilitates the management of testing activities, including test creation, participation, and automatic result evaluation. Designed with scalability, security, and modularity in mind, this API empowers both administrators and test takers to efficiently handle testing processes.
                        
                        Key Features:
                        
                        - **User Management**: Admins manage accounts and assign tests, while test takers participate seamlessly with autosave functionality.
                        - **Test Management**: Create and manage tests with diverse question types (MCQs, checkboxes, true/false, text-based).
                        - **Results Management**: Automated result evaluation with detailed summaries and pass/fail statuses.
                        
                        Explore the API endpoints interactively via Swagger-UI or use the [Postman collection](https://www.postman.com/mirodil-dev/workspace/testing-system) for pre-configured examples. The source code is accessible through the [GitHub repository.](https://github.com/mirodilkamilov/testing-system)
                        
                        **_Note: \uD83D\uDEA7 This project is a work in progress. Some interruptions may occur in the production environment. \uD83D\uDEA7_**""",
                version = "1.0",
                license = @License(
                        name = "MIT licence",
                        url = "https://github.com/mirodilkamilov/testing-system/blob/main/LICENSE"
                )
        ),
        servers = {
                @Server(
                        description = "PROD ENV",
                        url = "https://testing.mirodil.dev/api"
                ),
                @Server(
                        description = "Local ENV",
                        url = "http://localhost:8080/api"
                ),
        },
        security = {
                @SecurityRequirement(
                        name = "bearerAuth"
                )
        }
)
@SecurityScheme(
        name = "bearerAuth",
        scheme = "bearer",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER
)
@Configuration
public class OpenApiConfig implements WebMvcConfigurer {
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Redirect trailing slash to the correct Swagger UI endpoint
        registry.addRedirectViewController("/swagger-ui.html/", "/swagger-ui.html");
        registry.addRedirectViewController("/swagger-ui/index.html/", "/swagger-ui/index.html");
    }
}

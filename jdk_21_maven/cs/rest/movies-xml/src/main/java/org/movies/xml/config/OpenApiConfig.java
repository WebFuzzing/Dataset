package org.movies.xml.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.XML;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * The published schema has to describe the XML shape exactly, because that schema is
 * the only thing a client has to go on. Jackson's XML annotations do not surface in
 * the generated document by themselves, so the xml metadata is attached here.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI moviesXmlOpenApi() {
        return new OpenAPI().info(new Info()
                .title("movies-xml")
                .version("1.0.0")
                .description("An XML-only movie catalogue. Every request and response body is application/xml."));
    }

    /**
     * Two failures are produced by the exception handler rather than by any endpoint, so no
     * {@code @ApiResponse} on the controller would ever mention them, and every operation
     * can hit both. Declaring them here keeps the published schema honest without repeating
     * the same annotation on all seven endpoints, and keeps it that way when an endpoint
     * is added.
     *
     * <p>The handler also turns anything unexpected into a 500, and that one is deliberately
     * left undeclared: this is a fuzzing target, and a 500 is a fault to be reported, not a
     * documented answer a tool should accept as expected behaviour.
     */
    @Bean
    public OpenApiCustomizer handlerWideResponsesCustomizer() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }
            for (PathItem path : openApi.getPaths().values()) {
                for (Operation operation : path.readOperations()) {
                    ApiResponses responses = operation.getResponses();
                    if (responses == null) {
                        continue;
                    }
                    errorResponse(responses, "405", "Method not supported on this path");
                    errorResponse(responses, "406", "Client asked for a type other than application/xml");
                }
            }
        };
    }

    /** Adds one error document response, leaving an explicitly annotated one alone. */
    private static void errorResponse(ApiResponses responses, String code, String description) {
        if (responses.containsKey(code)) {
            return;
        }
        responses.addApiResponse(code, new ApiResponse()
                .description(description)
                .content(new Content().addMediaType("application/xml",
                        new io.swagger.v3.oas.models.media.MediaType()
                                .schema(new Schema<>().$ref("#/components/schemas/ErrorDto")))));
    }

    @Bean
    public OpenApiCustomizer xmlMetadataCustomizer() {
        return openApi -> {
            if (openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) {
                return;
            }
            Map<String, Schema> schemas = openApi.getComponents().getSchemas();

            named(schemas, "MovieDto", "movie", schema -> {
                attribute(schema, "id");
                attribute(schema, "year");
                wrappedArray(schema, "cast", "cast", "actor");
            });
            named(schemas, "DirectorDto", "director", schema -> attribute(schema, "nationality"));
            named(schemas, "ActorDto", "actor", schema -> attribute(schema, "billing"));
            named(schemas, "MoviesDto", "movies", schema -> unwrappedArray(schema, "movies", "movie"));
            named(schemas, "StatsDto", "stats", schema -> {
                attribute(schema, "total");
                unwrappedArray(schema, "genres", "genre");
            });
            named(schemas, "GenreStatsDto", "genre", schema -> {
                attribute(schema, "name");
                attribute(schema, "count");
            });
            named(schemas, "ErrorDto", "error", schema -> attribute(schema, "status"));
        };
    }

    /** Names the root element of a schema and then lets the caller decorate its properties. */
    private static void named(Map<String, Schema> schemas,
                              String schemaName,
                              String elementName,
                              java.util.function.Consumer<Schema<?>> decorate) {
        Schema<?> schema = schemas.get(schemaName);
        if (schema == null) {
            return;
        }
        schema.setXml(new XML().name(elementName));
        decorate.accept(schema);
    }

    private static void attribute(Schema<?> parent, String property) {
        Schema<?> target = property(parent, property);
        if (target != null) {
            target.setXml(new XML().name(property).attribute(true));
        }
    }

    /** An array rendered as {@code <wrapper><item/></wrapper>}. */
    private static void wrappedArray(Schema<?> parent, String property, String wrapperName, String itemName) {
        Schema<?> array = property(parent, property);
        if (array == null) {
            return;
        }
        array.setXml(new XML().name(wrapperName).wrapped(true));
        if (array.getItems() != null) {
            array.getItems().setXml(new XML().name(itemName));
        }
    }

    /** An array whose items are direct children of the enclosing element. */
    private static void unwrappedArray(Schema<?> parent, String property, String itemName) {
        Schema<?> array = property(parent, property);
        if (array == null) {
            return;
        }
        array.setXml(new XML().name(itemName).wrapped(false));
        if (array.getItems() != null) {
            array.getItems().setXml(new XML().name(itemName));
        }
    }

    private static Schema<?> property(Schema<?> parent, String name) {
        Map<String, Schema> properties = parent.getProperties();
        return (properties == null) ? null : properties.get(name);
    }
}

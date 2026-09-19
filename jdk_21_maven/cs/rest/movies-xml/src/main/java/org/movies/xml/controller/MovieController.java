package org.movies.xml.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.movies.xml.domain.Genre;
import org.movies.xml.dto.ErrorDto;
import org.movies.xml.dto.MovieDto;
import org.movies.xml.dto.MoviesDto;
import org.movies.xml.dto.StatsDto;
import org.movies.xml.service.MovieService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * The whole API. Every endpoint speaks XML and nothing else.
 */
@RestController
@RequestMapping(path = "/movies", produces = MediaType.APPLICATION_XML_VALUE)
public class MovieController {

    private static final String XML = MediaType.APPLICATION_XML_VALUE;

    private final MovieService service;

    public MovieController(MovieService service) {
        this.service = service;
    }

    @Operation(summary = "Create a movie")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created",
                    content = @Content(mediaType = XML, schema = @Schema(implementation = MovieDto.class))),
            @ApiResponse(responseCode = "400", description = "Malformed or invalid document",
                    content = @Content(mediaType = XML, schema = @Schema(implementation = ErrorDto.class))),
            @ApiResponse(responseCode = "409", description = "Title and year already taken",
                    content = @Content(mediaType = XML, schema = @Schema(implementation = ErrorDto.class))),
            @ApiResponse(responseCode = "415", description = "Body is not application/xml",
                    content = @Content(mediaType = XML, schema = @Schema(implementation = ErrorDto.class)))
    })
    @PostMapping(consumes = XML, produces = XML)
    public ResponseEntity<MovieDto> create(@Valid @RequestBody MovieDto request) {
        MovieDto created = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).contentType(MediaType.APPLICATION_XML).body(created);
    }

    @Operation(summary = "List movies, optionally filtered and sorted")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Matching movies, possibly none",
                    content = @Content(mediaType = XML, schema = @Schema(implementation = MoviesDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid query parameters",
                    content = @Content(mediaType = XML, schema = @Schema(implementation = ErrorDto.class)))
    })
    @GetMapping(produces = XML)
    public MoviesDto search(@RequestParam(required = false) Genre genre,
                            @RequestParam(required = false) String titleContains,
                            @RequestParam(required = false) Integer minYear,
                            @RequestParam(required = false) Integer maxYear,
                            @RequestParam(required = false) Double minRating,
                            @RequestParam(required = false) String sort) {
        List<MovieDto> movies = service.search(genre, titleContains, minYear, maxYear, minRating, sort);
        return new MoviesDto(movies);
    }

    @Operation(summary = "Per genre counts and average ratings")
    @ApiResponse(responseCode = "200", description = "Catalogue statistics",
            content = @Content(mediaType = XML, schema = @Schema(implementation = StatsDto.class)))
    @GetMapping(path = "/stats", produces = XML)
    public StatsDto stats() {
        return service.stats();
    }

    @Operation(summary = "Fetch one movie")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The movie",
                    content = @Content(mediaType = XML, schema = @Schema(implementation = MovieDto.class))),
            @ApiResponse(responseCode = "404", description = "No movie with that id",
                    content = @Content(mediaType = XML, schema = @Schema(implementation = ErrorDto.class)))
    })
    @GetMapping(path = "/{id}", produces = XML)
    public MovieDto byId(@PathVariable Long id) {
        return service.findById(id);
    }

    @Operation(summary = "Replace a movie in full")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The stored movie",
                    content = @Content(mediaType = XML, schema = @Schema(implementation = MovieDto.class))),
            @ApiResponse(responseCode = "400", description = "Malformed or invalid document",
                    content = @Content(mediaType = XML, schema = @Schema(implementation = ErrorDto.class))),
            @ApiResponse(responseCode = "404", description = "No movie with that id",
                    content = @Content(mediaType = XML, schema = @Schema(implementation = ErrorDto.class))),
            @ApiResponse(responseCode = "409", description = "Title and year already taken",
                    content = @Content(mediaType = XML, schema = @Schema(implementation = ErrorDto.class))),
            @ApiResponse(responseCode = "415", description = "Body is not application/xml",
                    content = @Content(mediaType = XML, schema = @Schema(implementation = ErrorDto.class)))
    })
    @PutMapping(path = "/{id}", consumes = XML, produces = XML)
    public MovieDto replace(@PathVariable Long id, @Valid @RequestBody MovieDto request) {
        return service.replace(id, request);
    }

    @Operation(summary = "Update only the fields present in the document")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The stored movie",
                    content = @Content(mediaType = XML, schema = @Schema(implementation = MovieDto.class))),
            @ApiResponse(responseCode = "400", description = "Malformed or invalid document",
                    content = @Content(mediaType = XML, schema = @Schema(implementation = ErrorDto.class))),
            @ApiResponse(responseCode = "404", description = "No movie with that id",
                    content = @Content(mediaType = XML, schema = @Schema(implementation = ErrorDto.class))),
            @ApiResponse(responseCode = "409", description = "Title and year already taken",
                    content = @Content(mediaType = XML, schema = @Schema(implementation = ErrorDto.class))),
            @ApiResponse(responseCode = "415", description = "Body is not application/xml",
                    content = @Content(mediaType = XML, schema = @Schema(implementation = ErrorDto.class)))
    })
    @PatchMapping(path = "/{id}", consumes = XML, produces = XML)
    public MovieDto patch(@PathVariable Long id, @RequestBody MovieDto request) {
        return service.patch(id, request);
    }

    @Operation(summary = "Delete a movie")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deleted", content = @Content),
            @ApiResponse(responseCode = "404", description = "No movie with that id",
                    content = @Content(mediaType = XML, schema = @Schema(implementation = ErrorDto.class)))
    })
    @DeleteMapping(path = "/{id}", produces = XML)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

package org.movies.xml.service;

import org.movies.xml.domain.Actor;
import org.movies.xml.domain.Genre;
import org.movies.xml.domain.Movie;
import org.movies.xml.dto.ActorDto;
import org.movies.xml.dto.DirectorDto;
import org.movies.xml.dto.GenreStatsDto;
import org.movies.xml.dto.MovieDto;
import org.movies.xml.dto.StatsDto;
import org.movies.xml.repository.MovieRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * All the branching lives here: the controllers only translate HTTP into calls on
 * this service and back.
 */
@Service
@Transactional
public class MovieService {

    private static final Pattern NATIONALITY = Pattern.compile("^[A-Z]{2}$");
    private static final Pattern WHITESPACE_RUN = Pattern.compile("\\s+");

    /** Above this rating a movie is expected to name at least one actor. */
    private static final double CAST_REQUIRED_ABOVE_RATING = 9.0;

    private static final int MAX_CAST_SIZE = 10;

    private final MovieRepository repository;

    public MovieService(MovieRepository repository) {
        this.repository = repository;
    }

    // ------------------------------------------------------------------ reads

    @Transactional(readOnly = true)
    public List<MovieDto> search(Genre genre,
                                 String titleContains,
                                 Integer minYear,
                                 Integer maxYear,
                                 Double minRating,
                                 String sort) {

        if (minYear != null && maxYear != null && minYear > maxYear) {
            throw new InvalidMovieException("minYear must not be greater than maxYear");
        }
        Comparator<Movie> comparator = comparatorFor(sort);

        List<Movie> matches = new ArrayList<>();
        for (Movie movie : repository.findAll()) {
            if (genre != null && movie.getGenre() != genre) {
                continue;
            }
            if (titleContains != null && !containsIgnoreCase(movie.getTitle(), titleContains)) {
                continue;
            }
            if (minYear != null && movie.getYear() < minYear) {
                continue;
            }
            if (maxYear != null && movie.getYear() > maxYear) {
                continue;
            }
            if (minRating != null && (movie.getRating() == null || movie.getRating() < minRating)) {
                continue;
            }
            matches.add(movie);
        }
        if (comparator != null) {
            matches.sort(comparator);
        }

        List<MovieDto> result = new ArrayList<>(matches.size());
        for (Movie movie : matches) {
            result.add(toDto(movie));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public MovieDto findById(Long id) {
        return toDto(load(id));
    }

    @Transactional(readOnly = true)
    public StatsDto stats() {
        List<Movie> all = repository.findAll();

        List<GenreStatsDto> perGenre = new ArrayList<>();
        for (Genre genre : Genre.values()) {
            long count = 0;
            double sum = 0.0;
            int rated = 0;
            for (Movie movie : all) {
                if (movie.getGenre() != genre) {
                    continue;
                }
                count++;
                if (movie.getRating() != null) {
                    sum += movie.getRating();
                    rated++;
                }
            }
            if (count == 0) {
                continue;
            }
            Double average = (rated == 0) ? null : round2(sum / rated);
            perGenre.add(new GenreStatsDto(genre.name(), count, average));
        }
        return new StatsDto(all.size(), perGenre);
    }

    // ----------------------------------------------------------------- writes

    public MovieDto create(MovieDto request) {
        String title = request.getTitle();
        Integer year = request.getYear();

        checkCast(request.getCast());
        checkRatingAgainstCast(request.getRating(), request.getCast());
        checkDuplicate(title, year, null);

        Movie movie = new Movie();
        movie.setTitle(title);
        movie.setYear(year);
        movie.setGenre(request.getGenre());
        movie.setRating(request.getRating());
        movie.setDirectorName(request.getDirector().getName());
        movie.setDirectorNationality(request.getDirector().getNationality());
        movie.setCast(toActors(request.getCast()));

        return toDto(repository.save(movie));
    }

    public MovieDto replace(Long id, MovieDto request) {
        Movie movie = load(id);

        checkCast(request.getCast());
        checkRatingAgainstCast(request.getRating(), request.getCast());
        checkDuplicate(request.getTitle(), request.getYear(), id);

        movie.setTitle(request.getTitle());
        movie.setYear(request.getYear());
        movie.setGenre(request.getGenre());
        movie.setRating(request.getRating());
        movie.setDirectorName(request.getDirector().getName());
        movie.setDirectorNationality(request.getDirector().getNationality());
        movie.setCast(toActors(request.getCast()));

        return toDto(repository.save(movie));
    }

    /**
     * Applies only the fields that were literally present in the received document.
     * Each field is its own branch, and an empty cast element clears the cast while
     * an absent one leaves it untouched.
     */
    public MovieDto patch(Long id, MovieDto request) {
        Movie movie = load(id);

        String title = movie.getTitle();
        Integer year = movie.getYear();

        if (request.isPresent("title")) {
            String value = request.getTitle();
            if (value == null || value.trim().isEmpty() || value.length() > 200) {
                throw new InvalidMovieException("title must be between 1 and 200 characters");
            }
            title = value;
        }
        if (request.isPresent("year")) {
            Integer value = request.getYear();
            if (value == null || value < 1888 || value > 2100) {
                throw new InvalidMovieException("year must be between 1888 and 2100");
            }
            year = value;
        }
        if (request.isPresent("title") || request.isPresent("year")) {
            checkDuplicate(title, year, id);
            movie.setTitle(title);
            movie.setYear(year);
        }
        if (request.isPresent("genre")) {
            if (request.getGenre() == null) {
                throw new InvalidMovieException("genre must be one of " + java.util.Arrays.toString(Genre.values()));
            }
            movie.setGenre(request.getGenre());
        }
        if (request.isPresent("rating")) {
            Double value = request.getRating();
            if (value != null && (value < 0.0 || value > 10.0)) {
                throw new InvalidMovieException("rating must be between 0.0 and 10.0");
            }
            movie.setRating(value);
        }
        if (request.isPresent("director")) {
            DirectorDto director = request.getDirector();
            if (director == null) {
                throw new InvalidMovieException("director must carry a name");
            }
            if (director.getName() != null) {
                String name = director.getName();
                if (name.trim().isEmpty() || name.length() > 120) {
                    throw new InvalidMovieException("director name must be between 1 and 120 characters");
                }
                movie.setDirectorName(name);
            }
            if (director.getNationality() != null) {
                checkNationality(director.getNationality());
                movie.setDirectorNationality(director.getNationality());
            }
        }

        List<ActorDto> cast = request.getCastAsReceived();
        if (cast != null) {
            checkCast(cast);
            movie.setCast(toActors(cast));
        }

        // The rating/cast rule is evaluated on the merged state, not on the patch alone.
        checkRatingAgainstCast(movie.getRating(), toActorDtos(movie.getCast()));

        return toDto(repository.save(movie));
    }

    public void delete(Long id) {
        repository.delete(load(id));
    }

    // ------------------------------------------------------------ b. rules

    private void checkDuplicate(String title, Integer year, Long excludedId) {
        String normalized = normalize(title);
        for (Movie other : repository.findAll()) {
            if (excludedId != null && excludedId.equals(other.getId())) {
                continue;
            }
            if (normalize(other.getTitle()).equals(normalized) && other.getYear().equals(year)) {
                throw new DuplicateMovieException(title, year);
            }
        }
    }

    private void checkCast(List<ActorDto> cast) {
        if (cast == null) {
            return;
        }
        if (cast.size() > MAX_CAST_SIZE) {
            throw new InvalidMovieException("cast must not hold more than " + MAX_CAST_SIZE + " actors");
        }
        Set<Integer> seen = new HashSet<>();
        for (ActorDto actor : cast) {
            if (actor == null || actor.getName() == null || actor.getName().trim().isEmpty()) {
                throw new InvalidMovieException("every actor must carry a name");
            }
            if (actor.getName().length() > 120) {
                throw new InvalidMovieException("actor name must be between 1 and 120 characters");
            }
            Integer billing = actor.getBilling();
            if (billing == null) {
                continue;
            }
            if (billing < 1 || billing > 99) {
                throw new InvalidMovieException("billing must be between 1 and 99");
            }
            if (!seen.add(billing)) {
                throw new InvalidMovieException("billing values must be unique within a cast");
            }
        }
    }

    private void checkRatingAgainstCast(Double rating, List<ActorDto> cast) {
        if (rating != null && rating > CAST_REQUIRED_ABOVE_RATING && (cast == null || cast.isEmpty())) {
            throw new InvalidMovieException(
                    "a movie rated above " + CAST_REQUIRED_ABOVE_RATING + " must list at least one actor");
        }
    }

    /**
     * Only PATCH reaches this. Create and replace take a {@code @Valid} body, so the
     * {@code @Pattern} on DirectorDto.nationality has already rejected the same values by
     * the time they get here; PATCH deliberately skips bean validation, because a document
     * that carries one field cannot satisfy constraints on the fields it leaves out.
     */
    private void checkNationality(String nationality) {
        if (nationality != null && !NATIONALITY.matcher(nationality).matches()) {
            throw new InvalidMovieException("nationality must be exactly two upper case letters");
        }
    }

    // -------------------------------------------------------------- helpers

    private Movie load(Long id) {
        return repository.findById(id).orElseThrow(() -> new MovieNotFoundException(id));
    }

    private Comparator<Movie> comparatorFor(String sort) {
        if (sort == null) {
            return null;
        }
        switch (sort) {
            case "title":
                return Comparator.comparing(m -> m.getTitle().toLowerCase(Locale.ROOT));
            case "year":
                return Comparator.comparing(Movie::getYear);
            case "rating":
                return Comparator.comparing(Movie::getRating, Comparator.nullsLast(Comparator.naturalOrder()));
            default:
                throw new InvalidMovieException("sort must be one of title, year, rating");
        }
    }

    private static boolean containsIgnoreCase(String haystack, String needle) {
        return haystack.toLowerCase(Locale.ROOT).contains(needle.toLowerCase(Locale.ROOT));
    }

    private static String normalize(String title) {
        if (title == null) {
            return "";
        }
        return WHITESPACE_RUN.matcher(title.trim()).replaceAll(" ").toLowerCase(Locale.ROOT);
    }

    private static Double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static List<Actor> toActors(List<ActorDto> cast) {
        List<Actor> actors = new ArrayList<>();
        if (cast != null) {
            for (ActorDto dto : cast) {
                actors.add(new Actor(dto.getName(), dto.getBilling()));
            }
        }
        return actors;
    }

    private static List<ActorDto> toActorDtos(List<Actor> cast) {
        List<ActorDto> dtos = new ArrayList<>();
        if (cast != null) {
            for (Actor actor : cast) {
                ActorDto dto = new ActorDto();
                dto.setName(actor.getName());
                dto.setBilling(actor.getBilling());
                dtos.add(dto);
            }
        }
        return dtos;
    }

    private static MovieDto toDto(Movie movie) {
        MovieDto dto = new MovieDto();
        dto.setId(movie.getId());
        dto.setTitle(movie.getTitle());
        dto.setYear(movie.getYear());
        dto.setGenre(movie.getGenre());
        dto.setRating(movie.getRating());

        DirectorDto director = new DirectorDto();
        director.setName(movie.getDirectorName());
        director.setNationality(movie.getDirectorNationality());
        dto.setDirector(director);

        dto.setCast(toActorDtos(movie.getCast()));
        return dto;
    }
}

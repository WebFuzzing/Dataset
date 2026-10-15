package it.pagopa.pn.papertracker.it;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.papertracker.it.model.ProductTestCase;
import org.junit.jupiter.params.provider.Arguments;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class SequenceLoader {

    public static Stream<Arguments> loadScenarios(URI uri, URI exclude) throws Exception {

        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

        List<Arguments> result = new ArrayList<>();

        Path dir = Path.of(uri);
        Path excludedPath = exclude != null ? Path.of(exclude) : null;

        try (Stream<Path> paths = Files.walk(dir)) {
            List<Path> jsonFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> Objects.isNull(excludedPath) || !p.startsWith(excludedPath))
                    .filter(p -> p.toString().endsWith(".json"))
                    .sorted()
                    .toList();
            for (Path path : jsonFiles) {
                String raw = Files.readString(path);
                ProductTestCase scenario = mapper.readValue(raw, ProductTestCase.class);
                result.add(Arguments.of(path.toAbsolutePath().toString(), scenario));
            }
        }

        if (result.isEmpty()) {
            throw new IllegalStateException("No scenarios found!");
        }

        return result.stream();
    }
}

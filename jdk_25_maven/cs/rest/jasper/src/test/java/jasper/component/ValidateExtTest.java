package jasper.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ValidateExtTest {

    Validate validate = new Validate();
    ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        validate.objectMapper = mapper;
    }

    @Test
    void testMerge() throws IOException {
        var root = (ObjectNode) mapper.readTree("""
        {
            "tag": "",
            "config": {
                "defaultSort": ["published"],
                "addTags": ["public"]
            }
        }""");
        var a = (ObjectNode) mapper.readTree("""
        {
            "tag": "a",
            "config": {
                "aProp": "a",
                "addTags": ["a"]
            }
        }""");
        var ab = (ObjectNode) mapper.readTree("""
        {
            "tag": "a/b",
            "config": {
                "abProp": "ab",
                "defaultSort": ["modified"]
            }
        }""");

        var defaults = List.of(root, a, ab);
        var finalMerged = defaults.stream()
            .sorted(Comparator.<ObjectNode, String>comparing(t -> t.get("tag").asText()))
            .reduce(null, validate::merge);
        assertThat(finalMerged.get("config").get("abProp").asText()).isEqualTo("ab");
        assertThat(finalMerged.get("config").get("aProp").asText()).isEqualTo("a");

        // Custom merge: arrays are OVERRIDDEN
        assertThat(finalMerged.get("config").get("defaultSort")).hasSize(1);
        assertThat(finalMerged.get("config").get("defaultSort").get(0).asText()).isEqualTo("modified");

        assertThat(finalMerged.get("config").get("addTags")).hasSize(1);
        assertThat(finalMerged.get("config").get("addTags").get(0).asText()).isEqualTo("a");
    }

    @Test
    void testMergeOrder() throws IOException {
        var root = (ObjectNode) mapper.readTree("""
        {
            "tag": "",
            "config": {
                "defaultSort": ["published"],
                "overrideMe": "root"
            }
        }""");
        var a = (ObjectNode) mapper.readTree("""
        {
            "tag": "a",
            "config": {
                "defaultSort": ["created"],
                "overrideMe": "a"
            }
        }""");
        var ab = (ObjectNode) mapper.readTree("""
        {
            "tag": "a/b",
            "config": {
                "defaultSort": ["modified"],
                "overrideMe": "ab"
            }
        }""");

        var defaults = List.of(root, a, ab);
        // Ascending: "", "a", "a/b"
        var merged = defaults.stream()
            .sorted(Comparator.<ObjectNode, String>comparing(t -> t.get("tag").asText()))
            .reduce(null, validate::merge);
        assertThat(merged.get("config").get("overrideMe").asText()).isEqualTo("ab");
        assertThat(merged.get("config").get("defaultSort")).hasSize(1);
        assertThat(merged.get("config").get("defaultSort").get(0).asText()).isEqualTo("modified");
    }

    @Test
    void testNotesMerge() throws IOException {
        var root = (ObjectNode) mapper.readTree("""
        {
            "tag": "",
            "defaults": {
                "defaultSort": ["published"],
                "addTags": ["public"]
            }
        }""");
        var notes = (ObjectNode) mapper.readTree("""
        {
            "tag": "notes",
            "defaults": {
                "defaultCols": 0,
                "badges": ["reminder", "important"],
                "defaultSort": ["modified"],
                "addTags": []
            }
        }""");

        var defaults = List.of(root, notes);
        var merged = defaults.stream()
            .sorted(Comparator.<ObjectNode, String>comparing(t -> t.get("tag").asText()))
            .map(t -> (ObjectNode) t.get("defaults"))
            .reduce(null, validate::merge);
        // Override arrays, NOT append
        assertThat(merged.get("defaultSort")).hasSize(1);
        assertThat(merged.get("defaultSort").get(0).asText()).isEqualTo("modified");

        assertThat(merged.get("addTags")).hasSize(0);
    }

    @Test
    void testMergeWithBothNull() {
        var result = validate.merge(null, null);
        assertThat(result).isNotNull();
        assertThat(result.isEmpty()).isTrue();
    }
}

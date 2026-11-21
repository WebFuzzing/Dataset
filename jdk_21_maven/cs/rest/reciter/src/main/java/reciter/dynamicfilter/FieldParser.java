package reciter.dynamicfilter;

import java.util.*;

public class FieldParser {

    public static Map<String, Object> parse(Set<String> fields) {
        Map<String, Object> map = new HashMap<>();
        if (fields == null) return map;

        for (String field : fields) {
            String[] parts = field.split("\\.");
            Map<String, Object> current = map;
            for (int i = 0; i < parts.length; i++) {
                String part = parts[i];
                if (i == parts.length - 1) {
                    current.put(part, null);
                } else {
                    current = (Map<String, Object>) current.computeIfAbsent(part, k -> new HashMap<>());
                }
            }
        }
        return map;
    }
}


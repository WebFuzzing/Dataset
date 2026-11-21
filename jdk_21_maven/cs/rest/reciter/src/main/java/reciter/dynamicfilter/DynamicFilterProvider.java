package reciter.dynamicfilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.ser.PropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import java.util.Map;

public class DynamicFilterProvider {

    private final Map<String, Object> fields;

    public DynamicFilterProvider(Map<String, Object> fields) {
        this.fields = fields;
    }

    public ObjectWriter getWriter(ObjectMapper mapper) {
        PropertyFilter filter = new SimpleBeanPropertyFilter() {
            @Override
            protected boolean include(com.fasterxml.jackson.databind.ser.BeanPropertyWriter writer) {
                return fields == null || fields.containsKey(writer.getName());
            }
        };

        SimpleFilterProvider provider = new SimpleFilterProvider();
        provider.addFilter("dynamicFilter", filter);
        provider.setFailOnUnknownId(false); // models without @JsonFilter

        return mapper.writer(provider);
    }
}



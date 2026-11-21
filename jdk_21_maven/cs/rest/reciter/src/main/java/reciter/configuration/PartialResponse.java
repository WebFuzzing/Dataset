/* package reciter.configuration;

import ch.mfrey.jackson.antpathfilter.AntPathPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import org.springframework.http.converter.json.MappingJacksonValue;

public class PartialResponse extends MappingJacksonValue {

    public PartialResponse(final Object value, final String... filters) {
        super(value);
        if (null == filters || filters.length <= 0) {
            setFilters(new SimpleFilterProvider().addFilter("antPathFilter", new AntPathPropertyFilter("**")));
        } else {
            setFilters(new SimpleFilterProvider().addFilter("antPathFilter", new AntPathPropertyFilter(filters)));
        }
    }
}
 */
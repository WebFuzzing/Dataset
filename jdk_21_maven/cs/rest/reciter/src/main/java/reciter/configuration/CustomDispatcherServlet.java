/* package reciter.configuration;

import ch.mfrey.jackson.antpathfilter.AntPathFilterMixin;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.util.List;

@Configuration
public class CustomDispatcherServlet extends WebMvcConfigurerAdapter {

    @Override
    public void configureMessageConverters(final List<HttpMessageConverter<?>> messageConverters) {
        ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().mixIn(PartialResponse.class, AntPathFilterMixin.class).build();
        messageConverters.add(new MappingJackson2HttpMessageConverter(objectMapper));
        extendMessageConverters(messageConverters);
    }
}
 */
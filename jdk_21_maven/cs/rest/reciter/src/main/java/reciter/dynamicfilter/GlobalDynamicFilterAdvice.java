package reciter.dynamicfilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalDynamicFilterAdvice implements ResponseBodyAdvice<Object> {

    private final ObjectMapper mapper;
    private final HttpServletRequest request;

    public GlobalDynamicFilterAdvice(ObjectMapper mapper, HttpServletRequest request) {
        this.mapper = mapper;
        this.request = request;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest serverRequest,
                                  ServerHttpResponse serverResponse) {

        if (body == null) return null;

        String fieldsParam = request.getParameter("fields");
        if (fieldsParam == null || fieldsParam.isBlank()) return body;

        Set<String> fieldsSet = Arrays.stream(fieldsParam.split(","))
                .map(String::trim)
                .collect(Collectors.toSet());

        Map<String, Object> fieldTree = FieldParser.parse(fieldsSet);

        try {
            ObjectWriter writer = new DynamicFilterProvider(fieldTree).getWriter(mapper);
            JsonNode node = mapper.readTree(writer.writeValueAsString(body));
            return node;
        } catch (Exception e) {
            throw new RuntimeException("Dynamic filtering failed", e);
        }
    }
}




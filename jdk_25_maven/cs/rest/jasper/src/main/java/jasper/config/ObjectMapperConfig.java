package jasper.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ObjectMapperConfig {

	@Autowired
	ObjectMapper objectMapper;

	@PostConstruct
	void initStatic() {
		JacksonConfiguration.om = objectMapper;
	}
}

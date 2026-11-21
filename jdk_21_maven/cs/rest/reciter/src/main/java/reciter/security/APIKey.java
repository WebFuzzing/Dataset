package reciter.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class APIKey {
	
	private String adminApiKey;
	private String consumerApiKey;

}

package reciter.database.dyanmodb.files;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import reciter.database.dynamodb.model.Identity;
import reciter.service.IdentityService;

/**
 * This class deals with import of Identity from files and import it into dynamodb
 * @author szd2013
 *
 */
@Component
public class IdentityFileImport {
	
	private static final Logger log = LoggerFactory.getLogger(IdentityFileImport.class);
	
	@Autowired
	private IdentityService identityService;
	
	/**
	 * This function imports identity data to identity table
	 */
	public void importIdentity() {
		ObjectMapper mapper = new ObjectMapper();
		List<Identity> identities = null;
		try {
			identities = Arrays.asList(mapper.readValue(getClass().getResourceAsStream("/files/Identity.json"), Identity[].class));
		} catch (IOException e) {
			log.error("IOException", e);
		}
		if(identities != null 
				&&
				identities.size() == identityService.getItemCount()) {
			log.info("The file Identity.json and the Identity table in DynamoDb is isomorphic and hence skipping import.");
		} else {
			if(identities != null 
					&&
					!identities.isEmpty()) {
				log.info("The file Identity.json and the Identity table in DynamoDb is not isomorphic and hence starting import.");
				
				identityService.save(identities.stream().map(identity->identity.getIdentity()).collect(Collectors.toList()));
			}
		}
	}
}

package reciter.database.dyanmodb.files;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import reciter.database.dynamodb.model.InstitutionAfid;
import reciter.service.dynamo.IDynamoDbInstitutionAfidService;

/**
 * This class deals with import of InstitutionAfid from files and import it into dynamodb
 * @author szd2013
 *
 */
@Component
public class InstitutionAfidFileImport {
	
	private static final Logger log = LoggerFactory.getLogger(InstitutionAfidFileImport.class);
	
	@Autowired
	private IDynamoDbInstitutionAfidService institutionAfIdService;
	
	/**
	 * This function imports identity data to identity table
	 */
	public void importInstitutionAfids() {
		ObjectMapper mapper = new ObjectMapper();
		List<InstitutionAfid> institutionAfids = null;
		try {
			institutionAfids = Arrays.asList(mapper.readValue(getClass().getResourceAsStream("/files/InstitutionAfid.json"), InstitutionAfid[].class));
		} catch (IOException e) {
			log.error("IOException", e);
		}
		if(institutionAfids != null 
				&&
				institutionAfids.size() == institutionAfIdService.getItemCount()) {
			log.info("The file InstitutionAfid.json and the InstitutionAfid table in DynamoDb is isomorphic and hence skipping import.");
		} else {
			if(institutionAfids != null 
					&&
					!institutionAfids.isEmpty()) {
				log.info("The file InstitutionAfid.json and the InstitutionAfid table in DynamoDb is not isomorphic and hence starting import.");
				
				institutionAfIdService.save(institutionAfids);
			}
		}
	}

}

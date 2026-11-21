package reciter.database.dyanmodb.files;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import reciter.database.dynamodb.model.MeshTerm;
import reciter.service.IDynamoDbMeshTermService;

/**
 * This class deals with import of MeshTerm from files and import it into dynamodb
 * @author szd2013
 *
 */
@Component
public class MeshTermFileImport {
	private static final Logger log = LoggerFactory.getLogger(MeshTermFileImport.class);
	
	@Autowired
	private IDynamoDbMeshTermService meshTermService;
	
	/**
	 * This function imports identity data to identity table
	 */
	public void importMeshTerms() {
		ObjectMapper mapper = new ObjectMapper();
		List<MeshTerm> meshTerms = null;
		try {
			meshTerms = Arrays.asList(mapper.readValue(getClass().getResourceAsStream("/files/MeshTerm.json"), MeshTerm[].class));
		} catch (IOException e) {
			log.error("IOException", e);
		}
		if(meshTerms != null 
				&&
				meshTerms.size() == meshTermService.getItemCount()) {
			log.info("The file MeshTerm.json and the MeshTerm table in DynamoDb is isomorphic and hence skipping import.");
		} else {
			if(meshTerms != null 
					&&
					!meshTerms.isEmpty()) {
				log.info("The file MeshTerm.json and the MeshTerm table in DynamoDb is not isomorphic and hence starting import.");
				meshTermService.save(meshTerms);
			}
		}
	}
}

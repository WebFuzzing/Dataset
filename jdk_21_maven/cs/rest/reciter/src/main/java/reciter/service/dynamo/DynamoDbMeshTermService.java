package reciter.service.dynamo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import reciter.database.dynamodb.model.MeshTerm;
import reciter.database.dynamodb.repository.DynamoMeshTermRepository;
import reciter.service.IDynamoDbMeshTermService;

@Service("dynamoDbMeshTermService")
@RequiredArgsConstructor
public class DynamoDbMeshTermService implements IDynamoDbMeshTermService {

    private final  DynamoMeshTermRepository dynamoMeshTermRepository;

    @Override
    public void save(List<MeshTerm> meshTerms) {
        dynamoMeshTermRepository.saveAll(meshTerms);
    }

    @Override
    public List<MeshTerm> findAll() {
        Iterable<MeshTerm> meshTermIterable = dynamoMeshTermRepository.findAll();
        List<MeshTerm> meshTerms = new ArrayList<>();
        Iterator<MeshTerm> iterator = meshTermIterable.iterator();
        while (iterator.hasNext()) {
            MeshTerm meshTerm = iterator.next();
            meshTerms.add(meshTerm);
        }
        return meshTerms;
    }

	@Override
	public long getItemCount() {
		return  dynamoMeshTermRepository.getItemCount();
	}
}

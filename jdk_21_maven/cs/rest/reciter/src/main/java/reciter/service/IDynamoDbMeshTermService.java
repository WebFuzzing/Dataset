package reciter.service;

import reciter.database.dynamodb.model.MeshTerm;

import java.util.List;

public interface IDynamoDbMeshTermService {
    void save(List<MeshTerm> meshTerms);
    List<MeshTerm> findAll();
    long getItemCount();
}

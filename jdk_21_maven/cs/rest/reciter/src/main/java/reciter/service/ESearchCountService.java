package reciter.service;

import reciter.database.dynamodb.model.ESearchCount;

public interface ESearchCountService {
    void save(ESearchCount eSearchCount);
    ESearchCount findByUid(String uid);
}

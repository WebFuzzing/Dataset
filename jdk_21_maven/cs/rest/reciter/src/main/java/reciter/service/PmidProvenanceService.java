package reciter.service;

import java.util.List;
import java.util.Set;

import reciter.database.dynamodb.model.PmidProvenance;

public interface PmidProvenanceService {
    void save(PmidProvenance pmidProvenance);
    void saveIfNotExists(PmidProvenance pmidProvenance);
    void saveAllIfNotExists(List<PmidProvenance> pmidProvenances);
    List<PmidProvenance> findByUid(String uid);
    Set<Long> findPmidsByUid(String uid);
    Set<Long> findPmidsByUidAndStrategy(String uid, String strategy);
    void updateStrategyIfBackfill(String uid, long pmid, String realStrategy);
}

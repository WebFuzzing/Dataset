package reciter.service.dynamo;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import reciter.database.dynamodb.model.ESearchPmid;
import reciter.database.dynamodb.model.ESearchResult;
import reciter.database.dynamodb.repository.ESearchResultRepository;
import reciter.service.ESearchResultService;

@Service("eSearchResultService")
public class ESearchResultServiceImpl implements ESearchResultService {

    @Autowired
    private ESearchResultRepository eSearchResultRepository;

    @Override
    public void save(ESearchResult eSearchResult) {
        eSearchResultRepository.save(eSearchResult);
    }

    @Override
    public ESearchResult findByUid(String uid) {
        return eSearchResultRepository.findById(uid).orElseGet(() -> null);
    }

    // #732: eSearchResultRepository.findAllById returns one entry per requested uid,
    // in request order, with null in place of any uid that has no ESearchResult
    // record. A null entry (no ESearchResult record for that uid) contributes no key
    // to the result; the caller distinguishes "no retrieved corpus" from "empty
    // corpus" by key presence, not by an empty array.
    @Override
    public Map<String, List<Long>> findRetrievedPmidsByUids(List<String> uids) {
        List<ESearchResult> eSearchResults = eSearchResultRepository.findAllById(uids);
        Map<String, List<Long>> pmidsByUid = new HashMap<>();
        for (ESearchResult eSearchResult : eSearchResults) {
            if (eSearchResult == null || eSearchResult.getUid() == null) {
                continue;
            }
            Set<Long> pmids = new TreeSet<>();
            if (eSearchResult.getESearchPmids() != null) {
                for (ESearchPmid eSearchPmid : eSearchResult.getESearchPmids()) {
                    if (eSearchPmid != null && eSearchPmid.getPmids() != null) {
                        pmids.addAll(eSearchPmid.getPmids());
                    }
                }
            }
            pmidsByUid.put(eSearchResult.getUid(), new ArrayList<>(pmids));
        }
        return pmidsByUid;
    }


	@Override
	public void deleteAll() {
		eSearchResultRepository.deleteAll();
	}

	@Override
	public void delete(String uid) {
		eSearchResultRepository.deleteById(uid);
	}

	@Override
	public void stampLastFullSweepIfNewer(String uid, Instant sweepTime) {
		eSearchResultRepository.stampLastFullSweepIfNewer(uid, sweepTime);
	}

	@Override
	public Instant findLastFullSweep(String uid) {
		return eSearchResultRepository.findLastFullSweep(uid);
	}

}

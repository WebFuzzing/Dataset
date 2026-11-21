package reciter.service;

import java.util.List;

import reciter.database.dynamodb.model.CwidSkipAudit;

public interface CwidSkipAuditService {

	void save(CwidSkipAudit cwidSkipAudit);

	List<CwidSkipAudit> findByCwid(String cwid);
}

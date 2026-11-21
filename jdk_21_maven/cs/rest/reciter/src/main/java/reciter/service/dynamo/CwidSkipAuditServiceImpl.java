package reciter.service.dynamo;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import reciter.database.dynamodb.model.CwidSkipAudit;
import reciter.database.dynamodb.repository.CwidSkipAuditRepository;
import reciter.service.CwidSkipAuditService;

@Service("cwidSkipAuditService")
public class CwidSkipAuditServiceImpl implements CwidSkipAuditService {

	@Autowired
	private CwidSkipAuditRepository cwidSkipAuditRepository;

	@Override
	public void save(CwidSkipAudit cwidSkipAudit) {
		cwidSkipAuditRepository.save(cwidSkipAudit);
	}

	@Override
	public List<CwidSkipAudit> findByCwid(String cwid) {
		return cwidSkipAuditRepository.findByCwid(cwid);
	}
}

package jasper.repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Transactional(readOnly = true)
public interface OriginMixin {
	List<String> origins();
	@Modifying(clearAutomatically = true)
	void deleteByOriginAndModifiedLessThanEqual(String origin, Instant olderThan);
}

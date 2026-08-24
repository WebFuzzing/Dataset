package jasper.repository;

import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Transactional(readOnly = true)
public interface ModifiedCursor {
	Instant getCursor(String origin);
}

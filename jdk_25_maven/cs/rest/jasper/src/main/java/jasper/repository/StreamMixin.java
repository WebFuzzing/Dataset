package jasper.repository;

import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.stream.Stream;

import static org.hibernate.jpa.AvailableHints.HINT_CACHEABLE;
import static org.hibernate.jpa.AvailableHints.HINT_FETCH_SIZE;
import static org.hibernate.jpa.AvailableHints.HINT_READ_ONLY;

@Transactional(readOnly = true)
public interface StreamMixin<T> {

	@QueryHints(value = {
		@QueryHint(name = HINT_FETCH_SIZE, value = "500"),
		@QueryHint(name = HINT_CACHEABLE, value = "false"),
		@QueryHint(name = HINT_READ_ONLY, value = "true")
	})
	Stream<T> streamAllByOriginAndModifiedGreaterThanEqualOrderByModifiedDesc(String origin, Instant newerThan);

	@QueryHints(value = {
		@QueryHint(name = HINT_FETCH_SIZE, value = "500"),
		@QueryHint(name = HINT_CACHEABLE, value = "false"),
		@QueryHint(name = HINT_READ_ONLY, value = "true")
	})
	Stream<T> streamAllByOriginOrderByModifiedDesc(String origin);
}

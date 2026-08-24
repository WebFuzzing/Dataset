package jasper.repository.spec;

import jasper.domain.Ref_;
import jasper.domain.proj.Cursor;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

import static org.springframework.data.jpa.domain.Specification.unrestricted;

public class ReplicationSpec {

	public static <T extends Cursor> Specification<T> isModifiedAfter(Instant i) {
		if (i == null) return unrestricted();
		return (root, query, cb) ->
			cb.greaterThan(
				root.get(Ref_.MODIFIED),
				i);
	}

	public static <T extends Cursor> Specification<T> isModifiedBefore(Instant i) {
		if (i == null) return unrestricted();
		return (root, query, cb) ->
			cb.lessThan(
				root.get(Ref_.MODIFIED),
				i);
	}
}

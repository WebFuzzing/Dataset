package jasper.domain.proj;

import java.time.Instant;

public interface Cursor extends HasOrigin {
	Instant getModified();
}

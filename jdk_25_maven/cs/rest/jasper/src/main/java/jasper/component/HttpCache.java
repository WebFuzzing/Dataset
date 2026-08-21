package jasper.component;

import jasper.domain.proj.Cursor;
import org.springframework.data.domain.Page;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class HttpCache {
	public static CacheControl ifNotModifiedCacheControl = CacheControl
			.noCache()
			.mustRevalidate()
			.cachePrivate();


	public <T extends Cursor> ResponseEntity<List<T>> ifNotModifiedList(List<T> result) {
		return ifNotModified(result);
	}

	public <T extends Cursor> ResponseEntity<Page<T>> ifNotModifiedPage(Page<T> result) {
		return ifNotModified(result);
	}

	public <T> ResponseEntity<T> ifNotModified(T result) {
		return ResponseEntity.ok()
			.cacheControl(ifNotModifiedCacheControl)
			.body(result);
	}
}

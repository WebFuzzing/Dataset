package jasper.component;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

public interface Fetch {

	FileRequest doScrape(String url, String origin) throws IOException;

	interface FileRequest extends Closeable {
		String getMimeType();
		InputStream getInputStream() throws IOException;
	}
}

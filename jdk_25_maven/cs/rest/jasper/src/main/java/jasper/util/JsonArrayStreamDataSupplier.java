/**
 * @license APACHE LICENSE, VERSION 2.0 http://www.apache.org/licenses/LICENSE-2.0
 * @author Michael Witbrock
 * https://github.com/witbrock/JacksonStream/blob/master/src/main/java/com/michaelwitbrock/jacksonstream/JsonArrayStreamDataSupplier.java
 */
package jasper.util;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class JsonArrayStreamDataSupplier<T> implements Iterator<T> {
	/*
	 * This class wraps the Jackson streaming API for arrays (a common kind of
	 * large JSON file) in a Java 8 Stream. The initial motivation was that
	 * use of a default objectmapper to a Java array was crashing for me on
	 * a very large JSON file (> 1GB).  And there didn't seem to be good example
	 * code for handling Jackson streams as Java 8 streams, which seems natural.
	 */

	ObjectMapper mapper;
	JsonParser parser;
	boolean maybeHasNext = false;
	int count = 0;
	JsonFactory factory = new JsonFactory();
	private Class<T> type;

	public JsonArrayStreamDataSupplier(InputStream in, Class<T> type, ObjectMapper mapper) {
		this.type = type;
		this.mapper = mapper;
		try {
			// Setup and get into a state to start iterating
			parser = factory.createParser(in);
			parser.setCodec(mapper);
			JsonToken token = parser.nextToken();
			if (token == null) {
				throw new RuntimeException("Can't get any JSON Token");
			}

			// the first token is supposed to be the start of array '['
			if (!JsonToken.START_ARRAY.equals(token)) {
				// return or throw exception
				maybeHasNext = false;
				throw new RuntimeException("Can't get any JSON Token from array start");
			}
		} catch (Exception e) {
			maybeHasNext = false;
		}
		maybeHasNext = true;
	}

	/*
	This method returns the stream, and is the only method other
	than the constructor that should be used.
	*/
	public Stream<T> getStream() {
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(this, 0), false);
	}

	/* The remaining methods are what enables this to be passed to the spliterator generator,
	   since they make it Iterable.
	*/
	@Override
	public boolean hasNext() {
		if (!maybeHasNext) {
			return false; // didn't get started
		}
		try {
			return (parser.nextToken() == JsonToken.START_OBJECT);
		} catch (Exception e) {
			System.out.println("Ex" + e);
			return false;
		}
	}

	@Override
	public T next() {
		try {
			JsonNode n = parser.readValueAsTree();
			//Because we can't send T as a parameter to the mapper
			T node = mapper.convertValue(n, type);
			return node;
		} catch (IOException | IllegalArgumentException e) {
			System.out.println("Ex" + e);
			return null;
		}

	}

}

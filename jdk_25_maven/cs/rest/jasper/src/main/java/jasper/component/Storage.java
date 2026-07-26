package jasper.component;


import jasper.errors.NotFoundException;
import org.springframework.stereotype.Component;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;

import static jasper.domain.proj.HasOrigin.formatOrigin;
import static jasper.domain.proj.HasOrigin.origin;

@Component
public interface Storage {
	byte[] get(String origin, String namespace, String id);
	boolean exists(String origin, String namespace, String id);
	long size(String origin, String namespace, String id);
	InputStream stream(String origin, String namespace, String id);
	long stream(String origin, String namespace, String id, OutputStream os);
	Zipped streamZip(String origin, String namespace, String id) throws IOException;
	void visitTenants(PathVisitor v);
	List<String> listTenants();
	void visitStorage(String origin, String namespace, PathVisitor v);
	List<StorageRef> listStorage(String origin, String namespace);
	void overwrite(String origin, String namespace, String id, byte[] cache) throws IOException;
	String store(String origin, String namespace, byte[] cache) throws IOException;
	void storeAt(String origin, String namespace, String id, byte[] cache) throws IOException;
	void storeAt(String origin, String namespace, String id, InputStream is) throws IOException;
	String store(String origin, String namespace, InputStream is) throws IOException;
	Zipped zipAt(String origin, String namespace, String id) throws IOException;
	void delete(String origin, String namespace, String id) throws IOException;
	void backup(String origin, String namespace, Zipped backup, Instant modifiedAfter) throws IOException;
	void restore(String origin, String namespace, Zipped backup) throws IOException;

	default String originTenant(String origin) {
		return formatOrigin(origin);
	}

	default String tenantOrigin(String tenant) {
		return origin(tenant);
	}

	default void sanitize(String ...paths) {
		for (var p : paths) {
			if (p.contains("/") || p.contains("\\")) throw new NotFoundException("Illegal characters");
		}
	}

	interface Zipped extends Closeable {
		Path get(String first, String... more);
		InputStream in(String filename);
		OutputStream out(String filename) throws IOException;
		Iterator<InputStream> list(String pattern) throws IOException;
	}

	interface PathVisitor {
		void visit(String filename);
	}

	record StorageRef(String id, long size) {}
}

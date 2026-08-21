package jasper.component;


import io.micrometer.core.annotation.Timed;
import jasper.config.Props;
import jasper.errors.AlreadyExistsException;
import jasper.errors.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Profile("storage")
@Component
public class StorageImplLocal implements Storage {
	private final Logger logger = LoggerFactory.getLogger(StorageImplLocal.class);

	@Autowired
	Props props;

	@Timed(value = "jasper.storage", histogram = true)
	public byte[] get(String origin, String namespace, String id) {
		try {
			return Files.readAllBytes(path(origin, namespace, id));
		} catch (IOException e) {
			throw new NotFoundException("Cache " + id);
		}
	}

	@Timed(value = "jasper.storage", histogram = true)
	public long size(String origin, String namespace, String id) {
		return path(origin, namespace, id).toFile().length();
	}

	@Timed(value = "jasper.storage", histogram = true)
	public InputStream stream(String origin, String namespace, String id) {
		try {
			return new FileInputStream(path(origin, namespace, id).toFile());
		} catch (IOException e) {
			throw new NotFoundException("Storage file (" + origin + ", " + namespace + ") " + id);
		}
	}

	@Timed(value = "jasper.storage", histogram = true)
	public long stream(String origin, String namespace, String id, OutputStream os) {
		try {
			return Files.copy(path(origin, namespace, id), os);
		} catch (IOException e) {
			throw new NotFoundException("Storage file (" + origin + ", " + namespace + ") " + id);
		}
	}

	@Override
	public Zipped streamZip(String origin, String namespace, String id) throws IOException {
		return new ZippedLocal(origin, namespace, id, false);
	}

	@Timed(value = "jasper.storage", histogram = true)
	public void visitTenants(PathVisitor v) {
		var dir = tenants();
		if (!dir.toFile().exists()) return;
		try (var list = Files.list(dir)) {
			list.forEach(t -> {
				if (!t.toFile().isDirectory()) return;
				var tenant = t.getFileName().toString();
				v.visit(tenantOrigin(tenant));
			});
		} catch (IOException e) {
			logger.warn("Error reading tenant dir", e);
		}
	}

	@Override
	public List<String> listTenants() {
		try (var list = Files.list(tenants())) {
			return list
				.map(f -> f.getFileName().toString())
				.collect(Collectors.toList());
		} catch (IOException e) {
			return Collections.emptyList();
		}
	}

	@Timed(value = "jasper.storage", histogram = true)
	public void visitStorage(String origin, String namespace, PathVisitor v) {
		var dir = dir(origin, namespace);
		if (!dir.toFile().exists()) return;
		try (var list = Files.list(dir)) {
			list.forEach(p -> v.visit(p.getFileName().toString()));
		} catch (IOException e) {
			logger.warn("Error reading storage", e);
		}
	}

	@Override
	public List<StorageRef> listStorage(String origin, String namespace) {
		try (var list = Files.list(dir(origin, namespace))) {
			return list
				.map(f -> new StorageRef(f.getFileName().toString(), f.toFile().length()))
				.collect(Collectors.toList());
		} catch (IOException e) {
			return Collections.emptyList();
		}
	}

	@Timed(value = "jasper.storage", histogram = true)
	public boolean exists(String origin, String namespace, String id) {
		return path(origin, namespace, id).toFile().exists();
	}

	@Timed(value = "jasper.storage", histogram = true)
	public void overwrite(String origin, String namespace, String id, byte[] file) throws IOException {
		if (!exists(origin, namespace, id)) throw new NotFoundException("Cache " + id);
		var path = path(origin, namespace, id);
		Files.write(path, file, StandardOpenOption.TRUNCATE_EXISTING);
	}

	@Timed(value = "jasper.storage", histogram = true)
	public String store(String origin, String namespace, byte[] file) throws IOException {
		var id = UUID.randomUUID().toString();
		storeAt(origin, namespace, id, file);
		return id;
	}

	@Timed(value = "jasper.storage", histogram = true)
	public String store(String origin, String namespace, InputStream is) throws IOException {
		var id = UUID.randomUUID().toString();
		var path = path(origin, namespace, id);
		Files.createDirectories(path.getParent());
		try (var fos = new FileOutputStream(path.toFile())) {
			StreamUtils.copy(is, fos);
		}
		return id;
	}

	@Override
	public Zipped zipAt(String origin, String namespace, String id) throws IOException {
		if (path(origin, namespace, id).toFile().exists()) throw new AlreadyExistsException();
		Files.createDirectories(dir(origin, namespace));
		return new ZippedLocal(origin, namespace, id, true);
	}

	@Timed(value = "jasper.storage", histogram = true)
	public void storeAt(String origin, String namespace, String id, byte[] file) throws IOException {
		var path = path(origin, namespace, id);
		if (path.toFile().exists()) throw new AlreadyExistsException();
		Files.createDirectories(path.getParent());
		Files.write(path, file, StandardOpenOption.CREATE_NEW);
	}

	@Timed(value = "jasper.storage", histogram = true)
	public void storeAt(String origin, String namespace, String id, InputStream is) throws IOException {
		var path = path(origin, namespace, id);
		if (path.toFile().exists()) throw new AlreadyExistsException();
		Files.createDirectories(path.getParent());
		try (var fos = new FileOutputStream(path.toFile())) {
			StreamUtils.copy(is, fos);
		}
	}

	@Timed(value = "jasper.storage", histogram = true)
	public void delete(String origin, String namespace, String id) throws IOException {
		Files.delete(path(origin, namespace, id));
	}

	@Override
	public void backup(String origin, String namespace, Zipped backup, Instant modifiedAfter) throws IOException {
		if (!dir(origin, namespace).toFile().exists()) return;
		Files.createDirectories(backup.get(namespace));
		try (var w = Files.walk(dir(origin, namespace))) {
			w.forEach(f -> {
				if (Files.isRegularFile(f) && (modifiedAfter == null || f.toFile().lastModified() > modifiedAfter.toEpochMilli())) {
					try {
						Files.copy(f, backup.get(namespace, f.getFileName().toString()));
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
			});
		}
	}

	@Override
	public void restore(String origin, String namespace, Zipped backup) throws IOException {
		if (!Files.exists(backup.get(namespace))) return;
		Files.createDirectories(dir(origin, namespace));
		try (var w = Files.walk(backup.get(namespace))) {
			w.forEach(f -> {
				if (Files.isRegularFile(f)) {
					try {
						Files.copy(f, path(origin, namespace, f.getFileName().toString()));
					} catch (FileAlreadyExistsException e) {
						// TODO: overwrite option?
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
			});
		}
	}

	Path tenants() {
		return Paths.get(props.getStorage());
	}

	Path dir(String origin, String namespace) {
		sanitize(origin, namespace);
		return Paths.get(props.getStorage(), originTenant(origin), namespace);
	}

	Path path(String origin, String namespace, String id) {
		sanitize(origin, namespace, id);
		return Paths.get(props.getStorage(), originTenant(origin), namespace, id);
	}

	private class ZippedLocal implements Zipped {
		private final FileSystem zipfs;
		private final boolean create;
		private final String origin;
		private final String namespace;
		private final String id;

		public ZippedLocal(String origin, String namespace, String id, boolean create) throws IOException {
			this.origin = origin;
			this.namespace = namespace;
			this.id = id;
			this.create = create;
			zipfs = FileSystems.newFileSystem(path(origin, namespace, create ? "_" + id : id), Map.of("create", create ? "true" : "false"));
		}

		@Override
		public Path get(String first, String... more) {
			return zipfs.getPath(first, more);
		}

		@Override
		public InputStream in(String filename) {
			logger.debug("Reading zip file {}", filename);
            try {
                return Files.newInputStream(zipfs.getPath(filename));
            } catch (IOException e) {
                return null;
            }
        }

		@Override
		public OutputStream out(String filename) throws IOException {
			logger.debug("Zipping up {}", filename);
			return Files.newOutputStream(zipfs.getPath(filename));
        }

		@Override
		public Iterator<InputStream> list(String pattern) throws IOException {
			logger.debug("Listing files matching pattern {}", pattern);
			var root = zipfs.getPath("/");
			if (!Files.exists(root)) return Collections.emptyIterator();
			try (var stream = Files.walk(root)) {
				var files = stream
					.filter(Files::isRegularFile)
					.map(p -> p.toString().startsWith("/") ? p.toString().substring(1) : p.toString())
					.filter(name -> name.matches(pattern))
					.collect(Collectors.toList());
				return files.stream()
					.map(filename -> {
						try {
							return Files.newInputStream(zipfs.getPath(filename));
						} catch (IOException e) {
							logger.warn("Failed to open file {} in zip", filename, e);
							return null;
						}
					})
					.filter(is -> is != null)
					.iterator();
			}
		}

		@Override
		public void close() throws IOException {
			zipfs.close();
			if (create) {
				// Remove underscore to indicate writing has finished
				Files.move(path(origin, namespace, "_" + id), path(origin, namespace, id));
			}
		}
	}
}

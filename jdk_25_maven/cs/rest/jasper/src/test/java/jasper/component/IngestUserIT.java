package jasper.component;

import jasper.IntegrationTest;
import jasper.domain.User;
import jasper.errors.AlreadyExistsException;
import jasper.errors.DuplicateModifiedDateException;
import jasper.errors.ModifiedException;
import jasper.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.util.ReflectionTestUtils.setField;

@IntegrationTest
@ExtendWith(MockitoExtension.class)
public class IngestUserIT {

	@InjectMocks
	@Autowired
	IngestUser ingest;

	@Autowired
	UserRepository userRepository;

	@BeforeEach
	void init() {
		userRepository.deleteAll();
	}

	@Test
	void testIngestExt() {
		var user = new User();
		user.setTag("+user/tester");

		ingest.create(user);

		assertThat(userRepository.existsByQualifiedTag("+user/tester"))
			.isTrue();
	}

	@Test
	void testCreateDuplicateExtFails() {
		var existing = new User();
		existing.setTag("+user/tester");
		userRepository.save(existing);
		var user = new User();
		user.setTag("+user/tester");

		assertThatThrownBy(() -> ingest.create(user))
			.isInstanceOf(AlreadyExistsException.class);

		assertThat(userRepository.existsByQualifiedTag("+user/tester"))
			.isTrue();
	}

	@Test
	void testDoubleIngestExtFails() {
		var ext1 = new User();
		ext1.setTag("+user/tester");
		ext1.setName("First");
		var ext2 = new User();
		ext2.setTag("+user/tester");
		ext2.setName("Second");

		ingest.ensureCreateUniqueModified(ext1);
		assertThatThrownBy(() -> ingest.ensureCreateUniqueModified(ext2))
			.isInstanceOf(AlreadyExistsException.class);

		assertThat(userRepository.existsByQualifiedTag("+user/tester"))
			.isTrue();
	}

	@Test
	void testUpdateExt() {
		var existing = new User();
		existing.setTag("+user/tester");
		existing.setName("First");
		userRepository.save(existing);
		var user = new User();
		user.setTag("+user/tester");
		user.setName("Second");
		user.setModified(existing.getModified());

		ingest.update(user);

		assertThat(userRepository.existsByQualifiedTag("+user/tester"))
			.isTrue();
		var fetched = userRepository.findOneByQualifiedTag("+user/tester").get();
		assertThat(fetched.getName())
			.isEqualTo("Second");
	}

	@Test
	void testDuplicateCreateModifiedFails() {
		var fixedClock = Clock.fixed(Instant.ofEpochSecond(1640000000), ZoneOffset.UTC);
		setField(ingest, "ensureUniqueModifiedClock", fixedClock);
		try {
			var ext1 = new User();
			ext1.setTag("+user/tester");
			ext1.setName("First");
			ingest.create(ext1);
			var ext2 = new User();
			ext2.setTag("+user/other");
			ext2.setName("Second");

			assertThatThrownBy(() -> ingest.create(ext2))
				.isInstanceOf(DuplicateModifiedDateException.class);

			assertThat(userRepository.existsByQualifiedTag("+user/tester"))
				.isTrue();
			var fetched1 = userRepository.findOneByQualifiedTag("+user/tester").get();
			assertThat(fetched1.getName())
				.isEqualTo("First");
			assertThat(fetched1.getModified())
				.isEqualTo(Instant.now(fixedClock));
			assertThat(userRepository.existsByQualifiedTag("+user/other"))
				.isFalse();
		} finally {
			setField(ingest, "ensureUniqueModifiedClock", Clock.systemUTC());
		}
	}

	@Test
	void testDuplicateUpdateModifiedFails() {
		var fixedClock = Clock.fixed(Instant.ofEpochSecond(1640000000), ZoneOffset.UTC);
		try {
			var ext1 = new User();
			ext1.setTag("+user/tester");
			ext1.setName("First");
			ingest.create(ext1);
			setField(ingest, "ensureUniqueModifiedClock", fixedClock);
			var ext2 = new User();
			ext2.setTag("+user/other");
			ext2.setName("Second");
			ingest.create(ext2);
			var update = new User();
			update.setTag("+user/tester");
			update.setName("Modified");
			update.setModified(ext1.getModified());

			assertThatThrownBy(() -> ingest.update(update))
				.isInstanceOf(DuplicateModifiedDateException.class);

			assertThat(userRepository.existsByQualifiedTag("+user/tester"))
				.isTrue();
			var fetched1 = userRepository.findOneByQualifiedTag("+user/tester").get();
			assertThat(fetched1.getName())
				.isEqualTo("First");
			var fetched2 = userRepository.findOneByQualifiedTag("+user/other").get();
			assertThat(fetched2.getName())
				.isEqualTo("Second");
			assertThat(fetched2.getModified())
				.isEqualTo(Instant.now(fixedClock));
		} finally {
			setField(ingest, "ensureUniqueModifiedClock", Clock.systemUTC());
		}
	}

	@Test
	void testModifiedOptimisticLock() throws InterruptedException {
		var user = new User();
		user.setTag("+user/tester");
		user.setName("First");
		ingest.create(user);
		var update1 = new User();
		update1.setTag("+user/tester");
		update1.setName("M1");
		update1.setModified(user.getModified());
		var update2 = new User();
		update2.setTag("+user/tester");
		update2.setName("M2");
		update2.setModified(user.getModified());

		ingest.update(update1);
		Thread.sleep(1);
		assertThatThrownBy(() -> ingest.update(update2))
			.isInstanceOf(ModifiedException.class);
	}

	@Test
	void testConcurrentModifiedOptimisticLock() throws InterruptedException {
		var user = new User();
		user.setTag("+user/tester");
		user.setName("First");
		ingest.create(user);
		var update1 = new User();
		update1.setTag("+user/tester");
		update1.setName("M1");
		update1.setModified(user.getModified());
		var update2 = new User();
		update2.setTag("+user/tester");
		update2.setName("M2");
		update2.setModified(user.getModified());

		AtomicBoolean firstException = new AtomicBoolean(false);
		AtomicBoolean secondException = new AtomicBoolean(false);

		var thread1 = new Thread(() -> {
			try {
				ingest.update(update1);
			} catch (ModifiedException t) {
				firstException.set(true);
			}
		});
		var thread2 = new Thread(() -> {
			try {
				ingest.update(update2);
			} catch (ModifiedException t) {
				secondException.set(true);
			}
		});
		thread1.start();
		thread2.start();
		thread1.join();
		thread2.join();

		assertThat(firstException.get())
			.isNotEqualTo(secondException.get());
	}

}

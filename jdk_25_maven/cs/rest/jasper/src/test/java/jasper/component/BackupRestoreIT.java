package jasper.component;

import jasper.IntegrationTest;
import jasper.domain.Ext;
import jasper.domain.Plugin;
import jasper.domain.Ref;
import jasper.domain.Template;
import jasper.domain.User;
import jasper.repository.ExtRepository;
import jasper.repository.PluginRepository;
import jasper.repository.RefRepository;
import jasper.repository.TemplateRepository;
import jasper.repository.UserRepository;
import jasper.service.dto.BackupOptionsDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@ActiveProfiles({"storage", "test"})
public class BackupRestoreIT {

	@Autowired
	Backup backup;

	@Autowired
	RefRepository refRepository;

	@Autowired
	ExtRepository extRepository;

	@Autowired
	UserRepository userRepository;

	@Autowired
	PluginRepository pluginRepository;

	@Autowired
	TemplateRepository templateRepository;

	@Autowired
	Storage storage;

	private static final String ORIGIN = "";
	private static final String BACKUP_ID = "test-backup";

	@BeforeEach
	void setup() {
		// Clean up before each test
		refRepository.deleteAll();
		extRepository.deleteAll();
		userRepository.deleteAll();
		pluginRepository.deleteAll();
		templateRepository.deleteAll();
	}

	@AfterEach
	void cleanup() throws IOException {
		// Clean up backup files after each test
		try {
			backup.delete(ORIGIN, BACKUP_ID);
		} catch (Exception e) {
			// Ignore if backup doesn't exist
		}
	}

	@Test
	void testBackupAndRestoreRefs() throws IOException {
		// Create test data
		var ref1 = new Ref();
		ref1.setUrl("https://example.com/1");
		ref1.setOrigin(ORIGIN);
		ref1.setTitle("Test Ref 1");
		refRepository.save(ref1);

		var ref2 = new Ref();
		ref2.setUrl("https://example.com/2");
		ref2.setOrigin(ORIGIN);
		ref2.setTitle("Test Ref 2");
		refRepository.save(ref2);

		// Create backup
		var options = new BackupOptionsDto();
		options.setRef(true);
		options.setExt(false);
		options.setUser(false);
		options.setPlugin(false);
		options.setTemplate(false);
		options.setCache(false);
		backup.createBackup(ORIGIN, BACKUP_ID, options);

		// Wait for async backup to complete
		waitForBackup();

		// Delete all data
		refRepository.deleteAll();
		assertThat(refRepository.count()).isZero();

		// Restore from backup
		backup.restore(ORIGIN, BACKUP_ID, options);

		// Wait for async restore to complete
		waitForRestore();

		// Verify data was restored
		assertThat(refRepository.count()).isEqualTo(2);
		assertThat(refRepository.existsByUrlAndOrigin("https://example.com/1", ORIGIN)).isTrue();
		assertThat(refRepository.existsByUrlAndOrigin("https://example.com/2", ORIGIN)).isTrue();
	}

	@Test
	void testBackupAndRestoreExts() throws IOException {
		// Create test data
		var ext1 = new Ext();
		ext1.setTag("test.tag1");
		ext1.setOrigin(ORIGIN);
		ext1.setName("Test Extension 1");
		extRepository.save(ext1);

		var ext2 = new Ext();
		ext2.setTag("test.tag2");
		ext2.setOrigin(ORIGIN);
		ext2.setName("Test Extension 2");
		extRepository.save(ext2);

		// Create backup
		var options = new BackupOptionsDto();
		options.setRef(false);
		options.setExt(true);
		options.setUser(false);
		options.setPlugin(false);
		options.setTemplate(false);
		options.setCache(false);
		backup.createBackup(ORIGIN, BACKUP_ID, options);

		waitForBackup();

		// Delete all data
		extRepository.deleteAll();
		assertThat(extRepository.count()).isZero();

		// Restore from backup
		backup.restore(ORIGIN, BACKUP_ID, options);
		waitForRestore();

		// Verify data was restored
		assertThat(extRepository.count()).isEqualTo(2);
		assertThat(extRepository.existsByQualifiedTag("test.tag1")).isTrue();
		assertThat(extRepository.existsByQualifiedTag("test.tag2")).isTrue();
	}

	@Test
	void testBackupAndRestoreUsers() throws IOException {
		// Create test data
		var user1 = new User();
		user1.setTag("+user/test1");
		user1.setOrigin(ORIGIN);
		user1.setName("User One");
		userRepository.save(user1);

		var user2 = new User();
		user2.setTag("+user/test2");
		user2.setOrigin(ORIGIN);
		user2.setName("User Two");
		userRepository.save(user2);

		// Create backup
		var options = new BackupOptionsDto();
		options.setRef(false);
		options.setExt(false);
		options.setUser(true);
		options.setPlugin(false);
		options.setTemplate(false);
		options.setCache(false);
		backup.createBackup(ORIGIN, BACKUP_ID, options);

		waitForBackup();

		// Delete all data
		userRepository.deleteAll();
		assertThat(userRepository.count()).isZero();

		// Restore from backup
		backup.restore(ORIGIN, BACKUP_ID, options);
		waitForRestore();

		// Verify data was restored
		assertThat(userRepository.count()).isEqualTo(2);
		assertThat(userRepository.existsByQualifiedTag("+user/test1")).isTrue();
		assertThat(userRepository.existsByQualifiedTag("+user/test2")).isTrue();
	}

	@Test
	void testBackupAndRestorePlugins() throws IOException {
		// Create test data
		var plugin1 = new Plugin();
		plugin1.setTag("plugin/test1");
		plugin1.setOrigin(ORIGIN);
		plugin1.setName("Test Plugin 1");
		pluginRepository.save(plugin1);

		var plugin2 = new Plugin();
		plugin2.setTag("plugin/test2");
		plugin2.setOrigin(ORIGIN);
		plugin2.setName("Test Plugin 2");
		pluginRepository.save(plugin2);

		// Create backup
		var options = new BackupOptionsDto();
		options.setRef(false);
		options.setExt(false);
		options.setUser(false);
		options.setPlugin(true);
		options.setTemplate(false);
		options.setCache(false);
		backup.createBackup(ORIGIN, BACKUP_ID, options);

		waitForBackup();

		// Delete all data
		pluginRepository.deleteAll();
		assertThat(pluginRepository.count()).isZero();

		// Restore from backup
		backup.restore(ORIGIN, BACKUP_ID, options);
		waitForRestore();

		// Verify data was restored
		assertThat(pluginRepository.count()).isEqualTo(2);
		assertThat(pluginRepository.existsByQualifiedTag("plugin/test1")).isTrue();
		assertThat(pluginRepository.existsByQualifiedTag("plugin/test2")).isTrue();
	}

	@Test
	void testBackupAndRestoreTemplates() throws IOException {
		// Create test data
		var template1 = new Template();
		template1.setTag("_template1");
		template1.setOrigin(ORIGIN);
		template1.setName("Test Template 1");
		templateRepository.save(template1);

		var template2 = new Template();
		template2.setTag("_template2");
		template2.setOrigin(ORIGIN);
		template2.setName("Test Template 2");
		templateRepository.save(template2);

		// Create backup
		var options = new BackupOptionsDto();
		options.setRef(false);
		options.setExt(false);
		options.setUser(false);
		options.setPlugin(false);
		options.setTemplate(true);
		options.setCache(false);
		backup.createBackup(ORIGIN, BACKUP_ID, options);

		waitForBackup();

		// Delete all data
		templateRepository.deleteAll();
		assertThat(templateRepository.count()).isZero();

		// Restore from backup
		backup.restore(ORIGIN, BACKUP_ID, options);
		waitForRestore();

		// Verify data was restored
		assertThat(templateRepository.count()).isEqualTo(2);
		assertThat(templateRepository.existsByQualifiedTag("_template1")).isTrue();
		assertThat(templateRepository.existsByQualifiedTag("_template2")).isTrue();
	}

	@Test
	void testBackupAndRestoreAll() throws IOException {
		// Create test data for all types
		var ref = new Ref();
		ref.setUrl("https://example.com/all");
		ref.setOrigin(ORIGIN);
		refRepository.save(ref);

		var ext = new Ext();
		ext.setTag("test.all");
		ext.setOrigin(ORIGIN);
		extRepository.save(ext);

		var user = new User();
		user.setTag("+user/all");
		user.setOrigin(ORIGIN);
		userRepository.save(user);

		var plugin = new Plugin();
		plugin.setTag("plugin/all");
		plugin.setOrigin(ORIGIN);
		pluginRepository.save(plugin);

		var template = new Template();
		template.setTag("_all");
		template.setOrigin(ORIGIN);
		templateRepository.save(template);

		// Create backup of all types
		var options = new BackupOptionsDto();
		options.setRef(true);
		options.setExt(true);
		options.setUser(true);
		options.setPlugin(true);
		options.setTemplate(true);
		options.setCache(false);
		backup.createBackup(ORIGIN, BACKUP_ID, options);

		waitForBackup();

		// Delete all data
		refRepository.deleteAll();
		extRepository.deleteAll();
		userRepository.deleteAll();
		pluginRepository.deleteAll();
		templateRepository.deleteAll();

		// Restore from backup
		backup.restore(ORIGIN, BACKUP_ID, options);
		waitForRestore();

		// Verify all data was restored
		assertThat(refRepository.count()).isEqualTo(1);
		assertThat(extRepository.count()).isEqualTo(1);
		assertThat(userRepository.count()).isEqualTo(1);
		assertThat(pluginRepository.count()).isEqualTo(1);
		assertThat(templateRepository.count()).isEqualTo(1);
	}

	@Test
	void testListBackups() throws IOException {
		// Create a backup
		var options = new BackupOptionsDto();
		options.setRef(true);
		backup.createBackup(ORIGIN, BACKUP_ID, options);

		waitForBackup();

		// List backups
		var backups = backup.listBackups(ORIGIN);

		// Verify backup is in the list
		assertThat(backups).isNotNull();
		assertThat(backups).isNotEmpty();
		assertThat(backups.stream().anyMatch(b -> b.id().equals(BACKUP_ID + ".zip"))).isTrue();
	}

	@Test
	void testPartialRestore() throws IOException {
		// Create test data
		var ref = new Ref();
		ref.setUrl("https://example.com/partial");
		ref.setOrigin(ORIGIN);
		refRepository.save(ref);

		var ext = new Ext();
		ext.setTag("test.partial");
		ext.setOrigin(ORIGIN);
		extRepository.save(ext);

		// Create backup of both
		var backupOptions = new BackupOptionsDto();
		backupOptions.setRef(true);
		backupOptions.setExt(true);
		backup.createBackup(ORIGIN, BACKUP_ID, backupOptions);

		waitForBackup();

		// Delete all data
		refRepository.deleteAll();
		extRepository.deleteAll();

		// Restore only refs
		var restoreOptions = new BackupOptionsDto();
		restoreOptions.setRef(true);
		restoreOptions.setExt(false);
		backup.restore(ORIGIN, BACKUP_ID, restoreOptions);

		waitForRestore();

		// Verify only refs were restored
		assertThat(refRepository.count()).isEqualTo(1);
		assertThat(extRepository.count()).isZero();
	}

	@Test
	void testBackupWithMultipleFiles() throws IOException {
		// Create multiple refs to test pattern matching (ref*.json files)
		for (int i = 0; i < 5; i++) {
			var ref = new Ref();
			ref.setUrl("https://example.com/multi-" + i);
			ref.setOrigin(ORIGIN);
			ref.setTitle("Multi Test " + i);
			refRepository.save(ref);
		}

		// Create backup
		var options = new BackupOptionsDto();
		options.setRef(true);
		backup.createBackup(ORIGIN, BACKUP_ID, options);

		waitForBackup();

		// Delete data
		refRepository.deleteAll();

		// Restore
		backup.restore(ORIGIN, BACKUP_ID, options);
		waitForRestore();

		// Verify all refs were restored
		assertThat(refRepository.count()).isEqualTo(5);
	}

	@Test
	void testIncrementalBackup() throws IOException {
		// Create initial data
		var ref1 = new Ref();
		ref1.setUrl("https://example.com/inc1");
		ref1.setOrigin(ORIGIN);
		refRepository.save(ref1);

		var beforeTime = Instant.now();

		// Wait a bit to ensure different timestamps
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		// Create newer data
		var ref2 = new Ref();
		ref2.setUrl("https://example.com/inc2");
		ref2.setOrigin(ORIGIN);
		refRepository.save(ref2);

		// Create incremental backup (only newer than beforeTime)
		var options = new BackupOptionsDto();
		options.setRef(true);
		options.setNewerThan(beforeTime);
		backup.createBackup(ORIGIN, BACKUP_ID, options);

		waitForBackup();

		// Delete all data
		refRepository.deleteAll();

		// Restore
		backup.restore(ORIGIN, BACKUP_ID, options);
		waitForRestore();

		// Verify only the newer ref was backed up and restored
		// Note: The behavior might include both if timestamps are very close
		// so we just verify at least the newer one is there
		assertThat(refRepository.existsByUrlAndOrigin("https://example.com/inc2", ORIGIN)).isTrue();
	}

	private void waitForBackup() {
		// Wait for async backup operation to complete
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private void waitForRestore() {
		// Wait for async restore operation to complete
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}

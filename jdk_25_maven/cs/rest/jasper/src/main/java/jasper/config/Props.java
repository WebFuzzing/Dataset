package jasper.config;

import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

import static jasper.domain.proj.HasOrigin.subOrigin;
import static java.lang.Integer.parseInt;
import static org.apache.commons.lang3.ArrayUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Properties specific to Jasper.
 * <p>
 * Properties are configured in the {@code application.yml} file.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "jasper", ignoreUnknownFields = false)
public class Props {
	/**
	 * Enable debug mode for additional logging.
	 */
	private boolean debug = false;
	/**
	 * List of workers to create by origin. Each worker will use the
	 * _config/server/sub-origin file in the local origin to perform certain tasks.
	 * The sub origin for this worker is at the index in the worker name.
	 */
	private String[] workload;
	/**
	 * The name of this worker. The number suffix is used as an index
	 * into workload to determine the worker sub origin.
	 */
	private String worker;
	/**
	 * Worker sub-origin.
	 */
	public String getWorkerOrigin() {
		if (isNotEmpty(workload) && isNotBlank(worker)) {
			var index = worker.replaceAll("\\D", "");
			return workload[isBlank(index) ? 0 : parseInt(index)];
		} else {
			return "";
		}
	}
	/**
	 * Local origin for this server.
	 */
	private String localOrigin = "";
	/**
	 * Computed origin (local + worker).
	 */
	public String getOrigin() {
		return subOrigin(getLocalOrigin(), getWorkerOrigin());
	}
	/**
	 * Allow pre-authentication of a user via the User-Tag header.
	 */
	private boolean allowUserTagHeader = false;
	/**
	 * Allow escalating user role via User-Role header.
	 */
	private boolean allowUserRoleHeader = false;
	/**
	 * Allow adding additional user permissions via Read-Access, Write-Access, Tag-Read-Access, and Tag-Write-Access headers.
	 */
	private boolean allowAuthHeaders = false;
	/**
	 * Highest role allowed access.
	 */
	private String maxRole = "ROLE_ADMIN";
	/**
	 * Minimum role for basic access.
	 */
	private String minRole = "ROLE_ANONYMOUS";
	/**
	 * Minimum role for writing.
	 */
	private String minWriteRole = "ROLE_VIEWER";
	/**
	 * Minimum role for fetching external resources.
	 */
	private String minFetchRole = "ROLE_USER";
	/**
	 * Minimum role for admin config.
	 */
	private String minConfigRole = "ROLE_ADMIN";
	/**
	 * Minimum role for downloading backups.
	 * Backups may contain private data or private SSH keys, so they are extremely sensitive.
	 */
	private String minReadBackupsRole = "ROLE_ADMIN";
	/**
	 * Default role given to every user.
	 */
	private String defaultRole = "ROLE_ANONYMOUS";
	/**
	 * Additional read access qualified tags to apply to all users.
	 */
	private String[] defaultReadAccess;
	/**
	 * Additional write access qualified tags to apply to all users.
	 */
	private String[] defaultWriteAccess;
	/**
	 * Additional tag read access qualified tags to apply to all users.
	 */
	private String[] defaultTagReadAccess;
	/**
	 * Additional tag write access qualified tags to apply to all users.
	 */
	private String[] defaultTagWriteAccess;

	/**
	 * Maximum number of retry attempts for getting a unique modified date when ingesting a Ref.
	 */
	private int ingestMaxRetry = 5;
	/**
	 * Size of buffer in bytes used to cache JSON in RAM before flushing to disk during backup.
	 */
	private int backupBufferSize = 1000000;
	/**
	 * Number of entities to restore in each transaction.
	 */
	private int restoreBatchSize = 500;
	/**
	 * Number of entities to generate Metadata for in each transaction when backfilling.
	 */
	private int backfillBatchSize = 100;
	/**
	 * Number of seconds the server must be idle (no REST API requests) before backfill runs.
	 * Set to 0 to disable idle detection and always run backfill.
	 */
	private int backfillIdleSec = 0;
	/**
	 * Number of seconds to throttle clearing the config cache.
	 */
	private int clearCacheCooldownSec = 2;
	/**
	 * Number of seconds to throttle pushing after modification.
	 */
	private int pushCooldownSec = 1;

	/**
	 * Path to the folder to use for storage. Used by the backup system.
	 */
	private String storage = "/var/lib/jasper";
	/**
	 * Path to node binary for running javascript deltas.
	 */
	private String node = "/usr/local/bin/node";
	/**
	 * Path to python binary for running python scripts.
	 */
	private String python = "/usr/bin/python";
	/**
	 * Path to shell binary for running shell scripts.
	 */
	private String shell = "/usr/bin/bash";
	/**
	 * HTTP address of an instance where storage is enabled.
	 */
	private String cacheApi = "";

	/**
	 * K8s namespace to write authorized_keys config map file to.
	 */
	private String sshConfigNamespace = "default";
	/**
	 * K8s config map name to write authorized_keys file to.
	 */
	private String sshConfigMapName = "ssh-authorized-keys";
	/**
	 * K8s secret name to write the host_key file to.
	 */
	private String sshSecretName = "ssh-host-key";

	private final Overrides override = new Overrides();
	private final Http http = new Http();
	private final Mail mail = new Mail();
	private final Security security = new Security();
	private final ApiDocs apiDocs = new ApiDocs();
	private final CorsConfiguration cors = new CorsConfiguration();
	private final AuditEvents auditEvents = new AuditEvents();

	@Getter
	@Setter
	public static class Overrides {
		/**
		 * Override any server settings for all origins.
		 */
		private final ServerOverrides server = new ServerOverrides();
		/**
		 * Override any security settings for all origins.
		 */
		private final SecurityOverrides security = new SecurityOverrides();
	}

	@Getter
	@Setter
	public static class ServerOverrides {
		/**
		 * Override the server email host.
		 */
		private String emailHost;
		/**
		 * Override the server max sources.
		 */
		private Integer maxSources;
		/**
		 * Override the server mod seals.
		 */
		private List<String> modSeals;
		/**
		 * Override the server editor seals.
		 */
		private List<String> editorSeals;
		/**
		 * Override the server origins with web access.
		 */
		private List<String> webOrigins;
		/**
		 * Override the server maximum batch size for replicate controller.
		 */
		private Integer maxReplEntityBatch;
		/**
		 * Override the server origins with SSH access.
		 */
		private List<String> sshOrigins;
		/**
		 * Override the server maximum batch size for push replicate.
		 */
		private Integer maxPushEntityBatch;
		/**
		 * Override the server maximum batch size for pull replicate.
		 */
		private Integer maxPullEntityBatch;
		/**
		 * Override the server tags and origins that can run scripts. No wildcard origins.
		 */
		private List<String> scriptSelectors;
		/**
		 * Override the server list of whitelisted script SHA-256 hashes.
		 */
		private List<String> scriptWhitelist;
		/**
		 * Override the server list of whitelisted hosts.
		 */
		private List<String> hostWhitelist;
		/**
		 * Override the server list of blacklisted hosts.
		 */
		private List<String> hostBlacklist;
		/**
		 * Override the server maximum HTTP requests per origin every 500 nanoseconds.
		 */
		private Integer maxRequests;
		/**
		 * Override the server global maximum concurrent HTTP requests (across all origins).
		 */
		private Integer maxConcurrentRequests;
		/**
		 * Override the server maximum concurrent script executions.
		 */
		private Integer maxConcurrentScripts;
		/**
		 * Override the server maximum concurrent replication push/pull operations.
		 */
		private Integer maxConcurrentReplication;
		/**
		 * Override the server maximum concurrent fetch operations (scraping).
		 */
		private Integer maxConcurrentFetch;
	}

	@Getter
	@Setter
	public static class SecurityOverrides {
		/**
		 * Override the security mode for all origins.
		 */
		private String mode = "";
		/**
		 * Override the security clientId for all origins.
		 */
		private String clientId = "";
		/**
		 * Override the security base64Secret for all origins.
		 */
		private String base64Secret = "";
		/**
		 * Override the security secret for all origins.
		 */
		private String secret = "";
		/**
		 * Override the security jwksUri for all origins.
		 */
		private String jwksUri = "";
		/**
		 * Override the security usernameClaim for all origins.
		 */
		private String usernameClaim = "";
		/**
		 * Override the security verifiedEmailClaim for all origins.
		 */
		private String verifiedEmailClaim = "unset";
		/**
		 * Override the security defaultUser for all origins.
		 */
		private String defaultUser = "";
		/**
		 * Override the security tokenEndpoint for all origins.
		 */
		private String tokenEndpoint = "";
		/**
		 * Override the security scimEndpoint for all origins.
		 */
		private String scimEndpoint = "";
		/**
		 * Override the per-origin maximum HTTP requests every 500 nanoseconds. This limit applies to all origins.
		 */
		private Integer maxRequests;
		/**
		 * Override the per-origin maximum concurrent script executions. This limit applies to all origins.
		 */
		private Integer maxConcurrentScripts;
	}


	@Getter
	@Setter
	public static class Http {
		private final Cache cache = new Cache();

		@Getter
		@Setter
		public static class Cache {
			/**
			 * Time to live for HTTP cache in days.
			 */
			private int timeToLiveInDays = 1461; // 4 years (including leap day)
		}
	}
	@Getter
	@Setter
	public static class Mail {
		/**
		 * Enable mail service.
		 */
		private boolean enabled = false;
		/**
		 * From address for outgoing emails.
		 */
		private String from = "";
		/**
		 * Base URL for email links.
		 */
		private String baseUrl = "";
	}

	@Getter
	@Setter
	public static class Security {
		/**
		 * Content Security Policy header value.
		 */
		private String contentSecurityPolicy = "default-src 'self'; frame-src 'self' data:; script-src 'self' 'unsafe-inline' 'unsafe-eval'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self' data:";
	}

	@Getter
	@Setter
	public static class ApiDocs {
		/**
		 * API documentation title.
		 */
		private String title = "";
		/**
		 * API documentation description.
		 */
		private String description = "";
		/**
		 * API version.
		 */
		private String version = "";
		/**
		 * Terms of service URL.
		 */
		private String termsOfServiceUrl = "";
		/**
		 * Contact name for API support.
		 */
		private String contactName = "";
		/**
		 * Contact URL for API support.
		 */
		private String contactUrl = "";
		/**
		 * Contact email for API support.
		 */
		private String contactEmail = "";
		/**
		 * API license information.
		 */
		private License license;
		/**
		 * Default include pattern for API docs.
		 */
		private String defaultIncludePattern = "";
		/**
		 * Management include pattern for API docs.
		 */
		private String managementIncludePattern = "";
		/**
		 * List of API servers.
		 */
		private List<Server> servers;
	}

	@Getter
	@Setter
	public static class AuditEvents {
		/**
		 * Audit event retention period in days.
		 */
		private int retentionPeriod = 30;
	}
}

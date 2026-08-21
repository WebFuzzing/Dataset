package jasper.service;

import jasper.MultiTenantIntegrationTest;
import jasper.component.ConfigCache;
import jasper.component.Ingest;
import jasper.config.Props;
import jasper.domain.Plugin;
import jasper.domain.Ref;
import jasper.domain.User;
import jasper.errors.AlreadyExistsException;
import jasper.errors.ModifiedException;
import jasper.repository.PluginRepository;
import jasper.repository.RefRepository;
import jasper.repository.UserRepository;
import jasper.repository.filter.RefFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@WithMockUser("+user/tester@other")
@MultiTenantIntegrationTest
public class RefServiceMTIT {

	@Autowired
	Props props;

	@Autowired
	Ingest ingest;

	@Autowired
	RefService refService;

	@Autowired
	RefRepository refRepository;

	@Autowired
	PluginRepository pluginRepository;

	@Autowired
	UserRepository userRepository;

	@Autowired
	ConfigCache configCache;

	static final String URL = "https://www.example.com/";

	Ref getRef() {
		return getRef("@other");
	}

	Ref getRef(String origin) {
		var ref = new Ref();
		ref.setUrl(URL);
		ref.setOrigin(origin);
		return ref;
	}

	User getUser() {
		return getUser("@other");
	}

	User getUser(String origin) {
		var user = new User();
		user.setOrigin(origin);
		return user;
	}

	Plugin getPlugin() {
		var plugin = new Plugin();
		plugin.setOrigin("@other");
		return plugin;
	}

	@BeforeEach
	void init() {
		refRepository.deleteAll();
		pluginRepository.deleteAll();
		userRepository.deleteAll();
		configCache.clearUserCache();
		configCache.clearPluginCache();
		configCache.clearTemplateCache();
	}

	@BeforeEach
	void clearDefaultPermissions() {
		props.setDefaultReadAccess(null);
		props.setDefaultWriteAccess(null);
		props.setDefaultTagReadAccess(null);
		props.setDefaultTagWriteAccess(null);
	}

	@Test
	void testCreateUntaggedRef() {
		var ref = getRef();

		refService.create(ref);

		assertThat(refRepository.existsByUrlAndOrigin(URL, "@other"))
			.isTrue();
	}

	@Test
	void testCreateUntaggedRemoteRefFailed() {
		var ref = getRef();
		ref.setUrl(URL);
		ref.setOrigin("@remote");

		assertThatThrownBy(() -> refService.create(ref))
			.isInstanceOf(AccessDeniedException.class);
	}

	@Test
	void testCreateDuplicateRefFails() {
		var existing = getRef();
		existing.setUrl(URL);
		existing.setTags(new ArrayList<>(List.of("+user/tester")));
		refRepository.save(existing);
		var ref = getRef();
		ref.setUrl(URL);

		assertThatThrownBy(() -> refService.create(ref))
			.isInstanceOf(AlreadyExistsException.class);

		assertThat(refRepository.existsByUrlAndOrigin(URL, "@other"))
			.isTrue();
	}

	@Test
	void testCreateDuplicateAltRefPasses() {
		var existing = getRef();
		existing.setUrl("https://www.different.com/");
		existing.setTags(new ArrayList<>(List.of("+user/tester")));
		existing.setAlternateUrls(List.of(URL));
		refRepository.save(existing);
		var ref = getRef();
		ref.setUrl(URL);

		refService.create(ref);

		assertThat(refRepository.existsByUrlAndOrigin("https://www.different.com/", "@other"))
			.isTrue();
		assertThat(refRepository.existsByUrlAndOrigin(URL, "@other"))
			.isTrue();
	}

	@Test
	void testCreateRefWithPublicTag() {
		var ref = getRef();
		ref.setTags(new ArrayList<>(List.of("public")));

		refService.create(ref);

		assertThat(refRepository.existsByUrlAndOrigin(URL, "@other"))
			.isTrue();
	}

	@Test
	void testCreateRefWithPublicTagRemoteFails() {
		var ref = getRef();
		ref.setUrl(URL);
		ref.setOrigin("@remote");
		ref.setTags(new ArrayList<>(List.of("public")));

		assertThatThrownBy(() -> refService.create(ref))
			.isInstanceOf(AccessDeniedException.class);
	}

	@Test
	void testCreateRefWithReadableTags() {
		var ref = getRef();
		ref.setUrl(URL);
		ref.setTags(new ArrayList<>(List.of("public", "custom", "tags")));

		refService.create(ref);

		assertThat(refRepository.existsByUrlAndOrigin(URL, "@other"))
			.isTrue();
	}

	@Test
	void testCreateRefWithUnreadableTagsFails() {
		var ref = getRef();
		ref.setUrl(URL);
		ref.setTags(new ArrayList<>(List.of("_secret")));

		assertThatThrownBy(() -> refService.create(ref))
			.isInstanceOf(AccessDeniedException.class);

		assertThat(refRepository.existsByUrlAndOrigin(URL, "@other"))
			.isFalse();
	}

	@Test
	void testCreateRefWithPrivateTags() {
		var user = getUser();
		user.setTag("+user/tester");
		user.setReadAccess(List.of("_secret"));
		userRepository.save(user);
		var ref = getRef();
		ref.setUrl(URL);
		ref.setTags(new ArrayList<>(List.of("_secret")));

		refService.create(ref);

		assertThat(refRepository.existsByUrlAndOrigin(URL, "@other"))
			.isTrue();
		assertThat(refRepository.findOneByUrlAndOrigin(URL, "@other").get().getTags())
			.containsExactly("_secret");
	}

	@Test
	void testCreateRefWithUserTags() {
		var ref = getRef();
		ref.setUrl(URL);
		ref.setTags(new ArrayList<>(List.of("+user/tester")));

		refService.create(ref);

		assertThat(refRepository.existsByUrlAndOrigin(URL, "@other"))
			.isTrue();
		assertThat(refRepository.findOneByUrlAndOrigin(URL, "@other").get().getTags())
			.containsExactly("+user/tester");
		assertThat(refRepository.findOneByUrlAndOrigin(URL, "@other").get().getExpandedTags())
			.containsExactly("+user/tester", "+user");
	}

	@Test
	@WithMockUser("_user/tester@other")
	void testCreateRefWithPrivateUserTags() {
		var ref = getRef();
		ref.setUrl(URL);
		ref.setTags(new ArrayList<>(List.of("_user/tester")));

		refService.create(ref);

		assertThat(refRepository.existsByUrlAndOrigin(URL, "@other"))
			.isTrue();
		assertThat(refRepository.findOneByUrlAndOrigin(URL, "@other").get().getTags())
			.containsExactly("_user/tester");
		assertThat(refRepository.findOneByUrlAndOrigin(URL, "@other").get().getExpandedTags())
			.containsExactly("_user/tester", "_user");
	}

	@Test
	@WithMockUser("_user/tester")
	void testCreateRefWithPrivateUserTagsFailed() {
		var ref = getRef();
		ref.setUrl(URL);
		ref.setTags(new ArrayList<>(List.of("_user/other")));

		assertThatThrownBy(() -> refService.create(ref))
			.isInstanceOf(AccessDeniedException.class);

		assertThat(refRepository.existsByUrlAndOrigin(URL, "@other"))
			.isFalse();
	}

	@Test
	void testUpdateRefSetsMetadataObsolete() {
		var ref = getRef("@other");
		ref.setTitle("Ref");
		refRepository.save(ref);
		var other = getRef("@other.nested");
		other.setTitle("Other");
		ingest.push("@other", other, true, false);

		assertThat(refRepository.existsByUrlAndOrigin(URL, "@other"))
			.isTrue();
		assertThat(refRepository.existsByUrlAndOrigin(URL, "@other.nested"))
			.isTrue();
		var fetched = refRepository.findAll(RefFilter.builder().url(URL).obsolete(false).build().spec());
		assertThat(fetched.size())
			.isEqualTo(1);
	}

	@Test
	void testUpdateRefIgnoresParentMetadataObsolete() {
		var ref = getRef("");
		ref.setTitle("Ref");
		refRepository.save(ref);
		var other = getRef("@other");
		other.setTitle("Other");
		refService.create(other);

		assertThat(refRepository.existsByUrlAndOrigin(URL, "@other"))
			.isTrue();
		assertThat(refRepository.existsByUrlAndOrigin(URL, ""))
			.isTrue();
		var fetched = refRepository.findAll(RefFilter.builder().url(URL).build().spec());
		assertThat(fetched.size())
			.isEqualTo(2);
	}

	@Test
	void testGetUntaggedRef() {
		var ref = getRef();
		ref.setUrl(URL);
		refRepository.save(ref);

		assertThatThrownBy(() -> refService.get(ref.getUrl(), ref.getOrigin()))
			.isInstanceOf(AccessDeniedException.class);
	}

	@Test
	@WithMockUser(value = "+user/tester", roles = {"MOD"})
	void testGetUntaggedRefMod() {
		var ref = getRef();
		ref.setUrl(URL);
		refRepository.save(ref);

		var fetch = refService.get(ref.getUrl(), ref.getOrigin());

		assertThat(fetch)
			.isNotNull();
	}

	@Test
	void testGetUntaggedRemoteRef() {
		var ref = getRef();
		ref.setUrl(URL);
		ref.setOrigin("@remote");
		refRepository.save(ref);

		assertThatThrownBy(() -> refService.get(ref.getUrl(), ref.getOrigin()))
			.isInstanceOf(AccessDeniedException.class);
	}

	@Test
	@WithMockUser(value = "+user/tester", roles = {"MOD"})
	void testGetUntaggedRemoteRefMod() {
		var ref = getRef();
		ref.setUrl(URL);
		ref.setOrigin("@remote");
		refRepository.save(ref);

		assertThatThrownBy(() -> refService.get(ref.getUrl(), ref.getOrigin()))
			.isInstanceOf(AccessDeniedException.class);
	}

	@Test
	@WithMockUser(value = "+user/tester", roles = {"ADMIN"})
	void testGetUntaggedRemoteRefAdmin() {
		var ref = getRef();
		ref.setUrl(URL);
		ref.setOrigin("@remote");
		refRepository.save(ref);

		assertThatThrownBy(() -> refService.get(ref.getUrl(), ref.getOrigin()))
			.isInstanceOf(AccessDeniedException.class);
	}

	@Test
	void testGetPageUntaggedRef() {
		var ref = getRef();
		ref.setUrl(URL);
		refRepository.save(ref);

		var page = refService.page(
			RefFilter.builder().build(),
			PageRequest.of(0, 10));

		assertThat(page.getTotalElements())
			.isEqualTo(0);
	}

	@Test
	@WithMockUser(value = "+user/tester", roles = {"MOD"})
	void testGetPageUntaggedRef_Mod() {
		var ref = getRef();
		ref.setUrl(URL);
		refRepository.save(ref);

		var page = refService.page(
			RefFilter.builder().build(),
			PageRequest.of(0, 10));

		assertThat(page.getTotalElements())
			.isEqualTo(1);
	}

	@Test
	void testGetPageUntaggedRemoteRef() {
		var ref = getRef();
		ref.setUrl(URL);
		ref.setOrigin("@remote");
		refRepository.save(ref);

		var page = refService.page(
			RefFilter.builder().build(),
			PageRequest.of(0, 10));

		assertThat(page.getTotalElements())
			.isEqualTo(0);
	}

	@Test
	@WithMockUser(value = "+user/tester", roles = {"MOD"})
	void testGetPageUntaggedRemoteRef_Mod() {
		var ref = getRef();
		ref.setUrl(URL);
		ref.setOrigin("@remote");
		refRepository.save(ref);

		var page = refService.page(
			RefFilter.builder().build(),
			PageRequest.of(0, 10));

		assertThat(page.getTotalElements())
			.isEqualTo(0);
	}

	@Test
	@WithMockUser(value = "+user/tester", roles = {"ADMIN"})
	void testGetPageUntaggedRemoteRef_Admin() {
		var ref = getRef();
		ref.setUrl(URL);
		ref.setOrigin("@remote");
		refRepository.save(ref);

		var page = refService.page(
			RefFilter.builder().build(),
			PageRequest.of(0, 10));

		assertThat(page.getTotalElements())
			.isEqualTo(0);
	}

	Ref refWithTags(String... tags) {
		var ref = getRef();
		ref.setUrl(URL + UUID.randomUUID());
		ref.setTags(new ArrayList<>(List.of(tags)));
		refRepository.save(ref);
		return ref;
	}

	@Test
	void testGetPageRefWithQuery() {
		refWithTags("public");
		refWithTags("public", "custom", "extra");

		var page = refService.page(
			RefFilter
				.builder()
				.query("custom@other")
				.build(),
			PageRequest.of(0, 10));

		assertThat(page.getTotalElements())
			.isEqualTo(1);
	}
	@Test
	void testGetPageRefWithQueryPrivateTagFailed() {
		refWithTags("public");
		refWithTags("public", "_custom", "extra");

		assertThatThrownBy(() -> refService.page(
			RefFilter
				.builder()
				.query("_custom@other")
				.build(),
			PageRequest.of(0, 10)))
			.isInstanceOf(AccessDeniedException.class);
	}

	@Test
	void testGetPageRefWithNotQuery() {
		refWithTags("public", "custom");
		refWithTags("public", "custom", "extra");

		var page = refService.page(
			RefFilter
				.builder()
				.query("!custom@other | extra@other")
				.build(),
			PageRequest.of(0, 10));

		assertThat(page.getTotalElements())
			.isEqualTo(1);
	}

	Ref refWithOriginTags(String origin, String... tags) {
		var ref = getRef(origin);
		ref.setUrl(URL + UUID.randomUUID());
		ref.setTags(new ArrayList<>(List.of(tags)));
		refRepository.save(ref);
		return ref;
	}

	@Test
	void testGetPageRemoteRefWithQuery() {
		props.setDefaultReadAccess(new String[]{"@remote"});
		refWithOriginTags("@remote", "public");
		refWithOriginTags("@remote", "public", "custom", "extra");

		var page = refService.page(
			RefFilter
				.builder()
				.query("custom@remote")
				.build(),
			PageRequest.of(0, 10));

		assertThat(page.getTotalElements())
			.isEqualTo(1);
	}

	@Test
	void testGetPageRemoteRefWithQueryFailed() {
		props.setDefaultTagReadAccess(new String[]{"@remote"});
		refWithOriginTags("@remote", "public");
		refWithOriginTags("@remote", "public", "custom", "extra");

		var page = refService.page(
			RefFilter
				.builder()
				.query("custom@remote")
				.build(),
			PageRequest.of(0, 10));

		assertThat(page.getTotalElements())
			.isEqualTo(0);
	}

	@Test
	void testGetPageRemoteRefWithQueryAccessFailed() {
		refWithOriginTags("@remote", "public");
		refWithOriginTags("@remote", "public", "custom", "extra");

		assertThatThrownBy(() -> refService.page(
			RefFilter
				.builder()
				.query("_custom@remote")
				.build(),
			PageRequest.of(0, 10)))
			.isInstanceOf(AccessDeniedException.class);
	}

	@Test
	void testGetPageRemoteRefWithQueryPublicEmpty() {
		refWithOriginTags("@remote", "public");
		refWithOriginTags("@remote", "public", "custom", "extra");


		var page = refService.page(
			RefFilter
				.builder()
				.query("custom@remote")
				.build(),
			PageRequest.of(0, 10));

		assertThat(page.getTotalElements())
			.isEqualTo(0);
	}

	@Test
	void testGetPageRefWithOriginQuery() {
		props.setDefaultReadAccess(new String[]{"@a", "@b"});
		refWithOriginTags("@a", "public", "custom");
		refWithOriginTags("@b", "public", "custom", "extra");

		var page = refService.page(
			RefFilter
				.builder()
				.query("custom@a")
				.build(),
			PageRequest.of(0, 10));

		assertThat(page.getTotalElements())
			.isEqualTo(1);
	}

	@Test
	void testGetPageRefWithOriginQueryEmpty() {
		props.setDefaultTagReadAccess(new String[]{"@a", "@b"});
		refWithOriginTags("@a", "public", "custom");
		refWithOriginTags("@b", "public", "custom", "extra");

		var page = refService.page(
			RefFilter
				.builder()
				.query("custom@a")
				.build(),
			PageRequest.of(0, 10));

		assertThat(page.getTotalElements())
			.isEqualTo(0);
	}

	@Test
	void testGetPageRefWithOriginOrQuery() {
		props.setDefaultReadAccess(new String[]{"@a", "@b", "@c"});
		refWithOriginTags("@a", "public", "custom");
		refWithOriginTags("@b", "public", "custom", "extra");
		refWithOriginTags("@c", "public", "custom", "extra");

		var page = refService.page(
			RefFilter
				.builder()
				.query("custom@a | custom@b")
				.build(),
			PageRequest.of(0, 10));

		assertThat(page.getTotalElements())
			.isEqualTo(2);
	}

	@Test
	void testGetPageRefWithOriginOrQueryEmpty() {
		props.setDefaultTagReadAccess(new String[]{"@a", "@b", "@c"});
		refWithOriginTags("@a", "public", "custom");
		refWithOriginTags("@b", "public", "custom", "extra");
		refWithOriginTags("@c", "public", "custom", "extra");

		var page = refService.page(
			RefFilter
				.builder()
				.query("custom@a | custom@b")
				.build(),
			PageRequest.of(0, 10));

		assertThat(page.getTotalElements())
			.isEqualTo(0);
	}

	@Test
	void testGetPageRefWithOriginOrExtraSpacesQuery() {
		props.setDefaultReadAccess(new String[]{"@a", "@b", "@c"});
		refWithOriginTags("@a", "public", "custom");
		refWithOriginTags("@b", "public", "custom", "extra");
		refWithOriginTags("@c", "public", "custom", "extra");

		var page = refService.page(
			RefFilter
				.builder()
				.query("  custom@a  |  custom@b  : extra@b  ")
				.build(),
			PageRequest.of(0, 10));

		assertThat(page.getTotalElements())
			.isEqualTo(2);
	}

	@Test
	void testGetPageRefWithNotOriginQuery() {
		props.setDefaultReadAccess(new String[]{"@a", "@b", "@c"});
		refWithOriginTags("@a", "public", "custom");
		refWithOriginTags("@b", "public", "custom", "extra");
		refWithOriginTags("@c", "public", "custom", "extra");

		var page = refService.page(
			RefFilter
				.builder()
				.query("!custom@c:extra@*")
				.build(),
			PageRequest.of(0, 10));

		assertThat(page.getTotalElements())
			.isEqualTo(1);
	}

	@Test
	void testGetPageRefWithImpossibleOriginQuery() {
		props.setDefaultReadAccess(new String[]{"@a", "@b", "@c"});
		refWithOriginTags("@a", "public", "custom");
		refWithOriginTags("@b", "public", "custom", "extra");
		refWithOriginTags("@c", "public", "custom", "extra");

		var page = refService.page(
			RefFilter
				.builder()
				.query("custom@a:custom@b")
				.build(),
			PageRequest.of(0, 10));

		assertThat(page.getTotalElements())
			.isEqualTo(0);
	}

	@Test
	void testGetPageRefWithQueryAllMultiTenant() {
		refWithOriginTags("@other", "public", "custom");
		refWithOriginTags("@other", "public", "custom", "extra");
		refWithOriginTags("@remote", "public", "custom");
		refWithOriginTags("@remote", "public", "custom", "extra");

		var page = refService.page(
			RefFilter
				.builder()
				.build(),
			PageRequest.of(0, 10));

		assertThat(page.getTotalElements())
			.isEqualTo(2);
	}

	@Test
	void testGetPageRefWithQueryMultiTenant() {
		refWithOriginTags("@other", "public", "custom");
		refWithOriginTags("@other", "public", "custom", "extra");
		refWithOriginTags("@remote", "public", "custom");
		refWithOriginTags("@remote", "public", "custom", "extra");

		var page = refService.page(
			RefFilter
				.builder()
				.query("extra@*")
				.build(),
			PageRequest.of(0, 10));

		assertThat(page.getTotalElements())
			.isEqualTo(1);
	}

	@Test
	void testGetPageRefWithQueryMultiAuthTenant() {
		props.setDefaultReadAccess(new String[]{"@remote"});
		refWithOriginTags("@other", "public", "custom");
		refWithOriginTags("@other", "public", "custom", "extra");
		refWithOriginTags("@remote", "public", "custom");
		refWithOriginTags("@remote", "public", "custom", "extra");

		var page = refService.page(
			RefFilter
				.builder()
				.build(),
			PageRequest.of(0, 10));

		assertThat(page.getTotalElements())
			.isEqualTo(4);
	}

	@Test
	void testGetPageRefWithPublicTag() {
		var ref = getRef();
		ref.setUrl(URL);
		ref.setTags(new ArrayList<>(List.of("public")));
		refRepository.save(ref);

		var page = refService.page(
			RefFilter.builder().build(),
			PageRequest.of(0, 10));

		assertThat(page.getTotalElements())
			.isEqualTo(1);
	}

	@Test
	void testGetPageRefWithReadableTags() {
		var user = getUser();
		user.setTag("+user/tester");
		user.setReadAccess(List.of("custom"));
		userRepository.save(user);
		var ref = getRef();
		ref.setUrl(URL);
		ref.setTags(new ArrayList<>(List.of("custom")));
		refRepository.save(ref);

		var page = refService.page(
			RefFilter.builder().build(),
			PageRequest.of(0, 10));

		assertThat(page.getTotalElements())
			.isEqualTo(1);
	}

	@Test
	void testGetPageRefWithReadablePrivateTags() {
		var user = getUser();
		user.setTag("+user/tester");
		user.setReadAccess(List.of("_secret", "_hot", "sauce"));
		userRepository.save(user);
		var ref = getRef();
		ref.setUrl(URL);
		ref.setTags(new ArrayList<>(List.of("_secret", "_other")));
		refRepository.save(ref);

		var page = refService.page(
			RefFilter.builder().build(),
			PageRequest.of(0, 10));

		assertThat(page.getTotalElements())
			.isEqualTo(1);
		assertThat(page.getContent().get(0).getTags())
			.containsExactly("_secret");
	}

	@Test
	void testGetPageRefWithUnreadablePrivateTags() {
		var ref = getRef();
		ref.setUrl(URL);
		ref.setTags(new ArrayList<>(List.of("_secret")));
		refRepository.save(ref);

		var page = refService.page(
			RefFilter.builder().build(),
			PageRequest.of(0, 10));

		assertThat(page.getTotalElements())
			.isEqualTo(0);
	}

	@Test
	void testGetPageRefFiltersUnreadablePrivateTags() {
		var ref = getRef();
		ref.setUrl(URL);
		ref.setTags(new ArrayList<>(List.of("public", "_secret")));
		refRepository.save(ref);

		var page = refService.page(
			RefFilter.builder().build(),
			PageRequest.of(0, 10));

		assertThat(page.getTotalElements())
			.isEqualTo(1);
		assertThat(page.getContent().get(0).getTags())
			.containsExactly("public");
	}

	@Test
	void testUpdateUntaggedRefFailed() {
		var ref = getRef();
		ref.setUrl(URL);
		ref.setTitle("First");
		refRepository.save(ref);
		var update = getRef();
		update.setUrl(URL);
		update.setTitle("Second");
		update.setModified(ref.getModified());

		assertThatThrownBy(() -> refService.update(update))
			.isInstanceOf(AccessDeniedException.class);

		assertThat(refRepository.existsByUrlAndOrigin(URL, "@other"))
			.isTrue();
		var fetched = refRepository.findOneByUrlAndOrigin(URL, "@other").get();
		assertThat(fetched.getTitle())
			.isEqualTo("First");
	}

	@Test
	void testUpdateRefWithPublicTagFailed() {
		var ref = getRef();
		ref.setUrl(URL);
		ref.setTitle("First");
		ref.setTags(new ArrayList<>(List.of("public")));
		refRepository.save(ref);
		var update = getRef();
		update.setUrl(URL);
		update.setTitle("Second");
		update.setTags(new ArrayList<>(List.of("public")));
		update.setModified(ref.getModified());

		assertThatThrownBy(() -> refService.update(update))
			.isInstanceOf(AccessDeniedException.class);

		assertThat(refRepository.existsByUrlAndOrigin(URL, "@other"))
			.isTrue();
		var fetched = refRepository.findOneByUrlAndOrigin(URL, "@other").get();
		assertThat(fetched.getTitle())
			.isEqualTo("First");
	}

	@Test
	void testUpdateRefWithUserTag() {
		var ref = getRef();
		ref.setUrl(URL);
		ref.setTitle("First");
		ref.setTags(new ArrayList<>(List.of("+user/tester")));
		refRepository.save(ref);
		var update = getRef();
		update.setUrl(URL);
		update.setTitle("Second");
		update.setTags(new ArrayList<>(List.of("+user/tester")));
		update.setModified(ref.getModified());

		refService.update(update);

		assertThat(refRepository.existsByUrlAndOrigin(URL, "@other"))
			.isTrue();
		var fetched = refRepository.findOneByUrlAndOrigin(URL, "@other").get();
		assertThat(fetched.getTitle())
			.isEqualTo("Second");
	}

	@Test
	void testUpdateLockedRefFailed() {
		var ref = getRef();
		ref.setUrl(URL);
		ref.setTitle("First");
		ref.setTags(new ArrayList<>(List.of("locked", "+user/tester")));
		refRepository.save(ref);
		var update = getRef();
		update.setUrl(URL);
		update.setTitle("Second");
		update.setTags(new ArrayList<>(List.of("+user/tester")));
		update.setModified(ref.getModified());

		assertThatThrownBy(() -> refService.update(update))
			.isInstanceOf(AccessDeniedException.class);

		assertThat(refRepository.existsByUrlAndOrigin(URL, "@other"))
			.isTrue();
		var fetched = refRepository.findOneByUrlAndOrigin(URL, "@other").get();
		assertThat(fetched.getTitle())
			.isEqualTo("First");
	}

	@Test
	@WithMockUser(value = "+user/tester", roles = "Admin")
	void testAdminUpdateLockedRefFailed() {
		var ref = getRef();
		ref.setUrl(URL);
		ref.setTitle("First");
		ref.setTags(new ArrayList<>(List.of("locked", "+user/tester")));
		refRepository.save(ref);
		var update = getRef();
		update.setUrl(URL);
		update.setTitle("Second");
		update.setTags(new ArrayList<>(List.of("+user/tester")));
		update.setModified(ref.getModified());

		assertThatThrownBy(() -> refService.update(update))
			.isInstanceOf(AccessDeniedException.class);

		assertThat(refRepository.existsByUrlAndOrigin(URL, "@other"))
			.isTrue();
		var fetched = refRepository.findOneByUrlAndOrigin(URL, "@other").get();
		assertThat(fetched.getTitle())
			.isEqualTo("First");
	}

	@Test
	void testUpdateModifiedRefFailed() {
		var ref = getRef();
		ref.setUrl(URL);
		ref.setTitle("First");
		ref.setTags(new ArrayList<>(List.of("+user/tester")));
		refRepository.save(ref);
		var update = getRef();
		update.setUrl(URL);
		update.setTitle("Second");
		update.setTags(new ArrayList<>(List.of("+user/tester")));
		update.setModified(ref.getModified().minusSeconds(60));

		assertThatThrownBy(() -> refService.update(update))
			.isInstanceOf(ModifiedException.class);

		assertThat(refRepository.existsByUrlAndOrigin(URL, "@other"))
			.isTrue();
		var fetched = refRepository.findOneByUrlAndOrigin(URL, "@other").get();
		assertThat(fetched.getTitle())
			.isEqualTo("First");
	}

	@Test
	void testUpdateRefWithReadableTagsFailed() {
		var user = getUser();
		user.setTag("+user/tester");
		user.setReadAccess(List.of("custom"));
		userRepository.save(user);
		var ref = getRef();
		ref.setUrl(URL);
		ref.setTitle("First");
		ref.setTags(new ArrayList<>(List.of("custom")));
		refRepository.save(ref);
		var update = getRef();
		update.setUrl(URL);
		update.setTitle("Second");
		update.setTags(new ArrayList<>(List.of("custom")));
		update.setModified(ref.getModified());

		assertThatThrownBy(() -> refService.update(update))
			.isInstanceOf(AccessDeniedException.class);

		assertThat(refRepository.existsByUrlAndOrigin(URL, "@other"))
			.isTrue();
		var fetched = refRepository.findOneByUrlAndOrigin(URL, "@other").get();
		assertThat(fetched.getTitle())
			.isEqualTo("First");
	}

	@Test
	void testUpdateRemoteRefFailed() {
		var user = getUser();
		user.setTag("+user/tester");
		user.setReadAccess(List.of("+custom"));
		user.setWriteAccess(List.of("+custom"));
		userRepository.save(user);
		var ref = getRef();
		ref.setUrl(URL);
		ref.setOrigin("@remote");
		ref.setTitle("First");
		ref.setTags(new ArrayList<>(List.of("+custom")));
		refRepository.save(ref);
		var update = getRef();
		update.setUrl(URL);
		update.setOrigin("@remote");
		update.setTitle("Second");
		update.setTags(new ArrayList<>(List.of("+custom")));
		update.setModified(ref.getModified());

		assertThatThrownBy(() -> refService.update(update))
			.isInstanceOf(AccessDeniedException.class);

		assertThat(refRepository.existsByUrlAndOrigin(URL, "@remote"))
			.isTrue();
		var fetched = refRepository.findOneByUrlAndOrigin(URL, "@remote").get();
		assertThat(fetched.getTitle())
			.isEqualTo("First");
	}

	@Test
	void testUpdateRefWithWritableTags() {
		var user = getUser();
		user.setTag("+user/tester");
		user.setWriteAccess(List.of("+custom"));
		userRepository.save(user);
		var ref = getRef();
		ref.setUrl(URL);
		ref.setTitle("First");
		ref.setTags(new ArrayList<>(List.of("+custom")));
		refRepository.save(ref);
		var update = getRef();
		update.setUrl(URL);
		update.setTitle("Second");
		update.setTags(new ArrayList<>(List.of("+custom")));
		update.setModified(ref.getModified());

		refService.update(update);

		assertThat(refRepository.existsByUrlAndOrigin(URL, "@other"))
			.isTrue();
		var fetched = refRepository.findOneByUrlAndOrigin(URL, "@other").get();
		assertThat(fetched.getTitle())
			.isEqualTo("Second");
	}

	@Test
	void testUpdateRefWithUnreadablePrivateTags() {
		var ref = getRef();
		ref.setUrl(URL);
		ref.setTitle("First");
		ref.setTags(new ArrayList<>(List.of("_secret")));
		refRepository.save(ref);
		var update = getRef();
		update.setUrl(URL);
		update.setTitle("Second");
		update.setModified(ref.getModified());

		assertThatThrownBy(() -> refService.update(update))
			.isInstanceOf(AccessDeniedException.class);

		assertThat(refRepository.existsByUrlAndOrigin(URL, "@other"))
			.isTrue();
		var fetched = refRepository.findOneByUrlAndOrigin(URL, "@other").get();
		assertThat(fetched.getTitle())
			.isEqualTo("First");
	}

	@Test
	void testUpdateRefWithLoosingHiddenTags() {
		var ref = getRef();
		ref.setUrl(URL);
		ref.setTitle("First");
		ref.setTags(new ArrayList<>(List.of("+user/tester", "_secret")));
		refRepository.save(ref);
		var update = getRef();
		update.setUrl(URL);
		update.setTitle("Second");
		update.setTags(new ArrayList<>(List.of("+user/tester", "custom")));
		update.setModified(ref.getModified());

		refService.update(update);

		assertThat(refRepository.existsByUrlAndOrigin(URL, "@other"))
			.isTrue();
		var fetched = refRepository.findOneByUrlAndOrigin(URL, "@other").get();
		assertThat(fetched.getTitle())
			.isEqualTo("Second");
		assertThat(fetched.getTags())
			.contains("+user/tester", "custom", "_secret");
	}

	@Test
	void testUpdateRefWithReadablePrivateTags() {
		var user = getUser();
		user.setTag("+user/tester");
		user.setReadAccess(List.of("_secret"));
		userRepository.save(user);
		var ref = getRef();
		ref.setUrl(URL);
		ref.setTitle("First");
		ref.setTags(new ArrayList<>(List.of("_secret")));
		refRepository.save(ref);
		var update = getRef();
		update.setUrl(URL);
		update.setTitle("Second");
		update.setModified(ref.getModified());

		assertThatThrownBy(() -> refService.update(update))
			.isInstanceOf(AccessDeniedException.class);

		assertThat(refRepository.existsByUrlAndOrigin(URL, "@other"))
			.isTrue();
		var fetched = refRepository.findOneByUrlAndOrigin(URL, "@other").get();
		assertThat(fetched.getTitle())
			.isEqualTo("First");
	}

	@Test
	void testUpdateRefWithWritablePrivateTags() {
		var user = getUser();
		user.setTag("+user/tester");
		user.setWriteAccess(List.of("_secret"));
		userRepository.save(user);
		var ref = getRef();
		ref.setUrl(URL);
		ref.setTitle("First");
		ref.setTags(new ArrayList<>(List.of("_secret")));
		refRepository.save(ref);
		var update = getRef();
		update.setUrl(URL);
		update.setTitle("Second");
		update.setModified(ref.getModified());

		refService.update(update);

		assertThat(refRepository.existsByUrlAndOrigin(URL, "@other"))
			.isTrue();
		var fetched = refRepository.findOneByUrlAndOrigin(URL, "@other").get();
		assertThat(fetched.getTitle())
			.isEqualTo("Second");
	}

	@Test
	void testUpdateRefCreatesMetadata() {
		var source = getRef();
		source.setUrl(URL + "source");
		source.setTitle("Source");
		source.setTags(new ArrayList<>(List.of("+user/tester")));
		refService.create(source);
		var ref = getRef();
		ref.setUrl(URL);
		ref.setTitle("First");
		ref.setTags(new ArrayList<>(List.of("+user/tester")));
		refService.create(ref);
		var update = getRef();
		update.setUrl(URL);
		update.setSources(List.of(URL + "source"));
		update.setTitle("Second");
		update.setTags(new ArrayList<>(List.of("+user/tester")));
		update.setModified(ref.getModified());

		refService.update(update);

		assertThat(refRepository.existsByUrlAndOrigin(URL + "source", "@other"))
			.isTrue();
		var fetched = refRepository.findOneByUrlAndOrigin(URL + "source", "@other").get();
		assertThat(fetched.getTitle())
			.isEqualTo("Source");
		assertThat(fetched.getMetadata().getResponses())
			.containsExactly(URL);
	}

	@Test
	void testUpdateRefCreatesMetadataWithoutInternal() {
		var source = getRef();
		source.setUrl(URL + "source");
		source.setTitle("Source");
		source.setTags(new ArrayList<>(List.of("+user/tester")));
		refService.create(source);
		var ref = getRef();
		ref.setUrl(URL);
		ref.setTitle("First");
		ref.setTags(new ArrayList<>(List.of("+user/tester", "internal")));
		refService.create(ref);
		var update = getRef();
		update.setUrl(URL);
		update.setSources(List.of(URL + "source"));
		update.setTitle("Second");
		update.setTags(new ArrayList<>(List.of("+user/tester", "internal")));
		update.setModified(ref.getModified());

		refService.update(update);

		assertThat(refRepository.existsByUrlAndOrigin(URL + "source", "@other"))
			.isTrue();
		var fetched = refRepository.findOneByUrlAndOrigin(URL + "source", "@other").get();
		assertThat(fetched.getTitle())
			.isEqualTo("Source");
		assertThat(fetched.getMetadata().getResponses())
			.isNull();
	}

	@Test
	void testUpdateRefUpdatesMetadata() {
		var source = getRef();
		source.setUrl(URL + "source");
		source.setTitle("Source");
		source.setTags(new ArrayList<>(List.of("+user/tester")));
		refService.create(source);
		var ref = getRef();
		ref.setUrl(URL);
		ref.setSources(List.of(URL + "source"));
		ref.setTitle("First");
		ref.setTags(new ArrayList<>(List.of("+user/tester")));
		refService.create(ref);
		var update = getRef();
		update.setUrl(URL);
		update.setTitle("Second");
		update.setTags(new ArrayList<>(List.of("+user/tester")));
		update.setModified(ref.getModified());

		refService.update(update);

		assertThat(refRepository.existsByUrlAndOrigin(URL + "source", "@other"))
			.isTrue();
		var fetched = refRepository.findOneByUrlAndOrigin(URL + "source", "@other").get();
		assertThat(fetched.getTitle())
			.isEqualTo("Source");
		assertThat(fetched.getMetadata().getResponses())
			.isNull();
	}

	@Test
	void testUpdateRefCreatesPluginMetadata() {
		var plugin = getPlugin();
		plugin.setTag("plugin/comment");
		pluginRepository.save(plugin);
		var source = getRef();
		source.setUrl(URL + "source");
		source.setTitle("Source");
		source.setTags(new ArrayList<>(List.of("+user/tester")));
		refService.create(source);
		var ref = getRef();
		ref.setUrl(URL);
		ref.setTitle("First");
		ref.setTags(new ArrayList<>(List.of("+user/tester", "plugin/comment")));
		refService.create(ref);
		var update = getRef();
		update.setUrl(URL);
		update.setSources(List.of(URL + "source"));
		update.setTitle("Second");
		update.setTags(new ArrayList<>(List.of("+user/tester", "plugin/comment")));
		update.setModified(ref.getModified());

		refService.update(update);

		assertThat(refRepository.existsByUrlAndOrigin(URL + "source", "@other"))
			.isTrue();
		var fetched = refRepository.findOneByUrlAndOrigin(URL + "source", "@other").get();
		assertThat(fetched.getTitle())
			.isEqualTo("Source");
		assertThat(fetched.getMetadata().getResponses())
			.containsExactly(URL);
		assertThat(fetched.getMetadata().getPlugins().get("plugin/comment"))
			.isEqualTo(1);
	}

	@Test
	void testUpdateRefUpdatesPluginMetadata() {
		var plugin = getPlugin();
		plugin.setTag("plugin/comment");
		pluginRepository.save(plugin);
		var source = getRef();
		source.setUrl(URL + "source");
		source.setTitle("Source");
		source.setTags(new ArrayList<>(List.of("+user/tester")));
		refService.create(source);
		var ref = getRef();
		ref.setUrl(URL);
		ref.setSources(List.of(URL + "source"));
		ref.setTitle("First");
		ref.setTags(new ArrayList<>(List.of("+user/tester", "plugin/comment")));
		refService.create(ref);
		var update = getRef();
		update.setUrl(URL);
		update.setTitle("Second");
		update.setTags(new ArrayList<>(List.of("+user/tester", "plugin/comment")));
		update.setModified(ref.getModified());

		refService.update(update);

		assertThat(refRepository.existsByUrlAndOrigin(URL + "source", "@other"))
			.isTrue();
		var fetched = refRepository.findOneByUrlAndOrigin(URL + "source", "@other").get();
		assertThat(fetched.getTitle())
			.isEqualTo("Source");
		assertThat(fetched.getMetadata().getResponses())
			.isNullOrEmpty();
		assertThat(fetched.getMetadata().getPlugins())
			.isNullOrEmpty();
	}

	@Test
	void testDeleteUntaggedRef() {
		var ref = getRef();
		ref.setUrl(URL);
		ref.setTitle("First");
		refRepository.save(ref);

		assertThatThrownBy(() -> refService.delete(ref.getUrl(), ref.getOrigin()))
			.isInstanceOf(AccessDeniedException.class);

		assertThat(refRepository.existsByUrlAndOrigin(URL, "@other"))
			.isTrue();
		var fetched = refRepository.findOneByUrlAndOrigin(URL, "@other").get();
		assertThat(fetched.getTitle())
			.isEqualTo("First");
	}

	@Test
	void testDeleteRemoteRefFailed() {
		var ref = getRef();
		ref.setUrl(URL);
		ref.setTitle("First");
		ref.setOrigin("@remote");
		refRepository.save(ref);

		assertThatThrownBy(() -> refService.delete(ref.getUrl(), ref.getOrigin()))
			.isInstanceOf(AccessDeniedException.class);

		assertThat(refRepository.existsByUrlAndOrigin(URL, "@remote"))
			.isTrue();
		var fetched = refRepository.findOneByUrlAndOrigin(URL, "@remote").get();
		assertThat(fetched.getTitle())
			.isEqualTo("First");
	}

	@Test
	void testDeleteRefWithPublicTag() {
		var ref = getRef();
		ref.setUrl(URL);
		ref.setTitle("First");
		ref.setTags(new ArrayList<>(List.of("public")));
		refRepository.save(ref);

		assertThatThrownBy(() -> refService.delete(ref.getUrl(), ref.getOrigin()))
			.isInstanceOf(AccessDeniedException.class);

		assertThat(refRepository.existsByUrlAndOrigin(URL, "@other"))
			.isTrue();
		var fetched = refRepository.findOneByUrlAndOrigin(URL, "@other").get();
		assertThat(fetched.getTitle())
			.isEqualTo("First");
	}

	@Test
	void testDeleteRefWithUserTag() {
		var ref = getRef();
		ref.setUrl(URL);
		ref.setTitle("First");
		ref.setTags(new ArrayList<>(List.of("+user/tester")));
		refRepository.save(ref);

		refService.delete(ref.getUrl(), ref.getOrigin());

		assertThat(refRepository.existsByUrlAndOrigin(URL, "@other"))
			.isFalse();
	}

	@Test
	void testDeleteRefWithReadableTags() {
		var user = getUser();
		user.setTag("+user/tester");
		user.setReadAccess(List.of("custom"));
		userRepository.save(user);
		var ref = getRef();
		ref.setUrl(URL);
		ref.setTitle("First");
		ref.setTags(new ArrayList<>(List.of("custom")));
		refRepository.save(ref);

		assertThatThrownBy(() -> refService.delete(ref.getUrl(), ref.getOrigin()))
			.isInstanceOf(AccessDeniedException.class);

		assertThat(refRepository.existsByUrlAndOrigin(URL, "@other"))
			.isTrue();
		var fetched = refRepository.findOneByUrlAndOrigin(URL, "@other").get();
		assertThat(fetched.getTitle())
			.isEqualTo("First");
	}

	@Test
	void testDeleteRefWithWritableTags() {
		var user = getUser();
		user.setTag("+user/tester");
		user.setWriteAccess(List.of("+custom"));
		userRepository.save(user);
		var ref = getRef();
		ref.setUrl(URL);
		ref.setTitle("First");
		ref.setTags(new ArrayList<>(List.of("+custom")));
		refRepository.save(ref);

		refService.delete(ref.getUrl(), ref.getOrigin());

		assertThat(refRepository.existsByUrlAndOrigin(URL, "@other"))
			.isFalse();
	}

	@Test
	void testDeleteRefWithUnreadablePrivateTags() {
		var ref = getRef();
		ref.setUrl(URL);
		ref.setTitle("First");
		ref.setTags(new ArrayList<>(List.of("_secret")));
		refRepository.save(ref);

		assertThatThrownBy(() -> refService.delete(ref.getUrl(), ref.getOrigin()))
			.isInstanceOf(AccessDeniedException.class);

		assertThat(refRepository.existsByUrlAndOrigin(URL, "@other"))
			.isTrue();
		var fetched = refRepository.findOneByUrlAndOrigin(URL, "@other").get();
		assertThat(fetched.getTitle())
			.isEqualTo("First");
	}

	@Test
	void testDeleteRefWithReadablePrivateTags() {
		var user = getUser();
		user.setTag("+user/tester");
		user.setReadAccess(List.of("_secret"));
		userRepository.save(user);
		var ref = getRef();
		ref.setUrl(URL);
		ref.setTitle("First");
		ref.setTags(new ArrayList<>(List.of("_secret")));
		refRepository.save(ref);

		assertThatThrownBy(() -> refService.delete(ref.getUrl(), ref.getOrigin()))
			.isInstanceOf(AccessDeniedException.class);

		assertThat(refRepository.existsByUrlAndOrigin(URL, "@other"))
			.isTrue();
		var fetched = refRepository.findOneByUrlAndOrigin(URL, "@other").get();
		assertThat(fetched.getTitle())
			.isEqualTo("First");
	}

	@Test
	void testDeleteRefWithWritablePrivateTags() {
		var user = getUser();
		user.setTag("+user/tester");
		user.setWriteAccess(List.of("_secret"));
		userRepository.save(user);
		var ref = getRef();
		ref.setUrl(URL);
		ref.setTitle("First");
		ref.setTags(new ArrayList<>(List.of("_secret")));
		refRepository.save(ref);

		refService.delete(ref.getUrl(), ref.getOrigin());

		assertThat(refRepository.existsByUrlAndOrigin(URL, "@other"))
			.isFalse();
	}

}

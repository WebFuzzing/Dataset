package jasper.security;

import jasper.component.ConfigCache;
import jasper.config.Config.SecurityConfig;
import jasper.config.Config.ServerConfig;
import jasper.config.Props;
import jasper.config.SecurityConfiguration;
import jasper.domain.Ref;
import jasper.domain.User;
import jasper.repository.RefRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static jasper.repository.spec.QualifiedTag.qt;
import static jasper.repository.spec.QualifiedTag.selector;
import static jasper.security.AuthoritiesConstants.ANONYMOUS;
import static jasper.security.AuthoritiesConstants.EDITOR;
import static jasper.security.AuthoritiesConstants.USER;
import static jasper.security.AuthoritiesConstants.VIEWER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AuthMultiTenantUnitTest {

	RoleHierarchy roleHierarchy = new SecurityConfiguration().roleHierarchy();

	Auth getAuth(User user, String ...roles) {
		var a = new Auth(new Props(), null, getConfigs(user), null);
		a.props.setLocalOrigin(user.getOrigin());
		a.principal = user.getQualifiedTag();
		a.user = Optional.of(user);
		a.roles = getRoles(roles);
		a.origin = user.getOrigin();
		return a;
	}

	Set<String> getRoles(String ...roles) {
		if (roles.length == 0) roles = new String[]{ ANONYMOUS };
		var userAuthorities = Arrays.stream(roles)
			.map(SimpleGrantedAuthority::new)
			.collect(Collectors.toList());
		return AuthorityUtils.authorityListToSet(roleHierarchy.getReachableGrantedAuthorities(userAuthorities));
	}

	User getUser(String userTag, String ...tags) {
		var u = new User();
		var qt = qt(userTag);
		u.setTag(qt.tag);
		u.setOrigin(qt.origin);
		u.setReadAccess(new ArrayList<>(List.of(tags)));
		u.setWriteAccess(new ArrayList<>(List.of(tags)));
		u.setTagReadAccess(new ArrayList<>(List.of(tags)));
		u.setTagWriteAccess(new ArrayList<>(List.of(tags)));
		return u;
	}

	Ref getRef(String ...tags) {
		var r = new Ref();
		r.setUrl("https://jasperkm.info/");
		r.setOrigin("@other");
		r.setTags(new ArrayList<>(List.of(tags)));
		return r;
	}

	RefRepository getRefRepo(Ref ...refs) {
		var mock = mock(RefRepository.class);
		when(mock.findOneByUrlAndOrigin(anyString(), anyString()))
			.thenReturn(Optional.empty());
		for (var ref : refs) {
			when(mock.findOneByUrlAndOrigin(ref.getUrl(), ref.getOrigin()))
				.thenReturn(Optional.of(ref));
		}
		return mock;
	}

	ConfigCache getConfigs(User ...users) {
		var configCache = mock(ConfigCache.class);
		when(configCache.getUser(anyString()))
			.thenReturn(null);
		for (var user : users) {
			when(configCache.getUser(user.getQualifiedTag()))
				.thenReturn(user);
		}
		var root = new ServerConfig();
		var security = new SecurityConfig();
		security.setMode("jwt");
		when(configCache.root()).thenReturn(root);
		when(configCache.security(anyString())).thenReturn(security);
		return configCache;
	}

	@Test
	void testCanReadRef_Public() {
		var auth = getAuth(getUser("+user/test@other"));
		var ref = getRef("public");

		assertThat(auth.canReadRef(ref))
			.isTrue();
	}

	@Test
	void testCanReadRef_PublicRemote() {
		var auth = getAuth(getUser("+user/test@other"));
		var ref = getRef("public");
		ref.setOrigin(ref.getOrigin() + ".remote");

		assertThat(auth.canReadRef(ref))
			.isTrue();
	}

	@Test
	void testCanReadRef_PublicOriginFailed() {
		var auth = getAuth(getUser("+user/test@other"));
		var ref = getRef("public");
		ref.setOrigin("@inaccessible");

		assertThat(auth.canReadRef(ref))
			.isFalse();
	}

	@Test
	void testCanReadRef_NonPublicFailed() {
		var auth = getAuth(getUser("+user/test@other"));
		var ref = getRef();

		assertThat(auth.canReadRef(ref))
			.isFalse();
	}

	@Test
	void testCanReadRef_RemoteRef() {
		var user = getUser("+user/test@other");
		user.setReadAccess(List.of("public"));
		var auth = getAuth(user);
		var ref = getRef("public");
		ref.setOrigin("@remote");

		assertThat(auth.canReadRef(ref))
			.isFalse();
	}

	@Test
	void testCanReadRef_RemoteRefFailed() {
		var auth = getAuth(getUser("+user/test@other"));
		var ref = getRef("public");
		ref.setOrigin("@remote");

		assertThat(auth.canReadRef(ref))
			.isFalse();
	}

	@Test
	void testCanReadRef_RemoteRefPermissionsFailed() {
		var auth = getAuth(getUser("+user/test@other"));
		auth.readAccess = List.of(selector("public"));
		var ref = getRef("public");
		ref.setOrigin("@remote");

		assertThat(auth.canReadRef(ref))
			.isFalse();
	}

	@Test
	void testCanReadRef_ReadAccess() {
		var user = getUser("+user/test@other");
		user.getReadAccess().add("+custom");
		var auth = getAuth(user, VIEWER);
		var ref = getRef("+custom");

		assertThat(auth.canReadRef(ref))
			.isTrue();
	}

	@Test
	void testCanReadRef_AuthReadAccess() {
		var auth = getAuth(getUser("+user/test@other"), VIEWER);
		auth.readAccess = List.of(selector("+custom"));
		var ref = getRef("+custom");

		assertThat(auth.canReadRef(ref))
			.isTrue();
	}

	@Test
	void testCanReadRef_AuthReadAccessRemote() {
		var auth = getAuth(getUser("+user/test@other"), VIEWER);
		auth.readAccess = List.of(selector("+custom"));
		var ref = getRef("+custom");
		ref.setOrigin("@other.remote");

		assertThat(auth.canReadRef(ref))
			.isTrue();
	}

	@Test
	void testCanReadRef_AuthReadAccessRemoteFailed() {
		var auth = getAuth(getUser("+user/test@other"), VIEWER);
		auth.readAccess = List.of(selector("+custom"));
		var ref = getRef("+custom");
		ref.setOrigin("@remote");

		assertThat(auth.canReadRef(ref))
			.isFalse();
	}

	@Test
	void testCanReadRef_WriteAccessFailed() {
		var user = getUser("+user/test@other");
		user.getWriteAccess().add("+custom");
		var auth = getAuth(user, VIEWER);
		var ref = getRef("+custom");

		assertThat(auth.canReadRef(ref))
			.isFalse();
	}

	@Test
	void testCanReadRef_TagReadAccessFailed() {
		var user = getUser("+user/test@other");
		user.getTagReadAccess().add("+custom");
		var auth = getAuth(user, VIEWER);
		var ref = getRef("+custom");

		assertThat(auth.canReadRef(ref))
			.isFalse();
	}

	@Test
	void testCanReadRef_TagWriteAccessFailed() {
		var user = getUser("+user/test@other");
		user.getTagWriteAccess().add("+custom");
		var auth = getAuth(user, VIEWER);
		var ref = getRef("+custom");

		assertThat(auth.canReadRef(ref))
			.isFalse();
	}

	@Test
	void testCanWriteRef_WriteAccess() {
		var user = getUser("+user/test@other");
		user.getWriteAccess().add("+custom");
		var auth = getAuth(user, USER);
		var ref = getRef("+custom");
		auth.refRepository = getRefRepo(ref);

		assertThat(auth.canWriteRef(ref))
			.isTrue();
	}

	@Test
	void testCanWriteRef_UserTag() {
		var user = getUser("+user/test@other");
		var auth = getAuth(user, USER);
		var ref = getRef("+user/test");
		auth.refRepository = getRefRepo(ref);

		assertThat(auth.canWriteRef(ref))
			.isTrue();
	}

	@Test
	void testCanWriteRef_RemoteFailed() {
		var user = getUser("+user/test@other");
		var auth = getAuth(user, USER);
		var ref = getRef("+user/test");
		ref.setOrigin("@remote");
		auth.refRepository = getRefRepo(ref);

		assertThat(auth.canWriteRef(ref))
			.isFalse();
	}

	@Test
	void testCanWriteRef_PrivateUserTag() {
		var user = getUser("_user/test@other");
		var auth = getAuth(user, USER);
		var ref = getRef("_user/test");
		auth.refRepository = getRefRepo(ref);

		assertThat(auth.canWriteRef(ref))
			.isTrue();
	}

	@Test
	void testCanWriteRef_ReadAccessFailed() {
		var user = getUser("+user/test@other");
		user.getReadAccess().add("+custom");
		var auth = getAuth(user, USER);
		var ref = getRef("+custom");
		auth.refRepository = getRefRepo(ref);

		assertThat(auth.canWriteRef(ref))
			.isFalse();
	}

	@Test
	void testCanWriteRef_TagReadAccessFailed() {
		var user = getUser("+user/test@other");
		user.getTagReadAccess().add("+custom");
		var auth = getAuth(user, USER);
		var ref = getRef("+custom");
		auth.refRepository = getRefRepo(ref);

		assertThat(auth.canWriteRef(ref))
			.isFalse();
	}

	@Test
	void testCanWriteRef_TagWriteAccessFailed() {
		var user = getUser("+user/test@other");
		user.getTagWriteAccess().add("+custom");
		var auth = getAuth(user, USER);
		var ref = getRef("+custom");
		auth.refRepository = getRefRepo(ref);

		assertThat(auth.canWriteRef(ref))
			.isFalse();
	}

	@Test
	void testCanWriteRef_LockedFailed() {
		var user = getUser("+user/test@other");
		user.getWriteAccess().add("+custom");
		var auth = getAuth(user, USER);
		var ref = getRef("+custom", "locked");
		auth.refRepository = getRefRepo(ref);

		assertThat(auth.canWriteRef(ref))
			.isFalse();
	}

	@Test
	void testCanWriteRef_AddPublic() {
		var user = getUser("+user/test@other");
		user.getWriteAccess().add("+custom");
		var auth = getAuth(user, USER);
		var ref = getRef("+custom");
		auth.refRepository = getRefRepo(ref);

		assertThat(auth.canWriteRef(getRef("+custom", "new")))
			.isTrue();
	}

	@Test
	void testCanWriteRef_AddProtectedFailed() {
		var user = getUser("+user/test@other");
		var auth = getAuth(user, USER);
		var ref = getRef("+user/test");
		auth.refRepository = getRefRepo(ref);

		assertThat(auth.canWriteRef(getRef("+user/test", "+custom")))
			.isFalse();
	}

	@Test
	void testCanWriteRef_AllowExistingProtected() {
		var user = getUser("+user/test@other");
		var auth = getAuth(user, USER);
		var ref = getRef("+user/test", "+custom");
		auth.refRepository = getRefRepo(ref);

		assertThat(auth.canWriteRef(getRef("+user/test", "+custom", "new")))
			.isTrue();
	}

	@Test
	void testCanWriteRef_AddPrivateFailed() {
		var user = getUser("+user/test@other");
		var auth = getAuth(user, USER);
		var ref = getRef("+user/test");
		auth.refRepository = getRefRepo(ref);

		assertThat(auth.canWriteRef(getRef("+user/test", "_custom")))
			.isFalse();
	}

	@Test
	void testCanWriteRef_AllowExistingPrivate() {
		var user = getUser("+user/test@other");
		var auth = getAuth(user, USER);
		var ref = getRef("+user/test", "_custom");
		auth.refRepository = getRefRepo(ref);

		assertThat(auth.canWriteRef(getRef("+user/test", "_custom", "new")))
			.isTrue();
	}

	@Test
	void testCanAddTag_Public() {
		var user = getUser("+user/test@other");
		var auth = getAuth(user, USER);

		assertThat(auth.canAddTag("custom"))
			.isTrue();
	}

	@Test
	void testCanAddTag_PrivateFailed() {
		var user = getUser("+user/test@other");
		var auth = getAuth(user, USER);

		assertThat(auth.canAddTag("_custom"))
			.isFalse();
	}

	@Test
	void testCanAddTag_PrivateTagReadAccess() {
		var user = getUser("+user/test@other");
		user.getTagReadAccess().add("_custom");
		var auth = getAuth(user, USER);

		assertThat(auth.canAddTag("_custom"))
			.isTrue();
	}

	@Test
	void testCanAddTag_PrivateTagWriteAccessFailed() {
		var user = getUser("+user/test@other");
		user.getTagWriteAccess().add("_custom");
		var auth = getAuth(user, USER);

		assertThat(auth.canAddTag("_custom"))
			.isFalse();
	}

	@Test
	void testCanAddTag_ProtectedTagReadAccess() {
		var user = getUser("+user/test@other");
		user.getTagReadAccess().add("+custom");
		var auth = getAuth(user, USER);

		assertThat(auth.canAddTag("+custom"))
			.isTrue();
	}

	@Test
	void testCanAddTag_ProtectedTagWriteAccessFailed() {
		var user = getUser("+user/test@other");
		user.getTagWriteAccess().add("+custom");
		var auth = getAuth(user, USER);

		assertThat(auth.canAddTag("+custom"))
			.isFalse();
	}

	@Test
	void testCanAddTag_UserTag() {
		var user = getUser("+user/test@other");
		var auth = getAuth(user, USER);

		assertThat(auth.canAddTag("+user/test"))
			.isTrue();
	}

	@Test
	void testCanAddTag_PrivateUserTag() {
		var user = getUser("_user/test@other");
		var auth = getAuth(user, USER);

		assertThat(auth.canAddTag("_user/test"))
			.isTrue();
	}

	@Test
	void testCanTag_Public() {
		var user = getUser("+user/test@other");
		user.getTagWriteAccess().add("custom");
		var auth = getAuth(user, USER);
		var ref = getRef("+user/test");
		auth.refRepository = getRefRepo(ref);

		assertThat(auth.canTag("custom", ref.getUrl(), ref.getOrigin()))
			.isTrue();
	}

	@Test
	void testCanTag_PublicRemoteFailed() {
		var user = getUser("+user/test@other");
		user.getTagWriteAccess().add("custom");
		var auth = getAuth(user, USER);
		var ref = getRef("+user/test");
		ref.setOrigin("@remote");
		auth.refRepository = getRefRepo(ref);

		assertThat(auth.canTag("custom", ref.getUrl(), ref.getOrigin()))
			.isFalse();
	}

	@Test
	void testCanTag_PublicRemotePermissionFailed() {
		var user = getUser("+user/test@other");
		user.getTagWriteAccess().add("custom@remote");
		var auth = getAuth(user, USER);
		var ref = getRef("+user/test");
		ref.setOrigin("@remote");
		auth.refRepository = getRefRepo(ref);

		assertThat(auth.canTag("custom", ref.getUrl(), ref.getOrigin()))
			.isFalse();
	}

	@Test
	void testCanTag_PublicFailed() {
		var user = getUser("+user/test@other");
		user.getTagReadAccess().add("custom");
		var auth = getAuth(user, USER);
		var ref = getRef();
		auth.refRepository = getRefRepo(ref);

		assertThat(auth.canTag("custom", ref.getUrl(), ref.getOrigin()))
			.isFalse();
	}

	@Test
	void testCanTag_Protected() {
		var user = getUser("+user/test@other");
		user.getTagReadAccess().add("+custom");
		var auth = getAuth(user, USER);
		var ref = getRef("+user/test");
		auth.refRepository = getRefRepo(ref);

		assertThat(auth.canTag("+custom", ref.getUrl(), ref.getOrigin()))
			.isTrue();
	}

	@Test
	void testCanTag_Private() {
		var user = getUser("+user/test@other");
		user.getTagReadAccess().add("_custom");
		var auth = getAuth(user, USER);
		var ref = getRef("+user/test");
		auth.refRepository = getRefRepo(ref);

		assertThat(auth.canTag("_custom", ref.getUrl(), ref.getOrigin()))
			.isTrue();
	}

	@Test
	void testCanTag_PrivateFailed() {
		var user = getUser("+user/test@other");
		var auth = getAuth(user, USER);
		var ref = getRef("+user/test");
		auth.refRepository = getRefRepo(ref);

		assertThat(auth.canTag("_custom", ref.getUrl(), ref.getOrigin()))
			.isFalse();
	}

	@Test
	void testCanTag_EditorPublic() {
		var user = getUser("+user/test@other");
		var auth = getAuth(user, EDITOR);
		var ref = getRef("public");
		auth.refRepository = getRefRepo(ref);

		assertThat(auth.canTag("custom", ref.getUrl(), ref.getOrigin()))
			.isTrue();
	}

	@Test
	void testCanTag_EditorMakePublicFailed() {
		var user = getUser("+user/test@other");
		user.getReadAccess().add("custom");
		var auth = getAuth(user, EDITOR);
		var ref = getRef("custom");
		auth.refRepository = getRefRepo(ref);

		assertThat(auth.canTag("public", ref.getUrl(), ref.getOrigin()))
			.isFalse();
	}

	@Test
	void testCanTag_EditorMakeLockedFailed() {
		var user = getUser("+user/test@other");
		user.getReadAccess().add("custom");
		var auth = getAuth(user, EDITOR);
		var ref = getRef("custom");
		auth.refRepository = getRefRepo(ref);

		assertThat(auth.canTag("locked", ref.getUrl(), ref.getOrigin()))
			.isFalse();
	}

	@Test
	void testCanTag_EditorProtectedFailed() {
		var user = getUser("+user/test@other");
		user.getTagReadAccess().add("+custom");
		var auth = getAuth(user, EDITOR);
		var ref = getRef("custom");
		auth.refRepository = getRefRepo(ref);

		assertThat(auth.canTag("locked", ref.getUrl(), ref.getOrigin()))
			.isFalse();
	}

	@Test
	void testCanReadTag_Public() {
		var user = getUser("+user/test@other");
		var auth = getAuth(user, VIEWER);

		assertThat(auth.canReadTag("custom@other"))
			.isTrue();
	}

	@Test
	void testCanReadTag_PublicRemote() {
		var user = getUser("+user/test@other");
		var auth = getAuth(user, VIEWER);

		assertThat(auth.canReadTag("custom@other.remote"))
			.isTrue();
	}

	@Test
	void testCanReadTag_PublicRemoteFailed() {
		var user = getUser("+user/test@other");
		var auth = getAuth(user, VIEWER);

		assertThat(auth.canReadTag("custom@remote"))
			.isFalse();
	}

	@Test
	void testCanReadTag_Protected() {
		var user = getUser("+user/test@other");
		var auth = getAuth(user, VIEWER);

		assertThat(auth.canReadTag("+custom@other"))
			.isTrue();
	}

	@Test
	void testCanReadTag_ProtectedRemote() {
		var user = getUser("+user/test@other");
		var auth = getAuth(user, VIEWER);

		assertThat(auth.canReadTag("+custom@other.remote"))
			.isTrue();
	}

	@Test
	void testCanReadTag_ProtectedRemoteFailed() {
		var user = getUser("+user/test@other");
		var auth = getAuth(user, VIEWER);

		assertThat(auth.canReadTag("+custom@remote"))
			.isFalse();
	}

	@Test
	void testCanReadTag_Private() {
		var user = getUser("+user/test@other");
		user.getTagReadAccess().add("_custom");
		var auth = getAuth(user, VIEWER);

		assertThat(auth.canReadTag("_custom@other"))
			.isTrue();
	}

	@Test
	void testCanReadTag_PrivateRemote() {
		var user = getUser("+user/test@other");
		var auth = getAuth(user, VIEWER);
		auth.readAccess = List.of(selector("_custom"));

		assertThat(auth.canReadTag("_custom@other.remote"))
			.isTrue();
	}

	@Test
	void testCanReadTag_PrivateRemoteFailed() {
		var user = getUser("+user/test@other");
		var auth = getAuth(user, VIEWER);

		assertThat(auth.canReadTag("_custom@remote"))
			.isFalse();
	}

	@Test
	void testCanReadTag_UserTag() {
		var user = getUser("_user/test@other");
		var auth = getAuth(user, VIEWER);

		assertThat(auth.canReadTag("_user/test@other"))
			.isTrue();
	}

	@Test
	void testCanReadTag_UserTagRemote() {
		var user = getUser("_user/test@other");
		var auth = getAuth(user, VIEWER);
		auth.readAccess = List.of(selector("_user/test"));

		assertThat(auth.canReadTag("_user/test@other.remote"))
			.isTrue();
	}

	@Test
	void testCanReadTag_UserTagRemoteFailed() {
		var user = getUser("_user/test@other");
		var auth = getAuth(user, VIEWER);

		assertThat(auth.canReadTag("_user/test@remote"))
			.isFalse();
	}

	@Test
	void testCanReadTag_PrivateFailed() {
		var user = getUser("+user/test@other");
		var auth = getAuth(user, VIEWER);

		assertThat(auth.canReadTag("_custom@other"))
			.isFalse();
	}

	@Test
	void testCanWriteTag_Public() {
		var user = getUser("+user/test@other");
		user.getTagWriteAccess().add("custom");
		var auth = getAuth(user, USER);

		assertThat(auth.canWriteTag("custom@other"))
			.isTrue();
	}

	@Test
	void testCanWriteTag_RemotePublicFailed() {
		var user = getUser("+user/test@other");
		user.getTagWriteAccess().add("custom@remote");
		var auth = getAuth(user, USER);

		assertThat(auth.canWriteTag("custom@remote"))
			.isFalse();
	}

	@Test
	void testCanWriteTag_PublicFailed() {
		var user = getUser("+user/test@other");
		var auth = getAuth(user, USER);

		assertThat(auth.canWriteTag("custom@other"))
			.isFalse();
	}

	@Test
	void testCanWriteTag_PublicEditor() {
		var user = getUser("+user/test@other");
		var auth = getAuth(user, EDITOR);

		assertThat(auth.canWriteTag("custom@other"))
			.isTrue();
	}

	@Test
	void testCanWriteTag_RemotePublicEditorFailed() {
		var user = getUser("+user/test@other");
		var auth = getAuth(user, EDITOR);
		auth.tagWriteAccess = List.of(selector("custom"));

		assertThat(auth.canWriteTag("custom@remote"))
			.isFalse();
	}

	@Test
	void testCanWriteTag_Protected() {
		var user = getUser("+user/test@other");
		user.getTagWriteAccess().add("+custom");
		var auth = getAuth(user, USER);

		assertThat(auth.canWriteTag("+custom@other"))
			.isTrue();
	}

	@Test
	void testCanWriteTag_RemoteProtectedFailed() {
		var user = getUser("+user/test@other");
		var auth = getAuth(user, USER);
		auth.tagWriteAccess = List.of(selector("+custom"));

		assertThat(auth.canWriteTag("+custom@remote"))
			.isFalse();
	}

	@Test
	void testCanWriteTag_ProtectedFailed() {
		var user = getUser("+user/test@other");
		var auth = getAuth(user, USER);

		assertThat(auth.canWriteTag("+custom@other"))
			.isFalse();
	}

	@Test
	void testCanWriteTag_Private() {
		var user = getUser("+user/test@other");
		user.getTagWriteAccess().add("_custom");
		var auth = getAuth(user, USER);

		assertThat(auth.canWriteTag("_custom@other"))
			.isTrue();
	}

	@Test
	void testCanWriteTag_RemotePrivateFailed() {
		var user = getUser("+user/test@other");
		var auth = getAuth(user, USER);
		auth.tagWriteAccess = List.of(selector("_custom"));

		assertThat(auth.canWriteTag("_custom@remote"))
			.isFalse();
	}

	@Test
	void testCanWriteTag_PrivateFailed() {
		var user = getUser("+user/test@other");
		var auth = getAuth(user, USER);

		assertThat(auth.canWriteTag("_custom@other"))
			.isFalse();
	}

	@Test
	void testCanWriteTag_UserTag() {
		var user = getUser("+user/test@other");
		var auth = getAuth(user, USER);

		assertThat(auth.canWriteTag("+user/test@other"))
			.isTrue();
	}

	@Test
	void testCanWriteTag_UserTagRemoteFailed() {
		var user = getUser("+user/test@other");
		var auth = getAuth(user, USER);
		auth.tagWriteAccess = List.of(selector("+user/test"));

		assertThat(auth.canWriteTag("+user/test@remote"))
			.isFalse();
	}

	@Test
	void testCanReadQuery_Public() {
		var user = getUser("+user/test@other");
		var auth = getAuth(user, VIEWER);

		assertThat(auth.canReadQuery(() -> "custom@other"))
			.isTrue();
	}

	@Test
	void testCanReadQuery_PublicRemote() {
		var user = getUser("+user/test@other");
		var auth = getAuth(user, VIEWER);

		assertThat(auth.canReadQuery(() -> "custom@remote"))
			.isTrue();
	}

	@Test
	void testCanReadQuery_Protected() {
		var user = getUser("+user/test@other");
		var auth = getAuth(user, VIEWER);

		assertThat(auth.canReadQuery(() -> "+custom@other"))
			.isTrue();
	}

	@Test
	void testCanReadQuery_ProtectedRemote() {
		var user = getUser("+user/test@other");
		var auth = getAuth(user, VIEWER);

		assertThat(auth.canReadQuery(() -> "+custom@remote"))
			.isTrue();
	}

	@Test
	void testCanReadQuery_Private() {
		var user = getUser("+user/test@other");
		user.getTagReadAccess().add("_custom");
		var auth = getAuth(user, VIEWER);

		assertThat(auth.canReadQuery(() -> "_custom@other"))
			.isTrue();
	}

	@Test
	void testCanReadQuery_PrivateFailed() {
		var user = getUser("+user/test@other");
		var auth = getAuth(user, USER);

		assertThat(auth.canReadQuery(() -> "_custom@other"))
			.isFalse();
	}

	@Test
	void testCanReadQuery_PrivateRemote() {
		var user = getUser("+user/test@other");
		var auth = getAuth(user, VIEWER);
		auth.tagReadAccess = List.of(selector("_custom"));

		assertThat(auth.canReadQuery(() -> "_custom@remote"))
			.isTrue();
	}

	@Test
	void testCanReadQuery_PrivateRemoteFailed() {
		var user = getUser("+user/test@other");
		var auth = getAuth(user, VIEWER);

		assertThat(auth.canReadQuery(() -> "_custom@remote"))
			.isFalse();
	}

	@Test
	void testCanReadQuery_UserTag() {
		var user = getUser("_user/test@other");
		var auth = getAuth(user, VIEWER);

		assertThat(auth.canReadQuery(() -> "_user/test@other"))
			.isTrue();
	}

	@Test
	void testCanReadQuery_UserTagRemote() {
		var user = getUser("+user/test@other");
		var auth = getAuth(user, VIEWER);

		assertThat(auth.canReadQuery(() -> "+user/test@remote"))
			.isTrue();
	}

	@Test
	void testGetHiddenTags_Public() {
		var user = getUser("+user/test@other");
		var auth = getAuth(user, VIEWER);

		assertThat(auth.hiddenTags(List.of("custom")))
			.isEmpty();
	}

	@Test
	void testGetHiddenTags_Protected() {
		var user = getUser("+user/test@other");
		var auth = getAuth(user, VIEWER);

		assertThat(auth.hiddenTags(List.of("+custom")))
			.isEmpty();
	}

	@Test
	void testGetHiddenTags_PrivateTagReadAccess() {
		var user = getUser("+user/test@other");
		user.getTagReadAccess().add("_custom");
		var auth = getAuth(user, VIEWER);

		assertThat(auth.hiddenTags(List.of("_custom")))
			.isEmpty();
	}

	@Test
	void testGetHiddenTags_PrivateReadAccess() {
		var user = getUser("+user/test@other");
		var auth = getAuth(user, VIEWER);

		assertThat(auth.hiddenTags(List.of("_custom")))
			.isNotEmpty();
	}

	@Test
	void testGetHiddenTags_PrivateWriteAccessHidden() {
		var user = getUser("+user/test@other");
		user.getWriteAccess().add("_custom");
		var auth = getAuth(user, VIEWER);

		assertThat(auth.hiddenTags(List.of("_custom")))
			.contains("_custom");
	}

	@Test
	void testGetHiddenTags_PrivateHidden() {
		var user = getUser("+user/test@other");
		var auth = getAuth(user, VIEWER);

		assertThat(auth.hiddenTags(List.of("_custom")))
			.contains("_custom");
	}

	@Test
	void testCanWriteUser_AddPublicReadAccess() {
		var user = getUser("+user/test@other");
		user.getTagWriteAccess().add("+user/alice");
		user.getWriteAccess().add("custom");
		var auth = getAuth(user, USER);
		var alice = getUser("+user/alice@other");
		auth.configs = getConfigs(alice);

		var aliceMod = getUser("+user/alice@other");
		aliceMod.getReadAccess().add("custom");
		assertThat(auth.canWriteUser(aliceMod))
			.isTrue();
	}

	@Test
	void testCanWriteUser_AddPublicReadAccessFailed() {
		var user = getUser("+user/test@other");
		user.getTagWriteAccess().add("+user/alice");
		var auth = getAuth(user, USER);
		var alice = getUser("+user/alice@other");
		auth.configs = getConfigs(alice);

		var aliceMod = getUser("+user/alice@other");
		aliceMod.getReadAccess().add("custom");
		assertThat(auth.canWriteUser(aliceMod))
			.isFalse();
	}

	@Test
	void testCanWriteUser_AddPublicReadAccessFailedUser() {
		var user = getUser("+user/test@other");
		user.getWriteAccess().add("custom");
		var auth = getAuth(user, USER);
		var alice = getUser("+user/alice@other");
		auth.configs = getConfigs(alice);

		var aliceMod = getUser("+user/alice@other");
		aliceMod.getReadAccess().add("custom");
		assertThat(auth.canWriteUser(aliceMod))
			.isFalse();
	}

	@Test
	void testCanWriteUser_AddPublicWriteAccessFailed() {
		var user = getUser("+user/test@other");
		user.getTagWriteAccess().add("+user/alice");
		user.getWriteAccess().add("custom");
		var auth = getAuth(user, USER);
		var alice = getUser("+user/alice@other");
		auth.configs = getConfigs(alice);

		var aliceMod = getUser("+user/alice@other");
		aliceMod.getWriteAccess().add("custom");
		assertThat(auth.canWriteUser(aliceMod))
			.isFalse();
	}

	@Test
	void testCanWriteUser_AddProtectedReadAccess() {
		var user = getUser("+user/test@other");
		user.getTagWriteAccess().add("+user/alice");
		user.getWriteAccess().add("+custom");
		var auth = getAuth(user, USER);
		var alice = getUser("+user/alice@other");
		auth.configs = getConfigs(alice);

		var aliceMod = getUser("+user/alice@other");
		aliceMod.getReadAccess().add("+custom");
		assertThat(auth.canWriteUser(aliceMod))
			.isTrue();
	}

	@Test
	void testCanWriteUser_AddProtectedReadAccessFailed() {
		var user = getUser("+user/test@other");
		user.getTagWriteAccess().add("+user/alice");
		var auth = getAuth(user, USER);
		var alice = getUser("+user/alice@other");
		auth.configs = getConfigs(alice);

		var aliceMod = getUser("+user/alice@other");
		aliceMod.getReadAccess().add("+custom");
		assertThat(auth.canWriteUser(aliceMod))
			.isFalse();
	}

	@Test
	void testCanWriteUser_AddProtectedReadAccessFailedUser() {
		var user = getUser("+user/test@other");
		user.getWriteAccess().add("+custom");
		var auth = getAuth(user, USER);
		var alice = getUser("+user/alice@other");
		auth.configs = getConfigs(alice);

		var aliceMod = getUser("+user/alice@other");
		aliceMod.getReadAccess().add("+custom");
		assertThat(auth.canWriteUser(aliceMod))
			.isFalse();
	}

	@Test
	void testCanWriteUser_AddProtectedWriteAccess() {
		var user = getUser("+user/test@other");
		user.getTagWriteAccess().add("+user/alice");
		user.getWriteAccess().add("+custom");
		var auth = getAuth(user, USER);
		var alice = getUser("+user/alice@other");
		auth.configs = getConfigs(alice);

		var aliceMod = getUser("+user/alice@other");
		aliceMod.getWriteAccess().add("+custom");
		assertThat(auth.canWriteUser(aliceMod))
			.isTrue();
	}

	@Test
	void testCanWriteUser_AddProtectedWriteAccessFailed() {
		var user = getUser("+user/test@other");
		user.getTagWriteAccess().add("+user/alice");
		var auth = getAuth(user, USER);
		var alice = getUser("+user/alice@other");
		auth.configs = getConfigs(alice);

		var aliceMod = getUser("+user/alice@other");
		aliceMod.getWriteAccess().add("+custom");
		assertThat(auth.canWriteUser(aliceMod))
			.isFalse();
	}

	@Test
	void testCanWriteUser_AddPrivateReadAccess() {
		var user = getUser("+user/test@other");
		user.getTagWriteAccess().add("+user/alice");
		user.getWriteAccess().add("_custom");
		var auth = getAuth(user, USER);
		var alice = getUser("+user/alice@other");
		auth.configs = getConfigs(alice);

		var aliceMod = getUser("+user/alice@other");
		aliceMod.getReadAccess().add("_custom");
		assertThat(auth.canWriteUser(aliceMod))
			.isTrue();
	}

	@Test
	void testCanWriteUser_AddPrivateReadAccessFailed() {
		var user = getUser("+user/test@other");
		user.getTagWriteAccess().add("+user/alice");
		var auth = getAuth(user, USER);
		var alice = getUser("+user/alice@other");
		auth.configs = getConfigs(alice);

		var aliceMod = getUser("+user/alice@other");
		aliceMod.getReadAccess().add("_custom");
		assertThat(auth.canWriteUser(aliceMod))
			.isFalse();
	}

	@Test
	void testCanWriteUser_AddPrivateReadAccessFailedUser() {
		var user = getUser("+user/test@other");
		user.getWriteAccess().add("_custom");
		var auth = getAuth(user, USER);
		var alice = getUser("+user/alice@other");
		auth.configs = getConfigs(alice);

		var aliceMod = getUser("+user/alice@other");
		aliceMod.getReadAccess().add("_custom");
		assertThat(auth.canWriteUser(aliceMod))
			.isFalse();
	}

	@Test
	void testCanWriteUser_AddPrivateWriteAccess() {
		var user = getUser("+user/test@other");
		user.getTagWriteAccess().add("+user/alice");
		user.getWriteAccess().add("_custom");
		var auth = getAuth(user, USER);
		var alice = getUser("+user/alice@other");
		auth.configs = getConfigs(alice);

		var aliceMod = getUser("+user/alice@other");
		aliceMod.getWriteAccess().add("_custom");
		assertThat(auth.canWriteUser(aliceMod))
			.isTrue();
	}

	@Test
	void testCanWriteUser_AddPrivateWriteAccessFailed() {
		var user = getUser("+user/test@other");
		user.getTagWriteAccess().add("+user/alice");
		var auth = getAuth(user, USER);
		var alice = getUser("+user/alice@other");
		auth.configs = getConfigs(alice);

		var aliceMod = getUser("+user/alice@other");
		aliceMod.getWriteAccess().add("_custom");
		assertThat(auth.canWriteUser(aliceMod))
			.isFalse();
	}
}

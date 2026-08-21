package jasper.security;

/**
 * Constants for Spring Security authorities.
 */
public final class AuthoritiesConstants {

	public static final String ROLE_PREFIX = "ROLE_";
	public static final String ADMIN = "ROLE_ADMIN";
	public static final String MOD = "ROLE_MOD";
	public static final String EDITOR = "ROLE_EDITOR";
	public static final String USER = "ROLE_USER";
	public static final String VIEWER = "ROLE_VIEWER";
	public static final String ANONYMOUS = "ROLE_ANONYMOUS";
	public static final String BANNED = "ROLE_BANNED";
	public static final String PRIVATE = "ROLE_PRIVATE";

	private AuthoritiesConstants() {}
}

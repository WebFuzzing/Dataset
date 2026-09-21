package dev.mirodil.testing_system.models;

import dev.mirodil.testing_system.models.enums.PermissionType;
import dev.mirodil.testing_system.models.enums.UserGender;
import dev.mirodil.testing_system.models.enums.UserRole;
import dev.mirodil.testing_system.models.enums.UserStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

@Table("users")
public class User implements UserDetails {
    private static final Set<String> ALLOWED_SORT_ATTRIBUTES = Set.of(
            "userId", "email", "roleId", "fname", "lname", "status", "createdAt"
    );
    private static final Map<String, Class<?>> ALLOWED_FILTER_ATTRIBUTES = Map.of(
            "userId", String.class,
            "email", String.class,
            "roleId", Integer.class,
            "fname", String.class,
            "lname", String.class,
            "status", String.class,
            "createdAt", Instant.class
    );

    @Id
    private Long id;
    private String email;
    private String password;
    private Long roleId;
    private String fname;
    private String lname;
    private UserGender gender;
    private UserStatus status;
    private Instant createdAt;

    @Transient
    private Role role;
    @Transient
    private Set<PermissionType> permissionNames;

    public User() {
    }

    /**
     * User Constructor for shallow reading
     */
    public User(Long id, String email, String fname, String lname) {
        this.id = id;
        this.email = email;
        this.fname = fname;
        this.lname = lname;
    }

    /**
     * User Constructor for detailed reading
     */
    public User(Long id, String email, String password, Long roleId, String fname, String lname, UserGender gender, UserStatus status, Instant createdAt) {
        this(email, password, roleId, fname, lname, gender);
        this.id = id;
        this.status = status;
        this.createdAt = createdAt;
    }

    /**
     * User constructor only for creating a new user (with default values to userStatus & createdAt)
     */
    public User(String email, String password, Long roleId, String fname, String lname, UserGender gender) {
        this.email = email;
        this.password = password;
        this.roleId = roleId;
        this.fname = fname;
        this.lname = lname;
        this.gender = gender;
        this.status = UserStatus.ACTIVE;
        this.createdAt = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    }

    public static Set<String> getAllowedSortAttributes() {
        return ALLOWED_SORT_ATTRIBUTES;
    }

    public static Map<String, Class<?>> getAllowedFilterAttributes() {
        return ALLOWED_FILTER_ATTRIBUTES;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    public Role getUserRole() {
        return role;
    }

    public void setUserRole(Role role) {
        this.role = role;
    }

    public UserRole getUserRoleName() {
        if (role == null) {
            return null;
        }

        return role.getName();
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(long roleId) {
        this.roleId = roleId;
    }

    public boolean isAdmin() {
        return role.getName() == UserRole.ADMIN;
    }

    public Set<PermissionType> getPermissionNames() {
        return permissionNames;
    }

    public void setPermissionNames(Set<PermissionType> permissionNames) {
        this.permissionNames = permissionNames;
    }

    public String getFirstName() {
        return fname;
    }

    public String getLastName() {
        return lname;
    }

    public UserGender getGender() {
        return gender;
    }

    public void setGender(UserGender gender) {
        this.gender = gender;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setUserStatus(UserStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Collection<SimpleGrantedAuthority> grantedAuthorities = new ArrayList<>();
        // Add the user's role as a GrantedAuthority
        grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName().toString().toUpperCase()));
        // Add permissions associated with the role
        permissionNames.forEach(permission ->
                grantedAuthorities.add(new SimpleGrantedAuthority(permission.name()))
        );

        return grantedAuthorities;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status != UserStatus.BLOCKED;
    }

    @Override
    public boolean isEnabled() {
        return isAccountNonLocked();
    }
}

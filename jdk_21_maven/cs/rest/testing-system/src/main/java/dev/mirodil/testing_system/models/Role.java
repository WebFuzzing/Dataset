package dev.mirodil.testing_system.models;

import dev.mirodil.testing_system.models.enums.PermissionType;
import dev.mirodil.testing_system.models.enums.UserRole;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;

import java.util.Set;
import java.util.stream.Collectors;

@Table("roles")
public class Role {
    @Id
    private Long id;
    private UserRole name;

    @Transient
    private Set<Permission> permissions;

    public Role() {
    }

    public Role(Long id, UserRole name) {
        this.id = id;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UserRole getName() {
        return name;
    }

    public void setName(UserRole name) {
        this.name = name;
    }

    public Set<Permission> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<Permission> permissions) {
        this.permissions = permissions;
    }

    public boolean isPermissionsSet() {
        return permissions != null;
    }

    public Set<PermissionType> getPermissionNames() {
        return permissions.stream()
                .map(Permission::getName)
                .collect(Collectors.toSet());
    }

    @Override
    public String toString() {
        return "Role{" +
                "id=" + id +
                ", name=" + name +
                ", permissions=" + permissions +
                '}';
    }
}

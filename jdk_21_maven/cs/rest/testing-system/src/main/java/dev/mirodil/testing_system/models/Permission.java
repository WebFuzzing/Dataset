package dev.mirodil.testing_system.models;

import dev.mirodil.testing_system.models.enums.PermissionType;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("permissions")
public class Permission {
    @Id
    private Long id;
    private PermissionType name;
    private String description;

    public Permission(PermissionType name) {
        this.name = name;
    }

    public Permission(Long id, PermissionType name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PermissionType getName() {
        return name;
    }

    public void setName(PermissionType name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "Permission{" +
                "id=" + id +
                ", name=" + name +
                ", description='" + description + '\'' +
                '}';
    }
}

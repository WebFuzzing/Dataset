package dev.mirodil.testing_system.repositories;

import dev.mirodil.testing_system.models.Role;
import dev.mirodil.testing_system.models.enums.UserRole;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface RoleRepository extends CrudRepository<Role, Long> {
    String BASE_QUERY = """
                SELECT r.id AS role_id, r.name AS role_name, p.id AS permission_id, p.name AS permission_name, p.description AS permission_description
                FROM roles r
                LEFT JOIN role_permissions rp ON r.id = rp.role_id
                LEFT JOIN permissions p ON rp.permission_id = p.id
            """;

    @Query(
            value = BASE_QUERY + " WHERE r.name = :name",
            resultSetExtractorRef = "roleWithPermissionResultSetExtractor"
    )
    Optional<Role> findRoleWithPermissionsByName(UserRole name);
}

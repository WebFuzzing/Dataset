package dev.mirodil.testing_system.repositories;

import dev.mirodil.testing_system.models.User;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface UserRepository extends CrudRepository<User, Long>, CustomUserRepository {
    String BASE_QUERY = """
            SELECT users.id AS user_id, email, password, fname, lname, gender, status, created_at,
                   roles.id AS role_id, roles.name AS role_name,
                   array_agg(p.name) FILTER (WHERE p.name IS NOT NULL) AS permission_names
            FROM users
            LEFT JOIN roles ON users.role_id = roles.id
            LEFT JOIN role_permissions rp ON roles.id = rp.role_id
            LEFT JOIN permissions p ON rp.permission_id = p.id
            """;
    String GROUP_BY_CLAUSE = " GROUP BY users.id, roles.id";

    @Query(value = BASE_QUERY + " WHERE users.email = :email" + GROUP_BY_CLAUSE,
            resultSetExtractorRef = "userWithPermissionResultSetExtractor")
    Optional<User> findUserByEmail(String email);

    @Query(value = BASE_QUERY + " WHERE users.id = :id" + GROUP_BY_CLAUSE,
            resultSetExtractorRef = "userWithPermissionResultSetExtractor")
    Optional<User> findUserById(Long id);
}

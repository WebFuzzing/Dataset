package com.example.arimaabackend.repository.sql;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.arimaabackend.model.sql.UserEntity;

public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {

    // Hibernate/JPA: Spring Data looks at the method name (findByEmail),
    // matches 'email' to the 'email' field on UserEntity, and generates
    // a JPQL query like: select u from UserEntity u where u.email = :email.
    // JPA then maps that to real SQL against the 'users' table.
    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByUsername(String username);

    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    // JOQL SQL @Query example. Uses UserEntity as table reference
    @Query("select u from UserEntity u where lower(u.email) like lower(concat('%', :part, '%'))")
    List<UserEntity> searchByEmailPart(@Param("part") String part);

    // Native SQL @Query example. Uses the real table name (users).
    @Query(
            value = "select * from users u where u.username like concat(:prefix, '%')",
            nativeQuery = true
    )
    List<UserEntity> findByUsernameStartingWith(@Param("prefix") String prefix);
}


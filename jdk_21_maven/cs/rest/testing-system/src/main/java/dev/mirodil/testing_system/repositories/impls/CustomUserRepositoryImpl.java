package dev.mirodil.testing_system.repositories.impls;

import dev.mirodil.testing_system.models.User;
import dev.mirodil.testing_system.repositories.CustomUserRepository;
import dev.mirodil.testing_system.repositories.GenericRepositoryWithPagination;
import dev.mirodil.testing_system.utils.PageWithFilterRequest;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class CustomUserRepositoryImpl implements CustomUserRepository {
    private final RowMapper<User> userRowMapper;
    private final GenericRepositoryWithPagination<User> paginationRepository;

    public CustomUserRepositoryImpl(RowMapper<User> userRowMapper, GenericRepositoryWithPagination<User> paginationRepository) {
        this.userRowMapper = userRowMapper;
        this.paginationRepository = paginationRepository;
    }

    @Override
    public List<User> findAndSortUsersWithPagination(PageWithFilterRequest pageable) {
        return paginationRepository.findAndSortModelWithPagination(pageable, getBaseQuery(), userRowMapper);
    }

    @Override
    public long countFilteredUsers(PageWithFilterRequest pageable) {
        return paginationRepository.countFilteredModel(pageable, getCountBaseQuery());
    }

    private StringBuilder getBaseQuery() {
        return new StringBuilder("""
                SELECT users.id AS user_id, email, password, fname, lname, gender, status, created_at,
                       roles.id AS role_id, roles.name AS role_name
                FROM users
                LEFT JOIN roles ON users.role_id = roles.id
                """
        );
    }

    private StringBuilder getCountBaseQuery() {
        return new StringBuilder("SELECT count(*) FROM users");
    }
}
package dev.mirodil.testing_system.repositories;

import dev.mirodil.testing_system.models.User;
import dev.mirodil.testing_system.utils.PageWithFilterRequest;

import java.util.List;

public interface CustomUserRepository {
    List<User> findAndSortUsersWithPagination(PageWithFilterRequest pageable);

    long countFilteredUsers(PageWithFilterRequest pageable);
}
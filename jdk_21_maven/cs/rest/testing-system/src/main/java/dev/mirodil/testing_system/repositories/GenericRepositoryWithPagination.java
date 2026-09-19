package dev.mirodil.testing_system.repositories;

import dev.mirodil.testing_system.utils.PageWithFilterRequest;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;

public interface GenericRepositoryWithPagination<T> {
    List<T> findAndSortModelWithPagination(PageWithFilterRequest pageable, StringBuilder queryBuilder, RowMapper<T> rowMapper);

    long countFilteredModel(PageWithFilterRequest pageable, StringBuilder queryBuilder);
}

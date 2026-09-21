package dev.mirodil.testing_system.repositories.impls;

import dev.mirodil.testing_system.repositories.GenericRepositoryWithPagination;
import dev.mirodil.testing_system.utils.FilterCriteria;
import dev.mirodil.testing_system.utils.PageWithFilterRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

import static dev.mirodil.testing_system.utils.DataUtil.appendOrderByClause;
import static dev.mirodil.testing_system.utils.DataUtil.appendWhereClause;

@Repository
public class GenericRepositoryWithPaginationImpl<T> implements GenericRepositoryWithPagination<T> {
    private final JdbcTemplate jdbcTemplate;

    public GenericRepositoryWithPaginationImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<T> findAndSortModelWithPagination(PageWithFilterRequest pageable, StringBuilder queryBuilder, RowMapper<T> rowMapper) {
        // Add filters dynamically
        List<FilterCriteria<?>> filters = pageable.getFilters();
        List<Object> queryParams = new ArrayList<>(
                appendWhereClause(queryBuilder, filters)
        );

        // Add sorting dynamically
        Sort sort = pageable.getSort();
        appendOrderByClause(queryBuilder, sort);

        // Add pagination
        queryBuilder.append(" LIMIT ? OFFSET ?");
        queryParams.add(pageable.getPageSize());
        queryParams.add(pageable.getOffset());

        // Execute the query
        return jdbcTemplate.query(
                queryBuilder.toString(),
                rowMapper,
                queryParams.toArray()
        );
    }

    @Override
    public long countFilteredModel(PageWithFilterRequest pageable, StringBuilder queryBuilder) {
        // Add filters dynamically
        List<FilterCriteria<?>> filters = pageable.getFilters();
        List<Object> queryParams = new ArrayList<>(
                appendWhereClause(queryBuilder, filters)
        );

        Long count = jdbcTemplate.queryForObject(
                queryBuilder.toString(),
                Long.class,
                queryParams.toArray()
        );
        return count != null ? count : 0L;
    }
}

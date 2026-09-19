package dev.mirodil.testing_system.repositories.impls;

import dev.mirodil.testing_system.models.TestEvent;
import dev.mirodil.testing_system.repositories.CustomTestEventRepository;
import dev.mirodil.testing_system.repositories.GenericRepositoryWithPagination;
import dev.mirodil.testing_system.utils.PageWithFilterRequest;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;

public class CustomTestEventRepositoryImpl implements CustomTestEventRepository {
    private final RowMapper<TestEvent> testEventRowMapper;
    private final GenericRepositoryWithPagination<TestEvent> paginationRepository;

    public CustomTestEventRepositoryImpl(RowMapper<TestEvent> testEventRowMapper, GenericRepositoryWithPagination<TestEvent> paginationRepository) {
        this.testEventRowMapper = testEventRowMapper;
        this.paginationRepository = paginationRepository;
    }

    @Override
    public List<TestEvent> findAndSortTestEventsWithPagination(PageWithFilterRequest pageable) {
        return paginationRepository.findAndSortModelWithPagination(pageable, getBaseQuery(), testEventRowMapper);
    }

    @Override
    public long countFilteredTestEvents(PageWithFilterRequest pageable) {
        return paginationRepository.countFilteredModel(pageable, getCountBaseQuery());
    }

    private StringBuilder getBaseQuery() {
        return new StringBuilder("""
                SELECT test_event_id, event_datetime, test_event_status,
                      score_points, score_percentage, is_passed, started_at, finished_at, test_event_created_at,
                      test_taker_id, email, fname, lname,
                      test_id, title, duration, no_of_questions, passing_percentage
                FROM test_events_view
                """);
    }

    private StringBuilder getCountBaseQuery() {
        return new StringBuilder("SELECT count(*) FROM test_events_view");
    }
}

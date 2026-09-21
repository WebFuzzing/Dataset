package dev.mirodil.testing_system.repositories;

import dev.mirodil.testing_system.models.TestEvent;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface TestEventRepository extends CrudRepository<TestEvent, Long>, CustomTestEventRepository {
    String GET_DETAILED_BASE_QUERY = "SELECT * FROM test_events_view";

    @Query(value = GET_DETAILED_BASE_QUERY + " WHERE test_event_id = :id",
            rowMapperRef = "testEventRowMapper")
    Optional<TestEvent> getTestEventsById(Long id);
}

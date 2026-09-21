package dev.mirodil.testing_system.repositories;

import dev.mirodil.testing_system.models.TestEvent;
import dev.mirodil.testing_system.utils.PageWithFilterRequest;

import java.util.List;

public interface CustomTestEventRepository {
    List<TestEvent> findAndSortTestEventsWithPagination(PageWithFilterRequest pageable);

    long countFilteredTestEvents(PageWithFilterRequest pageable);
}

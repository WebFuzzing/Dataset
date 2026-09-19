package dev.mirodil.testing_system.services;

import dev.mirodil.testing_system.dtos.PagedResponse;
import dev.mirodil.testing_system.dtos.TestEventResponseDTO;
import dev.mirodil.testing_system.exceptions.ResourceNotFoundException;
import dev.mirodil.testing_system.models.TestEvent;
import dev.mirodil.testing_system.repositories.TestEventRepository;
import dev.mirodil.testing_system.utils.PageWithFilterRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TestEventService {
    private final TestEventRepository testEventRepository;

    public TestEventService(TestEventRepository testEventRepository) {
        this.testEventRepository = testEventRepository;
    }

    public PagedResponse<TestEventResponseDTO> getTestEvents(PageWithFilterRequest pageable, String fullUrl) {
        List<TestEvent> testEvents = testEventRepository.findAndSortTestEventsWithPagination(pageable);
        long totalElements = testEventRepository.countFilteredTestEvents(pageable);

        List<TestEventResponseDTO> testEventsDTO = testEvents.stream()
                .map(TestEventResponseDTO::new)
                .toList();
        Page<TestEventResponseDTO> page = new PageImpl<>(testEventsDTO, pageable, totalElements);

        return new PagedResponse<>(page, fullUrl);
    }

    public TestEventResponseDTO getTestEventById(Long id) {
        TestEvent testEvent = testEventRepository.getTestEventsById(id).orElseThrow(
                () -> new ResourceNotFoundException("Test event not found with id: " + id)
        );
        return new TestEventResponseDTO(testEvent);
    }
}

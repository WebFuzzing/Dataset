package dev.mirodil.testing_system.controllers;

import dev.mirodil.testing_system.dtos.PagedResponse;
import dev.mirodil.testing_system.dtos.TestEventResponseDTO;
import dev.mirodil.testing_system.dtos.WrapResponseWithContentKey;
import dev.mirodil.testing_system.services.TestEventService;
import dev.mirodil.testing_system.utils.AppUtil;
import dev.mirodil.testing_system.utils.PageWithFilterRequest;
import dev.mirodil.testing_system.validations.ValidTestEventPageRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class TestEventController {
    private final TestEventService testEventService;

    public TestEventController(TestEventService testEventService) {
        this.testEventService = testEventService;
    }

    @GetMapping("/test-events")
    public PagedResponse<TestEventResponseDTO> getAllTestEvents(@ValidTestEventPageRequest PageWithFilterRequest pageable, HttpServletRequest request) {
        String fullUrl = AppUtil.getUrlWithQueryParams(request);
        return testEventService.getTestEvents(pageable, fullUrl);
    }

    @GetMapping("/test-events/{id}")
    public WrapResponseWithContentKey<?> getTestEventById(@PathVariable Long id) {
        TestEventResponseDTO testEventDTO = testEventService.getTestEventById(id);
        return new WrapResponseWithContentKey<>(testEventDTO);
    }

    @GetMapping("/test-events/{id}/test-attempt")
    public WrapResponseWithContentKey<?> getTestAttempt(@PathVariable Long id) {
        // TODO: implement
        return new WrapResponseWithContentKey<>(null);
    }
}

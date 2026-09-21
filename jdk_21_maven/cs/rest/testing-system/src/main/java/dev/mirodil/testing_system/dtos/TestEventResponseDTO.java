package dev.mirodil.testing_system.dtos;

import dev.mirodil.testing_system.controllers.TestEventController;
import dev.mirodil.testing_system.models.TestEvent;
import dev.mirodil.testing_system.models.enums.TestEventStatus;

import java.net.URI;
import java.time.Instant;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

public record TestEventResponseDTO(
        Long testEventId,
        UserResponseDTO testTaker,
        TestResponseDTO test,
        Instant eventDatetime,
        TestEventStatus status,
        Float scorePoints,
        Integer scorePercentage,
        Boolean isPassed,
        Instant startedAt,
        Instant finishedAt,
        Instant createdAt,
        // TODO: create separate TestAttemptDTO (with detailed questions and their options)
        URI path
) {
    public TestEventResponseDTO(TestEvent testEvent) {
        this(
                testEvent.getId(),
                new UserResponseDTO(testEvent.getTestTaker()),
                new TestResponseDTO(testEvent.getTest()),
                testEvent.getEventDateTime(),
                testEvent.getStatus(),
                testEvent.getScorePoints(),
                testEvent.getScorePercentage(),
                testEvent.getIsPassed(),
                testEvent.getStartedAt(),
                testEvent.getFinishedAt(),
                testEvent.getCreatedAt(),
                linkTo(methodOn(TestEventController.class).getTestEventById(testEvent.getId())).toUri()
        );
    }
}

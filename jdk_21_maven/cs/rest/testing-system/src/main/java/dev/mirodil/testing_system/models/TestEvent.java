package dev.mirodil.testing_system.models;

import dev.mirodil.testing_system.models.enums.TestEventStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Table("test_events")
public class TestEvent {
    private static final Set<String> ALLOWED_SORT_ATTRIBUTES = Set.of(
            "testEventId", "testTakerId", "testId", "eventDatetime", "testEventStatus", "scorePercentage", "testEventCreatedAt",
            "email", "fname", "lname", "title"
    );
    private static final Map<String, Class<?>> ALLOWED_FILTER_ATTRIBUTES = Map.of(
            "eventDatetime", Instant.class,
            "testEventStatus", String.class,
            "scorePercentage", Integer.class,
            "isPassed", Boolean.class,
            "email", String.class,
            "fname", String.class,
            "lname", String.class,
            "title", String.class
    );

    @Id
    private Long id;
    private Long testTakerId;
    private Long testId;
    @Column("event_datetime")
    private Instant eventDateTime;
    private TestEventStatus status;
    private Float scorePoints = null;
    private Integer scorePercentage = null;
    private Boolean isPassed = null;
    private Instant startedAt;
    private Instant finishedAt;
    private Instant createdAt;
    private List<TestAttempt> testAttempt;
    @Transient
    private User testTaker;
    @Transient
    private Test test;

    public TestEvent() {
    }

    public TestEvent(Long testTakerId, Long testId, Instant eventDateTime, TestEventStatus status) {
        this.testTakerId = testTakerId;
        this.testId = testId;
        this.eventDateTime = eventDateTime;
        this.status = status;
    }

    public TestEvent(Long id, Long testTakerId, Long testId, Instant eventDateTime, TestEventStatus status, Float scorePoints, Integer scorePercentage, Boolean isPassed, Instant startedAt, Instant finishedAt, Instant createdAt) {
        this(testTakerId, testId, eventDateTime, status);
        this.id = id;
        this.scorePoints = scorePoints;
        this.scorePercentage = scorePercentage;
        this.isPassed = isPassed;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.createdAt = createdAt;
    }

    public static Set<String> getAllowedSortAttributes() {
        return ALLOWED_SORT_ATTRIBUTES;
    }

    public static Map<String, Class<?>> getAllowedFilterAttributes() {
        return ALLOWED_FILTER_ATTRIBUTES;
    }

    public Long getId() {
        return id;
    }

    public Long getTestTakerId() {
        return testTakerId;
    }

    public Long getTestId() {
        return testId;
    }

    public Instant getEventDateTime() {
        return eventDateTime;
    }

    public TestEventStatus getStatus() {
        return status;
    }

    public void setStatus(TestEventStatus status) {
        this.status = status;
    }

    public Float getScorePoints() {
        return scorePoints;
    }

    public void setScorePoints(Float scorePoints) {
        this.scorePoints = scorePoints;
    }

    public Integer getScorePercentage() {
        return scorePercentage;
    }

    public void setScorePercentage(Integer scorePercentage) {
        this.scorePercentage = scorePercentage;
    }

    public Boolean getIsPassed() {
        return isPassed;
    }

    public void setIsPassed(Boolean isPassed) {
        this.isPassed = isPassed;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<TestAttempt> getTestAttempt() {
        return testAttempt;
    }

    public void setTestAttempt(List<TestAttempt> testAttempt) {
        this.testAttempt = testAttempt;
    }

    public User getTestTaker() {
        return testTaker;
    }

    public void setTestTaker(User testTaker) {
        this.testTaker = testTaker;
    }

    public Test getTest() {
        return test;
    }

    public void setTest(Test test) {
        this.test = test;
    }
}

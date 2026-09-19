package dev.mirodil.testing_system.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Table("tests")
public class Test {
    @Id
    private Long id;
    private String title;
    private String description;
    private int duration;
    private int noOfQuestions;
    private int passingPercentage;
    private Boolean shouldShuffle;
    private Boolean shouldRandomlyPick;
    private Instant deleted_at;

    public Test() {
    }

    /**
     * Test Constructor for shallow reading
     */
    public Test(Long id, String title, int duration, int noOfQuestions, int passingPercentage) {
        this.id = id;
        this.title = title;
        this.duration = duration;
        this.noOfQuestions = noOfQuestions;
        this.passingPercentage = passingPercentage;
    }

    /**
     * Test Constructor for detailed reading
     */
    public Test(Long id, String title, String description, int duration, int noOfQuestions, int passingPercentage, Boolean shouldShuffle, Boolean shouldRandomlyPick, Instant deleted_at) {
        this(
                title,
                description,
                duration,
                noOfQuestions,
                passingPercentage,
                shouldShuffle,
                shouldRandomlyPick
        );
        this.id = id;
        this.deleted_at = deleted_at;
    }

    /**
     * Test constructor for creating a new test
     */
    public Test(String title, String description, int duration, int noOfQuestions, int passingPercentage, Boolean shouldShuffle, Boolean shouldRandomlyPick) {
        this.title = title;
        this.description = description;
        this.duration = duration;
        this.noOfQuestions = noOfQuestions;
        this.passingPercentage = passingPercentage;
        this.shouldShuffle = shouldShuffle;
        this.shouldRandomlyPick = shouldRandomlyPick;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getDuration() {
        return duration;
    }

    public int getNoOfQuestions() {
        return noOfQuestions;
    }

    public int getPassingPercentage() {
        return passingPercentage;
    }

    public Boolean shouldShuffle() {
        return shouldShuffle;
    }

    public void setShouldShuffle(Boolean shouldShuffle) {
        this.shouldShuffle = shouldShuffle;
    }

    public Boolean shouldRandomlyPick() {
        return shouldRandomlyPick;
    }

    public void setShouldRandomlyPick(Boolean shouldRandomlyPick) {
        this.shouldRandomlyPick = shouldRandomlyPick;
    }

    public Instant getDeletedAt() {
        return deleted_at;
    }

    public void setDeletedAt(Instant deleted_at) {
        this.deleted_at = deleted_at;
    }
}

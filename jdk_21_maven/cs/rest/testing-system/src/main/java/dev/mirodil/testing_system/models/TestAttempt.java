package dev.mirodil.testing_system.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TestAttempt {
    @JsonProperty("question_id")
    private Long questionId;

    @JsonProperty("selected_option_id")
    private Long selectedOptionId;

    @JsonProperty("is_correct")
    private boolean isCorrect;

    public TestAttempt() {
    }

    public TestAttempt(Long questionId, Long selectedOptionId, boolean isCorrect) {
        this.questionId = questionId;
        this.selectedOptionId = selectedOptionId;
        this.isCorrect = isCorrect;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public Long getSelectedOptionId() {
        return selectedOptionId;
    }

    public void setSelectedOptionId(Long selectedOptionId) {
        this.selectedOptionId = selectedOptionId;
    }

    public boolean getIsCorrect() {
        return isCorrect;
    }

    public void setIsCorrect(boolean correct) {
        isCorrect = correct;
    }
}

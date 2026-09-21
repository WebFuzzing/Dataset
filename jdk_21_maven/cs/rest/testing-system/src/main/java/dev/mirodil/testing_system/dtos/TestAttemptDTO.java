package dev.mirodil.testing_system.dtos;

public record TestAttemptDTO(
//        QuestionResponseDTO question,
        Long selectedOptionId,
        boolean isCorrect
) {
}

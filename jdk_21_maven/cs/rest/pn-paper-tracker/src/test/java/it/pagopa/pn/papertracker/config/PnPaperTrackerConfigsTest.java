package it.pagopa.pn.papertracker.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PnPaperTrackerConfigsTest {

    private PnPaperTrackerConfigs configs;

    @BeforeEach
    void setup() {
        configs = new PnPaperTrackerConfigs();
    }

    @Test
    void validateSendOcrAttachmentsFinalValidationStock890_withCorrectValue_doesNotThrow() {
        // Arrange
        configs.setSendOcrAttachmentsFinalValidationStock890(List.of("1970-01-01;"));

        // Act & Assert: chiamo il metodo e non mi aspetto eccezioni
        configs.validateSendOcrAttachmentsFinalValidationStock890();
    }

    @Test
    void validateSendOcrAttachmentsFinalValidationStock890_withWrongValue_throwsException() {
        // Arrange
        configs.setSendOcrAttachmentsFinalValidationStock890(List.of("1970-01-01;ARCAD"));

        // Act & Assert
        assertThatThrownBy(() -> configs.validateSendOcrAttachmentsFinalValidationStock890())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void validateSendOcrAttachmentsFinalValidationStock890_withEmptyList_throwsException() {
        // Arrange
        configs.setSendOcrAttachmentsFinalValidationStock890(List.of());

        // Act & Assert
        assertThatThrownBy(() -> configs.validateSendOcrAttachmentsFinalValidationStock890())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void validateSendOcrAttachmentsFinalValidationStock890_withPartialMatch_throwsException() {
        // Arrange
        configs.setSendOcrAttachmentsFinalValidationStock890(List.of("1970-01-01")); // senza il ';'

        // Act & Assert
        assertThatThrownBy(() -> configs.validateSendOcrAttachmentsFinalValidationStock890())
                .isInstanceOf(IllegalStateException.class);
    }
}

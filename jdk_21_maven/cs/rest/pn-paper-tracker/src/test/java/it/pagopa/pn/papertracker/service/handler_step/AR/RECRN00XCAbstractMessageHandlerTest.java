package it.pagopa.pn.papertracker.service.handler_step.AR;

import it.pagopa.pn.papertracker.config.PnPaperTrackerConfigs;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RECRN00XCAbstractMessageHandlerTest {

    @InjectMocks
    private FinalEventBuilderAr finalEventBuilder;

    @Mock
    private PnPaperTrackerConfigs pnPaperTrackerConfigs;

    @Test
    void testGetDurationBetweenDates_NoTruncate() {
        when(pnPaperTrackerConfigs.isEnableTruncatedDateForRefinementCheck()).thenReturn(false);
        // Arrange
        var instant1 = Instant.parse("2025-03-05T09:00:00Z");
        var instant2 = Instant.parse("2025-03-06T08:00:00Z");

        // Act
        Duration duration = ReflectionTestUtils.invokeMethod(
                finalEventBuilder,
                "getDurationBetweenDates",
                instant1,
                instant2
        );

        // Assert
        assertThat(duration).isEqualTo(Duration.ofHours(23));
    }

    @Test
    void testGetDurationBetweenDates_Truncate() {
        when(pnPaperTrackerConfigs.isEnableTruncatedDateForRefinementCheck()).thenReturn(true);

        var instant1 = Instant.parse("2025-03-05T09:00:00Z");
        var instant2 = Instant.parse("2025-03-06T08:00:00Z");

        // Act
        Duration duration = ReflectionTestUtils.invokeMethod(
                finalEventBuilder,
                "getDurationBetweenDates",
                instant1,
                instant2
        );

        // Assert
        assertThat(duration).isEqualTo(Duration.ofDays(1));
    }

    @Test
    void testIsDifferenceGreaterRefinementDuration() {
        when(pnPaperTrackerConfigs.getRefinementDuration()).thenReturn(Duration.ofDays(10));
        when(pnPaperTrackerConfigs.isEnableTruncatedDateForRefinementCheck()).thenReturn(false);
        // Arrange
        Instant now = Instant.now();
        Instant nineDaysAfter = now.plus(Duration.ofDays(9));
        Instant tenDaysAfter = now.plus(Duration.ofDays(10));
        Instant elevenDaysAfter = now.plus(Duration.ofDays(11));

        // Act
        Boolean result9 = ReflectionTestUtils.invokeMethod(
                finalEventBuilder,
                "isDifferenceGreaterRefinementDuration",
                now,
                nineDaysAfter
        );
        Boolean result10 = ReflectionTestUtils.invokeMethod(
                finalEventBuilder,
                "isDifferenceGreaterRefinementDuration",
                now,
                tenDaysAfter
        );
        Boolean result11 = ReflectionTestUtils.invokeMethod(
                finalEventBuilder,
                "isDifferenceGreaterRefinementDuration",
                now,
                elevenDaysAfter
        );

        // Assert
        assertThat(result9).isFalse();
        assertThat(result10).isFalse();
        assertThat(result11).isTrue();
    }

    @Test
    void testIsDifferenceGreaterOrEqualToStockDuration() {
        when(pnPaperTrackerConfigs.getCompiutaGiacenzaArDuration()).thenReturn(Duration.ofDays(30));
        when(pnPaperTrackerConfigs.isEnableTruncatedDateForRefinementCheck()).thenReturn(false);
        // Arrange
        Instant now = Instant.now();
        Instant twentyNineDays = now.plus(Duration.ofDays(29));
        Instant thirtyDays = now.plus(Duration.ofDays(30));
        Instant thirtyOneDays = now.plus(Duration.ofDays(31));

        // Act
        Boolean result29 = ReflectionTestUtils.invokeMethod(
                finalEventBuilder,
                "isDifferenceGreaterOrEqualToStockDuration",
                now,
                twentyNineDays
        );
        Boolean result30 = ReflectionTestUtils.invokeMethod(
                finalEventBuilder,
                "isDifferenceGreaterOrEqualToStockDuration",
                now,
                thirtyDays
        );
        Boolean result31 = ReflectionTestUtils.invokeMethod(
                finalEventBuilder,
                "isDifferenceGreaterOrEqualToStockDuration",
                now,
                thirtyOneDays
        );

        // Assert
        assertThat(result29).isFalse();
        assertThat(result30).isTrue();
        assertThat(result31).isTrue();
    }

    @Test
    void addDuration_when_truncateDisabled_addsFullDuration() {
        when(pnPaperTrackerConfigs.isEnableTruncatedDateForRefinementCheck()).thenReturn(false);

        Instant base   = Instant.parse("2023-01-15T10:00:00Z");
        Duration delta = Duration.ofHours(5);

        // Act
        Instant result = ReflectionTestUtils.invokeMethod(
                finalEventBuilder,
                "addDurationToInstant",
                base,
                delta
        );

        // Assert
        assertEquals(base.plus(delta), result);
    }

    // Si considerano solo i "giorni civili" in Europe/Rome.
    @Test
    void addDuration_when_truncateEnabled_zeroDays() {
        // Arrange
        when(pnPaperTrackerConfigs.isEnableTruncatedDateForRefinementCheck()).thenReturn(true);

        Instant base   = Instant.parse("2023-01-15T10:00:00Z");
        Duration delta = Duration.ofHours(5);          // <24h = 0giorni

        // Inizio del giorno 2023-01-15 in UTC
        Instant expected = Instant.parse("2023-01-14T23:00:00Z");

        // Act
        Instant result = ReflectionTestUtils.invokeMethod(
                finalEventBuilder,
                "addDurationToInstant",
                base,
                delta
        );

        // Assert
        assertEquals(expected, result);
    }

    // Si considerano solo i "giorni civili" in Europe/Rome.
    @Test
    void addDuration_when_truncateEnabled_usesWholeDays() {
        // Arrange
        when(pnPaperTrackerConfigs.isEnableTruncatedDateForRefinementCheck()).thenReturn(true);

        Instant base   = Instant.parse("2023-01-15T10:00:00Z");
        Duration delta = Duration.ofDays(5);

        // Inizio del giorno 2023-01-20 in UTC
        Instant expected = Instant.parse("2023-01-19T23:00:00Z");

        // Act
        Instant result = ReflectionTestUtils.invokeMethod(
                finalEventBuilder,
                "addDurationToInstant",
                base,
                delta
        );

        // Assert
        assertEquals(expected, result);
    }

    // Salto DST: 1 giorno prima dell’ora legale → mezzanotte locale corretta.
    @Test
    void addDuration_when_truncateEnabled_handlesDstGap() {
        // Arrange
        when(pnPaperTrackerConfigs.isEnableTruncatedDateForRefinementCheck()).thenReturn(true);

        Instant base   = Instant.parse("2025-03-29T12:00:00Z"); // vigilia cambio DST
        Duration delta = Duration.ofDays(1); // +1 giorno civile

        // Inizio del giorno 2025-03-30 in UTC
        Instant expected = Instant.parse("2025-03-29T23:00:00Z");

        // Act
        Instant result = ReflectionTestUtils.invokeMethod(
                finalEventBuilder,
                "addDurationToInstant",
                base,
                delta
        );

        // Assert
        assertEquals(expected, result);
    }

    @Test
    void getDuration_when_truncateDisabled_returns24h() {
        // Arrange
        when(pnPaperTrackerConfigs.isEnableTruncatedDateForRefinementCheck()).thenReturn(false);

        Instant i1 = Instant.parse("2023-02-01T12:00:00Z");
        Instant i2 = Instant.parse("2023-02-02T12:00:00Z");

        // Act
        Duration result = ReflectionTestUtils.invokeMethod(
                finalEventBuilder,
                "getDurationBetweenDates",
                i1,
                i2
        );

        // Assert
        assertEquals(Duration.ofHours(24), result);
    }

   //Salto avanti (primavera): notte di 23 ore -> PT23H.

    @Test
    void getDuration_when_truncateDisabled_dstSpringForward_returns23h() {
        // Arrange
        when(pnPaperTrackerConfigs.isEnableTruncatedDateForRefinementCheck()).thenReturn(false);

        // 2025-03-30T00:00 Europe/Rome (UTC‑01:00)
        Instant i1 = Instant.parse("2025-03-29T23:00:00Z");
        // 2025-03-31T00:00 Europe/Rome (UTC‑02:00)
        Instant i2 = Instant.parse("2025-03-30T22:00:00Z");

        // Act
        Duration result = ReflectionTestUtils.invokeMethod(
                finalEventBuilder,
                "getDurationBetweenDates",
                i1,
                i2
        );

        // Assert
        assertEquals(Duration.ofHours(23), result);
    }

    //Ritorno all’ora solare (autunno): notte di 25 ore -> PT25H.

    @Test
    void getDuration_when_truncateDisabled_dstFallBack_returns25h() {
        // Arrange
        when(pnPaperTrackerConfigs.isEnableTruncatedDateForRefinementCheck()).thenReturn(false);

        // 2025‑10‑25T00:00 Europe/Rome (UTC‑02:00)
        Instant i1 = Instant.parse("2025-10-24T22:00:00Z");
        // 2025‑10‑26T00:00 Europe/Rome (UTC‑01:00)
        Instant i2 = Instant.parse("2025-10-25T23:00:00Z");

        // Act
        Duration result = ReflectionTestUtils.invokeMethod(
                finalEventBuilder,
                "getDurationBetweenDates",
                i1,
                i2
        );

        // Assert
        assertEquals(Duration.ofHours(25), result);
    }

    @Test
    void getDuration_when_truncateEnabled_returns10Days() {
        // Arrange
        when(pnPaperTrackerConfigs.isEnableTruncatedDateForRefinementCheck()).thenReturn(true);

        Instant i1 = Instant.parse("2023-02-01T16:00:00Z");
        Instant i2 = Instant.parse("2023-02-11T09:00:00Z");

        // Act
        Duration result = ReflectionTestUtils.invokeMethod(
                finalEventBuilder,
                "getDurationBetweenDates",
                i1,
                i2
        );

        // Assert
        assertEquals(Duration.ofDays(10), result);
    }

    //Salto avanti (primavera): notte di 23 ore
    @Test
    void getDuration_when_truncateEnabled_dstSpringForward_returns23h() {
        // Arrange
        when(pnPaperTrackerConfigs.isEnableTruncatedDateForRefinementCheck()).thenReturn(true);

        // 2025-03-30T00:00 Europe/Rome (UTC‑01:00)
        Instant i1 = Instant.parse("2025-03-29T23:00:00Z");
        // 2025-03-31T00:00 Europe/Rome (UTC‑02:00)
        Instant i2 = Instant.parse("2025-03-30T22:00:00Z");

        // Act
        Duration result = ReflectionTestUtils.invokeMethod(
                finalEventBuilder,
                "getDurationBetweenDates",
                i1,
                i2
        );

        // Assert
        assertEquals(Duration.ofDays(1), result);
    }

    //Ritorno all’ora solare (autunno): notte di 25 ore -> PT25H.
    @Test
    void getDuration_when_truncateEnabled_dstFallBack_returns25h() {
        // Arrange
        when(pnPaperTrackerConfigs.isEnableTruncatedDateForRefinementCheck()).thenReturn(true);

        // 2025‑10‑25T00:00 Europe/Rome (UTC‑02:00)
        Instant i1 = Instant.parse("2025-10-24T22:00:00Z");
        // 2025‑10‑26T00:00 Europe/Rome (UTC‑01:00)
        Instant i2 = Instant.parse("2025-10-25T23:00:00Z");

        // Act
        Duration result = ReflectionTestUtils.invokeMethod(
                finalEventBuilder,
                "getDurationBetweenDates",
                i1,
                i2
        );

        // Assert
        assertEquals(Duration.ofDays(1), result);
    }

    @Test
    void getDuration_when_truncateEnabled_returns30Days() {
        // Arrange
        when(pnPaperTrackerConfigs.isEnableTruncatedDateForRefinementCheck()).thenReturn(true);
        Instant i2 = Instant.parse("2025-05-03T09:57:04Z");
        Instant i1 = Instant.parse("2025-04-03T09:55:52Z");

        // Act
        Duration result = ReflectionTestUtils.invokeMethod(
                finalEventBuilder,
                "getDurationBetweenDates",
                i1,
                i2
        );

        // Assert
        assertEquals(Duration.ofDays(30), result);
    }

    @Test
    void getDuration_when_truncateEnabled_returns29Days() {
        // Arrange
        when(pnPaperTrackerConfigs.isEnableTruncatedDateForRefinementCheck()).thenReturn(true);
        Instant i2 = Instant.parse("2025-05-03T09:57:04Z");
        Instant i1 = Instant.parse("2025-04-04T09:55:52Z");

        // Act
        Duration result = ReflectionTestUtils.invokeMethod(
                finalEventBuilder,
                "getDurationBetweenDates",
                i1,
                i2
        );

        // Assert
        assertEquals(Duration.ofDays(29), result);
    }

    @Test
    void getDuration_when_truncateEnabled_returns31Days() {
        // Arrange
        when(pnPaperTrackerConfigs.isEnableTruncatedDateForRefinementCheck()).thenReturn(true);
        Instant i1 = Instant.parse("2025-05-04T09:55:52Z");
        Instant i2 = Instant.parse("2025-06-04T09:57:04Z");

        // Act
        Duration result = ReflectionTestUtils.invokeMethod(
                finalEventBuilder,
                "getDurationBetweenDates",
                i1,
                i2
        );

        // Assert
        assertEquals(Duration.ofDays(31), result);
    }

}

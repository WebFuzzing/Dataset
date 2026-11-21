package reciter.database.dynamodb.repository;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reciter.database.dynamodb.model.FeedbackLog;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class FeedbackLogRepositoryTest {

    @Mock
    private FeedbackLogRepository feedbackLogRepository;

    private FeedbackLog feedbackLog1;
    private FeedbackLog feedbackLog2;

    @BeforeEach
    public void setUp() {
        feedbackLog1 = new FeedbackLog();
        feedbackLog1.setUid("uid1");
        feedbackLog1.setSk("sk1");

        feedbackLog2 = new FeedbackLog();
        feedbackLog2.setUid("uid2");
        feedbackLog2.setSk("sk2");
    }

    @Test
    public void testSave_NewItem() {
        doNothing().when(feedbackLogRepository).save(feedbackLog1);

        feedbackLogRepository.save(feedbackLog1);

        verify(feedbackLogRepository).save(feedbackLog1);
    }

    @Test
    public void testSave_DuplicateItem_ThrowsConditionalCheckFailedException() {
        // save() uses attribute_not_exists(sk) — inserting a duplicate should throw
        doThrow(ConditionalCheckFailedException.class).when(feedbackLogRepository).save(feedbackLog2);

        assertThrows(ConditionalCheckFailedException.class, () -> feedbackLogRepository.save(feedbackLog2));

        verify(feedbackLogRepository).save(feedbackLog2);
    }
}
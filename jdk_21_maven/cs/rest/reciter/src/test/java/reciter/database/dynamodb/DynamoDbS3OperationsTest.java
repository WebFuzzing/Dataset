package reciter.database.dynamodb;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

/**
 * Guards the two ways an S3 offload used to corrupt a record:
 *   1. the payload was deleted before the re-upload, so a failed upload destroyed it;
 *   2. the failure was swallowed, so callers flagged the row S3-backed with nothing behind it.
 */
public class DynamoDbS3OperationsTest {

	@Mock
	private S3Client s3;

	private DynamoDbS3Operations ddbs3;

	@BeforeEach
	public void setUp() {
		MockitoAnnotations.openMocks(this);
		ddbs3 = new DynamoDbS3Operations();
		ReflectionTestUtils.setField(ddbs3, "s3", s3);
	}

	@Test
	public void saveLargeItem_reportsSuccess_andNeverDeletesFirst() {
		when(s3.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
				.thenReturn(PutObjectResponse.builder().build());

		assertTrue(ddbs3.saveLargeItem("reciter-dynamodb", "payload", "AnalysisOutput/abc1234"));

		// An overwriting putObject is sufficient. Deleting first opens a window where a
		// failed re-upload leaves the record with no payload at all.
		verify(s3, never()).deleteObject(any(DeleteObjectRequest.class));
	}

	@Test
	public void saveLargeItem_reportsFailure_whenUploadFails() {
		when(s3.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
				.thenThrow(SdkClientException.create("connection reset"));

		// Must be false: callers use this to decide whether to set the S3 flag on the row.
		assertFalse(ddbs3.saveLargeItem("reciter-dynamodb", "payload", "AnalysisOutput/abc1234"));
	}

	@Test
	public void saveLargeItem_reportsFailure_whenS3Unavailable() {
		ReflectionTestUtils.setField(ddbs3, "s3", null);

		assertFalse(ddbs3.saveLargeItem("reciter-dynamodb", "payload", "AnalysisOutput/abc1234"));
	}
}

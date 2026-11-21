package reciter.security;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for the constant-time api-key comparison used by {@link MultiApiKeyFilter}.
 */
public class MultiApiKeyFilterTest {

	@Test
	public void equalKeys_match() {
		assertTrue(MultiApiKeyFilter.constantTimeEquals("s3cr3t-api-key", "s3cr3t-api-key"));
	}

	@Test
	public void differentKeys_sameLength_doNotMatch() {
		assertFalse(MultiApiKeyFilter.constantTimeEquals("s3cr3t-api-key", "X3cr3t-api-key"));
	}

	@Test
	public void differentLengthKeys_doNotMatch() {
		assertFalse(MultiApiKeyFilter.constantTimeEquals("short", "a-much-longer-api-key"));
	}

	@Test
	public void nullExpected_doesNotMatch() {
		assertFalse(MultiApiKeyFilter.constantTimeEquals("provided-key", null));
	}

	@Test
	public void nullProvided_doesNotMatch() {
		assertFalse(MultiApiKeyFilter.constantTimeEquals(null, "expected-key"));
	}

	@Test
	public void emptyStrings_match() {
		assertTrue(MultiApiKeyFilter.constantTimeEquals("", ""));
	}
}

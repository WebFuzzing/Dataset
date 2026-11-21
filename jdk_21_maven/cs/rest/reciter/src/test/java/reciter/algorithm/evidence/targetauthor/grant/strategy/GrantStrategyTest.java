package reciter.algorithm.evidence.targetauthor.grant.strategy;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;

import reciter.model.identity.Identity;

public class GrantStrategyTest {

	// Blank and all-zero ids are in prod Identity rows (53 people on 2026-09-23); each one used to
	// throw StringIndexOutOfBoundsException and drop grant evidence for the whole person.
	@Test
	public void malformedIdentityGrantsDoNotThrow() {
		Identity identity = new Identity();
		identity.setUid("test");
		identity.setGrants(Arrays.asList("", " ", "0", "00000000", "K00", "NS035806"));
		assertEquals(0.0, new GrantStrategy().executeStrategy(new ArrayList<>(), identity), 0.0);
	}
}

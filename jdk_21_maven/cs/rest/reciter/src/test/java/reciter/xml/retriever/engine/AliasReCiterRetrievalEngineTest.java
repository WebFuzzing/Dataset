package reciter.xml.retriever.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import reciter.api.parameters.RetrievalRefreshFlag;
import reciter.model.identity.AuthorName;
import reciter.model.identity.Identity;
import reciter.xml.retriever.pubmed.RetrievalErrorTracker;
import reciter.xml.retriever.engine.AliasReCiterRetrievalEngine.IdentityNameType;

/**
 * Covers the blank-middleName hazard in deriveAdditionalName (an Identity primaryName
 * can carry middleName "" — non-null — which AuthorName's constructor substrings, so it
 * must behave exactly like null) and the crash-recording contract of
 * retrieveArticlesByDateRange: false means a retrieval worker died, while a clean run
 * that simply found nothing still returns true.
 */
public class AliasReCiterRetrievalEngineTest {

	private final AliasReCiterRetrievalEngine engine = new AliasReCiterRetrievalEngine();

	/** AuthorName("Manuel", "", ...) throws from the constructor, so set the blank after. */
	private static AuthorName nameWithBlankMiddle(String firstName, String middleName, String lastName) {
		AuthorName name = new AuthorName(firstName, null, lastName);
		name.setMiddleName(middleName);
		return name;
	}

	/**
	 * The no-arg constructor leaves firstName/lastName null, which is exactly the shape
	 * DynamoDB deserialization produces for a malformed Identity (#715/#717). The setters
	 * reject null, so this is the only way to build that state.
	 */
	private static AuthorName rawName(String firstName, String lastName) {
		AuthorName name = new AuthorName();
		if (firstName != null) {
			name.setFirstName(firstName);
		}
		if (lastName != null) {
			name.setLastName(lastName);
		}
		return name;
	}

	private static Set<String> flattened(Set<AuthorName> names) {
		return names.stream()
				.map(n -> n.getFirstName() + "|" + n.getMiddleName() + "|" + n.getLastName())
				.collect(Collectors.toSet());
	}

	@Test
	public void blankMiddleNameDerivesCompoundSurnameHalvesWithoutThrowing() {
		// The mah4006 shape: compound surname, middleName "" (empty string, non-null).
		Set<AuthorName> derived = engine.deriveAdditionalName(
				nameWithBlankMiddle("Manuel", "", "Hidalgo Medina"));

		assertEquals(2, derived.size());
		assertEquals(Set.of("Hidalgo", "Medina"),
				derived.stream().map(AuthorName::getLastName).collect(Collectors.toSet()));
	}

	@Test
	public void blankMiddleNameDerivesExactlyWhatNullDoes() {
		Set<AuthorName> fromBlank = engine.deriveAdditionalName(
				nameWithBlankMiddle("Manuel", "", "Hidalgo Medina"));
		Set<AuthorName> fromWhitespace = engine.deriveAdditionalName(
				nameWithBlankMiddle("Manuel", " ", "Hidalgo Medina"));
		Set<AuthorName> fromNull = engine.deriveAdditionalName(
				new AuthorName("Manuel", null, "Hidalgo Medina"));

		assertEquals(flattened(fromNull), flattened(fromBlank));
		assertEquals(flattened(fromNull), flattened(fromWhitespace));
	}

	@Test
	public void blankMiddleNameIsNotUsedAsAFirstNameSubstitute() {
		// The single-initial branch promotes middleName to firstName; a blank one would
		// hand AuthorName's constructor an empty firstName to substring. Like null, it
		// must derive nothing.
		assertTrue(engine.deriveAdditionalName(nameWithBlankMiddle("W", "", "Bracken")).isEmpty());
		assertTrue(engine.deriveAdditionalName(new AuthorName("W", null, "Bracken")).isEmpty());
	}

	@Test
	public void realMiddleNamesStillDeriveUnchanged() {
		// AuthorName(_, null, _) stores middleName as "".
		Set<AuthorName> derived = engine.deriveAdditionalName(new AuthorName("W", "Clay", "Bracken"));
		assertEquals(Set.of("Clay||Bracken"), flattened(derived));
	}

	@Test
	public void crashedRetrievalWorkerIsRecordedAndReportedAsFailure() throws IOException {
		// A bare engine has no services wired, so the worker thread dies on an unchecked
		// NPE at the top of retrieveData — the ForkJoinPool stores the throwable without
		// rethrowing it, so the recorded uid is the only trace the caller can observe.
		Identity identity = new Identity();
		identity.setUid("mah4006");

		assertFalse(engine.retrieveArticlesByDateRange(Collections.singletonList(identity),
				new Date(), new Date(), RetrievalRefreshFlag.ALL_PUBLICATIONS));
	}

	@Test
	public void runWithNoRetrievalWorkStillReportsSuccess() throws IOException {
		// FALSE dispatches no retrieval, so no worker can fail: the gate is "a worker
		// crashed", never "nothing was retrieved".
		Identity identity = new Identity();
		identity.setUid("mah4006");

		assertTrue(engine.retrieveArticlesByDateRange(Collections.singletonList(identity),
				new Date(), new Date(), RetrievalRefreshFlag.FALSE));
	}

	@Test
	public void periodWithoutSpaceSurnameDerivesNothingInsteadOfThrowing() {
		// "St.John" satisfies contains(".") but the split pattern matches whitespace and
		// hyphens only, so it returns ONE element — indexing [1] threw
		// ArrayIndexOutOfBoundsException inside the retrieval worker.
		assertTrue(engine.deriveAdditionalName(new AuthorName("Manuel", null, "St.John")).isEmpty());
		assertTrue(engine.deriveAdditionalName(new AuthorName("Manuel", null, "O.Brien")).isEmpty());
	}

	@Test
	public void periodWithSpaceSurnameStillDerivesBothHalves() {
		// The regression guard for the fix above: a period AND a space still splits.
		Set<AuthorName> derived = engine.deriveAdditionalName(new AuthorName("Manuel", null, "Della. Robbia"));

		assertEquals(Set.of("Della.", "Robbia"),
				derived.stream().map(AuthorName::getLastName).collect(Collectors.toSet()));
	}

	@Test
	public void blankAndNullLastNamesDeriveNothingInsteadOfThrowing() {
		assertTrue(engine.deriveAdditionalName(rawName("Manuel", "")).isEmpty());
		assertTrue(engine.deriveAdditionalName(rawName("Manuel", " ")).isEmpty());
		assertTrue(engine.deriveAdditionalName(rawName("Manuel", null)).isEmpty());
	}

	@Test
	public void nullFirstNameDerivesWithoutThrowing() {
		// Null firstName must not NPE on contains(); the surname half still derives.
		Set<AuthorName> derived = engine.deriveAdditionalName(
				rawName(null, "Hidalgo Medina"));

		assertEquals(Set.of("Hidalgo", "Medina"),
				derived.stream().map(AuthorName::getLastName).collect(Collectors.toSet()));
	}

	@Test
	public void trailingWhitespaceInitialIsTreatedAsAnInitial() {
		// "W. " is the same initial as "W."; the untrimmed length()==2 check skipped it.
		assertEquals(Set.of("Clay||Bracken"),
				flattened(engine.deriveAdditionalName(new AuthorName("W. ", "Clay", "Bracken"))));
		assertEquals(flattened(engine.deriveAdditionalName(new AuthorName("W.", "Clay", "Bracken"))),
				flattened(engine.deriveAdditionalName(new AuthorName("W. ", "Clay", "Bracken"))));
	}

	@Test
	public void genuineOneLetterFirstNameIsUnaffectedByTheTrimNormalization() {
		// A real one-letter first name carries neither a space nor a period, so it never
		// reaches the initial-detection branch: it derives only via the length()==1 case.
		assertEquals(Set.of("Clay||Bracken"),
				flattened(engine.deriveAdditionalName(new AuthorName("W", "Clay", "Bracken"))));
		assertTrue(engine.deriveAdditionalName(new AuthorName("W", null, "Bracken")).isEmpty());
	}

	@Test
	public void errorFromRetrievalWorkerIsRecordedBeforeItIsRethrown() throws IOException {
		// An Error is recorded and rethrown; returning false proves the uid reached
		// failedUids before the rethrow left the catch block.
		AliasReCiterRetrievalEngine erroringEngine = new AliasReCiterRetrievalEngine() {
			@Override
			Set<Long> retrieveData(Identity identity, RetrievalRefreshFlag refreshFlag) {
				throw new StackOverflowError("simulated worker Error");
			}
		};
		Identity identity = new Identity();
		identity.setUid("mah4006");

		assertFalse(erroringEngine.retrieveArticlesByDateRange(Collections.singletonList(identity),
				new Date(), new Date(), RetrievalRefreshFlag.ALL_PUBLICATIONS));
	}

	@Test
	public void swallowedPubMedFailureOnFullSweepIsReportedAsFailure() throws IOException {
		// The strategies do not throw on a PubMed tool 500 / NCBI 429; they mark the
		// thread-local tracker and return empty. Without the tracker check the run looks
		// clean and the caller scores an empty candidate set over a populated Analysis row.
		AliasReCiterRetrievalEngine swallowingEngine = new AliasReCiterRetrievalEngine() {
			@Override
			Set<Long> retrieveData(Identity identity, RetrievalRefreshFlag refreshFlag) {
				RetrievalErrorTracker.markError();
				return Collections.emptySet();
			}
		};
		Identity identity = new Identity();
		identity.setUid("prs4005");

		assertFalse(swallowingEngine.retrieveArticlesByDateRange(Collections.singletonList(identity),
				new Date(), new Date(), RetrievalRefreshFlag.ALL_PUBLICATIONS));
	}

	@Test
	public void cleanFullSweepFindingNothingStillReportsSuccess() throws IOException {
		// The gate must stay "a retrieval failed", never "nothing was retrieved": a person
		// with genuinely no articles has to keep returning 200.
		AliasReCiterRetrievalEngine emptyButCleanEngine = new AliasReCiterRetrievalEngine() {
			@Override
			Set<Long> retrieveData(Identity identity, RetrievalRefreshFlag refreshFlag) {
				RetrievalErrorTracker.reset();
				return Collections.emptySet();
			}
		};
		Identity identity = new Identity();
		identity.setUid("prs4005");

		assertTrue(emptyButCleanEngine.retrieveArticlesByDateRange(Collections.singletonList(identity),
				new Date(), new Date(), RetrievalRefreshFlag.ALL_PUBLICATIONS));
	}
}

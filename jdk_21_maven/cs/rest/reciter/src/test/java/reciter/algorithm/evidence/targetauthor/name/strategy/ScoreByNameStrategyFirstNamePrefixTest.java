package reciter.algorithm.evidence.targetauthor.name.strategy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

import reciter.algorithm.cluster.article.scorer.ReCiterArticleScorer;
import reciter.engine.StrategyParameters;
import reciter.engine.analysis.evidence.AuthorNameEvidence;
import reciter.model.article.ReCiterAuthor;
import reciter.model.identity.AuthorName;

/**
 * Regression tests for issue #746: the first-name matcher labelled a left-anchored
 * PREFIX match as {@code full-exact}, so a byline of "Shuofei" was reported as an exact
 * match against a registered "Shuo".
 *
 * <p>The two scoring entry points are private, so they are driven by reflection with a
 * hand-built {@link StrategyParameters}; that keeps the test on the pure name-matching
 * cascade with no Spring context, no DynamoDB and no retrieval stack.</p>
 *
 * <p>Every case asserts the emitted SCORE as well as the type string. The relabel is
 * required to be numerically inert: {@code nameMatchFirstScore} is a model feature in
 * both deployed pipelines, so a changed number here would silently shift a feature
 * distribution the 72/47-feature models were trained on.</p>
 */
public class ScoreByNameStrategyFirstNamePrefixTest {

	/** Values taken from src/main/resources/application.properties. */
	private static final double FULL_EXACT = 1.852;
	private static final double INFERRED_INITIALS_EXACT = 0.441;
	private static final double FULL_FUZZY = -0.75;
	private static final double NO_MATCH = -1.941;
	private static final double CONFLICTING_ALL_BUT_INITIALS = -2.646;
	private static final double CONFLICTING_ENTIRELY = -3.087;
	private static final double MIDDLE_IDENTITY_NULL = 0.794;
	private static final double MIDDLE_NO_MATCH = -0.794;
	private static final double MODIFIER_IDENTITY_SUBSTRING_OF_ARTICLE_FIRST = -1.608;

	private ScoreByNameStrategy strategy;
	private Method scoreFirstNameMiddleNameNull;
	private Method scoreFirstNameMiddleName;

	@Before
	public void setUp() throws Exception {
		StrategyParameters params = new StrategyParameters();
		// Read by the ScoreByNameStrategy field initializer.
		params.setNameExcludedSuffixes("Jr,Jr.,MD PhD,MD-PhD,PhD,MD,III,III.,II,II.,Sr,Sr.");

		params.setNameMatchFirstTypeFullExactScore(FULL_EXACT);
		params.setNameMatchFirstTypeInferredInitialsExactScore(INFERRED_INITIALS_EXACT);
		params.setNameMatchFirstTypeFullFuzzyScore(FULL_FUZZY);
		params.setNameMatchFirstTypeNoMatchScore(NO_MATCH);
		params.setNameMatchFirstTypeFullConflictingAllButInitialsScore(CONFLICTING_ALL_BUT_INITIALS);
		params.setNameMatchFirstTypeFullConflictingEntirelyScore(CONFLICTING_ENTIRELY);
		// #746 buckets, defaulted to the full-exact value exactly as shipped.
		params.setNameMatchFirstTypeFullPrefixScore(FULL_EXACT);
		params.setNameMatchFirstTypeFullSuffixScore(FULL_EXACT);
		params.setNameMatchFirstTypeInferredInitialsPrefixScore(FULL_EXACT);

		params.setNameMatchMiddleTypeIdentityNullMatchNotAttemptedScore(MIDDLE_IDENTITY_NULL);
		params.setNameMatchMiddleTypeNoMatchScore(MIDDLE_NO_MATCH);
		params.setNameMatchModifierIdentitySubstringOfArticleFirstnameScore(
				MODIFIER_IDENTITY_SUBSTRING_OF_ARTICLE_FIRST);

		ReCiterArticleScorer.strategyParameters = params;

		strategy = new ScoreByNameStrategy();
		scoreFirstNameMiddleNameNull = ScoreByNameStrategy.class.getDeclaredMethod(
				"scoreFirstNameMiddleNameNull", AuthorName.class, AuthorName.class,
				Map.class, AuthorNameEvidence.class);
		scoreFirstNameMiddleNameNull.setAccessible(true);
		scoreFirstNameMiddleName = ScoreByNameStrategy.class.getDeclaredMethod(
				"scoreFirstNameMiddleName", AuthorName.class, AuthorName.class,
				Map.class, AuthorNameEvidence.class);
		scoreFirstNameMiddleName.setAccessible(true);
	}

	// ---- helpers -----------------------------------------------------------

	/**
	 * Builds a name the way AuthorNameSanitizationUtils leaves one: periods, spaces and
	 * dashes already stripped, initials derived from the surviving characters.
	 */
	private static AuthorName name(String first, String middle, String last) {
		AuthorName n = new AuthorName();
		n.setFirstName(first);
		n.setMiddleName(middle);
		n.setLastName(last);
		if (first != null && !first.isBlank()) {
			n.setFirstInitial(first.substring(0, 1));
		}
		if (middle != null && !middle.isEmpty()) {
			n.setMiddleInitial(middle.substring(0, 1));
		}
		return n;
	}

	private static Map<ReCiterAuthor, ReCiterAuthor> articleAuthor(AuthorName articleName) {
		ReCiterAuthor author = new ReCiterAuthor(articleName, null);
		author.setTargetAuthor(true);
		Map<ReCiterAuthor, ReCiterAuthor> map = new LinkedHashMap<>();
		map.put(author, author);
		return map;
	}

	/** Runs the middle-name-is-null cascade (scoreFirstNameMiddleNameNull). */
	private AuthorNameEvidence scoreMiddleNull(String identityFirst, String identityLast,
			String articleFirst, String articleLast) throws Exception {
		AuthorName identity = name(identityFirst, null, identityLast);
		AuthorNameEvidence evidence = new AuthorNameEvidence();
		scoreFirstNameMiddleNameNull.invoke(strategy, identity, identity,
				articleAuthor(name(articleFirst, null, articleLast)), evidence);
		return evidence;
	}

	/** Runs the middle-name-present cascade (scoreFirstNameMiddleName). */
	private AuthorNameEvidence scoreWithMiddle(String identityFirst, String identityMiddle,
			String identityLast, String articleFirst, String articleLast) throws Exception {
		AuthorName identity = name(identityFirst, identityMiddle, identityLast);
		AuthorNameEvidence evidence = new AuthorNameEvidence();
		scoreFirstNameMiddleName.invoke(strategy, identity, identity,
				articleAuthor(name(articleFirst, null, articleLast)), evidence);
		return evidence;
	}

	private static void assertFirst(AuthorNameEvidence e, String type, double score) {
		assertEquals("nameMatchFirstType", type, e.getNameMatchFirstType());
		assertEquals("nameMatchFirstScore", score, e.getNameMatchFirstScore(), 1e-9);
	}

	// ---- the defect --------------------------------------------------------

	/**
	 * The reported case. "Shuo" is a registered first name; "Shuofei" is a different
	 * given name that happens to begin with it. Before #746 this was reported as
	 * full-exact, indistinguishable from a real exact match.
	 */
	@Test
	public void shuoAgainstShuofeiIsAPrefixNotAnExactMatch() throws Exception {
		AuthorNameEvidence e = scoreMiddleNull("Shuo", "Chen", "Shuofei", "Chen");
		assertFirst(e, "full-prefix", FULL_EXACT);
		assertEquals("identitySubstringOfArticle-firstName", e.getNameMatchModifier());
		assertEquals(MODIFIER_IDENTITY_SUBSTRING_OF_ARTICLE_FIRST,
				e.getNameMatchModifierScore(), 1e-9);
	}

	/** The branch's intended case (a glued first+middle byline) must still match. */
	@Test
	public void paulAgainstPaulJamesStillMatchesAsAPrefix() throws Exception {
		AuthorNameEvidence e = scoreMiddleNull("Paul", "Albert", "PaulJames", "Albert");
		assertFirst(e, "full-prefix", FULL_EXACT);
		assertEquals("identityNull-MatchNotAttempted", e.getNameMatchMiddleType());
		assertEquals("identitySubstringOfArticle-firstName", e.getNameMatchModifier());
	}

	/**
	 * The single-letter class: a registered name that sanitized down to one letter
	 * ('M.' -> "M") merely BEGINS the byline. That is an initial match, not an exact
	 * one, and it gets its own bucket rather than falling through to a negative one.
	 */
	@Test
	public void singleLetterRegisteredNameAgainstFullBylineIsAnInferredInitial() throws Exception {
		AuthorNameEvidence e = scoreMiddleNull("M", "Rivera", "Mayra", "Rivera");
		assertFirst(e, "inferredInitials-prefix", FULL_EXACT);
		assertEquals("identitySubstringOfArticle-firstName", e.getNameMatchModifier());
	}

	/**
	 * Guard for the fall-through trap: if the prefix branch had simply been guarded on
	 * length >= 2, "M" vs "Mayra" would have skipped the three-character and Levenshtein
	 * branches and landed on full-conflictingAllButInitials (-2.646) -- worse than the
	 * status quo, and a numeric change.
	 */
	@Test
	public void singleLetterClassDoesNotFallThroughToAConflictBucket() throws Exception {
		AuthorNameEvidence e = scoreMiddleNull("M", "Rivera", "Mayra", "Rivera");
		assertTrue("must not land in a negative bucket", e.getNameMatchFirstScore() > 0);
		assertEquals(FULL_EXACT, e.getNameMatchFirstScore(), 1e-9);
	}

	// ---- directions and buckets that must NOT change ------------------------

	/** The reverse direction was already correct and stays correct. */
	@Test
	public void articleInitialAgainstFullRegisteredNameStaysInferredInitialsExact() throws Exception {
		AuthorNameEvidence e = scoreMiddleNull("Paul", "Albert", "P", "Albert");
		assertFirst(e, "inferredInitials-exact", INFERRED_INITIALS_EXACT);
		assertNull("reverse direction sets no modifier", e.getNameMatchModifier());
	}

	/** A genuine exact match is still full-exact. */
	@Test
	public void exactFirstNameStaysFullExact() throws Exception {
		AuthorNameEvidence e = scoreMiddleNull("Paul", "Albert", "Paul", "Albert");
		assertFirst(e, "full-exact", FULL_EXACT);
		assertNull(e.getNameMatchModifier());
	}

	/** A non-match is still the terminal conflict bucket. */
	@Test
	public void unrelatedFirstNamesStayFullConflictingEntirely() throws Exception {
		AuthorNameEvidence e = scoreMiddleNull("Pascale", "Albert", "Curtis", "Albert");
		assertFirst(e, "full-conflictingEntirely", CONFLICTING_ENTIRELY);
	}

	/** Same initial, different name: unchanged. */
	@Test
	public void sameInitialDifferentNameStaysConflictingAllButInitials() throws Exception {
		AuthorNameEvidence e = scoreMiddleNull("Paul", "Albert", "Peter", "Albert");
		assertFirst(e, "full-conflictingAllButInitials", CONFLICTING_ALL_BUT_INITIALS);
	}

	// ---- the middle-name-present cascade -----------------------------------

	/** The regex prefix branch (identity.firstName + "%") shares the defect. */
	@Test
	public void regexPrefixBranchReportsFullPrefix() throws Exception {
		AuthorNameEvidence e = scoreWithMiddle("Robert", "Q", "Smith", "RobertR", "Smith");
		assertFirst(e, "full-prefix", FULL_EXACT);
		assertEquals("noMatch", e.getNameMatchMiddleType());
		assertEquals("identitySubstringOfArticle-firstName", e.getNameMatchModifier());
	}

	/** The regex prefix branch also carries the single-letter class. */
	@Test
	public void regexPrefixBranchSplitsTheSingleLetterClass() throws Exception {
		AuthorNameEvidence e = scoreWithMiddle("M", "Q", "Smith", "Mayra", "Smith");
		assertFirst(e, "inferredInitials-prefix", FULL_EXACT);
	}

	/** The regex suffix branch ("%" + identity.firstName) gets its own honest label. */
	@Test
	public void regexSuffixBranchReportsFullSuffix() throws Exception {
		AuthorNameEvidence e = scoreWithMiddle("Cary", "Q", "Smith", "MCary", "Smith");
		assertFirst(e, "full-suffix", FULL_EXACT);
		assertEquals("noMatch", e.getNameMatchMiddleType());
		assertEquals("identitySubstringOfArticle-firstName", e.getNameMatchModifier());
	}

	/** An exact first name in the middle-present cascade is untouched. */
	@Test
	public void exactFirstNameWithMiddlePresentStaysFullExact() throws Exception {
		AuthorNameEvidence e = scoreWithMiddle("Paul", "J", "Smith", "Paul", "Jones");
		assertFirst(e, "full-exact", FULL_EXACT);
	}

	// ---- the numeric contract ----------------------------------------------

	/**
	 * The shipped defaults must keep the three new buckets numerically identical to
	 * full-exact. Retuning any of them changes nameMatchFirstScore, a feature in both
	 * deployed models, and also nameMatchTypeOrdinal in the scoring Lambda -- neither
	 * can move without a retrain. If someone edits a value, this test is the tripwire.
	 */
	@Test
	public void newBucketsShipAtTheFullExactValue() throws Exception {
		Properties props = new Properties();
		try (InputStream in = getClass().getClassLoader()
				.getResourceAsStream("application.properties")) {
			assertNotNull("application.properties must be on the test classpath", in);
			props.load(in);
		}
		String fullExact = props.getProperty("nameMatchFirstType.full-exact");
		assertNotNull(fullExact);
		for (String key : new String[] { "nameMatchFirstType.full-prefix",
				"nameMatchFirstType.full-suffix",
				"nameMatchFirstType.inferredInitials-prefix" }) {
			String value = props.getProperty(key);
			assertNotNull(key + " must be declared", value);
			assertEquals(key + " must still equal nameMatchFirstType.full-exact; "
					+ "changing it requires a model retrain (issue #746)",
					Double.parseDouble(fullExact), Double.parseDouble(value), 1e-9);
		}
	}
}

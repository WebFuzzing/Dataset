package reciter.utils;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import reciter.engine.StrategyParameters;
import reciter.model.identity.AuthorName;
import reciter.model.identity.Identity;

@RunWith(MockitoJUnitRunner.class)
public class AuthorNameSanitizationUtilsTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public final void testSanitizeArticleAuthorNames() {
		//fail("Not yet implemented");
	}

	@Test
	public final void testSanitizeIdentityAuthorNames() {
		//fail("Not yet implemented");
	}

	@Test
	public final void testCheckToIgnoreNameVariants() {
		Map<AuthorName, AuthorName> sanitizedIdentityAuthorMap = new HashMap<AuthorName, AuthorName>(); 
		AuthorNameSanitizationUtils authorNameSanitizationUtils = new AuthorNameSanitizationUtils();
		sanitizedIdentityAuthorMap.put(new AuthorName("Joerg-Patrick", null, "Stuebgen"), new AuthorName("JoergPatrick", null, "Stuebgen"));
		sanitizedIdentityAuthorMap.put(new AuthorName("Jo", null, "Stuebgen"), new AuthorName("Jo", null, "Stuebgen"));
		authorNameSanitizationUtils.checkToIgnoreNameVariants(sanitizedIdentityAuthorMap);
		//Check for AuthorName firstName starts with other AuthorName and middle name is null or empty
		assertEquals("Removed one name", 1, sanitizedIdentityAuthorMap.size());
		assertEquals("JoergPatrick", sanitizedIdentityAuthorMap.values().iterator().next().getFirstName());
		sanitizedIdentityAuthorMap.clear();
		sanitizedIdentityAuthorMap.put(new AuthorName("Joerg-Patrick", null, "Stuebgen"), new AuthorName("JoergPatrick", null, "Stuebgen"));
		sanitizedIdentityAuthorMap.put(new AuthorName("JoE", null, "Stuebgen"), new AuthorName("JoE", null, "Stuebgen"));
		authorNameSanitizationUtils.checkToIgnoreNameVariants(sanitizedIdentityAuthorMap);
		assertEquals("Case ensitive check", 1, sanitizedIdentityAuthorMap.size());
		assertEquals("JoergPatrick", sanitizedIdentityAuthorMap.values().iterator().next().getFirstName());
		
		
		sanitizedIdentityAuthorMap.clear();
		sanitizedIdentityAuthorMap.put(new AuthorName("Joerg-Patrick", null, "Stuebgen"), new AuthorName("JoergPatrick", null, "Stuebgen"));
		sanitizedIdentityAuthorMap.put(new AuthorName("JoE", null, "Stuebgen"), new AuthorName("JoE", null, "Stuebgen"));
		sanitizedIdentityAuthorMap.put(new AuthorName("Jo", null, "Stuebgen"), new AuthorName("Jo", null, "Stuebgen"));
		sanitizedIdentityAuthorMap.put(new AuthorName("Joseph", "Patrick", "Stuebgen"), new AuthorName("Joseph", "Patrick", "Stuebgen"));
		sanitizedIdentityAuthorMap.put(new AuthorName("Jose", "Patrick", "Stuebgen"), new AuthorName("Jose", "Patrick", "Stuebgen"));
		authorNameSanitizationUtils.checkToIgnoreNameVariants(sanitizedIdentityAuthorMap);
		assertEquals("All check including case sensitive, first case & second case", 3, sanitizedIdentityAuthorMap.size());
		// JoE and Jo are prefixes of JoergPatrick with no middle name; Jose/Patrick survives
		// because a non-blank middle name is never dropped by the prefix rules.
		assertEquals(new HashSet<>(Arrays.asList("JoergPatrick", "Joseph", "Jose")), firstNames(sanitizedIdentityAuthorMap));
	}

	// #704: alc4061's identity — primary "Alberto Mario" with an EMPTY middle name plus alternates
	// "Alberto Mario" / null and "Alberto" / null. Two rules fire in one pass (blank-middle and
	// starts-with) and the old iterator-mutating loop threw IllegalStateException from a second
	// remove() without an intervening next(), 500-ing every feature-generator call for the uid.
	// Built with setters, not the 3-arg constructor: that constructor normalises a null middle
	// name to "", while sanitizeIdentityAuthorNames only sets a middle name when the alias has
	// one — so production really does mix "" and null (asserted end-to-end in
	// testSanitizeIdentityAuthorNamesMixesBlankAndNullMiddleNames), which is what trips the two
	// rules at once.
	@Test
	public final void testCheckToIgnoreNameVariantsDoesNotThrowWhenTwoRulesFireInOnePass() {
		Map<AuthorName, AuthorName> names = new HashMap<AuthorName, AuthorName>();
		AuthorNameSanitizationUtils utils = new AuthorNameSanitizationUtils();
		names.put(name("Alberto Mario", "", "Ceballos Arroyo"), name("AlbertoMario", "", "CeballosArroyo"));
		names.put(name("Alberto Mario", null, "Ceballos Arroyo"), name("AlbertoMario", null, "CeballosArroyo"));
		names.put(name("Alberto", null, "Ceballos Arroyo"), name("Alberto", null, "CeballosArroyo"));
		assertEquals(3, names.size());
		utils.checkToIgnoreNameVariants(names);
		assertEquals("blank-middle and prefix variants both dropped, one full name kept", 1, names.size());
		AuthorName kept = names.values().iterator().next();
		assertEquals("AlbertoMario", kept.getFirstName());
		assertNull(kept.getMiddleName());

		// a chain where every variant is a prefix of the next and one carries a blank middle:
		// every pair matches a rule, in every order — must never throw, must never remove the
		// last name, and the longest full name is the one left standing.
		names.clear();
		names.put(name("A", null, "Last"), name("A", null, "Last"));
		names.put(name("Ab", null, "Last"), name("Ab", null, "Last"));
		names.put(name("Abc", "", "Last"), name("Abc", "", "Last"));
		names.put(name("Abc", null, "Last"), name("Abc", null, "Last"));
		assertEquals(4, names.size());
		utils.checkToIgnoreNameVariants(names);
		assertEquals(1, names.size());
		assertEquals("Abc", names.values().iterator().next().getFirstName());
		assertNull(names.values().iterator().next().getMiddleName());
	}

	// The alc4061 shape fed through the real entry point. Threw IllegalStateException before #704.
	@Test
	public final void testSanitizeIdentityAuthorNamesAlc4061EndToEnd() {
		Identity identity = new Identity();
		identity.setPrimaryName(name("Alberto Mario", "", "Ceballos Arroyo"));
		identity.setAlternateNames(Arrays.asList(
				name("Alberto Mario", null, "Ceballos Arroyo"),
				name("Alberto", null, "Ceballos Arroyo")));

		Map<AuthorName, AuthorName> sanitized = utilsWithSuffixes().sanitizeIdentityAuthorNames(identity);

		assertEquals(1, sanitized.size());
		AuthorName kept = sanitized.values().iterator().next();
		assertEquals("AlbertoMario", kept.getFirstName());
		assertNull(kept.getMiddleName());
		assertEquals("CeballosArroyo", kept.getLastName());
	}

	// Why production mixes "" and null: the primary's middle name is copied verbatim (an empty
	// string stays ""), but an alias's middle name is only set when the alias has one (null
	// stays null). No prune rule fires across that ""/null pair, so both names survive and the
	// mix is observable. If sanitizeIdentityAuthorNames is ever changed to normalise both to
	// "" or both to null, this test fails and the #704 regression test above must be revisited.
	@Test
	public final void testSanitizeIdentityAuthorNamesMixesBlankAndNullMiddleNames() {
		Identity identity = new Identity();
		identity.setPrimaryName(name("Alberto Mario", "", "Ceballos Arroyo"));
		identity.setAlternateNames(Arrays.asList(name("Alberto", null, "Ceballos Arroyo")));

		Map<AuthorName, AuthorName> sanitized = utilsWithSuffixes().sanitizeIdentityAuthorNames(identity);

		assertEquals(2, sanitized.size());
		assertTrue(sanitized.values().stream().anyMatch(n -> "".equals(n.getMiddleName())));
		assertTrue(sanitized.values().stream().anyMatch(n -> n.getMiddleName() == null));
	}

	// Rule 3 is a case-only duplicate check. Each side is a variant of the other, so this is
	// also the "never empty the map" case: exactly one survives. Rules 1 and 2 are asymmetric
	// (the fuller name always wins), but rule 3 is symmetric, so without the fullest-first
	// sort the survivor would be whichever the map iterated first — a LinkedHashMap with the
	// lowercase name inserted first pins that down: the sort's tie-break (uppercase sorts
	// before lowercase) must still pick "Andrew".
	@Test
	public final void testCaseOnlyDuplicateCollapsesToOneDeterministically() {
		Map<AuthorName, AuthorName> names = new LinkedHashMap<AuthorName, AuthorName>();
		names.put(name("andrew", "j", "dannenberg"), name("andrew", "j", "dannenberg"));
		names.put(name("Andrew", "J", "Dannenberg"), name("Andrew", "J", "Dannenberg"));
		new AuthorNameSanitizationUtils().checkToIgnoreNameVariants(names);
		assertEquals(1, names.size());
		assertEquals("Andrew", names.values().iterator().next().getFirstName());
	}

	// Pre-#704 behaviour, preserved: a middle initial is NOT collapsed into a full middle name.
	@Test
	public final void testMiddleInitialIsNotCollapsedIntoFullMiddleName() {
		Map<AuthorName, AuthorName> names = new HashMap<AuthorName, AuthorName>();
		names.put(name("Andrew", "J", "Dannenberg"), name("Andrew", "J", "Dannenberg"));
		names.put(name("Andrew", "Jess", "Dannenberg"), name("Andrew", "Jess", "Dannenberg"));
		new AuthorNameSanitizationUtils().checkToIgnoreNameVariants(names);
		assertEquals(2, names.size());
	}

	// Bad or missing data: an empty map, a null value, and names missing a first or last
	// name must neither throw nor be dropped (they can be neither the "full" name nor a
	// candidate under any rule).
	@Test
	public final void testToleratesEmptyAndPartialNames() {
		AuthorNameSanitizationUtils utils = new AuthorNameSanitizationUtils();
		Map<AuthorName, AuthorName> names = new HashMap<AuthorName, AuthorName>();
		utils.checkToIgnoreNameVariants(names);
		assertEquals(0, names.size());

		names.put(name(null, null, "Stuebgen"), name(null, null, "Stuebgen"));
		names.put(name("Jo", null, null), name("Jo", null, null));
		names.put(name("Joerg", null, "Stuebgen"), name("Joerg", null, "Stuebgen"));
		names.put(name("Null", "Value", "Entry"), null);
		utils.checkToIgnoreNameVariants(names);
		assertEquals(4, names.size());
	}

	// setFirstName/setLastName reject null, so a missing first or last name is left unset.
	private static AuthorName name(String first, String middle, String last) {
		AuthorName n = new AuthorName();
		if (first != null) {
			n.setFirstName(first);
		}
		n.setMiddleName(middle);
		if (last != null) {
			n.setLastName(last);
		}
		return n;
	}

	private static Set<String> firstNames(Map<AuthorName, AuthorName> names) {
		return names.values().stream().map(AuthorName::getFirstName).collect(Collectors.toSet());
	}

	private static AuthorNameSanitizationUtils utilsWithSuffixes() {
		StrategyParameters strategyParameters = new StrategyParameters();
		strategyParameters.setNameExcludedSuffixes("Jr,MD PhD,MD-PhD,PhD,MD,III,II,Sr");
		return new AuthorNameSanitizationUtils(strategyParameters);
	}

	@Test
	public final void testGenerateSuffixRegex() {
		//fail("Not yet implemented");
	}

}

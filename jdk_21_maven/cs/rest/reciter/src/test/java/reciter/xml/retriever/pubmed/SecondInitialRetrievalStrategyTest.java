package reciter.xml.retriever.pubmed;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.springframework.test.util.ReflectionTestUtils.invokeMethod;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.Test;

import reciter.model.identity.AuthorName;
import reciter.model.identity.Identity;

public class SecondInitialRetrievalStrategyTest {

	private final SecondInitialRetrievalStrategy strategy = new SecondInitialRetrievalStrategy();

	private Identity identity(AuthorName primary, Set<AuthorName> alternates) {
		Identity id = new Identity();
		id.setPrimaryName(primary);
		id.setAlternateNames(alternates == null ? Collections.<AuthorName>emptyList() : new java.util.ArrayList<>(alternates));
		return id;
	}

	private AuthorName name(String first, String middle, String last) {
		return new AuthorName(first, middle, last);
	}

	// Bug repro: "Eulho Jung" -> only one uppercase letter, no qualifying alternates -> previously returned "()"
	@Test
	public void singleUppercase_noAlternates_returnsNull() {
		Identity id = identity(name("Eulho", null, "Jung"), Collections.<AuthorName>emptySet());
		String keyword = invokeMethod(strategy, "getStrategySpecificKeyword", id);
		assertNull("Single-uppercase first name with no alternates must not produce '()'", keyword);
	}

	// Same bug with single-uppercase alternates that also fail the >=2 check
	@Test
	public void singleUppercase_unqualifiedAlternates_returnsNull() {
		Set<AuthorName> alts = new LinkedHashSet<>();
		alts.add(name("eulho", null, "jung"));
		Identity id = identity(name("Eulho", null, "Jung"), alts);
		String keyword = invokeMethod(strategy, "getStrategySpecificKeyword", id);
		assertNull("All-lowercase variants should not produce '()'", keyword);
	}

	// Positive case: two-uppercase first name should still produce a normal author query
	@Test
	public void twoUppercase_returnsAuthorQuery() {
		Identity id = identity(name("McAdams", null, "Smith"), Collections.<AuthorName>emptySet());
		String keyword = invokeMethod(strategy, "getStrategySpecificKeyword", id);
		assertEquals("(Smith MA[au])", keyword);
	}

	// Positive case: first + middle giving two uppercase letters
	@Test
	public void firstAndMiddleUppercase_returnsAuthorQuery() {
		Identity id = identity(name("John", "Robert", "Doe"), Collections.<AuthorName>emptySet());
		String keyword = invokeMethod(strategy, "getStrategySpecificKeyword", id);
		assertEquals("(Doe JR[au])", keyword);
	}
}

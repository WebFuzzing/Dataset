package reciter.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import reciter.api.parameters.FilterFeedbackType;
import reciter.api.parameters.GoldStandardUpdateFlag;
import reciter.api.parameters.RetrievalRefreshFlag;
import reciter.api.parameters.UseGoldStandard;
import reciter.database.dynamodb.model.AnalysisOutput;
import reciter.database.dynamodb.model.ESearchPmid;
import reciter.database.dynamodb.model.ESearchResult;
import reciter.database.dynamodb.model.GoldStandard;
import reciter.engine.StrategyParameters;
import reciter.engine.analysis.ReCiterArticleFeature;
import reciter.engine.analysis.ReCiterArticleFeature.PublicationFeedback;
import reciter.engine.analysis.ReCiterFeature;
import reciter.feedback.EntryPath;
import reciter.model.identity.Identity;
import reciter.service.AnalysisService;
import reciter.service.ESearchResultService;
import reciter.service.ExternalArticleService;
import reciter.service.IdentityService;
import reciter.service.ScopusService;
import reciter.service.dynamo.IDynamoDbGoldStandardService;
import reciter.xml.retriever.engine.ReCiterRetrievalEngine;

@ExtendWith(MockitoExtension.class)
public class ReCiterControllerTest {

	@Mock
	private ESearchResultService eSearchResultService;
	private static final Logger log = LoggerFactory.getLogger(ReCiterControllerTest.class);

	@Mock
	private IdentityService identityService;

	@Mock
	private ScopusService scopusService;

	@Mock
	private StrategyParameters strategyParameters;

	@Mock
	private AnalysisService analysisService;

	@Mock
	private IDynamoDbGoldStandardService dynamoDbGoldStandardService;

	@Mock
	private ReCiterRetrievalEngine aliasReCiterRetrievalEngine;
	
	@Mock
	private ExternalArticleService externalArticleService;

	@InjectMocks
	private ReCiterController reCiterController;

	private GoldStandard validGoldStandard;
	private List<GoldStandard> validGoldStandardList;
	private List<Identity> identityList;
	private String testUid = "test123";
	private Identity identity;
	private ESearchResult testESearchResult;
	private List<String> testUids;
	private List<AnalysisOutput> analysisOutputList;
	private Double testScore = 0.9;
	private int maxArticles = 5;
	private List<String> personTypes;
	private AnalysisOutput analysisOutput;
	private ReCiterFeature reCiterFeature;
	private GoldStandard goldStandard;
	private List<ReCiterArticleFeature> articleFeatures;
	private Double defaultScoreThreshold = 0.7;

	@BeforeEach
	public void setUp() throws IOException {
		// retrieveArticlesByDateRange returning false now means "a retrieval worker
		// crashed"; default the mock to a clean run so only tests that stub a failure
		// (or doThrow) exercise the error paths.
		lenient().when(aliasReCiterRetrievalEngine.retrieveArticlesByDateRange(anyList(), any(), any(), any()))
				.thenReturn(true);

		// Set the necessary properties
		ReflectionTestUtils.setField(reCiterController, "useScopusArticles", true);
		ReflectionTestUtils.setField(reCiterController, "totalArticleScoreStandardizedDefault", 0.7);
		ReflectionTestUtils.setField(reCiterController, "nameIgnoredCoAuthors", "Smith, J;Doe, J");
		ReflectionTestUtils.setField(reCiterController, "keywordsMax", 10.0);
		ReflectionTestUtils.setField(reCiterController, "uidsMaxCount", 50);
		// Set the default score threshold
		ReflectionTestUtils.setField(reCiterController, "totalArticleScoreStandardizedDefault", defaultScoreThreshold);

		// Create test data
		validGoldStandard = createValidGoldStandard(testUid);

		// Create a list of valid gold standards
		validGoldStandardList = new ArrayList<>();
		validGoldStandardList.add(validGoldStandard);
		validGoldStandardList.add(createValidGoldStandard("test456"));

		// Create identity list for retrieveArticles method
		identityList = new ArrayList<>();
		identity = new Identity();

		identity.setUid(testUid);
		identityList.add(identity);

		Identity identity2 = new Identity();
		identity2.setUid("test456");
		identityList.add(identity2);

		testESearchResult = new ESearchResult();
		testESearchResult.setUid(testUid);
		testESearchResult.setRetrievalDate(Instant.now());
		testUids = Arrays.asList(testUid, "test456");
		personTypes = Arrays.asList("faculty", "student");

		// Create article features
		articleFeatures = new ArrayList<>();

		// Article with score above threshold and NULL feedback
		ReCiterArticleFeature article1 = new ReCiterArticleFeature();
		article1.setPmid(12345L);
		article1.setAuthorshipLikelihoodScore(8.5);
		article1.setUserAssertion(PublicationFeedback.NULL);

		// Article with ACCEPTED feedback
		ReCiterArticleFeature article2 = new ReCiterArticleFeature();
		article2.setPmid(23456L);
		article2.setAuthorshipLikelihoodScore(9.2);
		article2.setUserAssertion(PublicationFeedback.ACCEPTED);

		// Article with REJECTED feedback
		ReCiterArticleFeature article3 = new ReCiterArticleFeature();
		article3.setPmid(34567L);
		article3.setAuthorshipLikelihoodScore(5.1);
		article3.setUserAssertion(PublicationFeedback.REJECTED);

		// Article with score below threshold and NULL feedback
		ReCiterArticleFeature article4 = new ReCiterArticleFeature();
		article4.setPmid(45678L);
		article4.setAuthorshipLikelihoodScore(3.5);
		article4.setUserAssertion(PublicationFeedback.NULL);

		articleFeatures.add(article1);
		articleFeatures.add(article2);
		articleFeatures.add(article3);
		articleFeatures.add(article4);

		// Create gold standard data
		goldStandard = new GoldStandard();
		goldStandard.setUid(testUid);
		goldStandard.setKnownPmids(Arrays.asList(23456L));

		// Create ReCiterFeature
		reCiterFeature = new ReCiterFeature();
		reCiterFeature.setPersonIdentifier(testUid);
		reCiterFeature.setReCiterArticleFeatures(new ArrayList<>(articleFeatures));
		reCiterFeature.setCountSuggestedArticles(articleFeatures.size());
		reCiterFeature.setCountPendingArticles(1L);
		reCiterFeature.setPrecision(0.66);
		reCiterFeature.setRecall(0.75);
		reCiterFeature.setOverallAccuracy(0.7);

		// Create analysis output
		analysisOutput = new AnalysisOutput();
		analysisOutput.setUid(testUid);
		analysisOutput.setReCiterFeature(reCiterFeature);

		// Create analysis output list
		analysisOutputList = new ArrayList<>();
		for (String uid : testUids) {
			AnalysisOutput analysisOutput = new AnalysisOutput();
			ReCiterFeature reCiterFeature = new ReCiterFeature();
			reCiterFeature.setPersonIdentifier(uid);

			List<ReCiterArticleFeature> articleFeatures = new ArrayList<>();
			// Create articles with scores above threshold
			for (int i = 0; i < 10; i++) {
				ReCiterArticleFeature article = new ReCiterArticleFeature();
				article.setPmid((long) (10000 + i));
				article.setUserAssertion(PublicationFeedback.NULL);
				article.setAuthorshipLikelihoodScore(1.0); // Set score above threshold
				articleFeatures.add(article);
			}
			reCiterFeature.setReCiterArticleFeatures(articleFeatures);
			analysisOutput.setReCiterFeature(reCiterFeature);
			analysisOutputList.add(analysisOutput);
		}
	}

	private GoldStandard createValidGoldStandard(String uid) {

		GoldStandard goldStandard = new GoldStandard();
		goldStandard.setUid(uid);
		goldStandard.setKnownPmids(Arrays.asList(12345L, 67890L));
		goldStandard.setRejectedPmids(Arrays.asList(54321L, 98765L));
		return goldStandard;
	}

	@Test
	public final void testReCiterFeatureGeneratorGroupFilters() {
		List<String> identityInstitutions = Arrays.asList("Weill Cornell Medical College",
				"New York-Presbyterian Hospital", "Hamad Medical Corporation");
		List<String> filterInstitution = Arrays.asList("New York-Presbyterian Hospital", "MSKCC");
		List<String> identityOrgUnits = Arrays.asList("Pediatrics", "General Internal Medicine (Medicine)",
				"Doctor of Medicine");
		List<String> filterOrgUnits = Arrays.asList("Pediatrics");
		List<String> identityPersonTypes = Arrays.asList("academic", "affiliate-nyp-epic", "affiliate");
		List<String> filterPersonTypes = Arrays.asList("affiliate-nyp-epic");

		// Valid filters match
		assertFalse(Collections.disjoint(identityInstitutions, filterInstitution));
		assertFalse(Collections.disjoint(identityOrgUnits, filterOrgUnits));
		assertFalse(Collections.disjoint(identityPersonTypes, filterPersonTypes));

		if (identityPersonTypes != null && !identityPersonTypes.isEmpty()
				&& !Collections.disjoint(identityPersonTypes, filterPersonTypes) && identityInstitutions != null
				&& !identityInstitutions.isEmpty() && !Collections.disjoint(identityInstitutions, filterInstitution)
				&& identityOrgUnits != null && !identityOrgUnits.isEmpty()
				&& !Collections.disjoint(identityOrgUnits, filterOrgUnits)) {
			System.out.println("Filter for both orgunits and personType");
		}

		// No match filter
		identityOrgUnits = Arrays.asList("Pediatrics", "General Internal Medicine (Medicine)", "Doctor of Medicine");
		filterOrgUnits = Arrays.asList("Neurology");
		assertTrue(Collections.disjoint(identityOrgUnits, filterOrgUnits));
	}

	@Test
	public void testUpdateGoldStandardSuccess() {
		// Arrange
		doNothing().when(dynamoDbGoldStandardService).save(any(GoldStandard.class), eq(GoldStandardUpdateFlag.UPDATE),
				eq(testUid), any(EntryPath.class), anyInt());

		// Act
		ResponseEntity<?> response = reCiterController.updateGoldStandard(validGoldStandard,
				GoldStandardUpdateFlag.UPDATE, testUid, testUid, 0);

		// Assert
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(validGoldStandard, response.getBody());

		// Verify
		verify(dynamoDbGoldStandardService, times(1)).save(eq(validGoldStandard), eq(GoldStandardUpdateFlag.UPDATE),
				eq(testUid), any(EntryPath.class), eq(0));
	}

	@Test
	public void testUpdateGoldStandardNullFlag() {
		doNothing().when(dynamoDbGoldStandardService).save(any(GoldStandard.class), eq(GoldStandardUpdateFlag.UPDATE),
				eq(testUid), any(EntryPath.class), anyInt());

		// Act
		ResponseEntity<?> response = reCiterController.updateGoldStandard(validGoldStandard, null, testUid, testUid, 0);

		// Assert
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(validGoldStandard, response.getBody());

		// Verify
		verify(dynamoDbGoldStandardService).save(eq(validGoldStandard), eq(GoldStandardUpdateFlag.UPDATE), eq(testUid),
				any(EntryPath.class), eq(0));
	}

	@Test
	public void testUpdateGoldStandardDeleteFlag() {

		// 1. Setup the Mock correctly.
		// Notice how every single argument is wrapped in a matcher (eq or any).
		// No casting is used for the GoldStandard object.
		doNothing().when(dynamoDbGoldStandardService).save(eq(validGoldStandard), // Use eq() instead of casting List
				eq(GoldStandardUpdateFlag.DELETE), // Use eq() instead of any() if you know the exact value
				eq(testUid), any(EntryPath.class), eq(0) // Wrap raw ints in eq()
		);

		// 2. Call the controller
		ResponseEntity<?> response = reCiterController.updateGoldStandard(validGoldStandard,
				GoldStandardUpdateFlag.DELETE, testUid, testUid, 0);

		// 3. Assertions
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(validGoldStandard, response.getBody());

		// 4. Verification
		// Again, ensure every argument is wrapped in a matcher.
		verify(dynamoDbGoldStandardService, times(1)).save(eq(validGoldStandard), eq(GoldStandardUpdateFlag.DELETE),
				eq(testUid), any(EntryPath.class), eq(0));
	}

	@Test
	public void testUpdateGoldStandardRefreshFlag() {
		// Arrange
		// 1. Wrap testUid in eq()
		doNothing().when(dynamoDbGoldStandardService).save(any(GoldStandard.class), any(GoldStandardUpdateFlag.class),
				eq(testUid), any(EntryPath.class), anyInt());

		// Act
		ResponseEntity<?> response = reCiterController.updateGoldStandard(validGoldStandard,
				GoldStandardUpdateFlag.REFRESH, testUid, testUid, 0);

		// Assert
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(validGoldStandard, response.getBody());

		// Verify
		// 1. Wrap everything in eq()
		verify(dynamoDbGoldStandardService, times(1)).save(eq(validGoldStandard), eq(GoldStandardUpdateFlag.REFRESH),
				eq(testUid), any(EntryPath.class), eq(0));
	}

	@Test
	public void testUpdateGoldStandardNullGoldStandard() {
		GoldStandard goldStandard = null;
		// Act
		ResponseEntity<?> response = reCiterController.updateGoldStandard(goldStandard, GoldStandardUpdateFlag.UPDATE,
				testUid, testUid, 0);

		// Assert
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertEquals("The api requires a GoldStandard model", response.getBody());
		verify(dynamoDbGoldStandardService, never()).save(any(GoldStandard.class), any(GoldStandardUpdateFlag.class),
				eq(testUid));
	}

	@Test
	public void testUpdateGoldStandardNullUid() {
		// Arrange
		GoldStandard invalidGoldStandard = new GoldStandard();

		// Act
		ResponseEntity<?> response = reCiterController.updateGoldStandard(invalidGoldStandard,
				GoldStandardUpdateFlag.UPDATE, testUid, testUid, 0);

		// Assert
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertEquals("The api requires a valid uid to be passed with GoldStandard model", response.getBody());

		verify(dynamoDbGoldStandardService, never()).save(any(GoldStandard.class), any(GoldStandardUpdateFlag.class),
				anyString(), any(EntryPath.class), anyInt());
	}

	@Test
	public void testUpdateGoldStandardListSuccess() {
		// Arrange
		doNothing().when(dynamoDbGoldStandardService).save(eq(validGoldStandardList), eq(GoldStandardUpdateFlag.UPDATE),
				eq(testUid), any(EntryPath.class));

		// Act
		ResponseEntity<List<GoldStandard>> response = reCiterController.updateGoldStandard(validGoldStandardList,
				GoldStandardUpdateFlag.UPDATE, testUid, testUid);

		// Assert
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(validGoldStandardList, response.getBody());

		// Verify
		verify(dynamoDbGoldStandardService, times(1)).save(eq(validGoldStandardList), eq(GoldStandardUpdateFlag.UPDATE),
				eq(testUid), any(EntryPath.class));
	}

	@Test
	public void testUpdateGoldStandardListNullFlag() {
		// Arrange
		doNothing().when(dynamoDbGoldStandardService).save(eq(validGoldStandardList), eq(GoldStandardUpdateFlag.UPDATE),
				eq(testUid), any(EntryPath.class));

		// Act
		ResponseEntity<List<GoldStandard>> response = reCiterController.updateGoldStandard(validGoldStandardList, null, // We
																														// pass
																														// null
																														// to
																														// the
																														// controller
																														// here
				testUid, testUid);

		// Assert
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(validGoldStandardList, response.getBody());

		// Verify
		verify(dynamoDbGoldStandardService, times(1)).save(eq(validGoldStandardList), eq(GoldStandardUpdateFlag.UPDATE),
				eq(testUid), any(EntryPath.class));
	}

	@Test
	public void testUpdateGoldStandardListDeleteFlag() {
		// Arrange
		// 1. Remove the bad cast and use eq(validGoldStandardList)
		// 2. Wrap the flag and testUid in eq()
		doNothing().when(dynamoDbGoldStandardService).save(eq(validGoldStandardList), eq(GoldStandardUpdateFlag.DELETE),
				eq(testUid), any(EntryPath.class));

		// Act
		ResponseEntity<List<GoldStandard>> response = reCiterController.updateGoldStandard(validGoldStandardList,
				GoldStandardUpdateFlag.DELETE, testUid, testUid);

		// Assert
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(validGoldStandardList, response.getBody());

		// Verify
		// Match the setup exactly
		verify(dynamoDbGoldStandardService, times(1)).save(eq(validGoldStandardList), eq(GoldStandardUpdateFlag.DELETE),
				eq(testUid), any(EntryPath.class));
	}

	public void testUpdateGoldStandardListRefreshFlag() {
		// Arrange
		doNothing().when(dynamoDbGoldStandardService).save(eq(validGoldStandardList),
				eq(GoldStandardUpdateFlag.REFRESH), eq(testUid), any(EntryPath.class));

		// Act
		ResponseEntity<List<GoldStandard>> response = reCiterController.updateGoldStandard(validGoldStandardList,
				GoldStandardUpdateFlag.REFRESH, testUid, testUid);

		// Assert
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(validGoldStandardList, response.getBody());

		// Verify
		verify(dynamoDbGoldStandardService, times(1)).save(eq(validGoldStandardList),
				eq(GoldStandardUpdateFlag.REFRESH), eq(testUid), any(EntryPath.class));
	}

	@Test
	public void testRetrieveGoldStandardByUidSuccess() {
		// Arrange
		when(dynamoDbGoldStandardService.findByUid("test123")).thenReturn(validGoldStandard);

		// Act
		ResponseEntity<GoldStandard> response = reCiterController.retrieveGoldStandardByUid("test123");

		// Assert
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(validGoldStandard, response.getBody());
		verify(dynamoDbGoldStandardService, times(1)).findByUid("test123");
	}

	@Test
	public void testRetrieveGoldStandardByUidNotFound() {
		// Arrange
		when(dynamoDbGoldStandardService.findByUid("nonexistent")).thenReturn(null);

		// Act
		ResponseEntity<GoldStandard> response = reCiterController.retrieveGoldStandardByUid("nonexistent");

		// Assert
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNull(response.getBody());
		verify(dynamoDbGoldStandardService, times(1)).findByUid("nonexistent");
	}

	@Test
	public void testRetrieveArticlesSuccess() throws IOException {
		// Arrange
		when(identityService.findAll()).thenReturn(identityList);

		// Act
		ResponseEntity<?> response = reCiterController.retrieveArticles(RetrievalRefreshFlag.ALL_PUBLICATIONS);

		// Assert
		assertEquals(HttpStatus.OK, response.getStatusCode());
		verify(identityService, times(1)).findAll();

		// Verify that retrieveArticlesByDateRange was called with correct parameters
		verify(aliasReCiterRetrievalEngine, times(1)).retrieveArticlesByDateRange(eq(identityList), any(Date.class),
				any(Date.class), eq(RetrievalRefreshFlag.ALL_PUBLICATIONS));
	}

	@Test
	public void testRetrieveArticlesWithoutRefreshFlag() throws IOException {
		LocalDate now = LocalDate.now();
		// Arrange
		when(identityService.findAll()).thenReturn(identityList);

		LocalDate startDate = now.withDayOfMonth(1);
		LocalDate endDate = now.withDayOfMonth(now.lengthOfMonth());

		// Act
		ResponseEntity<?> response = reCiterController.retrieveArticles(null);

		// Assert
		assertEquals(HttpStatus.OK, response.getStatusCode());
		verify(identityService, times(1)).findAll();
		verify(aliasReCiterRetrievalEngine, times(1)).retrieveArticlesByDateRange(eq(identityList),
				eq(Date.valueOf(startDate)), eq(Date.valueOf(endDate)), isNull());
	}

	@Test
	public void testRetrieveArticlesWhenIOExceptionOccurs() throws IOException {
		// Arrange
		when(identityService.findAll()).thenReturn(identityList);
		doThrow(new IOException("Junit Test exception")).when(aliasReCiterRetrievalEngine)
				.retrieveArticlesByDateRange(any(), any(), any(), any());

		// Act
		ResponseEntity<?> response = reCiterController.retrieveArticles(null);

		// Assert
		assertEquals(HttpStatus.OK, response.getStatusCode());
		verify(identityService, times(1)).findAll();
		verify(aliasReCiterRetrievalEngine, times(1)).retrieveArticlesByDateRange(any(), any(), any(), any());
	}

	@Test
	public void testRetrieveArticlesWithEmptyIdentityList() throws IOException {
		// Arrange
		when(identityService.findAll()).thenReturn(new ArrayList<>());

		// Act
		ResponseEntity<?> response = reCiterController.retrieveArticles(null);

		// Assert
		assertEquals(HttpStatus.OK, response.getStatusCode());
		verify(identityService, times(1)).findAll();
		verify(aliasReCiterRetrievalEngine, times(1)).retrieveArticlesByDateRange(eq(new ArrayList<>()),
				any(Date.class), any(Date.class), isNull());
	}

	@Test
	public void testRetrieveArticlesByUidIdentityNotFound() {
		// Arrange
		when(identityService.findByUid(testUid)).thenReturn(null);

		// Act
		ResponseEntity<?> response = reCiterController.retrieveArticlesByUid(testUid, null);

		// Assert
		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		assertTrue(response.getBody().toString().contains("not found in the Identity table"));
		verify(identityService, times(1)).findByUid(testUid);
	}

	@Test
	public void testRetrieveArticlesByUidUseCache() {
		// Arrange
		when(identityService.findByUid(testUid)).thenReturn(identity);
		when(eSearchResultService.findByUid(testUid.trim())).thenReturn(testESearchResult);

		// Act
		ResponseEntity<?> response = reCiterController.retrieveArticlesByUid(testUid, RetrievalRefreshFlag.FALSE);

		// Assert
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertTrue(response.getBody().toString().contains("cached results"));
		verify(identityService, times(1)).findByUid(testUid);
		verify(eSearchResultService, times(1)).findByUid(testUid.trim());
	}

	@Test
	public void testRetrieveArticlesByUidRefreshAllPublications() throws IOException {
		// Arrange
		when(identityService.findByUid(testUid)).thenReturn(identity);
		when(eSearchResultService.findByUid(testUid.trim())).thenReturn(testESearchResult);

		// This is causing the error -
		// aliasReCiterRetrievalEngine.retrieveArticlesByDateRange likely returns a
		// value
		// Replace doNothing() with when().thenReturn() pattern
		when(aliasReCiterRetrievalEngine.retrieveArticlesByDateRange(anyList(), any(Date.class), any(Date.class),
				any(RetrievalRefreshFlag.class))).thenReturn(true);

		// If eSearchResultService.delete is void, this is fine
		doNothing().when(eSearchResultService).delete(anyString());

		// Act
		ResponseEntity<?> response = reCiterController.retrieveArticlesByUid(testUid,
				RetrievalRefreshFlag.ALL_PUBLICATIONS);

		// Assert
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertTrue(response.getBody().toString().contains("Successfully retrieved all candidate articles"));
		verify(identityService, times(1)).findByUid(testUid);
		verify(eSearchResultService, times(1)).findByUid(testUid.trim());
		verify(eSearchResultService, times(1)).delete(testUid.trim());
		verify(aliasReCiterRetrievalEngine, times(1)).retrieveArticlesByDateRange(anyList(), any(Date.class),
				any(Date.class), eq(RetrievalRefreshFlag.ALL_PUBLICATIONS));
	}

	@Test
	public void testRetrieveArticlesByUidRefreshAllPublicationsNoExistingESearchResult() throws IOException {
		// Arrange
		when(identityService.findByUid(testUid)).thenReturn(identity);
		when(eSearchResultService.findByUid(testUid.trim())).thenReturn(null);

		// Act
		ResponseEntity<?> response = reCiterController.retrieveArticlesByUid(testUid,
				RetrievalRefreshFlag.ALL_PUBLICATIONS);

		// Assert
		assertEquals(HttpStatus.OK, response.getStatusCode());
		verify(identityService, times(1)).findByUid(testUid);
		verify(eSearchResultService, times(1)).findByUid(testUid.trim());
		verify(aliasReCiterRetrievalEngine, times(1)).retrieveArticlesByDateRange(anyList(), any(Date.class),
				any(Date.class), eq(RetrievalRefreshFlag.ALL_PUBLICATIONS));
	}

	@Test
	public void testRetrieveArticlesByUidRefreshAllPublicationsFailedSweepRestoresPrior() throws IOException {
		// #737 case (g): a failed full sweep with a prior ESearchResult on record must
		// restore it, so partial per-strategy entries the engine already wrote don't
		// shrink the next run's Analysis candidate set.
		when(identityService.findByUid(testUid)).thenReturn(identity);
		when(eSearchResultService.findByUid(testUid.trim())).thenReturn(testESearchResult);
		when(aliasReCiterRetrievalEngine.retrieveArticlesByDateRange(anyList(), any(Date.class), any(Date.class),
				any(RetrievalRefreshFlag.class))).thenReturn(false);

		ResponseEntity<?> response = reCiterController.retrieveArticlesByUid(testUid,
				RetrievalRefreshFlag.ALL_PUBLICATIONS);

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
		verify(eSearchResultService, times(1)).delete(testUid.trim());
		verify(eSearchResultService, times(1)).save(testESearchResult);
	}

	@Test
	public void testRetrieveArticlesByUidRefreshAllPublicationsFailedSweepNoPriorDeletes() throws IOException {
		// #737 case (h): with no prior ESearchResult (first-ever retrieval), a failed
		// sweep deletes the partial one instead of restoring, so the next run
		// auto-upgrades to a full sweep rather than looking like a covered uid.
		when(identityService.findByUid(testUid)).thenReturn(identity);
		when(eSearchResultService.findByUid(testUid.trim())).thenReturn(null);
		when(aliasReCiterRetrievalEngine.retrieveArticlesByDateRange(anyList(), any(Date.class), any(Date.class),
				any(RetrievalRefreshFlag.class))).thenReturn(false);

		ResponseEntity<?> response = reCiterController.retrieveArticlesByUid(testUid,
				RetrievalRefreshFlag.ALL_PUBLICATIONS);

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
		verify(eSearchResultService, times(1)).delete(testUid.trim());
		verify(eSearchResultService, never()).save(any(ESearchResult.class));
	}

	@Test
	public void testRetrieveArticlesByUidRefreshAllPublicationsSuccessNeverRestores() throws IOException {
		// #737 case (i): a successful full sweep never touches restore — only the
		// initial pre-sweep delete happens.
		when(identityService.findByUid(testUid)).thenReturn(identity);
		when(eSearchResultService.findByUid(testUid.trim())).thenReturn(testESearchResult);
		when(aliasReCiterRetrievalEngine.retrieveArticlesByDateRange(anyList(), any(Date.class), any(Date.class),
				any(RetrievalRefreshFlag.class))).thenReturn(true);

		ResponseEntity<?> response = reCiterController.retrieveArticlesByUid(testUid,
				RetrievalRefreshFlag.ALL_PUBLICATIONS);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		verify(eSearchResultService, times(1)).delete(testUid.trim());
		verify(eSearchResultService, never()).save(any(ESearchResult.class));
	}

	@Test
	public void testRetrieveArticlesByUidOnlyNewPublications() throws IOException {
		// Arrange
		when(identityService.findByUid(testUid)).thenReturn(identity);
		when(eSearchResultService.findByUid(testUid.trim())).thenReturn(testESearchResult);

		// Act
		ResponseEntity<?> response = reCiterController.retrieveArticlesByUid(testUid,
				RetrievalRefreshFlag.ONLY_NEWLY_ADDED_PUBLICATIONS);

		// Assert
		assertEquals(HttpStatus.OK, response.getStatusCode());
		verify(identityService, times(1)).findByUid(testUid);
		verify(eSearchResultService, times(2)).findByUid(testUid.trim());
		verify(aliasReCiterRetrievalEngine, times(1)).retrieveArticlesByDateRange(anyList(), any(Date.class),
				any(Date.class), eq(RetrievalRefreshFlag.ONLY_NEWLY_ADDED_PUBLICATIONS));
	}

	@Test
	public void testRetrieveArticlesByUidOnlyNewPublicationsNoExistingESearchResult() {
		// Arrange
		when(identityService.findByUid(testUid)).thenReturn(identity);
		when(eSearchResultService.findByUid(testUid.trim())).thenReturn(testESearchResult);

		// Act
		ResponseEntity<?> response = reCiterController.retrieveArticlesByUid(testUid,
				RetrievalRefreshFlag.ONLY_NEWLY_ADDED_PUBLICATIONS);

		// Assert
		assertEquals(HttpStatus.OK, response.getStatusCode());
		verify(identityService, times(1)).findByUid(testUid);
		verify(eSearchResultService, times(2)).findByUid(testUid.trim());
	}

	@Test
	public void testRetrieveArticlesByUidIOExceptionDuringRetrieval() throws IOException {
		// Arrange
		when(identityService.findByUid(testUid)).thenReturn(identity);
		when(eSearchResultService.findByUid(testUid.trim())).thenReturn(null);
		doThrow(new IOException("Test retrieval exception")).when(aliasReCiterRetrievalEngine)
				.retrieveArticlesByDateRange(anyList(), any(Date.class), any(Date.class),
						any(RetrievalRefreshFlag.class));

		// Act
		ResponseEntity<?> response = reCiterController.retrieveArticlesByUid(testUid,
				RetrievalRefreshFlag.ALL_PUBLICATIONS);

		// Assert
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
		assertTrue(response.getBody().toString().contains("failed to retrieve articles"));
		verify(identityService, times(1)).findByUid(testUid);
		verify(eSearchResultService, times(1)).findByUid(testUid.trim());
		verify(aliasReCiterRetrievalEngine, times(1)).retrieveArticlesByDateRange(anyList(), any(Date.class),
				any(Date.class), any(RetrievalRefreshFlag.class));
	}

	@Test
	public void testRetrieveArticlesByUidRetrievalCrashReturns500() throws IOException {
		// A retrieval worker crashed: retrieveArticlesByDateRange reports false (a
		// successful run that found nothing still returns true). The endpoint must
		// answer with the 500 error shape, not a misleading success.
		when(identityService.findByUid(testUid)).thenReturn(identity);
		when(eSearchResultService.findByUid(testUid.trim())).thenReturn(null);
		when(aliasReCiterRetrievalEngine.retrieveArticlesByDateRange(anyList(), any(Date.class), any(Date.class),
				eq(RetrievalRefreshFlag.ALL_PUBLICATIONS))).thenReturn(false);

		// Act
		ResponseEntity<?> response = reCiterController.retrieveArticlesByUid(testUid,
				RetrievalRefreshFlag.ALL_PUBLICATIONS);

		// Assert
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
		assertTrue(response.getBody().toString().contains("failed to retrieve articles"));
		verify(aliasReCiterRetrievalEngine, times(1)).retrieveArticlesByDateRange(anyList(), any(Date.class),
				any(Date.class), eq(RetrievalRefreshFlag.ALL_PUBLICATIONS));
	}

	@Test
	public void testRetrieveArticlesByUidRetrievalCrashOnIncrementalReturns500() throws IOException {
		// Same crash gate on the ONLY_NEWLY_ADDED_PUBLICATIONS path.
		when(identityService.findByUid(testUid)).thenReturn(identity);
		when(eSearchResultService.findByUid(testUid.trim())).thenReturn(testESearchResult);
		when(aliasReCiterRetrievalEngine.retrieveArticlesByDateRange(anyList(), any(Date.class), any(Date.class),
				eq(RetrievalRefreshFlag.ONLY_NEWLY_ADDED_PUBLICATIONS))).thenReturn(false);

		// Act
		ResponseEntity<?> response = reCiterController.retrieveArticlesByUid(testUid,
				RetrievalRefreshFlag.ONLY_NEWLY_ADDED_PUBLICATIONS);

		// Assert
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
		assertTrue(response.getBody().toString().contains("failed to retrieve articles"));
		verify(aliasReCiterRetrievalEngine, times(1)).retrieveArticlesByDateRange(anyList(), any(Date.class),
				any(Date.class), eq(RetrievalRefreshFlag.ONLY_NEWLY_ADDED_PUBLICATIONS));
	}

	@Test
	public void testRetrieveArticlesByUidNullRefreshFlagNoExistingESearchResult() throws IOException {
		// Arrange
		when(identityService.findByUid(testUid)).thenReturn(identity);
		when(eSearchResultService.findByUid(testUid.trim())).thenReturn(null);

		// Act
		ResponseEntity<?> response = reCiterController.retrieveArticlesByUid(testUid, null);

		// Assert
		assertEquals(HttpStatus.OK, response.getStatusCode());
		verify(identityService, times(1)).findByUid(testUid);
		verify(eSearchResultService, times(1)).findByUid(testUid.trim());
		verify(aliasReCiterRetrievalEngine, times(1)).retrieveArticlesByDateRange(anyList(), any(Date.class),
				any(Date.class), eq(RetrievalRefreshFlag.ALL_PUBLICATIONS));
	}

	// ---- #696: staleness escalation on ONLY_NEWLY_ADDED requests ----

	private ESearchResult eSearchResultWithFullSweepDaysAgo(long daysAgo) {
		ESearchResult result = new ESearchResult();
		result.setUid(testUid);
		result.setRetrievalDate(Instant.now());
		List<ESearchPmid> entries = new ArrayList<>();
		entries.add(new ESearchPmid(Arrays.asList(1L, 2L), "EmailRetrievalStrategy",
				Instant.now().minus(daysAgo, ChronoUnit.DAYS),
				ESearchPmid.RetrievalRefreshFlag.ALL_PUBLICATIONS));
		result.setESearchPmids(entries);
		return result;
	}

	@Test
	public void testEscalatesWhenInferredLastSweepExceedsMaxAge() throws IOException {
		// No persisted stamp yet (pre-A2 record): the decision falls back to the
		// lookupType inference, which shows a sweep 120 days ago against a 90-day max.
		ReflectionTestUtils.setField(reCiterController, "fullSweepJitterDays", 0);
		when(identityService.findByUid(testUid)).thenReturn(identity);
		when(eSearchResultService.findByUid(testUid.trim())).thenReturn(eSearchResultWithFullSweepDaysAgo(120));
		when(eSearchResultService.findLastFullSweep(testUid.trim())).thenReturn(null);

		ResponseEntity<?> response = reCiterController.retrieveArticlesByUid(testUid,
				RetrievalRefreshFlag.ONLY_NEWLY_ADDED_PUBLICATIONS, 90);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		verify(aliasReCiterRetrievalEngine, times(1)).retrieveArticlesByDateRange(anyList(), any(Date.class),
				any(Date.class), eq(RetrievalRefreshFlag.ALL_PUBLICATIONS));
		// Escalation refreshes the item in place; only the manual ALL path deletes it.
		verify(eSearchResultService, never()).delete(anyString());
		assertEquals("true", response.getHeaders().getFirst("X-Reciter-Escalated"));
		assertEquals("ALL_PUBLICATIONS", response.getHeaders().getFirst("X-Reciter-Retrieval-Mode"));
		assertEquals("false", response.getHeaders().getFirst("X-Reciter-Auto-Upgraded"));
	}

	@Test
	public void testDoesNotEscalateBelowMaxAge() throws IOException {
		ReflectionTestUtils.setField(reCiterController, "fullSweepJitterDays", 0);
		when(identityService.findByUid(testUid)).thenReturn(identity);
		when(eSearchResultService.findByUid(testUid.trim())).thenReturn(eSearchResultWithFullSweepDaysAgo(30));
		when(eSearchResultService.findLastFullSweep(testUid.trim())).thenReturn(null);

		ResponseEntity<?> response = reCiterController.retrieveArticlesByUid(testUid,
				RetrievalRefreshFlag.ONLY_NEWLY_ADDED_PUBLICATIONS, 90);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		verify(aliasReCiterRetrievalEngine, times(1)).retrieveArticlesByDateRange(anyList(), any(Date.class),
				any(Date.class), eq(RetrievalRefreshFlag.ONLY_NEWLY_ADDED_PUBLICATIONS));
		assertEquals("false", response.getHeaders().getFirst("X-Reciter-Escalated"));
		assertEquals("ONLY_NEWLY_ADDED_PUBLICATIONS", response.getHeaders().getFirst("X-Reciter-Retrieval-Mode"));
	}

	@Test
	public void testPersistedStampIsPreferredOverStaleInference() throws IOException {
		// Entries claim a 400-day-old sweep, but the persisted stamp (written by a
		// clean sweep) says 10 days ago. The stamp wins: no escalation.
		ReflectionTestUtils.setField(reCiterController, "fullSweepJitterDays", 0);
		when(identityService.findByUid(testUid)).thenReturn(identity);
		when(eSearchResultService.findByUid(testUid.trim())).thenReturn(eSearchResultWithFullSweepDaysAgo(400));
		when(eSearchResultService.findLastFullSweep(testUid.trim()))
				.thenReturn(Instant.now().minus(10, ChronoUnit.DAYS));

		ResponseEntity<?> response = reCiterController.retrieveArticlesByUid(testUid,
				RetrievalRefreshFlag.ONLY_NEWLY_ADDED_PUBLICATIONS, 90);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		verify(aliasReCiterRetrievalEngine, times(1)).retrieveArticlesByDateRange(anyList(), any(Date.class),
				any(Date.class), eq(RetrievalRefreshFlag.ONLY_NEWLY_ADDED_PUBLICATIONS));
		assertEquals("false", response.getHeaders().getFirst("X-Reciter-Escalated"));
	}

	@Test
	public void testStalePersistedStampEscalatesEvenWhenEntriesLookFresh() throws IOException {
		// The inverse: incremental runs keep re-stamping entries (E13), so entries can
		// look fresh while the real last full sweep is 200 days old. The stamp wins.
		ReflectionTestUtils.setField(reCiterController, "fullSweepJitterDays", 0);
		when(identityService.findByUid(testUid)).thenReturn(identity);
		when(eSearchResultService.findByUid(testUid.trim())).thenReturn(eSearchResultWithFullSweepDaysAgo(5));
		when(eSearchResultService.findLastFullSweep(testUid.trim()))
				.thenReturn(Instant.now().minus(200, ChronoUnit.DAYS));

		ResponseEntity<?> response = reCiterController.retrieveArticlesByUid(testUid,
				RetrievalRefreshFlag.ONLY_NEWLY_ADDED_PUBLICATIONS, 90);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		verify(aliasReCiterRetrievalEngine, times(1)).retrieveArticlesByDateRange(anyList(), any(Date.class),
				any(Date.class), eq(RetrievalRefreshFlag.ALL_PUBLICATIONS));
		assertEquals("true", response.getHeaders().getFirst("X-Reciter-Escalated"));
	}

	@Test
	public void testParameterlessRequestNeverEscalates() throws IOException {
		// A uid that has NEVER been fully swept, on a request without fullSweepMaxAgeDays
		// (Publication Manager, ad-hoc callers): must stay incremental — and must not
		// even read the stamp, since the decision is disabled.
		when(identityService.findByUid(testUid)).thenReturn(identity);
		ESearchResult neverSwept = eSearchResultWithFullSweepDaysAgo(2000);
		when(eSearchResultService.findByUid(testUid.trim())).thenReturn(neverSwept);

		ResponseEntity<?> response = reCiterController.retrieveArticlesByUid(testUid,
				RetrievalRefreshFlag.ONLY_NEWLY_ADDED_PUBLICATIONS);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		verify(aliasReCiterRetrievalEngine, times(1)).retrieveArticlesByDateRange(anyList(), any(Date.class),
				any(Date.class), eq(RetrievalRefreshFlag.ONLY_NEWLY_ADDED_PUBLICATIONS));
		verify(eSearchResultService, never()).findLastFullSweep(anyString());
		assertEquals("false", response.getHeaders().getFirst("X-Reciter-Escalated"));
	}

	@Test
	public void testZeroMaxAgeNeverEscalates() throws IOException {
		when(identityService.findByUid(testUid)).thenReturn(identity);
		when(eSearchResultService.findByUid(testUid.trim())).thenReturn(eSearchResultWithFullSweepDaysAgo(2000));

		ResponseEntity<?> response = reCiterController.retrieveArticlesByUid(testUid,
				RetrievalRefreshFlag.ONLY_NEWLY_ADDED_PUBLICATIONS, 0);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		verify(aliasReCiterRetrievalEngine, times(1)).retrieveArticlesByDateRange(anyList(), any(Date.class),
				any(Date.class), eq(RetrievalRefreshFlag.ONLY_NEWLY_ADDED_PUBLICATIONS));
		verify(eSearchResultService, never()).findLastFullSweep(anyString());
		assertEquals("false", response.getHeaders().getFirst("X-Reciter-Escalated"));
	}

	@Test
	public void testRetrieveBulkFeatureGeneratorWithUids() {
		// Arrange
		when(analysisService.findByUids(testUids)).thenReturn(analysisOutputList);

		// Act
		ResponseEntity<?> response = reCiterController.retrieveBulkFeatureGenerator(testUids, null, null, null,
				testScore, maxArticles);

		// Assert
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNotNull(response.getBody());
		assertTrue(response.getBody() instanceof List);
		List<ReCiterFeature> resultFeatures = (List<ReCiterFeature>) response.getBody();
		assertEquals(2, resultFeatures.size());
		// Verify each feature has max 5 articles (as specified by maxArticles)
		for (ReCiterFeature feature : resultFeatures) {
			assertTrue(feature.getReCiterArticleFeatures().size() <= maxArticles);
		}
		verify(analysisService, times(1)).findByUids(testUids);
	}

	@Test
	public void testRetrieveBulkFeatureGeneratorBadRequest() {
		// Act
		ResponseEntity<?> response = reCiterController.retrieveBulkFeatureGenerator(null, null, null, null, testScore,
				maxArticles);

		// Assert
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		verify(identityService, never()).findAll();
		verify(analysisService, never()).findByUids(anyList());
	}

	@Test
	public void testRetrieveBulkFeatureGeneratorTooManyUids() {
		// Arrange
		List<String> tooManyUids = new ArrayList<>();
		for (int i = 0; i < 51; i++) {
			tooManyUids.add("test" + i);
		}

		// Act
		ResponseEntity<?> response = reCiterController.retrieveBulkFeatureGenerator(tooManyUids, null, null, null,
				testScore, maxArticles);

		// Assert
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		verify(analysisService, never()).findByUids(anyList());
	}

	@Test
	public void testRetrieveBulkFeatureGeneratorEmptyIdentities() {
		// Arrange
		when(identityService.findAll()).thenReturn(Collections.emptyList());

		// Act
		ResponseEntity<?> response = reCiterController.retrieveBulkFeatureGenerator(null, personTypes, null, null,
				testScore, maxArticles);

		// Assert
		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		verify(identityService, times(1)).findAll();
		verify(analysisService, never()).findByUids(anyList());
	}

	@Test
	public void testRetrieveBulkFeatureGeneratorNoPublicationsFound() {
		// Arrange
		when(analysisService.findByUids(testUids)).thenReturn(Collections.emptyList());

		// Act
		ResponseEntity<?> response = reCiterController.retrieveBulkFeatureGenerator(testUids, null, null, null,
				testScore, maxArticles);

		// Assert
		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		assertTrue(response.getBody().toString().contains("There is no publications data for the group"));
		verify(analysisService, times(1)).findByUids(testUids);
	}

	@Test
	public void testRetrieveBulkFeatureGeneratorServiceException() {
		// Arrange
		when(identityService.findAll()).thenThrow(new RuntimeException("Database error"));

		// Act
		ResponseEntity<?> response = reCiterController.retrieveBulkFeatureGenerator(null, personTypes, null, null,
				testScore, maxArticles);

		// Assert
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
		assertTrue(response.getBody().toString().contains("Issue with the request"));
	}

	@Test
	public void testRunFeatureGeneratorNoIdentityFound() {
		// Arrange
		String uid = "test989";
		when(identityService.findByUid(uid)).thenReturn(null);

		// Act
		ResponseEntity<?> response = reCiterController.runFeatureGenerator(uid, null, null, null, false, null, null, false);

		// Assert
		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		assertEquals("The uid provided '" + uid + "' was not found in the Identity table", response.getBody());
		verify(identityService, times(1)).findByUid(uid);
	}

	@Test
	public void testRunFeatureGeneratorAutoUpgradeRetrievalCrashReturns404AndNeverSavesAnalysis() throws IOException {
		// The uid has no ESearchResult, so initializeEngineParameters auto-upgrades to a
		// full ALL_PUBLICATIONS retrieval — which crashes (retrieveArticlesByDateRange
		// reports false → 500 from retrieveArticlesByUid). Feature generation must bail
		// out to the 404, and the Analysis row must NOT be overwritten with a
		// zero-feature result built from the empty candidate set.
		when(identityService.findByUid(testUid)).thenReturn(identity);
		when(analysisService.findByUid(testUid.trim())).thenReturn(null);
		when(eSearchResultService.findByUid(testUid.trim())).thenReturn(null);
		when(aliasReCiterRetrievalEngine.retrieveArticlesByDateRange(anyList(), any(Date.class), any(Date.class),
				eq(RetrievalRefreshFlag.ALL_PUBLICATIONS))).thenReturn(false);

		// Act
		ResponseEntity<?> response = reCiterController.runFeatureGenerator(testUid, testScore,
				UseGoldStandard.AS_EVIDENCE, FilterFeedbackType.ALL, false, null, null, false);

		// Assert
		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		assertTrue(response.getBody().toString().contains("does not have any candidate records"));
		verify(aliasReCiterRetrievalEngine, times(1)).retrieveArticlesByDateRange(anyList(), any(Date.class),
				any(Date.class), eq(RetrievalRefreshFlag.ALL_PUBLICATIONS));
		verify(analysisService, never()).save(any(AnalysisOutput.class));
	}

	@Test
	public void testRunFeatureGeneratorExistingAnalysisNoRefresh() {
		// Arrange
		when(identityService.findByUid(testUid)).thenReturn(identity);
		when(analysisService.findByUid(testUid)).thenReturn(analysisOutput);
		when(dynamoDbGoldStandardService.findByUid(testUid)).thenReturn(goldStandard);

		// Act
		ResponseEntity<?> response = reCiterController.runFeatureGenerator(testUid, testScore,
				UseGoldStandard.AS_EVIDENCE, FilterFeedbackType.ALL, false, null, null, false);

		// Assert
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNotNull(response.getBody());
		ReCiterFeature result = (ReCiterFeature) response.getBody();

		// Should include articles that are:
		// 1. Above score threshold with NULL feedback OR
		// 2. Any score with ACCEPTED feedback OR
		// 3. Any score with REJECTED feedback
		assertEquals(4, result.getReCiterArticleFeatures().size()); // in for loop one also +1 total 1+3

		verify(identityService, times(1)).findByUid(testUid);
		verify(analysisService, times(1)).findByUid(testUid);
		verify(dynamoDbGoldStandardService, times(1)).findByUid(testUid);
	}

	@Test
	public void testRunFeatureGeneratorExistingAnalysisAcceptedOnly() {
		// Arrange
		when(identityService.findByUid(testUid)).thenReturn(identity);
		when(analysisService.findByUid(testUid)).thenReturn(analysisOutput);
		when(dynamoDbGoldStandardService.findByUid(testUid)).thenReturn(goldStandard);

		// Act
		ResponseEntity<?> response = reCiterController.runFeatureGenerator(testUid, testScore,
				UseGoldStandard.AS_EVIDENCE, FilterFeedbackType.ACCEPTED_ONLY, false, null, null, false);

		// Assert
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNotNull(response.getBody());
		ReCiterFeature result = (ReCiterFeature) response.getBody();

		assertEquals(1, result.getReCiterArticleFeatures().size());
		assertEquals(PublicationFeedback.ACCEPTED, result.getReCiterArticleFeatures().get(0).getUserAssertion());
		assertEquals(23456L, result.getReCiterArticleFeatures().get(0).getPmid());
	}

	@Test
	public void testRunFeatureGeneratorExistingAnalysisRejectedOnly() {
		// Arrange
		when(identityService.findByUid(testUid)).thenReturn(identity);
		when(analysisService.findByUid(testUid)).thenReturn(analysisOutput);
		when(dynamoDbGoldStandardService.findByUid(testUid)).thenReturn(goldStandard);

		// Act
		ResponseEntity<?> response = reCiterController.runFeatureGenerator(testUid, testScore,
				UseGoldStandard.AS_EVIDENCE, FilterFeedbackType.REJECTED_ONLY, false, null, null, false);

		// Assert
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNotNull(response.getBody());
		ReCiterFeature result = (ReCiterFeature) response.getBody();

		assertEquals(1, result.getReCiterArticleFeatures().size());
		assertEquals(PublicationFeedback.REJECTED, result.getReCiterArticleFeatures().get(0).getUserAssertion());
		assertEquals(34567L, result.getReCiterArticleFeatures().get(0).getPmid());
	}

	@Test
	public void testRunFeatureGeneratorExistingAnalysisAcceptedAndNull() {
		// Arrange
		when(identityService.findByUid(testUid)).thenReturn(identity);
		when(analysisService.findByUid(testUid)).thenReturn(analysisOutput);
		when(dynamoDbGoldStandardService.findByUid(testUid)).thenReturn(goldStandard);

		// Act
		ResponseEntity<?> response = reCiterController.runFeatureGenerator(testUid, testScore,
				UseGoldStandard.AS_EVIDENCE, FilterFeedbackType.ACCEPTED_AND_NULL, false, null, null, false);

		// Assert
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNotNull(response.getBody());
		ReCiterFeature result = (ReCiterFeature) response.getBody();

		// Should include articles that are:
		// 1. Above score threshold with NULL feedback OR here two null so 2+1
		// 2. Any score with ACCEPTED feedback
		assertEquals(3, result.getReCiterArticleFeatures().size());

		// Verify the PMIDs of included articles
		List<Long> pmids = new ArrayList<>();
		for (ReCiterArticleFeature article : result.getReCiterArticleFeatures()) {
			pmids.add(article.getPmid());
		}

		assertTrue(pmids.contains(12345L)); // NULL feedback above threshold
		assertTrue(pmids.contains(23456L)); // ACCEPTED feedback
		assertTrue(pmids.contains(45678L));// NULL feedback above threshold
	}

	@Test
	public void testRunFeatureGeneratorExistingAnalysisRejectedAndNull() {
		// Arrange
		when(identityService.findByUid(testUid)).thenReturn(identity);
		when(analysisService.findByUid(testUid)).thenReturn(analysisOutput);
		when(dynamoDbGoldStandardService.findByUid(testUid)).thenReturn(goldStandard);

		// Act
		ResponseEntity<?> response = reCiterController.runFeatureGenerator(testUid, testScore,
				UseGoldStandard.AS_EVIDENCE, FilterFeedbackType.REJECTED_AND_NULL, false, null, null, false);

		// Assert
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNotNull(response.getBody());
		ReCiterFeature result = (ReCiterFeature) response.getBody();

		// Should include articles that are:
		// 1. Above score threshold with NULL feedback OR
		// 2. Any score with REJECTED feedback
		assertEquals(3, result.getReCiterArticleFeatures().size());

		// Verify the PMIDs of included articles
		List<Long> pmids = new ArrayList<>();
		for (ReCiterArticleFeature article : result.getReCiterArticleFeatures()) {
			pmids.add(article.getPmid());
		}

		assertTrue(pmids.contains(45678L)); // NULL feedback above threshold
		assertTrue(pmids.contains(12345L)); // NULL feedback above threshold
		assertTrue(pmids.contains(34567L)); // REJECTED feedback
	}

	@Test
	public void testRunFeatureGeneratorExistingAnalysisAcceptedAndRejected() {
		// Arrange
		when(identityService.findByUid(testUid)).thenReturn(identity);
		when(analysisService.findByUid(testUid)).thenReturn(analysisOutput);
		when(dynamoDbGoldStandardService.findByUid(testUid)).thenReturn(goldStandard);

		// Act
		ResponseEntity<?> response = reCiterController.runFeatureGenerator(testUid, testScore,
				UseGoldStandard.AS_EVIDENCE, FilterFeedbackType.ACCEPTED_AND_REJECTED, false, null, null, false);

		// Assert
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNotNull(response.getBody());
		ReCiterFeature result = (ReCiterFeature) response.getBody();

		// Should include articles with ACCEPTED or REJECTED feedback
		assertEquals(2, result.getReCiterArticleFeatures().size());

		// Verify the PMIDs of included articles
		List<Long> pmids = new ArrayList<>();
		for (ReCiterArticleFeature article : result.getReCiterArticleFeatures()) {
			pmids.add(article.getPmid());
		}

		assertTrue(pmids.contains(23456L)); // ACCEPTED feedback
		assertTrue(pmids.contains(34567L)); // REJECTED feedback
	}

	@Test
	public void testRunFeatureGeneratorExistingAnalysisNullOnly() {
		// Arrange
		when(identityService.findByUid(testUid)).thenReturn(identity);
		when(analysisService.findByUid(testUid)).thenReturn(analysisOutput);
		when(dynamoDbGoldStandardService.findByUid(testUid)).thenReturn(goldStandard);

		// Act
		ResponseEntity<?> response = reCiterController.runFeatureGenerator(testUid, testScore,
				UseGoldStandard.AS_EVIDENCE, FilterFeedbackType.NULL, false, null, null, false);

		// Assert
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNotNull(response.getBody());
		ReCiterFeature result = (ReCiterFeature) response.getBody();

		// Should include only articles with NULL feedback and score above threshold
		assertEquals(2, result.getReCiterArticleFeatures().size());
		assertEquals(12345L, result.getReCiterArticleFeatures().get(0).getPmid());
		assertEquals(PublicationFeedback.NULL, result.getReCiterArticleFeatures().get(0).getUserAssertion());
	}

	@Test
	public void testRunArticleRetrievalByUidIdentityNotFound() {
		// Arrange
		when(identityService.findByUid(anyString())).thenReturn(null);

		// Act
		ResponseEntity<ReCiterFeature> response = reCiterController.runArticleRetrievalByUid(testUid, null, null);

		// Assert
		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		assertEquals("The uid provided '" + testUid + "' was not found in the Identity table", response.getBody());
		verify(identityService, times(1)).findByUid(testUid);
	}

	@Test
	public void testRunArticleRetrievalByUidNullPointerException() {
		// Arrange
		when(identityService.findByUid(anyString())).thenThrow(new NullPointerException());

		// Act
		ResponseEntity<ReCiterFeature> response = reCiterController.runArticleRetrievalByUid(testUid, null, null);

		// Assert
		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		assertEquals("The uid provided '" + testUid + "' was not found in the Identity table", response.getBody());
		verify(identityService, times(1)).findByUid(testUid);
	}

	@Test
	public void testRunArticleRetrievalByUidNoAnalysisDataFound() {
		// Arrange
		when(identityService.findByUid(anyString())).thenReturn(identity);
		when(analysisService.findByUid(anyString())).thenReturn(null);

		// Act
		ResponseEntity<ReCiterFeature> response = reCiterController.runArticleRetrievalByUid(testUid, null, null);

		// Assert
		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		assertEquals("There is no publications data for uid " + testUid
				+ ". Please wait while feature-generator re-runs tonight.", response.getBody());
		verify(identityService, times(1)).findByUid(testUid);
		verify(analysisService, times(1)).findByUid(testUid.trim());
	}

	@Test
	public void testRunArticleRetrievalByUidDefaultFilter() {
		// Arrange
		when(identityService.findByUid(anyString())).thenReturn(identity);
		when(analysisService.findByUid(anyString())).thenReturn(analysisOutput);

		// Expected results: articles with score >= threshold and NULL feedback, or any
		// feedback (ACCEPTED or REJECTED)
		List<ReCiterArticleFeature> expectedFeatures = articleFeatures.stream()
				.filter(article -> (article.getAuthorshipLikelihoodScore() >= defaultScoreThreshold
						&& article.getUserAssertion() == PublicationFeedback.NULL)
						|| article.getUserAssertion() == PublicationFeedback.ACCEPTED
						|| article.getUserAssertion() == PublicationFeedback.REJECTED)
				.collect(Collectors.toList());

		// Act
		ResponseEntity<ReCiterFeature> response = (ResponseEntity<ReCiterFeature>) reCiterController
				.runArticleRetrievalByUid(testUid, null, null);

		// Assert
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals(expectedFeatures.size(), response.getBody().getReCiterArticleFeatures().size());

		// Verify evidence field is null
		for (ReCiterArticleFeature feature : response.getBody().getReCiterArticleFeatures()) {
			assertEquals(null, feature.getEvidence());
		}

		// Verify metrics fields are null
		assertEquals(null, response.getBody().getInGoldStandardButNotRetrieved());
		assertEquals(null, response.getBody().getPrecision());
		assertEquals(null, response.getBody().getRecall());
		assertEquals(null, response.getBody().getOverallAccuracy());

		verify(identityService, times(1)).findByUid(testUid);
		verify(analysisService, times(1)).findByUid(testUid.trim());
	}

	@Test
	public void testRunArticleRetrievalByUidCustomScoreThreshold() {
		// Arrange
		Double customScoreThreshold = 0.5;
		when(identityService.findByUid(anyString())).thenReturn(identity);
		when(analysisService.findByUid(anyString())).thenReturn(analysisOutput);

		// Expected results: articles with score >= customScoreThreshold and NULL
		// feedback, or any feedback (ACCEPTED or REJECTED)
		List<ReCiterArticleFeature> expectedFeatures = articleFeatures.stream()
				.filter(article -> (article.getAuthorshipLikelihoodScore() >= customScoreThreshold
						&& article.getUserAssertion() == PublicationFeedback.NULL)
						|| article.getUserAssertion() == PublicationFeedback.ACCEPTED
						|| article.getUserAssertion() == PublicationFeedback.REJECTED)
				.collect(Collectors.toList());

		// Act
		ResponseEntity<ReCiterFeature> response = (ResponseEntity<ReCiterFeature>) reCiterController
				.runArticleRetrievalByUid(testUid, customScoreThreshold, null);

		// Assert
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals(expectedFeatures.size(), response.getBody().getReCiterArticleFeatures().size());

		verify(identityService, times(1)).findByUid(testUid);
		verify(analysisService, times(1)).findByUid(testUid.trim());
	}

	@Test
	public void testRunArticleRetrievalByUidFilterAcceptedOnly() {
		// Arrange
		when(identityService.findByUid(anyString())).thenReturn(identity);
		when(analysisService.findByUid(anyString())).thenReturn(analysisOutput);

		// Expected results: only ACCEPTED articles
		List<ReCiterArticleFeature> expectedFeatures = articleFeatures.stream()
				.filter(article -> article.getUserAssertion() == PublicationFeedback.ACCEPTED)
				.collect(Collectors.toList());

		// Act
		ResponseEntity<ReCiterFeature> response = (ResponseEntity<ReCiterFeature>) reCiterController
				.runArticleRetrievalByUid(testUid, null, FilterFeedbackType.ACCEPTED_ONLY);

		// Assert
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals(expectedFeatures.size(), response.getBody().getReCiterArticleFeatures().size());

		// Verify all articles have ACCEPTED feedback
		for (ReCiterArticleFeature feature : response.getBody().getReCiterArticleFeatures()) {
			assertEquals(PublicationFeedback.ACCEPTED, feature.getUserAssertion());
		}

		verify(identityService, times(1)).findByUid(testUid);
		verify(analysisService, times(1)).findByUid(testUid.trim());
	}

	@Test
	public void testRunArticleRetrievalByUidFilterRejectedOnly() {
		// Arrange
		when(identityService.findByUid(anyString())).thenReturn(identity);
		when(analysisService.findByUid(anyString())).thenReturn(analysisOutput);

		// Expected results: only REJECTED articles
		List<ReCiterArticleFeature> expectedFeatures = articleFeatures.stream()
				.filter(article -> article.getUserAssertion() == PublicationFeedback.REJECTED)
				.collect(Collectors.toList());

		// Act
		ResponseEntity<ReCiterFeature> response = (ResponseEntity<ReCiterFeature>) reCiterController
				.runArticleRetrievalByUid(testUid, null, FilterFeedbackType.REJECTED_ONLY);

		// Assert
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals(expectedFeatures.size(), response.getBody().getReCiterArticleFeatures().size());

		// Verify all articles have REJECTED feedback
		for (ReCiterArticleFeature feature : response.getBody().getReCiterArticleFeatures()) {
			assertEquals(PublicationFeedback.REJECTED, feature.getUserAssertion());
		}

		verify(identityService, times(1)).findByUid(testUid);
		verify(analysisService, times(1)).findByUid(testUid.trim());
	}

	@Test
	public void testRunArticleRetrievalByUidFilterAcceptedAndNull() {
		// Arrange
		when(identityService.findByUid(anyString())).thenReturn(identity);
		when(analysisService.findByUid(anyString())).thenReturn(analysisOutput);

		// Expected results: articles with score >= threshold and NULL feedback, or
		// ACCEPTED
		List<ReCiterArticleFeature> expectedFeatures = articleFeatures.stream()
				.filter(article -> (article.getAuthorshipLikelihoodScore() >= defaultScoreThreshold
						&& article.getUserAssertion() == PublicationFeedback.NULL)
						|| article.getUserAssertion() == PublicationFeedback.ACCEPTED)
				.collect(Collectors.toList());

		// Act
		ResponseEntity<ReCiterFeature> response = (ResponseEntity<ReCiterFeature>) reCiterController
				.runArticleRetrievalByUid(testUid, null, FilterFeedbackType.ACCEPTED_AND_NULL);

		// Assert
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals(expectedFeatures.size(), response.getBody().getReCiterArticleFeatures().size());

		verify(identityService, times(1)).findByUid(testUid);
		verify(analysisService, times(1)).findByUid(testUid.trim());
	}

	@Test
	public void testRunArticleRetrievalByUidFilterRejectedAndNull() {
		// Arrange
		when(identityService.findByUid(anyString())).thenReturn(identity);
		when(analysisService.findByUid(anyString())).thenReturn(analysisOutput);

		// Expected results: articles with score >= threshold and NULL feedback, or
		// REJECTED
		List<ReCiterArticleFeature> expectedFeatures = articleFeatures.stream()
				.filter(article -> (article.getAuthorshipLikelihoodScore() >= defaultScoreThreshold
						&& article.getUserAssertion() == PublicationFeedback.NULL)
						|| article.getUserAssertion() == PublicationFeedback.REJECTED)
				.collect(Collectors.toList());

		// Act
		ResponseEntity<ReCiterFeature> response = (ResponseEntity<ReCiterFeature>) reCiterController
				.runArticleRetrievalByUid(testUid, null, FilterFeedbackType.REJECTED_AND_NULL);

		// Assert
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals(expectedFeatures.size(), response.getBody().getReCiterArticleFeatures().size());

		verify(identityService, times(1)).findByUid(testUid);
		verify(analysisService, times(1)).findByUid(testUid.trim());
	}

	@Test
	public void testRunArticleRetrievalByUidFilterAcceptedAndRejected() {
		// Arrange
		when(identityService.findByUid(anyString())).thenReturn(identity);
		when(analysisService.findByUid(anyString())).thenReturn(analysisOutput);

		// Expected results: only ACCEPTED or REJECTED articles
		List<ReCiterArticleFeature> expectedFeatures = articleFeatures.stream()
				.filter(article -> article.getUserAssertion() == PublicationFeedback.ACCEPTED
						|| article.getUserAssertion() == PublicationFeedback.REJECTED)
				.collect(Collectors.toList());

		// Act
		ResponseEntity<ReCiterFeature> response = (ResponseEntity<ReCiterFeature>) reCiterController
				.runArticleRetrievalByUid(testUid, null, FilterFeedbackType.ACCEPTED_AND_REJECTED);

		// Assert
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals(expectedFeatures.size(), response.getBody().getReCiterArticleFeatures().size());

		verify(identityService, times(1)).findByUid(testUid);
		verify(analysisService, times(1)).findByUid(testUid.trim());
	}

	@Test
	public void testRunArticleRetrievalByUidFilterNull() {
		// Arrange
		when(identityService.findByUid(anyString())).thenReturn(identity);
		when(analysisService.findByUid(anyString())).thenReturn(analysisOutput);

		// Expected results: only NULL feedback articles with score >= threshold
		List<ReCiterArticleFeature> expectedFeatures = articleFeatures.stream()
				.filter(article -> article.getAuthorshipLikelihoodScore() >= defaultScoreThreshold
						&& article.getUserAssertion() == PublicationFeedback.NULL)
				.collect(Collectors.toList());

		// Act
		ResponseEntity<ReCiterFeature> response = (ResponseEntity<ReCiterFeature>) reCiterController
				.runArticleRetrievalByUid(testUid, null, FilterFeedbackType.NULL);

		// Assert
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals(expectedFeatures.size(), response.getBody().getReCiterArticleFeatures().size());

		// Verify all articles have NULL feedback
		for (ReCiterArticleFeature feature : response.getBody().getReCiterArticleFeatures()) {
			assertEquals(PublicationFeedback.NULL, feature.getUserAssertion());
		}

		verify(identityService, times(1)).findByUid(testUid);
		verify(analysisService, times(1)).findByUid(testUid.trim());
	}

	@Test
	public void testRunArticleRetrievalByUidAllFilterType() {
		// Arrange
		when(identityService.findByUid(anyString())).thenReturn(identity);
		when(analysisService.findByUid(anyString())).thenReturn(analysisOutput);

		// Expected results: same as default filter - articles with score >= threshold
		// and NULL feedback, or any feedback
		List<ReCiterArticleFeature> expectedFeatures = articleFeatures.stream()
				.filter(article -> (article.getAuthorshipLikelihoodScore() >= defaultScoreThreshold
						&& article.getUserAssertion() == PublicationFeedback.NULL)
						|| article.getUserAssertion() == PublicationFeedback.ACCEPTED
						|| article.getUserAssertion() == PublicationFeedback.REJECTED)
				.collect(Collectors.toList());

		// Act
		ResponseEntity<ReCiterFeature> response = (ResponseEntity<ReCiterFeature>) reCiterController
				.runArticleRetrievalByUid(testUid, null, FilterFeedbackType.ALL);

		// Assert
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals(expectedFeatures.size(), response.getBody().getReCiterArticleFeatures().size());

		verify(identityService, times(1)).findByUid(testUid);
		verify(analysisService, times(1)).findByUid(testUid.trim());
	}


	@Test
	public void testDropMalformedAlternateNamesFiltersNullNameParts() {
		Identity identity = new Identity();
		identity.setUid("kns2002");
		reciter.model.identity.AuthorName good = new reciter.model.identity.AuthorName("Kristina", "Nori", "Shigyo");
		reciter.model.identity.AuthorName noFirst = new reciter.model.identity.AuthorName();
		noFirst.setLastName("Shigyo");
		identity.setAlternateNames(new ArrayList<>(Arrays.asList(noFirst, good, null)));

		ReCiterController.dropMalformedAlternateNames(identity);

		assertEquals(1, identity.getAlternateNames().size());
		assertEquals("Shigyo", identity.getAlternateNames().get(0).getLastName());
		assertEquals("Kristina", identity.getAlternateNames().get(0).getFirstName());
	}

	@Test
	public void testDropMalformedAlternateNamesLeavesValidNamesAndNullsAlone() {
		Identity identity = new Identity();
		identity.setUid("abc1001");
		List<reciter.model.identity.AuthorName> names = new ArrayList<>(Arrays.asList(
				new reciter.model.identity.AuthorName("Jane", null, "Doe")));
		identity.setAlternateNames(names);

		ReCiterController.dropMalformedAlternateNames(identity);
		assertEquals(names, identity.getAlternateNames());

		identity.setAlternateNames(null);
		ReCiterController.dropMalformedAlternateNames(identity);
		assertNull(identity.getAlternateNames());

		ReCiterController.dropMalformedAlternateNames(null);
	}

	// ---- #752: a gold-standard write patches userAssertion on the stored Analysis ----

	private static ReCiterArticleFeature storedArticle(long pmid, PublicationFeedback assertion) {
		ReCiterArticleFeature article = new ReCiterArticleFeature();
		article.setPmid(pmid);
		article.setUserAssertion(assertion);
		return article;
	}

	private static AnalysisOutput storedAnalysis(String uid, boolean usingS3, ReCiterArticleFeature... articles) {
		ReCiterFeature feature = new ReCiterFeature();
		feature.setReCiterArticleFeatures(new ArrayList<>(Arrays.asList(articles)));
		AnalysisOutput analysis = new AnalysisOutput();
		analysis.setUid(uid);
		analysis.setUsingS3(usingS3);
		analysis.setReCiterFeature(feature);
		return analysis;
	}

	@Test
	public void goldStandardWritePatchesStoredAnalysisAssertions() {
		// The post-write gold standard, not the request body, is the source: UPDATE bodies
		// are deltas. Pmid 2 is in both lists and must come out REJECTED, as
		// Analysis.performAnalysis resolves it; pmid 4 was un-accepted and must clear.
		GoldStandard stored = new GoldStandard();
		stored.setUid(testUid);
		stored.setKnownPmids(Arrays.asList(1L, 2L));
		stored.setRejectedPmids(Arrays.asList(2L, 3L));
		when(dynamoDbGoldStandardService.findByUid(testUid)).thenReturn(stored);
		AnalysisOutput analysis = storedAnalysis(testUid, true,
				storedArticle(1L, PublicationFeedback.NULL),
				storedArticle(2L, PublicationFeedback.ACCEPTED),
				storedArticle(3L, PublicationFeedback.NULL),
				storedArticle(4L, PublicationFeedback.ACCEPTED),
				storedArticle(5L, PublicationFeedback.NULL));
		when(analysisService.findByUid(testUid)).thenReturn(analysis);

		ResponseEntity<?> response = reCiterController.updateGoldStandard(validGoldStandard,
				GoldStandardUpdateFlag.UPDATE, testUid, testUid, 0);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		ArgumentCaptor<AnalysisOutput> saved = ArgumentCaptor.forClass(AnalysisOutput.class);
		verify(analysisService).save(saved.capture());
		List<ReCiterArticleFeature> articles = saved.getValue().getReCiterFeature().getReCiterArticleFeatures();
		assertEquals(PublicationFeedback.ACCEPTED, articles.get(0).getUserAssertion());
		assertEquals(PublicationFeedback.REJECTED, articles.get(1).getUserAssertion());
		assertEquals(PublicationFeedback.REJECTED, articles.get(2).getUserAssertion());
		assertEquals(PublicationFeedback.NULL, articles.get(3).getUserAssertion());
		assertEquals(PublicationFeedback.NULL, articles.get(4).getUserAssertion());
		// usingS3 is reset so AnalysisServiceImpl.save re-makes the offload decision from
		// size; saving with it still true would leave the next read on the stale S3 copy.
		assertFalse(saved.getValue().isUsingS3());
	}

	@Test
	public void goldStandardWriteDoesNotRewriteAnUnchangedAnalysis() {
		GoldStandard stored = new GoldStandard();
		stored.setUid(testUid);
		stored.setKnownPmids(Arrays.asList(1L));
		stored.setRejectedPmids(null);
		when(dynamoDbGoldStandardService.findByUid(testUid)).thenReturn(stored);
		when(analysisService.findByUid(testUid)).thenReturn(storedAnalysis(testUid, false,
				storedArticle(1L, PublicationFeedback.ACCEPTED),
				storedArticle(2L, PublicationFeedback.NULL)));

		reCiterController.updateGoldStandard(validGoldStandard, GoldStandardUpdateFlag.UPDATE, testUid, testUid, 0);

		verify(analysisService, never()).save(any(AnalysisOutput.class));
	}

	@Test
	public void goldStandardWriteSurvivesAMissingOrFailingAnalysis() {
		// No stored Analysis yet (never scored): nothing to patch, no error.
		when(dynamoDbGoldStandardService.findByUid(testUid)).thenReturn(validGoldStandard);
		when(analysisService.findByUid(testUid)).thenReturn(null);
		assertEquals(HttpStatus.OK, reCiterController.updateGoldStandard(validGoldStandard,
				GoldStandardUpdateFlag.UPDATE, testUid, testUid, 0).getStatusCode());
		verify(analysisService, never()).save(any(AnalysisOutput.class));

		// The Analysis read blows up (e.g. the S3 offload is unreadable): the gold standard
		// was already saved, so the write still returns 200 and the patch is skipped.
		when(analysisService.findByUid(testUid)).thenThrow(new RuntimeException("s3 down"));
		assertEquals(HttpStatus.OK, reCiterController.updateGoldStandard(validGoldStandard,
				GoldStandardUpdateFlag.UPDATE, testUid, testUid, 0).getStatusCode());
		verify(analysisService, never()).save(any(AnalysisOutput.class));
	}

	@Test
	public void bulkGoldStandardWritePatchesEveryUid() {
		for (GoldStandard goldStandard : validGoldStandardList) {
			GoldStandard stored = new GoldStandard();
			stored.setUid(goldStandard.getUid());
			stored.setKnownPmids(Arrays.asList(1L));
			when(dynamoDbGoldStandardService.findByUid(goldStandard.getUid())).thenReturn(stored);
			when(analysisService.findByUid(goldStandard.getUid())).thenReturn(
					storedAnalysis(goldStandard.getUid(), false, storedArticle(1L, PublicationFeedback.NULL)));
		}

		reCiterController.updateGoldStandard(validGoldStandardList, GoldStandardUpdateFlag.UPDATE, testUid, testUid);

		verify(analysisService, times(validGoldStandardList.size())).save(any(AnalysisOutput.class));
	}
}

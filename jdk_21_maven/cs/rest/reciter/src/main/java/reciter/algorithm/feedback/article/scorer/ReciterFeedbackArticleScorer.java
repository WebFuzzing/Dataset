package reciter.algorithm.feedback.article.scorer;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StopWatch;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import reciter.algorithm.article.score.predictor.NeuralNetworkModelArticlesScorer;
import reciter.algorithm.evidence.StrategyContext;
import reciter.algorithm.evidence.article.ReCiterArticleStrategyContext;
import reciter.storage.s3.AwsScoringClients;
import reciter.algorithm.evidence.article.feedbackevidence.FeedbackEvidenceStrategyContext;
import reciter.algorithm.evidence.article.feedbackevidence.strategy.FeedbackEvidenceStrategy;
import reciter.algorithm.evidence.feedback.targetauthor.TargetAuthorFeedbackStrategyContext;
import reciter.algorithm.evidence.targetauthor.feedback.bibliographiccoupling.BibliographicCouplingFeedbackStrategyContext;
import reciter.algorithm.evidence.targetauthor.feedback.bibliographiccoupling.strategy.BibliographicCouplingFeedbackStrategy;
import reciter.algorithm.evidence.targetauthor.feedback.cites.CitesFeedbackStrategyContext;
import reciter.algorithm.evidence.targetauthor.feedback.cites.strategy.CitesFeedbackStrategy;
import reciter.algorithm.evidence.targetauthor.feedback.coauthorname.CoauthorNameFeedbackStrategyContext;
import reciter.algorithm.evidence.targetauthor.feedback.coauthorname.strategy.CoauthorNameFeedbackStrategy;
import reciter.algorithm.evidence.targetauthor.feedback.email.EmailFeedbackStrategyContext;
import reciter.algorithm.evidence.targetauthor.feedback.email.strategy.EmailFeedbackStrategy;
import reciter.algorithm.evidence.targetauthor.feedback.institution.InstitutionFeedbackStrategyContext;
import reciter.algorithm.evidence.targetauthor.feedback.institution.strategy.InstitutionFeedbackStrategy;
import reciter.algorithm.evidence.targetauthor.feedback.journal.JournalFeedbackStrategyContext;
import reciter.algorithm.evidence.targetauthor.feedback.journal.strategy.JournalFeedbackStrategy;
import reciter.algorithm.evidence.targetauthor.feedback.journalsubfield.JournalSubFieldFeedbackStrategyContext;
import reciter.algorithm.evidence.targetauthor.feedback.journalsubfield.strategy.JournalSubFieldFeedbackStrategy;
import reciter.algorithm.evidence.targetauthor.feedback.journaltitlesimilarity.JournalTitleSimilarityFeedbackStrategyContext;
import reciter.algorithm.evidence.targetauthor.feedback.journaltitlesimilarity.strategy.JournalTitleSimilarityFeedbackStrategy;
import reciter.algorithm.evidence.targetauthor.feedback.keyword.KeywordFeedbackStrategyContext;
import reciter.algorithm.evidence.targetauthor.feedback.keyword.strategy.KeywordFeedbackStrategy;
import reciter.algorithm.evidence.targetauthor.feedback.orcid.OrcidFeedbackStrategyContext;
import reciter.algorithm.evidence.targetauthor.feedback.orcid.strategy.OrcidFeedbackStrategy;
import reciter.algorithm.evidence.targetauthor.feedback.orcidcoauthor.OrcidCoauthorFeedbackStrategyContext;
import reciter.algorithm.evidence.targetauthor.feedback.orcidcoauthor.strategy.OrcidCoauthorFeedbackStrategy;
import reciter.algorithm.evidence.targetauthor.feedback.organization.OrganizationFeedbackStrategyContext;
import reciter.algorithm.evidence.targetauthor.feedback.organization.strategy.OrganizationFeedbackStrategy;
import reciter.algorithm.evidence.targetauthor.feedback.targetauthorname.TargetAuthorNameFeedbackStrategyContext;
import reciter.algorithm.evidence.targetauthor.feedback.targetauthorname.strategy.TargetAuthorNameFeedbackStrategy;
import reciter.algorithm.evidence.targetauthor.feedback.textsimilarity.TextSimilarityFeedbackStrategyContext;
import reciter.algorithm.evidence.targetauthor.feedback.textsimilarity.strategy.TextSimilarityFeedbackStrategy;
import reciter.algorithm.evidence.targetauthor.feedback.year.YearFeedbackStrategyContext;
import reciter.algorithm.evidence.targetauthor.feedback.year.strategy.YearFeedbackStrategy;
import reciter.engine.EngineParameters;
import reciter.engine.StrategyParameters;
import reciter.engine.analysis.evidence.AffiliationEvidence;
import reciter.engine.analysis.evidence.ArticleCountEvidence;
import reciter.engine.analysis.evidence.AuthorCountEvidence;
import reciter.engine.analysis.evidence.AuthorNameEvidence;
import reciter.engine.analysis.evidence.EducationYearEvidence;
import reciter.engine.analysis.evidence.EmailEvidence;
import reciter.engine.analysis.evidence.GenderEvidence;
import reciter.engine.analysis.evidence.JournalCategoryEvidence;
import reciter.engine.analysis.evidence.NonTargetAuthorScopusAffiliation;
import reciter.engine.analysis.evidence.TargetAuthorPubmedAffiliation;
import reciter.engine.analysis.evidence.TargetAuthorScopusAffiliation;
import reciter.model.article.ReCiterArticle;
import reciter.model.article.ReCiterArticleFeedbackIdentityScore;
import reciter.model.article.ReCiterAuthor;
import reciter.model.identity.Identity;
import reciter.utils.PropertiesUtils;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

public class ReciterFeedbackArticleScorer extends AbstractFeedbackArticleScorer {

	
	private static final Logger log = LoggerFactory.getLogger(ReciterFeedbackArticleScorer.class);
	private static final int ACCEPTED_ASSERTION =1;
	private static final int REJECTED_ASSERTION =-1;
	private List<ReCiterArticle> reciterArticles;
	private Identity identity;
	public static StrategyParameters strategyParameters;
	/**
	 * Journal Category Score
	 */
	private StrategyContext journalStrategyContext;
	private StrategyContext journalSubFieldStrategyContext;
	private StrategyContext orcidStrategyContext;
	private StrategyContext yearStrategyContext;
	private StrategyContext targetAuthorNameStrategyContext;
	private StrategyContext organizationStrategyContext;
	private StrategyContext orcidCoAuthorStrategyContext;
	private StrategyContext keywordStrategyContext;
	private StrategyContext institutionStrategyContext;
	private StrategyContext emailStrategyContext;
	private StrategyContext coAuthorNameStrategyContext;
	private StrategyContext citesStrategyContext;
	private StrategyContext bibliographicCouplingStrategyContext;
	private StrategyContext textSimilarityStrategyContext;
	private StrategyContext journalTitleSimilarityStrategyContext;
	private StrategyContext feedbackEvidenceStrategyContext;

	ExecutorService executorService = Executors.newWorkStealingPool(15);
	
	public ReciterFeedbackArticleScorer(List<ReCiterArticle> articles,Identity identity,EngineParameters parameters,StrategyParameters strategyParameters)
	{
		ReciterFeedbackArticleScorer.strategyParameters = strategyParameters;
		this.reciterArticles = articles;
		this.identity = identity;

		// Configure informed absence for target author name strategy
		TargetAuthorNameFeedbackStrategy targetAuthorNameStrategy = new TargetAuthorNameFeedbackStrategy();
		targetAuthorNameStrategy.setInformedAbsenceConfig(
			strategyParameters.isInformedAbsenceEnabled(),
			strategyParameters.getInformedAbsenceScale(),
			strategyParameters.getInformedAbsenceTargetAuthorNameStrength());

		// Configure informed absence for co-author name strategy
		CoauthorNameFeedbackStrategy coAuthorNameStrategy = new CoauthorNameFeedbackStrategy();
		coAuthorNameStrategy.setInformedAbsenceConfig(
			strategyParameters.isInformedAbsenceEnabled(),
			strategyParameters.getInformedAbsenceScale(),
			strategyParameters.getInformedAbsenceCoAuthorNameStrength());

		JournalFeedbackStrategy journalStrategy = new JournalFeedbackStrategy();
		journalStrategy.setInformedAbsenceConfig(
			strategyParameters.isInformedAbsenceEnabled(),
			strategyParameters.getInformedAbsenceScale(),
			strategyParameters.getInformedAbsenceJournalStrength());

		JournalSubFieldFeedbackStrategy journalSubFieldStrategy = new JournalSubFieldFeedbackStrategy();
		journalSubFieldStrategy.setInformedAbsenceConfig(
			strategyParameters.isInformedAbsenceEnabled(),
			strategyParameters.getInformedAbsenceScale(),
			strategyParameters.getInformedAbsenceJournalSubFieldStrength());

		OrcidFeedbackStrategy orcidStrategy = new OrcidFeedbackStrategy();
		orcidStrategy.setInformedAbsenceConfig(
			strategyParameters.isInformedAbsenceEnabled(),
			strategyParameters.getInformedAbsenceScale(),
			strategyParameters.getInformedAbsenceOrcidStrength());

		this.journalStrategyContext = new JournalFeedbackStrategyContext(journalStrategy);
		this.journalSubFieldStrategyContext = new JournalSubFieldFeedbackStrategyContext(journalSubFieldStrategy);
		this.orcidStrategyContext = new OrcidFeedbackStrategyContext(orcidStrategy);
		this.yearStrategyContext = new YearFeedbackStrategyContext(new YearFeedbackStrategy());
		this.targetAuthorNameStrategyContext = new TargetAuthorNameFeedbackStrategyContext(targetAuthorNameStrategy);
		OrganizationFeedbackStrategy organizationStrategy = new OrganizationFeedbackStrategy();
		organizationStrategy.setInformedAbsenceConfig(
			strategyParameters.isInformedAbsenceEnabled(),
			strategyParameters.getInformedAbsenceScale(),
			strategyParameters.getInformedAbsenceOrganizationStrength());
		this.organizationStrategyContext = new OrganizationFeedbackStrategyContext(organizationStrategy);
		this.orcidCoAuthorStrategyContext = new OrcidCoauthorFeedbackStrategyContext(new OrcidCoauthorFeedbackStrategy());
		KeywordFeedbackStrategy keywordStrategy = new KeywordFeedbackStrategy(strategyParameters);
		keywordStrategy.setInformedAbsenceConfig(
			strategyParameters.isInformedAbsenceEnabled(),
			strategyParameters.getInformedAbsenceScale(),
			strategyParameters.getInformedAbsenceKeywordStrength());
		this.keywordStrategyContext = new KeywordFeedbackStrategyContext(keywordStrategy);
		TextSimilarityFeedbackStrategy textSimilarityStrategy = new TextSimilarityFeedbackStrategy(strategyParameters);
		this.textSimilarityStrategyContext = new TextSimilarityFeedbackStrategyContext(textSimilarityStrategy);
		JournalTitleSimilarityFeedbackStrategy journalTitleSimilarityStrategy = new JournalTitleSimilarityFeedbackStrategy(strategyParameters);
		this.journalTitleSimilarityStrategyContext = new JournalTitleSimilarityFeedbackStrategyContext(journalTitleSimilarityStrategy);
		InstitutionFeedbackStrategy institutionStrategy = new InstitutionFeedbackStrategy();
		institutionStrategy.setInformedAbsenceConfig(
			strategyParameters.isInformedAbsenceEnabled(),
			strategyParameters.getInformedAbsenceScale(),
			strategyParameters.getInformedAbsenceInstitutionStrength());
		this.institutionStrategyContext = new InstitutionFeedbackStrategyContext(institutionStrategy);

		EmailFeedbackStrategy emailStrategy = new EmailFeedbackStrategy();
		emailStrategy.setInformedAbsenceConfig(
			strategyParameters.isInformedAbsenceEnabled(),
			strategyParameters.getInformedAbsenceScale(),
			strategyParameters.getInformedAbsenceEmailStrength());
		this.emailStrategyContext = new EmailFeedbackStrategyContext(emailStrategy);
		this.coAuthorNameStrategyContext = new CoauthorNameFeedbackStrategyContext(coAuthorNameStrategy);
		CitesFeedbackStrategy citesStrategy = new CitesFeedbackStrategy();
		citesStrategy.setInformedAbsenceConfig(
			strategyParameters.isInformedAbsenceEnabled(),
			strategyParameters.getInformedAbsenceScale(),
			strategyParameters.getInformedAbsenceCitesStrength());
		this.citesStrategyContext = new CitesFeedbackStrategyContext(citesStrategy);
		BibliographicCouplingFeedbackStrategy bibCouplingStrategy = new BibliographicCouplingFeedbackStrategy();
		bibCouplingStrategy.setInformedAbsenceConfig(
			strategyParameters.isInformedAbsenceEnabled(),
			strategyParameters.getInformedAbsenceScale(),
			strategyParameters.getInformedAbsenceBibliographicCouplingStrength());
		this.bibliographicCouplingStrategyContext = new BibliographicCouplingFeedbackStrategyContext(bibCouplingStrategy);
		this.feedbackEvidenceStrategyContext = new FeedbackEvidenceStrategyContext(new FeedbackEvidenceStrategy());


	}
	public void runFeedbackArticleScorer(List<ReCiterArticle> reCiterArticles, Identity identity) 
	{
		 List<Future<?>> futures = new ArrayList<>();

		if(strategyParameters.isFeedbackScoreJournal()) {
			futures.add(submitAndLogTime("Journal Category", executorService, journalStrategyContext, reCiterArticles, identity));
		}
		if(strategyParameters.isFeedbackScoreJournalSubField()) {
			futures.add(submitAndLogTime("Journal SubField Category", executorService, journalSubFieldStrategyContext, reCiterArticles, identity));

		}
		if(strategyParameters.isFeedbackScoreOrcid()) {
			futures.add(submitAndLogTime("Orcid Category", executorService, orcidStrategyContext, reCiterArticles, identity));

		}
		if(strategyParameters.isFeedbackScoreYear()) {
			futures.add(submitAndLogTime("Year Category", executorService, yearStrategyContext, reCiterArticles, identity));

		}
		if(strategyParameters.isFeedbackScoreEmail()) {
			futures.add(submitAndLogTime("Email Category", executorService, emailStrategyContext, reCiterArticles, identity));

		}
		if(strategyParameters.isFeedbackScoreOrganization()) {
			futures.add(submitAndLogTime("Organization Category", executorService, organizationStrategyContext, reCiterArticles, identity));

		}
		if(strategyParameters.isFeedbackScoreInstitution()) {
			futures.add(submitAndLogTime("Institution Category", executorService, institutionStrategyContext, reCiterArticles, identity));

		}
		if(strategyParameters.isFeedbackScoreTargetAuthorName()) {
			futures.add(submitAndLogTime("Target Author Name Category", executorService, targetAuthorNameStrategyContext, reCiterArticles, identity));

		}
		if(strategyParameters.isFeedbackScoreOrcidCoAuthor()) {
			futures.add(submitAndLogTime("Orcid coAuthor Category", executorService, orcidCoAuthorStrategyContext, reCiterArticles, identity));
		}
		if(strategyParameters.isFeedbackScoreKeyword()) {
			futures.add(submitAndLogTime("Keyword Category", executorService, keywordStrategyContext, reCiterArticles, identity));
		}
		if(strategyParameters.isFeedbackScoreTextSimilarity()) {
			futures.add(submitAndLogTime("Text Similarity Category", executorService, textSimilarityStrategyContext, reCiterArticles, identity));
		}
		if(strategyParameters.isFeedbackScoreJournalTitleSimilarity()) {
			futures.add(submitAndLogTime("Journal Title Similarity Category", executorService, journalTitleSimilarityStrategyContext, reCiterArticles, identity));
		}
		if(strategyParameters.isFeedbackScoreCoauthorName()) {
			futures.add(submitAndLogTime("CoAuthorName Category", executorService, coAuthorNameStrategyContext, reCiterArticles, identity));

		}
		if(strategyParameters.isFeedbackScoreCites()) {
			futures.add(submitAndLogTime("Cites Category", executorService, citesStrategyContext, reCiterArticles, identity));

		}
		if(strategyParameters.isFeedbackScoreBibliographicCoupling()) {
			futures.add(submitAndLogTime("Bibliographic Coupling Category", executorService, bibliographicCouplingStrategyContext, reCiterArticles, identity));
		}
		//futures.add(submitAndLogTime("authors Count Category", executorService, authorCountStrategyContext, reCiterArticles, identity));
		// Shutdown executorService after submitting all tasks
        executorService.shutdown();
        
        // Wait for all tasks to complete
        try {
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        boolean allTasksCompleted = true;
        // Print execution times from futures
        for (Future<?> future : futures) {
            try {
                future.get(); // Ensure all tasks are completed
            } catch (InterruptedException | ExecutionException e) {
                log.error("Task execution interrupted or encountered an error", e);
                allTasksCompleted=false;
            }
        }
        if (allTasksCompleted) {
        	//Export item Level Data
        	exportArticleItemLevelReport(reCiterArticles, identity);
            // All tasks completed successfully
            exportArticlesConsolidateReport(reCiterArticles, identity);
         	((ReCiterArticleStrategyContext) feedbackEvidenceStrategyContext).executeStrategy(reCiterArticles);
         	executePythonScriptForArticleFeedbackTotal(reCiterArticles,identity);
	    } else {
            log.error("One or more tasks failed; report generation may be incomplete.");
        }


	}
	
	private Future<?> submitAndLogTime(String category, ExecutorService executorService,
			StrategyContext context,List<ReCiterArticle> reCiterArticles, Identity identity) 
	{

		return executorService.submit(() -> {
		StopWatch stopWatch = new StopWatch(category);
		stopWatch.start(category);
		((TargetAuthorFeedbackStrategyContext)context).executeFeedbackStrategy(reCiterArticles, identity);
		stopWatch.stop();
		log.info(stopWatch.getId() + " took " + stopWatch.getTotalTimeSeconds() + "s");
		});
	}
	private void exportArticlesConsolidateReport(List<ReCiterArticle> reCiterArticles, Identity identity )
	{
		// printing all the article lookup PMID and CoAuthors associated with other
		// PMIDs.
		Map<Long, ReCiterArticle> articlesMap = reCiterArticles.stream()
	            .collect(Collectors.toMap(ReCiterArticle::getArticleId, Function.identity()));
		exportConsolidatedFeedbackScores(identity.getUid(), articlesMap);
	}
	private void exportArticleItemLevelReport(List<ReCiterArticle> reCiterArticles, Identity identity )
	{
		// printing all the article lookup PMID and CoAuthors associated with other
		// PMIDs.
		exportArticleItemLevelFeedbackScores(identity.getUid(), reCiterArticles);
	}
	protected void exportConsolidatedFeedbackScores(String personIdentifier, Map<Long,ReCiterArticle> articleMap)
	{
		String[] csvHeaders = { "PersonIdentifier","Pmid","userAssertion","scoreCites","scoreBibliographicCoupling","scoreCoAuthorName","scoreEmail",
    		 	"scoreInstitution","scoreJournal","scoreJournalSubField","scoreKeyword","scoreTextSimilarity","scoreJournalTitleSimilarity","scoreOrcid","scoreOrcidCoAuthor",
    		 	"scoreOrganization","scoreTargetAuthorName","scoreYear" };
		
		Path filePath = Paths.get(personIdentifier + "_consolidated.csv");
		
		if(isS3UploadRequired()) 
		{	
			try ( // Create BufferedWriter
					ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		             OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
					// Create CSVPrinter
					CSVPrinter csvPrinter = new CSVPrinter(writer,CSVFormat.Builder.create(CSVFormat.DEFAULT)
			                             .setHeader(csvHeaders)
			                             .build())) {	
				
				mapConsolidatedCSVData(articleMap,csvPrinter,personIdentifier);

				csvPrinter.flush();
				uploadCsvToS3(outputStream.toString(StandardCharsets.UTF_8.name()),filePath.toString());
				
			} catch (IOException e) {
				log.error(
						"IOException while exporting consolidated CSV for S3. personIdentifier={}, filePath={}, articleCount={}",
						personIdentifier, filePath, articleMap != null ? articleMap.size() : 0, e);
				
			}	
		}
		else
		{
			try ( // Create BufferedWriter
					BufferedWriter writer = new BufferedWriter(new FileWriter(filePath.toFile()));
					// Create CSVPrinter
					CSVPrinter csvPrinter = new CSVPrinter(writer,CSVFormat.Builder.create(CSVFormat.DEFAULT)
			                             .setHeader(csvHeaders)
			                             .build())) {	
				
				mapConsolidatedCSVData(articleMap,csvPrinter,personIdentifier);
				csvPrinter.flush();
				
			 
			} catch (IOException e) {
				log.error(
						"IOException while exporting consolidated CSV locally. personIdentifier={}, filePath={}, articleCount={}",
						personIdentifier, filePath, articleMap != null ? articleMap.size() : 0, e);

			}
		}
	}
	protected void exportArticleItemLevelFeedbackScores(String personIdentifier, List<ReCiterArticle> reCiterArticles)
	{
		
		String[] csvHeaders = { "PersonIdentifier", "Pmid", "CountAccepted", "CountRejected",
				"subscoreType1", "subscoreValue", "subScoreIndividualScore","UserAssertion" };
		
		Path filePath = Paths.get(personIdentifier + "_item_level.csv");
		
		if(isS3UploadRequired()) 
		{
			try ( // Create BufferedWriter
					ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		             OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
					// Create CSVPrinter
					CSVPrinter csvPrinter = new CSVPrinter(writer,CSVFormat.Builder.create(CSVFormat.DEFAULT)
	                        .setHeader(csvHeaders).build())) {
	
					mapItemLevelCSVData(reCiterArticles,csvPrinter,personIdentifier);
				
				csvPrinter.flush();
				uploadCsvToS3(outputStream.toString(StandardCharsets.UTF_8.name()),filePath.toString());
				
			} catch (IOException e) {
				log.error("IOException while exporting item-level CSV for S3 upload. personIdentifier={}, filePath={}",personIdentifier, filePath, e);
			}
		}
		else
		{
			try ( // Create BufferedWriter
					BufferedWriter writer = new BufferedWriter(new FileWriter(filePath.toFile()));
					// Create CSVPrinter
					CSVPrinter csvPrinter = new CSVPrinter(writer,CSVFormat.Builder.create(CSVFormat.DEFAULT)
	                        .setHeader(csvHeaders).build())) {
	
					mapItemLevelCSVData(reCiterArticles,csvPrinter,personIdentifier);
				
				csvPrinter.flush();
			} catch (IOException e) {
				log.error("IOException while exporting item-level CSV locally. personIdentifier={}, filePath={}",personIdentifier, filePath, e);
			}
		}
		
	}
	private boolean uploadCsvToS3(String csvContent,String fileName) {
       
		String feedbackScoreBucketName = PropertiesUtils.get("aws.s3.feedback.score.bucketName");
		byte[] csvBytes = csvContent.getBytes(StandardCharsets.UTF_8);
        // Create InputStream from CSV content
        ByteArrayInputStream inputStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));
        
        // Upload the CSV file
        try {
        	final S3Client s3 = AwsScoringClients.s3();
        	// Check if the bucket exists
	        HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
	                .bucket(feedbackScoreBucketName)
	                .build();
	        try {
	  				s3.headBucket(headBucketRequest);
	  				log.info("Uploading CSV file to S3 bucket: {}",feedbackScoreBucketName);
	  			} catch (S3Exception e) {
	  				if (e.statusCode() == 404) {
	  					log.error("S3 bucket does not exist: {}" , feedbackScoreBucketName);
	  				} else {
	  					log.error("Error checking bucket existence: {}" ,e.getMessage());
	  				}
	  				return false;
	  			}
	  	        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
	  					.bucket(feedbackScoreBucketName.toLowerCase()).key(fileName).contentType("text/csv").build();
	  	        PutObjectResponse putObjectResponse = s3.putObject(putObjectRequest, RequestBody.fromBytes(csvBytes));
	  	        
	  	        if (putObjectResponse.sdkHttpResponse().isSuccessful()) {
	                  log.info("CSV file uploaded successfully to S3 bucket: {}" , feedbackScoreBucketName);
	                  return true;
	              } else {
						log.error("Failed to upload CSV file to S3. fileName={}, bucketName={}", fileName,feedbackScoreBucketName);
	                  return false;
	              }
	          } catch (Exception e) {
					log.error("Unexpected exception while uploading CSV to S3. fileName={}, bucketName={}", fileName,feedbackScoreBucketName, e);
	  			return false;			 
	          }
	      }
	
	private void mapConsolidatedCSVData(Map<Long,ReCiterArticle> articleMap,CSVPrinter csvPrinter,String personIdentifier)
	{
		articleMap.forEach((articleId, article) -> {
			try {
				
						
					String citiesFeedbackScore = article.getExportedCitesFeedbackScore()!=null? article.getExportedCitesFeedbackScore() :"0" ;
					String bibliographicCouplingFeedbackScore = article.getExportedBibliographicCouplingFeedbackScore()!=null? article.getExportedBibliographicCouplingFeedbackScore() :"0" ;
					String coAuthorFeedbackScore = article.getExportedCoAuthorNameFeedbackScore()!= null?article.getExportedCoAuthorNameFeedbackScore() :"0";
					String emailFeedbackScore = article.getExportedEmailFeedbackScore()!=null?article.getExportedEmailFeedbackScore():"0";
					String institutionFeedbackScore = article.getExportedInstitutionFeedbackScore()!=null?article.getExportedInstitutionFeedbackScore():"0" ;
					String journalFeedbackScore = article.getExportedJournalFeedackScore()!=null?article.getExportedJournalFeedackScore():"0";
					String journalSubFieldFeedbackScore = article.getExportedJournalSubFieldFeedbackScore()!=null?article.getExportedJournalSubFieldFeedbackScore():"0";
					String keywordFeedbackScore = article.getExportedKeywordFeedackScore()!=null?article.getExportedKeywordFeedackScore():"0";
					String textSimilarityFeedbackScore = article.getExportedTextSimilarityFeedbackScore()!=null?article.getExportedTextSimilarityFeedbackScore():"0";
					String journalTitleSimilarityFeedbackScore = article.getExportedJournalTitleSimilarityFeedbackScore()!=null?article.getExportedJournalTitleSimilarityFeedbackScore():"0";
					String orcidFeedbackScore = article.getExportedOrcidFeedbackScore()!=null?article.getExportedOrcidFeedbackScore():"0";
					String orcidCoAuthorFeedbackScore = article.getExportedOrcidCoAuthorFeedbackScore()!=null?article.getExportedOrcidCoAuthorFeedbackScore():"0";
					String organizationFeedbackScore = article.getExportedOrganizationFeedbackScore()!=null?article.getExportedOrganizationFeedbackScore():"0";
					String targetAuthorNameFeedbackScore = article.getExportedTargetAuthorNameFeedbackScore()!=null?article.getExportedOrganizationFeedbackScore():"0";
					String yearFeedbackScore = article.getExportedYearFeedbackScore();
					
					
					csvPrinter.printRecord(personIdentifier,
							articleId,
							article.getGoldStandard(),
							citiesFeedbackScore,
							bibliographicCouplingFeedbackScore,
							coAuthorFeedbackScore,
							emailFeedbackScore,
							institutionFeedbackScore,
							journalFeedbackScore,
							journalSubFieldFeedbackScore,
							keywordFeedbackScore,
							textSimilarityFeedbackScore,
							journalTitleSimilarityFeedbackScore,
							orcidFeedbackScore,
							orcidCoAuthorFeedbackScore,
							organizationFeedbackScore,
							targetAuthorNameFeedbackScore,
							yearFeedbackScore
							);
					
					
			} catch (IOException e) {
				log.error(
						"IOException while writing consolidated CSV data. personIdentifier={}, articleId={}, goldStandard={}",
						personIdentifier, articleId, article != null ? article.getGoldStandard() : null, e);
			}

		});
	}
	private void mapItemLevelCSVData(List<ReCiterArticle> reCiterArticles,CSVPrinter csvPrinter,String personIdentifier)
	{
		reCiterArticles.stream()
		 .filter(article -> article!=null && article.getArticleFeedbackScoresMap()!=null && article.getArticleFeedbackScoresMap().size() > 0)	
		.forEach(article -> {
				try {
					 
					
					article.getArticleFeedbackScoresMap().stream()
						  .flatMap(map -> map.entrySet().stream()
							 .flatMap(entry -> entry.getValue().stream()))
						 .forEach(feedbackScore -> { 
									if(feedbackScore.getFeedbackScore() != 0.0)
									{
										//String feedbackScore = article.getExportedFeedbackScore();
										try {
											csvPrinter.printRecord(personIdentifier, feedbackScore.getArticleId(),
													feedbackScore.getAcceptedCount(), feedbackScore.getRejectedCount(), feedbackScore.getFeedbackScoreType(),
													feedbackScore.getFeedbackScoreFieldValue(), feedbackScore.getFeedbackScore(),feedbackScore.getGoldStandard());
										} catch (IOException e) {
											log.error(
													"IOException while writing CSV record. personIdentifier={}, articleId={}, feedbackScoreType={}, feedbackScoreFieldValue={}",
													personIdentifier, feedbackScore.getArticleId(),
													feedbackScore.getFeedbackScoreType(),
													feedbackScore.getFeedbackScoreFieldValue(), e);
										}
									}
							 });
							 
					} catch (Exception e) {
						log.error("Exception while mapping item level CSV data. personIdentifier={}, articleId={}",personIdentifier, article.getArticleId(), e);
				}

			});
	}
	
    private boolean isS3UploadRequired()
    {
    	  	  
  	     // Retrieve properties
         String awsS3Use = PropertiesUtils.get("aws.s3.use");
         boolean isS3Use = Boolean.parseBoolean(awsS3Use);
         String dynamoDDLocal = PropertiesUtils.get("aws.dynamoDb.local");
         boolean isDynamoDBLocal = Boolean.parseBoolean(dynamoDDLocal);
         if(isS3Use && !isDynamoDBLocal) 
        	 return true;
    	return false;
    }
      private List<ReCiterArticle> executePythonScriptForArticleFeedbackTotal(List<ReCiterArticle> reCiterArticles, Identity identity) {
    
    	//retrieving countAccepted, CountRejected and CountNull 
		
		Map<Integer, List<ReCiterArticle>> groupedByGoldStandard = reCiterArticles.stream()
	            .collect(Collectors.groupingBy(ReCiterArticle::getGoldStandard));
		
		//countAccepted
		int countAccepted = groupedByGoldStandard.getOrDefault(ACCEPTED_ASSERTION, Collections.emptyList()).size();
		
		//countRejected
		int countRejected = groupedByGoldStandard.getOrDefault(REJECTED_ASSERTION, Collections.emptyList()).size();
		
    	List<ReCiterArticleFeedbackIdentityScore> articleIdentityFeedbackScore = reCiterArticles.parallelStream()
    																	.map( article -> mapToFeedbackScore(article, countAccepted, countRejected))
														    		    .collect(Collectors.toList());
	
    	ObjectMapper objectMapper = new ObjectMapper();

    	// Enrich scores with identity names and per-article name evidence for Python scoring
    	String identityFirstName = (identity.getPrimaryName() != null && identity.getPrimaryName().getFirstName() != null)
    	        ? identity.getPrimaryName().getFirstName() : "";
    	String identityMiddleName = (identity.getPrimaryName() != null && identity.getPrimaryName().getMiddleName() != null)
    	        ? identity.getPrimaryName().getMiddleName() : "";

    	// Build articleId → article map for per-article name evidence enrichment
    	Map<Long, ReCiterArticle> articleByIdMap = reCiterArticles.stream()
    	        .collect(Collectors.toMap(ReCiterArticle::getArticleId, Function.identity(), (a, b) -> a));

    	List<ObjectNode> enrichedScores = articleIdentityFeedbackScore.stream()
    	        .map(score -> {
    	            ObjectNode node = objectMapper.convertValue(score, ObjectNode.class);
    	            node.put("identityFirstName", identityFirstName);
    	            node.put("identityMiddleName", identityMiddleName);

    	            // Per-article name evidence fields
    	            ReCiterArticle article = articleByIdMap.get(score.getArticleId());
    	            if (article != null && article.getAuthorNameEvidence() != null) {
    	                AuthorNameEvidence ev = article.getAuthorNameEvidence();
    	                node.put("articleAuthorFirstName",
    	                    ev.getArticleAuthorName() != null && ev.getArticleAuthorName().getFirstName() != null
    	                    ? ev.getArticleAuthorName().getFirstName() : "");
    	                node.put("nameMatchFirstType",
    	                    ev.getNameMatchFirstType() != null ? ev.getNameMatchFirstType() : "");
    	                node.put("nameMatchMiddleType",
    	                    ev.getNameMatchMiddleType() != null ? ev.getNameMatchMiddleType() : "");
    	            }
    	            return node;
    	        })
    	        .collect(Collectors.toList());

		String fileName = StringUtils.join(identity.getUid(), "-feedbackIdentityScoringInput.json");
		boolean isS3UploadRequired = isS3UploadRequired();
		String feedbackIdentityS3BucketName = PropertiesUtils.get("aws.s3.feedback.score.bucketName");
        try {
			NeuralNetworkModelArticlesScorer nnmodel = new NeuralNetworkModelArticlesScorer();
        	  if(isS3UploadRequired)
        	  {
        		  File jsonFile = new File(fileName);

        		  // Write the User object to the JSON file
                  objectMapper.writeValue(jsonFile, enrichedScores);
                  log.info("JSON data written to file successfully: {}", jsonFile.getAbsolutePath());
                  uploadJsonFileIntoS3(fileName, jsonFile);
        	  }
        	  else
        	  {
        		  File jsonFile = new File("src/main/resources/scripts/"+fileName);
	        	  objectMapper.writeValue(jsonFile, enrichedScores);
	        	  log.info("JSON written to file successfully. {}", jsonFile.getAbsolutePath() +"-" + fileName);
        	  }
        	  String isS3UploadRequiredString = Boolean.toString(isS3UploadRequired);
			  
			  JSONArray articlesIdentityFeedbackScoreTotal = nnmodel.executeArticleScorePredictor("feedback", fileName,feedbackIdentityS3BucketName,isS3UploadRequiredString);
			  if(articlesIdentityFeedbackScoreTotal!=null && articlesIdentityFeedbackScoreTotal.length() > 0)
			  {  
				  List<ReCiterArticle> articlesScores =  mapAuthorshipLikelihoodScore(reCiterArticles, articlesIdentityFeedbackScoreTotal);
				  articlesScores.forEach(article -> log.debug("articles : " + article));
			  	 return articlesScores;	
			  }  	
				  
			  
		} catch (IOException e) {
			log.error(
		            "IOException occurred while executing article feedback scoring. identityId={}, fileName={}, articleCount={}, acceptedCount={}, rejectedCount={}",
		            identity != null ? identity.getUid() : null,
		            fileName,
		            reCiterArticles != null ? reCiterArticles.size() : 0,
		            countAccepted,
		            countRejected,
		            e);
		}
        return null;
    }
      private static ReCiterArticleFeedbackIdentityScore mapToFeedbackScore(ReCiterArticle article,int countAccepted, int countRejected) {
   	   
          try {
          	
          	return new ReCiterArticleFeedbackIdentityScore(
  														    article.getArticleId(),
  														    getFeedbackScore(article.getCitesFeedbackScore()),
  														    getFeedbackScore(article.getCoAuthorNameFeedbackScore()),
  														    getFeedbackScore(article.getEmailFeedbackScore()),
  														    getFeedbackScore(article.getInstitutionFeedbackScore()),
  														    getFeedbackScore(article.getJournalFeedackScore()),
  														    getFeedbackScore(article.getJournalSubFieldFeedbackScore()),
  														    getFeedbackScore(article.getKeywordFeedackScore()),
  														    getFeedbackScore(article.getTextSimilarityFeedbackScore()),
  														    getFeedbackScore(article.getJournalTitleSimilarityFeedbackScore()),
  														    getFeedbackScore(article.getOrcidFeedbackScore()),
  														    getFeedbackScore(article.getOrcidCoAuthorFeedbackScore()),
  														    getFeedbackScore(article.getOrganizationFeedbackScore()),
  														    getFeedbackScore(article.getTargetAuthorNameFeedbackScore()),
  														    getFeedbackScore(article.getYearFeedbackScore()),
  														    getFeedbackScore(article.getBibliographicCouplingFeedbackScore()),
  														    getArticleCountScore(article.getArticleCountEvidence()),
  														    getAuthorsCountScore(article.getAuthorCountEvidence()),													   
  														    getEducationYearScore(article.getEducationYearEvidence()),
  														    getEmailMatchScore(article.getEmailEvidence()),
  														    getGenderScore(article.getGenderEvidence()),
  														    article.getGrantEvidenceTotalScore(), 
  														    getJournalSubfieldScore(article.getJournalCategoryEvidence()),
  														    getNameMatchScore(article.getAuthorNameEvidence(), AuthorNameEvidence::getNameMatchFirstScore),
  														    getNameMatchScore(article.getAuthorNameEvidence(), AuthorNameEvidence::getNameMatchLastScore),
  														    getNameMatchScore(article.getAuthorNameEvidence(), AuthorNameEvidence::getNameMatchMiddleScore),
  														    getNameMatchScore(article.getAuthorNameEvidence(), AuthorNameEvidence::getNameMatchModifierScore),
  														    getFeedbackScore(article.getOrganizationalEvidencesTotalScore()),
  														    (article.getRelationshipEvidence() == null ? 0.0 : article.getRelationshipEvidence().getRelationshipPositiveMatchScore()),
  														    (article.getRelationshipEvidence() == null ? 0.0 : article.getRelationshipEvidence().getRelationshipNegativeMatchScore()),
  														    (article.getRelationshipEvidence() == null ? 0L : article.getRelationshipEvidence().getRelationshipIdentityCount()),
  														    getNonTargetAuthorInstitutionalAffiliationScore(article.getAffiliationEvidence()),
  														    getTargetAuthorAffiliationScore(article.getAffiliationEvidence()),
  														    getPubmedTargetAuthorAffiliationScore(article.getAffiliationEvidence()),
  														    article.getGoldStandard()==1? countAccepted-1 : countAccepted,  
  														    article.getGoldStandard()==-1? countRejected-1: countRejected,
  														    ((article.getGoldStandard()==1)? "ACCEPTED" : (article.getGoldStandard()==-1)? "REJECTED" :"PENDING"));
  		} catch (Exception e) {
			log.error(
					"Exception occurred while mapping feedback score for articleId {}. acceptedCount={}, rejectedCount={}",
					article != null ? article.getArticleId() : null, countAccepted, countRejected, e);
  		}
  		return null;
      }
    
	// Helper methods
	private static double getFeedbackScore(Double score) {
	    return Optional.ofNullable(score).orElse(0.0);
	}

	private static double getArticleCountScore(ArticleCountEvidence evidence) {
	    return Optional.ofNullable(evidence)
	            .map(ArticleCountEvidence::getArticleCountScore)
	            .orElse(0.0);
	}
	
	private static double getAuthorsCountScore(AuthorCountEvidence evidence)
	{
		return Optional.ofNullable(evidence)
	            .map(AuthorCountEvidence::getAuthorCountScore)
	            .orElse(0.0);
	}
	
	private static double getEducationYearScore(EducationYearEvidence evidence) {
	    return Optional.ofNullable(evidence)
	            .map(EducationYearEvidence::getDiscrepancyDegreeYearDoctoralScore)
	            .filter(score -> score != 0.0)  
	            .orElseGet(() -> Optional.ofNullable(evidence)
	                                      .map(EducationYearEvidence::getDiscrepancyDegreeYearBachelorScore)
	                                      .orElse(0.0));   
	}

	private static double getEmailMatchScore(EmailEvidence evidence) {
	    return Optional.ofNullable(evidence)
	            .map(EmailEvidence::getEmailMatchScore)
	            .orElse(0.0);
	}

	private static double getGenderScore(GenderEvidence evidence) {
	    return Optional.ofNullable(evidence)
	            .map(GenderEvidence::getGenderScoreIdentityArticleDiscrepancy)
	            .orElse(0.0);
	}

	private static double getJournalSubfieldScore(JournalCategoryEvidence evidence) {
	    return Optional.ofNullable(evidence)
	            .map(JournalCategoryEvidence::getJournalSubfieldScore)
	            .orElse(0.0);
	}

	private static double getNameMatchScore(AuthorNameEvidence evidence, Function<AuthorNameEvidence, Double> scoreFunction) {
	    return Optional.ofNullable(evidence)
	            .map(scoreFunction)
	            .orElse(0.0);
	}

	private static double getNonTargetAuthorInstitutionalAffiliationScore(AffiliationEvidence evidence) {
	    return Optional.ofNullable(evidence)
	            .map(AffiliationEvidence::getScopusNonTargetAuthorAffiliation)
	            .map(NonTargetAuthorScopusAffiliation::getNonTargetAuthorInstitutionalAffiliationScore)
	            .orElse(0.0);
	}

	private static double getTargetAuthorAffiliationScore(AffiliationEvidence evidence) {
	    return Optional.ofNullable(evidence)
	            .map(AffiliationEvidence::getScopusTargetAuthorAffiliation)
	            .map(affiliations -> affiliations.stream()
	                    .mapToDouble(TargetAuthorScopusAffiliation::getTargetAuthorInstitutionalAffiliationMatchTypeScore)
	                    .sum())
	            .orElse(0.0);
	}

	private static double getPubmedTargetAuthorAffiliationScore(AffiliationEvidence evidence) {
	    return Optional.ofNullable(evidence)
	            .map(AffiliationEvidence::getPubmedTargetAuthorAffiliation)
	            .map(TargetAuthorPubmedAffiliation::getTargetAuthorInstitutionalAffiliationMatchTypeScore)
	            .orElse(0.0);
	}
	
	
	private static List<ReCiterArticle> mapAuthorshipLikelihoodScore(List<ReCiterArticle> reCiterArticles, JSONArray authorshipLikelihoodScoreArray)
	{
		 Map<Long, Double> scoreMap = buildScoreMap(authorshipLikelihoodScoreArray);
		 return reCiterArticles.parallelStream()
				 				 .filter(Objects::nonNull)
				 				 .map(article -> {
				 					 // Look up score from pre-built map (O(1) instead of O(n))
				 					 article.setAuthorshipLikelihoodScore(scoreMap.getOrDefault(article.getArticleId(), 0.0));
					 		            // count the targetAuthors per article
				 		        	 	long targetAuthorCount = article.getArticleCoAuthors().getAuthors().stream()
				 		                     .filter(ReCiterAuthor::isTargetAuthor)  // Filter target authors
				 		                     .count();  // Count them
					 		                 //if the targetAuthorCount is zero then impose the penality in the article authorshipLikelyhood score.
				 		                 article.setTargetAuthorCount(targetAuthorCount);
				 		                 if(targetAuthorCount == 0)
				 		                 {
				 		                	 //double authorshipLikelyhoodScore = (strategyParameters.getTargetAuthorMissingPenaltyPercent() * (article.getAuthorshipLikelihoodScore()/100));
				 		                	 
				 		                	 // FIX (#640-C): capture the ORIGINAL score before overwriting it. Previously
				 		                	 // the penalty was computed AFTER setAuthorshipLikelihoodScore, so it read back
				 		                	 // the new value and the delta was always x-x=0.
				 		                	 double originalAuthorshipLikelihoodScore = article.getAuthorshipLikelihoodScore();
				 		                	 double authorshipLikelyhoodScore = (strategyParameters.getTargetAuthorMissingPenaltyPercent() * (originalAuthorshipLikelihoodScore/100));
				 		                	 
				 		                	 article.setAuthorshipLikelihoodScore(authorshipLikelyhoodScore);
				 		                	article.setTargetAuthorCountPenalty(authorshipLikelyhoodScore - originalAuthorshipLikelihoodScore);
				 		                 }
				 		            return article;
				 				 })
						         .filter(Objects::nonNull)
						         .collect(Collectors.toList());
	}
	// Build a lookup map from JSONArray for O(1) score access per article
	private static Map<Long, Double> buildScoreMap(JSONArray jsonArray) {
	    Map<Long, Double> scoreMap = new HashMap<>(jsonArray.length());
	    for (int i = 0; i < jsonArray.length(); i++) {
	        JSONObject obj = jsonArray.getJSONObject(i);
	        scoreMap.put(obj.getLong("id"), obj.optDouble("scoreTotal", 0.0));
	    }
	    return scoreMap;
	}
	private boolean uploadJsonFileIntoS3(String keyName,File file)
	{
		String feedbackScoreBucketName = PropertiesUtils.get("aws.s3.feedback.score.bucketName");
        
		// Upload the python file
				 S3Client s3 = AwsScoringClients.s3();
				 try {
					// Check if the bucket exists
				        HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
				                .bucket(feedbackScoreBucketName)
				                .build();
				        try {
							s3.headBucket(headBucketRequest);
							log.info("Uploading files to S3 bucket: {}", feedbackScoreBucketName);
						} catch (S3Exception e) {
							if (e.statusCode() == 404) {
								log.error("S3 bucket does not exist: {}" , feedbackScoreBucketName);
								return false;
							}
						}
				        // Upload file
			            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
			                    .bucket(feedbackScoreBucketName.toLowerCase())
			                    .key(keyName)
			                    .contentType("application/json") // Set metadata
			                    .build();
			            PutObjectResponse putObjectResponse = s3.putObject(putObjectRequest, RequestBody.fromFile(Paths.get(file.getAbsolutePath())));

			            if (putObjectResponse.sdkHttpResponse().isSuccessful()) {
			                log.info("JSON file uploaded successfully to S3 bucket:{} ", feedbackScoreBucketName);
			                return true;
			            } else {
			                log.error("Failed to upload JSON file to S3.");
			                return false;
			            }
			            
					
		        } catch (Exception e) {
					log.error(
							"Unexpected exception while uploading JSON file to S3. keyName={}, filePath={}, bucketName={}",
							keyName, file != null ? file.getAbsolutePath() : null, feedbackScoreBucketName, e);

		            return false;		 
		        }
			}

		}
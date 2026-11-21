package reciter.engine;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reciter.algorithm.cluster.similarity.clusteringstrategy.article.MeshMajorClusteringStrategy;
import reciter.api.parameters.UseGoldStandard;
import reciter.engine.analysis.ReCiterArticleAuthorFeature;
import reciter.engine.analysis.ReCiterArticleFeature;
import reciter.engine.analysis.ReCiterArticleFeature.ArticleKeyword;
import reciter.engine.analysis.ReCiterArticleFeature.ArticleKeyword.KeywordType;
import reciter.engine.analysis.ReCiterArticleFeature.PublicationFeedback;
import reciter.engine.analysis.ReCiterArticlePublicationType;
import reciter.engine.analysis.ReCiterFeature;
import reciter.engine.analysis.evidence.Evidence;
import reciter.engine.erroranalysis.Analysis;
import reciter.model.article.ReCiterArticle;
import reciter.model.article.ReCiterArticleMeshHeading;
import reciter.model.article.ReCiterAuthor;
import reciter.model.identity.Identity;
import reciter.utils.ReCiterStringUtil;
import lombok.Data;

@Data
public class ReCiterFeatureGenerator {

	private static final Logger log = LoggerFactory.getLogger(ReCiterFeatureGenerator.class);
	
    private double precision;
    private double recall;

    private int truePos;
    private int trueNeg;
    private int falseNeg;
    private int falsePos;
    private int goldStandardSize;
    private int selectedClusterSize;
    private List<Long> truePositiveList = new ArrayList<>();
    private List<Long> trueNegativeList = new ArrayList<>();
    private List<Long> falsePositiveList = new ArrayList<>();
    private List<Long> falseNegativeList = new ArrayList<>();

    public ReCiterFeature computeFeatures(UseGoldStandard mode,
                                          final double filterScore,
                                          final double keywordsMax,
                                          List<ReCiterArticle> reCiterArticles,
                                          List<Long> goldStandardPmids,
                                          List<Long> rejectedPmids,Identity identity) {

        ReCiterFeature reCiterFeature = new ReCiterFeature();
        reCiterFeature.setPersonIdentifier(identity.getUid());
        reCiterFeature.setDateAdded(Instant.now()); // TODO Add 'date_added' to identity.
        reCiterFeature.setDateUpdated(Instant.now()); // TODO add 'date_updated' to identity.
        reCiterFeature.setMode(mode);

        Set<Long> pmidsRetrieved = new HashSet<>();
        for (ReCiterArticle reCiterArticle : reCiterArticles) {
            pmidsRetrieved.add(reCiterArticle.getArticleId());
        }
    // in gold standard but not retrieved TODO optimize
    List<Long> inGoldStandardButNotRetrieved = new ArrayList<>();
    if(goldStandardPmids != null && goldStandardPmids.size() > 0) {
        for (long pmid : goldStandardPmids) {
            if (!pmidsRetrieved.contains(pmid)) {
				log.info("Updating accepted inGoldStandardButNotRetrieved List {} ", pmid); 
                inGoldStandardButNotRetrieved.add(pmid);
            }
        }
    }
    if(rejectedPmids != null && rejectedPmids.size() > 0) {
        for (long pmid : rejectedPmids) {
            if (!pmidsRetrieved.contains(pmid)) {
				 log.info("Updating rejected inGoldStandardButNotRetrieved List {}" , pmid);
                inGoldStandardButNotRetrieved.add(pmid);
            }
        }
    }
	reCiterFeature.setInGoldStandardButNotRetrieved(inGoldStandardButNotRetrieved);
	
        List<ReCiterArticle> selectedArticles = new ArrayList<>();
        if(mode == UseGoldStandard.AS_EVIDENCE) {
        	selectedArticles = reCiterArticles 
        			.stream()
        			.filter(reCiterArticle -> reCiterArticle.getAuthorshipLikelihoodScore() >= filterScore || reCiterArticle.getGoldStandard() == 1 || reCiterArticle.getGoldStandard() == -1)
        			.collect(Collectors.toList());
        } else {
        	
        	selectedArticles = reCiterArticles
        			.stream()
        			.filter(reCiterArticle -> reCiterArticle.getAuthorshipLikelihoodScore() >= filterScore)
        			.collect(Collectors.toList());
        }

        reCiterFeature.setCountSuggestedArticles(selectedArticles.size());

        // Count of pending publications
        List<ReCiterArticle> pendingArticles = selectedArticles
                .stream()
                .filter(reCiterArticle -> reCiterArticle.getAuthorshipLikelihoodScore() >= filterScore && reCiterArticle.getGoldStandard() == 0)
                .collect(Collectors.toList());
        reCiterFeature.setCountPendingArticles(pendingArticles.size());
       // List<Long> filteredArticles = selectedArticles.stream().map(article -> article.getArticleId()).collect(Collectors.toList());
        
       // Analysis analysis = Analysis.performAnalysis(finalArticles, filteredArticles, goldStandardPmids);
        Analysis analysis = Analysis.performAnalysis(reCiterArticles,goldStandardPmids);
        
        log.info("Analysis for uid=[" + identity.getUid() + "]");
        log.info("Precision=" + analysis.getPrecision());
        log.info("Recall=" + analysis.getRecall());
        
        double truePositiveListSize = 0.0;
        double trueNegativeListSize = 0.0;
        double falsePositiveListSize = 0.0;
        double falseNegativeListSize =0.0;
        // Calculate accuracy using the new formula
        if(analysis.getTruePositiveList()!=null && analysis.getTruePositiveList().size() > 0)
        {
        	truePositiveListSize = analysis.getTruePositiveList().size();
        	log.info("True Positive List [" + analysis.getTruePositiveList().size() + "]: " + analysis.getTruePositiveList());
        }
        if(analysis.getTrueNegativeList()!=null && analysis.getTrueNegativeList().size() > 0)
        {
        	trueNegativeListSize = analysis.getTrueNegativeList().size();
        	log.info("True Negative List: [" + analysis.getTrueNegativeList().size() + "]: " + analysis.getTrueNegativeList());
        }
        if(analysis.getFalsePositiveList()!=null && analysis.getFalsePositiveList().size() > 0)
        {
        	falsePositiveListSize = analysis.getFalsePositiveList().size();
        	log.info("False Positive List: [" + analysis.getFalsePositiveList().size() + "]: " + analysis.getFalsePositiveList());
        }
        if(analysis.getFalseNegativeList()!=null && analysis.getFalseNegativeList().size() > 0)
        {
        	falseNegativeListSize = analysis.getFalseNegativeList().size();
        	log.info("False Negative List: [" + analysis.getFalseNegativeList().size() + "]: " + analysis.getFalseNegativeList());
        }
        double accuracy =0.0;
        if((truePositiveListSize + trueNegativeListSize + falsePositiveListSize + falseNegativeListSize) > 0)
        	accuracy = 
        		(double)(truePositiveListSize + trueNegativeListSize ) / (double)(truePositiveListSize + trueNegativeListSize + falsePositiveListSize + falseNegativeListSize);
        
        log.info("Accuracy=" + accuracy);
        
        // Set overall accuracy using the new formula
        reCiterFeature.setOverallAccuracy(Double.isNaN(accuracy)? 0.0:accuracy);

        // precision
        reCiterFeature.setPrecision(analysis.getPrecision());

        // recall
        reCiterFeature.setRecall(analysis.getRecall());

        // "suggestedArticles"
        List<ReCiterArticleFeature> reCiterArticleFeatures = new ArrayList<>(selectedArticles.size());
        for (ReCiterArticle reCiterArticle : selectedArticles) {
            ReCiterArticleFeature reCiterArticleFeature = new ReCiterArticleFeature();
            reCiterArticleFeature.setPmid(reCiterArticle.getArticleId());
       
            //authorshipLikelihoodScore
			reCiterArticleFeature.setAuthorshipLikelihoodScore(reCiterArticle.getAuthorshipLikelihoodScore()); 
            // true; false; null. make it Boolean
            // userAssertion TODO get from DB
            //if(goldStandardPmids != null && goldStandardPmids.contains(reCiterArticle.getArticleId())) {
            if (reCiterArticle.getGoldStandard() == 1) {
                reCiterArticleFeature.setUserAssertion(PublicationFeedback.ACCEPTED);
            } //else if(rejectedPmids != null && rejectedPmids.contains(reCiterArticle.getArticleId())) {
            else if (reCiterArticle.getGoldStandard() == -1) {
                reCiterArticleFeature.setUserAssertion(PublicationFeedback.REJECTED);
            } else if (reCiterArticle.getGoldStandard() == 0) {
                reCiterArticleFeature.setUserAssertion(PublicationFeedback.NULL);
            }

            //publicationDateDisplay
            reCiterArticleFeature.setPublicationDateDisplay(reCiterArticle.getPublicationDateDisplay());
            
            //publicationDateStandardized
            reCiterArticleFeature.setPublicationDateStandardized(reCiterArticle.getPublicationDateStandardized());
            
            //datePublicationAddedToEntrez
            if(reCiterArticle.getDatePublicationAddedToEntrez() != null) {
            	reCiterArticleFeature.setDatePublicationAddedToEntrez(reCiterArticle.getDatePublicationAddedToEntrez());
            }

            //datePublicationAddedToPMC
            if(reCiterArticle.getDatePublicationAddedToPMC() != null) {
            	reCiterArticleFeature.setDatePublicationAddedToPMC(reCiterArticle.getDatePublicationAddedToPMC());
            }

            //Publication type
            ReCiterArticlePublicationType reCiterPublicationType = ReCiterArticlePublicationType.builder().build();
            
            if(reCiterArticle.getPublicationTypeCanonical() != null) {
            	reCiterPublicationType.setPublicationTypeCanonical(reCiterArticle.getPublicationTypeCanonical());
            }
            if(reCiterArticle.getPublicationTypePubmed() != null) {
            	reCiterPublicationType.setPublicationTypePubMed(reCiterArticle.getPublicationTypePubmed());
            }
            if(reCiterArticle.getPublicationTypeScopus() != null) {
            	reCiterPublicationType.setPublicationTypeScopus(reCiterArticle.getPublicationTypeScopus());
            }
            
            reCiterArticleFeature.setPublicationType(reCiterPublicationType);
            
            //times cited
            if(reCiterArticle.getTimesCited() != null) {
            	reCiterArticleFeature.setTimesCited(reCiterArticle.getTimesCited());
            }
            
            //abstract
            if(reCiterArticle.getPublicationAbstract() != null) {
            	reCiterArticleFeature.setPublicationAbstract(ReCiterStringUtil.stripBackslashes(reCiterArticle.getPublicationAbstract()));
            }
            
            //article keywords
            if(reCiterArticle.getMeshHeadings() != null && !reCiterArticle.getMeshHeadings().isEmpty()) {
                List<ReCiterArticleFeature.ArticleKeyword> articleKeywords = new ArrayList<>();
                for (ReCiterArticleMeshHeading reCiterArticleMeshHeading : reCiterArticle.getMeshHeadings()) {
                    if(MeshMajorClusteringStrategy.isMeshMajor(reCiterArticleMeshHeading)) {
                        ReCiterArticleFeature.ArticleKeyword articleKeyword = new ArticleKeyword(reCiterArticleMeshHeading.getDescriptorName().getDescriptorName(), KeywordType.MESH_MAJOR, EngineParameters.getMeshCountMap().get(reCiterArticleMeshHeading.getDescriptorName().getDescriptorName()));
                        articleKeywords.add(articleKeyword);
                    }
                }
                if(!articleKeywords.isEmpty()) {
                    reCiterArticleFeature.setArticleKeywords(articleKeywords);
                }
                
            }
            //scopus doc id
            if(reCiterArticle.getScopusDocId() != null) {
            	reCiterArticleFeature.setScopusDocID(ReCiterStringUtil.stripBackslashes(reCiterArticle.getScopusDocId()));
            }
       
            // journal title
            reCiterArticleFeature.setJournalTitleVerbose(reCiterArticle.getJournal() == null ? null : ReCiterStringUtil.stripBackslashes(reCiterArticle.getJournal().getJournalTitle()));
            
            //journal issn
            reCiterArticleFeature.setIssn(reCiterArticle.getJournal() == null ? null : reCiterArticle.getJournal().getJournalIssn());

            // journal title ISO Abbreviation
            reCiterArticleFeature.setJournalTitleISOabbreviation(reCiterArticle.getJournal() == null ? null : ReCiterStringUtil.stripBackslashes(reCiterArticle.getJournal().getIsoAbbreviation()));

            // article title
            reCiterArticleFeature.setArticleTitle(ReCiterStringUtil.stripBackslashes(reCiterArticle.getArticleTitle()));

            // volume
            reCiterArticleFeature.setVolume(ReCiterStringUtil.stripBackslashes(reCiterArticle.getVolume()));
            
            // issue
            reCiterArticleFeature.setIssue(ReCiterStringUtil.stripBackslashes(reCiterArticle.getIssue()));

            /**
             * <Pagination>
             <MedlinePgn>1083-95</MedlinePgn> get the first one.
             </Pagination>
             */
            // pages
            reCiterArticleFeature.setPages(ReCiterStringUtil.stripBackslashes(reCiterArticle.getPages()));

            // pmcid (https://www.ncbi.nlm.nih.gov/pubmed/26174865?report=xml&format=text)
            // Need to add parsing for <OtherID Source="NLM">PMC5009940 [Available on 01/01/17]</OtherID>
            // Or check <ArticleId IdType="pmc">PMC2907408</ArticleId>
            if(reCiterArticle.getPmcid() != null) {
            	reCiterArticleFeature.setPmcid(ReCiterStringUtil.stripBackslashes(reCiterArticle.getPmcid()));
            }

            // doi
            // <ArticleId IdType="doi">10.1093/jnci/djq238</ArticleId>
            reCiterArticleFeature.setDoi(ReCiterStringUtil.stripBackslashes(reCiterArticle.getDoi()));

            // author list
            List<ReCiterArticleAuthorFeature> reCiterArticleAuthorFeatures = new ArrayList<>();
            int i = 1;

            for (ReCiterAuthor reCiterArticleAuthor : reCiterArticle.getArticleCoAuthors().getAuthors()) {
                ReCiterArticleAuthorFeature reCiterArticleAuthorFeature = new ReCiterArticleAuthorFeature();
                // rank
                reCiterArticleAuthorFeature.setRank(i++);

                // lastname
                reCiterArticleAuthorFeature.setLastName(ReCiterStringUtil.stripBackslashes(reCiterArticleAuthor.getAuthorName().getLastName()));
                // first name
                reCiterArticleAuthorFeature.setFirstName(ReCiterStringUtil.stripBackslashes(reCiterArticleAuthor.getAuthorName().getFirstName()));
                // initials
                reCiterArticleAuthorFeature.setInitials(ReCiterStringUtil.stripBackslashes(reCiterArticleAuthor.getAuthorName().getFirstInitial()));
                // affiliation scopus
                //Commenting out as not necessary at the moment
                /*ReCiterArticleAffiliationFeature reCiterArticleAffiliationFeature =
                        new ReCiterArticleAffiliationFeature();
                if(reCiterArticleAuthor.getAffiliation() != null) {
                    reCiterArticleAffiliationFeature.setAffiliationStatementLabel(reCiterArticleAuthor.getAffiliation());
                    reCiterArticleAffiliationFeature.setAffiliationStatementLabelSource(InstitutionalAffiliationSource.PUBMED);
                }

                // affiliation Scopus 
                if(reCiterArticle.getScopusArticle() != null) {
                    Author scopusAuthor = reCiterArticle.getScopusArticle().getAuthors().stream().filter(author -> reCiterArticleAuthor.getRank() == author.getSeq()).findFirst().orElse(null);
                    if(scopusAuthor != null && scopusAuthor.getAfids() != null && !scopusAuthor.getAfids().isEmpty()) {
                        List<ReCiterArticleAffiliationFeature.ReCiterArticleAffiliationInstitution> affiliationInstitutions = new ArrayList<>(scopusAuthor.getAfids().size());
                        for(Integer afid: scopusAuthor.getAfids()) {
                            Affiliation affiliationScopus = reCiterArticle.getScopusArticle().getAffiliations()
                                    .stream()
                                    .filter(affiliation -> affiliation.getAfid() == afid)
                                    .findFirst()
                                    .orElse(null);
                            ReCiterArticleAffiliationFeature.ReCiterArticleAffiliationInstitution articleAffiliationInstitution = new ReCiterArticleAffiliationInstitution();
                            articleAffiliationInstitution.setAffiliationInstitutionSource(InstitutionalAffiliationSource.SCOPUS);
                            articleAffiliationInstitution.setAffiliationInstitutionId(afid);
                            if(affiliationScopus != null && affiliationScopus.getAffilname() != null)
                                articleAffiliationInstitution.setAffiliationInstitutionLabel(affiliationScopus.getAffilname());
                            affiliationInstitutions.add(articleAffiliationInstitution);
                        }
                        reCiterArticleAffiliationFeature.setAffiliationInstitutions(affiliationInstitutions);
                    }
                }
                reCiterArticleAuthorFeature.setAffiliations(reCiterArticleAffiliationFeature);*/
                //email
                if(reCiterArticleAuthor.getAffiliation() != null) {
                    Pattern pattern = Pattern.compile("([a-z0-9_.-]+)@([a-z0-9_.-]+[a-z])", Pattern.CASE_INSENSITIVE);
                    Matcher matcher = pattern.matcher(reCiterArticleAuthor.getAffiliation());
                    while(matcher.find()) {
                        reCiterArticleAuthorFeature.setEmail(matcher.group());
                    }
                }
                // isTargetAuthor
                reCiterArticleAuthorFeature.setTargetAuthor(reCiterArticleAuthor.isTargetAuthor());

                // Orcid
                if(reCiterArticleAuthor.getOrcid() != null && !reCiterArticleAuthor.getOrcid().isBlank()) {
                    Pattern pattern = Pattern.compile("[0-9]{4}-[0-9]{4}-[0-9]{4}-[0-9]{4}");
                    Matcher matcher = pattern.matcher(reCiterArticleAuthor.getOrcid());
                    if(matcher.find()) {
                        reCiterArticleAuthorFeature.setOrcid(matcher.group());
                    }
                }
                //EqualContrib
                if(reCiterArticleAuthor.getEqualContrib()!=null && !reCiterArticleAuthor.getEqualContrib().isBlank())
                {
                	reCiterArticleAuthorFeature.setEqualContrib(ReCiterStringUtil.stripBackslashes(reCiterArticleAuthor.getEqualContrib()));
                }
                

                reCiterArticleAuthorFeatures.add(reCiterArticleAuthorFeature);
            }
            reCiterArticleFeature.setReCiterArticleAuthorFeatures(reCiterArticleAuthorFeatures);

            Evidence evidence = new Evidence();
            // Affiliation Evidence
            if (reCiterArticle.getAffiliationEvidence() != null) {
                evidence.setAffiliationEvidence(reCiterArticle.getAffiliationEvidence());
            }

            // AuthorName Evidence (the most complete author name in the article)
            /*AuthorNameEvidence authorNameEvidence = new AuthorNameEvidence();
            ReCiterAuthor reCiterAuthor = RemoveByNameStrategy.getCorrectAuthor(reCiterArticle, identity);
            if (reCiterAuthor != null) {
                authorNameEvidence.setArticleAuthorName(reCiterAuthor.getAuthorName());
                authorNameEvidence.setInstitutionalAuthorName(identity.getPrimaryName());
            }
            positiveEvidence.setAuthorNameEvidence(authorNameEvidence);*/
            if (reCiterArticle.getAuthorNameEvidence() != null) {
                evidence.setAuthorNameEvidence(reCiterArticle.getAuthorNameEvidence());
            }

            // Grant Evidence
            if (reCiterArticle.getGrantEvidence() != null) {
                evidence.setGrantEvidence(reCiterArticle.getGrantEvidence());
            }
            
            //Journal Category Evidence
            if(reCiterArticle.getJournalCategoryEvidence() != null) {
            	evidence.setJournalCategoryEvidence(reCiterArticle.getJournalCategoryEvidence());
            }
            // Relationship Evidence
            if (reCiterArticle.getRelationshipEvidence() != null) {
            
            /**
             * TODO : remove the relationshipEvidenceTotalScore from the response. currently it is displaying as 0 	
             */
            	 // Create an ObjectMapper
              /*  ObjectMapper objectMapper = new ObjectMapper();
                
                objectMapper.addMixIn(RelationshipEvidence.class, RelationshipEvidenceMixIn.class);
                
                // Create a FilterProvider to exclude the 'relationshipEvidenceTotalScore' property dynamically during serialization
                SimpleFilterProvider filters = new SimpleFilterProvider()
                        .addFilter("dynamicFilter", SimpleBeanPropertyFilter.serializeAllExcept("relationshipEvidenceTotalScore"));

                // Set the filter provider to the ObjectMapper
                objectMapper.setFilterProvider(filters);
                
                RelationshipEvidence releationshipEvidence = null;
               	String relationshipEvidenceJson = null;
               	RelationshipEvidence result =null;
				try 
				{
					relationshipEvidenceJson = objectMapper.writeValueAsString(reCiterArticle.getRelationshipEvidence());
					releationshipEvidence = objectMapper.readValue(relationshipEvidenceJson, RelationshipEvidence.class);
					
					// The object remains as RelationshipEvidence
		            // Serialize the object back to JSON (field 'relationshipEvidenceTotalScore' will be excluded)
		            result = objectMapper.convertValue(releationshipEvidence, RelationshipEvidence.class);
		        	
				} catch (JsonMappingException e) {
					e.printStackTrace();
				} catch (JsonProcessingException e) {
					e.printStackTrace();
				}*/
            	
            	/* RelationshipEvidence deserializedEvidence=null;
				try 
				{
					// Use the Jackson ObjectMapper
			        ObjectMapper objectMapper = JsonUtils.configureObjectMapper();
			        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
			        // Serialize to JSON (with relationshipEvidenceTotalScore completely ignored)
			        String jsonResponse = objectMapper.writeValueAsString(reCiterArticle.getRelationshipEvidence());
			        
			        // Print the JSON response (you should not see the `relationshipEvidenceTotalScore` field here)
			        //System.out.println("Serialized JSON: " + jsonResponse);

			        // Deserialize back into a RelationshipEvidence instance
			        deserializedEvidence = objectMapper.readValue(jsonResponse, RelationshipEvidence.class);

			        // The object itself is unchanged, and relationshipEvidenceTotalScore is still set to 0.0
			        //System.out.println("Deserialized Object: " + deserializedEvidence);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}*/
                evidence.setRelationshipEvidence(reCiterArticle.getRelationshipEvidence());//deserializedEvidence);
            }

            if (reCiterArticle.getEducationYearEvidence() != null) {
                evidence.setEducationYearEvidence(reCiterArticle.getEducationYearEvidence());
            }

            if (reCiterArticle.getAcceptedRejectedEvidence() != null) {
                evidence.setAcceptedRejectedEvidence(reCiterArticle.getAcceptedRejectedEvidence());
            }

            if (reCiterArticle.getOrganizationalUnitEvidences() != null) {
                evidence.setOrganizationalUnitEvidence(reCiterArticle.getOrganizationalUnitEvidences());
            }

            if (reCiterArticle.getPersonTypeEvidence() != null) {
                evidence.setPersonTypeEvidence(reCiterArticle.getPersonTypeEvidence());
            }

            if (reCiterArticle.getEmailEvidence() != null) {
                evidence.setEmailEvidence(reCiterArticle.getEmailEvidence());
            }

            if (reCiterArticle.getArticleCountEvidence() != null) {
                evidence.setArticleCountEvidence(reCiterArticle.getArticleCountEvidence());
            }
  
            if (reCiterArticle.getAuthorCountEvidence() != null) {
            	evidence.setAuthorCountEvidence(reCiterArticle.getAuthorCountEvidence());
            }
            
            if (reCiterArticle.getAverageClusteringEvidence() != null) {
                evidence.setAverageClusteringEvidence(reCiterArticle.getAverageClusteringEvidence());
            }
            
            if(reCiterArticle.getGenderEvidence() != null && reCiterArticle.getGenderEvidence().getGenderScoreIdentityArticleDiscrepancy() != null) {
            	evidence.setGenderEvidence(reCiterArticle.getGenderEvidence());
            }
            // Clustering Evidence
            //positiveEvidence.setClusteringEvidence(reCiterArticle.getClusteringEvidence());
            
            if(mode == UseGoldStandard.AS_EVIDENCE && reCiterArticle.getFeedbackEvidence() !=null ) {
				evidence.setFeedbackEvidence(reCiterArticle.getFeedbackEvidence());
			}
            evidence.setTargetAuthorCount(reCiterArticle.getTargetAuthorCount());
            evidence.setTargetAuthorCountPenalty(reCiterArticle.getTargetAuthorCountPenalty());
            
            reCiterArticleFeature.setEvidence(evidence);
 
            reCiterArticleFeatures.add(reCiterArticleFeature);
        }
        calculateTopKeywords(reCiterArticleFeatures, reCiterFeature, keywordsMax);
        //Sorting the List in descending order based on TotalScoreNonStandardized
       // reCiterFeature.setReCiterArticleFeatures(reCiterArticleFeatures.stream().sorted(Comparator.comparing(ReCiterArticleFeature::getTotalArticleScoreNonStandardized).reversed()).collect(Collectors.toList()));
        reCiterFeature.setReCiterArticleFeatures(reCiterArticleFeatures.stream().sorted(Comparator.comparing(ReCiterArticleFeature::getAuthorshipLikelihoodScore).reversed()).collect(Collectors.toList()));
	
        return reCiterFeature;
    }

    private void calculateTopKeywords(List<ReCiterArticleFeature> reCiterArticleFeatures, ReCiterFeature reCiterFeature, double keywordsMax) {
        
        Map<String, Long> acceptedArticleKeywords = reCiterArticleFeatures
            .stream()
            .filter(reCiterArticleFeature -> reCiterArticleFeature.getUserAssertion() != null 
                && reCiterArticleFeature.getUserAssertion() == PublicationFeedback.ACCEPTED
                && reCiterArticleFeature.getArticleKeywords() != null)
            .flatMap(keyword -> keyword.getArticleKeywords().stream())
            .map(ReCiterArticleFeature.ArticleKeyword::getKeyword)
            .collect(Collectors.groupingBy(acceptedKeyword -> acceptedKeyword , Collectors.counting()));

        if(acceptedArticleKeywords != null && !acceptedArticleKeywords.isEmpty()) {
            //Sort descending by Count and ascending by Keyword
            LinkedHashMap<String, Long> sortedAcceptedKeywordCount = acceptedArticleKeywords.entrySet().stream()
                        .sorted(Map.Entry
                        .<String, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                        .limit((long)keywordsMax)
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                                (oldValue, newValue) -> oldValue, LinkedHashMap::new));
            
            List<ReCiterArticleFeature.ArticleKeyword> acceptedKeywords = 
                sortedAcceptedKeywordCount.entrySet()
                    .stream()
                    .map(keyword -> 
                        new ArticleKeyword(keyword.getKey(), KeywordType.MESH_MAJOR, keyword.getValue()))
                    .collect(Collectors.toList());
            
            reCiterFeature.setArticleKeywordsAcceptedArticles(acceptedKeywords);
        }
    }
 
 //obsolete   
public ReCiterFeature updateFeedbackFeatures(ReCiterFeature reCiterFeature,final double filterScore, List<ReCiterArticle> reciterArticles, List<Long> goldStandardPmids, List<Long> rejectedPmids, Identity identity) {
		
		
		List<ReCiterArticle> selectedArticles = new ArrayList<>();
        	selectedArticles = reciterArticles
        			.stream()
        			.filter(reCiterArticle -> reCiterArticle.getTotalArticleScoreStandardized() >= filterScore || reCiterArticle.getGoldStandard() == 1 || reCiterArticle.getGoldStandard() == -1)
        			.collect(Collectors.toList());
         
        
      // "suggestedArticles"
		List<ReCiterArticleFeature> reCiterArticleFeatures = reCiterFeature.getReCiterArticleFeatures();
		for (ReCiterArticle reCiterArticle : selectedArticles) {
			//ReCiterArticleFeature reCiterArticleFeature = new ReCiterArticleFeature();

			for(ReCiterArticleFeature reCiterArticleFeature : reCiterArticleFeatures)
			{
				if(reCiterArticle!=null && reCiterArticleFeature!=null && reCiterArticle.getArticleId() == reCiterArticleFeature.getPmid())
				{	
					//authorshipLikelihoodScore
					reCiterArticleFeature.setAuthorshipLikelihoodScore(reCiterArticle.getAuthorshipLikelihoodScore());
					
					Evidence evidence = reCiterArticleFeature.getEvidence();
		
					//Feedback Evidence
					if(reCiterArticle.getFeedbackEvidence() !=null ) {
						evidence.setFeedbackEvidence(reCiterArticle.getFeedbackEvidence());
					}
					reCiterArticleFeature.setEvidence(evidence);
		
					//reCiterArticleFeatures.add(reCiterArticleFeature);
					break;
			   }
			}
		}
		
		//Sorting the List in descending order based on TotalScoreNonStandardized
		reCiterFeature.setReCiterArticleFeatures(reCiterArticleFeatures.stream()
				.sorted(Comparator.comparing(ReCiterArticleFeature::getAuthorshipLikelihoodScore).reversed())
				.collect(Collectors.toList()));

		return reCiterFeature;
	}
	
}
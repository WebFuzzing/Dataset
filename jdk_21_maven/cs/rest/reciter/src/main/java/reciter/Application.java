/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package reciter;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.annotation.EnableAsync;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterables;

import reciter.database.dyanmodb.files.GenderFileImport;
import reciter.database.dyanmodb.files.IdentityFileImport;
import reciter.database.dyanmodb.files.InstitutionAfidFileImport;
import reciter.database.dyanmodb.files.MeshTermFileImport;
import reciter.database.dyanmodb.files.NameFrequencyFileImport;
import reciter.database.dyanmodb.files.ScienceMetrixDepartmentCategoryFileImport;
import reciter.database.dyanmodb.files.ScienceMetrixFileImport;
import reciter.database.dynamodb.model.Gender;
import reciter.database.dynamodb.model.InstitutionAfid;
import reciter.database.dynamodb.model.MeshTerm;
import reciter.database.dynamodb.model.NameFrequency;
import reciter.database.dynamodb.model.ScienceMetrix;
import reciter.database.dynamodb.model.ScienceMetrixDepartmentCategory;
import reciter.engine.EngineParameters;
import reciter.security.APIKey;
import reciter.service.GenderService;
import reciter.service.NameFrequencyService;
import reciter.service.ScienceMetrixDepartmentCategoryService;
import reciter.service.ScienceMetrixService;
import reciter.service.dynamo.DynamoDbInstitutionAfidService;
import reciter.service.dynamo.DynamoDbMeshTermService;
import reciter.utils.AffiliationStrategyUtils;
import reciter.utils.DegreeYearStrategyUtils;

@SpringBootApplication
@Configuration
@EnableAutoConfiguration
@EnableAsync
@ComponentScan("reciter")
public class Application {
	
	private static final Logger log = LoggerFactory.getLogger(Application.class);

	@Autowired
	private DynamoDbMeshTermService dynamoDbMeshTermService;

	@Autowired
	@Qualifier("scienceMetrixServiceImpl")
	private ScienceMetrixService scienceMetrixService;

	@Autowired
	private ScienceMetrixDepartmentCategoryService scienceMetrixDepartmentCategoryService;

	@Autowired
	private DynamoDbInstitutionAfidService dynamoDbInstitutionAfidService;

	@Autowired
	private GenderService genderService;

    @Autowired
    private NameFrequencyService nameFrequencyService;
    
    @Autowired
    private DegreeYearStrategyUtils degreeYearStrategyUtils;
    
    @Autowired
    private AffiliationStrategyUtils affiliationStrategyUtils;

    @Value("${use.scopus.articles}")
    private boolean useScopusArticles;

    @Value("${spring.security.enabled}")
    private boolean useAPISecurity;

    @Value("${aws.dynamodb.settings.file.import}")
    private boolean isFileImport;

    @Value("${strategy.gender}")
	private boolean useGenderStrategy;

    @Value("${strategy.nameFrequency:true}")
	private boolean useNameFrequencyStrategy;
	
	@Value("${strategy.discrepancyDegreeYear.degreeYearDiscrepancyScore}")
	private String degreeYearDiscrepancyScore;
	
	@Value("${strategy.authorAffiliationScoringStrategy.institutionStopwords}")
    private String instAfflInstitutionStopwords;
    
    private String scopusService = System.getenv("SCOPUS_SERVICE");
    
    private String pubmedService = System.getenv("PUBMED_SERVICE");
    
    private String reciterScoringService = System.getenv("RECITERSCORING_SERVICE_URL");
    
	@Autowired 
	private Environment env;
	
	@Value("${aws.reciterscoring.service.portNo}")
	private String reciterScoringPortNumber;
	
	
	public static void main(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(Application.class, args);
		
		Iterable<ObjectMapper> objectMappers = context.getBeansOfType(ObjectMapper.class)
	            .values();
		
        ObjectMapper objectMapper = Iterables.getFirst(objectMappers, null);

        // Enable Squiggly for Jackson message converter
        if (objectMapper != null) {
            for (MappingJackson2HttpMessageConverter converter : context.getBeansOfType(MappingJackson2HttpMessageConverter.class).values()) {
                converter.setObjectMapper(objectMapper);
            }
        }
	}
	
	
	/**
	 * This function will check for pubmed and scopus service url provided
	 */
	@EventListener(ApplicationReadyEvent.class)
	public void checkScopusPubmedService() {
		if(useScopusArticles && scopusService != null && !scopusService.isBlank()) {
			try {
				URL siteURL = new URL(scopusService + "/scopus/ping");
				HttpURLConnection connection = (HttpURLConnection) siteURL.openConnection();
				connection.setRequestMethod("GET");
				connection.setConnectTimeout(10000);
				connection.connect();
	 
				int code = connection.getResponseCode();
				if (code == 200) {
					log.info("The Scopus Service endpoint {} provided is valid and reachable", scopusService);
				} else {
					log.info("The Scopus Service endpoint {} provided is not valid and not reachable", scopusService);
				}
			} catch (Exception e) {
				log.error("Failed to connect to Scopus service endpoint Wrong domain - Exception {}", scopusService, e);
	 
			}
		}
		
		if(pubmedService != null && !pubmedService.isBlank()) {
			try {
				URL siteURL = new URL(pubmedService  + "/pubmed/ping");
				HttpURLConnection connection = (HttpURLConnection) siteURL.openConnection();
				connection.setRequestMethod("GET");
				connection.setConnectTimeout(10000);
				connection.connect();
	 
				int code = connection.getResponseCode();
				if (code == 200) {
					log.info("The Pubmed Service endpoint {} provided is valid and reachable",pubmedService);
				} else {
					log.info("The Pubmed Service endpoint {} provided is not valid and not reachable",pubmedService);
				}
			} catch (Exception e) {
				log.error("Wrong domain - Exception: " + e.getMessage());
	 
			}
		}
		
		
		if(reciterScoringService != null && !reciterScoringService.isBlank()) 
		{
			String urlString = "http://localhost:"+ reciterScoringPortNumber +"/2015-03-31/functions/function/invocations";
	        ObjectMapper mapper = new ObjectMapper();
	        
	        Map<String, Object> payloadMap = new HashMap<>();
	        payloadMap.put("category", "test");
	        payloadMap.put("file_Name", "test");
	        payloadMap.put("useS3Bucket", "feedbackScore");
	        payloadMap.put("bucket_name", false);
	        
	        String payloadJson=null;
			try {
				payloadJson = mapper.writeValueAsString(payloadMap);
			} catch (JsonProcessingException e) {
				log.error("ReCiter---Scoring payload: Failed to serialize payload map to JSON {} ",e.getMessage());
			}
	        
	        try {
		            URL url = new URL(urlString);
		            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		            connection.setRequestMethod("POST");
		            connection.setRequestProperty("Content-Type", "application/json");
		            connection.setDoOutput(true);
		            connection.getOutputStream().write(payloadJson.getBytes("UTF-8"));
		            int responseCode = connection.getResponseCode();
		            if (responseCode == 200) {
						log.info("The reciterScoring Service endpoint {} provided is valid and reachable",reciterScoringService);
					} else {
						log.info("The ReciterScoring Service endpoint {} provided is not valid and not reachable",reciterScoringService);
					}
	        }catch (Exception e) {
				log.error("ReCiter---Scoring Container is not running. Please start the service: {}" , e.getMessage());
			}
		}
		
		if(pubmedService == null) {
			log.warn("ReCiter Application will not run without a pubmed service. Please download from https://github.com/wcmc-its/ReCiter-PubMed-Retrieval-Tool.git and setup Pubmed Service.");
		}
		
		if(scopusService == null) {
			if(useScopusArticles) {
				log.warn("The property `use.scopus.articles` is set to `true` in the application.properties file. If you haven't done so already, please be sure to install the ReCiter Scopus Retrieval Tool, located at https://github.com/wcmc-its/ReCiter-Scopus-Retrieval-Tool. If you wish to run ReCiter without the Scopus service, set `use.scopus.articles=false`.");
			} else {
				log.warn("The property `use.scopus.articles` is set to `false` in the application.properties file, so it will not be using the ReCiter Scopus Retrieval Tool. To install the ReCiter Scopus Retrieval Tool, you would need to go to https://github.com/wcmc-its/ReCiter-Scopus-Retrieval-Tool and set property `use.scopus.articles=true`.");
			}
		}
		if(reciterScoringService == null || reciterScoringService.isBlank()) {
			log.warn("ReCiter Application will not run without a ReCiterScoring service. Please download from https://github.com/wcmc-its/ReCiter---Scoring.git and setup ReCiter---Scoring Service.");
		}
	}
	
	
	/**
	 * This function will load all the necessary data from files folder to dynamodb
	 */
	@EventListener(ApplicationReadyEvent.class)
	public void loadDynamoDbTablesAfterStartUp() {
		if(isFileImport) {
			ScienceMetrixDepartmentCategoryFileImport scienceMetrixDepartmentCategoryFileImport = ApplicationContextHolder.getContext().getBean(ScienceMetrixDepartmentCategoryFileImport.class);
			scienceMetrixDepartmentCategoryFileImport.importScienceMetrixDepartmentCategory();
			
			ScienceMetrixFileImport scienceMetrixFileImport = ApplicationContextHolder.getContext().getBean(ScienceMetrixFileImport.class);
			scienceMetrixFileImport.importScienceMetrix();
			
			MeshTermFileImport meshTermFileImport = ApplicationContextHolder.getContext().getBean(MeshTermFileImport.class);
			meshTermFileImport.importMeshTerms();
			
			IdentityFileImport identityFileImport = ApplicationContextHolder.getContext().getBean(IdentityFileImport.class);
			identityFileImport.importIdentity();
			
			if(useGenderStrategy) {
				GenderFileImport genderFileImport = ApplicationContextHolder.getContext().getBean(GenderFileImport.class);
				genderFileImport.importGender();
			} else {
				log.info("Gender strategy use is set to false. Please update strategy.gender to true in application.properties file to use it.\n"
			+ "Its recommened to use this strategy to get better scores.");
			}

			if(useNameFrequencyStrategy) {
				NameFrequencyFileImport nameFrequencyFileImport = ApplicationContextHolder.getContext().getBean(NameFrequencyFileImport.class);
				nameFrequencyFileImport.importNameFrequency();
			} else {
				log.info("NameFrequency strategy use is set to false. Set strategy.nameFrequency=true in application.properties to enable.");
			}
			
			if(useScopusArticles) {
				InstitutionAfidFileImport institutionAfidFileImport = ApplicationContextHolder.getContext().getBean(InstitutionAfidFileImport.class);
				institutionAfidFileImport.importInstitutionAfids();
			}
		}
	}
	
	@EventListener(ApplicationReadyEvent.class)
	public void populateStaticEngineParameters() {
		
		log.info("Loading ScienceMetrixJournals to Engine Parameters");
		List<ScienceMetrix> scienceMetrixJournals = scienceMetrixService.findAll();
		if (scienceMetrixJournals != null) {
			EngineParameters.setScienceMetrixJournals(scienceMetrixJournals);
		}

        log.info("Loading ScienceMetrixDepartmentCategories to Engine Parameters");
		
		List<ScienceMetrixDepartmentCategory> scienceMetrixDeptCategories = scienceMetrixDepartmentCategoryService
				.findAll();
		if (scienceMetrixDeptCategories != null) {
			EngineParameters.setScienceMetrixDepartmentCategories(scienceMetrixDeptCategories);
		}
		 
        
        log.info("Loading MeshTermCounts to Engine Parameters");
        if (EngineParameters.getMeshCountMap() == null) {
            List<MeshTerm> meshTerms = dynamoDbMeshTermService.findAll();
            Map<String, Long> meshCountMap =
                    meshTerms.stream()
                             .collect(Collectors.toMap(
                                     MeshTerm::getMesh,
                                     MeshTerm::getCount));
            EngineParameters.setMeshCountMap(meshCountMap);
        }
        if(useGenderStrategy) {
	        log.info("Loading GenderProbability to Engine Parameters");
			
			List<Gender> genders = genderService.findAll();
			if (genders != null && !genders.isEmpty()) {
				EngineParameters.setGenders(genders);
			}

        }
        if(useNameFrequencyStrategy) {
	        log.info("Loading NameFrequency to Engine Parameters");
	        List<NameFrequency> nameFrequencies = nameFrequencyService.findAll();
	        if(nameFrequencies != null && !nameFrequencies.isEmpty()) {
	        	EngineParameters.setNameFrequencies(nameFrequencies);
	        	Map<String, NameFrequency> nameFrequencyMap = new HashMap<>();
	        	for (NameFrequency nf : nameFrequencies) {
	        		nameFrequencyMap.put(nf.getName(), nf);
	        	}
	        	EngineParameters.setNameFrequencyMap(nameFrequencyMap);
	        	log.info("Loaded {} name frequencies to Engine Parameters", nameFrequencies.size());
	        }
        }
        
        if(useScopusArticles) {
	        log.info("Loading ScopusInstitutionalAfids to Engine Parameters");
			
			List<InstitutionAfid> instAfids = dynamoDbInstitutionAfidService.findAll();
			if (instAfids != null && instAfids.size() > 0) {
				Map<String, List<String>> institutionAfids = instAfids.stream()
						.collect(Collectors.toMap(InstitutionAfid::getInstitution, InstitutionAfid::getAfids));
				EngineParameters.setAfiliationNameToAfidMap(institutionAfids);
			}
			 
		}
		EngineParameters.setDegreeYearDiscrepancyScoreMap(degreeYearStrategyUtils.getDegreeYearDiscrepancyScoreMap(this.degreeYearDiscrepancyScore));

		EngineParameters.setRegexForStopWords(affiliationStrategyUtils.constructRegexForStopWords(this.instAfflInstitutionStopwords));
		
        log.info("ReCiter is up and ready to use. Please make sure its other components such as Pubmed-Retrieval-Tool is also setup if you wish to do retrieval.");
	}
	


    /**
     * @return APIKey
     */
    @Bean
    public APIKey apiKeySetter() {
    	APIKey apiKey = new APIKey();
    	apiKey.setAdminApiKey(env.getProperty("ADMIN_API_KEY"));
    	apiKey.setConsumerApiKey(env.getProperty("CONSUMER_API_KEY"));
    	if(useAPISecurity && (apiKey.getAdminApiKey() == null || apiKey.getConsumerApiKey() == null)) {
    		throw new BeanCreationException("The ADMIN_API_KEY and CONSUMER_API_KEY should be set in environment variable. Or mark spring.security.enabled as false in application.propeties file.");
    	}
        return apiKey;
    }
}

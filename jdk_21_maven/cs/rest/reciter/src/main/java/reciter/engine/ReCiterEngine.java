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
package reciter.engine;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StopWatch;

import reciter.algorithm.cluster.article.scorer.ArticleScorer;
import reciter.algorithm.cluster.article.scorer.ReCiterArticleScorer;
import reciter.algorithm.feedback.article.scorer.ArticleFeedbackScorer;
import reciter.algorithm.feedback.article.scorer.ReciterFeedbackArticleScorer;
import reciter.api.parameters.UseGoldStandard;
import reciter.engine.analysis.ReCiterFeature;
import reciter.engine.erroranalysis.Analysis;
import reciter.model.article.ReCiterArticle;
import reciter.model.identity.Identity;




public class ReCiterEngine implements Engine {

	private static final Logger log = LoggerFactory.getLogger(ReCiterEngine.class);
	  
    @Override
    public EngineOutput run(EngineParameters parameters, StrategyParameters strategyParameters, double filterScore, double keywordsMax) {

        Identity identity = parameters.getIdentity();
      
        List<ReCiterArticle> reCiterArticles = parameters.getReciterArticles();

        log.info("ReCiter Articles in ReCiter Engine: " + reCiterArticles.size());
        Analysis.assignGoldStandard(reCiterArticles, parameters.getKnownPmids(), parameters.getRejectedPmids());

        EngineOutput engineOutput = new EngineOutput();
        
        ReCiterFeatureGenerator reCiterFeatureGenerator = new ReCiterFeatureGenerator();
        UseGoldStandard mode;
        if (strategyParameters.isUseGoldStandardEvidence()) {
            mode = UseGoldStandard.AS_EVIDENCE;
        } else {
            mode = UseGoldStandard.FOR_TESTING_ONLY;
        }
      
        //Checking for Accepted and Rejected articles 
        Map<Integer, List<ReCiterArticle>> groupedByGoldStandard = reCiterArticles.stream()
	            .collect(Collectors.groupingBy(ReCiterArticle::getGoldStandard));
		
		//articles with the user assertion 1
		int acceptedArticles = groupedByGoldStandard.getOrDefault(1, Collections.emptyList()).size();
		
		//articles with the user assertion -1
		int rejectedArticles = groupedByGoldStandard.getOrDefault(-1, Collections.emptyList()).size();
        
		 log.info("Accepted articles: " +acceptedArticles +"  and Rejected articles:  "+ rejectedArticles);

		 
        if(mode == UseGoldStandard.FOR_TESTING_ONLY || (acceptedArticles == 0 && rejectedArticles == 0))
        {
        	StopWatch stopWatch = new StopWatch("Article Scorer");
	        stopWatch.start("Article Scorer");
	        
            ArticleScorer articleScorer = new ReCiterArticleScorer(/*clusterer.getClusters()*/reCiterArticles, identity, strategyParameters);
	         articleScorer.runArticleScorer(reCiterArticles, identity);
	    	 if(reCiterArticles!=null && reCiterArticles.size() > 0)
	        	  articleScorer.executePythonScriptForArticleIdentityTotalScore(reCiterArticles,identity);
	        
	        ReCiterFeature reCiterFeature = reCiterFeatureGenerator.computeFeatures(
	                mode, filterScore, keywordsMax,
	                reCiterArticles, parameters.getKnownPmids(), parameters.getRejectedPmids(),identity);
	        
	        engineOutput.setReCiterFeature(reCiterFeature);
	
	        stopWatch.stop();
	        log.info(stopWatch.getId() + " took " + stopWatch.getTotalTimeSeconds() + "s");
        }
        else if(mode == UseGoldStandard.AS_EVIDENCE && (acceptedArticles > 0 || rejectedArticles > 0)) //useGoldstandardEvidence = true then it runs.
		{
			StopWatch stopWatch = new StopWatch("Article Identity and Feedback Scorer");
			stopWatch.start("Article Identity and Feedback Scorer");

			ArticleScorer articleScorer = new ReCiterArticleScorer(reCiterArticles, identity, strategyParameters);
			articleScorer.runArticleScorer(reCiterArticles, identity);

			ArticleFeedbackScorer feedbackArticleScorer = new ReciterFeedbackArticleScorer(reCiterArticles, identity,
					parameters, strategyParameters);
			feedbackArticleScorer.runFeedbackArticleScorer(reCiterArticles, identity);

			ReCiterFeature reCiterFeature = reCiterFeatureGenerator.computeFeatures(mode, filterScore, keywordsMax,
					reCiterArticles, parameters.getKnownPmids(), parameters.getRejectedPmids(), identity);

			if (reCiterFeature != null) {
				engineOutput.setReCiterFeature(reCiterFeature);
			} else {
				log.error("computeFeatures returned null for uid={}, falling back to identity-only scoring",
						identity.getUid());
				// Fallback: re-score with identity-only model
				ArticleScorer fallbackScorer = new ReCiterArticleScorer(reCiterArticles, identity, strategyParameters);
				fallbackScorer.runArticleScorer(reCiterArticles, identity);
				fallbackScorer.executePythonScriptForArticleIdentityTotalScore(reCiterArticles, identity);
				reCiterFeature = reCiterFeatureGenerator.computeFeatures(UseGoldStandard.FOR_TESTING_ONLY, filterScore,
						keywordsMax, reCiterArticles, parameters.getKnownPmids(), parameters.getRejectedPmids(),
						identity);
				engineOutput.setReCiterFeature(reCiterFeature);
			}

			stopWatch.stop();
			log.info(stopWatch.getId() + " took " + stopWatch.getTotalTimeSeconds() + "s");
		}
       
        return engineOutput;       
    }

}

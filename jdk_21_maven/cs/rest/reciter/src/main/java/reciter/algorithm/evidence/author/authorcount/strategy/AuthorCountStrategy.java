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
package reciter.algorithm.evidence.author.authorcount.strategy;

import java.util.List;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reciter.algorithm.evidence.targetauthor.AbstractTargetAuthorStrategy;
import reciter.engine.Feature;
import reciter.engine.StrategyParameters;
import reciter.engine.analysis.evidence.AuthorCountEvidence;
import reciter.model.article.ReCiterArticle;
import reciter.model.identity.Identity;

/**
 * @author mjangari
 * Author Count Strategy
 */
public class AuthorCountStrategy extends AbstractTargetAuthorStrategy {
	
	private static final Logger slf4jLogger = LoggerFactory.getLogger(AuthorCountStrategy.class);

	
	public static StrategyParameters strategyParameters;
	
	public AuthorCountStrategy(StrategyParameters strategyParameters) {
		this.strategyParameters = strategyParameters;
	}
	
	@Override
	public double executeStrategy(ReCiterArticle reCiterArticle, Identity identity) {
		return 0.0;
	}

	@Override
	public double executeStrategy(List<ReCiterArticle> reCiterArticles, Identity identity) {
		
			reCiterArticles.forEach(reCiterArticle -> {
				
				int authorCount = reCiterArticle.getArticleCoAuthors().getAuthors().size();
		  		AuthorCountEvidence authorCountEvidence = new AuthorCountEvidence();
				double adjustedScore = calculateAdjustedArticleCountScore.apply(authorCount);
				authorCountEvidence.setCountAuthors(authorCount);
				authorCountEvidence.setAuthorCountScore(adjustedScore);
				reCiterArticle.setAuthorCountEvidence(authorCountEvidence);
				
		});
		return 0.0;
	}

	@Override
	public void populateFeature(ReCiterArticle reCiterArticle, Identity identity, Feature feature) {
		// TODO Auto-generated method stub
		
	}
	 // Function to calculate likelihood adjustment
    private static Function<Integer, Double> calculateLikelihoodAdjustment = authorCount -> {
        // Baseline likelihood (at authorCountThreshold)
    	
        double y_baseline = strategyParameters.getLnCoefficient() * Math.log(strategyParameters.getAuthorCountThreshold()) + strategyParameters.getConstantCoefficient();

        // Likelihood for the given author count
        double y = authorCount > 0 ? strategyParameters.getLnCoefficient() * Math.log(authorCount) + strategyParameters.getConstantCoefficient() : y_baseline;

        // Adjustment is scaled by gamma
        return strategyParameters.getAuthorCountAdjustmentGamma() * (y - y_baseline);
    };

    // Function to calculate adjusted article count score
    private static Function<Integer, Double> calculateAdjustedArticleCountScore = authorCount -> {
        // Apply the likelihood adjustment function
        return calculateLikelihoodAdjustment.apply(authorCount);
    };
}

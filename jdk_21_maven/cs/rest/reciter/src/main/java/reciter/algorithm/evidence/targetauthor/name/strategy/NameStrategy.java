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
package reciter.algorithm.evidence.targetauthor.name.strategy;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import reciter.algorithm.evidence.targetauthor.AbstractTargetAuthorStrategy;
import reciter.engine.Feature;
import reciter.model.article.ReCiterArticle;
import reciter.model.article.ReCiterArticleAuthors;
import reciter.model.article.ReCiterAuthor;
import reciter.model.identity.Identity;

public class NameStrategy extends AbstractTargetAuthorStrategy {

	@Override
	public double executeStrategy(ReCiterArticle reCiterArticle, Identity identity) {
		boolean isMatchName = false;

		ReCiterArticleAuthors authors = reCiterArticle.getArticleCoAuthors();
		if (authors != null) {
			for (ReCiterAuthor author : authors.getAuthors()) {
				String firstName = author.getAuthorName().getFirstName();
				String middleInitial = author.getAuthorName().getMiddleInitial();
				String lastName = author.getAuthorName().getLastName();
				
				String targetAuthorFirstName = identity.getPrimaryName().getFirstName();
				String targetAuthorMiddleInitial = identity.getPrimaryName().getMiddleInitial();
				String targetAuthorLastName = identity.getPrimaryName().getLastName();
				
				if (StringUtils.equalsIgnoreCase(firstName, targetAuthorFirstName) &&
					StringUtils.equalsIgnoreCase(middleInitial, targetAuthorMiddleInitial) &&
					StringUtils.equalsIgnoreCase(lastName, targetAuthorLastName))
					
					isMatchName = true;
			}
		}

		if (isMatchName) {
			reCiterArticle.setNameStrategyScore(1);
			return 1;
		} else {
			reCiterArticle.setNameStrategyScore(0);
			return 0;
		}
	}

	@Override
	public double executeStrategy(List<ReCiterArticle> reCiterArticles, Identity identity) {
		double sum = 0;
		for (ReCiterArticle reCiterArticle : reCiterArticles) {
			sum += executeStrategy(reCiterArticle, identity);
		}
		return sum;
	}

	@Override
	public void populateFeature(ReCiterArticle reCiterArticle, Identity identity, Feature feature) {
		// TODO Auto-generated method stub
		
	}
}

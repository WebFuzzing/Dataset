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

import reciter.database.dynamodb.model.Gender;
import reciter.database.dynamodb.model.NameFrequency;
import reciter.database.dynamodb.model.ScienceMetrix;
import reciter.database.dynamodb.model.ScienceMetrixDepartmentCategory;
import reciter.model.article.ReCiterArticle;
import reciter.model.identity.Identity;
import reciter.model.pubmed.PubMedArticle;
import reciter.model.scopus.ScopusArticle;

import java.util.List;
import java.util.Map;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class EngineParameters {
	
	@Getter
	@Setter
    private static Map<String, Long> meshCountMap;
	@Getter
	@Setter
    private static Map<String, List<String>> afiliationNameToAfidMap;
	@Getter
	@Setter
    private static List<ScienceMetrix> scienceMetrixJournals;
	@Getter
	@Setter
    private static List<ScienceMetrixDepartmentCategory> scienceMetrixDepartmentCategories;
	@Getter
	@Setter
    private static List<Gender> genders;
	@Getter
	@Setter
    private static List<NameFrequency> nameFrequencies;
	@Getter
	@Setter
    private static Map<String, NameFrequency> nameFrequencyMap;
    private Identity identity;
    private List<PubMedArticle> pubMedArticles;
    private List<ScopusArticle> scopusArticles;
    private List<ReCiterArticle> reciterArticles;
    private List<Long> knownPmids;
    private List<Long> rejectedPmids;
    private double totalStandardzizedArticleScore;
    @Getter
	@Setter
    private static Map<Double, Double> degreeYearDiscrepancyScoreMap;
    @Getter
	@Setter
    private static String regexForStopWords;
}

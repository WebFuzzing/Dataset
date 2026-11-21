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
package reciter.xml.retriever.pubmed;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import reciter.model.identity.AuthorName;
import reciter.model.identity.Identity;
import reciter.model.scopus.ScopusArticle;
import reciter.xml.retriever.engine.AliasReCiterRetrievalEngine.IdentityNameType;
import reciter.xml.retriever.pubmed.AbstractRetrievalStrategy.RetrievalResult;

public interface RetrievalStrategy {
	
	/**
	 * Return the name of the retrieval strategy.
	 * 
	 * @return name of the retrieval strategy.
	 */
	String getRetrievalStrategyName();

	/**
	 * Retrieve Scopus articles based on list of pmids.
	 * 
	 * @param pmids List of pmids.
	 * 
	 * @return list of Scopus articles.
	 */
	List<ScopusArticle> retrieveScopus(Collection<Long> pmids);

	/**
	 * Retrieve the articles for this identity.
	 * 
	 * @param identity
	 * 
	 * @return Unique map of PMID to of PubMed articles for this identity.
	 */
	RetrievalResult retrievePubMedArticles(Identity identity, Map<IdentityNameType, Set<AuthorName>> identityNames, boolean useStrictQueryOnly) throws IOException;
	
	/**
	 * Retrieve the articles for this identity restricted by the start date and end date.
	 * 
	 * @param identity
	 * 
	 * @return Unique map of PMID to of PubMed articles for this identity.
	 */
	RetrievalResult retrievePubMedArticles(Identity identity, Map<IdentityNameType, Set<AuthorName>> identityNames, Date startDate, Date endDate, boolean useStrictQueryOnly) throws IOException;
	
	/**
	 * Retrieve the articles for list of pmids.
	 * 
	 * @param pmids
	 * 
	 * @return Unique map of PMID to of PubMed articles for this identity.
	 */
	RetrievalResult retrievePubMedArticles(List<Long> pmids) throws IOException;

	/**
	 * Retrieve Scopus articles based on list of doi strings.
	 * 
	 * @param dois
	 * @return
	 */
	List<ScopusArticle> retrieveScopusDoi(Collection<String> dois);
}

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
package reciter.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reciter.model.identity.AuthorName;
import reciter.model.identity.Identity;
import reciter.model.pubmed.MedlineCitationArticleAuthor;
import reciter.model.pubmed.PubMedArticle;

public class AuthorNameUtils {

	private final static Logger slf4jLogger = LoggerFactory.getLogger(AuthorNameUtils.class);

	/**
	 * Check whether two author names match on first name, middle name and
	 * last name.
	 * 
	 * @param name
	 * @param other
	 * 
	 * @return {@code true} if the two author names match on first name, middle
	 * name and last name.
	 */
	public static boolean isFullNameMatch(AuthorName name, AuthorName other) {

		return StringUtils.equalsIgnoreCase(name.getFirstName(), other.getFirstName()) &&
				StringUtils.equalsIgnoreCase(name.getMiddleName(), other.getMiddleName()) &&
				StringUtils.equalsIgnoreCase(name.getLastName(), other.getLastName());
	}
	
	public static boolean isFirstNameInitialMatch(AuthorName name, AuthorName other) {
		if (name != null && other != null) {
			return StringUtils.equalsIgnoreCase(name.getFirstInitial(), other.getFirstInitial());
		}
		return false;
	}
	
	public static boolean isFirstNameMatch(AuthorName name, AuthorName other) {
		return StringUtils.equalsIgnoreCase(name.getFirstName(), other.getFirstName());
	}
	
	public static boolean isMiddleNameMatch(AuthorName name, AuthorName other) {
		return StringUtils.equalsIgnoreCase(name.getMiddleName(), name.getMiddleName());
	}
	
	public static boolean isLastNameMatch(AuthorName name, AuthorName other) {
		return StringUtils.equalsIgnoreCase(name.getLastName(), other.getLastName());
	}
	
	public static boolean isLastNameAndFirstInitialMatch(AuthorName name, AuthorName other) {
		return isLastNameMatch(name, other) && isFirstNameInitialMatch(name, other);
	}
	
	// How to determine if an article contains an alternate name of the target author?
	
	// 1. If the article already contains target author's name, then the other authors with the same last name
	// as the target author is not likely to be the same as the target author.
	
	// 2. If the article contains an author's whose first name initial is the same as that of the 
	// target author's first name initial, and another name in the article that has the same last name 
	// as the target author, then that name is not likely an alternate name for the target author.
	
	// 3. If the article contains only one author with the same last name as the target author and the first names
	// of the target author and the aforementioned author do not match, then that author's first name is likely
	// an alternate name of the target author.
	public static Map<Long, List<AuthorName>> extractAlternateNamesFromPubMedArticlesRetrievedByEmail(
			List<PubMedArticle> pubMedArticles, Identity targetAuthor) {
		Map<Long, List<AuthorName>> map = new HashMap<Long, List<AuthorName>>(); // PMID to alternate author name.
		for (PubMedArticle pubMedArticle : pubMedArticles) {
			List<MedlineCitationArticleAuthor> authorList = pubMedArticle.getMedlinecitation().getArticle().getAuthorlist();
			
			boolean foundAuthorWithFullNameMatch = false;
			List<AuthorName> likelyAlternativeNameList = new ArrayList<AuthorName>();
			if (authorList != null) {
				for (MedlineCitationArticleAuthor medlineCitationArticleAuthor : authorList) {
					AuthorName authorName = PubMedConverter.extractAuthorName(medlineCitationArticleAuthor);
					if (authorName != null) {
						boolean isFullNameMatch = AuthorNameUtils.isFullNameMatch(authorName, targetAuthor.getPrimaryName());
						if (isFullNameMatch) {
							foundAuthorWithFullNameMatch = true;
							break; // Go to the next article.
						} else {
							boolean isLastNameAndFirstInitialMatch = AuthorNameUtils.isLastNameAndFirstInitialMatch
									(authorName, targetAuthor.getPrimaryName());
							if (isLastNameAndFirstInitialMatch) {
								likelyAlternativeNameList.add(authorName);
							}
						}
					}
				}
				if (!foundAuthorWithFullNameMatch && !likelyAlternativeNameList.isEmpty()) {
					map.put(pubMedArticle.getMedlinecitation().getMedlinecitationpmid().getPmid(), likelyAlternativeNameList);
				}
			}
		}
		return map;
	}
	
	public static Map<Long, AuthorName> calculatePotentialAlias(Identity identity, Collection<PubMedArticle> emailPubMedArticles) {
		Map<Long, AuthorName> aliasSet = new HashMap<Long, AuthorName>();
		for (PubMedArticle pubMedArticle : emailPubMedArticles) {
			for (MedlineCitationArticleAuthor author : pubMedArticle.getMedlinecitation().getArticle().getAuthorlist()) {
				String affiliation = author.getAffiliation();
				if (affiliation != null) {
					for (String email : identity.getEmails()) {
						if (affiliation.contains(email)) {
							// possibility of an alias:
							if (author.getLastname().equals(identity.getPrimaryName().getLastName())) {
								// sanity check: last name matches
								AuthorName alias = PubMedConverter.extractAuthorName(author);
								if (!alias.getFirstInitial().equals(identity.getPrimaryName().getFirstInitial())) {
									// check if the same first initial is already added to the set.
									if (aliasSet.isEmpty()) {
										aliasSet.put(pubMedArticle.getMedlinecitation().getMedlinecitationpmid().getPmid(), alias);
										slf4jLogger.info(identity.getUid() + ": " + identity.getPrimaryName() + ": (Empty set) Adding alias: " + alias);
									} else {
										for (AuthorName aliasAuthorName : aliasSet.values()) {
											if (!aliasAuthorName.getFirstInitial().equals(alias.getFirstInitial())) {
												aliasSet.put(pubMedArticle.getMedlinecitation().getMedlinecitationpmid().getPmid(), alias);
												slf4jLogger.info(identity.getUid() + ": " + identity.getPrimaryName() + ": (Different first initial) Adding alias: " + alias);
												break;
											} else {
												String firstNameInSet = aliasAuthorName.getFirstName();
												String currentFirstName = alias.getFirstName();
												// prefer the name with the longer first name: i.e., prefer 'Clay' over 'C.'
												// so remove the 'C.' and add the 'Clay'
												if (firstNameInSet.length() < currentFirstName.length()) {
													aliasSet.remove(pubMedArticle.getMedlinecitation().getMedlinecitationpmid().getPmid());
													aliasSet.put(pubMedArticle.getMedlinecitation().getMedlinecitationpmid().getPmid(), alias);
													slf4jLogger.info(identity.getUid() + ": " + identity.getPrimaryName() + ": (Prefer longer first name) Adding alias: " + alias);
													break;
												}
											}
										}
									}
								}
							}
							break;
						}
					}
				}
			}
		}
		return aliasSet;
	}
}

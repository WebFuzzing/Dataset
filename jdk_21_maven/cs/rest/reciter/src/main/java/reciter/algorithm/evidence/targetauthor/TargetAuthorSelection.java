package reciter.algorithm.evidence.targetauthor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reciter.utils.ReCiterStringUtil;
import reciter.model.article.ReCiterArticle;
import reciter.model.article.ReCiterArticleAuthors;
import reciter.model.article.ReCiterAuthor;
import reciter.model.identity.AuthorName;
import reciter.model.identity.Identity;

/**
 * @author szd2013
 * <p>This class identifies target author for a ReCiterArticle based on names in Idenity table<p>
 *
 */
public class TargetAuthorSelection {
	
	private static final Logger slf4jLogger = LoggerFactory.getLogger(TargetAuthorSelection.class);
	
	/**
	 * This function identifies target author for articles.
	 * @see <a href="https://github.com/wcmc-its/ReCiter/issues/185">Details</a>
	 * @param reciterArticles
	 * @param identity
	 */
	public void identifyTargetAuthor(List<ReCiterArticle> reciterArticles, Identity identity) {
		
		for(ReCiterArticle reciterArticle: reciterArticles) {
			//Setting this for debug purposes
			if(reciterArticle.getArticleId() == 15590364) {
				slf4jLogger.debug("here");
			}
			ReCiterArticleAuthors authors = reciterArticle.getArticleCoAuthors();
			Set<Entry<ReCiterAuthor, ReCiterAuthor>> sanitizedAritcleAuthors = authors.getSanitizedAuthorMap().entrySet();
			List<AuthorName> sanitizedIdentityAuthors = new ArrayList<AuthorName>(identity.getSanitizedNames().values());
			
			Set<Entry<ReCiterAuthor, ReCiterAuthor>> multipleMarkedTargetAuthor = new HashSet<Entry<ReCiterAuthor, ReCiterAuthor>>();
			if (authors != null && !sanitizedAritcleAuthors.isEmpty()) {
				
				int lastMiddleFirstMatchCount = 0;
				int lastNameMiddleInitialFirstMatchCount = 0;
				int lastNameFirstNameMatchCount = 0;
				int lastNameFirstNameSubstringIdentityMatchCount = 0;
				int lastNameFirstNameIdentitySubstringMatchCount = 0;
				int lastNameFirstInitialMatchCount = 0;
				int emailMatchcount = 0;
				int middleToFirstInitialAndFirstInitialToMiddleMatchCount = 0;
				int lastNamePartialFirstInitialMatchCount = 0;
				int lastNameMatchCount = 0;
				int firstNameMatchCount = 0;
				int fullLastNameToIdentityPartialMatchCount = 0;
	            
	            lastMiddleFirstMatchCount = checkExactLastMiddleFirstNameMatch(sanitizedAritcleAuthors, sanitizedIdentityAuthors, emailMatchcount, multipleMarkedTargetAuthor);
	            if(lastMiddleFirstMatchCount == 0 || lastMiddleFirstMatchCount > 1)
	            	lastNameMiddleInitialFirstMatchCount = checkExactLastMiddleInitialFirstNameMatch(sanitizedAritcleAuthors, sanitizedIdentityAuthors, lastMiddleFirstMatchCount, multipleMarkedTargetAuthor);
	            if(lastMiddleFirstMatchCount ==1) {
	            	slf4jLogger.debug("Exact Last Name, Middle Name and First Name Match found for article: " + reciterArticle.getArticleId());
	            	continue;
	            }
	            
	            if(lastNameMiddleInitialFirstMatchCount == 0 || lastNameMiddleInitialFirstMatchCount > 1)
	            	lastNameFirstNameMatchCount = checkExactLastFirstNameMatch(sanitizedAritcleAuthors, sanitizedIdentityAuthors, lastNameMiddleInitialFirstMatchCount, multipleMarkedTargetAuthor);
	            if(lastNameMiddleInitialFirstMatchCount == 1) {
	            	slf4jLogger.debug("Last Name Middle Initial and First Name Match found for article: " + reciterArticle.getArticleId());
	            	continue;
	            }
	            
	            if(lastNameFirstNameMatchCount == 0 || lastNameFirstNameMatchCount > 1)
	            	lastNameFirstNameSubstringIdentityMatchCount = checkExactLastFirstNamePartialSubstringIdentityMatch(sanitizedAritcleAuthors, sanitizedIdentityAuthors, lastNameFirstNameMatchCount, multipleMarkedTargetAuthor);
	            if(lastNameFirstNameMatchCount == 1) {
	            	slf4jLogger.debug("Last Name First Name exact Match found for article: " + reciterArticle.getArticleId());
	            	continue;
	            }
	            
	            if(lastNameFirstNameSubstringIdentityMatchCount == 0 || lastNameFirstNameSubstringIdentityMatchCount > 1)
	            	lastNameFirstNameIdentitySubstringMatchCount = checkExactLastFirstNamePartialIdentityPartialSubstringMatch(authors.getSanitizedAuthorMap().entrySet(), sanitizedIdentityAuthors, lastNameMiddleInitialFirstMatchCount, multipleMarkedTargetAuthor);
	            if(lastNameFirstNameSubstringIdentityMatchCount == 1) {
	            	slf4jLogger.debug("Last Name First Name partial match of Identity Match found for article: " + reciterArticle.getArticleId());
	            	continue;
	            }
	            
	            if(lastNameFirstNameIdentitySubstringMatchCount == 0 || lastNameFirstNameIdentitySubstringMatchCount > 1)
	            	lastNameFirstInitialMatchCount = checkExactLastFirstInitialNameMatch(sanitizedAritcleAuthors, sanitizedIdentityAuthors, lastNameMiddleInitialFirstMatchCount, multipleMarkedTargetAuthor);
	            if(lastNameFirstNameIdentitySubstringMatchCount == 1) {
	            	slf4jLogger.debug("Last Name Identity First name partial of Article Match found for article: " + reciterArticle.getArticleId());
	            	continue;
	            }
	            
	            if(lastNameFirstInitialMatchCount == 0 || lastNameFirstInitialMatchCount > 1)
	            	emailMatchcount = checkEmailMatch(sanitizedAritcleAuthors, identity, emailMatchcount, multipleMarkedTargetAuthor);
	            if(lastNameFirstInitialMatchCount == 1) {	
	            	slf4jLogger.debug("Last Name First Initial exact Match found for article: " + reciterArticle.getArticleId());
	            	continue;
	            }
	            
	            if(emailMatchcount == 0 || emailMatchcount > 1)
	            	middleToFirstInitialAndFirstInitialToMiddleMatchCount = checkFirstInitialTomiddleInitialAndmiddleInitialToFirstInitialMatch(sanitizedAritcleAuthors, sanitizedIdentityAuthors, lastNameFirstInitialMatchCount, multipleMarkedTargetAuthor);
	            if(emailMatchcount == 1) {
	            	slf4jLogger.debug("Email Match found for article: " + reciterArticle.getArticleId());
	            	continue;
	            }
	            
	            if(middleToFirstInitialAndFirstInitialToMiddleMatchCount == 0 || middleToFirstInitialAndFirstInitialToMiddleMatchCount > 1)
	            	lastNamePartialFirstInitialMatchCount = checkPartialLastNameFirstInitialMatch(sanitizedAritcleAuthors, sanitizedIdentityAuthors, middleToFirstInitialAndFirstInitialToMiddleMatchCount, multipleMarkedTargetAuthor);
	            if(middleToFirstInitialAndFirstInitialToMiddleMatchCount == 1) {
	            	slf4jLogger.debug("Middle intial to first initial and first intial to middle initial Match found for article: " + reciterArticle.getArticleId());
	            	continue;
	            }
	            
	            if(lastNamePartialFirstInitialMatchCount == 0 || lastNamePartialFirstInitialMatchCount > 1)
	            	lastNameMatchCount = checkLastNameExactMatch(sanitizedAritcleAuthors, sanitizedIdentityAuthors, lastNamePartialFirstInitialMatchCount, multipleMarkedTargetAuthor);
	            if(lastNamePartialFirstInitialMatchCount == 1) {
	            	slf4jLogger.debug("Last Name Partial First Initial Match found for article: " + reciterArticle.getArticleId());
	            	continue;
	            }
	            
	            if(lastNameMatchCount == 0 || lastNameMatchCount > 1)
	            	firstNameMatchCount = checkFirstNameExactMatch(sanitizedAritcleAuthors, sanitizedIdentityAuthors, lastNameMatchCount, multipleMarkedTargetAuthor);
	            if(lastNameMatchCount == 1) {
	            	slf4jLogger.debug("Exact First name match found for article: " + reciterArticle.getArticleId());
	            	continue;
	            }
	            
	            if(firstNameMatchCount == 0 || firstNameMatchCount > 1)
	            	fullLastNameToIdentityPartialMatchCount = checkLastNameFullArticleToIdentityPartialMatch(sanitizedAritcleAuthors, sanitizedIdentityAuthors, firstNameMatchCount, multipleMarkedTargetAuthor);
	            if(firstNameMatchCount == 1) {
	            	slf4jLogger.debug("Exact First name match found for article: " + reciterArticle.getArticleId());
	            	continue;
	            }
	            
	            if(fullLastNameToIdentityPartialMatchCount == 1) {
	            	slf4jLogger.debug("Full Last Name match to partial Identity Last Name: " + reciterArticle.getArticleId());
	            	continue;
	            }

	            //Step 13: Attempt strict first name and first initial of last name match
	            int firstNameLastInitialMatchCount = 0;
	            if(fullLastNameToIdentityPartialMatchCount == 0 || fullLastNameToIdentityPartialMatchCount > 1)
	            	firstNameLastInitialMatchCount = checkFirstNameAndLastInitialMatch(sanitizedAritcleAuthors, sanitizedIdentityAuthors, fullLastNameToIdentityPartialMatchCount, multipleMarkedTargetAuthor);
	            if(firstNameLastInitialMatchCount == 1) {
	            	slf4jLogger.debug("First Name and Last Initial Match found for article: " + reciterArticle.getArticleId());
	            	continue;
	            }

	            //Step 14: Attempt identity middle name to article last name match
	            int middleNameToLastNameMatchCount = 0;
	            if(firstNameLastInitialMatchCount == 0 || firstNameLastInitialMatchCount > 1)
	            	middleNameToLastNameMatchCount = checkMiddleNameToLastNameMatch(sanitizedAritcleAuthors, sanitizedIdentityAuthors, firstNameLastInitialMatchCount, multipleMarkedTargetAuthor);
	            if(middleNameToLastNameMatchCount == 1) {
	            	slf4jLogger.debug("Identity Middle Name to Article Last Name Match found for article: " + reciterArticle.getArticleId());
	            	continue;
	            }

	            //Step 15: Attempt first-to-last and last-to-first name swap match
	            int firstLastSwapMatchCount = 0;
	            if(middleNameToLastNameMatchCount == 0 || middleNameToLastNameMatchCount > 1)
	            	firstLastSwapMatchCount = checkFirstLastNameSwapMatch(sanitizedAritcleAuthors, sanitizedIdentityAuthors, middleNameToLastNameMatchCount, multipleMarkedTargetAuthor);
	            if(firstLastSwapMatchCount == 1) {
	            	slf4jLogger.debug("First-Last Name Swap Match found for article: " + reciterArticle.getArticleId());
	            	continue;
	            }


	            //Step 16: Attempt identity last name to article first name + identity first initial to article last initial

	            int lastToFirstAndInitialMatchCount = 0;
	            if(firstLastSwapMatchCount == 0 || firstLastSwapMatchCount > 1)
	            	lastToFirstAndInitialMatchCount = checkLastNameToFirstNameAndFirstInitialToLastInitialMatch(sanitizedAritcleAuthors, sanitizedIdentityAuthors, firstLastSwapMatchCount, multipleMarkedTargetAuthor);
	            if(lastToFirstAndInitialMatchCount == 1) {
	            	slf4jLogger.debug("Identity Last-to-Article First + Initial Match found for article: " + reciterArticle.getArticleId());
	            	continue;
	            }

	            //Step 17: Attempt both initials match (first initial to first initial, last initial to last initial)

	            int bothInitialsMatchCount = 0;
	            if(lastToFirstAndInitialMatchCount == 0 || lastToFirstAndInitialMatchCount > 1)
	            	bothInitialsMatchCount = checkBothInitialsMatch(sanitizedAritcleAuthors, sanitizedIdentityAuthors, lastToFirstAndInitialMatchCount, multipleMarkedTargetAuthor);
	            if(bothInitialsMatchCount == 1) {
	            	slf4jLogger.debug("Both Initials Match found for article: " + reciterArticle.getArticleId());
	            	continue;
	            }

	            //Step 18: Attempt fuzzy last name match (Levenshtein distance <= 2) with first name or first initial match

	            int fuzzyLastNameMatchCount = 0;
	            if(bothInitialsMatchCount == 0 || bothInitialsMatchCount > 1)
	            	fuzzyLastNameMatchCount = checkFuzzyLastNameMatch(sanitizedAritcleAuthors, sanitizedIdentityAuthors, bothInitialsMatchCount, multipleMarkedTargetAuthor);
	            if(fuzzyLastNameMatchCount == 1) {
	            	slf4jLogger.debug("Fuzzy Last Name Match (Levenshtein <= 2) found for article: " + reciterArticle.getArticleId());
	            	continue;
	            }

	            if(fuzzyLastNameMatchCount == 0) {
	            	slf4jLogger.info("There was no target author found for " + reciterArticle.getArticleId());
	            	assignTargetAuthorFalse(authors.getAuthors());
	            }
	            else if(fuzzyLastNameMatchCount > 1) {
	            	slf4jLogger.debug(fuzzyLastNameMatchCount + " authors were marked as target author for article " + reciterArticle.getArticleId() + ". Attempting disambiguation...");
	            	int disambiguatedCount = disambiguateMultipleMatches(multipleMarkedTargetAuthor, sanitizedIdentityAuthors);
	            	if(disambiguatedCount == 1) {
	            		slf4jLogger.debug("Disambiguation resolved to 1 target author for article: " + reciterArticle.getArticleId());
	            	} else {
	            		slf4jLogger.info("Disambiguation could not resolve: " + disambiguatedCount + " authors remain for article " + reciterArticle.getArticleId());
	            	}
	            }
            	
            		
	        }
		}
		
	}
	
	//Step 7 : attempt email match if match then automatically its a target author
	/**
	 * Check for email match from affiliation statement with Identity email
	 * @param authors
	 * @param identity
	 * @param matchCount
	 * @param multipleMarkedTargetAuthor
	 * @return
	 */
	public int checkEmailMatch(Set<Entry<ReCiterAuthor, ReCiterAuthor>> authors, Identity identity, int matchCount, Set<Entry<ReCiterAuthor, ReCiterAuthor>> multipleMarkedTargetAuthor) {
		String affiliation = null;
		for (Entry<ReCiterAuthor, ReCiterAuthor> entry : authors) {
			ReCiterAuthor author = entry.getValue();
			ReCiterAuthor originalAuthor = entry.getKey();
			if (originalAuthor.getAffiliation() != null && !originalAuthor.getAffiliation().isBlank()) {
				affiliation = originalAuthor.getAffiliation();
				if(identity!=null && identity.getEmails()!=null && !identity.getEmails().isEmpty() && affiliation != null) {
					for(String email: identity.getEmails()) {
						if(affiliation.contains(email)) {
							author.setTargetAuthor(true);
							originalAuthor.setTargetAuthor(true);
							matchCount++;
							multipleMarkedTargetAuthor.add(entry);
						}
					}
				}
			}
			else {
				author.setTargetAuthor(false);
				originalAuthor.setTargetAuthor(false);
			}
		}
		return matchCount;
	}
	
	//Step 1 : Attempt strict last name, strict middle name, and strict first name match
	/**
	 * Check for exact last name, middle and last name match from article to identity
	 * @param authors
	 * @param sanitizedIdentityAuthors
	 * @param matchCount
	 * @param multipleMarkedTargetAuthor
	 * @return
	 */
	public int checkExactLastMiddleFirstNameMatch(Set<Entry<ReCiterAuthor, ReCiterAuthor>> authors, List<AuthorName> sanitizedIdentityAuthors, int matchCount, Set<Entry<ReCiterAuthor, ReCiterAuthor>> multipleMarkedTargetAuthor) {
		int count = 0;
		if(matchCount > 1) {
			authors = new HashSet<Entry<ReCiterAuthor, ReCiterAuthor>>(multipleMarkedTargetAuthor);
		}
		for (Entry<ReCiterAuthor, ReCiterAuthor> entry : authors) {
			ReCiterAuthor author = entry.getValue();
			ReCiterAuthor originalAuthor = entry.getKey();
			if(author.getAuthorName().getFirstName() != null && author.getAuthorName().getLastName() != null && author.getAuthorName().getMiddleName() != null) {
				if(matchCount > 1 && author.isTargetAuthor()) {
					if(sanitizedIdentityAuthors.stream().anyMatch(sanitizedIdentityName -> sanitizedIdentityName.getFirstName() != null
							&&
							sanitizedIdentityName.getMiddleName() != null
							&& 
							sanitizedIdentityName.getLastName() != null
							&& 
							author.getAuthorName().getLastName().equalsIgnoreCase(sanitizedIdentityName.getLastName())
							&& 
							author.getAuthorName().getMiddleName().equalsIgnoreCase(sanitizedIdentityName.getMiddleName())
							&& 
							author.getAuthorName().getFirstName().equalsIgnoreCase(sanitizedIdentityName.getFirstName()))) {
						author.setTargetAuthor(true);
						originalAuthor.setTargetAuthor(true);
						multipleMarkedTargetAuthor.add(entry);
						count++;
					}
					else {
						author.setTargetAuthor(false);
						originalAuthor.setTargetAuthor(false);
					}
					
					
				}
				else if(matchCount == 0) {
					if(sanitizedIdentityAuthors.stream().anyMatch(sanitizedIdentityName -> sanitizedIdentityName.getFirstName() != null
							&&
							sanitizedIdentityName.getMiddleName() != null
							&& 
							sanitizedIdentityName.getLastName() != null
							&& 
							author.getAuthorName().getLastName().equalsIgnoreCase(sanitizedIdentityName.getLastName())
							&& 
							author.getAuthorName().getMiddleName().equalsIgnoreCase(sanitizedIdentityName.getMiddleName())
							&& 
							author.getAuthorName().getFirstName().equalsIgnoreCase(sanitizedIdentityName.getFirstName()))) {
						author.setTargetAuthor(true);
						originalAuthor.setTargetAuthor(true);
						multipleMarkedTargetAuthor.add(entry);
						count++;
					}
					else {
						author.setTargetAuthor(false);
						originalAuthor.setTargetAuthor(false);
					}
				}
			}
		}
		if(matchCount > 1 && count == 0) {
			return matchCount;
		}
		return count;
	}
	
	//Step 2 : Attempt strict last name, middle initial, and strict first name match
	/**
	 * Check for exact last name, middle initial and first name match from article to identity
	 * @param authors
	 * @param sanitizedIdentityAuthors
	 * @param matchCount
	 * @param multipleMarkedTargetAuthor
	 * @return
	 */
	public int checkExactLastMiddleInitialFirstNameMatch(Set<Entry<ReCiterAuthor, ReCiterAuthor>> authors, List<AuthorName> sanitizedIdentityAuthors, int matchCount, Set<Entry<ReCiterAuthor, ReCiterAuthor>> multipleMarkedTargetAuthor) {
		int count = 0;
		if(matchCount > 1) {
			authors = new HashSet<Entry<ReCiterAuthor, ReCiterAuthor>>(multipleMarkedTargetAuthor);
		}
		for (Entry<ReCiterAuthor, ReCiterAuthor> entry : authors) {
			ReCiterAuthor author = entry.getValue();
			ReCiterAuthor originalAuthor = entry.getKey();
			if(author.getAuthorName().getFirstName() != null && author.getAuthorName().getLastName() != null && author.getAuthorName().getMiddleInitial() != null) {
				if(matchCount > 1 && author.isTargetAuthor()) {
					if(sanitizedIdentityAuthors.stream().anyMatch(sanitizedIdentityName -> sanitizedIdentityName.getFirstName() != null
							&&
							sanitizedIdentityName.getMiddleInitial() != null
							&& 
							sanitizedIdentityName.getLastName() != null
							&& 
							author.getAuthorName().getLastName().equalsIgnoreCase(sanitizedIdentityName.getLastName())
							&& 
							author.getAuthorName().getMiddleInitial().equalsIgnoreCase(sanitizedIdentityName.getMiddleInitial())
							&& 
							author.getAuthorName().getFirstName().equalsIgnoreCase(sanitizedIdentityName.getFirstName()))) {
						author.setTargetAuthor(true);
						originalAuthor.setTargetAuthor(true);
						multipleMarkedTargetAuthor.add(entry);
						count++;
					}
					else {
						author.setTargetAuthor(false);
						originalAuthor.setTargetAuthor(false);
					}
				}
				else if(matchCount == 0) {
					if(sanitizedIdentityAuthors.stream().anyMatch(sanitizedIdentityName -> sanitizedIdentityName.getFirstName() != null
							&&
							sanitizedIdentityName.getMiddleInitial() != null
							&& 
							sanitizedIdentityName.getLastName() != null
							&& 
							author.getAuthorName().getLastName().equalsIgnoreCase(sanitizedIdentityName.getLastName())
							&& 
							author.getAuthorName().getMiddleInitial().equalsIgnoreCase(sanitizedIdentityName.getMiddleInitial())
							&& 
							author.getAuthorName().getFirstName().equalsIgnoreCase(sanitizedIdentityName.getFirstName()))) {
						author.setTargetAuthor(true);
						originalAuthor.setTargetAuthor(true);
						multipleMarkedTargetAuthor.add(entry);
						count++;
					}
					else {
						author.setTargetAuthor(false);
						originalAuthor.setTargetAuthor(false);
					}
				}
			}
		}
		if(matchCount > 1 && count == 0) {
			return matchCount;
		}
		return count;
	}
		
	/**
	 * Check for exact last and first name match from article to identity
	 * @param authors
	 * @param sanitizedIdentityAuthors
	 * @param matchCount
	 * @param multipleMarkedTargetAuthor
	 * @return
	 */
	//Step 3 : Attempt strict last name and strict first name match
	public int checkExactLastFirstNameMatch(Set<Entry<ReCiterAuthor, ReCiterAuthor>> authors, List<AuthorName> sanitizedIdentityAuthors, int matchCount, Set<Entry<ReCiterAuthor, ReCiterAuthor>> multipleMarkedTargetAuthor) {
		int count = 0;
		if(matchCount > 1) {
			authors = new HashSet<Entry<ReCiterAuthor, ReCiterAuthor>>(multipleMarkedTargetAuthor);
		}
		for (Entry<ReCiterAuthor, ReCiterAuthor> entry : authors) {
			ReCiterAuthor author = entry.getValue();
			ReCiterAuthor originalAuthor = entry.getKey();
			if(author.getAuthorName().getFirstName() != null && author.getAuthorName().getLastName() != null) {
				if(matchCount > 1 && author.isTargetAuthor()) {
					if(sanitizedIdentityAuthors.stream().anyMatch(sanitizedIdentityName -> sanitizedIdentityName.getFirstName() != null
							&&
							sanitizedIdentityName.getLastName() != null
							&& 
							author.getAuthorName().getLastName().equalsIgnoreCase(sanitizedIdentityName.getLastName())
							&& 
							author.getAuthorName().getFirstName().equalsIgnoreCase(sanitizedIdentityName.getFirstName()))) {
						author.setTargetAuthor(true);
						originalAuthor.setTargetAuthor(true);
						multipleMarkedTargetAuthor.add(entry);
						count++;
					}
					else {
						author.setTargetAuthor(false);
						originalAuthor.setTargetAuthor(false);
					}
				}
				else if(matchCount == 0) {
					if(sanitizedIdentityAuthors.stream().anyMatch(sanitizedIdentityName -> sanitizedIdentityName.getFirstName() != null
							&& 
							sanitizedIdentityName.getLastName() != null
							&& 
							author.getAuthorName().getLastName().equalsIgnoreCase(sanitizedIdentityName.getLastName())
							&& 
							author.getAuthorName().getFirstName().equalsIgnoreCase(sanitizedIdentityName.getFirstName()))) {
						author.setTargetAuthor(true);
						originalAuthor.setTargetAuthor(true);
						multipleMarkedTargetAuthor.add(entry);
						count++;
					}
					else {
						author.setTargetAuthor(false);
						originalAuthor.setTargetAuthor(false);
					}
				}
			}
		}
		if(matchCount > 1 && count == 0) {
			return matchCount;
		}
		return count;
	}
	
	//Step 4:Attempt strict last name and partial first name match, in which article is substring of identity
	/**
	 * Check for exact last name and partial first name where article first name is a substring of identity first name
	 * @param authors
	 * @param sanitizedIdentityAuthors
	 * @param matchCount
	 * @param multipleMarkedTargetAuthor
	 * @return
	 */
	public int checkExactLastFirstNamePartialSubstringIdentityMatch(Set<Entry<ReCiterAuthor, ReCiterAuthor>> authors, List<AuthorName> sanitizedIdentityAuthors, int matchCount, Set<Entry<ReCiterAuthor, ReCiterAuthor>> multipleMarkedTargetAuthor) {
		int count = 0;
		if(matchCount > 1) {
			authors = new HashSet<Entry<ReCiterAuthor, ReCiterAuthor>>(multipleMarkedTargetAuthor);
		}
		for (Entry<ReCiterAuthor, ReCiterAuthor> entry : authors) {
			ReCiterAuthor author = entry.getValue();
			ReCiterAuthor originalAuthor = entry.getKey();
			if(author.getAuthorName().getFirstName() != null && author.getAuthorName().getLastName() != null) {
				if(matchCount > 1 && author.isTargetAuthor()) {
					if(sanitizedIdentityAuthors.stream().anyMatch(sanitizedIdentityName -> sanitizedIdentityName.getFirstName() != null
							&& 
							sanitizedIdentityName.getLastName() != null
							&& 
							author.getAuthorName().getLastName().equalsIgnoreCase(sanitizedIdentityName.getLastName())
							&& 
							StringUtils.containsIgnoreCase(sanitizedIdentityName.getFirstName(), author.getAuthorName().getFirstName()))) {
						author.setTargetAuthor(true);
						originalAuthor.setTargetAuthor(true);
						multipleMarkedTargetAuthor.add(entry);
						count++;
					}
					else {
						author.setTargetAuthor(false);
						originalAuthor.setTargetAuthor(false);
					}
					
					
				}
				else if(matchCount == 0) {
					if(sanitizedIdentityAuthors.stream().anyMatch(sanitizedIdentityName -> sanitizedIdentityName.getFirstName() != null
							&& 
							sanitizedIdentityName.getLastName() != null
							&& 
							author.getAuthorName().getLastName().equalsIgnoreCase(sanitizedIdentityName.getLastName())
							&& 
							StringUtils.containsIgnoreCase(sanitizedIdentityName.getFirstName(), author.getAuthorName().getFirstName()))) {
						author.setTargetAuthor(true);
						originalAuthor.setTargetAuthor(true);
						multipleMarkedTargetAuthor.add(entry);
						count++;
					}
					else {
						author.setTargetAuthor(false);
						originalAuthor.setTargetAuthor(false);
					}
				}
			}
		}
		if(matchCount > 1 && count == 0) {
			return matchCount;
		}
		return count;
	}
	
	//Step 5:Attempt strict last name and partial first name match, in which identity is substring of article.
	/**
	 * Check for exact last name and partial first name where identity first name is a substring of article first name
	 * @param authors
	 * @param sanitizedIdentityAuthors
	 * @param matchCount
	 * @param multipleMarkedTargetAuthor
	 * @return
	 */
	public int checkExactLastFirstNamePartialIdentityPartialSubstringMatch(Set<Entry<ReCiterAuthor, ReCiterAuthor>> authors, List<AuthorName> sanitizedIdentityAuthors, int matchCount, Set<Entry<ReCiterAuthor, ReCiterAuthor>> multipleMarkedTargetAuthor) {
		int count = 0;
		if(matchCount > 1) {
			authors = new HashSet<Entry<ReCiterAuthor, ReCiterAuthor>>(multipleMarkedTargetAuthor);
		}
		for (Entry<ReCiterAuthor, ReCiterAuthor> entry : authors) {
			ReCiterAuthor author = entry.getValue();
			ReCiterAuthor originalAuthor = entry.getKey();
			if(author.getAuthorName().getFirstName() != null && author.getAuthorName().getLastName() != null) {
				if(matchCount > 1 && author.isTargetAuthor()) {
					if(sanitizedIdentityAuthors.stream().anyMatch(sanitizedIdentityName -> sanitizedIdentityName.getFirstName() != null
							&& 
							sanitizedIdentityName.getLastName() != null
							&& 
							author.getAuthorName().getLastName().equalsIgnoreCase(sanitizedIdentityName.getLastName())
							&& 
							StringUtils.containsIgnoreCase(author.getAuthorName().getFirstName(), sanitizedIdentityName.getFirstName()))) {
						author.setTargetAuthor(true);
						originalAuthor.setTargetAuthor(true);
						multipleMarkedTargetAuthor.add(entry);
						count++;
					}
					else {
						author.setTargetAuthor(false);
						originalAuthor.setTargetAuthor(false);
					}	
				}
				else if(matchCount == 0) {
					if(sanitizedIdentityAuthors.stream().anyMatch(sanitizedIdentityName -> sanitizedIdentityName.getFirstName() != null
							&& 
							sanitizedIdentityName.getLastName() != null
							&& 
							author.getAuthorName().getLastName().equalsIgnoreCase(sanitizedIdentityName.getLastName())
							&& 
							StringUtils.containsIgnoreCase(author.getAuthorName().getFirstName(), sanitizedIdentityName.getFirstName()))) {
						author.setTargetAuthor(true);
						originalAuthor.setTargetAuthor(true);
						multipleMarkedTargetAuthor.add(entry);
						count++;
					}
					else {
						author.setTargetAuthor(false);
						originalAuthor.setTargetAuthor(false);
					}
				}
			}
		}
		if(matchCount > 1 && count == 0) {
			return matchCount;
		}
		return count;
	}
	
	/**
	 * Check for exact last and first initial match from article to identity
	 * @param authors
	 * @param sanitizedIdentityAuthors
	 * @param matchCount
	 * @param multipleMarkedTargetAuthor
	 * @return
	 */
	//Step 6 : Attempt strict last name and first initial match
	public int checkExactLastFirstInitialNameMatch(Set<Entry<ReCiterAuthor, ReCiterAuthor>> authors, List<AuthorName> sanitizedIdentityAuthors, int matchCount, Set<Entry<ReCiterAuthor, ReCiterAuthor>> multipleMarkedTargetAuthor) {
		int count = 0;
		if(matchCount > 1) {
			authors = new HashSet<Entry<ReCiterAuthor, ReCiterAuthor>>(multipleMarkedTargetAuthor);
		}
		for (Entry<ReCiterAuthor, ReCiterAuthor> entry : authors) {
			ReCiterAuthor author = entry.getValue();
			ReCiterAuthor originalAuthor = entry.getKey();
			if(author.getAuthorName().getFirstInitial() != null && author.getAuthorName().getLastName() != null) {
				if(matchCount > 1 && author.isTargetAuthor()) {
					if(sanitizedIdentityAuthors.stream().anyMatch(sanitizedIdentityName -> sanitizedIdentityName.getFirstInitial() != null
							&&
							sanitizedIdentityName.getLastName() != null
							&& 
							author.getAuthorName().getLastName().equalsIgnoreCase(sanitizedIdentityName.getLastName())
							&& 
							author.getAuthorName().getFirstInitial().equalsIgnoreCase(sanitizedIdentityName.getFirstInitial()))) {
						author.setTargetAuthor(true);
						originalAuthor.setTargetAuthor(true);
						multipleMarkedTargetAuthor.add(entry);
						count++;
					}
					else {
						author.setTargetAuthor(false);
						originalAuthor.setTargetAuthor(false);
					}	
				}
				else if(matchCount == 0) {
					if(sanitizedIdentityAuthors.stream().anyMatch(sanitizedIdentityName -> sanitizedIdentityName.getFirstInitial() != null
							&&
							sanitizedIdentityName.getLastName() != null
							&& 
							author.getAuthorName().getLastName().equalsIgnoreCase(sanitizedIdentityName.getLastName())
							&& 
							author.getAuthorName().getFirstInitial().equalsIgnoreCase(sanitizedIdentityName.getFirstInitial()))) {
						author.setTargetAuthor(true);
						originalAuthor.setTargetAuthor(true);
						multipleMarkedTargetAuthor.add(entry);
						count++;
					}
					else {
						author.setTargetAuthor(false);
						originalAuthor.setTargetAuthor(false);
					}
				}
			}
		}
		if(matchCount > 1 && count == 0) {
			return matchCount;
		}
		return count;
	}
	
	/**
	 * Check for article first initial to middle initial and article middle initial to first initial and exact lastname match
	 * @param authors
	 * @param sanitizedIdentityAuthors
	 * @param matchCount
	 * @param multipleMarkedTargetAuthor
	 * @return
	 */
	//Step 8:Attempt firstInitial to middleInitial, and middleInitial to firstInitial match with strict lastname match
	public int checkFirstInitialTomiddleInitialAndmiddleInitialToFirstInitialMatch(Set<Entry<ReCiterAuthor, ReCiterAuthor>> authors, List<AuthorName> sanitizedIdentityAuthors, int matchCount, Set<Entry<ReCiterAuthor, ReCiterAuthor>> multipleMarkedTargetAuthor) {
		int count = 0;
		if(matchCount > 1) {
			authors = new HashSet<Entry<ReCiterAuthor, ReCiterAuthor>>(multipleMarkedTargetAuthor);
		}
		for (Entry<ReCiterAuthor, ReCiterAuthor> entry : authors) {
			ReCiterAuthor author = entry.getValue();
			ReCiterAuthor originalAuthor = entry.getKey();
			if(author.getAuthorName().getFirstInitial() != null && author.getAuthorName().getLastName() != null && author.getAuthorName().getMiddleInitial() != null) {
				if(matchCount > 1 && author.isTargetAuthor()) {
					if(author.getAuthorName().getFirstInitial() != null && author.getAuthorName().getFirstInitial().length() > 0 
							&& 
							author.getAuthorName().getMiddleInitial() != null && author.getAuthorName().getMiddleInitial().length() > 0 
							&& 
							sanitizedIdentityAuthors.stream().anyMatch(sanitizedIdentityName -> sanitizedIdentityName.getFirstInitial() != null && sanitizedIdentityName.getFirstInitial().length() > 0
							&&
							sanitizedIdentityName.getMiddleInitial() != null && sanitizedIdentityName.getMiddleInitial().length() > 0
							&& 
							(StringUtils.equalsIgnoreCase(author.getAuthorName().getFirstInitial().trim(), sanitizedIdentityName.getMiddleInitial().trim())) //FirstInitial to MiddleInitial
							&&
							(StringUtils.equalsIgnoreCase(author.getAuthorName().getMiddleInitial().trim(), sanitizedIdentityName.getFirstInitial().trim()))//MiddleInitial to First Initial
							&&
							(StringUtils.equalsIgnoreCase(author.getAuthorName().getLastName().trim(), sanitizedIdentityName.getLastName().trim()))//Exact lastname match
						    )) {
						author.setTargetAuthor(true);
						originalAuthor.setTargetAuthor(true);
						multipleMarkedTargetAuthor.add(entry);
						count++;
					}
					else {
						author.setTargetAuthor(false);
						originalAuthor.setTargetAuthor(false);
					}
				}
				else if(matchCount == 0) {
					if(author.getAuthorName().getFirstInitial() != null && author.getAuthorName().getFirstInitial().length() > 0 
							&& 
							author.getAuthorName().getMiddleInitial() != null && author.getAuthorName().getMiddleInitial().length() > 0 
							&& 
							sanitizedIdentityAuthors.stream().anyMatch(sanitizedIdentityName -> sanitizedIdentityName.getFirstInitial() != null && sanitizedIdentityName.getFirstInitial().length() > 0
							&&
							sanitizedIdentityName.getMiddleInitial() != null && sanitizedIdentityName.getMiddleInitial().length() > 0
							&& 
							(StringUtils.equalsIgnoreCase(author.getAuthorName().getFirstInitial().trim(), sanitizedIdentityName.getMiddleInitial().trim())) //FirstInitial to MiddleInitial
							&&
							(StringUtils.equalsIgnoreCase(author.getAuthorName().getMiddleInitial().trim(), sanitizedIdentityName.getFirstInitial().trim()))//MiddleInitial to First Initial
							&&
							(StringUtils.equalsIgnoreCase(author.getAuthorName().getLastName().trim(), sanitizedIdentityName.getLastName().trim()))//Exact lastname match
						    )) {
						author.setTargetAuthor(true);
						originalAuthor.setTargetAuthor(true);
						multipleMarkedTargetAuthor.add(entry);
						count++;
					}
					else {
						author.setTargetAuthor(false);
						originalAuthor.setTargetAuthor(false);
					}
				}
			}
		}
		if(matchCount > 1 && count == 0) {
			return matchCount;
		}
		return count;
	}
	
	//Step 9: Attempt partial last name and exact first initial match
	/**
	 * Check for partial last name with exact first initial match from article to identity
	 * @param authors
	 * @param sanitizedIdentityAuthors
	 * @param matchCount
	 * @param multipleMarkedTargetAuthor
	 * @return
	 */
	public int checkPartialLastNameFirstInitialMatch(Set<Entry<ReCiterAuthor, ReCiterAuthor>> authors, List<AuthorName> sanitizedIdentityAuthors, int matchCount, Set<Entry<ReCiterAuthor, ReCiterAuthor>> multipleMarkedTargetAuthor) {
        int count = 0;
        if(matchCount > 1) {
			authors = new HashSet<Entry<ReCiterAuthor, ReCiterAuthor>>(multipleMarkedTargetAuthor);
		}
		for (Entry<ReCiterAuthor, ReCiterAuthor> entry : authors) {
			ReCiterAuthor author = entry.getValue();
			ReCiterAuthor originalAuthor = entry.getKey();
			if(author.getAuthorName().getFirstInitial() != null && author.getAuthorName().getLastName() != null) {
				if(matchCount > 1 && author.isTargetAuthor()) {
					if(sanitizedIdentityAuthors.stream().anyMatch(sanitizedIdentityName -> sanitizedIdentityName.getFirstInitial() != null
							&& 
							sanitizedIdentityName.getLastName() != null
							&& 
							author.getAuthorName().getFirstInitial().equalsIgnoreCase(sanitizedIdentityName.getFirstInitial())
							&& 
							StringUtils.containsIgnoreCase(author.getAuthorName().getLastName(), sanitizedIdentityName.getLastName()))) {
						author.setTargetAuthor(true);
						originalAuthor.setTargetAuthor(true);
						multipleMarkedTargetAuthor.add(entry);
						count++;
					}
					else {
						author.setTargetAuthor(false);
						originalAuthor.setTargetAuthor(false);
					}
				}
				else if(matchCount == 0) {
					if(sanitizedIdentityAuthors.stream().anyMatch(sanitizedIdentityName -> sanitizedIdentityName.getFirstInitial() != null
							&& 
							sanitizedIdentityName.getLastName() != null
							&& 
							author.getAuthorName().getFirstInitial().equalsIgnoreCase(sanitizedIdentityName.getFirstInitial())
							&& 
							StringUtils.containsIgnoreCase(author.getAuthorName().getLastName(), sanitizedIdentityName.getLastName()))) {
						author.setTargetAuthor(true);
						originalAuthor.setTargetAuthor(true);
						multipleMarkedTargetAuthor.add(entry);
						count++;
					}
					else {
						author.setTargetAuthor(false);
						originalAuthor.setTargetAuthor(false);
					}
				}
			}
		}
		if(matchCount > 1 && count == 0) {
			return matchCount;
		}
		return count;
	}
	
	
	//Step 10: attempt lastName match from article to identity using all name aliases in identity table
	/**
	 * Check for exact last name match from article to identity
	 * @param authors
	 * @param sanitizedIdentityAuthors
	 * @param matchCount
	 * @param multipleMarkedTargetAuthor
	 * @return
	 */
	public int checkLastNameExactMatch(Set<Entry<ReCiterAuthor, ReCiterAuthor>> authors, List<AuthorName> sanitizedIdentityAuthors, int matchCount, Set<Entry<ReCiterAuthor, ReCiterAuthor>> multipleMarkedTargetAuthor) {
		int count = 0;
		if(matchCount > 1) {
			authors = new HashSet<Entry<ReCiterAuthor, ReCiterAuthor>>(multipleMarkedTargetAuthor);
		}
		for (Entry<ReCiterAuthor, ReCiterAuthor> entry : authors) {
			ReCiterAuthor author = entry.getValue();
			ReCiterAuthor originalAuthor = entry.getKey();
			if(author.getAuthorName().getLastName() != null) {
				if(matchCount > 1 && author.isTargetAuthor()) {
					if(sanitizedIdentityAuthors.stream().anyMatch(sanitizedIdentityName -> sanitizedIdentityName.getLastName() != null
							&&
							StringUtils.equalsIgnoreCase(author.getAuthorName().getLastName().trim(), sanitizedIdentityName.getLastName().trim()))) {
						author.setTargetAuthor(true);
						originalAuthor.setTargetAuthor(true);
						multipleMarkedTargetAuthor.add(entry);
						count++;
					}
					else {
						author.setTargetAuthor(false);
						originalAuthor.setTargetAuthor(false);
					}		
				}
				else if(matchCount == 0) {
					if(sanitizedIdentityAuthors.stream().anyMatch(sanitizedIdentityName -> sanitizedIdentityName.getLastName() != null
							&&
							StringUtils.equalsIgnoreCase(author.getAuthorName().getLastName().trim(), sanitizedIdentityName.getLastName().trim()))) {
						author.setTargetAuthor(true);
						originalAuthor.setTargetAuthor(true);
						multipleMarkedTargetAuthor.add(entry);
						count++;
					}
					else {
						author.setTargetAuthor(false);
						originalAuthor.setTargetAuthor(false);
					}
				}
			}
		}
		if(matchCount > 1 && count == 0) {
			return matchCount;
		}
		return count;
	}
	
	//Step 11: attempt strict firstname match
	/**
	 * Check for exact first name match from article to identity
	 * @param authors
	 * @param sanitizedIdentityAuthors
	 * @param matchCount
	 * @param multipleMarkedTargetAuthor
	 * @return
	 */
	public int checkFirstNameExactMatch(Set<Entry<ReCiterAuthor, ReCiterAuthor>> authors, List<AuthorName> sanitizedIdentityAuthors, int matchCount, Set<Entry<ReCiterAuthor, ReCiterAuthor>> multipleMarkedTargetAuthor) {
		int count = 0;
		if(matchCount > 1) {
			authors = new HashSet<Entry<ReCiterAuthor, ReCiterAuthor>>(multipleMarkedTargetAuthor);
		}
		for (Entry<ReCiterAuthor, ReCiterAuthor> entry : authors) {
			ReCiterAuthor author = entry.getValue();
			ReCiterAuthor originalAuthor = entry.getKey();
			if(author.getAuthorName().getFirstName() != null) {
				if(matchCount > 1 && author.isTargetAuthor()) {
					if(sanitizedIdentityAuthors.stream().anyMatch(sanitizedIdentityName -> sanitizedIdentityName.getFirstName() != null
							&&
							StringUtils.equalsIgnoreCase(author.getAuthorName().getFirstName().trim(), sanitizedIdentityName.getFirstName().trim()))) {
						author.setTargetAuthor(true);
						originalAuthor.setTargetAuthor(true);
						multipleMarkedTargetAuthor.add(entry);
						count++;
					}
					else {
						author.setTargetAuthor(false);
						originalAuthor.setTargetAuthor(false);
					}
				}
				else if(matchCount == 0) {
					if(sanitizedIdentityAuthors.stream().anyMatch(sanitizedIdentityName -> sanitizedIdentityName.getFirstName() != null
							&&
							StringUtils.equalsIgnoreCase(author.getAuthorName().getFirstName().trim(), sanitizedIdentityName.getFirstName().trim()))) {
						author.setTargetAuthor(true);
						originalAuthor.setTargetAuthor(true);
						multipleMarkedTargetAuthor.add(entry);
						count++;
					}
					else {
						author.setTargetAuthor(false);
						originalAuthor.setTargetAuthor(false);
					}
				}
			}
		}
		if(matchCount > 1 && count == 0) {
			return matchCount;
		}
		return count;
	}
	
	//Step 12: Attempt full last name match from article to partial last name from identity. (e.g., Somersan-Karakaya)
	/**
	 * Check for full last name with partial last name from identity
	 * @param authors
	 * @param sanitizedIdentityAuthors
	 * @param matchCount
	 * @param multipleMarkedTargetAuthor
	 * @return
	 */
	public int checkLastNameFullArticleToIdentityPartialMatch(Set<Entry<ReCiterAuthor, ReCiterAuthor>> authors, List<AuthorName> sanitizedIdentityAuthors, int matchCount, Set<Entry<ReCiterAuthor, ReCiterAuthor>> multipleMarkedTargetAuthor) {
		int count = 0;
		if(matchCount > 1) {
			authors = new HashSet<Entry<ReCiterAuthor, ReCiterAuthor>>(multipleMarkedTargetAuthor);
		}
		for (Entry<ReCiterAuthor, ReCiterAuthor> entry : authors) {
			ReCiterAuthor author = entry.getValue();
			ReCiterAuthor originalAuthor = entry.getKey();
			if(author.getAuthorName().getLastName() != null) {
				if(matchCount > 1 && author.isTargetAuthor()) {
					if(sanitizedIdentityAuthors.stream().anyMatch(sanitizedIdentityName -> sanitizedIdentityName.getLastName() != null
							&&
							StringUtils.containsIgnoreCase(sanitizedIdentityName.getLastName().trim(), author.getAuthorName().getLastName().trim()))) {
						author.setTargetAuthor(true);
						multipleMarkedTargetAuthor.add(entry);
						originalAuthor.setTargetAuthor(true);
						count++;
					}
					else {
						author.setTargetAuthor(false);
						originalAuthor.setTargetAuthor(false);
					}
				}
				else if(matchCount == 0) {
					if(sanitizedIdentityAuthors.stream().anyMatch(sanitizedIdentityName -> sanitizedIdentityName.getLastName() != null
							&&
							StringUtils.containsIgnoreCase(sanitizedIdentityName.getLastName().trim(), author.getAuthorName().getLastName().trim()))) {
						author.setTargetAuthor(true);
						originalAuthor.setTargetAuthor(true);
						multipleMarkedTargetAuthor.add(entry);
						count++;
					}
					else {
						author.setTargetAuthor(false);
						originalAuthor.setTargetAuthor(false);
					}
				}
			}
		}
		if(matchCount > 1 && count == 0) {
			return matchCount;
		}
		return count;
	}
	
	
	/**
	 * Disambiguate when multiple authors are flagged as target author.
	 * Scores each matched author by how closely their name components match
	 * the identity record, then picks the single best match if one exists.
	 *
	 * Scoring:
	 *   +3  exact first name match
	 *   +1  first initial match (only if no exact first name match)
	 *   +2  exact middle name match
	 *   +1  middle initial match (only if no exact middle name match)
	 *
	 * If exactly one author has the highest score and it exceeds the runner-up,
	 * that author is kept and the others are unmarked. If tied, all remain marked
	 * (no guessing).
	 *
	 * @param multipleMarkedTargetAuthor the set of authors currently marked as target
	 * @param sanitizedIdentityAuthors the identity name variants to compare against
	 * @return the number of authors still marked as target after disambiguation
	 */
	public int disambiguateMultipleMatches(Set<Entry<ReCiterAuthor, ReCiterAuthor>> multipleMarkedTargetAuthor,
	                                        List<AuthorName> sanitizedIdentityAuthors) {
		if(multipleMarkedTargetAuthor == null || multipleMarkedTargetAuthor.size() <= 1) {
			return multipleMarkedTargetAuthor == null ? 0 : multipleMarkedTargetAuthor.size();
		}

		// Score each matched author
		Entry<ReCiterAuthor, ReCiterAuthor> bestEntry = null;
		int bestScore = -1;
		boolean tied = false;

		for(Entry<ReCiterAuthor, ReCiterAuthor> entry : multipleMarkedTargetAuthor) {
			ReCiterAuthor author = entry.getValue();
			if(!author.isTargetAuthor()) continue;

			int score = 0;
			for(AuthorName identityName : sanitizedIdentityAuthors) {
				int nameScore = 0;

				// First name scoring
				if(author.getAuthorName().getFirstName() != null && identityName.getFirstName() != null
						&& author.getAuthorName().getFirstName().length() > 1 && identityName.getFirstName().length() > 1
						&& author.getAuthorName().getFirstName().equalsIgnoreCase(identityName.getFirstName())) {
					nameScore += 3;
				} else if(author.getAuthorName().getFirstInitial() != null && identityName.getFirstInitial() != null
						&& author.getAuthorName().getFirstInitial().equalsIgnoreCase(identityName.getFirstInitial())) {
					nameScore += 1;
				}

				// Middle name scoring
				if(author.getAuthorName().getMiddleName() != null && identityName.getMiddleName() != null
						&& author.getAuthorName().getMiddleName().length() > 1 && identityName.getMiddleName().length() > 1
						&& author.getAuthorName().getMiddleName().equalsIgnoreCase(identityName.getMiddleName())) {
					nameScore += 2;
				} else if(author.getAuthorName().getMiddleInitial() != null && identityName.getMiddleInitial() != null
						&& author.getAuthorName().getMiddleInitial().length() > 0 && identityName.getMiddleInitial().length() > 0
						&& author.getAuthorName().getMiddleInitial().equalsIgnoreCase(identityName.getMiddleInitial())) {
					nameScore += 1;
				}

				// Take the best score across all identity name variants
				score = Math.max(score, nameScore);
			}

			if(score > bestScore) {
				bestScore = score;
				bestEntry = entry;
				tied = false;
			} else if(score == bestScore) {
				tied = true;
			}
		}

		// Only disambiguate if there's a clear winner (not tied)
		if(!tied && bestEntry != null && bestScore > 0) {
			for(Entry<ReCiterAuthor, ReCiterAuthor> entry : multipleMarkedTargetAuthor) {
				ReCiterAuthor author = entry.getValue();
				ReCiterAuthor originalAuthor = entry.getKey();
				if(entry.equals(bestEntry)) {
					author.setTargetAuthor(true);
					originalAuthor.setTargetAuthor(true);
				} else {
					author.setTargetAuthor(false);
					originalAuthor.setTargetAuthor(false);
				}
			}
			return 1;
		}

		// Tied or no score — leave as-is
		int remaining = 0;
		for(Entry<ReCiterAuthor, ReCiterAuthor> entry : multipleMarkedTargetAuthor) {
			if(entry.getValue().isTargetAuthor()) remaining++;
		}
		return remaining;
	}

	//Step 18: Attempt fuzzy last name match (Levenshtein distance <= 2) with first name or first initial match
	/**
	 * Check if last names are within Levenshtein distance 2 (handling typos and
	 * spelling variants) AND the first name or first initial also matches.
	 * This is the loosest matching step and only fires when all other steps fail.
	 * Examples:
	 *   - "Kovanhkaya" vs "Kovanlikaya" (PubMed typo, distance 2)
	 *   - "Polanecsky" vs "Polaneczky" (variant spelling, distance 2)
	 *   - "Seidman" vs "Siedman" (transposition, distance 2)
	 * Requires last name length >= 4 to avoid false positives on short names.
	 */
	public int checkFuzzyLastNameMatch(Set<Entry<ReCiterAuthor, ReCiterAuthor>> authors, List<AuthorName> sanitizedIdentityAuthors, int matchCount, Set<Entry<ReCiterAuthor, ReCiterAuthor>> multipleMarkedTargetAuthor) {
		int count = 0;
		if(matchCount > 1) {
			authors = new HashSet<Entry<ReCiterAuthor, ReCiterAuthor>>(multipleMarkedTargetAuthor);
		}
		for (Entry<ReCiterAuthor, ReCiterAuthor> entry : authors) {
			ReCiterAuthor author = entry.getValue();
			ReCiterAuthor originalAuthor = entry.getKey();
			if(author.getAuthorName().getLastName() != null && author.getAuthorName().getLastName().length() >= 4
					&& author.getAuthorName().getFirstInitial() != null) {
				if(matchCount > 1 && author.isTargetAuthor()) {
					if(sanitizedIdentityAuthors.stream().anyMatch(sanitizedIdentityName -> sanitizedIdentityName.getLastName() != null
							&& sanitizedIdentityName.getLastName().length() >= 4
							&&
							sanitizedIdentityName.getFirstInitial() != null
							&&
							ReCiterStringUtil.levenshteinDistance(
								author.getAuthorName().getLastName().toLowerCase(),
								sanitizedIdentityName.getLastName().toLowerCase()) <= 2
							&&
							ReCiterStringUtil.levenshteinDistance(
								author.getAuthorName().getLastName().toLowerCase(),
								sanitizedIdentityName.getLastName().toLowerCase()) > 0
							&&
							(author.getAuthorName().getFirstInitial().equalsIgnoreCase(sanitizedIdentityName.getFirstInitial())
							||
							(author.getAuthorName().getFirstName() != null && sanitizedIdentityName.getFirstName() != null
							&& author.getAuthorName().getFirstName().equalsIgnoreCase(sanitizedIdentityName.getFirstName()))))) {
						author.setTargetAuthor(true);
						originalAuthor.setTargetAuthor(true);
						multipleMarkedTargetAuthor.add(entry);
						count++;
					}
					else {
						author.setTargetAuthor(false);
						originalAuthor.setTargetAuthor(false);
					}
				}
				else if(matchCount == 0) {
					if(sanitizedIdentityAuthors.stream().anyMatch(sanitizedIdentityName -> sanitizedIdentityName.getLastName() != null
							&& sanitizedIdentityName.getLastName().length() >= 4
							&&
							sanitizedIdentityName.getFirstInitial() != null
							&&
							ReCiterStringUtil.levenshteinDistance(
								author.getAuthorName().getLastName().toLowerCase(),
								sanitizedIdentityName.getLastName().toLowerCase()) <= 2
							&&
							ReCiterStringUtil.levenshteinDistance(
								author.getAuthorName().getLastName().toLowerCase(),
								sanitizedIdentityName.getLastName().toLowerCase()) > 0
							&&
							(author.getAuthorName().getFirstInitial().equalsIgnoreCase(sanitizedIdentityName.getFirstInitial())
							||
							(author.getAuthorName().getFirstName() != null && sanitizedIdentityName.getFirstName() != null
							&& author.getAuthorName().getFirstName().equalsIgnoreCase(sanitizedIdentityName.getFirstName()))))) {
						author.setTargetAuthor(true);
						originalAuthor.setTargetAuthor(true);
						multipleMarkedTargetAuthor.add(entry);
						count++;
					}
					else {
						author.setTargetAuthor(false);
						originalAuthor.setTargetAuthor(false);
					}
				}
			}
		}
		if(matchCount > 1 && count == 0) {
			return matchCount;
		}
		return count;
	}

	//Step 13: Attempt strict first name and first initial of last name match
	/**
	 * Check for exact first name match and matching first character of last name.
	 * Handles cases where the last name is garbled but the first name is intact.
	 * Example: identity "Mark Polanec" matching article author "M Polanec" where
	 * last name may be truncated but first name and last initial are correct.
	 */
	public int checkFirstNameAndLastInitialMatch(Set<Entry<ReCiterAuthor, ReCiterAuthor>> authors, List<AuthorName> sanitizedIdentityAuthors, int matchCount, Set<Entry<ReCiterAuthor, ReCiterAuthor>> multipleMarkedTargetAuthor) {
		int count = 0;
		if(matchCount > 1) {
			authors = new HashSet<Entry<ReCiterAuthor, ReCiterAuthor>>(multipleMarkedTargetAuthor);
		}
		for (Entry<ReCiterAuthor, ReCiterAuthor> entry : authors) {
			ReCiterAuthor author = entry.getValue();
			ReCiterAuthor originalAuthor = entry.getKey();
			if(author.getAuthorName().getFirstName() != null && author.getAuthorName().getLastName() != null
					&& author.getAuthorName().getFirstName().length() > 1 && author.getAuthorName().getLastName().length() > 0) {
				if(matchCount > 1 && author.isTargetAuthor()) {
					if(sanitizedIdentityAuthors.stream().anyMatch(sanitizedIdentityName -> sanitizedIdentityName.getFirstName() != null
							&& sanitizedIdentityName.getFirstName().length() > 1
							&&
							sanitizedIdentityName.getLastName() != null
							&& sanitizedIdentityName.getLastName().length() > 0
							&&
							author.getAuthorName().getFirstName().equalsIgnoreCase(sanitizedIdentityName.getFirstName())
							&&
							author.getAuthorName().getLastName().substring(0, 1).equalsIgnoreCase(sanitizedIdentityName.getLastName().substring(0, 1)))) {
						author.setTargetAuthor(true);
						originalAuthor.setTargetAuthor(true);
						multipleMarkedTargetAuthor.add(entry);
						count++;
					}
					else {
						author.setTargetAuthor(false);
						originalAuthor.setTargetAuthor(false);
					}
				}
				else if(matchCount == 0) {
					if(sanitizedIdentityAuthors.stream().anyMatch(sanitizedIdentityName -> sanitizedIdentityName.getFirstName() != null
							&& sanitizedIdentityName.getFirstName().length() > 1
							&&
							sanitizedIdentityName.getLastName() != null
							&& sanitizedIdentityName.getLastName().length() > 0
							&&
							author.getAuthorName().getFirstName().equalsIgnoreCase(sanitizedIdentityName.getFirstName())
							&&
							author.getAuthorName().getLastName().substring(0, 1).equalsIgnoreCase(sanitizedIdentityName.getLastName().substring(0, 1)))) {
						author.setTargetAuthor(true);
						originalAuthor.setTargetAuthor(true);
						multipleMarkedTargetAuthor.add(entry);
						count++;
					}
					else {
						author.setTargetAuthor(false);
						originalAuthor.setTargetAuthor(false);
					}
				}
			}
		}
		if(matchCount > 1 && count == 0) {
			return matchCount;
		}
		return count;
	}

	//Step 14: Attempt identity middle name to article last name match
	/**
	 * Check if identity middle name matches article last name, with first name or
	 * first initial also matching. Handles cases where PubMed indexed the middle
	 * name as the last name (common with multi-part names).
	 * Example: identity "Brian C Edwards" (middle="C") where article has author
	 * "C Edwards" indexed differently.
	 */
	public int checkMiddleNameToLastNameMatch(Set<Entry<ReCiterAuthor, ReCiterAuthor>> authors, List<AuthorName> sanitizedIdentityAuthors, int matchCount, Set<Entry<ReCiterAuthor, ReCiterAuthor>> multipleMarkedTargetAuthor) {
		int count = 0;
		if(matchCount > 1) {
			authors = new HashSet<Entry<ReCiterAuthor, ReCiterAuthor>>(multipleMarkedTargetAuthor);
		}
		for (Entry<ReCiterAuthor, ReCiterAuthor> entry : authors) {
			ReCiterAuthor author = entry.getValue();
			ReCiterAuthor originalAuthor = entry.getKey();
			if(author.getAuthorName().getLastName() != null && author.getAuthorName().getFirstInitial() != null) {
				if(matchCount > 1 && author.isTargetAuthor()) {
					if(sanitizedIdentityAuthors.stream().anyMatch(sanitizedIdentityName -> sanitizedIdentityName.getMiddleName() != null
							&& sanitizedIdentityName.getMiddleName().length() > 1
							&&
							sanitizedIdentityName.getFirstInitial() != null
							&&
							author.getAuthorName().getLastName().equalsIgnoreCase(sanitizedIdentityName.getMiddleName())
							&&
							(author.getAuthorName().getFirstInitial().equalsIgnoreCase(sanitizedIdentityName.getFirstInitial())
							||
							(author.getAuthorName().getFirstName() != null && sanitizedIdentityName.getFirstName() != null
							&& author.getAuthorName().getFirstName().equalsIgnoreCase(sanitizedIdentityName.getFirstName()))))) {
						author.setTargetAuthor(true);
						originalAuthor.setTargetAuthor(true);
						multipleMarkedTargetAuthor.add(entry);
						count++;
					}
					else {
						author.setTargetAuthor(false);
						originalAuthor.setTargetAuthor(false);
					}
				}
				else if(matchCount == 0) {
					if(sanitizedIdentityAuthors.stream().anyMatch(sanitizedIdentityName -> sanitizedIdentityName.getMiddleName() != null
							&& sanitizedIdentityName.getMiddleName().length() > 1
							&&
							sanitizedIdentityName.getFirstInitial() != null
							&&
							author.getAuthorName().getLastName().equalsIgnoreCase(sanitizedIdentityName.getMiddleName())
							&&
							(author.getAuthorName().getFirstInitial().equalsIgnoreCase(sanitizedIdentityName.getFirstInitial())
							||
							(author.getAuthorName().getFirstName() != null && sanitizedIdentityName.getFirstName() != null
							&& author.getAuthorName().getFirstName().equalsIgnoreCase(sanitizedIdentityName.getFirstName()))))) {
						author.setTargetAuthor(true);
						originalAuthor.setTargetAuthor(true);
						multipleMarkedTargetAuthor.add(entry);
						count++;
					}
					else {
						author.setTargetAuthor(false);
						originalAuthor.setTargetAuthor(false);
					}
				}
			}
		}
		if(matchCount > 1 && count == 0) {
			return matchCount;
		}
		return count;
	}

	//Step 15: Attempt first-to-last and last-to-first name swap match
	/**
	 * Check if identity first name matches article last name AND identity last name
	 * matches article first name. Handles cases where PubMed reversed the first
	 * and last name fields.
	 * Example: identity "Daniel Li" matching article author "Li Daniel".
	 */
	public int checkFirstLastNameSwapMatch(Set<Entry<ReCiterAuthor, ReCiterAuthor>> authors, List<AuthorName> sanitizedIdentityAuthors, int matchCount, Set<Entry<ReCiterAuthor, ReCiterAuthor>> multipleMarkedTargetAuthor) {
		int count = 0;
		if(matchCount > 1) {
			authors = new HashSet<Entry<ReCiterAuthor, ReCiterAuthor>>(multipleMarkedTargetAuthor);
		}
		for (Entry<ReCiterAuthor, ReCiterAuthor> entry : authors) {
			ReCiterAuthor author = entry.getValue();
			ReCiterAuthor originalAuthor = entry.getKey();
			if(author.getAuthorName().getFirstName() != null && author.getAuthorName().getFirstName().length() > 1
					&& author.getAuthorName().getLastName() != null && author.getAuthorName().getLastName().length() > 1) {
				if(matchCount > 1 && author.isTargetAuthor()) {
					if(sanitizedIdentityAuthors.stream().anyMatch(sanitizedIdentityName -> sanitizedIdentityName.getFirstName() != null
							&& sanitizedIdentityName.getFirstName().length() > 1
							&&
							sanitizedIdentityName.getLastName() != null
							&& sanitizedIdentityName.getLastName().length() > 1
							&&
							author.getAuthorName().getFirstName().equalsIgnoreCase(sanitizedIdentityName.getLastName())
							&&
							author.getAuthorName().getLastName().equalsIgnoreCase(sanitizedIdentityName.getFirstName()))) {
						author.setTargetAuthor(true);
						originalAuthor.setTargetAuthor(true);
						multipleMarkedTargetAuthor.add(entry);
						count++;
					}
					else {
						author.setTargetAuthor(false);
						originalAuthor.setTargetAuthor(false);
					}
				}
				else if(matchCount == 0) {
					if(sanitizedIdentityAuthors.stream().anyMatch(sanitizedIdentityName -> sanitizedIdentityName.getFirstName() != null
							&& sanitizedIdentityName.getFirstName().length() > 1
							&&
							sanitizedIdentityName.getLastName() != null
							&& sanitizedIdentityName.getLastName().length() > 1
							&&
							author.getAuthorName().getFirstName().equalsIgnoreCase(sanitizedIdentityName.getLastName())
							&&
							author.getAuthorName().getLastName().equalsIgnoreCase(sanitizedIdentityName.getFirstName()))) {
						author.setTargetAuthor(true);
						originalAuthor.setTargetAuthor(true);
						multipleMarkedTargetAuthor.add(entry);
						count++;
					}
					else {
						author.setTargetAuthor(false);
						originalAuthor.setTargetAuthor(false);
					}
				}
			}
		}
		if(matchCount > 1 && count == 0) {
			return matchCount;
		}
		return count;
	}

	//Step 16: Attempt identity last name to article first name + identity first initial to article last name initial

	/**
	 * Check if identity last name matches article first name AND identity first
	 * initial matches the first character of article last name. Handles partial
	 * name swaps where the last name appears in the first name field.
	 * Example: identity "Brian Harvey" matching article "Harvey B" where first
	 * name field has "Harvey" and last name starts with "B".
	 */
	public int checkLastNameToFirstNameAndFirstInitialToLastInitialMatch(Set<Entry<ReCiterAuthor, ReCiterAuthor>> authors, List<AuthorName> sanitizedIdentityAuthors, int matchCount, Set<Entry<ReCiterAuthor, ReCiterAuthor>> multipleMarkedTargetAuthor) {
		int count = 0;
		if(matchCount > 1) {
			authors = new HashSet<Entry<ReCiterAuthor, ReCiterAuthor>>(multipleMarkedTargetAuthor);
		}
		for (Entry<ReCiterAuthor, ReCiterAuthor> entry : authors) {
			ReCiterAuthor author = entry.getValue();
			ReCiterAuthor originalAuthor = entry.getKey();
			if(author.getAuthorName().getFirstName() != null && author.getAuthorName().getFirstName().length() > 1
					&& author.getAuthorName().getLastName() != null && author.getAuthorName().getLastName().length() > 0) {
				if(matchCount > 1 && author.isTargetAuthor()) {
					if(sanitizedIdentityAuthors.stream().anyMatch(sanitizedIdentityName -> sanitizedIdentityName.getLastName() != null
							&& sanitizedIdentityName.getLastName().length() > 1
							&&
							sanitizedIdentityName.getFirstInitial() != null
							&&
							author.getAuthorName().getFirstName().equalsIgnoreCase(sanitizedIdentityName.getLastName())
							&&
							author.getAuthorName().getLastName().substring(0, 1).equalsIgnoreCase(sanitizedIdentityName.getFirstInitial()))) {
						author.setTargetAuthor(true);
						originalAuthor.setTargetAuthor(true);
						multipleMarkedTargetAuthor.add(entry);
						count++;
					}
					else {
						author.setTargetAuthor(false);
						originalAuthor.setTargetAuthor(false);
					}
				}
				else if(matchCount == 0) {
					if(sanitizedIdentityAuthors.stream().anyMatch(sanitizedIdentityName -> sanitizedIdentityName.getLastName() != null
							&& sanitizedIdentityName.getLastName().length() > 1
							&&
							sanitizedIdentityName.getFirstInitial() != null
							&&
							author.getAuthorName().getFirstName().equalsIgnoreCase(sanitizedIdentityName.getLastName())
							&&
							author.getAuthorName().getLastName().substring(0, 1).equalsIgnoreCase(sanitizedIdentityName.getFirstInitial()))) {
						author.setTargetAuthor(true);
						originalAuthor.setTargetAuthor(true);
						multipleMarkedTargetAuthor.add(entry);
						count++;
					}
					else {
						author.setTargetAuthor(false);
						originalAuthor.setTargetAuthor(false);
					}
				}
			}
		}
		if(matchCount > 1 && count == 0) {
			return matchCount;
		}
		return count;
	}

	//Step 17: Attempt both initials match (first initial to first initial, last initial to last initial)

	/**
	 * Check if identity first initial matches article first initial AND identity
	 * last name initial matches article last name initial. This is the loosest
	 * matching step and only fires when all other steps have failed.
	 * Example: identity "Cassie Sied" matching article "CS" where names are
	 * heavily abbreviated.
	 */
	public int checkBothInitialsMatch(Set<Entry<ReCiterAuthor, ReCiterAuthor>> authors, List<AuthorName> sanitizedIdentityAuthors, int matchCount, Set<Entry<ReCiterAuthor, ReCiterAuthor>> multipleMarkedTargetAuthor) {
		int count = 0;
		if(matchCount > 1) {
			authors = new HashSet<Entry<ReCiterAuthor, ReCiterAuthor>>(multipleMarkedTargetAuthor);
		}
		for (Entry<ReCiterAuthor, ReCiterAuthor> entry : authors) {
			ReCiterAuthor author = entry.getValue();
			ReCiterAuthor originalAuthor = entry.getKey();
			if(author.getAuthorName().getFirstInitial() != null && author.getAuthorName().getFirstInitial().length() > 0
					&& author.getAuthorName().getLastName() != null && author.getAuthorName().getLastName().length() > 0) {
				if(matchCount > 1 && author.isTargetAuthor()) {
					if(sanitizedIdentityAuthors.stream().anyMatch(sanitizedIdentityName -> sanitizedIdentityName.getFirstInitial() != null
							&& sanitizedIdentityName.getFirstInitial().length() > 0
							&&
							sanitizedIdentityName.getLastName() != null
							&& sanitizedIdentityName.getLastName().length() > 0
							&&
							author.getAuthorName().getFirstInitial().equalsIgnoreCase(sanitizedIdentityName.getFirstInitial())
							&&
							author.getAuthorName().getLastName().substring(0, 1).equalsIgnoreCase(sanitizedIdentityName.getLastName().substring(0, 1)))) {
						author.setTargetAuthor(true);
						originalAuthor.setTargetAuthor(true);
						multipleMarkedTargetAuthor.add(entry);
						count++;
					}
					else {
						author.setTargetAuthor(false);
						originalAuthor.setTargetAuthor(false);
					}
				}
				else if(matchCount == 0) {
					if(sanitizedIdentityAuthors.stream().anyMatch(sanitizedIdentityName -> sanitizedIdentityName.getFirstInitial() != null
							&& sanitizedIdentityName.getFirstInitial().length() > 0
							&&
							sanitizedIdentityName.getLastName() != null
							&& sanitizedIdentityName.getLastName().length() > 0
							&&
							author.getAuthorName().getFirstInitial().equalsIgnoreCase(sanitizedIdentityName.getFirstInitial())
							&&
							author.getAuthorName().getLastName().substring(0, 1).equalsIgnoreCase(sanitizedIdentityName.getLastName().substring(0, 1)))) {
						author.setTargetAuthor(true);
						originalAuthor.setTargetAuthor(true);
						multipleMarkedTargetAuthor.add(entry);
						count++;
					}
					else {
						author.setTargetAuthor(false);
						originalAuthor.setTargetAuthor(false);
					}
				}
			}
		}
		if(matchCount > 1 && count == 0) {
			return matchCount;
		}
		return count;
	}

	//Not Used
	public int checkLastNamePartMatch(List<ReCiterAuthor> authors, Identity identity, int matchCount) {
		int count = 0;
		
		for (ReCiterAuthor author : authors) {
			if(matchCount > 1 && author.isTargetAuthor()) {
				if(ReCiterStringUtil.deAccent(author.getAuthorName().getLastName().trim()).contains(ReCiterStringUtil.deAccent(identity.getPrimaryName().getLastName().trim())) ||
						identity.getAlternateNames().stream().anyMatch(alternateName -> ReCiterStringUtil.deAccent(author.getAuthorName().getLastName()).contains(ReCiterStringUtil.deAccent(alternateName.getLastName())))) {
					author.setTargetAuthor(true);
					count++;
				}
				else
					author.setTargetAuthor(false);
			}
			else if(matchCount == 0 && !author.isTargetAuthor()) {
				if(ReCiterStringUtil.deAccent(author.getAuthorName().getLastName().trim()).contains(ReCiterStringUtil.deAccent(identity.getPrimaryName().getLastName().trim())) ||
						identity.getAlternateNames().stream().anyMatch(alternateName -> ReCiterStringUtil.deAccent(author.getAuthorName().getLastName()).contains(ReCiterStringUtil.deAccent(alternateName.getLastName())))) {
					author.setTargetAuthor(true);
					count++;
				}
				else
					author.setTargetAuthor(false);
			}
		}
		return count;
	}
		
	//Not Used
	public int checkFirstNameMatch(List<ReCiterAuthor> authors, Identity identity, int matchCount) {
		int count = 0;
		for (ReCiterAuthor author : authors) {
			if(matchCount > 1 && author.isTargetAuthor()) {
				if(ReCiterStringUtil.deAccent(author.getAuthorName().getFirstName().trim()).contains(ReCiterStringUtil.deAccent(identity.getPrimaryName().getFirstName().trim())) ||
						identity.getAlternateNames().stream().anyMatch(alternateName -> ReCiterStringUtil.deAccent(author.getAuthorName().getFirstName()).contains(ReCiterStringUtil.deAccent(alternateName.getFirstName())))) {
					author.setTargetAuthor(true);
					count++;
				}
				else
					author.setTargetAuthor(false);
			}
			else if(matchCount == 0 && !author.isTargetAuthor()) {
				if(ReCiterStringUtil.deAccent(author.getAuthorName().getFirstName().trim()).contains(ReCiterStringUtil.deAccent(identity.getPrimaryName().getFirstName().trim())) ||
						identity.getAlternateNames().stream().anyMatch(alternateName -> ReCiterStringUtil.deAccent(author.getAuthorName().getFirstName()).contains(ReCiterStringUtil.deAccent(alternateName.getFirstName())))) {
					author.setTargetAuthor(true);
					count++;
				}
				else
					author.setTargetAuthor(false);
			}
			
		}
		return count;
	}
	
	//Not Used
	public int checkFirstInitialMatch(List<ReCiterAuthor> authors, Identity identity, int matchCount) {
        int count = 0;
		for (ReCiterAuthor author : authors) {
			if(author.getAuthorName().getFirstInitial() != null && identity.getPrimaryName().getFirstInitial() != null) {
				if(matchCount > 1 && author.isTargetAuthor()) {
					if(StringUtils.equalsIgnoreCase(ReCiterStringUtil.deAccent(author.getAuthorName().getFirstInitial().trim()), ReCiterStringUtil.deAccent(identity.getPrimaryName().getFirstInitial().trim())) ||
							identity.getAlternateNames().stream().anyMatch(alternateName -> ReCiterStringUtil.deAccent(author.getAuthorName().getFirstInitial()).equalsIgnoreCase(ReCiterStringUtil.deAccent(alternateName.getFirstInitial())))) {
						author.setTargetAuthor(true);
						count++;
					}
					else
						author.setTargetAuthor(false);
				}
				else if(matchCount == 0 && !author.isTargetAuthor()) {
					if(StringUtils.equalsIgnoreCase(ReCiterStringUtil.deAccent(author.getAuthorName().getFirstInitial().trim()), ReCiterStringUtil.deAccent(identity.getPrimaryName().getFirstInitial().trim())) ||
							identity.getAlternateNames().stream().anyMatch(alternateName -> ReCiterStringUtil.deAccent(author.getAuthorName().getFirstInitial()).equalsIgnoreCase(ReCiterStringUtil.deAccent(alternateName.getFirstInitial())))) {
						author.setTargetAuthor(true);
						count++;
					}
					else
						author.setTargetAuthor(false);
				}
			}
		}
		return count;
	}
	
	public void assignTargetAuthorFalse(List<ReCiterAuthor> authors) {
		for (ReCiterAuthor author : authors) {
			author.setTargetAuthor(false);
		}
	}
}

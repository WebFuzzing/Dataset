package reciter.algorithm.evidence.targetauthor.name.strategy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reciter.algorithm.cluster.article.scorer.ReCiterArticleScorer;
import reciter.algorithm.evidence.targetauthor.AbstractTargetAuthorStrategy;
import reciter.database.dynamodb.model.NameFrequency;
import reciter.engine.EngineParameters;
import reciter.engine.Feature;
import reciter.engine.analysis.evidence.AuthorNameEvidence;
import reciter.model.article.ReCiterArticle;
import reciter.model.article.ReCiterArticleAuthors;
import reciter.model.article.ReCiterAuthor;
import reciter.model.identity.AuthorName;
import reciter.model.identity.Identity;
import reciter.utils.ReCiterStringUtil;

/**
 * @author szd2013
 * This class scores ReCiterArticles based on name and assigns score for each part of the name - first, middle and last
 *
 */
public class ScoreByNameStrategy extends AbstractTargetAuthorStrategy {

	private static final Logger slf4jLogger = LoggerFactory.getLogger(ScoreByNameStrategy.class);
	
	private final List<String> nameExcludedSuffixes = Arrays.asList(ReCiterArticleScorer.strategyParameters.getNameExcludedSuffixes().trim().split("\\s*,\\s*"));

	@Override
	public double executeStrategy(List<ReCiterArticle> reCiterArticles, Identity identity) {
		
		//List<AuthorName> sanitizedIdentityAuthor = new ArrayList<AuthorName>();
		//Set<AuthorName> sanitizedTargetAuthor = new HashSet<AuthorName>();
		
		
		AuthorNameEvidence authorNameEvidence;
		
		/*if(identity != null) { 
			sanitizeIdentityAuthorNames(identity, sanitizedIdentityAuthor);
			checkToIgnoreNameVariants(sanitizedIdentityAuthor);
		}*/
		
		List<AuthorNameEvidence> authorNameEvidences = new ArrayList<AuthorNameEvidence>(identity.getSanitizedNames().size());
		
		for(ReCiterArticle reCiterArticle: reCiterArticles) {
			ReCiterArticleAuthors authors = reCiterArticle.getArticleCoAuthors();
			
			Map<ReCiterAuthor, ReCiterAuthor> sanitizedTargetAuthor = authors.getSanitizedAuthorMap()
			.entrySet()
			.stream()
			.filter(sanitizedArticleAuthor -> sanitizedArticleAuthor.getKey().getAuthorName() != null && sanitizedArticleAuthor.getKey().isTargetAuthor())
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
			
			
			int targetAuthorCount = getTargetAuthorCount(reCiterArticle);
			if(targetAuthorCount >=1) {			
				//sanitizeTargetAuthorNames(reCiterArticle, sanitizedTargetAuthor);
				for(Entry<AuthorName, AuthorName> entry: identity.getSanitizedNames().entrySet()) {
					AuthorName identityAuthorNameOriginal = entry.getKey();
					AuthorName identityAuthorName = entry.getValue();
					authorNameEvidence = new AuthorNameEvidence();
					//Combine following identity.middleName, identity.lastName into mergedName. Now attempt match against article.lastName.
					//Example: Garcia (identity.middleName) + Marquez (identity.lastName) = GarciaMarques (article.lastName)
					//If match: stop scoring middle and last name; move on to only score first name;
					
					scoreCombinedMiddleLastName(identityAuthorName, identityAuthorNameOriginal, sanitizedTargetAuthor, authorNameEvidence);
					if(authorNameEvidence.getNameMatchFirstType() != null 
							&&
							authorNameEvidence.getNameMatchMiddleType() != null
							&&
							authorNameEvidence.getNameMatchLastType() != null 
							&&
							authorNameEvidence.getNameMatchModifier() != null) {
						slf4jLogger.info("Combine following identity.middleName, identity.lastName into mergedName. Now attempt match against article.lastName.");
					}
					else {
						if(!isNotNullIdentityMiddleName(identity.getSanitizedNames().values())) {
							scoreFirstNameMiddleNameNull(identityAuthorName, identityAuthorNameOriginal, sanitizedTargetAuthor, authorNameEvidence);
						} else {
							if(identityAuthorName.getMiddleName() == null) {
								scoreFirstNameMiddleNameNull(identityAuthorName, identityAuthorNameOriginal, sanitizedTargetAuthor, authorNameEvidence);
							} else {
								scoreFirstNameMiddleName(identityAuthorName, identityAuthorNameOriginal, sanitizedTargetAuthor, authorNameEvidence);
							}
						}
						if(authorNameEvidence.getNameMatchLastType() == null) {
							scoreLastName(identityAuthorName, identityAuthorNameOriginal, sanitizedTargetAuthor, authorNameEvidence);
						}
					}
					
					if(authorNameEvidence.getNameMatchMiddleType() != null 
							&&
							StringUtils.equalsIgnoreCase(authorNameEvidence.getNameMatchMiddleType(), "full-exact")
							&&
							identityAuthorName.getMiddleName() != null
							&&
							identityAuthorName.getMiddleName().length() == 1) {
						authorNameEvidence.setNameMatchMiddleType("exact-singleInitial");
						authorNameEvidence.setNameMatchMiddleScore(ReCiterArticleScorer.strategyParameters.getNameMatchMiddleTypeExactSingleInitialScore());
						authorNameEvidence.setNameScoreTotal(authorNameEvidence.getNameMatchFirstScore() + authorNameEvidence.getNameMatchLastScore() + authorNameEvidence.getNameMatchMiddleScore() + authorNameEvidence.getNameMatchModifierScore());
					}
					authorNameEvidences.add(authorNameEvidence);
				}
				authorNameEvidence = calculateHighestScore(authorNameEvidences);
				
				//Clear the target author set
				if(sanitizedTargetAuthor != null 
						&&
						sanitizedTargetAuthor.size() > 0) {
					sanitizedTargetAuthor.clear();
				}
				//Clear AuthorNameEvidences
				if(authorNameEvidences != null
						&&
						authorNameEvidences.size() > 0) {
					authorNameEvidences.clear();
				}
			} else {
				authorNameEvidence = new AuthorNameEvidence();
				authorNameEvidence.setInstitutionalAuthorName(identity.getPrimaryName());
				authorNameEvidence.setNameMatchFirstType("nullTargetAuthor-MatchNotAttempted");
				authorNameEvidence.setNameMatchLastType("nullTargetAuthor-MatchNotAttempted");
				authorNameEvidence.setNameMatchMiddleType("nullTargetAuthor-MatchNotAttempted");
			}
			
			// Add first name frequency modifier: rare names get higher scores
			double nameFreqScore = computeFirstNameFrequencyScore(identity);
			if (nameFreqScore != 0.0 && authorNameEvidence.getNameMatchFirstScore() > 0) {
				double currentModifier = authorNameEvidence.getNameMatchModifierScore();
				authorNameEvidence.setNameMatchModifierScore(currentModifier + nameFreqScore);
				authorNameEvidence.setNameScoreTotal(
					authorNameEvidence.getNameMatchFirstScore()
					+ authorNameEvidence.getNameMatchLastScore()
					+ authorNameEvidence.getNameMatchMiddleScore()
					+ authorNameEvidence.getNameMatchModifierScore()
				);
			}

			reCiterArticle.setAuthorNameEvidence(authorNameEvidence);
			//enable if requrired for debugging
			//slf4jLogger.info("Pmid: " + reCiterArticle.getArticleId() + " " + authorNameEvidence.toString());
		}
		return 1;
	}
	
	/**
	 * Compute IDF-like score for the identity's first name.
	 * Rare names get higher scores; common names get ~0.
	 * For compound names (e.g., "Jean-Pierre"), averages the scores of each token.
	 */
	private double computeFirstNameFrequencyScore(Identity identity) {
		Map<String, NameFrequency> nameFrequencyMap = EngineParameters.getNameFrequencyMap();
		if (nameFrequencyMap == null || nameFrequencyMap.isEmpty()) {
			return 0.0;
		}
		String firstName = (identity.getPrimaryName() != null && identity.getPrimaryName().getFirstName() != null)
				? identity.getPrimaryName().getFirstName().trim() : "";
		if (firstName.isBlank()) {
			return 0.0;
		}
		// Split compound names by spaces and hyphens, discard single-char tokens (initials)
		String[] tokens = firstName.toLowerCase().split("[\\s\\-]+");
		double totalScore = 0.0;
		int validTokens = 0;
		for (String token : tokens) {
			token = token.replaceAll("[^a-z]", "");
			if (token.length() <= 1) {
				continue;
			}
			NameFrequency nf = nameFrequencyMap.get(token);
			if (nf != null) {
				totalScore += nf.getScore();
			} else {
				// Unknown name — use median score (0.9997 from our frequency table)
				totalScore += 1.0;
			}
			validTokens++;
		}
		if (validTokens == 0) {
			return 0.0;
		}
		return totalScore / validTokens;
	}

	private void scoreCombinedMiddleLastName(AuthorName identityAuthor, AuthorName identityAuthorNameOriginal, Map<ReCiterAuthor, ReCiterAuthor> articleAuthorNames, AuthorNameEvidence authorNameEvidence) {
		if(articleAuthorNames.size() > 0) {
			Map.Entry<ReCiterAuthor,ReCiterAuthor> entry = articleAuthorNames.entrySet().iterator().next();
			AuthorName articleAuthorName = entry.getValue().getAuthorName();
			AuthorName articleAuthorNameOriginal = entry.getKey().getAuthorName();
			if(identityAuthor.getMiddleName() != null 
				&& 
				StringUtils.equalsIgnoreCase(identityAuthor.getMiddleName() + identityAuthor.getLastName(), articleAuthorName.getLastName())
				&&
				StringUtils.equalsIgnoreCase(identityAuthor.getFirstName(), articleAuthorName.getFirstName())
					) {
				//Combine following identity.middleName, identity.lastName into mergedName. Now attempt match against article.lastName.
				//Example: Garcia (identity.middleName) + Marquez (identity.lastName) = GarciaMarquez (article.lastName)
				authorNameEvidence.setNameMatchFirstType("full-exact");
				authorNameEvidence.setNameMatchFirstScore(ReCiterArticleScorer.strategyParameters.getNameMatchFirstTypeFullExactScore());
				authorNameEvidence.setNameMatchLastType("full-exact");
				authorNameEvidence.setNameMatchLastScore(ReCiterArticleScorer.strategyParameters.getNameMatchLastTypeFullExactScore());
				authorNameEvidence.setNameMatchMiddleType("full-exact");
				authorNameEvidence.setNameMatchMiddleScore(ReCiterArticleScorer.strategyParameters.getNameMatchMiddleTypeFullExactScore());
				authorNameEvidence.setNameMatchModifier("combinedMiddleNameLastName");
				authorNameEvidence.setNameMatchModifierScore(ReCiterArticleScorer.strategyParameters.getNameMatchModifierCombinedMiddleNameLastNameScore());
			} else if(identityAuthor.getMiddleName() != null 
					&& 
					StringUtils.equalsIgnoreCase(identityAuthor.getMiddleName() + identityAuthor.getLastName(), articleAuthorName.getLastName())
					&&
					StringUtils.equalsIgnoreCase(identityAuthor.getFirstInitial(), articleAuthorName.getFirstName())) {
				authorNameEvidence.setNameMatchFirstType("inferredInitials-exact");
				authorNameEvidence.setNameMatchFirstScore(ReCiterArticleScorer.strategyParameters.getNameMatchFirstTypeInferredInitialsExactScore());
				authorNameEvidence.setNameMatchLastType("full-exact");
				authorNameEvidence.setNameMatchLastScore(ReCiterArticleScorer.strategyParameters.getNameMatchLastTypeFullExactScore());
				authorNameEvidence.setNameMatchMiddleType("full-exact");
				authorNameEvidence.setNameMatchMiddleScore(ReCiterArticleScorer.strategyParameters.getNameMatchMiddleTypeFullExactScore());
				authorNameEvidence.setNameMatchModifier("combinedMiddleNameLastName");
				authorNameEvidence.setNameMatchModifierScore(ReCiterArticleScorer.strategyParameters.getNameMatchModifierCombinedMiddleNameLastNameScore());
			}
			authorNameEvidence.setInstitutionalAuthorName(identityAuthorNameOriginal);
			authorNameEvidence.setArticleAuthorName(articleAuthorNameOriginal);
			authorNameEvidence.setNameScoreTotal(authorNameEvidence.getNameMatchFirstScore() + authorNameEvidence.getNameMatchMiddleScore() + authorNameEvidence.getNameMatchLastScore() + authorNameEvidence.getNameMatchModifierScore());
		}
	}
	
	private void scoreLastName(AuthorName identityAuthor, AuthorName identityAuthorNameOriginal, Map<ReCiterAuthor, ReCiterAuthor> articleAuthorNames, AuthorNameEvidence authorNameEvidence) {
		if(!articleAuthorNames.isEmpty()) {
			Map.Entry<ReCiterAuthor,ReCiterAuthor> entry = articleAuthorNames.entrySet().iterator().next();
			AuthorName articleAuthorName = entry.getValue().getAuthorName();
			AuthorName articleAuthorNameOriginal = entry.getKey().getAuthorName();
			if(StringUtils.equalsIgnoreCase(identityAuthor.getLastName(), articleAuthorName.getLastName())) {
				//Attempt full exact match where identity.lastName = article.lastName.
				//Example: Cole (identity.lastName) = Cole (article.lastName)
				authorNameEvidence.setNameMatchLastType("full-exact");
				authorNameEvidence.setNameMatchLastScore(ReCiterArticleScorer.strategyParameters.getNameMatchLastTypeFullExactScore());
			}  else if(identityAuthor.getMiddleName() != null && StringUtils.containsIgnoreCase(identityAuthor.getLastName(), articleAuthorName.getLastName())) {
				//Attempt partial match where "%" + identity.lastName + "%" = article.lastName
				//Example: Cole (identity.lastName) = Del Cole (article.lastName)
				authorNameEvidence.setNameMatchLastType("full-exact");
				authorNameEvidence.setNameMatchLastScore(ReCiterArticleScorer.strategyParameters.getNameMatchLastTypeFullExactScore());
				authorNameEvidence.setNameMatchModifier("identitySubstringOfArticle-lastName");
				authorNameEvidence.setNameMatchModifierScore(ReCiterArticleScorer.strategyParameters.getNameMatchModifierIdentitySubstringOfArticleLastnameScore());
			} else if(identityAuthor.getLastName().length() >= 4 && ReCiterStringUtil.levenshteinDistance(identityAuthor.getLastName(), articleAuthorName.getLastName()) <= 1) {
				//Attempt match where identity.lastName >= 4 characters and levenshteinDistance between identity.lastName and article.lastName is <=1.
				//Example: Kaushal (identity.lastName) = Kaushai (article.lastName)
				authorNameEvidence.setNameMatchLastType("full-fuzzy");
				authorNameEvidence.setNameMatchLastScore(ReCiterArticleScorer.strategyParameters.getNameMatchLastTypeFullFuzzyScore());
			} else {
				authorNameEvidence.setNameMatchLastType("full-conflictingEntirely");
				authorNameEvidence.setNameMatchLastScore(ReCiterArticleScorer.strategyParameters.getNameMatchLastTypeFullConflictingEntirelyScore());
			}
			authorNameEvidence.setInstitutionalAuthorName(identityAuthorNameOriginal);
			authorNameEvidence.setArticleAuthorName(articleAuthorNameOriginal);
			authorNameEvidence.setNameScoreTotal(authorNameEvidence.getNameMatchFirstScore() + authorNameEvidence.getNameMatchMiddleScore() + authorNameEvidence.getNameMatchLastScore() + authorNameEvidence.getNameMatchModifierScore());
		}
	}
	
	/**
	 * Label a first-name match in which the registered (identity) first name is a
	 * left-anchored prefix of the article first name, rather than equal to it.
	 *
	 * <p>Before issue #746 both callers of this helper reported {@code full-exact}, so a
	 * byline of "Shuofei" was indistinguishable from a genuine exact match against a
	 * registered "Shuo". The compensating {@code identitySubstringOfArticle-firstName}
	 * modifier lives in a different feature, so nothing downstream could tell the two
	 * apart. Two honest buckets replace it:</p>
	 * <ul>
	 *   <li>{@code inferredInitials-prefix} when the registered first name sanitized down
	 *       to a single letter ('M.' &rarr; M matching "Mayra"). That is an initial match;
	 *       {@code inferredInitials-exact} (0.441) is the semantically correct score, but
	 *       moving it there is a numeric change and is deliberately deferred.</li>
	 *   <li>{@code full-prefix} otherwise (Paul &rarr; PaulJames, Shuo &rarr; Shuofei).</li>
	 * </ul>
	 *
	 * <p>Both scores default to the previous {@code full-exact} value (1.852) so this
	 * relabel moves no model input. Retuning either is a one-line properties change that
	 * requires a retrain plus an evaluation sweep, because the type string is itself
	 * consumed as a numeric feature ({@code nameMatchTypeOrdinal}) by the scoring Lambda.</p>
	 *
	 * <p>This helper only assigns the first-name type and score. Callers keep full control
	 * of the middle-name type/score and the modifier, which are unchanged.</p>
	 *
	 * @param identityAuthor the sanitized identity name whose first name matched
	 * @param authorNameEvidence evidence object to populate
	 */
	private void assignFirstNamePrefixEvidence(AuthorName identityAuthor, AuthorNameEvidence authorNameEvidence) {
		if(identityAuthor.getFirstName() != null && identityAuthor.getFirstName().length() == 1) {
			authorNameEvidence.setNameMatchFirstType("inferredInitials-prefix");
			authorNameEvidence.setNameMatchFirstScore(ReCiterArticleScorer.strategyParameters.getNameMatchFirstTypeInferredInitialsPrefixScore());
		} else {
			authorNameEvidence.setNameMatchFirstType("full-prefix");
			authorNameEvidence.setNameMatchFirstScore(ReCiterArticleScorer.strategyParameters.getNameMatchFirstTypeFullPrefixScore());
		}
	}

	/** This function matches and scores first name and middle name where middle name is null in identity
	 * All of the matching that follows should be evaluated as a series of ifElse statements (once we get a match, we stop). The goal is to match as early on in the process as possible.
	 * 	Matching should be case insensitive, however, we will pull out some characters below based on case.
	*/
	private void scoreFirstNameMiddleNameNull(AuthorName identityAuthor, AuthorName identityAuthorNameOriginal, Map<ReCiterAuthor, ReCiterAuthor> articleAuthorNames, AuthorNameEvidence authorNameEvidence) {
		if(articleAuthorNames.size() > 0) {
			Map.Entry<ReCiterAuthor,ReCiterAuthor> entry = articleAuthorNames.entrySet().iterator().next();
			AuthorName articleAuthorName = entry.getValue().getAuthorName();
			AuthorName articleAuthorNameOriginal = entry.getKey().getAuthorName();
			
			if(identityAuthor.getFirstName() != null 
					&&
					StringUtils.equalsIgnoreCase(identityAuthor.getFirstName(), articleAuthorName.getFirstName())) {
				//Attempt match where identity.firstName = article.firstName
				//Example: Paul (identity.firstName) = Paul (article.firstName)
				authorNameEvidence.setNameMatchFirstType("full-exact");
				authorNameEvidence.setNameMatchFirstScore(ReCiterArticleScorer.strategyParameters.getNameMatchFirstTypeFullExactScore());
				authorNameEvidence.setNameMatchMiddleType("identityNull-MatchNotAttempted");
				authorNameEvidence.setNameMatchMiddleScore(ReCiterArticleScorer.strategyParameters.getNameMatchMiddleTypeIdentityNullMatchNotAttemptedScore());
			} else if(identityAuthor.getFirstName() != null 
					&& 
					identityAuthor.getLastName() != null 
					&& 
					articleAuthorName.getFirstName() != null
					&&
					articleAuthorName.getLastName() != null
					&& 
					StringUtils.equalsIgnoreCase((identityAuthor.getFirstName() + identityAuthor.getLastName()).replaceAll("[\\s-]+", ""), (articleAuthorName.getFirstName() + articleAuthorName.getLastName()).replaceAll("[\\s-]+", ""))) {
				//Attempt match where identity.firstName + identity.lastName = article.firstName + article.lastName
				//Example: Landys (identity.firstName) + Lopez quezada (identity.lastName) = Landys Lopez (article.firstName) + Quezada (article.lastName)
				authorNameEvidence.setNameMatchFirstType("full-exact");
				authorNameEvidence.setNameMatchFirstScore(ReCiterArticleScorer.strategyParameters.getNameMatchFirstTypeFullExactScore());
				authorNameEvidence.setNameMatchLastType("full-exact");
				authorNameEvidence.setNameMatchLastScore(ReCiterArticleScorer.strategyParameters.getNameMatchLastTypeFullExactScore());
				authorNameEvidence.setNameMatchMiddleType("identityNull-MatchNotAttempted");
				authorNameEvidence.setNameMatchMiddleScore(ReCiterArticleScorer.strategyParameters.getNameMatchMiddleTypeIdentityNullMatchNotAttemptedScore());
				authorNameEvidence.setNameMatchModifier("combinedFirstNameLastName");
				authorNameEvidence.setNameMatchModifierScore(ReCiterArticleScorer.strategyParameters.getNameMatchModifierCombinedFirstNameLastNameScore());
			} else if(identityAuthor.getFirstName() != null 
					&& 
					articleAuthorName.getFirstName() != null
					&&
					identityAuthorNameOriginal.getFirstName() != null && (identityAuthorNameOriginal.getFirstName().contains(" ") || identityAuthorNameOriginal.getFirstName().contains("-"))
					&&
					StringUtils.equalsIgnoreCase(Arrays.stream(identityAuthorNameOriginal.getFirstName().split("[-\\s]")).map(s -> s.charAt(0)).collect(Collectors.toList()).stream().map(String::valueOf).collect(Collectors.joining()), articleAuthorName.getFirstName())) {
				//Attempt match where identity.firstName + identity.lastName = article.firstName + article.lastName
				//Example: Landys (identity.firstName) + Lopez quezada (identity.lastName) = Landys Lopez (article.firstName) + Quezada (article.lastName)
				authorNameEvidence.setNameMatchFirstType("inferredInitials-exact");
				authorNameEvidence.setNameMatchFirstScore(ReCiterArticleScorer.strategyParameters.getNameMatchFirstTypeInferredInitialsExactScore());
				authorNameEvidence.setNameMatchMiddleType("identityNull-MatchNotAttempted");
				authorNameEvidence.setNameMatchMiddleScore(ReCiterArticleScorer.strategyParameters.getNameMatchMiddleTypeIdentityNullMatchNotAttemptedScore());
				
			} else if(identityAuthor.getFirstName() != null 
					&&
					articleAuthorName.getFirstName().toLowerCase().startsWith(identityAuthor.getFirstName().toLowerCase())) { 
				//Attempt match where identity.firstName is a left-anchored substring of article.firstName
				//Example: Paul (identity.firstName) = PaulJames (article.firstName)
				//This is NOT an exact match. The same predicate also fires for
				//Shuo (identity.firstName) = Shuofei (article.firstName), a different given
				//name, and for a registered name that sanitized down to a single initial:
				//'M.' -> M (identity.firstName) = Mayra (article.firstName). See issue #746.
				//The predicate is untouched; only the reported type is split, so exactly the
				//same articles reach exactly the same score.
				assignFirstNamePrefixEvidence(identityAuthor, authorNameEvidence);
				authorNameEvidence.setNameMatchMiddleType("identityNull-MatchNotAttempted");
				authorNameEvidence.setNameMatchMiddleScore(ReCiterArticleScorer.strategyParameters.getNameMatchMiddleTypeIdentityNullMatchNotAttemptedScore());
				authorNameEvidence.setNameMatchModifier("identitySubstringOfArticle-firstName");
				authorNameEvidence.setNameMatchModifierScore(ReCiterArticleScorer.strategyParameters.getNameMatchModifierIdentitySubstringOfArticleFirstnameScore());
			} else if(identityAuthor.getFirstName() != null
					&&
					identityAuthor.getFirstName().toLowerCase().startsWith(articleAuthorName.getFirstName().toLowerCase())) {
				//Attempt match where article.firstName is a left-anchored substring of identity.firstName
				//Example: Paul (identity.firstName) = P (article.firstName)
				authorNameEvidence.setNameMatchFirstType("inferredInitials-exact");
				authorNameEvidence.setNameMatchFirstScore(ReCiterArticleScorer.strategyParameters.getNameMatchFirstTypeInferredInitialsExactScore());
				authorNameEvidence.setNameMatchMiddleType("identityNull-MatchNotAttempted");
				authorNameEvidence.setNameMatchMiddleScore(ReCiterArticleScorer.strategyParameters.getNameMatchMiddleTypeIdentityNullMatchNotAttemptedScore());
			} else if(identityAuthor.getFirstName() != null 
					&& 
					identityAuthor.getFirstName().length() >= 3 
					&& 
					articleAuthorName.getFirstName().length() >= 3 
					&&
					StringUtils.equalsIgnoreCase(identityAuthor.getFirstName().substring(0, 3),articleAuthorName.getFirstName().substring(0, 3))) {
				//Attempt match where first three characters of identity.firstName = first three characters of article.firstName
				//Example: Paul (identity.firstName) = Pau (article.firstName)
				authorNameEvidence.setNameMatchFirstType("full-fuzzy");
				authorNameEvidence.setNameMatchFirstScore(ReCiterArticleScorer.strategyParameters.getNameMatchFirstTypeFullFuzzyScore());
				authorNameEvidence.setNameMatchMiddleType("identityNull-MatchNotAttempted");
				authorNameEvidence.setNameMatchMiddleScore(ReCiterArticleScorer.strategyParameters.getNameMatchMiddleTypeIdentityNullMatchNotAttemptedScore());
			} else if(identityAuthor.getFirstName() != null 
					&&
					identityAuthor.getFirstName().length() >= 4 
					&& 
					ReCiterStringUtil.levenshteinDistance(identityAuthor.getFirstName(), articleAuthorName.getFirstName()) == 1) {
				//Attempt match where identity.firstName is greater than 4 characters and Levenshtein distance between identity.firstName and article.firstName is 1.
				//Example: Paula (identity.firstName) = Pauly (article.firstName)
				authorNameEvidence.setNameMatchFirstType("full-fuzzy");
				authorNameEvidence.setNameMatchFirstScore(ReCiterArticleScorer.strategyParameters.getNameMatchFirstTypeFullFuzzyScore());
				authorNameEvidence.setNameMatchMiddleType("identityNull-MatchNotAttempted");
				authorNameEvidence.setNameMatchMiddleScore(ReCiterArticleScorer.strategyParameters.getNameMatchMiddleTypeIdentityNullMatchNotAttemptedScore());
			} else if(identityAuthor.getFirstName() != null 
					&&
					StringUtils.equalsIgnoreCase(identityAuthor.getFirstInitial(),articleAuthorName.getFirstInitial())) {
				//Attempt match where first character of identity.firstName = first character of article.firstName
				//Example: Paul (identity.firstName) = Peter (article.firstName)
				authorNameEvidence.setNameMatchFirstType("full-conflictingAllButInitials");
				authorNameEvidence.setNameMatchFirstScore(ReCiterArticleScorer.strategyParameters.getNameMatchFirstTypeFullConflictingAllButInitialsScore());
				authorNameEvidence.setNameMatchMiddleType("identityNull-MatchNotAttempted");
				authorNameEvidence.setNameMatchMiddleScore(ReCiterArticleScorer.strategyParameters.getNameMatchMiddleTypeIdentityNullMatchNotAttemptedScore());
			} else {
				authorNameEvidence.setNameMatchFirstType("full-conflictingEntirely");
				authorNameEvidence.setNameMatchFirstScore(ReCiterArticleScorer.strategyParameters.getNameMatchFirstTypeFullConflictingEntirelyScore());
				authorNameEvidence.setNameMatchMiddleType("identityNull-MatchNotAttempted");
				authorNameEvidence.setNameMatchMiddleScore(ReCiterArticleScorer.strategyParameters.getNameMatchMiddleTypeIdentityNullMatchNotAttemptedScore());
			}
			authorNameEvidence.setInstitutionalAuthorName(identityAuthorNameOriginal);
			authorNameEvidence.setArticleAuthorName(articleAuthorNameOriginal);
			authorNameEvidence.setNameScoreTotal(authorNameEvidence.getNameMatchFirstScore() + authorNameEvidence.getNameMatchMiddleScore() + authorNameEvidence.getNameMatchLastScore() + authorNameEvidence.getNameMatchModifierScore());
		}
	}
	
	/**
	 * We need to score first and middle names in conjunction with each other, because PubMed and Scopus combine them into a single field; also, it's often the case that first and middle names are conflated in identity systems of record.
		All of the matching that follows should be evaluated as a series of ifElse statements (once we get a match, we stop). The goal is to match as early on in the process as possible.
		It's conceivable that we can use the firstName from one alias in conjunction with the middleName of another to match to our article. We can use any combination of first and middle names to do the matching.
		Preprocessing: ignore/discard name variants in which it's pretty clear that one name variant has a middle name that is an abbreviation of another.

		Example: there are two name variants for ajdannen:
		"Andrew[firstName] + Jess[middleName] + Dannenberg[lastName]"
		"Andrew[firstName] + J[middleName] + Dannenberg[lastName]"
		In cases where one of the middle names is a left-anchored substring of the other, discard/ignore the shorter one.
	 * @param identityAuthor
	 * @param articleAuthorNames
	 * @param authorNameEvidence
	 */
	private void scoreFirstNameMiddleName(AuthorName identityAuthor, AuthorName identityAuthorNameOriginal, Map<ReCiterAuthor, ReCiterAuthor> articleAuthorNames, AuthorNameEvidence authorNameEvidence) {
		if(!articleAuthorNames.isEmpty()) {
			Map.Entry<ReCiterAuthor,ReCiterAuthor> entry = articleAuthorNames.entrySet().iterator().next();
			AuthorName articleAuthorName = entry.getValue().getAuthorName();
			AuthorName articleAuthorNameOriginal = entry.getKey().getAuthorName();
			if(identityAuthor.getFirstName() != null 
					&& 
					identityAuthor.getMiddleName() != null 
					&& 
					articleAuthorName.getFirstName() != null  
					&& 
					StringUtils.equalsIgnoreCase(identityAuthor.getFirstName() + identityAuthor.getMiddleName(), articleAuthorName.getFirstName())) {
				//Attempt match where identity.firstName + identity.middleName = article.firstName
				//Example: Paul (identity.firstName) + James (identity.middleName) = PaulJames (article.firstName)
				authorNameEvidence.setNameMatchFirstType("full-exact");
				authorNameEvidence.setNameMatchFirstScore(ReCiterArticleScorer.strategyParameters.getNameMatchFirstTypeFullExactScore());
				authorNameEvidence.setNameMatchMiddleType("full-exact");
				authorNameEvidence.setNameMatchMiddleScore(ReCiterArticleScorer.strategyParameters.getNameMatchMiddleTypeFullExactScore());
			} else if(identityAuthor.getFirstName() != null  
					&& 
					identityAuthor.getMiddleName() != null 
					&& 
					articleAuthorName.getFirstName() != null  
					&& 
					articleAuthorName.getFirstName().toLowerCase().matches(identityAuthor.getFirstName().toLowerCase() + "(.*)" + identityAuthor.getMiddleName().toLowerCase())) {
				//Attempt match where identity.firstName + "%" + identity.middleName = article.firstName
				//Example: Paul (identity.firstName) + James (identity.middleName) = PaulaJames (article.firstName)
				authorNameEvidence.setNameMatchFirstType("full-exact");
				authorNameEvidence.setNameMatchFirstScore(ReCiterArticleScorer.strategyParameters.getNameMatchFirstTypeFullExactScore());
				authorNameEvidence.setNameMatchMiddleType("full-exact");
				authorNameEvidence.setNameMatchMiddleScore(ReCiterArticleScorer.strategyParameters.getNameMatchMiddleTypeFullExactScore());
				authorNameEvidence.setNameMatchModifier("identitySubstringOfArticle-firstMiddleName");
				authorNameEvidence.setNameMatchModifierScore(ReCiterArticleScorer.strategyParameters.getNameMatchModifierIdentitySubstringOfArticleFirstMiddlenameScore());
			} else if(identityAuthor.getFirstName() != null  
					&& 
					identityAuthor.getMiddleName() != null 
					&& 
					articleAuthorName.getFirstName() != null  
					&& 
					StringUtils.equalsIgnoreCase(identityAuthor.getFirstName() + identityAuthor.getMiddleInitial(), articleAuthorName.getFirstName())) {
				//Attempt match where identity.firstName + identity.middleInitial = article.firstName
				//Example: Paul (identity.firstName) + J (identity.middleInitial) = PaulJ (article.firstName)
				authorNameEvidence.setNameMatchFirstType("full-exact");
				authorNameEvidence.setNameMatchFirstScore(ReCiterArticleScorer.strategyParameters.getNameMatchFirstTypeFullExactScore());
				authorNameEvidence.setNameMatchMiddleType("inferredInitials-exact");
				authorNameEvidence.setNameMatchMiddleScore(ReCiterArticleScorer.strategyParameters.getNameMatchMiddleTypeInferredInitialsExactScore());
			} else if(identityAuthor.getFirstName() != null  
					&& 
					identityAuthor.getMiddleName() != null 
					&& 
					articleAuthorName.getFirstName() != null  
					&& 
					articleAuthorName.getFirstName().toLowerCase().matches(identityAuthor.getFirstName().toLowerCase() + "(.*)" + identityAuthor.getMiddleInitial().toLowerCase())) {
				//Attempt match where identity.firstName + "%" + identity.middleInitial = article.firstName
				//Example: Paul (identity.firstName) + J (identity.middleInitial) = PaulaJ (article.firstName)
				authorNameEvidence.setNameMatchFirstType("full-exact");
				authorNameEvidence.setNameMatchFirstScore(ReCiterArticleScorer.strategyParameters.getNameMatchFirstTypeFullExactScore());
				authorNameEvidence.setNameMatchMiddleType("inferredInitials-exact");
				authorNameEvidence.setNameMatchMiddleScore(ReCiterArticleScorer.strategyParameters.getNameMatchMiddleTypeInferredInitialsExactScore());
				authorNameEvidence.setNameMatchModifier("identitySubstringOfArticle-firstMiddleName");
				authorNameEvidence.setNameMatchModifierScore(ReCiterArticleScorer.strategyParameters.getNameMatchModifierIdentitySubstringOfArticleFirstMiddlenameScore());
			} else if(identityAuthor.getFirstName() != null 
					&& 
					identityAuthor.getMiddleName() != null 
					&& 
					articleAuthorName.getFirstName() != null  
					&& 
					(StringUtils.equalsIgnoreCase(identityAuthor.getFirstInitial() + identityAuthor.getMiddleInitial(), articleAuthorName.getFirstName())
					||
					StringUtils.equalsIgnoreCase(identityAuthor.getFirstInitial() + " " + identityAuthor.getMiddleInitial(), articleAuthorName.getFirstName()))) //When inferring initials, also attempt match by adding space: identity.firstInitial + " " + identity.middleInitial = article.firstName
			{
				//Attempt match where identity.firstInitial + identity.middleInitial = article.firstName
				//Example: P (identity.firstInitial) + J (identity.middleInitial) = PJ (article.firstName)
				authorNameEvidence.setNameMatchFirstType("inferredInitials-exact");
				authorNameEvidence.setNameMatchFirstScore(ReCiterArticleScorer.strategyParameters.getNameMatchFirstTypeInferredInitialsExactScore());
				authorNameEvidence.setNameMatchMiddleType("inferredInitials-exact");
				authorNameEvidence.setNameMatchMiddleScore(ReCiterArticleScorer.strategyParameters.getNameMatchMiddleTypeInferredInitialsExactScore());
			} else if(identityAuthor.getFirstName() != null 
					&& 
					identityAuthor.getMiddleName() != null 
					&& 
					articleAuthorName.getFirstName() != null  
					&& 
					StringUtils.equalsIgnoreCase(identityAuthor.getFirstInitial() + identityAuthor.getMiddleName(), articleAuthorName.getFirstName())) {
				//Attempt match where identity.firstInitial + identity.middleName = article.firstName
				//Example: M (identity.firstInitial) + Carrington (identity.middleName) = MCarrington (article.firstName)
				authorNameEvidence.setNameMatchFirstType("inferredInitials-exact");
				authorNameEvidence.setNameMatchFirstScore(ReCiterArticleScorer.strategyParameters.getNameMatchFirstTypeInferredInitialsExactScore());
				authorNameEvidence.setNameMatchMiddleType("full-exact");
				authorNameEvidence.setNameMatchMiddleScore(ReCiterArticleScorer.strategyParameters.getNameMatchMiddleTypeFullExactScore());
			} else if(identityAuthor.getFirstName() != null 
					&& 
					identityAuthor.getLastName() != null 
					&& 
					articleAuthorName.getFirstName() != null
					&&
					articleAuthorName.getLastName() != null
					&& 
					StringUtils.equalsIgnoreCase((identityAuthor.getFirstName() + identityAuthor.getLastName()).replaceAll("[\\s-]+", ""), (articleAuthorName.getFirstName() + articleAuthorName.getLastName()).replaceAll("[\\s-]+", ""))) {
				//Attempt match where identity.firstName + identity.lastName = article.firstName + article.lastName
				//Example: Landys (identity.firstName) + Lopez quezada (identity.lastName) = Landys Lopez (article.firstName) + Quezada (article.lastName)
				authorNameEvidence.setNameMatchFirstType("full-exact");
				authorNameEvidence.setNameMatchFirstScore(ReCiterArticleScorer.strategyParameters.getNameMatchFirstTypeFullExactScore());
				authorNameEvidence.setNameMatchLastType("full-exact");
				authorNameEvidence.setNameMatchLastScore(ReCiterArticleScorer.strategyParameters.getNameMatchLastTypeFullExactScore());
				authorNameEvidence.setNameMatchModifier("combinedFirstNameLastName");
				authorNameEvidence.setNameMatchModifierScore(ReCiterArticleScorer.strategyParameters.getNameMatchModifierCombinedFirstNameLastNameScore());

			} else if(identityAuthor.getFirstName() != null 
					&& 
					identityAuthor.getMiddleName() != null 
					&& 
					articleAuthorName.getFirstName() != null  
					&& 
					articleAuthorName.getFirstName().toLowerCase().matches(identityAuthor.getFirstName().toLowerCase() + identityAuthor.getMiddleName().toLowerCase() + "(.*)")) {
				//Attempt match where identity.firstName + identity.middleName + "%" = article.firstName
				//Example: Paul (identity.firstName) + James (identity.middleName) = PaulJamesA (article.firstName)
				authorNameEvidence.setNameMatchFirstType("full-exact");
				authorNameEvidence.setNameMatchFirstScore(ReCiterArticleScorer.strategyParameters.getNameMatchFirstTypeFullExactScore());
				authorNameEvidence.setNameMatchMiddleType("full-exact");
				authorNameEvidence.setNameMatchMiddleScore(ReCiterArticleScorer.strategyParameters.getNameMatchMiddleTypeFullExactScore());
				authorNameEvidence.setNameMatchModifier("identitySubstringOfArticle-firstMiddleName");
				authorNameEvidence.setNameMatchModifierScore(ReCiterArticleScorer.strategyParameters.getNameMatchModifierIdentitySubstringOfArticleFirstMiddlenameScore());
			} else if(identityAuthor.getFirstName() != null 
					&& 
					identityAuthor.getMiddleName() != null 
					&& 
					articleAuthorName.getFirstName() != null  
					&& 
					articleAuthorName.getFirstName().toLowerCase().matches(identityAuthor.getFirstName().toLowerCase() + identityAuthor.getMiddleInitial().toLowerCase() + "(.*)")) {
				//Attempt match where identity.firstName + identity.middleInitial + "%" = article.firstName
				//Example: Paul (identity.firstName) + J (identity.middleInitial) = PaulJZ (article.firstName)
				authorNameEvidence.setNameMatchFirstType("full-exact");
				authorNameEvidence.setNameMatchFirstScore(ReCiterArticleScorer.strategyParameters.getNameMatchFirstTypeFullExactScore());
				authorNameEvidence.setNameMatchMiddleType("inferredInitials-exact");
				authorNameEvidence.setNameMatchMiddleScore(ReCiterArticleScorer.strategyParameters.getNameMatchMiddleTypeInferredInitialsExactScore());
				authorNameEvidence.setNameMatchModifier("identitySubstringOfArticle-firstMiddleName");
				authorNameEvidence.setNameMatchModifierScore(ReCiterArticleScorer.strategyParameters.getNameMatchModifierIdentitySubstringOfArticleFirstMiddlenameScore());
			} else if(identityAuthor.getFirstName() != null 
					&& 
					articleAuthorName.getFirstName() != null  
					&& 
					StringUtils.equalsIgnoreCase(identityAuthor.getFirstName(), articleAuthorName.getFirstName())) {
				//Attempt match where identity.firstName = article.firstName
				//Example: Paul (identity.firstName) = Paul (article.firstName)
				authorNameEvidence.setNameMatchFirstType("full-exact");
				authorNameEvidence.setNameMatchFirstScore(ReCiterArticleScorer.strategyParameters.getNameMatchFirstTypeFullExactScore());
				authorNameEvidence.setNameMatchMiddleType("noMatch");
				authorNameEvidence.setNameMatchMiddleScore(ReCiterArticleScorer.strategyParameters.getNameMatchMiddleTypeNoMatchScore());
			} else if(identityAuthor.getFirstName() != null 
					&& 
					identityAuthor.getMiddleName() != null 
					&& 
					articleAuthorName.getFirstName() != null  
					&& 
					StringUtils.equalsIgnoreCase(identityAuthor.getMiddleInitial() + identityAuthor.getFirstInitial(), articleAuthorName.getFirstName())) {
				authorNameEvidence.setNameMatchFirstType("inferredInitials-exact");
				authorNameEvidence.setNameMatchFirstScore(ReCiterArticleScorer.strategyParameters.getNameMatchFirstTypeInferredInitialsExactScore());
				authorNameEvidence.setNameMatchMiddleType("inferredInitials-exact");
				authorNameEvidence.setNameMatchMiddleScore(ReCiterArticleScorer.strategyParameters.getNameMatchMiddleTypeInferredInitialsExactScore());
				authorNameEvidence.setNameMatchModifier("incorrectOrder");
				authorNameEvidence.setNameMatchModifierScore(ReCiterArticleScorer.strategyParameters.getNameMatchModifierIncorrectOrderScore());
			} else if(identityAuthor.getFirstName() != null 
					&& 
					identityAuthor.getMiddleName() != null 
					&& 
					articleAuthorName.getFirstName() != null  
					&& 
					(identityAuthor.getFirstName().codePoints().filter(c-> c>='A' && c<='Z').count() > 1 || identityAuthor.getMiddleName().codePoints().filter(c-> c>='A' && c<='Z').count() > 1) //check if there is more than 1 capital letters
					&& 
					StringUtils.equalsIgnoreCase(identityAuthor.getFirstName().chars().filter(Character::isUpperCase)
	                           .mapToObj(c -> Character.toString((char)c))
	                           .collect(Collectors.joining()) + 
	                           identityAuthor.getMiddleName().chars().filter(Character::isUpperCase)
	                           .mapToObj(c -> Character.toString((char)c))
	                           .collect(Collectors.joining()), 
							articleAuthorName.getFirstName())) {
				//If there's more than one capital letter in identity.firstName or identity.middleName, attempt match where any capitals in identity.firstName + any capital letters in identity.middleName = article.firstName
				//Example: KS (identity.initialsInFirstName) + C (identity.initialsInMiddleName) = KSC (article.firstName)
				authorNameEvidence.setNameMatchFirstType("inferredInitials-exact");
				authorNameEvidence.setNameMatchFirstScore(ReCiterArticleScorer.strategyParameters.getNameMatchFirstTypeInferredInitialsExactScore());
				authorNameEvidence.setNameMatchMiddleType("inferredInitials-exact");
				authorNameEvidence.setNameMatchMiddleScore(ReCiterArticleScorer.strategyParameters.getNameMatchMiddleTypeInferredInitialsExactScore());
			} else if(identityAuthor.getFirstName() != null 
					&& 
					articleAuthorName.getFirstName() != null  
					&& 
					identityAuthor.getFirstName().codePoints().filter(c-> c>='A' && c<='Z').count() > 1 //check if there is more than 1 capital letters
					&& 
					StringUtils.equalsIgnoreCase(identityAuthor.getFirstName().chars().filter(Character::isUpperCase)
	                           .mapToObj(c -> Character.toString((char)c))
	                           .collect(Collectors.joining()) , 
							articleAuthorName.getFirstName())) {
				//If there's more than one capital letter in identity.firstName, attempt match where any capitals in identity.firstName = article.firstName
				//Example: KS (identity.initialsInFirstName) = KS (article.firstName)
				authorNameEvidence.setNameMatchFirstType("inferredInitials-exact");
				authorNameEvidence.setNameMatchFirstScore(ReCiterArticleScorer.strategyParameters.getNameMatchFirstTypeInferredInitialsExactScore());
				authorNameEvidence.setNameMatchMiddleType("noMatch");
				authorNameEvidence.setNameMatchMiddleScore(ReCiterArticleScorer.strategyParameters.getNameMatchMiddleTypeNoMatchScore());
			} else if(identityAuthor.getFirstName() != null 
					&& 
					identityAuthor.getMiddleName() != null 
					&& 
					articleAuthorName.getFirstName() != null  
					&& 
					identityAuthor.getFirstName().codePoints().filter(c-> c>='A' && c<='Z').count() > 1 && //check if there is more than 1 capital letters
					StringUtils.equalsIgnoreCase(identityAuthor.getFirstName().chars().filter(Character::isUpperCase)
	                           .mapToObj(c -> Character.toString((char)c))
	                           .collect(Collectors.joining()) + identityAuthor.getMiddleName() , 
							articleAuthorName.getFirstName())) {
				//If there's more than one capital letter in identity.firstName, attempt match where any capitals in identity.firstName + identity.middleName = article.firstName
				//Example: KS (identity.initialsInFirstName) + Clifford (identity.middleName) = KSClifford (article.firstName)
				authorNameEvidence.setNameMatchFirstType("inferredInitials-exact");
				authorNameEvidence.setNameMatchFirstScore(ReCiterArticleScorer.strategyParameters.getNameMatchFirstTypeInferredInitialsExactScore());
				authorNameEvidence.setNameMatchMiddleType("full-exact");
				authorNameEvidence.setNameMatchMiddleScore(ReCiterArticleScorer.strategyParameters.getNameMatchMiddleTypeFullExactScore());
			} else if(identityAuthor.getFirstName() != null 
					&& 
					articleAuthorName.getFirstName() != null  
					&& 
					articleAuthorName.getFirstName().toLowerCase().matches(identityAuthor.getFirstName().toLowerCase() + "(.*)")) {
				//Attempt match where identity.firstName + "%" = article.firstName
				//Example: Robert (identity.firstName) = RobertR (article.firstName)
				//Same over-claim as the substring branch in scoreFirstNameMiddleNameNull:
				//this is a prefix, not an exact match. See issue #746.
				assignFirstNamePrefixEvidence(identityAuthor, authorNameEvidence);
				authorNameEvidence.setNameMatchMiddleType("noMatch");
				authorNameEvidence.setNameMatchMiddleScore(ReCiterArticleScorer.strategyParameters.getNameMatchMiddleTypeNoMatchScore());
				authorNameEvidence.setNameMatchModifier("identitySubstringOfArticle-firstName");
				authorNameEvidence.setNameMatchModifierScore(ReCiterArticleScorer.strategyParameters.getNameMatchModifierIdentitySubstringOfArticleFirstnameScore());
			} else if(identityAuthor.getFirstName() != null
					&&
					articleAuthorName.getFirstName() != null
					&&
					articleAuthorName.getFirstName().toLowerCase().matches("(.*)" + identityAuthor.getFirstName().toLowerCase())) {
				//Attempt match where "%" + identity.firstName = article.firstName
				//Example: Cary (identity.firstName) = MCary (article.firstName)
				//A right-anchored suffix is not an exact match either. See issue #746.
				//Unlike the prefix direction this bucket is NOT split by identity name
				//length: a trailing single letter is a coincidence, not an inferred
				//initial, so there is no initials bucket it could honestly move to.
				authorNameEvidence.setNameMatchFirstType("full-suffix");
				authorNameEvidence.setNameMatchFirstScore(ReCiterArticleScorer.strategyParameters.getNameMatchFirstTypeFullSuffixScore());
				authorNameEvidence.setNameMatchMiddleType("noMatch");
				authorNameEvidence.setNameMatchMiddleScore(ReCiterArticleScorer.strategyParameters.getNameMatchMiddleTypeNoMatchScore());
				authorNameEvidence.setNameMatchModifier("identitySubstringOfArticle-firstName");
				authorNameEvidence.setNameMatchModifierScore(ReCiterArticleScorer.strategyParameters.getNameMatchModifierIdentitySubstringOfArticleFirstnameScore());
			} else if(identityAuthor.getFirstName() != null
					&& 
					identityAuthor.getMiddleName() != null 
					&& 
					articleAuthorName.getFirstName() != null  
					&& 
					StringUtils.equalsIgnoreCase(identityAuthor.getMiddleName(), articleAuthorName.getFirstName())) {
				//Attempt match where identity.middleName = article.firstName
				//Example: Clifford (identity.middleName) = Clifford (article.firstName)
				authorNameEvidence.setNameMatchFirstType("noMatch");
				authorNameEvidence.setNameMatchFirstScore(ReCiterArticleScorer.strategyParameters.getNameMatchFirstTypeNoMatchScore());
				authorNameEvidence.setNameMatchMiddleType("full-exact");
				authorNameEvidence.setNameMatchMiddleScore(ReCiterArticleScorer.strategyParameters.getNameMatchMiddleTypeFullExactScore());
			} else if(identityAuthor.getFirstName() != null 
					&& 
					identityAuthor.getMiddleName() != null 
					&& 
					articleAuthorName.getFirstName() != null  
					&& 
					articleAuthorName.getFirstName().toLowerCase().matches(identityAuthor.getMiddleName().toLowerCase() + "(.*)")) {
				//Attempt match where identity.middleName + "%" = article.firstName
				//Example: Clifford (identity.middleName) = CliffordKS (article.firstName)
				authorNameEvidence.setNameMatchFirstType("noMatch");
				authorNameEvidence.setNameMatchFirstScore(ReCiterArticleScorer.strategyParameters.getNameMatchFirstTypeNoMatchScore());
				authorNameEvidence.setNameMatchMiddleType("full-exact");
				authorNameEvidence.setNameMatchMiddleScore(ReCiterArticleScorer.strategyParameters.getNameMatchMiddleTypeFullExactScore());
				authorNameEvidence.setNameMatchModifier("identitySubstringOfArticle-middleName");
				authorNameEvidence.setNameMatchModifierScore(ReCiterArticleScorer.strategyParameters.getNameMatchModifierIdentitySubstringOfArticleMiddlenameScore());
			} else if(identityAuthor.getFirstName() != null 
					&& 
					identityAuthor.getMiddleName() != null 
					&& 
					articleAuthorName.getFirstName() != null  
					&& 
					articleAuthorName.getFirstName().toLowerCase().matches("(.*)" + identityAuthor.getMiddleName().toLowerCase())) {
				//Attempt match where "%" + identity.middleName = article.firstName
				//Example: Clifford (identity.middleName) = KunSungClifford (article.firstName)
				authorNameEvidence.setNameMatchFirstType("noMatch");
				authorNameEvidence.setNameMatchFirstScore(ReCiterArticleScorer.strategyParameters.getNameMatchFirstTypeNoMatchScore());
				authorNameEvidence.setNameMatchMiddleType("full-exact");
				authorNameEvidence.setNameMatchMiddleScore(ReCiterArticleScorer.strategyParameters.getNameMatchMiddleTypeFullExactScore());
				authorNameEvidence.setNameMatchModifier("identitySubstringOfArticle-middleName");
				authorNameEvidence.setNameMatchModifierScore(ReCiterArticleScorer.strategyParameters.getNameMatchModifierIdentitySubstringOfArticleMiddlenameScore());
			} else if(identityAuthor.getFirstName() != null 
					&& 
					identityAuthor.getMiddleName() != null 
					&& 
					articleAuthorName.getFirstName() != null  
					&& 
					ReCiterStringUtil.levenshteinDistance(identityAuthor.getFirstName() + identityAuthor.getMiddleName(), articleAuthorName.getFirstName()) <= 2) {
				//Attempt match where levenshteinDistance between identity.firstName + identity.middleName and article.firstName is <=2.
				//Example: Manney (identity.firstName) + Carrington (identity.middleName) = MannyCarrington (article.firstName)
				authorNameEvidence.setNameMatchFirstType("full-fuzzy");
				authorNameEvidence.setNameMatchFirstScore(ReCiterArticleScorer.strategyParameters.getNameMatchFirstTypeFullFuzzyScore());
				authorNameEvidence.setNameMatchMiddleType("full-fuzzy");
				authorNameEvidence.setNameMatchMiddleScore(ReCiterArticleScorer.strategyParameters.getNameMatchMiddleTypeFullFuzzyScore());
			} else if(identityAuthor.getFirstName() != null 
					&& 
					articleAuthorName.getFirstName() != null  
					&&
					identityAuthor.getFirstName().length() >= 4 
					&& 
					ReCiterStringUtil.levenshteinDistance(identityAuthor.getFirstName(), articleAuthorName.getFirstName()) <= 1) {
				//Attempt match where identity.firstName >= 4 characters and levenshteinDistance between identity.firstName and article.firstName is <=1.
				//Example: Nassar (identity.firstName) = Nasser (article.firstName)
				authorNameEvidence.setNameMatchFirstType("full-fuzzy");
				authorNameEvidence.setNameMatchFirstScore(ReCiterArticleScorer.strategyParameters.getNameMatchFirstTypeFullFuzzyScore());
				authorNameEvidence.setNameMatchMiddleType("noMatch");
				authorNameEvidence.setNameMatchMiddleScore(ReCiterArticleScorer.strategyParameters.getNameMatchMiddleTypeNoMatchScore());
			} else if(identityAuthor.getFirstName() != null 
					&& 
					articleAuthorName.getFirstName() != null  
					&&
					identityAuthor.getFirstName().length() >=3 
					&& 
					articleAuthorName.getFirstName().length() >=3 
					&&
					StringUtils.equalsIgnoreCase(identityAuthor.getFirstName().substring(0, 3), articleAuthorName.getFirstName().substring(0, 3))) {
				//Attempt match where first three characters of identity.firstName = first three characters of identity.firstName.
				//Example: Massimiliano (identity.firstName) = Massimo (article.firstName)
				authorNameEvidence.setNameMatchFirstType("full-fuzzy");
				authorNameEvidence.setNameMatchFirstScore(ReCiterArticleScorer.strategyParameters.getNameMatchFirstTypeFullFuzzyScore());
				authorNameEvidence.setNameMatchMiddleType("noMatch");
				authorNameEvidence.setNameMatchMiddleScore(ReCiterArticleScorer.strategyParameters.getNameMatchMiddleTypeNoMatchScore());
			} else if(identityAuthor.getFirstName() != null 
					&& 
					identityAuthor.getMiddleName() != null 
					&& 
					articleAuthorName.getFirstName() != null  
					&& 
					articleAuthorName.getFirstName().toLowerCase().matches(identityAuthor.getFirstInitial().toLowerCase() + "(.*)" + identityAuthor.getMiddleName().toLowerCase())) {
				//Attempt match where identity.firstInitial + "%" + identity.middleName = article.firstName
				//Example: M (identity.firstInitial) + Carrington (identity.middleName) = MannyCarrington (article.firstName)
				authorNameEvidence.setNameMatchFirstType("inferredInitials-exact");
				authorNameEvidence.setNameMatchFirstScore(ReCiterArticleScorer.strategyParameters.getNameMatchFirstTypeInferredInitialsExactScore());
				authorNameEvidence.setNameMatchMiddleType("full-exact");
				authorNameEvidence.setNameMatchMiddleScore(ReCiterArticleScorer.strategyParameters.getNameMatchMiddleTypeFullExactScore());
				authorNameEvidence.setNameMatchModifier("identitySubstringOfArticle-firstMiddleName");
				authorNameEvidence.setNameMatchModifierScore(ReCiterArticleScorer.strategyParameters.getNameMatchModifierIdentitySubstringOfArticleFirstMiddlenameScore());
			} else if(identityAuthor.getFirstName() != null 
					&& 
					identityAuthor.getMiddleName() != null 
					&& 
					articleAuthorName.getFirstName() != null  
					&& 
					StringUtils.equalsIgnoreCase(identityAuthor.getMiddleName() + identityAuthor.getFirstInitial(), articleAuthorName.getFirstName())) {
				//Attempt match where identity.middleName + identity.firstInitial = article.firstName
				//Example: Carrington (identity.middleName) + M (identity.firstInitial) = CarringtonM (article.firstName)
				authorNameEvidence.setNameMatchFirstType("inferredInitials-exact");
				authorNameEvidence.setNameMatchFirstScore(ReCiterArticleScorer.strategyParameters.getNameMatchFirstTypeInferredInitialsExactScore());
				authorNameEvidence.setNameMatchMiddleType("full-exact");
				authorNameEvidence.setNameMatchMiddleScore(ReCiterArticleScorer.strategyParameters.getNameMatchMiddleTypeFullExactScore());
				authorNameEvidence.setNameMatchModifier("incorrectOrder");
				authorNameEvidence.setNameMatchModifierScore(ReCiterArticleScorer.strategyParameters.getNameMatchModifierIncorrectOrderScore());
			} else if(identityAuthor.getFirstName() != null 
					&& 
					articleAuthorName.getFirstName() != null  
					&&
					articleAuthorName.getFirstName().length() == 1 
					&&
					StringUtils.equalsIgnoreCase(identityAuthor.getFirstInitial(), articleAuthorName.getFirstName())) {
				//Attempt match where article.firstName is only one character and identity.firstName = first character of article.firstName.
				//Example: Jessica (identity.firstName) = J (article.firstName)
				authorNameEvidence.setNameMatchFirstType("inferredInitials-exact");  
				authorNameEvidence.setNameMatchFirstScore(ReCiterArticleScorer.strategyParameters.getNameMatchFirstTypeInferredInitialsExactScore());
				authorNameEvidence.setNameMatchMiddleType("noMatch");
				authorNameEvidence.setNameMatchMiddleScore(ReCiterArticleScorer.strategyParameters.getNameMatchMiddleTypeNoMatchScore());
			} else if(identityAuthor.getFirstName() != null 
					&& 
					articleAuthorName.getFirstName() != null
					&&
					identityAuthor.getFirstName().length() > 0 
					&&
					articleAuthorName.getFirstName().length() > 0
					&&
					StringUtils.equalsIgnoreCase(identityAuthor.getFirstName().substring(0, 1), articleAuthorName.getFirstName().substring(0, 1))) {
				//Attempt match where first character of identity.firstName = first character of identity.firstName.
				//Example: Jessica (identity.firstName) = Jochen (article.firstName)
				authorNameEvidence.setNameMatchFirstType("full-conflictingAllButInitials");
				authorNameEvidence.setNameMatchFirstScore(ReCiterArticleScorer.strategyParameters.getNameMatchFirstTypeFullConflictingAllButInitialsScore());
				authorNameEvidence.setNameMatchMiddleType("noMatch");
				authorNameEvidence.setNameMatchMiddleScore(ReCiterArticleScorer.strategyParameters.getNameMatchMiddleTypeNoMatchScore());
			} else {
				//Else, we have no match of any kind.
				//Example: Pascale vs. Curtis
				authorNameEvidence.setNameMatchFirstType("full-conflictingEntirely");
				authorNameEvidence.setNameMatchFirstScore(ReCiterArticleScorer.strategyParameters.getNameMatchFirstTypeFullConflictingEntirelyScore());
				authorNameEvidence.setNameMatchMiddleType("full-conflictingEntirely");
				authorNameEvidence.setNameMatchMiddleScore(ReCiterArticleScorer.strategyParameters.getNameMatchMiddleTypeFullConflictingEntirelyScore());
			}
			authorNameEvidence.setInstitutionalAuthorName(identityAuthorNameOriginal);
			authorNameEvidence.setArticleAuthorName(articleAuthorNameOriginal);
			authorNameEvidence.setNameScoreTotal(authorNameEvidence.getNameMatchFirstScore() + authorNameEvidence.getNameMatchMiddleScore() + authorNameEvidence.getNameMatchLastScore() + authorNameEvidence.getNameMatchModifierScore());
		}
	}
	
	private int getTargetAuthorCount(ReCiterArticle reCiterArticle) {
		int targetAuthorCount = 0;
		
		for(ReCiterAuthor targetAuthorName: reCiterArticle.getArticleCoAuthors().getAuthors()) {
			if(targetAuthorName.isTargetAuthor()) {
				targetAuthorCount++;
			}
		}
		return targetAuthorCount;
	}
	
	/**
	 * @author szd2013
	 * 	Preprocess identity.firstName, identity.middleName, and article.firstName
		If any names are in quotes or parentheses in identity.firstName or identity.middleName, pull them out so they can be matched against.
		Wing Tak "Jack" --> add "Jack" to identity.firstName, add "Wing Tak" to identity.firstName
		Qihui (Jim) --> add Jim to identity.firstName, add Qihui to identity.firstName
		Remove any periods, spaces, or dashes from both identity.firstName, identity.middleName, and article.firstName. For example:
		"Chi-chao" --> "Chichao"
		"Minh-Nhut Yvonne" --> "MinhNhutYvonne"
		"Eliot A." --> "EliotA"
		Disregard any cases where one variant of identity.firstName is a substring of another case of identity.firstName. Disregard any cases where one variant of identity.middleName is a substring of another case of identity.middleName. For example:
		"Cary" (keep) vs "C" (disregard)
		Null for middle name should only be included as possible value if none of the names or aliases contain a middle name
		A given target author might have N different first names and M different middle names.
		Go to D
		Retrieve article.lastName where targetAuthor=TRUE and all distinct cases of identity.lastName for our target author from identity. Preprocess identity.lastName and article.lastName.
		
		Remove any periods from both identity.lastName and article.lastName
		Remove any of the following endings from identity.lastName:
		", Jr"
		", MD PhD"
		", MD-PhD"
		", PhD"
		", MD"
		", III"
		", II"
		", Sr"
		" Jr"
		" MD PhD"
		" MD-PhD"
		" PhD"
		" MD"
		" III"
		" II"
		" Sr"
		Remove any periods, spaces, dashes, or quotes.
		For example:
		"Del Cole" --> "Delcole"
		"Garcia-Marquez" --> ""GarciaMarquez"
		"Capetillo Gonzalez de Zarate" --> "CapetilloGonzalezdeZarate"
	 *
	 */
	private void sanitizeIdentityAuthorNames(Identity identity, List<AuthorName> sanitizedIdentityAuthorName) {
		AuthorName identityPrimaryName = new AuthorName();
		String suffixRegex = generateSuffixRegex();
		AuthorName additionalName = new AuthorName();
		String firstName = null;
		String lastName = null;
		String middleName = null;
		//Sanitize Identity Names
		if(identity.getPrimaryName() != null) {
			
			if(identity.getPrimaryName().getFirstName() != null) {
				if(identity.getPrimaryName().getFirstName().contains("\"") || (identity.getPrimaryName().getFirstName().contains("(") && identity.getPrimaryName().getFirstName().contains(")"))) {
					firstName = identity.getPrimaryName().getFirstName().replaceAll("[-.,()\\s]", "").replaceAll("\"([^\"]*)\"|(\"([^\"]*)\")|(([a-z]*))/i/g", "");
					if(firstName !=null) {
						additionalName.setFirstName(firstName);
					}
					Pattern pattern = Pattern.compile("\"([^\"]*)\"|(\"([^\"]*)\")|(([a-z]*))/i/g");
					Matcher matcher = pattern.matcher(identity.getPrimaryName().getFirstName());
					while(matcher.find()) {
						identityPrimaryName.setFirstName(matcher.group().replaceAll("\"", ""));
					}
				} else {
					identityPrimaryName.setFirstName(identity.getPrimaryName().getFirstName().replaceAll("[-.,()\\s]", ""));
				}
			}
			if(identity.getPrimaryName().getMiddleName() != null) {
				if(identity.getPrimaryName().getMiddleName().contains("\"") || (identity.getPrimaryName().getMiddleName().contains("(") && identity.getPrimaryName().getMiddleName().contains(")"))) {
					middleName = identity.getPrimaryName().getMiddleName().replaceAll("[-.,()\\s]", "").replaceAll("\"([^\"]*)\"|(\"([^\"]*)\")|(([a-z]*))/i/g", "");
					if(middleName !=null) {
						additionalName.setMiddleName(middleName);
					}
					Pattern pattern = Pattern.compile("\"([^\"]*)\"|(\"([^\"]*)\")|(([a-z]*))/i/g");
					Matcher matcher = pattern.matcher(identity.getPrimaryName().getMiddleName());
					while(matcher.find()) {
						identityPrimaryName.setMiddleName(matcher.group().replaceAll("\"", ""));
					}
				} else {
					identityPrimaryName.setMiddleName(identity.getPrimaryName().getMiddleName().replaceAll("[-.,()\\s]", ""));
				}
			}
			if(identity.getPrimaryName().getLastName() != null) {
				//lastName = identity.getPrimaryName().getLastName().replaceAll("[-.,,()\\s]|(,Jr|, Jr|, MD PhD|,MD PhD|, MD-PhD|,MD-PhD|, PhD|,PhD|, MD|,MD|, III|,III|, II|,II|, Sr|,Sr|Jr|MD PhD|MD-PhD|PhD|MD|III|II|Sr)$", "");
				lastName = identity.getPrimaryName().getLastName().replaceAll("[-.,,()\\s]|(" + suffixRegex + ")$", "");
				identityPrimaryName.setLastName(lastName);
				if(additionalName.getFirstName() != null) {
					additionalName.setLastName(lastName);
				}
			}
			if(identityPrimaryName.getLastName() != null) {
				sanitizedIdentityAuthorName.add(identityPrimaryName);
			}
		}
		
		if(identity.getAlternateNames() != null && identity.getAlternateNames().size() > 0) {
			for(AuthorName aliasAuthorName: identity.getAlternateNames()) {
				AuthorName identityAliasAuthorName = new AuthorName();
				if(aliasAuthorName.getFirstName() != null) {
					identityAliasAuthorName.setFirstName(aliasAuthorName.getFirstName().replaceAll("[-.\",()\\s]", ""));
				}
				if(aliasAuthorName.getMiddleName() != null) {
					identityAliasAuthorName.setMiddleName(aliasAuthorName.getMiddleName().replaceAll("[-.\",()\\s]", ""));
				}
				if(aliasAuthorName.getLastName() != null) {
					identityAliasAuthorName.setLastName(aliasAuthorName.getLastName().replaceAll("[-.\",()\\s]|(" + suffixRegex + ")$", ""));
				}
				
				if(identityAliasAuthorName.getLastName() != null) {
					sanitizedIdentityAuthorName.add(identityAliasAuthorName);
				}
			}
		}
		if(additionalName != null && additionalName.getLastName() != null) {
			sanitizedIdentityAuthorName.add(additionalName);
		}
	}
	
	private void sanitizeTargetAuthorNames(ReCiterArticle reCiterArticle, Set<AuthorName> sanitizedAuthorName) {
		//Sanitize targetAuthorName
		for(ReCiterAuthor targetAuthorName: reCiterArticle.getArticleCoAuthors().getAuthors()) {
			if(targetAuthorName.isTargetAuthor()) {
				AuthorName targetAuthor = new AuthorName();
				if(targetAuthorName.getAuthorName().getFirstName() != null) {
					targetAuthor.setFirstName(targetAuthorName.getAuthorName().getFirstName().replaceAll("[-.\"() ]", ""));
				}
				if(targetAuthorName.getAuthorName().getLastName() != null) {
					targetAuthor.setLastName(targetAuthorName.getAuthorName().getLastName().replaceAll("[-.\",()\\s]|(,Jr|, Jr|, MD PhD|,MD PhD|, MD-PhD|,MD-PhD|, PhD|,PhD|, MD|,MD|, III|,III|, II|,II|, Sr|,Sr|Jr|MD PhD|MD-PhD|PhD|MD|III|II|Sr)$", ""));
				}
				sanitizedAuthorName.add(targetAuthor);
			}
		}
	}
	
	/**
	 * This function check if middle name is null in all variants of identity name. If there is null return false
	 */
	private boolean isNotNullIdentityMiddleName(Collection<AuthorName> idenityAuthorName) {
		boolean middleNameNull = false;
		for(AuthorName authorName: idenityAuthorName) {
			if(authorName.getMiddleName() != null) {
				middleNameNull = true;
				return middleNameNull;
			}
		}
		return middleNameNull;
		
	}
	
	private void checkToIgnoreNameVariants(List<AuthorName> idenityAuthorNames) {
		for(int i= 0 ; i < idenityAuthorNames.size() ; i++) {
			for(int j = 0; j < idenityAuthorNames.size(); j++) {
				if(i==j) {
					continue;
				}
				if(idenityAuthorNames.size() > 1 
						&&
						idenityAuthorNames.size() -1 >= i
						&&
						idenityAuthorNames.size() - 1 >= j
						&&
						idenityAuthorNames.get(i) != null && idenityAuthorNames.get(j) != null
						&&
						idenityAuthorNames.get(i).getFirstName() != null
						&&
						idenityAuthorNames.get(i).getLastName() != null
						&&
						idenityAuthorNames.get(j).getFirstName() != null
						&&
						idenityAuthorNames.get(j).getLastName() != null) {
					if(StringUtils.equalsIgnoreCase(idenityAuthorNames.get(i).getLastName(), idenityAuthorNames.get(j).getLastName()) && 
							idenityAuthorNames.get(i).getFirstName().startsWith(idenityAuthorNames.get(j).getFirstName())) {
						if(idenityAuthorNames.get(j).getMiddleName() == null) {
							idenityAuthorNames.remove(j);
						}
					}
					//Case - ajdannen - Throw away Andrew J Dannenberg because Andrew Jess Dannenberg exists
					if(idenityAuthorNames.size() > 1 
							&&
							idenityAuthorNames.size() -1 >= i
							&&
							idenityAuthorNames.size() - 1 >= j) {
						if(idenityAuthorNames.get(i) != null
								&&
								idenityAuthorNames.get(j) != null
								&&
								idenityAuthorNames.get(i).getLastName() != null 
								&&
								idenityAuthorNames.get(j).getLastName() != null 
								&&
								idenityAuthorNames.get(i).getFirstName() != null 
								&&
								idenityAuthorNames.get(j).getFirstName() != null
								&&
								idenityAuthorNames.get(i).getMiddleName() != null 
								&&
								idenityAuthorNames.get(j).getMiddleName() != null
								&&
								StringUtils.equalsIgnoreCase(idenityAuthorNames.get(i).getLastName(), idenityAuthorNames.get(j).getLastName()) 
								&& 
								StringUtils.equalsIgnoreCase(idenityAuthorNames.get(i).getFirstName(), idenityAuthorNames.get(j).getFirstName()) 
								&&
								idenityAuthorNames.get(i).getMiddleName().startsWith(idenityAuthorNames.get(j).getMiddleName())) {
								idenityAuthorNames.remove(j);
						}
					}
				}
			}
				
		}
	}
	
	/**
	 * This function generates regex of suffix from application.properties file
	 * @return regex string
	 */
	private String generateSuffixRegex() {
		String suffixRegex = "";
		String suffixTogether = "";
		for(String suffix: this.nameExcludedSuffixes) {
			suffixRegex = suffixRegex + "," + suffix + "|, " + suffix + "|";
			suffixTogether = suffixTogether + suffix + "|";
		}
		if(suffixTogether.endsWith("|")) {
			suffixTogether = suffixTogether.substring(0, suffixTogether.length() - 1);
		}
		return suffixRegex + "," + suffixTogether;
	}
		
	/**
	 * This function compares all the AuthorNameEvidences and returns the highest AuthorNameEvidence total score
	 * @param authorNameEvidences
	 * @return
	 */
	private AuthorNameEvidence calculateHighestScore(List<AuthorNameEvidence> authorNameEvidences) {
		return authorNameEvidences.stream().max(Comparator.comparing(AuthorNameEvidence::getNameScoreTotal)).orElseThrow(NoSuchElementException::new);
	}

	@Override
	public double executeStrategy(ReCiterArticle reCiterArticle, Identity identity) {
		return 0;
	}

	@Override
	public void populateFeature(ReCiterArticle reCiterArticle, Identity identity, Feature feature) {
		// TODO Auto-generated method stub
		
	}

}

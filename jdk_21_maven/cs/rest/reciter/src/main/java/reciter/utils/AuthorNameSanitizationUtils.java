package reciter.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import lombok.NoArgsConstructor;
import reciter.engine.StrategyParameters;
import reciter.model.article.ReCiterArticle;
import reciter.model.article.ReCiterAuthor;
import reciter.model.identity.AuthorName;
import reciter.model.identity.Identity;

/**
 * @author szd2013
 * <p>This class sanitizes author names for both Identity and Article.
 *  Preprocess identity.firstName, identity.middleName, and article.firstName
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
	"Capetillo Gonzalez de Zarate" --> "CapetilloGonzalezdeZarate"<p>
 *
 */
@NoArgsConstructor
public class AuthorNameSanitizationUtils {
	
	private StrategyParameters strategyParameters; 
	
	private List<String> nameExcludedSuffixes;
	
	public AuthorNameSanitizationUtils(StrategyParameters strategyParameters) {
		this.strategyParameters = strategyParameters;
	}
	
	/**
	 * @author szd2013
	 * @param reCiterArticle
	 * @return 
	 * Sanitize Article Authors
	 */
	public Map<ReCiterAuthor, ReCiterAuthor> sanitizeArticleAuthorNames(ReCiterArticle reCiterArticle) {
		Map<ReCiterAuthor, ReCiterAuthor> sanitizeArticleAuthors = new LinkedHashMap<ReCiterAuthor, ReCiterAuthor>();
		if(reCiterArticle.getArticleCoAuthors() != null && reCiterArticle.getArticleCoAuthors().getAuthors() != null) {
			for(ReCiterAuthor authorName: reCiterArticle.getArticleCoAuthors().getAuthors()) {
				AuthorName articleAuthor = new AuthorName();
				if(authorName.getAuthorName().getFirstName() != null) {
					articleAuthor.setFirstName(ReCiterStringUtil.deAccent(authorName.getAuthorName().getFirstName().replaceAll("[-.\"() ]", "")));
				}
				if(authorName.getAuthorName().getLastName() != null) {
					articleAuthor.setLastName(ReCiterStringUtil.deAccent(authorName.getAuthorName().getLastName().replaceAll("[-.\",()\\s]|(" + generateSuffixRegex() + ")$", "")));
				}
				ReCiterAuthor sanitizedReCiterAuthor = new ReCiterAuthor(articleAuthor, authorName.getAffiliation());
				sanitizedReCiterAuthor.setRank(authorName.getRank());
				sanitizedReCiterAuthor.setTargetAuthor(authorName.isTargetAuthor());
				sanitizedReCiterAuthor.setValidEmail(authorName.getValidEmail());
				sanitizeArticleAuthors.put(authorName, sanitizedReCiterAuthor);
			}
		}
		return sanitizeArticleAuthors;
	}
	
	/**
	 * Sanitize Identity Author Names and also derive author names if there is double code (") in the first name
	 * @param identity
	 * @return Sanitized Map of Identity primary names, alternate names and derived names
	 */
	public Map<AuthorName, AuthorName> sanitizeIdentityAuthorNames(Identity identity) {
		
		Map<AuthorName, AuthorName> sanitizedIdentityAuthorName = null;
		if(identity != null) {
			sanitizedIdentityAuthorName = new HashMap<AuthorName, AuthorName>();
			AuthorName identityPrimaryName = new AuthorName();
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
							additionalName.setFirstName(ReCiterStringUtil.deAccent(firstName));
						}
						Pattern pattern = Pattern.compile("\"([^\"]*)\"|(\"([^\"]*)\")|(([a-z]*))/i/g");
						Matcher matcher = pattern.matcher(identity.getPrimaryName().getFirstName());
						while(matcher.find()) {
							identityPrimaryName.setFirstName(ReCiterStringUtil.deAccent(matcher.group().replaceAll("\"", "")));
						}
					} else {
						identityPrimaryName.setFirstName(ReCiterStringUtil.deAccent(identity.getPrimaryName().getFirstName().replaceAll("[-.,()\\s]", "")));
					}
				}
				if(identity.getPrimaryName().getMiddleName() != null) {
					if(identity.getPrimaryName().getMiddleName().contains("\"") || (identity.getPrimaryName().getMiddleName().contains("(") && identity.getPrimaryName().getMiddleName().contains(")"))) {
						middleName = identity.getPrimaryName().getMiddleName().replaceAll("[-.,()\\s]", "").replaceAll("\"([^\"]*)\"|(\"([^\"]*)\")|(([a-z]*))/i/g", "");
						if(middleName !=null) {
							additionalName.setMiddleName(ReCiterStringUtil.deAccent(middleName));
						}
						Pattern pattern = Pattern.compile("\"([^\"]*)\"|(\"([^\"]*)\")|(([a-z]*))/i/g");
						Matcher matcher = pattern.matcher(identity.getPrimaryName().getMiddleName());
						while(matcher.find()) {
							identityPrimaryName.setMiddleName(ReCiterStringUtil.deAccent(matcher.group().replaceAll("\"", "")));
						}
					} else {
						identityPrimaryName.setMiddleName(ReCiterStringUtil.deAccent(identity.getPrimaryName().getMiddleName().replaceAll("[-.,()\\s]", "")));
					}
				}
				if(identity.getPrimaryName().getLastName() != null) {
					//lastName = identity.getPrimaryName().getLastName().replaceAll("[-.,,()\\s]|(,Jr|, Jr|, MD PhD|,MD PhD|, MD-PhD|,MD-PhD|, PhD|,PhD|, MD|,MD|, III|,III|, II|,II|, Sr|,Sr|Jr|MD PhD|MD-PhD|PhD|MD|III|II|Sr)$", "");
					lastName = identity.getPrimaryName().getLastName().replaceAll("[-.,,()\\s]|(" + generateSuffixRegex() + ")$", "");
					identityPrimaryName.setLastName(ReCiterStringUtil.deAccent(lastName));
					if(additionalName.getFirstName() != null) {
						additionalName.setLastName(ReCiterStringUtil.deAccent(lastName));
					}
				}
				if(identityPrimaryName.getLastName() != null) {
					sanitizedIdentityAuthorName.put(identity.getPrimaryName(), identityPrimaryName);
				}
			}
			
			if(identity.getAlternateNames() != null && identity.getAlternateNames().size() > 0) {
				for(AuthorName aliasAuthorName: identity.getAlternateNames()) {
					AuthorName identityAliasAuthorName = new AuthorName();
					if(aliasAuthorName.getFirstName() != null) {
						identityAliasAuthorName.setFirstName(ReCiterStringUtil.deAccent(aliasAuthorName.getFirstName().replaceAll("[-.\",()\\s]", "")));
					}
					if(aliasAuthorName.getMiddleName() != null) {
						identityAliasAuthorName.setMiddleName(ReCiterStringUtil.deAccent(aliasAuthorName.getMiddleName().replaceAll("[-.\",()\\s]", "")));
					}
					if(aliasAuthorName.getLastName() != null) {
						identityAliasAuthorName.setLastName(ReCiterStringUtil.deAccent(aliasAuthorName.getLastName().replaceAll("[-.\",()\\s]|(" + generateSuffixRegex() + ")$", "")));
					}
					
					if(identityAliasAuthorName.getLastName() != null) {
						sanitizedIdentityAuthorName.put(aliasAuthorName, identityAliasAuthorName);
					}
				}
			}
			if(additionalName != null && additionalName.getLastName() != null) {
				sanitizedIdentityAuthorName.put(additionalName, additionalName);
			}
			if(sanitizedIdentityAuthorName.size() > 1) {
				checkToIgnoreNameVariants(sanitizedIdentityAuthorName);
			}
		}
		return sanitizedIdentityAuthorName;
	}
	
	/**
	 * Drops lesser name variants when a fuller one exists (see {@link #isNameVariantToIgnore}).
	 *
	 * <p>#704: the previous version mutated the map through two live iterators inside a nested
	 * loop, with three separate blocks each able to call remove() in the same pass. When a
	 * removed entry was the iterator's last, the "advance past it" guard had nothing to
	 * advance to and the next block's remove() threw IllegalStateException — a 500 on every
	 * feature-generator call for any identity whose name variants matched two rules at once
	 * (alc4061: "Alberto Mario" / "" + "Alberto Mario" / null + "Alberto" / null).
	 *
	 * <p>Now: decide over a snapshot of the keys, remove from the map by key. The snapshot is
	 * sorted fullest-name-first so the survivor never depends on HashMap iteration order, a
	 * key already dropped is never used as the "full" name that drops others, and the last
	 * remaining name is never removed.
	 * @param identityAuthorNames raw identity name → sanitized name; pruned in place
	 */
	public void checkToIgnoreNameVariants(Map<AuthorName, AuthorName> identityAuthorNames) {
		List<AuthorName> keys = new ArrayList<>(identityAuthorNames.keySet());
		keys.sort(Comparator.comparingInt((AuthorName k) -> nameLength(identityAuthorNames.get(k))).reversed()
				.thenComparing(k -> String.valueOf(identityAuthorNames.get(k))));
		for (AuthorName ik : keys) {
			AuthorName iv = identityAuthorNames.get(ik);
			if (iv == null) {
				continue; // already dropped as a lesser variant of an earlier (fuller) name
			}
			for (AuthorName jk : keys) {
				AuthorName jv = identityAuthorNames.get(jk);
				if (jv == null || ik.equals(jk)) {
					continue;
				}
				if (identityAuthorNames.size() > 1 && isNameVariantToIgnore(iv, jv)) {
					identityAuthorNames.remove(jk);
				}
			}
		}
	}

	private static int nameLength(AuthorName n) {
		return n == null ? 0 : StringUtils.length(n.getFirstName()) + StringUtils.length(n.getMiddleName())
				+ StringUtils.length(n.getLastName());
	}

	/**
	 * True when {@code candidate} is a lesser variant of {@code full} and should be dropped:
	 * same last name, and either (1) full's first name starts with candidate's and neither has a
	 * middle name, (2) same start-with and candidate's middle name is blank, or (3) first, middle
	 * and last are all equal ignoring case — a case-only duplicate such as "Andrew J Dannenberg"
	 * vs "andrew j dannenberg". Rule 3 does NOT collapse a middle initial into a full middle name
	 * ("Andrew J" vs "Andrew Jess" are both kept); that is the pre-#704 behaviour, preserved.
	 */
	static boolean isNameVariantToIgnore(AuthorName full, AuthorName candidate) {
		if (full == null || candidate == null || full.equals(candidate)) {
			return false;
		}
		if (full.getFirstName() == null || candidate.getFirstName() == null || candidate.getLastName() == null
				|| !StringUtils.equalsIgnoreCase(full.getLastName(), candidate.getLastName())) {
			return false;
		}
		boolean firstStartsWith = full.getFirstName().toLowerCase().startsWith(candidate.getFirstName().toLowerCase());
		if (firstStartsWith && candidate.getMiddleName() == null && full.getMiddleName() == null) {
			return true;
		}
		if (firstStartsWith && candidate.getMiddleName() != null && candidate.getMiddleName().isBlank()) {
			return true;
		}
		return full.getMiddleName() != null && candidate.getMiddleName() != null
				&& StringUtils.equalsIgnoreCase(full.getFirstName(), candidate.getFirstName())
				&& StringUtils.equalsIgnoreCase(full.getMiddleName(), candidate.getMiddleName());
	}
	
	
	/**
	 * This function generates regex of suffix from application.properties file
	 * @return regex string
	 */
	public String generateSuffixRegex() {
		nameExcludedSuffixes = Arrays.asList(strategyParameters.getNameExcludedSuffixes().trim().split("\\s*,\\s*"));
		String suffixRegex = "";
		String suffixTogether = "";
		for(String suffix: nameExcludedSuffixes) {
			suffixRegex = suffixRegex + "," + suffix + "|, " + suffix + "|";
			suffixTogether = suffixTogether + suffix + "|";
		}
		if(suffixTogether.endsWith("|")) {
			suffixTogether = suffixTogether.substring(0, suffixTogether.length() - 1);
		}
		return suffixRegex + "," + suffixTogether;
	}
	

}

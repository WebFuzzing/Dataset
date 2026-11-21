package reciter.xml.retriever.pubmed;

import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import reciter.utils.ReCiterStringUtil;
import reciter.model.identity.AuthorName;
import reciter.model.identity.Identity;
import reciter.pubmed.retriever.PubMedQuery;
import reciter.xml.retriever.pubmed.PubMedQueryType.PubMedQueryBuilder;

/**
 * @author Sarbajit Dutta(szd2013)
 * @see <a href= "https://github.com/wcmc-its/ReCiter/issues/269">Details here</a>
 * This class does lookup in Pubmed based on first two capital letters in Identity names(first or middle)
 */
@Component("secondInitialRetrievalStrategy")
public class SecondInitialRetrievalStrategy extends AbstractNameRetrievalStrategy {
	
	private static final String retrievalStrategyName = "SecondInitialRetrievalStrategy";
	private final static Logger slf4jLogger = LoggerFactory.getLogger(SecondInitialRetrievalStrategy.class);

	@Override
	public String getRetrievalStrategyName() {
		return retrievalStrategyName;
	}

	@Override
	protected PubMedQuery buildNameQuery(Set<AuthorName> identitynames, Identity identity) {
		PubMedQueryBuilder pubMedQueryBuilder = 
				new PubMedQueryBuilder(getStrategySpecificKeyword(identity))
					.author(false, false, identitynames);
		
		PubMedQuery query = pubMedQueryBuilder.build();
		slf4jLogger.info(retrievalStrategyName + " produced query=[" + query + "]");
		return query;
	}

	@Override
	protected PubMedQuery buildNameQuery(Set<AuthorName> identitynames, Identity identity, Date startDate,
			Date endDate) {
		PubMedQueryBuilder pubMedQueryBuilder = 
				new PubMedQueryBuilder(getStrategySpecificKeyword(identity))
					.author(false, false, identitynames)
					.dateRange(true, startDate, endDate);
		
		return pubMedQueryBuilder.build();
	}

	@Override
	protected String getStrategySpecificKeyword(Identity identity) {
		if (identity.getPrimaryName() != null || (identity.getAlternateNames() != null && !identity.getAlternateNames().isEmpty())) {
			String middleName = null;
			if(identity.getPrimaryName().getMiddleName() != null) {
				middleName = identity.getPrimaryName().getMiddleName();
			} else {
				middleName = "";
			}
			String combinedFirstMiddleName = identity.getPrimaryName().getFirstName() + middleName;	
			String capitalizeFirstMiddle = ReCiterStringUtil.deAccent(combinedFirstMiddleName.chars().filter(Character::isUpperCase)
						.mapToObj(c -> Character.toString((char)c))
						.collect(Collectors.joining()));
			String first = null;
			if(capitalizeFirstMiddle.length() >= 2) {
				first = identity.getPrimaryName().getLastName() + " " + capitalizeFirstMiddle.substring(0, 2) + "[au]";
			}
			
			if(first != null && identity.getAlternateNames() != null && !identity.getAlternateNames().isEmpty()) {
				return first;
			}
			
			Iterator<AuthorName> iter =  identity.getAlternateNames() != null ? identity.getAlternateNames().iterator() : Collections.emptyIterator();
			
			final StringBuilder buf = new StringBuilder();
			if(first != null) {
				buf.append("(" + first);
			} else {
				buf.append("(");
			}
			while(iter.hasNext()) {
				AuthorName alternateName = iter.next();
				String alternateNameWithInitial = null;
				if(alternateName.getMiddleName() != null) {
					middleName = alternateName.getMiddleName();
				} else {
					middleName = "";
				}
				combinedFirstMiddleName = alternateName.getFirstName() + middleName;	
				capitalizeFirstMiddle = ReCiterStringUtil.deAccent(combinedFirstMiddleName.chars().filter(Character::isUpperCase)
							.mapToObj(c -> Character.toString((char)c))
							.collect(Collectors.joining()));
				if(capitalizeFirstMiddle.length() >= 2) {
					alternateNameWithInitial = alternateName.getLastName() + " " + capitalizeFirstMiddle.substring(0, 2) + "[au]";
				}
				if(alternateNameWithInitial != null) {
					buf.append(alternateNameWithInitial);
					buf.append(" OR ");
				}
			}
			buf.append(")");
			String result = buf.toString();
			if(result.equals("()")) {
				return null;
			}
			if(result.endsWith(" OR )")) {
				return result.substring(0, result.length() - 5) + ")";
			}
			else {
				return result;
			}
		} else {
			return null;
		}
	}
	

}

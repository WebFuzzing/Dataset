package reciter.xml.retriever.pubmed;

import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import reciter.model.identity.AuthorName;
import reciter.model.identity.Identity;
import reciter.model.identity.KnownRelationship;
import reciter.pubmed.retriever.PubMedQuery;
import reciter.xml.retriever.pubmed.PubMedQueryType.PubMedQueryBuilder;

/**
 * @author szd2013
 * This class gets all the known relationship from Identity such as HR relationship, co-investigator, manager & mentor/mentee and 
 * creates query to be searched in Pubmed in strict retrieval mode
 */
@Component("knownRelationshipRetrievalStrategy")
public class KnownRelationshipRetrievalStrategy extends AbstractNameRetrievalStrategy {
	
	private static final String retrievalStrategyName = "KnownRelationshipRetrievalStrategy";
	private final static Logger slf4jLogger = LoggerFactory.getLogger(KnownRelationshipRetrievalStrategy.class);
	
	@Value("${namesIgnoredCoauthors}")
	private String excludeNames;

	@Override
	public String getRetrievalStrategyName() {
		return retrievalStrategyName;
	}

	@Override
	protected PubMedQuery buildNameQuery(Set<AuthorName> identitynames, Identity identity) {
		PubMedQueryBuilder pubMedQueryBuilder = 
				new PubMedQueryBuilder(getStrategySpecificKeyword(identity))
					.author(true, false, identitynames);
		
		PubMedQuery query = pubMedQueryBuilder.build();
		slf4jLogger.info(retrievalStrategyName + " produced query=[" + query + "]");
		return query;
	}

	@Override
	protected PubMedQuery buildNameQuery(Set<AuthorName> identitynames, Identity identity, Date startDate,
			Date endDate) {
		PubMedQueryBuilder pubMedQueryBuilder = 
				new PubMedQueryBuilder(getStrategySpecificKeyword(identity))
					.author(true, false, identitynames)
					.dateRange(true, startDate, endDate);
		
		return pubMedQueryBuilder.build();
	}

	@Override
	protected String getStrategySpecificKeyword(Identity identity) {
		if (identity.getKnownRelationships() != null && !identity.getKnownRelationships().isEmpty()) {
			List<String> nameIgnoredAuthors = Arrays.asList(excludeNames.trim().split("\\s*,\\s*"));
			Iterator<KnownRelationship> iter = identity.getKnownRelationships().iterator();
			final KnownRelationship firstRelationship = iter.next(); 
			String first = null;
			if(!nameIgnoredAuthors.contains(firstRelationship.getName().getLastName() + " " + firstRelationship.getName().getFirstInitial())) {
				first = firstRelationship.getName().getLastName() + " " + firstRelationship.getName().getFirstInitial() + "[au]";
			}
			
			if (!iter.hasNext()) {
				return first;
			}
			
			//for more than 1 institutions 
			final StringBuilder buf = new StringBuilder();
			if(first != null) {
				buf.append("(" + first);
			} 
			while(iter.hasNext()) {
				KnownRelationship knownRelationship = iter.next();
				if(!nameIgnoredAuthors.contains(knownRelationship.getName().getLastName() + " " + knownRelationship.getName().getFirstInitial())) {
					buf.append(" OR ");
					buf.append(knownRelationship.getName().getLastName() + " " + firstRelationship.getName().getFirstInitial() + "[au]");
				}
			}
			buf.append(")");
			return buf.toString();
		} else {
			return null;
		}
	}

}

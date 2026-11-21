package reciter.algorithm.cluster.similarity.clusteringstrategy.article;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import reciter.algorithm.cluster.ReCiterClusterer;
import reciter.algorithm.cluster.clusteringstrategy.article.AbstractClusteringStrategy;
import reciter.algorithm.cluster.model.ReCiterCluster;
import reciter.model.article.ReCiterArticle;
import reciter.model.article.ReCiterArticleGrant;

/**
 * @author <b>Sarbajit Dutta(szd2013)</b>
 * 
 * @description:
 * This class parses NIH grant identifiers for all articles in a standardized logic of Funding Agency-4 to 6 digit grant code. Then matches and form clusters.
 * It also checks for transitive property matches as well. 
 * For full details refer to https://github.com/wcmc-its/ReCiter/issues/217 section 4. - Feature: grant identifiers.
 */
public class GrantFeatureClusteringStrategy extends AbstractClusteringStrategy {
	
	//private static final Logger slf4jLogger = LoggerFactory.getLogger(GrantFeatureClusteringStrategy.class);

	@Override
	public Map<Long, ReCiterCluster> cluster(List<ReCiterArticle> reCiterArticles) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Long, ReCiterCluster> cluster(List<ReCiterArticle> reCiterArticles, Set<Long> seedPmids) {
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * This function will group from the email clusters with articles having an similar grant identifier
	 * @param clusters List of clusters from initial clustering
	 * @return list of clusters
	 */
	@Override
	public Map<Long, ReCiterCluster> cluster(Map<Long, ReCiterCluster> clusters) {
		
		for (Entry<Long, ReCiterCluster> entry : clusters.entrySet()) {
	        ReCiterCluster reCiterCluster = entry.getValue();
	        for (ReCiterArticle reCiterArticle : reCiterCluster.getArticleCluster()) {
				checkForValidGrant(reCiterArticle);
			}
		}
		
		//Compare each clusters with all other for matching grant ID
		long mapSize = ReCiterClusterer.baselineClusterSize;
		for(long i=(long) 1 ; i <= mapSize ; i++) {
			for(long j = (long) 1; j <= mapSize; j++) {
				if(i==j) {
					continue;
				}
				else {
					if(clusters.get(i) != null && clusters.get(j) != null) {
						if(clusters.get(i).compareTo(clusters.get(j), "grant") == 1) {
							clusters.get(i).addAll(clusters.get(j).getArticleCluster());
							clusters.remove(j);
						}
					}
				}
			}
		}
		
		return clusters;
	}
	
	private void checkForValidGrant(ReCiterArticle reCiterArticle) {
		if (reCiterArticle.getGrantList() == null) return;
		for (ReCiterArticleGrant grant : reCiterArticle.getGrantList()) {
			if(grant.getGrantID() != null) {
				String sanitizedGrant = sanitizeGrant(grant.getGrantID().replaceAll("[\\s\\-]", ""));
				if(sanitizedGrant != null) {
					grant.setSanitizedGrantID(sanitizedGrant);
				}
			}
		}	
	}
	
	private String sanitizeGrant(String grant) {
		String fundingAgency = null;
		String grantId = null;
		Pattern pattern = Pattern.compile("([a-zA-Z][a-zA-Z]+)");
		Matcher matcher = pattern.matcher(grant);
		int matchCount = 0;
		while(matcher.find()) {
			matchCount++;
			if(matchCount == 2) {
				fundingAgency = matcher.group();
			}
			fundingAgency = matcher.group();
		}
		
		matchCount = 0;
		pattern = Pattern.compile("[0-9]{4,6}");
		matcher = pattern.matcher(grant);
		while(matcher.find()) {
			grantId = matcher.group().replaceFirst("^0*", "");
		}
		
		if(fundingAgency != null && grantId != null) {
			return fundingAgency + "-" + grantId;
		}
		return null;
	}

}

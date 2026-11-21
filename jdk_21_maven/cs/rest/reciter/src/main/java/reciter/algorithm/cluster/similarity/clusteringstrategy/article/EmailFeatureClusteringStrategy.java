package reciter.algorithm.cluster.similarity.clusteringstrategy.article;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reciter.algorithm.cluster.ReCiterClusterer;
import reciter.algorithm.cluster.clusteringstrategy.article.AbstractClusteringStrategy;
import reciter.algorithm.cluster.model.ReCiterCluster;
import reciter.model.article.ReCiterArticle;
import reciter.model.article.ReCiterAuthor;

/**
 * @author szd2013
 * This class parses email addresses of all authors including where targetAuthor is FALSE and TRUE. Preprocess the affiliation string
 * in standardized format and if has a valid email then put that article in the same cluster
 */
public class EmailFeatureClusteringStrategy extends AbstractClusteringStrategy {
	
	private static final Logger slf4jLogger = LoggerFactory.getLogger(EmailFeatureClusteringStrategy.class);

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
	 * This function will group from the initial clusters with articles having an valid email and email matches
	 * @param clusters List of clusters from initial clustering
	 * @return list of clusters
	 */
	@Override
	public Map<Long, ReCiterCluster> cluster(Map<Long, ReCiterCluster> clusters) {
		
		for (Entry<Long, ReCiterCluster> entry : clusters.entrySet()) {
	        ReCiterCluster reCiterCluster = entry.getValue();
	        for (ReCiterArticle reCiterArticle : reCiterCluster.getArticleCluster()) {
				checkForValidEmail(reCiterArticle);
			}
		}
		
		//Compare each clusters with all other for matching email
		long mapSize = ReCiterClusterer.baselineClusterSize;
		for(long i=(long) 1 ; i <= mapSize ; i++) {
			long index = i;
			for(long j = 1; j <= mapSize; j++) {
				if(i==j) {
					continue;
				}
				else {
					if(clusters.get(j) != null && clusters.get(index) != null) {
						if(clusters.get(index).compareTo(clusters.get(j)) == 1) {
							clusters.get(index).addAll(clusters.get(j).getArticleCluster());
							clusters.remove(j);
						}
					}
				}
			}
		}
		return clusters;
	}
	
	private void checkForValidEmail(ReCiterArticle reCiterArticle) {
		for (ReCiterAuthor author : reCiterArticle.getArticleCoAuthors().getAuthors()) {
			String affiliation = null;
			if (author.getAffiliation() != null && !author.getAffiliation().isBlank()) {
				affiliation = author.getAffiliation();
			}
			if(affiliation != null) {
				String validEmail = sanitizeAffiliation(affiliation);
				if(validEmail != null) {
					slf4jLogger.info("Valid Email found in article: " + reCiterArticle.getArticleId() + " with email: " + validEmail);
					author.setValidEmail(validEmail);
				}
			}
		}	
	}
	
	
	public static String sanitizeAffiliation(String affiliation) {
		Pattern pattern = Pattern.compile("([a-z0-9_.-]+)@([a-z0-9_.-]+[a-z])");
		Matcher matcher = pattern.matcher(affiliation.toLowerCase());
		while(matcher.find()) {
			return matcher.group();
		}
		return null;
	}

}

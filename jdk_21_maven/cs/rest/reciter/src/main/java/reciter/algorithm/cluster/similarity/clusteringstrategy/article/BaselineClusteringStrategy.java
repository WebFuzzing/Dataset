package reciter.algorithm.cluster.similarity.clusteringstrategy.article;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import reciter.algorithm.cluster.clusteringstrategy.article.AbstractClusteringStrategy;
import reciter.algorithm.cluster.model.ReCiterCluster;
import reciter.model.article.ReCiterArticle;


/**
 * @author szd2013
 * This class assigns each individual article to its own cluster with articles retrieved from eSearchResults
 *
 */
public class BaselineClusteringStrategy extends AbstractClusteringStrategy {
	
	
	/**
	 * Select the first article from the list. Iterate through the remaining
	 * articles and assign article based on target author name match.
	 */
	@Override
	public Map<Long, ReCiterCluster> cluster(List<ReCiterArticle> reCiterArticles) {
		
		// Reset ReCiterCluster's static id counter to 0, so that subsequent calls
		// to cluster method has ReCiterCluster id starts with 0.
		ReCiterCluster.getClusterIDCounter().set(0);
		
		Map<Long, ReCiterCluster> clusters = new HashMap<>();

		reCiterArticles.forEach(article -> {
			// create its own cluster.
			ReCiterCluster newReCiterCluster = new ReCiterCluster();
			//newReCiterCluster.setClusterOriginator(article.getArticleId());
			newReCiterCluster.add(article);
			clusters.put(newReCiterCluster.getClusterID(), newReCiterCluster);
		});
		return clusters;
	}



	@Override
	public Map<Long, ReCiterCluster> cluster(List<ReCiterArticle> reCiterArticles, Set<Long> seedPmids) {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public Map<Long, ReCiterCluster> cluster(Map<Long, ReCiterCluster> clusters) {
		// TODO Auto-generated method stub
		return null;
	}

}

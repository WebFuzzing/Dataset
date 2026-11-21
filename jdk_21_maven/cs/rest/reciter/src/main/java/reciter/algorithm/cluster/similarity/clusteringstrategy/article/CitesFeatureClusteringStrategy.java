package reciter.algorithm.cluster.similarity.clusteringstrategy.article;

import java.util.List;
import java.util.Map;
import java.util.Set;

import reciter.algorithm.cluster.ReCiterClusterer;
import reciter.algorithm.cluster.clusteringstrategy.article.AbstractClusteringStrategy;
import reciter.algorithm.cluster.model.ReCiterCluster;
import reciter.model.article.ReCiterArticle;

/**
 * @author szd2013
 *
 */
public class CitesFeatureClusteringStrategy extends AbstractClusteringStrategy {
	
	//private static final Logger slf4jLogger = LoggerFactory.getLogger(CitesFeatureClusteringStrategy.class);

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
	 * This function will group from the grant clusters with articles citation matches. If article A cites article B or vice versa they should be inthe same cluster.
	 * @param clusters List of clusters from initial clustering
	 * @return list of clusters
	 */
	@Override
	public Map<Long, ReCiterCluster> cluster(Map<Long, ReCiterCluster> clusters) {
		
		//Compare each clusters with all other for matching email
		long mapSize = ReCiterClusterer.baselineClusterSize;
		for(long i = 1 ; i <= mapSize ; i++) {
			for(long j = 1; j <= mapSize; j++) { // The iteration can be reduced since citation check both ways is done in the compareTo function. Possibly with i+1 for start. Need to test more
				if(i==j) {
					continue;
				}
				else {
					if(clusters.get(j) != null && clusters.get(i) != null) {
						
						if(clusters.get(i).compareTo(clusters.get(j),"cites") == 1) {
							clusters.get(i).addAll(clusters.get(j).getArticleCluster());
							clusters.remove(j);
						}
					}
				}
			}
		}
		return clusters;
	}

}

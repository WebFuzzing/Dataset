package reciter.algorithm.cluster.similarity.clusteringstrategy.article;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reciter.algorithm.cluster.ReCiterClusterer;
import reciter.algorithm.cluster.clusteringstrategy.article.AbstractClusteringStrategy;
import reciter.algorithm.cluster.model.ReCiterCluster;
import reciter.model.article.ReCiterArticle;

/**
 * @author szd2013
 * This strategy combines clusters based on features which are calculated by journal title, meshMajor and co-authors.
 * Theory: these features generally occur fewer than 100,000 times in a corpus of 30 million records. We will use these merge articles into a single cluster but only when they occur a certain proportion of the time.
 * General instructions: compare clusters to each other using the below features. If their similarity exceeds some threshold, combine the clusters.
 * Rationale: Because these features may occur more often by chance, we do not automatically combine the clusters if they share the feature. Instead the cluster-cluster comparison needs to meet or exceed a scoring threshold (which is described below).
 */
public class TepidClusteringStrategy extends AbstractClusteringStrategy {
	
	private static final Logger slf4jLogger = LoggerFactory.getLogger(TepidClusteringStrategy.class);
	
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

	@Override
	public Map<Long, ReCiterCluster> cluster(Map<Long, ReCiterCluster> clusters) {
		//Compare each clusters with all other for matching grant ID
		long mapSize = ReCiterClusterer.baselineClusterSize;
		for(long i=(long) 1 ; i <= mapSize ; i++) {
			for(long j = (long) 1; j <= mapSize; j++) {
				if(i==j) {
					continue;
				}
				else {
					if(clusters.get(i) != null && clusters.get(j) != null) {
						if(clusters.get(i).compareTo(clusters.get(j), "tepid") == 1) {
							//slf4jLogger.info("Cluster " + i + " match with Cluster " + j);
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

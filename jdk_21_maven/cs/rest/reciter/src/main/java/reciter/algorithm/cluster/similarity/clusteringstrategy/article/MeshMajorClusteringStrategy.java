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
import reciter.model.article.ReCiterArticleMeshHeading;
import reciter.model.article.ReCiterCitationYNEnum;
import reciter.model.article.ReCiterMeshHeadingQualifierName;

/**
 * @author szd2013
 * This class identify cases where an article from one cluster shares the same MeSH major as an article from another cluster, 
 * and that MeSH major has a global count of < 4,000. 
 */
public class MeshMajorClusteringStrategy extends AbstractClusteringStrategy {

	private static final Logger slf4jLogger = LoggerFactory.getLogger(MeshMajorClusteringStrategy.class);

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
			
			//Compare each clusters with all other for matching grant ID
			long mapSize = ReCiterClusterer.baselineClusterSize;
			for(long i=(long) 1 ; i <= mapSize ; i++) {
				for(long j = (long) 1; j <= mapSize; j++) {
					if(i==j) {
						continue;
					}
					else {
						if(clusters.get(i) != null && clusters.get(j) != null) {
							if(clusters.get(i).compareTo(clusters.get(j), "meshMajor") == 1) {
								clusters.get(i).addAll(clusters.get(j).getArticleCluster());
								clusters.remove(j);
							}
						}
					}
				}
			}
			
			return clusters;
		}
		
		/**
		 * <p>
		 * MeSH major parsing
		 * <p>
		 * Easy: no subheading (e.g., for 23919362, �Contracts" is MeSH major)
		 * <p>
		 * Single subheading (e.g., for 21740463, "Cold Temperature� is MeSH major)
		 * <p>
		 * Harder: Multiple subheadings modifying the same MeSH term. (e.g., for 6358887, "Aspirin" is mesh major even 
		 * though there are cases where aspirin is not listed as MeSH major.)
		 * @param meshHeading
		 * @return
		 */
		public static boolean isMeshMajor(ReCiterArticleMeshHeading meshHeading) {
			// Easy case:
			if (meshHeading.getDescriptorName().getMajorTopicYN().equals(ReCiterCitationYNEnum.Y.name())) {
				return true;
			}
			// Single subheading or Multiple subheading:
			for (ReCiterMeshHeadingQualifierName qualifierName : meshHeading.getQualifierNameList()) {
				if (qualifierName.getMajorTopicYN() == ReCiterCitationYNEnum.Y) {
					return true;
				}
			}

			return false;
		}
		
}

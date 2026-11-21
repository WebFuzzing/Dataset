/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package reciter.algorithm.cluster;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Getter;
import lombok.Setter;
import reciter.algorithm.cluster.clusteringstrategy.article.ClusteringStrategy;
import reciter.algorithm.cluster.model.ReCiterCluster;
import reciter.algorithm.cluster.similarity.clusteringstrategy.article.BaselineClusteringStrategy;
import reciter.algorithm.cluster.similarity.clusteringstrategy.article.CitesFeatureClusteringStrategy;
import reciter.algorithm.cluster.similarity.clusteringstrategy.article.EmailFeatureClusteringStrategy;
import reciter.algorithm.cluster.similarity.clusteringstrategy.article.GrantFeatureClusteringStrategy;
import reciter.algorithm.cluster.similarity.clusteringstrategy.article.MeshMajorClusteringStrategy;
import reciter.algorithm.cluster.similarity.clusteringstrategy.article.TepidClusteringStrategy;
import reciter.model.article.ReCiterArticle;
import reciter.model.identity.Identity;

@Getter
@Setter
public class ReCiterClusterer extends AbstractClusterer {
	
	private static final Logger log = LoggerFactory.getLogger(ReCiterClusterer.class);

	private List<ReCiterArticle> reCiterArticles;
	private Identity identity;
	private Map<Long, ReCiterCluster> clusters;
	private ClusteringStrategy clusteringStrategy;
	public static int baselineClusterSize;
	
	public ReCiterClusterer(Identity identity, List<ReCiterArticle> reCiterArticles) {
		this.reCiterArticles = reCiterArticles;
		this.identity = identity;
		clusters = new HashMap<Long, ReCiterCluster>();
		//clusteringStrategy = new NameMatchingClusteringStrategy(identity);
		clusteringStrategy = new BaselineClusteringStrategy();
	}

	/**
	 * <p>
	 * Phase I clustering.
	 * <p>
	 * Cluster articles together based on the ClusteringStrategy selected.
	 */
	@Override
	public void cluster() {
		log.info("Running ReCiter for: [" + identity.getUid() + "] "
				+ "Number of articles to be clustered:" + reCiterArticles.size());
		//Baseline Clustering Strategy
		clusters = clusteringStrategy.cluster(reCiterArticles);
		log.info("Number of clusters after Baseline clustering: " + clusters.size());
		log.info("Baseline Clustering Strategy results: " + toString());
		
		baselineClusterSize = clusters.size();
		
		//Tepid Clustering Strategy
		clusteringStrategy = new TepidClusteringStrategy();
		clusters = clusteringStrategy.cluster(clusters);
		log.info("Number of clusters after tepid strategy clustering: " + clusters.size());
		log.info("tepid strategy Clustering Strategy results: " + toString());
		
		//Email Clustering Strategy
		clusteringStrategy = new EmailFeatureClusteringStrategy();
		clusters = clusteringStrategy.cluster(clusters);
		log.info("Number of clusters after email strategy clustering: " + clusters.size());
		log.info("email strategy Clustering Strategy results: " + toString());
		
		//Grant Clustering Strategy
		clusteringStrategy = new GrantFeatureClusteringStrategy();
		clusters = clusteringStrategy.cluster(clusters);
		log.info("Number of clusters after grant strategy clustering: " + clusters.size());
		log.info("grant strategy Clustering Strategy results: " + toString());
		
		//Cites or Cited by Clustering Strategy
		clusteringStrategy = new CitesFeatureClusteringStrategy();
		clusters = clusteringStrategy.cluster(clusters);
		log.info("Number of clusters after cites strategy clustering: " + clusters.size());
		log.info("cites strategy Clustering Strategy results: " + toString());
		
		//Mesh Major Clustering Strategy
		clusteringStrategy = new MeshMajorClusteringStrategy();
		clusters = clusteringStrategy.cluster(clusters);
		log.info("Number of clusters after mesh major strategy clustering: " + clusters.size());
		log.info("Mesh Major strategy Clustering Strategy results: " + toString());
	}
	

	@Override
	public void cluster(Set<Long> seedPmids) {
		log.info("Running ReCiter for: [" + identity.getUid() + "] "
				+ "Number of articles to be clustered:" + reCiterArticles.size() + " initial seeds=" + seedPmids);
		clusters = clusteringStrategy.cluster(reCiterArticles, seedPmids);
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		for (Entry<Long, ReCiterCluster> cluster : clusters.entrySet()) {
			sb.append("\nCluster id: " + cluster.getKey() + "= ," + cluster.getValue().getClusterInfo());
			for (ReCiterArticle reCiterArticle : cluster.getValue().getArticleCluster()) {
				sb.append(reCiterArticle.getArticleId() + ", ");
			}
		}
		return sb.toString();
	}
}

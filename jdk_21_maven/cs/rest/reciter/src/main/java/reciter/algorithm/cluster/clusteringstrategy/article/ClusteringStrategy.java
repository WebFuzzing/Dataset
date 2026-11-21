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
package reciter.algorithm.cluster.clusteringstrategy.article;

import java.util.List;
import java.util.Map;
import java.util.Set;

import reciter.algorithm.cluster.model.ReCiterCluster;
import reciter.model.article.ReCiterArticle;

public interface ClusteringStrategy {

	Map<Long, ReCiterCluster> cluster(List<ReCiterArticle> reCiterArticles);
	
	Map<Long, ReCiterCluster> cluster(List<ReCiterArticle> reCiterArticles, Set<Long> seedPmids);
	
	Map<Long, ReCiterCluster> cluster(Map<Long, ReCiterCluster> clusters);
}

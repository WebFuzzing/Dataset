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
package reciter.algorithm.cluster.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;

import reciter.algorithm.cluster.similarity.clusteringstrategy.article.MeshMajorClusteringStrategy;
import reciter.engine.EngineParameters;
import reciter.model.article.ReCiterArticle;
import reciter.model.article.ReCiterArticleFeatures;
import reciter.model.article.ReCiterArticleMeshHeading;
import reciter.model.article.ReCiterAuthor;
import reciter.model.identity.Identity;

public class ReCiterCluster implements Comparable<ReCiterCluster>{

	/**
	 * Cluster Id.
	 */
	private final long clusterId;
	
	/**
	 * Atomic integer counter
	 */
	private static AtomicInteger clusterIDCounter = new AtomicInteger(0);

	/**
	 * List of articles in its cluster.
	 */
	private List<ReCiterArticle> articleCluster;
	
	private double clusterReliabilityScore;

	/**
	 * Cluster originator.
	 */
	private long clusterOriginator;
	
	private boolean isSelected;

	private String clusterInfo = "";
	
	public static class MeshTermCount {
		private String mesh;
		private long count;
		
		public MeshTermCount() {}
		
		public String getMesh() {
			return mesh;
		}
		public void setMesh(String mesh) {
			this.mesh = mesh;
		}
		public long getCount() {
			return count;
		}
		public void setCount(long count) {
			this.count = count;
		}
	}
	
	private List<MeshTermCount> meshTermCounts;
	
	/**
	 * Returns a list of pmids of articles in this cluster.
	 */
	public Set<Long> getPmidSet() {
		Set<Long> pmidSet = new HashSet<Long>();
		for (ReCiterArticle reCiterArticle : articleCluster) {
			pmidSet.add(reCiterArticle.getArticleId());
		}
		return pmidSet;
	}

	/**
	 * Calculates the similarity of this cluster with another cluster.
	 * @param c
	 * @return
	 */
	public double similar(ReCiterCluster c) {
		double maxSim = -1;
		for (ReCiterArticle article : c.getArticleCluster()) {
			double sim = contentSimilarity(article);
			if (sim > maxSim) {
				maxSim = sim;
			}
		}
		return maxSim;
	}

	/**
	 * Calculates the similarity between an ReCiterArticle and a ReCiterCluster.
	 * @param currentArticle
	 * @return
	 */
	public double contentSimilarity(ReCiterArticle currentArticle) {
		double similarityScore = -1;
		for (ReCiterArticle article : articleCluster) {
//			double sim = article.getDocumentSimmilarity().documentSimilarity(article, currentArticle);
			double sim = 0;
			if (sim > similarityScore) {
				similarityScore = sim;
			}
		}
		return similarityScore;
	}

	/**
	 * Checks whether this cluster contains an author who has a name variant
	 * to the target author.
	 * @param type
	 * @param initials
	 * @return
	 */
	public boolean containsNameVariant(ReCiterAuthor targetAuthor) {
//		for (ReCiterArticle article : articleCluster) {
//			for (ReCiterAuthor author : article.getArticleCoAuthors().getAuthors()) {
//				if (targetAuthor.getAuthorName().isNameVariant(author.getAuthorName())) {
//					return true;
//				}
//			}
//		}
		return false;
	}

	/**
	 * Get number of matching coauthors except the target author for each of the articles in this cluster 
	 * with the <code>currentArticle</code> and return the max number of matching coauthors out of the articles in this cluster.
	 * @param currentArticle current article being compared.
	 * @return the max number of matching coauthors.
	 * 
	 * TODO revise the if statement logic.
	 */
	public int getMatchingCoauthorCount(ReCiterArticle currentArticle, Identity targetAuthor) {
		int matchingCoauthorCount = 0;
		// For each article in this cluster.
		for (ReCiterArticle article : articleCluster) {
			// For each author in this article.
			for (ReCiterAuthor author : article.getArticleCoAuthors().getAuthors()) {
				// For each author in the currentArticle.
				for (ReCiterAuthor currentAuthor : currentArticle.getArticleCoAuthors().getAuthors()) {
					
					// Check if the names match.
					if ((currentAuthor.getAuthorName().isFullNameMatch(author.getAuthorName()) 
							&& !currentAuthor.getAuthorName().firstInitialLastNameMatch(targetAuthor.getPrimaryName())
							&& !author.getAuthorName().firstInitialLastNameMatch(targetAuthor.getPrimaryName()))) {
						
						matchingCoauthorCount += 1;
						
					} else if (currentAuthor.getAffiliation() != null && author.getAffiliation() != null) {
						if (currentAuthor.getAuthorName().firstInitialLastNameMatch(author.getAuthorName())) {
							
//							slf4jLogger.debug(currentAuthor.getAuthorName() + " " + author.getAuthorName());
								matchingCoauthorCount += 1;
							}
					}
				}
			}
		}
		return matchingCoauthorCount;
	}

	public void add(ReCiterArticle article) {
		this.articleCluster.add(article);
	}

	public void addAll(List<ReCiterArticle> reCiterArticles) {
		articleCluster.addAll(reCiterArticles);
	}

	public List<ReCiterArticle> getArticleCluster() {
		return this.articleCluster;
	}

	public void setArticleCluster(List<ReCiterArticle> articleCluster) {
		this.articleCluster = articleCluster;
	}

	public double getClusterReliabilityScore() {
		return clusterReliabilityScore;
	}

	public void setClusterReliabilityScore(double clusterReliabilityScore) {
		this.clusterReliabilityScore = clusterReliabilityScore;
	}

	public ReCiterCluster() {
		this.clusterId = clusterIDCounter.incrementAndGet();
		this.articleCluster = new ArrayList<ReCiterArticle>();
	}

	public long getClusterID() {
		return clusterId;
	}
	
	public static AtomicInteger getClusterIDCounter() {
		return clusterIDCounter;
	}

	public long getClusterOriginator() {
		return clusterOriginator;
	}

	public void setClusterOriginator(long clusterOriginator) {
		this.clusterOriginator = clusterOriginator;
	}

	public String getClusterInfo() {
		return clusterInfo;
	}

	public void setClusterInfo(String clusterInfo) {
		this.clusterInfo = clusterInfo;
	}

	public List<MeshTermCount> getMeshTermCounts() {
		return meshTermCounts;
	}

	public void setMeshTermCounts(List<MeshTermCount> meshTermCounts) {
		this.meshTermCounts = meshTermCounts;
	}

	public boolean isSelected() {
		return isSelected;
	}

	public void setSelected(boolean isSelected) {
		this.isSelected = isSelected;
	}

	@Override
	public int compareTo(ReCiterCluster o) {
		boolean emailMatch = false;
		for(ReCiterArticle reCiterArticle: o.getArticleCluster()) {
			for(ReCiterAuthor authoro: reCiterArticle.getArticleCoAuthors().getAuthors()) {
				if(authoro.getValidEmail() != null && !authoro.getValidEmail().isBlank()) {
					emailMatch = this.articleCluster.stream().anyMatch(article -> article.getArticleCoAuthors().getAuthors().stream().anyMatch(author -> author.getValidEmail() != null && !author.getValidEmail().isBlank() &&
							StringUtils.equalsIgnoreCase(author.getValidEmail(), authoro.getValidEmail())));
					if(emailMatch) {
						return 1;
					}
				}
			}
		}
		return 0;
	}
	
	/**
	 * @param o The ReCiterCluster to compare to
	 * @param comparisonType what kind of comparison happening e.g. email or grants etc.
	 * @return 1 if equal or 0 if not
	 */
	public int compareTo(ReCiterCluster o, String comparisonType) {
		boolean match = false;
		if(comparisonType.equalsIgnoreCase("grant")) {
			for(ReCiterArticle reCiterArticle: o.getArticleCluster()) {
				/*if(reCiterArticle.getGrantList().stream().filter(articlegrant -> articlegrant.getSanitizedGrantID() != null).count() <= ReCiterEngine.clutseringGrantsThreshold) {
					for(ReCiterArticleGrant granto: reCiterArticle.getGrantList()) {
						if(granto.getSanitizedGrantID() != null && !granto.getSanitizedGrantID().isEmpty()) {
							match = this.articleCluster.stream().anyMatch(articleList -> articleList.getGrantList().stream().filter(articlegrant -> articlegrant.getSanitizedGrantID() != null).count() <= ReCiterEngine.clutseringGrantsThreshold
							&&
							articleList.getGrantList().stream().anyMatch(grant -> grant.getSanitizedGrantID() != null && !grant.getSanitizedGrantID().isEmpty() 
							&&
							StringUtils.equalsIgnoreCase(grant.getSanitizedGrantID().trim(), granto.getSanitizedGrantID().trim())));
							if(match) {
								return 1;
							}
						}
						
					}
				}*/
			}
		} else if(comparisonType.equalsIgnoreCase("cites")) {
			for(ReCiterArticle reCiterArticle: o.getArticleCluster()) {
					//A cites B
					match = this.articleCluster.stream().anyMatch(articleList -> reCiterArticle.getCommentsCorrectionsPmids() != null && 
							reCiterArticle.getCommentsCorrectionsPmids().size() > 0 && articleList.getArticleId() != 0 && 
							reCiterArticle.getCommentsCorrectionsPmids().contains(articleList.getArticleId()));
					//B cites A
					if(!match) {
						match = this.articleCluster.stream().anyMatch(articleList -> articleList.getCommentsCorrectionsPmids() != null && 
								articleList.getCommentsCorrectionsPmids().size() > 0 && reCiterArticle.getArticleId() != 0 &&
								articleList.getCommentsCorrectionsPmids().contains(reCiterArticle.getArticleId()));
					}
					if(match) {
						return 1;
					}
			}
		} else if(comparisonType.equalsIgnoreCase("meshMajor")) {
			for(ReCiterArticle reCiterArticle: o.getArticleCluster()) {
				if(reCiterArticle.getMeshHeadings() == null) continue;
				for(ReCiterArticleMeshHeading meshHeading: reCiterArticle.getMeshHeadings()) {
					if(meshHeading != null && MeshMajorClusteringStrategy.isMeshMajor(meshHeading)) {
						match = this.articleCluster.stream().anyMatch(articleList -> articleList.getMeshHeadings() != null && 
								articleList.getMeshHeadings().stream().anyMatch(mesh -> MeshMajorClusteringStrategy.isMeshMajor(mesh) && 
								StringUtils.equalsIgnoreCase(mesh.getDescriptorName().getDescriptorName(), meshHeading.getDescriptorName().getDescriptorName()) && 
								EngineParameters.getMeshCountMap() != null && EngineParameters.getMeshCountMap().containsKey(meshHeading.getDescriptorName().getDescriptorName()) &&
								EngineParameters.getMeshCountMap().get(meshHeading.getDescriptorName().getDescriptorName()) < 4000L
								));
					}
					if(match) {
						return 1;
					}
				}
			}
		} else if(comparisonType.equalsIgnoreCase("tepid")) {
			int matchCount = 0;
			double clusterSimilarityScore = 0;
			for(ReCiterArticle reCiterArticleo: o.getArticleCluster()) {
				for(ReCiterArticle reCiterArticle: this.articleCluster) {
					matchCount = reCiterOverlapCount(reCiterArticle.getReCiterArticleFeatures(), reCiterArticleo.getReCiterArticleFeatures());
					if(matchCount > 0 && reCiterArticle.getReCiterArticleFeatures().getFeatureCount() >= 3 && reCiterArticleo.getReCiterArticleFeatures().getFeatureCount() >= 3) {
						clusterSimilarityScore = computeClusterSimilarityScore(reCiterArticle.getReCiterArticleFeatures().getFeatureCount(), reCiterArticleo.getReCiterArticleFeatures().getFeatureCount(), matchCount);
						/*if(clusterSimilarityScore > ReCiterEngine.clusterSimilarityThresholdScore) {
							return 1;
						}*/
					}
				}
			}
		}
		return 0;
	}
	
	private int reCiterOverlapCount(ReCiterArticleFeatures reCiterArticleFeature1, ReCiterArticleFeatures reCiterArticleFeature2) {
		int matchCount = 0;
		//Journal Feature match
		if(reCiterArticleFeature1.getJournalName() != null && !reCiterArticleFeature1.getJournalName().isBlank() && 
				reCiterArticleFeature2.getJournalName() != null && !reCiterArticleFeature2.getJournalName().isBlank() &&
				StringUtils.equalsIgnoreCase(reCiterArticleFeature1.getJournalName(), reCiterArticleFeature2.getJournalName())) {
			matchCount++;
		}
		//MeshMajor Feature match
		if(reCiterArticleFeature1.getMeshMajor() != null && reCiterArticleFeature1.getMeshMajor().size() > 0 &&
				reCiterArticleFeature2.getMeshMajor() != null && reCiterArticleFeature2.getMeshMajor().size() > 0) {
			List<String> matchingMeshMajor = new ArrayList<String>(reCiterArticleFeature1.getMeshMajor());
			matchingMeshMajor.retainAll(reCiterArticleFeature2.getMeshMajor());
			if(matchingMeshMajor.size() > 0) {
				matchCount = matchCount + matchingMeshMajor.size();
			}
		}
		//Co-Author Feature match
		if(reCiterArticleFeature1.getCoAuthors() != null && reCiterArticleFeature1.getCoAuthors().size() > 0 &&
				reCiterArticleFeature2.getCoAuthors() != null && reCiterArticleFeature2.getCoAuthors().size() > 0) {
			List<String> matchingCoAuthor = new ArrayList<String>(reCiterArticleFeature1.getCoAuthors());
			matchingCoAuthor.retainAll(reCiterArticleFeature2.getCoAuthors());
			if(matchingCoAuthor.size() > 0) {
				matchCount = matchCount + matchingCoAuthor.size();
			}
		}
		
		if(reCiterArticleFeature1.getAffiliationIds() != null && reCiterArticleFeature1.getAffiliationIds().size() > 0 &&
				reCiterArticleFeature2.getAffiliationIds() != null && reCiterArticleFeature2.getAffiliationIds().size() > 0) {
			List<Integer> matchingAffiliationId = new ArrayList<Integer>(reCiterArticleFeature1.getAffiliationIds());
			matchingAffiliationId.retainAll(reCiterArticleFeature2.getAffiliationIds());
			if(matchingAffiliationId.size() > 0) {
				matchCount = matchCount + 1;
			}
		}
		return matchCount;
	}
	
	private double computeClusterSimilarityScore(int clusterScore1, int clusterScore2, int overlapScore) {
		return Math.pow(overlapScore, 2)/(clusterScore1 * clusterScore2);
	}
	

}

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
package reciter.algorithm.tfidf;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class TfIdf {

	private List<Document> documents;
	private Map<String, Term> terms;

	public TfIdf(List<Document> documents) {
		this.documents = documents;
		terms = new HashMap<String, Term>();
		for (Document document : documents) {
			for (String termStr : document.getTermToFreqMap().keySet()) {
				if (terms.containsKey(termStr)) {
					Term term = terms.get(termStr);
					term.getDocumentIds().add(document.getId());
				} else {
					Term term = new Term(termStr);
					term.getDocumentIds().add(document.getId());
					terms.put(termStr, term);
				}
			}
		}
	}

	public double tf(Term term, Document document) {
		return document.getTermToFreqMap().get(term.getTerm());
	}

	public double idf(Term term, List<Document> documents) {
		int numberDocuments = documents.size();
		long numberDocumentsTermAppearsIn = terms.get(term.getTerm()).getDocumentIds().size();
		if (numberDocumentsTermAppearsIn == 0) {
			return 0;
		} else {
			return Math.log10((double) numberDocuments / numberDocumentsTermAppearsIn);
		}
	}

	public double computeTfIdfScoreForSingleTerm(Term term, Document document) {
		double tf = tf(term, document);
//		double idf = idf(term, documents);
//		return tf * idf;
		return tf;
	}
	
	public void computeTfIdf() {
		for (Document document : documents) {
			for (Term term : document.getTerms().values()) {
				term.setTfidfScore(computeTfIdfScoreForSingleTerm(term, document));
			}
		}
	}
	
	public List<Document> getDocuments() {
		return documents;
	}

	public double[] createVector(Document document) {
		double[] tfidfArray = new double[terms.size()];
		int index = 0;
		for (Term term : terms.values()) {
			if (document.getTerms().containsKey(term.getTerm())) {
				tfidfArray[index] = document.getTerms().get(term.getTerm()).getTfidfScore();
			}
			index++;
		}
		return tfidfArray;
	}
	
	public double cosineSimilarity(double[] v1, double[] v2) {
		double dotProduct = dotProduct(v1, v2);
		double normProduct = norm(v1) * norm(v2);
		if (normProduct == 0) {
			return 0;
		} else {
			return dotProduct / normProduct;
		}
	}
	
	public double dotProduct(double[] v1, double[] v2) {
		double dotProduct = 0;
		for (int i = 0; i < v1.length; i++) {
			dotProduct += v1[i] * v2[i];
		}
		return dotProduct;
	}
	
	public double norm(double[] vector) {
		double sumSquare = 0;
		for (double val : vector) {
			sumSquare += val * val;
		}
		return Math.sqrt(sumSquare);
	}
	
}

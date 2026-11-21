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
import java.util.Map;

public class Document {

	private long id;
	private Map<String, Long> termToFreqMap;
	private Map<String, Term> terms;

	public Document(String s) {
		termToFreqMap = new HashMap<String, Long>();
		terms = new HashMap<String, Term>();
		String[] tokens = tokenize(s);
		updateTermFrequency(tokens);
	}
	
	protected String[] tokenize(String s) {
		return s.replaceAll("[^A-Za-z0-9\\s+]", " ").split("\\s+");
	}
	
	private void updateTermFrequency(String[] tokens) {
		for (String token : tokens) {
			if (termToFreqMap.containsKey(token)) {
				long currentFreq = termToFreqMap.get(token);
				currentFreq += 1;
				termToFreqMap.put(token, currentFreq);
			} else {
				termToFreqMap.put(token, 1L);
				terms.put(token, new Term(token)); // add to terms.
			}
		}
	}
	
	public Map<String, Long> getTermToFreqMap() {
		return termToFreqMap;
	}

	public boolean contains(String s) {
		return termToFreqMap.containsKey(s);
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}
	
	public Map<String, Term> getTerms() {
		return terms;
	}

}

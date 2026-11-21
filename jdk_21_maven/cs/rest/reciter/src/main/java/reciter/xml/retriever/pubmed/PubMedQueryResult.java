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
package reciter.xml.retriever.pubmed;

import reciter.pubmed.retriever.PubMedQuery;

public class PubMedQueryResult {

	private PubMedQuery query;
	private int numResult;
	private boolean isUsed;

	public PubMedQueryResult(PubMedQuery query) {
		this.query = query;
	}

	public PubMedQuery getQuery() {
		return query;
	}
	public void setQuery(PubMedQuery query) {
		this.query = query;
	}
	public int getNumResult() {
		return numResult;
	}
	public void setNumResult(int numResult) {
		this.numResult = numResult;
	}
	public boolean isUsed() {
		return isUsed;
	}
	public void setUsed(boolean isUsed) {
		this.isUsed = isUsed;
	}
	@Override
	public String toString() {
		return "PubMedQueryResult [query=" + query + ", numResult=" + numResult + ", isUsed=" + isUsed + "]";
	}
}

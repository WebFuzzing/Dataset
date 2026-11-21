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

/**
 * Reference documentation for the various parameters in this class: 
 * {@link http://www.ncbi.nlm.nih.gov/books/NBK25499/}
 * @author jil3004
 *
 */
public class PubmedXmlQuery {

	public static final int DEFAULT_RETMAX = 10000;
	
	/**
	 * Required Parameters.
	 */
	
	/**
	 * Database to search. Value must be a valid Entrez database name. (Default={@code pubmed})
	 */
	private String db = "pubmed";
	
	/**
	 * Entrez text query. All special characters must be URL encoded. Spaces may be replaced by '+'
	 * signs. For very long queries (more than several hundred characters long), consider using 
	 * an HTTP POST call. (Required parameter).
	 */
	private String term;
	
	/**
	 * Optional Parameters.
	 */
	
	/**
	 * Total number of UIDs from the retrieved set to be shown in the XML output.
	 * PubMed default is 20. 
	 * 
	 * Total number of DocSums from the input set to be retrieved, up to a maximum of 10,000. 
	 * If the total set is larger than this maximum, the value of retstart can be iterated 
	 * while holding retmax constant, thereby downloading the entire set in batches of size retmax.
	 */
	private int retMax = DEFAULT_RETMAX;
	
	/**
	 * Sequential index of the first UID in the retrieved set to be shown in the XML output, 
	 * corresponding to the first record of the entire set. PubMed default is 0. This parameter 
	 * can be used in conjunction with {@link retMax} to download an arbitrary subset of UIDs
	 * retrieved from a search.
	 */
	private int retStart;
	
	/**
	 * When {@link useHistory} is set to {@code true}, ESearch will post the UIDs resulting 
	 * from the search operation onto the PubMed history server so that they can be used 
	 * directly in a subsequent E-utility call. Also {@link useHistory} must be set to {@code true}
	 * for ESearch to interpret query key values included in {@link term} or to accept a
	 * {@link webEnv} as input.
	 */
	private String useHistory = "y";
	
	/**
	 * Web environment string returned from a previous ESearch, EPost or ELink call. When provided, 
	 * ESearch will post the results of the search operation to this pre-existing {@link webEnv}, 
	 * thereby appending the results to the existing environment. 
	 */
	private String webEnv;
	
	/**
	 * Integer query key returned by a previous ESearch, EPost or Elink call.
	 */
	private int queryKey = 1;
	
	/**
	 * Returned format for query. xml or json.
	 */
	private String retMode = "xml";
	
	protected static final String ESEARCH_BASE_URL = "https://www.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi";
	protected static final String EFETCH_BASE_URL = "https://www.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi";
	
	public PubmedXmlQuery() {}
	
	public PubmedXmlQuery(String db, String term) {
		this.db = db;
		this.term = term;
	}
	
	public PubmedXmlQuery(String term) {
		this.term = term;
	}
	
	/**
	 * Constructs a ESearch query String. 
	 * 
	 * @return a String in the format http://www.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=pubmed&retmax=1&usehistory=y&term=Kukafka%20R[au]
	 */
	public String buildESearchQuery() {
		StringBuilder sb = new StringBuilder();
		sb.append(ESEARCH_BASE_URL);
		sb.append("?db=");
		sb.append(db);
		sb.append("&retmax=");
		sb.append(retMax);
		sb.append("&usehistory=");
		sb.append(useHistory);
		sb.append("&term=");
		sb.append(term);
		sb.append("&retmode=");
		sb.append(retMode);
		return sb.toString();
	}
	
	/**
	 * Construct a EFetch query String.
	 * 
	 * @return a String in the format 
	 * http://www.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?retmode=xml&db=pubmed&retstart=retstart&retmax=retmax&query_key=1&WebEnv=webenv
	 */
	public String buildEFetchQuery() {
		StringBuilder sb = new StringBuilder();
		sb.append(EFETCH_BASE_URL);
		sb.append("?retmode=");
		sb.append(retMode);
		sb.append("&db=");
		sb.append(db);
		sb.append("&retstart=");
		sb.append(retStart);
		sb.append("&retmax=");
		sb.append(retMax);
		sb.append("&query_key=");
		sb.append(queryKey);
		sb.append("&WebEnv=");
		sb.append(webEnv);
		return sb.toString();
	}

	public int getRetMax() {
		return retMax;
	}

	public void setRetMax(int retMax) {
		this.retMax = retMax;
	}

	public int getRetStart() {
		return retStart;
	}

	public void setRetStart(int retStart) {
		this.retStart = retStart;
	}

	public String getWebEnv() {
		return webEnv;
	}

	public void setWevEnv(String webEnv) {
		this.webEnv = webEnv;
	}
	
	public int getQueryKey() {
		return queryKey;
	}

	public void setQueryKey(int queryKey) {
		this.queryKey = queryKey;
	}

	public String getTerm() {
		return term;
	}

	public void setTerm(String term) {
		this.term = term;
	}

	public String getDb() {
		return db;
	}

	public void setDb(String db) {
		this.db = db;
	}

	public String getUseHistory() {
		return useHistory;
	}

	public void setUseHistory(String useHistory) {
		this.useHistory = useHistory;
	}

	public String getRetMode() {
		return retMode;
	}

	public void setRetMode(String retMode) {
		this.retMode = retMode;
	}

}
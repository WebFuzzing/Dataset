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
package reciter.utils;

import reciter.model.identity.AuthorName;
import reciter.model.pubmed.MedlineCitationArticleAuthor;

public class PubMedConverter {

	/**
	 * PubMed sometimes concatenates the first name and middle initial into <ForeName> xml tag.
	 * This extracts the first name and middle initial. 
	 * Sometimes forename doesn't exist in XML (ie: 8661541). So initials are used instead. 
	 * Forename take precedence. If foreName doesn't exist, use initials. If initials doesn't exist, use null.
	 *  
	 * TODO: Deal with collective names in XML.
	 * @param medlineCitationArticleAuthor
	 * @return
	 */
	public static AuthorName extractAuthorName(MedlineCitationArticleAuthor medlineCitationArticleAuthor) {
		String lastName = medlineCitationArticleAuthor.getLastname();

		if (lastName == null) {
			return null;
		}
		
		String foreName = medlineCitationArticleAuthor.getForename();
		String initials = medlineCitationArticleAuthor.getInitials();

		String firstName = null;
		String middleName = null;
		
		if (foreName != null) {
			String[] foreNameArray = foreName.split("\\s+");
			if (foreNameArray.length == 2) {
				firstName = foreNameArray[0];
				middleName = foreNameArray[1];
			} else {
				firstName = foreName;
			}
		} else if (initials != null) {
			firstName = initials;
		}
		
		AuthorName authorName = new AuthorName(firstName, middleName, lastName);
		return authorName;
	}
}

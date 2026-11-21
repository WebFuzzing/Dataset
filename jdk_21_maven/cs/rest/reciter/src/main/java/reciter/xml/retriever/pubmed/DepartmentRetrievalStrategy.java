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

import java.util.Date;
import java.util.Iterator;
import java.util.Set;

import org.springframework.stereotype.Component;

import reciter.model.identity.AuthorName;
import reciter.model.identity.Identity;
import reciter.model.identity.OrganizationalUnit;
import reciter.pubmed.retriever.PubMedQuery;
import reciter.xml.retriever.pubmed.PubMedQueryType.PubMedQueryBuilder;

@Component("departmentRetrievalStrategy")
public class DepartmentRetrievalStrategy extends AbstractNameRetrievalStrategy {

	private static final String retrievalStrategyName = "DepartmentRetrievalStrategy";
	
	@Override
	public String getRetrievalStrategyName() {
		return retrievalStrategyName;
	}

	@Override
	protected String getStrategySpecificKeyword(Identity identity) {
		if (identity.getOrganizationalUnits() != null && !identity.getOrganizationalUnits().isEmpty()) {
			Iterator<OrganizationalUnit> iter = identity.getOrganizationalUnits().iterator();
			final OrganizationalUnit firstDepartment = iter.next(); 
			final String first	= firstDepartment.getOrganizationalUnitLabel() + "[affiliation]";
			if (!iter.hasNext()) {
				return first;
			}
			
			//for more than 1 institutions 
			final StringBuilder buf = new StringBuilder();
			if(first != null) {
				buf.append("(" + first);
			} 
			while(iter.hasNext()) {
				OrganizationalUnit orgUnit = iter.next();
				buf.append(" OR ");
				buf.append(orgUnit.getOrganizationalUnitLabel() + "[affiliation]");
			}
			buf.append(")");
			return buf.toString();
		} else {
			return null;
		}
	}

	@Override
	protected PubMedQuery buildNameQuery(Set<AuthorName> identitynames, Identity identity) {
		PubMedQueryBuilder pubMedQueryBuilder = 
				new PubMedQueryBuilder(getStrategySpecificKeyword(identity))
					.author(true, false, identitynames);
		
		return pubMedQueryBuilder.build();
	}
	
	@Override
	protected PubMedQuery buildNameQuery(Set<AuthorName> identitynames, Identity identity, Date startDate,
			Date endDate) {
		PubMedQueryBuilder pubMedQueryBuilder = 
				new PubMedQueryBuilder(getStrategySpecificKeyword(identity))
					.author(true, false, identitynames)
					.dateRange(true, startDate, endDate);
		
		return pubMedQueryBuilder.build();
	}
}

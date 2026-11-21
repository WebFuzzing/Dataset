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
use reciter;

/* Viewing data */

/* Total number of documents in this identity collection */
db.identity.count();

/* View all available data for a given UID */
db.identity.find({"_id": "rgcryst"});

/* Find pubmed article with PMID 18341570 */
db.pubmedarticle.find({"_id": 18341570});

/* Find the PubMed search strategies for uid 'amb9023' */
db.esearchresult.find({"uid": "wcb2001"});

/* Find all identities */
db.identity.find();

/* Find identity whose uid is 'rgcryst' */
db.identity.find({"_id": "rgcryst"});

/* Working with the gold standard */

/* Get a list of all UIDs in the gold standard */

db.goldstandard.find();

/* Get a list of all PMIDs in the gold standard for a given UID */
db.goldstandard.find({"_id" : "rgcryst"});

/** Updating gold standard for rak2007 */
db.goldstandard.update({"_id": "rgcryst"}, {$addToSet: {"knownPmids": {$each: [
NumberLong(11946695)
]}}});

/* Add pmid 26976629 to gold standard for 'cnathan' */
db.goldstandard.update({"_id": "mlg2007"}, {$addToSet: {"knownPmids": NumberLong(26898884)}});

/* Remove from gold standard for 'cnathan' an erroneous array [26976629] */
db.goldstandard.update({"_id": "rgcryst"}, {$pull: {"knownPmids": [18492313]}});

/* Remove from gold standard for 'cnathan' an erroneous pmid NumberLong(26976629) */
db.goldstandard.update({"_id": "rgcryst"}, {$pull: {"knownPmids": NumberLong(18492313)}});

/* Find if PMID exist in this uid */
db.goldstandard.find({"_id": "rgcryst", "knownPmids": {$in: [NumberLong(26927796)]}});

/* Add a new gold standard */
db.goldstandard.insertOne(
	{
		"_id": "ccole",
		"knownPmids" : [
        	NumberLong(17238381),
			NumberLong(18999229),
			NumberLong(20478738),
			NumberLong(22465355),
			NumberLong(22874398),
			NumberLong(23578816),
			NumberLong(15360861)
        ]
	}
);

/* Working with analysis results */

/* Find average precision and recall in analysis */
db.analysis.aggregate([
{
	$group: {
		_id:null, precision: {$avg:"$analysis.precision"}, recall: {$avg:"$analysis.recall"}
	}
}
]);

/* Display the results of the analysis for a given UID */
db.analysis.find({"_id": "rbaergen"});

/* Find all uids whose precision is less than 0.1 */
db.analysis.find({"analysis.precision": {"$lt" : 0.1}});

/* Find all uids whose recall is less than 0.1 */
db.analysis.find({"analysis.recall": {"$lt" : 0.1}});

/* Additional query examples */

/* Delete the esearch result where the uid is 'rbaergen' */
db.esearchresult.deleteMany({"uid": "rbaergen"});

db.meshterm.find();

db.analysis.count();

db.recitercluster.find();

db.recitercluster.find({"_id": "ccole"});

db.recitercluster.find({"$and" : [
  {"reCiterClusters.articleCluster.meshMajorStrategyScore": {"$gt" : 0.0}}, 
  {"_id" : "aas2004"}
 ]
});

db.createCollection("institutionafid");
// import as csv file.
db.institutionafid.find();
db.institutionafid.find({"institution": "University of London (United Kingdom)"});

db.scopusarticle.find();

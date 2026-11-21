

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
package reciter.algorithm.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import reciter.algorithm.cluster.similarity.clusteringstrategy.article.MeshMajorClusteringStrategy;
import reciter.engine.EngineParameters;
import reciter.engine.StrategyParameters;
import reciter.model.article.ReCiterArticle;
import reciter.model.article.ReCiterArticleAuthors;
import reciter.model.article.ReCiterArticleFeatures;
import reciter.model.article.ReCiterArticleGrant;
import reciter.model.article.ReCiterArticleKeywords;
import reciter.model.article.ReCiterArticleMeshHeading;
import reciter.model.article.ReCiterAuthor;
import reciter.model.article.ReCiterCitationYNEnum;
import reciter.model.article.ReCiterJournal;
import reciter.model.article.ReCiterMeshHeadingDescriptorName;
import reciter.model.article.ReCiterMeshHeadingQualifierName;
import reciter.model.article.ReCiterPublicationTypeScopus;
import reciter.model.identity.AuthorName;
import reciter.model.pubmed.MedlineCitationArticleAuthor;
import reciter.model.pubmed.MedlineCitationCommentsCorrections;
import reciter.model.pubmed.MedlineCitationDate;
import reciter.model.pubmed.MedlineCitationGrant;
import reciter.model.pubmed.MedlineCitationJournalISSN;
import reciter.model.pubmed.MedlineCitationKeyword;
import reciter.model.pubmed.MedlineCitationKeywordList;
import reciter.model.pubmed.MedlineCitationMeshHeading;
import reciter.model.pubmed.MedlineCitationMeshHeadingQualifierName;
import reciter.model.pubmed.MedlineCitationYNEnum;
import reciter.model.pubmed.PubMedArticle;
import reciter.model.pubmed.PubMedPubDate;
import reciter.model.scopus.Author;
import reciter.model.scopus.ScopusArticle;
import reciter.utils.AuthorNameSanitizationUtils;

/**
 * Translator that translates a PubmedArticle to ReCiterArticle.
 *
 * @author jil3004
 */
@Component
public class ArticleTranslator {

    private static final Logger slf4jLogger = LoggerFactory.getLogger(ArticleTranslator.class);

    /**
     * Translates a PubmedArticle into a ReCiterArticle.
     *
     * @param pubmedArticle
     * @return
     */
    public static ReCiterArticle translate(PubMedArticle pubmedArticle, ScopusArticle scopusArticle, String nameIgnoredCoAuthors, StrategyParameters strategyParameters) {

    	 if (pubmedArticle == null) {
             slf4jLogger.warn("PubMedArticle is null, cannot translate to ReCiterArticle");
             return null;
         }
    	 
        // PMID
        long pmid = pubmedArticle.getMedlinecitation().getMedlinecitationpmid().getPmid();
        ReCiterArticle reCiterArticle = new ReCiterArticle(pmid);
        
        //PMCID
        
        if(pubmedArticle.getPubmeddata() != null && 
        		pubmedArticle.getPubmeddata().getArticleIdList() != null &&
        		pubmedArticle.getPubmeddata().getArticleIdList().getPmc() != null) {
        	reCiterArticle.setPmcid(pubmedArticle.getPubmeddata().getArticleIdList().getPmc());
        }

        // Article title
        String articleTitle = pubmedArticle.getMedlinecitation().getArticle().getArticletitle();

        // Journal Title (may be null for PubmedBookArticle records)
        String journalTitle = null;
        List<MedlineCitationJournalISSN> journalIssn = null;
        int journalIssuePubDateYear = 0;
        String isoAbbreviation = null;
        boolean hasJournal = pubmedArticle.getMedlinecitation().getArticle().getJournal() != null;

        if (hasJournal) {
            journalTitle = pubmedArticle.getMedlinecitation().getArticle().getJournal().getTitle();
            journalIssn = pubmedArticle.getMedlinecitation().getArticle().getJournal().getIssn();
            isoAbbreviation = pubmedArticle.getMedlinecitation().getArticle().getJournal().getIsoAbbreviation();
            if (pubmedArticle.getMedlinecitation().getArticle().getJournal().getJournalissue() != null
                    && pubmedArticle.getMedlinecitation().getArticle().getJournal().getJournalissue().getPubdate() != null
                    && pubmedArticle.getMedlinecitation().getArticle().getJournal().getJournalissue().getPubdate().getYear() != null) {
                journalIssuePubDateYear = Integer.parseInt(pubmedArticle.getMedlinecitation().getArticle().getJournal().getJournalissue().getPubdate().getYear());
            }
        }

        //Pubmed Publication Type
        if(pubmedArticle.getMedlinecitation().getArticle().getPublicationtypelist() != null && !pubmedArticle.getMedlinecitation().getArticle().getPublicationtypelist().isEmpty()) {
	        List<String> publicationTypePubmed = pubmedArticle.getMedlinecitation().getArticle().getPublicationtypelist().stream().map(pubType -> pubType.getPublicationtype()).collect(Collectors.toList());
	        
	        reCiterArticle.setPublicationTypePubmed(publicationTypePubmed);
        }
        
        Optional.ofNullable(pubmedArticle.getMedlinecitation())
        .map(medlineCitation -> medlineCitation.getArticle())
        .map(article -> article.getPublicationAbstract())
        .map(publicationAbstract -> publicationAbstract.getAbstractTexts())
        .filter(abstractTexts -> abstractTexts != null && !abstractTexts.isEmpty())
        .ifPresent(abstractTexts -> {
            String pubmedPublicationAbstract = abstractTexts
                    .stream()
                    .filter(Objects::nonNull)
                    .map(pubAbstract -> {
                        String label = pubAbstract.getAbstractTextLabel() != null ? pubAbstract.getAbstractTextLabel() + ": " : "";
                        String text = pubAbstract.getAbstractText() != null ? pubAbstract.getAbstractText() : "";
                        return label + text;
                    })
                    .collect(Collectors.joining(" "));
            reCiterArticle.setPublicationAbstract(pubmedPublicationAbstract);
        });
        
        
        
        
        

        // Co-authors
        List<MedlineCitationArticleAuthor> coAuthors = pubmedArticle.getMedlinecitation().getArticle().getAuthorlist();

        // Translating Co-Authors
        int i = 1;
        ReCiterArticleAuthors reCiterCoAuthors = new ReCiterArticleAuthors();
        if (coAuthors != null) {
            for (MedlineCitationArticleAuthor author : coAuthors) {
            	if(author!=null) {
                String lastName = author.getLastname();
                String foreName = author.getForename();
                String initials = author.getInitials();
                String firstName = null;
                String middleName = null;

                // PubMed sometimes concatenates the first name and middle initial into <ForeName> xml tag.
                // This extracts the first name and middle initial.

                // Sometimes forename doesn't exist in XML (ie: 8661541). So initials are used instead.
                // Forename take precedence. If foreName doesn't exist, use initials. If initials doesn't exist, use null.
                // TODO: Deal with collective names in XML.
                if (lastName != null) {
                    if (foreName != null) {
                        //String[] foreNameArray = foreName.split("\\s+");
                        //if (foreNameArray.length == 2) {
                        //	firstName = foreNameArray[0];
                        //	middleName = foreNameArray[1];
                        //} else {
                        //	firstName = foreName;
                        //}
                        firstName = foreName;
                    } else if (initials != null) {
                        firstName = initials;
                    }
                    // Fix #529: Detect reversed names where lastName is a single character
                    // and firstName is a full name (e.g., lastName="S", firstName="John").
                    // PubMed occasionally parses names this way (~0.03% of records).
                    if (firstName != null && firstName.length() > 1 && lastName.length() == 1) {
                        String temp = lastName;
                        lastName = firstName;
                        firstName = temp;
                        slf4jLogger.info("Swapped reversed author name for PMID {}: firstName='{}', lastName='{}'",
                            pubmedArticle.getMedlinecitation().getMedlinecitationpmid().getPmid(), firstName, lastName);
                    }

                    String affiliation = author.getAffiliation();
                    // AuthorName's constructor derives an initial by substring(0, 1) of any
                    // non-null firstName/middleName, so a blank-or-whitespace value crashes
                    // with StringIndexOutOfBoundsException (e.g. a PubMed author with an
                    // empty <ForeName>). Normalize blanks to null, which the constructor
                    // handles. lastName is null-guarded above and never substringed.
                    AuthorName authorName = new AuthorName(blankToNull(firstName), blankToNull(middleName), lastName);

                    ReCiterAuthor reCiterAuthor = new ReCiterAuthor(authorName, affiliation);
                    reCiterAuthor.setRank(i++);
                    reCiterAuthor.setOrcid(author.getOrcid());
                    reCiterAuthor.setEqualContrib(author.getEqualContrib());
                    reCiterCoAuthors.addAuthor(reCiterAuthor);
                }
            }
        }
        }

        MedlineCitationKeywordList keywordList = pubmedArticle.getMedlinecitation().getKeywordlist();

        // Translating Keywords.
        ReCiterArticleKeywords articleKeywords = new ReCiterArticleKeywords();
        if (keywordList != null && keywordList.getKeywordlist() != null) {
            for (MedlineCitationKeyword keyword : keywordList.getKeywordlist()) {
                // A keyword list can contain null entries (observed in PubMed data) —
                // skip them, and defensively skip entries whose keyword text is null.
                if (keyword != null && keyword.getKeyword() != null) {
                    articleKeywords.addKeyword(keyword.getKeyword());
                }
            }
        }

        List<MedlineCitationMeshHeading> meshList = pubmedArticle.getMedlinecitation().getMeshheadinglist();
        List<ReCiterArticleMeshHeading> reCiterArticleMeshHeadings = new ArrayList<>();
        if (meshList != null) {
            // Translating Mesh
            for (MedlineCitationMeshHeading medlineCitationMeshHeading : meshList) {
                String descriptorNameString = medlineCitationMeshHeading.getDescriptorname().getDescriptorname();
                MedlineCitationYNEnum meshMajorTopicYN = medlineCitationMeshHeading.getDescriptorname().getMajortopicyn();
                List<MedlineCitationMeshHeadingQualifierName> medlineCitationMeshHeadingQualifierNames =
                        medlineCitationMeshHeading.getQualifiernamelist();

                List<ReCiterMeshHeadingQualifierName> reCiterMeshHeadingQualifierNames =
                        new ArrayList<>(medlineCitationMeshHeadingQualifierNames.size());

                // Set descriptor name and major topic.
                ReCiterArticleMeshHeading reCiterArticleMeshHeading = new ReCiterArticleMeshHeading();
                ReCiterMeshHeadingDescriptorName reCiterMeshHeadingDescriptorName = new ReCiterMeshHeadingDescriptorName();
                reCiterMeshHeadingDescriptorName.setDescriptorName(descriptorNameString);
                reCiterMeshHeadingDescriptorName.setMajorTopicYN(meshMajorTopicYN.getVal());
                reCiterArticleMeshHeading.setDescriptorName(reCiterMeshHeadingDescriptorName);

                // For each qualifier, set name and major topic.
                for (MedlineCitationMeshHeadingQualifierName medlineCitationMeshHeadingQualifierName : medlineCitationMeshHeadingQualifierNames) {
                    ReCiterMeshHeadingQualifierName reCiterMeshHeadingQualifierName = new ReCiterMeshHeadingQualifierName();
                    reCiterMeshHeadingQualifierName.setQualifierName(medlineCitationMeshHeadingQualifierName.getQualifiername());

                    ReCiterCitationYNEnum e;
                    if ("Y".equals(medlineCitationMeshHeadingQualifierName.getMajortopicyn().getVal())) {
                        e = ReCiterCitationYNEnum.Y;
                    } else {
                        e = ReCiterCitationYNEnum.N;
                    }
                    reCiterMeshHeadingQualifierName.setMajorTopicYN(e);
                    reCiterMeshHeadingQualifierNames.add(reCiterMeshHeadingQualifierName);
                }

                reCiterArticleMeshHeading.setQualifierNameList(reCiterMeshHeadingQualifierNames);
                reCiterArticleMeshHeadings.add(reCiterArticleMeshHeading);
            }
        }

        reCiterArticle.setArticleTitle(articleTitle);
        if (hasJournal) {
            reCiterArticle.setJournal(new ReCiterJournal(journalTitle));
            reCiterArticle.getJournal().setJournalIssuePubDateYear(journalIssuePubDateYear);
            reCiterArticle.getJournal().setJournalIssn(journalIssn);
            reCiterArticle.getJournal().setIsoAbbreviation(isoAbbreviation);
        }
        reCiterArticle.setArticleCoAuthors(reCiterCoAuthors);
        reCiterArticle.setArticleKeywords(articleKeywords);
        reCiterArticle.setMeshHeadings(reCiterArticleMeshHeadings);

        // Use PubMed Article date as the publication date (pubdate)
        // TODO optimize date (single date across ReCiter files) (Tech Debt)
        MedlineCitationDate medlineCitationDate = pubmedArticle.getMedlinecitation().getArticle().getArticledate();
        if (medlineCitationDate != null && medlineCitationDate.getYear() != null) {
        	String articleDateMonth = null;
        	String articleDateDay = null;
        	if(medlineCitationDate.getMonth()== null) {
        		articleDateMonth = "01";
        	} else {
        		articleDateMonth = medlineCitationDate.getMonth();
        	}
        	
        	if(medlineCitationDate.getDay()== null) {
        		articleDateDay = "01";
        	} else {
        		articleDateDay = medlineCitationDate.getDay();
        	}
            LocalDate localDate = new LocalDate(Integer.parseInt(medlineCitationDate.getYear()),
                    Integer.parseInt(articleDateMonth), // convert JUL to 7
                    Integer.parseInt(articleDateDay));
            Date date = localDate.toDate();
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            reCiterArticle.setPublicationDateStandardized(df.format(date));
            df = new SimpleDateFormat("yyyy MMM dd");
            reCiterArticle.setPublicationDateDisplay(df.format(date));
        }
        
        //Case when ArticleDate does not exist use PubDate as date standardized date
        if (reCiterArticle.getPublicationDateStandardized() == null
        		&& hasJournal
        		&& pubmedArticle.getMedlinecitation().getArticle().getJournal().getJournalissue() != null
        		&& pubmedArticle.getMedlinecitation().getArticle().getJournal().getJournalissue().getPubdate() != null) {
        	medlineCitationDate = pubmedArticle.getMedlinecitation().getArticle().getJournal().getJournalissue().getPubdate();
        	String articleDateMonth = null;
        	String articleDateDay = null;
        	if(medlineCitationDate.getMonth()== null) {
        		articleDateMonth = "01";
        	} else {
        		articleDateMonth = medlineCitationDate.getMonth();
        	}
        	
        	if(medlineCitationDate.getDay()== null) {
        		articleDateDay = "01";
        	} else {
        		articleDateDay = medlineCitationDate.getDay();
        	}
        	LocalDate localDate = new LocalDate(Integer.parseInt(medlineCitationDate.getYear()),
                    Integer.parseInt(articleDateMonth), // convert JUL to 7
                    Integer.parseInt(articleDateDay));
            Date date = localDate.toDate();
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            reCiterArticle.setPublicationDateStandardized(df.format(date));
            
            if(reCiterArticle.getPublicationDateDisplay()== null) {
            	df = new SimpleDateFormat("yyyy MMM dd");
                reCiterArticle.setPublicationDateDisplay(df.format(date));
            }
        }
        
        //Populate datePublicationAddedToEntrez if it exits in pubmed
        if(pubmedArticle.getPubmeddata() != null
        		&&
        		pubmedArticle.getPubmeddata().getHistory() != null
        		&&
        		pubmedArticle.getPubmeddata().getHistory().getPubmedPubDate() != null
        		&& pubmedArticle.getPubmeddata().getHistory().getPubmedPubDate().size() > 0) {
        	PubMedPubDate pubmedPubDateEntrez =  pubmedArticle.getPubmeddata().getHistory().getPubmedPubDate().
        			stream().filter(pubmedPubDate -> pubmedPubDate.getPubStatus().equalsIgnoreCase("entrez")).
        			findAny().orElse(null);
        	if(pubmedPubDateEntrez != null) {
        		String entrezDateMonth = null;
            	String entrezDateDay = null;
            	if(pubmedPubDateEntrez.getPubMedPubDate().getMonth() == null) {
            		entrezDateMonth = "01";
            	} else {
            		entrezDateMonth = pubmedPubDateEntrez.getPubMedPubDate().getMonth();
            	}
            	
            	if(pubmedPubDateEntrez.getPubMedPubDate().getDay() == null) {
            		entrezDateDay = "01";
            	} else {
            		entrezDateDay = pubmedPubDateEntrez.getPubMedPubDate().getDay();
            	}
        		LocalDate localDate = new LocalDate(Integer.parseInt(pubmedPubDateEntrez.getPubMedPubDate().getYear()),
                        Integer.parseInt(entrezDateMonth), // convert JUL to 7
                        Integer.parseInt(entrezDateDay));
                Date date = localDate.toDate();
                DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        		reCiterArticle.setDatePublicationAddedToEntrez(df.format(date));
        	}
        }

        //Populate datePublicationAddedToPMC if it exists in pubmed
        if(pubmedArticle.getPubmeddata() != null
        		&&
        		pubmedArticle.getPubmeddata().getHistory() != null
        		&&
        		pubmedArticle.getPubmeddata().getHistory().getPubmedPubDate() != null
        		&& pubmedArticle.getPubmeddata().getHistory().getPubmedPubDate().size() > 0) {
        	PubMedPubDate pubmedPubDatePmcRelease = pubmedArticle.getPubmeddata().getHistory().getPubmedPubDate().
        			stream().filter(pubmedPubDate -> pubmedPubDate.getPubStatus().equalsIgnoreCase("pmc-release")).
        			findAny().orElse(null);
        	if(pubmedPubDatePmcRelease != null) {
        		String pmcDateMonth = null;
            	String pmcDateDay = null;
            	if(pubmedPubDatePmcRelease.getPubMedPubDate().getMonth() == null) {
            		pmcDateMonth = "01";
            	} else {
            		pmcDateMonth = pubmedPubDatePmcRelease.getPubMedPubDate().getMonth();
            	}

            	if(pubmedPubDatePmcRelease.getPubMedPubDate().getDay() == null) {
            		pmcDateDay = "01";
            	} else {
            		pmcDateDay = pubmedPubDatePmcRelease.getPubMedPubDate().getDay();
            	}
        		LocalDate localDate = new LocalDate(Integer.parseInt(pubmedPubDatePmcRelease.getPubMedPubDate().getYear()),
                        Integer.parseInt(pmcDateMonth),
                        Integer.parseInt(pmcDateDay));
                Date date = localDate.toDate();
                DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        		reCiterArticle.setDatePublicationAddedToPMC(df.format(date));
        	}
        }

        // Update PubMed's authors' first name from Scopus Article. Logic is as follows:
        // 1. First compare last name if match:
        // 2. Check scopus's first name has length > 1, so no initials (b/c PubMed already contains this info.)
        // 3. Check first initial is same.
        // 4. Check that scopus author's first name is more "complete" than Pubmed's author name.
        // 5. Only update if PubMed's author name is length = 1.
        // 6. Sanitization: remove periods and whitespaces. Grab only the first name (Scopus also provides middle initial).

        if (scopusArticle != null) {
            for (Author scopusAuthor : scopusArticle.getAuthors()) {
                String scopusAuthorFirstName = scopusAuthor.getGivenName();
                String scopusAuthorLastName = scopusAuthor.getSurname();
                for (ReCiterAuthor reCiterAuthor : reCiterArticle.getArticleCoAuthors().getAuthors()) {
                    String reCiterAuthorLastName = reCiterAuthor.getAuthorName().getLastName();
                    if (StringUtils.equalsIgnoreCase(scopusAuthorLastName, reCiterAuthorLastName)) {
                        String reCiterAuthorFirstName = reCiterAuthor.getAuthorName().getFirstName();
                        String reCiterAuthorFirstInitial = reCiterAuthor.getAuthorName().getFirstInitial();
                        if (scopusAuthorFirstName != null && scopusAuthorFirstName.length() > 1) {
                            if (scopusAuthorFirstName.substring(0, 1).equals(reCiterAuthorFirstInitial)) {
                                if (scopusAuthorFirstName.length() > reCiterAuthorFirstName.length()) {

                                    if (reCiterAuthorFirstName.length() == 1) {

                                        scopusAuthorFirstName = scopusAuthorFirstName.replaceAll("[\\.]", "");
                                        int indexOfWhiteSpace = scopusAuthorFirstName.indexOf(" "); // index should be calculated here because scopusFirstName is updated.
                                        // i.e. If scopusAuthorFirstName = "A. J.", indexOfWhiteSpace would be 2, but it should be 1 after the scopusAuthorFirstName is trimmed.
                                        if (indexOfWhiteSpace == -1) {
                                            reCiterAuthor.getAuthorName().setFirstName(scopusAuthorFirstName);
                                        } else {
                                            reCiterAuthor.getAuthorName().setFirstName(scopusAuthorFirstName.substring(0, indexOfWhiteSpace));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            //Scopus Publication Type
            if(scopusArticle.getSubTypeDescription() != null && scopusArticle.getSubType() != null) {
            	reCiterArticle.setPublicationTypeScopus(ReCiterPublicationTypeScopus.builder().publicationTypeScopusAbbreviation(scopusArticle.getSubType()).publicationTypeScopusLabel(scopusArticle.getSubTypeDescription()).build());
            }
            //Times Cited
            if(scopusArticle.getCitedByCount() > 0) {
            	reCiterArticle.setTimesCited(scopusArticle.getCitedByCount());
            }
            if(scopusArticle.getScopusDocId() != null) {
            	reCiterArticle.setScopusDocId(scopusArticle.getScopusDocId());
            }
        }
        reCiterArticle.setScopusArticle(scopusArticle);

        // Grant lists
        List<MedlineCitationGrant> medlineCitationGrants = pubmedArticle.getMedlinecitation().getArticle().getGrantlist();
        List<ReCiterArticleGrant> reCiterArticleGrants = new ArrayList<>();
        if (medlineCitationGrants != null) {
            for (MedlineCitationGrant medlineCitationGrant : medlineCitationGrants) {
                ReCiterArticleGrant reCiterArticleGrant = new ReCiterArticleGrant();
                reCiterArticleGrant.setAcronym(medlineCitationGrant.getAcronym());
                reCiterArticleGrant.setAgency(medlineCitationGrant.getAgency());
                reCiterArticleGrant.setCountry(medlineCitationGrant.getCountry());
                reCiterArticleGrant.setGrantID(medlineCitationGrant.getGrantid());
                reCiterArticleGrants.add(reCiterArticleGrant);
            }
        }
        reCiterArticle.setGrantList(reCiterArticleGrants);
        
        determinePublicationTypeCanonical(reCiterArticle, scopusArticle);

        // translate the CommentsCorrections.

        if (pubmedArticle.getMedlinecitation().getCommentscorrectionslist() != null) {
            Set<Long> commentsCorrectionsPmids = new HashSet<>();
            Map<Long, String> commentsCorrectionsRefTypes = new HashMap<>();
            List<MedlineCitationCommentsCorrections> commentsCorrectionsList = pubmedArticle.getMedlinecitation().getCommentscorrectionslist();
            for (MedlineCitationCommentsCorrections medlineCitationCommentsCorrections : commentsCorrectionsList) {
                String ccPmidStr = medlineCitationCommentsCorrections.getPmid();
                if (ccPmidStr != null) {
                    if (ccPmidStr.matches("\\d+")) {
                        Long ccPmid = Long.parseLong(ccPmidStr);
                        commentsCorrectionsPmids.add(ccPmid);
                        if (medlineCitationCommentsCorrections.getReftype() != null) {
                            commentsCorrectionsRefTypes.put(ccPmid, medlineCitationCommentsCorrections.getReftype());
                        }
                    } else {
                        slf4jLogger.debug("Skipping non-numeric CommentsCorrections PMID for article {}: {}",
                                pubmedArticle.getMedlinecitation().getMedlinecitationpmid().getPmid(), ccPmidStr);
                    }
                }
            }
            reCiterArticle.setCommentsCorrectionsPmids(commentsCorrectionsPmids);
            reCiterArticle.setCommentsCorrectionsRefTypes(commentsCorrectionsRefTypes);
        }

        // Volume
        if (hasJournal && pubmedArticle.getMedlinecitation().getArticle().getJournal().getJournalissue() != null
                && pubmedArticle.getMedlinecitation().getArticle().getJournal().getJournalissue().getVolume() != null
                && !pubmedArticle.getMedlinecitation().getArticle().getJournal().getJournalissue().getVolume().isBlank()) {
            reCiterArticle.setVolume(pubmedArticle.getMedlinecitation().getArticle().getJournal().getJournalissue().getVolume());
        }
        // issue
        if (hasJournal && pubmedArticle.getMedlinecitation().getArticle().getJournal().getJournalissue() != null
                && pubmedArticle.getMedlinecitation().getArticle().getJournal().getJournalissue().getIssue() != null
                && !pubmedArticle.getMedlinecitation().getArticle().getJournal().getJournalissue().getIssue().isBlank()) {
            reCiterArticle.setIssue(pubmedArticle.getMedlinecitation().getArticle().getJournal().getJournalissue().getIssue());
        }

        // pages
        if (pubmedArticle.getMedlinecitation().getArticle().getPagination() != null &&
                pubmedArticle.getMedlinecitation().getArticle().getPagination().getMedlinepgns() != null &&
                !pubmedArticle.getMedlinecitation().getArticle().getPagination().getMedlinepgns().isEmpty()) {
            reCiterArticle.setPages(pubmedArticle.getMedlinecitation().getArticle().getPagination().getMedlinepgns().get(0));
        }

        // doi
        if (pubmedArticle.getMedlinecitation().getArticle().getElocationid() != null) {
            reCiterArticle.setDoi(pubmedArticle.getMedlinecitation().getArticle().getElocationid().getElocationid());
        }
        
        populateFeatures(reCiterArticle, nameIgnoredCoAuthors);
        
      //Add sanitized Author
	  AuthorNameSanitizationUtils authorNameSanitizationUtils= new AuthorNameSanitizationUtils(strategyParameters);
	  Map<ReCiterAuthor, ReCiterAuthor> sanitizedAuthorMap = authorNameSanitizationUtils.sanitizeArticleAuthorNames(reCiterArticle);
	  reCiterArticle.getArticleCoAuthors().setSanitizedAuthorMap(sanitizedAuthorMap);

        return reCiterArticle;
    }
    
    /**
     * Normalizes a blank-or-whitespace name part to null so that AuthorName's
     * constructor (which substrings any non-null value to derive the initial)
     * cannot throw StringIndexOutOfBoundsException on it.
     * Package-private for testing.
     */
    static String blankToNull(String namePart) {
        if (namePart == null || namePart.isBlank()) {
            return null;
        }
        return namePart;
    }

    /**
     * Phase 1: Determine canonical publication type from PubMed publication types.
     *
     * Priority order matters — first match wins. Retraction takes highest priority
     * among content types because it supersedes the original article type.
     *
     * Phase 2 (evidence-based reclassification) runs after this method when the
     * result is "Academic Article" or "Article", using supplementary signals like
     * title patterns, journal title, and abstract structure.
     *
     * See docs/README-publication-type.md for full rationale and audit results.
     */
    private static void determinePublicationTypeCanonical(ReCiterArticle reCiterArticle, ScopusArticle scopusArticle) {
    	String publicationTypeCanonical = null;

        if(reCiterArticle.getPublicationTypePubmed() != null) {
            List<String> pubTypes = reCiterArticle.getPublicationTypePubmed();

            if(pubTypes.contains("Editorial")
                    ||
                    pubTypes.contains("Introductory Journal Article")) {
                publicationTypeCanonical = "Editorial Article";
            } else if(pubTypes.contains("Letter")) {
                publicationTypeCanonical = "Letter";
            } else if(pubTypes.contains("Comment")) {
                publicationTypeCanonical = "Comment";
            } else if(pubTypes.contains("Preprint")) {
                publicationTypeCanonical = "Preprint";
            } else if(pubTypes.contains("Retraction of Publication")
                    ||
                    pubTypes.contains("Retracted Publication")
                    ||
                    pubTypes.contains("Retraction Notice")) {
                publicationTypeCanonical = "Retraction";
            } else if(pubTypes.contains("Published Erratum")) {
                publicationTypeCanonical = "Erratum";
            } else if(pubTypes.contains("Consensus Development Conference")
                    ||
                    pubTypes.contains("Consensus Development Conference, NIH")
                    ||
                    pubTypes.contains("Address")
                    ||
                    pubTypes.contains("Congress")
                    ||
                    pubTypes.contains("Conference Proceedings")
                    ||
                    pubTypes.contains("Lecture")) {
                publicationTypeCanonical = "Conference Paper";
            } else if(pubTypes.contains("Guideline")
                    ||
                    pubTypes.contains("Practice Guideline")
                    ||
                    pubTypes.contains("Consensus Statement")) {
                publicationTypeCanonical = "Guideline";
            } else if(pubTypes.contains("Case Reports")) {
                publicationTypeCanonical = "Case Report";
            } else if(pubTypes.contains("Meta-Analysis")
                    ||
                    pubTypes.contains("Review")
                    ||
                    pubTypes.contains("Systematic Review")
                    ||
                    pubTypes.contains("Scoping Review")
                    ||
                    pubTypes.contains("Network Meta-Analysis")
                    ||
                    pubTypes.contains("Classical Article")
                    ||
                    pubTypes.contains("Scientific Integrity Review")) {
                publicationTypeCanonical = "Review";
            } else if(pubTypes.contains("Journal Article")
                    ||
                    pubTypes.contains("Clinical Trial, Phase I")
                    ||
                    pubTypes.contains("Clinical Trial, Phase II")
                    ||
                    pubTypes.contains("Clinical Trial, Phase III")
                    ||
                    pubTypes.contains("Clinical Trial, Phase IV")
                    ||
                    pubTypes.contains("Controlled Clinical Trial")
                    ||
                    pubTypes.contains("Randomized Controlled Trial")
                    ||
                    pubTypes.contains("Multicenter Study")
                    ||
                    pubTypes.contains("Twin Study")
                    ||
                    pubTypes.contains("Validation Study")
                    ||
                    pubTypes.contains("Pragmatic Clinical Trial")
                    ||
                    pubTypes.contains("Clinical Study")
                    ||
                    pubTypes.contains("Clinical Trial Protocol")
                    ||
                    pubTypes.contains("Comparative Study")
                    ||
                    pubTypes.contains("Observational Study")
                    ||
                    pubTypes.contains("Evaluation Study")
                    ||
                    pubTypes.contains("Clinical Trial")
                    ||
                    pubTypes.contains("Technical Report")
                    ||
                    pubTypes.contains("Clinical Conference")) {
                publicationTypeCanonical = "Academic Article";
            } else {
                publicationTypeCanonical = "Article";
            }
        }

        // Fallback: if PubMed types were null/empty or no branch matched, default to "Article".
        if (publicationTypeCanonical == null) {
            publicationTypeCanonical = "Article";
        }

        // Phase 2: Evidence-based reclassification for ambiguous defaults.
        // Only runs when Phase 1 returns "Academic Article" or "Article" (the catch-all types).
        // Uses title patterns, journal title, and abstract structure to override.
        if ("Academic Article".equals(publicationTypeCanonical) || "Article".equals(publicationTypeCanonical)) {
            String reclassified = reclassifyByEvidence(reCiterArticle, publicationTypeCanonical);
            if (reclassified != null) {
                publicationTypeCanonical = reclassified;
            }
        }

    	reCiterArticle.setPublicationTypeCanonical(publicationTypeCanonical);
    }

    /**
     * Phase 2: Evidence-based reclassification.
     *
     * Evaluates supplementary signals (title patterns, journal title, abstract
     * structure) to reclassify articles that Phase 1 defaulted to "Academic Article"
     * or "Article". Each signal contributes a weighted score toward candidate types.
     * A candidate needs score >= 3 (one Strong signal) to override.
     *
     * Weights: Strong = 3, Moderate = 2, Weak = 1.
     *
     * The phase1Type parameter ("Academic Article" or "Article") conditions certain
     * signal weights. When PubMed's indexers actively assigned "Journal Article"
     * (Phase 1 = "Academic Article"), the prior for genuine article is higher and
     * ambiguous signals are downweighted.
     *
     * Returns the new canonical type, or null if no candidate meets threshold.
     */
    private static String reclassifyByEvidence(ReCiterArticle reCiterArticle, String phase1Type) {
        String title = reCiterArticle.getArticleTitle() != null ? reCiterArticle.getArticleTitle() : "";
        String titleLower = title.toLowerCase();
        String journalTitle = reCiterArticle.getJournal() != null && reCiterArticle.getJournal().getJournalTitle() != null
                ? reCiterArticle.getJournal().getJournalTitle().toLowerCase() : "";
        boolean hasAbstract = reCiterArticle.getPublicationAbstract() != null
                && !reCiterArticle.getPublicationAbstract().isBlank();
        // Distinguish full IMRaD structure (METHODS + RESULTS) from partial structure
        // (has section labels like BACKGROUND/CONCLUSIONS but no methods/results).
        // Full IMRaD strongly confirms original research. Partial structure is weaker —
        // perspectives, commentaries, and reviews can have BACKGROUND/CONCLUSIONS sections.
        boolean hasMethodsOrResults = hasAbstract && (
                reCiterArticle.getPublicationAbstract().contains("METHODS:")
                || reCiterArticle.getPublicationAbstract().contains("RESULTS:")
                || reCiterArticle.getPublicationAbstract().contains("MATERIALS AND METHODS:")
                || reCiterArticle.getPublicationAbstract().contains("FINDINGS:"));
        boolean hasPartialStructure = !hasMethodsOrResults && hasAbstract && (
                reCiterArticle.getPublicationAbstract().contains("BACKGROUND:")
                || reCiterArticle.getPublicationAbstract().contains("OBJECTIVE:")
                || reCiterArticle.getPublicationAbstract().contains("OBJECTIVES:")
                || reCiterArticle.getPublicationAbstract().contains("CONCLUSIONS:")
                || reCiterArticle.getPublicationAbstract().contains("CONCLUSION:")
                || reCiterArticle.getPublicationAbstract().contains("PURPOSE:")
                || reCiterArticle.getPublicationAbstract().contains("AIM:")
                || reCiterArticle.getPublicationAbstract().contains("AIMS:")
                || reCiterArticle.getPublicationAbstract().contains("INTRODUCTION:"));
        boolean hasStructuredAbstract = hasMethodsOrResults || hasPartialStructure;
        int authorCount = reCiterArticle.getArticleCoAuthors() != null
                && reCiterArticle.getArticleCoAuthors().getAuthors() != null
                ? reCiterArticle.getArticleCoAuthors().getAuthors().size() : 0;

        // Track scores for each candidate type
        int reviewScore = 0;
        int caseReportScore = 0;
        int editorialScore = 0;
        int letterScore = 0;
        int commentScore = 0;
        int erratumScore = 0;
        int retractionScore = 0;

        // --- CommentsCorrections RefType signals ---
        Map<Long, String> refTypes = reCiterArticle.getCommentsCorrectionsRefTypes();
        boolean hasCommentOn = false;
        if (refTypes != null) {
            for (String refType : refTypes.values()) {
                if ("RetractionOf".equals(refType) || "RetractionIn".equals(refType)) {
                    return "Retraction"; // Deterministic
                }
                if ("ErratumFor".equals(refType)) {
                    return "Erratum"; // Deterministic
                }
                if ("CommentOn".equals(refType)) {
                    hasCommentOn = true;
                }
            }
        }

        // --- Retraction signals ---
        if (titleLower.startsWith("retraction:") || titleLower.startsWith("retraction notice")) {
            retractionScore += 3; // Strong
        }

        // --- Erratum signals ---
        if (titleLower.startsWith("erratum") || titleLower.startsWith("corrigendum")
                || titleLower.startsWith("correction to") || titleLower.startsWith("correction:")) {
            erratumScore += 3; // Strong
        }

        // --- Review signals ---
        // Journal title patterns (Strong)
        if (journalTitle.contains("annual review of")
                || journalTitle.startsWith("nature reviews")
                || journalTitle.startsWith("current opinion in")
                || journalTitle.contains("cochrane database of systematic reviews")
                || journalTitle.contains("campbell systematic reviews")
                || journalTitle.endsWith(" reviews")
                || journalTitle.contains("systematic reviews")) {
            reviewScore += 3; // Strong
        }
        // Title contains "systematic review" (Strong)
        if (titleLower.contains("systematic review")) {
            reviewScore += 3; // Strong
        }
        // Title contains "a review" or "review of" without "systematic" (Weak)
        if ((titleLower.contains("a review") || titleLower.contains("review of"))
                && !titleLower.contains("systematic review")) {
            reviewScore += 1; // Weak
        }
        // No structured abstract + long article (Weak) — approximate via page string
        if (!hasStructuredAbstract && hasAbstract) {
            reviewScore += 1; // Weak (at most; can't reliably check page count)
        }

        // --- Case Report signals ---
        if (titleLower.contains("case report")) {
            caseReportScore += 3; // Strong
        }
        if (titleLower.startsWith("a case of") || titleLower.startsWith("a rare case of")) {
            caseReportScore += 3; // Strong
        }
        // Journal title patterns (Moderate)
        if (journalTitle.contains("case reports")) {
            caseReportScore += 2; // Moderate
        }
        // Abstract has CASE section labels (Moderate)
        if (hasAbstract && (reCiterArticle.getPublicationAbstract().contains("CASE PRESENTATION:")
                || reCiterArticle.getPublicationAbstract().contains("CASE REPORT:")
                || reCiterArticle.getPublicationAbstract().contains("CASE:"))) {
            caseReportScore += 2; // Moderate
        }

        // --- Editorial signals ---
        if (titleLower.startsWith("editorial:") || titleLower.startsWith("editor's note")
                || titleLower.endsWith("[editorial]")) {
            editorialScore += 3; // Strong
        }
        // No abstract + single page — weight depends on Phase 1 result.
        // When Phase 1 = "Article" (catch-all, no PubMed type), this signal is Strong (96.6% accuracy).
        // When Phase 1 = "Academic Article" (PubMed assigned "Journal Article"), this signal alone
        // is not strong enough to override PubMed's positive classification (42.4% accuracy).
        boolean isSinglePage = isSinglePageArticle(reCiterArticle.getPages());
        if (!hasAbstract && isSinglePage) {
            if ("Article".equals(phase1Type)) {
                editorialScore += 3; // Strong — no PubMed type to contradict
            } else {
                editorialScore += 1; // Weak — PubMed assigned "Journal Article"; needs other signals to fire
            }
        }
        // No abstract + 1 author (Weak + Weak combined as Moderate)
        if (!hasAbstract && authorCount == 1) {
            editorialScore += 2; // Moderate (combined weak signals)
        }
        // CommentOn RefType — article responds to another article (Moderate)
        if (hasCommentOn) {
            editorialScore += 2; // Moderate — response articles are often editorial in nature
            commentScore += 2;   // Moderate — also evidence for Comment type
        }

        // --- Letter signals ---
        if (titleLower.startsWith("letter to the editor") || titleLower.startsWith("letter:")
                || titleLower.startsWith("correspondence:")) {
            letterScore += 3; // Strong
        }

        // --- Comment signals ---
        if (titleLower.startsWith("comment on") || titleLower.startsWith("response to")
                || titleLower.startsWith("reply to")) {
            commentScore += 3; // Strong
        }

        // --- Confirming Academic Article (prevents weak signals from overriding) ---
        // Full IMRaD (METHODS/RESULTS) = Strong (3): near-definitive evidence of original research.
        // Partial structure (BACKGROUND/CONCLUSIONS only) = Moderate (2): perspectives,
        // commentaries, and reviews can have these labels, so a Strong reclassification
        // signal (3) can override.
        int academicScore = 0;
        if (hasMethodsOrResults) {
            academicScore += 3; // Strong — full IMRaD confirms original research
        } else if (hasPartialStructure) {
            academicScore += 2; // Moderate — partial structure, weaker evidence
        }

        // Find the highest-scoring candidate that meets threshold
        // Tiebreaker priority: Retraction > Erratum > Review > Case Report > Editorial > Letter > Comment
        int threshold = 3;
        String bestType = null;
        int bestScore = 0;

        if (retractionScore >= threshold && retractionScore > bestScore) {
            bestScore = retractionScore; bestType = "Retraction";
        }
        if (erratumScore >= threshold && erratumScore > bestScore) {
            bestScore = erratumScore; bestType = "Erratum";
        }
        if (reviewScore >= threshold && reviewScore > bestScore) {
            bestScore = reviewScore; bestType = "Review";
        }
        if (caseReportScore >= threshold && caseReportScore > bestScore) {
            bestScore = caseReportScore; bestType = "Case Report";
        }
        if (editorialScore >= threshold && editorialScore > bestScore) {
            bestScore = editorialScore; bestType = "Editorial Article";
        }
        if (letterScore >= threshold && letterScore > bestScore) {
            bestScore = letterScore; bestType = "Letter";
        }
        if (commentScore >= threshold && commentScore > bestScore) {
            bestScore = commentScore; bestType = "Comment";
        }

        // Don't override if Academic Article evidence is equally strong or stronger
        if (bestType != null && academicScore >= bestScore) {
            return null;
        }

        return bestType;
    }

    /**
     * Determine if an article is a single-page article based on the pages string.
     * Handles formats: "123-123" (same page), "e12345" (electronic pagination),
     * "S123" (supplement), "123" (single number), and abbreviated end pages
     * like "1262-71" (= 1262-1271, NOT single page).
     */
    private static boolean isSinglePageArticle(String pages) {
        if (pages == null || pages.isBlank()) {
            return false;
        }
        pages = pages.trim();

        // If contains a hyphen, parse as range
        if (pages.contains("-")) {
            // Extract numeric parts, handling prefixes like "S1-S2"
            String[] parts = pages.split("-", 2);
            String startStr = parts[0].trim().replaceAll("^[A-Za-z]+", "");
            String endStr = parts[1].trim().replaceAll("^[A-Za-z]+", "");
            try {
                int start = Integer.parseInt(startStr);
                int end = Integer.parseInt(endStr);
                // Handle abbreviated end pages: "1262-71" → 1262-1271
                if (end < start) {
                    String prefix = startStr.substring(0, startStr.length() - endStr.length());
                    end = Integer.parseInt(prefix + endStr);
                }
                return (end - start + 1) == 1;
            } catch (NumberFormatException e) {
                return false;
            }
        }

        // No hyphen: single-number formats like "e12345", "S123", "123" = 1 page
        return pages.matches("^[A-Za-z]?\\d+$");
    }

    private static void populateFeatures(ReCiterArticle reCiterArticle, String nameIgnoredCoAuthors) {
    	List<String> ignoredCoAuthorNames = Arrays.asList(nameIgnoredCoAuthors.trim().split("\\s*,\\s*"));
    	ReCiterArticleFeatures reCiterArticleFeatures = new ReCiterArticleFeatures();
    	int featureCount = reCiterArticleFeatures.getFeatureCount();
    	
    	//Journal Feature Name
		if (reCiterArticle.getJournal() != null && reCiterArticle.getJournal().exist()) {
			reCiterArticleFeatures.setJournalName(reCiterArticle.getJournal().getJournalTitle());
			featureCount++;
		}
		
		//Co-Author Feature Name
		if (reCiterArticle.getArticleCoAuthors().exist()) {
			for (ReCiterAuthor author : reCiterArticle.getArticleCoAuthors().getAuthors()) {
				if(author != null && author.getAuthorName() != null &&
						author.getAuthorName().getFirstInitial() != null && author.getAuthorName().getLastName() != null && !author.isTargetAuthor() 
						/*&&
					(
					(!author.getAuthorName().getFirstInitial().equals("Y") && !author.getAuthorName().getLastName().equals("Wang"))
					||
					(!author.getAuthorName().getFirstInitial().equals("J") && !author.getAuthorName().getLastName().equals("Wang"))	
					||
					(!author.getAuthorName().getFirstInitial().equals("J") && !author.getAuthorName().getLastName().equals("Smith"))
					||
					(!author.getAuthorName().getFirstInitial().equals("S") && !author.getAuthorName().getLastName().equals("Kim"))
					||
					(!author.getAuthorName().getFirstInitial().equals("S") && !author.getAuthorName().getLastName().equals("Lee"))
					||
					(!author.getAuthorName().getFirstInitial().equals("J") && !author.getAuthorName().getLastName().equals("Lee"))
					)*/
					) {
					boolean addCoAuthor = true;
					for(String ignoredCoAuthorName: ignoredCoAuthorNames) {
						String[] nameArray = ignoredCoAuthorName.split(" ");
						if(nameArray[1] != null && !nameArray[1].isBlank() 
								&&
								author.getAuthorName().getFirstInitial().equalsIgnoreCase(nameArray[1])
								&&
								nameArray[0] != null && !nameArray[0].isBlank()
								&&
								author.getAuthorName().getLastName().equalsIgnoreCase(nameArray[0])) { 
							addCoAuthor = false;
							break;
						}
					}
					if(addCoAuthor) {
						reCiterArticleFeatures.getCoAuthors().add(author.getAuthorName().getFirstInitial() + "." + author.getAuthorName().getLastName());
					}
				}
			}
			if(!reCiterArticleFeatures.getCoAuthors().isEmpty()) {
				featureCount = featureCount + reCiterArticleFeatures.getCoAuthors().size();
			}
		}
		
		//MeshMajor Feature
		if(reCiterArticle.getMeshHeadings() != null && !reCiterArticle.getMeshHeadings().isEmpty()) {
			for(ReCiterArticleMeshHeading meshHeading: reCiterArticle.getMeshHeadings()) {
				if(MeshMajorClusteringStrategy.isMeshMajor(meshHeading) && EngineParameters.getMeshCountMap() != null && EngineParameters.getMeshCountMap().containsKey(meshHeading.getDescriptorName().getDescriptorName()) &&
						EngineParameters.getMeshCountMap().get(meshHeading.getDescriptorName().getDescriptorName()) < 100000L) {
					reCiterArticleFeatures.getMeshMajor().add(meshHeading.getDescriptorName().getDescriptorName());
				}
			}
			if(!reCiterArticleFeatures.getMeshMajor().isEmpty()) {
				featureCount = featureCount + reCiterArticleFeatures.getMeshMajor().size();
			}
		}
		
		if(reCiterArticle.getScopusArticle() != null && reCiterArticle.getScopusArticle().getAuthors().size() == reCiterArticle.getArticleCoAuthors().getNumberOfAuthors()) {
			int i = 0;
			for(ReCiterAuthor author: reCiterArticle.getArticleCoAuthors().getAuthors()) {
				/*if(author.isTargetAuthor()) {
					break;
				}*/
				Author scopusAuthor = reCiterArticle.getScopusArticle().getAuthors().get(i);
				if(scopusAuthor != null && scopusAuthor.getAfids() != null) {
					reCiterArticleFeatures.getAffiliationIds().addAll(scopusAuthor.getAfids());
				}
				i++;			
			}
			if(!reCiterArticleFeatures.getAffiliationIds().isEmpty()) {
				featureCount = featureCount + reCiterArticleFeatures.getAffiliationIds().size();
			}
			
		}
		reCiterArticleFeatures.setFeatureCount(featureCount);
		reCiterArticle.setReCiterArticleFeatures(reCiterArticleFeatures);
    			
    }
}

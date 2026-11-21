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
package reciter.engine;

import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Validated
@Component
@Getter
@Setter
@ConfigurationProperties
@PropertySource("classpath:application.properties")
public class StrategyParameters {
	
	@Value("${reciter.minimumStorageThreshold}")
	private double minimumStorageThreshold;
	
	@Value("${use.scopus.articles}")
	private boolean isUseScopusArticles;
	
    @Value("${strategy.email}")
    private boolean isEmail;
    
    @Value("${strategy.gender}")
    private boolean isGender;

    @Value("${strategy.department}")
    private boolean isDepartment;
    
    @Value("${strategy.journalcategory}")
    private boolean isJournalCategory;

    @Value("${strategy.known.relationship}")
    private boolean isKnownRelationship;

    @Value("${strategy.affiliation}")
    private boolean isAffiliation;

    @Value("${strategy.scopus.common.affiliation}")
    private boolean isScopusCommonAffiliation;

    @Value("${strategy.grant}")
    private boolean isGrant;

    @Value("${strategy.article.size}")
    private boolean isArticleSize;

    @Value("${strategy.persontype}")
    private boolean isPersonType;

    private boolean useGoldStandardEvidence;
    
    @NotEmpty(message = "namesIgnoredCoauthors should not be empty. Its a list of authors separated by commas who are two common in publications. We found these ones which can be ignored \"Wang Y, Wang J, Smith J, Kim S, Lee S, Lee J\". Notice the format is <lastName><space><firstInitial>.")
    @Value("${namesIgnoredCoauthors}")
    private String nameIgnoredCoAuthors;
    
    @NotEmpty(message = "nameScoringStrategy-excludedSuffixes should not be empty. We recommend these suffixes \"Jr,MD PhD,MD-PhD,PhD,MD,III,II,Sr\". You can add more if you like separated by comma.")
    @Value("${nameScoringStrategy-excludedSuffixes}")
    private String nameExcludedSuffixes;
    
    @Value("${nameMatchFirstType.full-exact}")
    private double nameMatchFirstTypeFullExactScore;

    @Value("${nameMatchFirstType.inferredInitials-exact}")
    private double nameMatchFirstTypeInferredInitialsExactScore;

    /**
     * Score for a registered first name of two or more characters that is a
     * left-anchored prefix of the article first name (Paul -> PaulJames, but
     * also Shuo -> Shuofei). Before issue #746 these rows were labelled
     * "full-exact"; the label is now honest but the score deliberately defaults
     * to the same value so relabelling changes no model input. Retuning this
     * number is a separate, retrain-gated change.
     */
    @Value("${nameMatchFirstType.full-prefix:1.852}")
    private double nameMatchFirstTypeFullPrefixScore;

    /**
     * Score for a registered first name of two or more characters that is a
     * right-anchored suffix of the article first name (Cary -> MCary). Same
     * relabel-only contract as {@code full-prefix}.
     */
    @Value("${nameMatchFirstType.full-suffix:1.852}")
    private double nameMatchFirstTypeFullSuffixScore;

    /**
     * Score for a registered first name that sanitizes down to a SINGLE letter
     * and merely begins the article first name ('M.' -> M, matching "Mayra").
     * Semantically this is an initial match and belongs in the
     * inferredInitials-exact bucket (0.441), but the default preserves the
     * pre-#746 value so the relabel ships without moving a model feature.
     */
    @Value("${nameMatchFirstType.inferredInitials-prefix:1.852}")
    private double nameMatchFirstTypeInferredInitialsPrefixScore;

    @Value("${nameMatchFirstType.full-fuzzy}")
    private double nameMatchFirstTypeFullFuzzyScore;

    @Value("${nameMatchFirstType.noMatch}")
    private double nameMatchFirstTypeNoMatchScore;

    @Value("${nameMatchFirstType.full-conflictingAllButInitials}")
    private double nameMatchFirstTypeFullConflictingAllButInitialsScore;

    @Value("${nameMatchFirstType.full-conflictingEntirely}")
    private double nameMatchFirstTypeFullConflictingEntirelyScore;

    @Value("${nameMatchFirstType.nullTargetAuthor-MatchNotAttempted}")
    private double nameMatchFirstTypeNullTargetAuthorMatchNotAttemptedScore;

    @Value("${nameMatchLastType.full-exact}")
    private double nameMatchLastTypeFullExactScore;

    @Value("${nameMatchLastType.full-fuzzy}")
    private double nameMatchLastTypeFullFuzzyScore;

    @Value("${nameMatchLastType.full-conflictingEntirely}")
    private double nameMatchLastTypeFullConflictingEntirelyScore;

    @Value("${nameMatchLastType.nullTargetAuthor-MatchNotAttempted}")
    private double nameMatchLastTypeNullTargetAuthorMatchNotAttemptedScore;

    @Value("${nameMatchMiddleType.inferredInitials-exact}")
    private double nameMatchMiddleTypeInferredInitialsExactScore;

    @Value("${nameMatchMiddleType.full-exact}")
    private double nameMatchMiddleTypeFullExactScore;

    @Value("${nameMatchMiddleType.exact-singleInitial}")
    private double nameMatchMiddleTypeExactSingleInitialScore;

    @Value("${nameMatchMiddleType.noMatch}")
    private double nameMatchMiddleTypeNoMatchScore;

    @Value("${nameMatchMiddleType.full-fuzzy}")
    private double nameMatchMiddleTypeFullFuzzyScore;

    @Value("${nameMatchMiddleType.full-conflictingEntirely}")
    private double nameMatchMiddleTypeFullConflictingEntirelyScore;

    @Value("${nameMatchMiddleType.nullTargetAuthor-MatchNotAttempted}")
    private double nameMatchMiddleTypeNullTargetAuthorMatchNotAttemptedScore;

    @Value("${nameMatchMiddleType.identityNull-MatchNotAttempted}")
    private double nameMatchMiddleTypeIdentityNullMatchNotAttemptedScore;

    @Value("${nameMatchModifier.combinedFirstNameLastName}")
    private double nameMatchModifierCombinedFirstNameLastNameScore;

    @Value("${nameMatchModifier.incorrectOrder}")
    private double nameMatchModifierIncorrectOrderScore;

    @Value("${nameMatchModifier.articleSubstringOfIdentity-lastName}")
    private double nameMatchModifierArticleSubstringOfIdentityLastnameScore;

    @Value("${nameMatchModifier.articleSubstringOfIdentity-firstMiddleName}")
    private double nameMatchModifierArticleSubstringOfIdentityFirstMiddlenameScore;

    @Value("${nameMatchModifier.identitySubstringOfArticle-lastName}")
    private double nameMatchModifierIdentitySubstringOfArticleLastnameScore;

    @Value("${nameMatchModifier.identitySubstringOfArticle-firstName}")
    private double nameMatchModifierIdentitySubstringOfArticleFirstnameScore;

    @Value("${nameMatchModifier.identitySubstringOfArticle-middleName}")
    private double nameMatchModifierIdentitySubstringOfArticleMiddlenameScore;

    @Value("${nameMatchModifier.identitySubstringOfArticle-firstMiddleName}")
    private double nameMatchModifierIdentitySubstringOfArticleFirstMiddlenameScore;

    @Value("${nameMatchModifier.combinedMiddleNameLastName}")
    private double nameMatchModifierCombinedMiddleNameLastNameScore;

    @Value("${strategy.email.emailMatchScore}")
    private double emailMatchScore;

    @Value("${strategy.email.emailNoMatchScore}")
    private double emailNoMatchScore;
    
    @Value("${strategy.email.default.suffixes}")
    private String defaultSuffixes;

    @Value("${strategy.grant.grantMatchScore}")
    private double grantMatchScore;

    @Value("${strategy.knownrelationships.relationshipMatchingScore}")
    private double relationshipMatchingScore;
    
    @Value("${strategy.knownrelationships.relationshipMinimumTotalScore}")
    private double relationshipMinimumTotalScore;
    
    @Value("${strategy.knownrelationships.relationshipNonMatchScore}")
    private double relationshipNonMatchScore;

    @Value("${strategy.knownrelationships.relationshipVerboseMatchModifier}")
    private double relationshipVerboseMatchModifier;

    @Value("${strategy.knownrelationships.relationshipMatchModifier-Mentor}")
    private double relationshipMatchModifierMentor;

    @Value("${strategy.knownrelationships.relationshipMatchModifier-Mentor-SeniorAuthor}")
    private double relationshipMatchModifierMentorSeniorAuthor;
    
    @Value("${strategy.knownrelationships.relationshipMatchModifier-Manager}")
    private double relationshipMatchModifierManager;

    @Value("${strategy.knownrelationships.relationshipMatchModifier-Manager-SeniorAuthor}")
    private double relationshipMatchModifierManagerSeniorAuthor;

    @Value("${strategy.discrepancyDegreeYear.degreeYearDiscrepancyScore}")
    private String degreeYearDiscrepancyScore;

    @Value("${strategy.discrepancyDegreeYear.bachelorYearWeight}")
    private int bachelorYearWeight;

    @Value("${strategy.orgUnitScoringStrategy.organizationalUnitDepartmentMatchingScore}")
    private double organizationalUnitDepartmentMatchingScore;
    
    @Value("${strategy.orgUnitScoringStrategy.organizationalUnitModifier}")
    private String organizationalUnitModifier;

    @Value("${strategy.orgUnitScoringStrategy.organizationalUnitModifierScore}")
    private double organizationalUnitModifierScore;

    @Value("${strategy.orgUnitScoringStrategy.organizationalUnitProgramMatchingScore}")
    private double organizationalUnitProgramMatchingScore;
    
    @Value("${strategy.articleCountScoringStrategy.articleCountThresholdScore}")
    private double articleCountThresholdScore;

    @Value("${strategy.articleCountScoringStrategy.articleCountWeight}")
    private double articleCountWeight;

    @Value("${strategy.personTypeScoringStrategy.personTypeScore-academic-faculty-weillfulltime}")
    private double personTypeScoreAcademicFacultyWeillfulltime;

    @Value("${strategy.personTypeScoringStrategy.personTypeScore-student-md-new-york}")
    private double personTypeScoreStudentMdNewyork;
 
    @Value("${authorshipLikelihoodScore}")
    private double totalArticleScoreStandardizedDefault;

    @Value("${strategy.authorAffiliationScoringStrategy.targetAuthor-institutionalAffiliation-matchType-positiveMatch-individual-score}")
    private double targetAuthorInstAfflMatchTypePositiveIndividualScore;

    @Value("${strategy.authorAffiliationScoringStrategy.targetAuthor-institutionalAffiliation-matchType-positiveMatch-institution-score}")
    private double targetAuthorInstAfflMatchTypePositiveInstitutionScore;

    @Value("${strategy.authorAffiliationScoringStrategy.targetAuthor-institutionalAffiliation-matchType-null-score}")
    private double targetAuthorInstAfflMatchTypeNullScore;

    @Value("${strategy.authorAffiliationScoringStrategy.targetAuthor-institutionalAffiliation-matchType-noMatch-score}")
    private double targetAuthorInstAfflMatchTypeNoMatchScore;

    @Value("${strategy.authorAffiliationScoringStrategy.nonTargetAuthor-institutionalAffiliation-matchType-positiveMatch-individual-score}")
    private double nonTargetAuthorInstAfflMatchTypePositiveIndividualScore;

    @Value("${strategy.authorAffiliationScoringStrategy.nonTargetAuthor-institutionalAffiliation-matchType-positiveMatch-institution-score}")
    private double nonTargetAuthorInstAfflMatchTypePositiveInstitutionScore;

    @Value("${strategy.authorAffiliationScoringStrategy.nonTargetAuthor-institutionalAffiliation-matchType-null-score}")
    private double nonTargetAuthorInstAfflMatchTypeNullScore;

    @Value("${strategy.authorAffiliationScoringStrategy.nonTargetAuthor-institutionalAffiliation-matchType-noMatch-score}")
    private double nonTargetAuthorInstAfflMatchTypeNoMatchScore;

    @Value("${strategy.authorAffiliationScoringStrategy.nonTargetAuthor-institutionalAffiliation-weight}")
    private double nonTargetAuthorInstAfflMatchTypeWeight;

    @Value("${strategy.authorAffiliationScoringStrategy.nonTargetAuthor-institutionalAffiliation-maxScore}")
    private double nonTargetAuthorInstAfflMatchTypeMaxScore;
    
    @Value("${strategy.authorAffiliationScoringStrategy.homeInstitution-keywords}")
    private String instAfflHomeInstKeywords;
    
    @Value("${strategy.authorAffiliationScoringStrategy.homeInstitution-label}")
    private String instAfflInstLabel;
    
    @NotEmpty
    @Value("${strategy.authorAffiliationScoringStrategy.homeInstitution-scopusInstitutionIDs}")
    private String instAfflHomeInstScopusInstIDs;
    
    @Value("${strategy.authorAffiliationScoringStrategy.collaboratingInstitutions-scopusInstitutionIDs}")
    private String instAfflCollaboratingInstScopusInstIDs;
    
    @Value("${strategy.authorAffiliationScoringStrategy.collaboratingInstitutions-keywords}")
    private String instAfflCollaboratingInstKeywords;
    
    @Value("${strategy.authorAffiliationScoringStrategy.institutionStopwords}")
    private String instAfflInstitutionStopwords;
    
    @NotNull
    @Positive(message = "searchStrategy-strict-threshold should be a positive integer. We recommend a number of 1000.")
    @Value("${searchStrategy-strict-threshold}")
    private double searchStrategyStrictThreshold;
    
    @NotNull
    @Positive(message = "searchStrategy-lenient-threshold should be a positive integer. We recommend a number of 2000.")
    @Value("${searchStrategy-lenient-threshold}")
    private double searchStrategyLenientThreshold;
    
    @Value("${strategy.journalCategoryScore.journalSubfieldScore}")
    private double journalSubfieldScore;
    
    @Value("${strategy.journalCategoryScore.journalSubfieldFactorScore}")
    private double journalSubfieldFactorScore;
    
    @Value("${strategy.genderStrategyScore.minimumScore}")
    private double genderStrategyMinScore;
    
    @Value("${strategy.genderStrategyScore.rangeScore}")
    private double genderStrategyRangeScore;

    @Value("${reciter.feature.generator.keywordCountMax}")
    private double keywordCountMax;

    @Value("${reciter.feature.generator.group.uids.maxCount}")
    private int uidsMaxCount;
    
    @Value("${strategy.feedback.score.orcid}")
    private boolean isFeedbackScoreOrcid;
    
    @Value("${strategy.feedback.score.year}")
    private boolean isFeedbackScoreYear;
    
    @Value("${strategy.feedback.score.targetAuthorName}")
    private boolean isFeedbackScoreTargetAuthorName;
    
    @Value("${strategy.feedback.score.orcidCoAuthor}")
    private boolean isFeedbackScoreOrcidCoAuthor;
    
    @Value("${strategy.feedback.score.keyword}")
    private boolean isFeedbackScoreKeyword;

    @Value("${strategy.feedback.score.textSimilarity}")
    private boolean isFeedbackScoreTextSimilarity;

    @Value("${strategy.feedback.score.journalTitleSimilarity}")
    private boolean isFeedbackScoreJournalTitleSimilarity;

    @Value("${strategy.feedback.score.institution}")
    private boolean isFeedbackScoreInstitution;
    
    @Value("${strategy.feedback.score.email}")
    private boolean isFeedbackScoreEmail;
    
    @Value("${strategy.feedback.score.coauthorName}")
    private boolean isFeedbackScoreCoauthorName;
    
    @Value("${strategy.feedback.score.organization}")
    private boolean isFeedbackScoreOrganization;
    
    @Value("${strategy.feedback.score.journal}")
    private boolean isFeedbackScoreJournal;
    
    @Value("${strategy.feedback.score.journalsubfield}")
    private boolean isFeedbackScoreJournalSubField;
    
    @Value("${strategy.feedback.score.journalfield}")
    private boolean isFeedbackScoreJournalField;
    
    @Value("${strategy.feedback.score.journaldomain}")
    private boolean isFeedbackScoreJournalDomain;
    
    @Value("${strategy.feedback.score.cites}")
    private boolean isFeedbackScoreCites;

    @Value("${strategy.feedback.score.bibliographiccoupling}")
    private boolean isFeedbackScoreBibliographicCoupling;

    
    @Value("${strategy.articleCountScoringStrategy.authorCountThreshold}")
    private int authorCountThreshold;
    
    @Value("${strategy.articleCountScoringStrategy.authorCountAdjustmentGamma}")
    private double authorCountAdjustmentGamma;
    
    @Value("${strategy.articleCountScoringStrategy.lnCoefficient}")
    private double lnCoefficient;
    
    @Value("${strategy.articleCountScoringStrategy.constantCoefficient}")
    private double constantCoefficient;
    
    @Value("${strategy.feedback.keywordCountBaseline}")
    private long keywordCountBaseline;
    
    @Value("${strategy.feedback.keywordLogBase}")
    private int keywordLogBase;
    
    @Value("${strategy.feedback.keywordOffset}")
    private double keywordOffset;
    
    @Value("${targetAuthorMissingPenaltyPercent}")
    private double targetAuthorMissingPenaltyPercent;
    
    @Value("${strategy.education.year.discrepancy}")
    private boolean isEducationYearDiscrepancy;

    @Value("${strategy.feedback.informedAbsence.enabled:true}")
    private boolean informedAbsenceEnabled;

    @Value("${strategy.feedback.informedAbsence.scale:10.0}")
    private double informedAbsenceScale;

    @Value("${strategy.feedback.informedAbsence.targetAuthorName.strength:1.0}")
    private double informedAbsenceTargetAuthorNameStrength;

    @Value("${strategy.feedback.informedAbsence.coAuthorName.strength:0.9}")
    private double informedAbsenceCoAuthorNameStrength;

    @Value("${strategy.feedback.informedAbsence.keyword.strength:0.9}")
    private double informedAbsenceKeywordStrength;

    @Value("${strategy.feedback.informedAbsence.journal.strength:0.7}")
    private double informedAbsenceJournalStrength;

    @Value("${strategy.feedback.informedAbsence.journalSubField.strength:0.7}")
    private double informedAbsenceJournalSubFieldStrength;

    @Value("${strategy.feedback.informedAbsence.institution.strength:0.5}")
    private double informedAbsenceInstitutionStrength;

    @Value("${strategy.feedback.informedAbsence.cites.strength:0.5}")
    private double informedAbsenceCitesStrength;

    @Value("${strategy.feedback.informedAbsence.bibliographiccoupling.strength:0.5}")
    private double informedAbsenceBibliographicCouplingStrength;

    @Value("${strategy.feedback.informedAbsence.organization.strength:0.5}")
    private double informedAbsenceOrganizationStrength;

    @Value("${strategy.feedback.informedAbsence.email.strength:0.3}")
    private double informedAbsenceEmailStrength;

    @Value("${strategy.feedback.informedAbsence.orcid.strength:0.3}")
    private double informedAbsenceOrcidStrength;
    
    @Value("${dynamodb.migrations.Analysis.schema.version}")
    private String analysisSchemaVersion;
}

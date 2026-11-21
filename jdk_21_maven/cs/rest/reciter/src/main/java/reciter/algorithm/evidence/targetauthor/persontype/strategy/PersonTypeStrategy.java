package reciter.algorithm.evidence.targetauthor.persontype.strategy;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reciter.algorithm.cluster.article.scorer.ReCiterArticleScorer;
import reciter.algorithm.evidence.targetauthor.AbstractTargetAuthorStrategy;
import reciter.engine.Feature;
import reciter.engine.analysis.evidence.PersonTypeEvidence;
import reciter.model.article.ReCiterArticle;
import reciter.model.identity.Identity;

public class PersonTypeStrategy extends AbstractTargetAuthorStrategy {

	private static final Logger slf4jLogger = LoggerFactory.getLogger(PersonTypeStrategy.class);
	
	@Override
	public double executeStrategy(ReCiterArticle reCiterArticle, Identity identity) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double executeStrategy(List<ReCiterArticle> reCiterArticles, Identity identity) {
		reCiterArticles.forEach(reCiterArticle -> {
			if(identity.getPersonTypes() != null 
					&&
					identity.getPersonTypes().contains("academic-faculty-weillfulltime")) {
				PersonTypeEvidence personTypeEvidence = new PersonTypeEvidence();
				personTypeEvidence.setPersonType("academic-faculty-weillfulltime");
				personTypeEvidence.setPersonTypeScore(ReCiterArticleScorer.strategyParameters.getPersonTypeScoreAcademicFacultyWeillfulltime());
				reCiterArticle.setPersonTypeEvidence(personTypeEvidence);
				//enable if requrired for debugging
				//slf4jLogger.info("Pmid: " + reCiterArticle.getArticleId() + " " + personTypeEvidence.toString());
			} else if(identity.getPersonTypes() != null 
					&&
					identity.getPersonTypes().contains("student-md-new-york")) {
				PersonTypeEvidence personTypeEvidence = new PersonTypeEvidence();
				personTypeEvidence.setPersonType("student-md-new-york");
				personTypeEvidence.setPersonTypeScore(ReCiterArticleScorer.strategyParameters.getPersonTypeScoreStudentMdNewyork());
				reCiterArticle.setPersonTypeEvidence(personTypeEvidence);
				//enable if requrired for debugging
				//slf4jLogger.info("Pmid: " + reCiterArticle.getArticleId() + " " + personTypeEvidence.toString());
			}
			});
		return 0;
	}

	@Override
	public void populateFeature(ReCiterArticle reCiterArticle, Identity identity, Feature feature) {
		// TODO Auto-generated method stub
		
	}
	

}

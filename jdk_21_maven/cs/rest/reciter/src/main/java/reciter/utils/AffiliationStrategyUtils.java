package reciter.utils;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class AffiliationStrategyUtils {
    
    public String constructRegexForStopWords(String instAfflInstitutionStopwords) {
		String regex = "(?i)[-,]|(";
		List<String> stopWords = Arrays.asList(instAfflInstitutionStopwords.trim().split("\\s*,\\s*"));
		for(String stopwWord: stopWords) {
			regex = regex + " \\b" + stopwWord + "\\b|" + "\\b" + stopwWord + "\\b" + " |";  
		}
		regex = regex.replaceAll("\\|$", "") + ")";
		return regex;
    }
}
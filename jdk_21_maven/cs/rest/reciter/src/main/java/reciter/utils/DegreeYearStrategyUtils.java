package reciter.utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import lombok.NoArgsConstructor;

@NoArgsConstructor
@Component
public class DegreeYearStrategyUtils {

    public Map<Double, Double> getDegreeYearDiscrepancyScoreMap(String degreeYearDiscrepancyScore) {
        Map<Double, Double> degreeYearDiscrepancyScoreMap = new HashMap<>();
        List<String> degreeYearScoreList = Arrays.asList(degreeYearDiscrepancyScore.trim().split("\\s*,\\s*"));
        for(String degreeYearScore: degreeYearScoreList) {
            String[] discrepancyToScore = degreeYearScore.trim().split("\\s*\\|\\s*");
            if(discrepancyToScore.length == 2) {
                degreeYearDiscrepancyScoreMap.put(Double.parseDouble(discrepancyToScore[0]), Double.parseDouble(discrepancyToScore[1]));
            }
        }
        return degreeYearDiscrepancyScoreMap;
    }
    
}
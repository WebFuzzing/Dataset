package reciter.utils;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource(locations="classpath:application.properties")
public class DegreeYearStrategyUtilsTest {

	private static final Logger log = LoggerFactory.getLogger(DegreeYearStrategyUtilsTest.class);
	
    @Value("${strategy.discrepancyDegreeYear.degreeYearDiscrepancyScore}")
    private String degreeYearDiscrepancyScore;

    @Test
    public final void testGetDegreeYearDiscrepancyScoreMap() {
        DegreeYearStrategyUtils degreeYearStrategyUtils = new DegreeYearStrategyUtils();
        Map<Double, Double> degreeYearDiscrepancyScoreMap = degreeYearStrategyUtils.getDegreeYearDiscrepancyScoreMap(this.degreeYearDiscrepancyScore);
        log.info(degreeYearDiscrepancyScoreMap.size() + "Size");
        assertEquals("Map size matches", 200, degreeYearDiscrepancyScoreMap.size());
    }
    
}

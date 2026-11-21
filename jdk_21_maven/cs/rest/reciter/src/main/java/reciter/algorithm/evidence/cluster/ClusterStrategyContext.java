package reciter.algorithm.evidence.cluster;

import reciter.algorithm.cluster.model.ReCiterCluster;
import reciter.algorithm.evidence.StrategyContext;

public interface ClusterStrategyContext extends StrategyContext {
	double executeStrategy(ReCiterCluster reCiterCluster);
}

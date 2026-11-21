package reciter.algorithm.evidence.cluster.averageclustering;

import reciter.algorithm.cluster.model.ReCiterCluster;
import reciter.algorithm.evidence.cluster.ClusterStrategy;
import reciter.algorithm.evidence.cluster.ClusterStrategyContext;

public class AverageClusteringStrategyContext implements ClusterStrategyContext {
	
	private final ClusterStrategy strategy;
	
	public AverageClusteringStrategyContext(ClusterStrategy strategy) {
		this.strategy = strategy;
	}

	@Override
	public double executeStrategy(ReCiterCluster reCiterCluster) {
		// TODO Auto-generated method stub
		this.strategy.executeStrategy(reCiterCluster);
		return 0;
	}



}

package org.nasdanika.ai.emf;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.emf.ecore.EStructuralFeature;
import org.nasdanika.graph.emf.EObjectNode;

public class DoubleEStructuralFeatureSimilarityConnectionFactory extends SimilarityConnectionFactory<Double,DoubleEStructuralFeatureConnection, DoubleSimilarityConnection> {
	
	public DoubleSimilarityConnection createSimilarityConnection(
			EObjectNode source, 
			EObjectNode target, 
			boolean visitTargetNode) {
		return new DoubleSimilarityConnection(source, target, visitTargetNode, computeValue(source, target));
	}
	
	protected Double getFeatureWeight(EStructuralFeature feature) {
		return 1.0;
	}

	@Override
	protected Double computeValue(
			EObjectNode source, 
			EObjectNode target,
			List<DoubleEStructuralFeatureConnection> outgoingConnections) {
		
		Optional<Double> outgoingConnectionsWeight = outgoingConnections
			.stream()
			.filter(DoubleEStructuralFeatureConnection.class::isInstance)
			.map(c -> getFeatureWeight(c.getFeature()))
			.filter(Objects::nonNull)
			.reduce((a, b) -> a + b);
		
		if (outgoingConnectionsWeight.isEmpty() || outgoingConnectionsWeight.get() == 0.0) {
			return null;
		}
		
		Optional<Double> outgoingConnectionsValue = outgoingConnections
				.stream()
				.filter(DoubleEStructuralFeatureConnection.class::isInstance)
				.map(c -> {
					Double fw = getFeatureWeight(c.getFeature());
					if (fw == null || fw == 0.0) {
						return null;
					}
					Double cv = c.get();
					if (cv == null || cv == 0.0) {
						return null;
					}
					return fw * cv;
				})
				.filter(Objects::nonNull)
				.reduce((a, b) -> a + b);
		
		if (outgoingConnectionsValue.isEmpty() || outgoingConnectionsValue.get() == 0.0) {
			return null;
		}
				
		return outgoingConnectionsValue.get() / outgoingConnectionsWeight.get(); 		
	}	

}

package org.nasdanika.ai.emf.similarity;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.emf.ecore.EStructuralFeature;
import org.nasdanika.graph.emf.EObjectNode;

public class FloatEStructuralFeatureSimilarityConnectionFactory extends SimilarityConnectionFactory<Float,FloatEStructuralFeatureConnection, FloatSimilarityConnection> {
	
	public FloatSimilarityConnection createSimilarityConnection(
			EObjectNode source, 
			EObjectNode target, 
			boolean visitTargetNode) {
		return new FloatSimilarityConnection(source, target, visitTargetNode, computeValue(source, target));
	}
	
	protected Float getFeatureWeight(EStructuralFeature feature) {
		return 1.0f;
	}

	@Override
	protected Float computeValue(
			EObjectNode source, 
			EObjectNode target,
			List<FloatEStructuralFeatureConnection> outgoingConnections) {
		
		Optional<Float> outgoingConnectionsWeight = outgoingConnections
			.stream()
			.filter(FloatEStructuralFeatureConnection.class::isInstance)
			.map(c -> getFeatureWeight(c.getFeature()))
			.filter(Objects::nonNull)
			.reduce((a, b) -> a + b);
		
		if (outgoingConnectionsWeight.isEmpty() || outgoingConnectionsWeight.get() == 0.0) {
			return null;
		}
		
		Optional<Float> outgoingConnectionsValue = outgoingConnections
				.stream()
				.filter(FloatEStructuralFeatureConnection.class::isInstance)
				.map(c -> {
					Float fw = getFeatureWeight(c.getFeature());
					if (fw == null || fw == 0.0) {
						return null;
					}
					Float cv = c.get();
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

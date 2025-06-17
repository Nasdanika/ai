package org.nasdanika.ai.emf.similarity;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.emf.ecore.EStructuralFeature;
import org.nasdanika.graph.emf.EObjectNode;

public class FloatEStructuralFeatureVectorSimilarityConnectionFactory
	extends EStructuralFeatureVectorSimilarityConnectionFactory<
		Float,
		FloatEStructuralFeatureSimilarity,
		FloatEStructuralFeatureVectorSimilarityConnection, 
		FloatEStructuralFeatureConnection> {
	
	protected Float getFeatureWeight(EStructuralFeature feature) {
		return 1.0f;
	}	
	
	@Override
    protected Float aggregate(Map<EStructuralFeature, Float> featureValues) {
		Optional<Float> totalWeight = featureValues
			.keySet()
			.stream()
			.map(this::getFeatureWeight)
			.filter(Objects::nonNull)
			.reduce((a, b) -> a + b);
		
		if (totalWeight.isEmpty() || totalWeight.get() == 0.0) {
			return null;
		}
		
		Optional<Float> value = featureValues
				.entrySet()
				.stream()
				.map(c -> {
					Float fw = getFeatureWeight(c.getKey());
					if (fw == null || fw == 0.0) {
						return null;
					}
					Float cv = c.getValue();
					if (cv == null || cv == 0.0) {
						return null;
					}
					return fw * cv;
				})
				.filter(Objects::nonNull)
				.reduce((a, b) -> a + b);
		
		if (value.isEmpty() || value.get() == 0.0) {
			return null;
		}
				
		return value.get() / totalWeight.get(); 		
    }

    @Override
    public FloatEStructuralFeatureVectorSimilarityConnection createSimilarityConnection(
    		EObjectNode source,
    		EObjectNode target, 
    		boolean visitTargetNode) {
    	
		return new FloatEStructuralFeatureVectorSimilarityConnection(source, target, visitTargetNode, computeValue(source, target));
    }

    @Override
    protected FloatEStructuralFeatureSimilarity computeValue(
    		EObjectNode source, 
    		EObjectNode target,
    		List<FloatEStructuralFeatureConnection> outgoingConnections) {
    	
    	Map<EStructuralFeature, Float> featureValues = computeFeatureValues(source, target, outgoingConnections);    	
    	Float value = aggregate(featureValues);    	
    	return new FloatEStructuralFeatureSimilarity() {
			
			@Override
			public Float get() {
				return value;
			}
			
			@Override
			public Map<EStructuralFeature, Float> getFeatureSimilarities() {
				return featureValues;
			}
		};
    }
}

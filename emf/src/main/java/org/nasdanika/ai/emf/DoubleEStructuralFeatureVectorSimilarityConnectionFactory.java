package org.nasdanika.ai.emf;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.emf.ecore.EStructuralFeature;
import org.nasdanika.graph.emf.EObjectNode;

public class DoubleEStructuralFeatureVectorSimilarityConnectionFactory 
	extends EStructuralFeatureVectorSimilarityConnectionFactory<
		Double,
		DoubleEStructuralFeatureSimilarity,
		DoubleEStructuralFeatureVectorSimilarityConnection, 
		DoubleEStructuralFeatureConnection> {
	
	protected Double getFeatureWeight(EStructuralFeature feature) {
		return 1.0;
	}	
	
	@Override
    protected Double aggregate(Map<EStructuralFeature, Double> featureValues) {
		Optional<Double> totalWeight = featureValues
			.keySet()
			.stream()
			.map(this::getFeatureWeight)
			.filter(Objects::nonNull)
			.reduce((a, b) -> a + b);
		
		if (totalWeight.isEmpty() || totalWeight.get() == 0.0) {
			return null;
		}
		
		Optional<Double> value = featureValues
				.entrySet()
				.stream()
				.map(c -> {
					Double fw = getFeatureWeight(c.getKey());
					if (fw == null || fw == 0.0) {
						return null;
					}
					Double cv = c.getValue();
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
    public DoubleEStructuralFeatureVectorSimilarityConnection createSimilarityConnection(
    		EObjectNode source,
    		EObjectNode target, 
    		boolean visitTargetNode) {
    	
		return new DoubleEStructuralFeatureVectorSimilarityConnection(source, target, visitTargetNode, computeValue(source, target));
    }

    @Override
    protected DoubleEStructuralFeatureSimilarity computeValue(
    		EObjectNode source, 
    		EObjectNode target,
    		List<DoubleEStructuralFeatureConnection> outgoingConnections) {
    	
    	Map<EStructuralFeature, Double> featureValues = computeFeatureValues(source, target, outgoingConnections);    	
    	Double value = aggregate(featureValues);    	
    	return new DoubleEStructuralFeatureSimilarity() {
			
			@Override
			public Double get() {
				return value;
			}
			
			@Override
			public Map<EStructuralFeature, Double> getFeatureSimilarities() {
				return featureValues;
			}
		};
    }

}

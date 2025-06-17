package org.nasdanika.ai.emf.similarity;

import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.EStructuralFeature;
import org.nasdanika.graph.emf.EObjectNode;

public class DoubleEStructuralFeatureVectorMessageCollectorSimilarityConnectionFactory 
	extends EStructuralFeatureVectorMessageCollectorSimilarityConnectionFactory<
		Double,
		double[],
		DoubleEStructuralFeatureSimilarity,
		DoubleEStructuralFeatureVectorSimilarityConnection> {

	@Override
	protected double[] createAccumulator() {
		return new double[] { 0.0 };
	}

	@Override
	protected void add(Double value, double[] accumulator) {
		if (value != null) {
			synchronized (accumulator) {
				accumulator[0] += value;
			}
		}
	}

	@Override
	protected DoubleEStructuralFeatureVectorSimilarityConnection createConnection(
			EObjectNode source,
			EObjectNode target, 
			Double initialValue, 
			Accumulator<double[]> accumulator) {
		
		DoubleEStructuralFeatureSimilarity value = new DoubleEStructuralFeatureSimilarity() {
			
			@Override
			public Double get() {
				return accumulator.accumulator()[0];
			}
			
			@Override
			public Map<EStructuralFeature, Double> getFeatureSimilarities() {
				return accumulator
						.featureAccumulators()
						.entrySet()
						.stream()
						.collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue()[0]));
			}
		};
				
		return new DoubleEStructuralFeatureVectorSimilarityConnection(source, target, false, value);
	}

}

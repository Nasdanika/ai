package org.nasdanika.ai.emf;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.EStructuralFeature;
import org.nasdanika.graph.emf.EObjectNode;

public abstract class EStructuralFeatureVectorSimilarityConnectionFactory<
	V, 
	S extends EStructuralFeatureSimilarity<V>, 
	C extends EStructuralFeatureVectorSimilarityConnection<V,S>, 
	F extends EStructuralFeatureConnection<V>> extends SimilarityConnectionFactory<S,F,C> {
	
	protected Map<EStructuralFeature, V> computeFeatureValues(
			EObjectNode source, 
			EObjectNode target,
			List<F> outgoingConnections) {
		
		return outgoingConnections
				.stream()
				.filter(FloatEStructuralFeatureConnection.class::isInstance)
				.map(c -> (F) c)
				.filter(c -> testValue(c.get()))
				.collect(Collectors.toMap(EStructuralFeatureConnection::getFeature, EStructuralFeatureConnection::get));
	}	
	
	protected boolean testValue(V value) {
		return value != null;
	}
	
	protected abstract V aggregate(Map<EStructuralFeature, V> featureValues);
	
}

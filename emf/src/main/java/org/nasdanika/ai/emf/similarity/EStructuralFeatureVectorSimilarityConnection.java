package org.nasdanika.ai.emf.similarity;

import org.nasdanika.graph.emf.EObjectNode;

public class EStructuralFeatureVectorSimilarityConnection<V, S extends EStructuralFeatureSimilarity<V>> extends SimilarityConnection<S> {

	public EStructuralFeatureVectorSimilarityConnection(
			EObjectNode source, 
			EObjectNode target, 
			boolean visitTargetNode, 
			S value) {
		
		super(source, target, visitTargetNode, value);
	}	

}

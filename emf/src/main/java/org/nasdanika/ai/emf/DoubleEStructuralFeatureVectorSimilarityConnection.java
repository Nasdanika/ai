package org.nasdanika.ai.emf;

import org.nasdanika.graph.emf.EObjectNode;

public class DoubleEStructuralFeatureVectorSimilarityConnection extends EStructuralFeatureVectorSimilarityConnection<Double, DoubleEStructuralFeatureSimilarity> {

	public DoubleEStructuralFeatureVectorSimilarityConnection(
			EObjectNode source, 
			EObjectNode target, 
			boolean visitTargetNode,
			DoubleEStructuralFeatureSimilarity value) {
		super(source, target, visitTargetNode, value);
	}

}

package org.nasdanika.ai.emf;

import org.nasdanika.graph.emf.EObjectNode;

public class FloatEStructuralFeatureVectorSimilarityConnection extends EStructuralFeatureVectorSimilarityConnection<Float, FloatEStructuralFeatureSimilarity> {

	public FloatEStructuralFeatureVectorSimilarityConnection(
			EObjectNode source, 
			EObjectNode target, 
			boolean visitTargetNode,
			FloatEStructuralFeatureSimilarity value) {
		super(source, target, visitTargetNode, value);
	}

}

package org.nasdanika.ai.emf;

import org.nasdanika.graph.emf.EObjectNode;

public class FloatSimilarityConnection extends SimilarityConnection<Float> {

	public FloatSimilarityConnection(
			EObjectNode source, 
			EObjectNode target, 
			boolean visitTargetNode, 
			Float value) {
		
		super(source, target, visitTargetNode, value);
	}	

}

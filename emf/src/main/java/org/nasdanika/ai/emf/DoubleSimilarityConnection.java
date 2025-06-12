package org.nasdanika.ai.emf;

import org.nasdanika.graph.emf.EObjectNode;

public class DoubleSimilarityConnection extends SimilarityConnection<Double> {

	public DoubleSimilarityConnection(
			EObjectNode source, 
			EObjectNode target, 
			boolean visitTargetNode, 
			Double value) {
		
		super(source, target, visitTargetNode, value);
	}	

}

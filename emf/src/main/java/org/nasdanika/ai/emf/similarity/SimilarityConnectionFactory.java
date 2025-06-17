package org.nasdanika.ai.emf.similarity;

import java.util.List;

import org.nasdanika.graph.emf.EObjectNode;

public abstract class SimilarityConnectionFactory<V,F extends EStructuralFeatureConnection<?>, C extends SimilarityConnection<V>> {
	
	public abstract C createSimilarityConnection(
			EObjectNode source, 
			EObjectNode target, 
			boolean visitTargetNode);
	
	@SuppressWarnings("unchecked")
	protected V computeValue(
			EObjectNode source, 
			EObjectNode target) {
		
		List<F> oc = source
			.getOutgoingConnections()
			.stream()
			.filter(c -> c.getTarget() == target)
			.filter(EStructuralFeatureConnection.class::isInstance)
			.map(c -> (F) c)
			.toList();
		
		return computeValue(source, target, oc);
	}
	
	protected abstract V computeValue(
			EObjectNode source, 
			EObjectNode target,
			List<F> outgoingConnections);	

}

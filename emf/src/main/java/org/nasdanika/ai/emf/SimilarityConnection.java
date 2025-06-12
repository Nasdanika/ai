package org.nasdanika.ai.emf;

import org.nasdanika.graph.emf.EObjectNode;
import org.nasdanika.graph.emf.EObjectValueConnection;

/**
 * Connection value indicates similarity of the target to the source.
 * Similarity doesn't have to be symmetrical. I.e. A -&gt; B similarity can be different from B -&gt; A similarity.
 * If similarity is computed by sending messages then the source would send messages and similarity would be computed
 * from messages received by the target.
 * 
 * Similarity can also be computed from {@link EStructuralFeatureConnection}s.
 * @param <T>
 */
public class SimilarityConnection<T> extends EObjectValueConnection<T> {

	public SimilarityConnection(
			EObjectNode source, 
			EObjectNode target, 
			boolean visitTargetNode, 
			T value) {
		
		super(source, target, visitTargetNode, value);
	}	

}

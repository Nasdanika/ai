package org.nasdanika.ai.emf.similarity;

import org.eclipse.emf.ecore.EStructuralFeature;
import org.nasdanika.graph.emf.EObjectNode;
import org.nasdanika.graph.emf.EObjectValueConnection;

public class EStructuralFeatureConnection<V> extends EObjectValueConnection<V> {

	private EStructuralFeature feature;

	public EStructuralFeatureConnection(
			EObjectNode source, 
			EObjectNode target, 
			boolean visitTargetNode, 
			V value,
			EStructuralFeature feature) {
		super(source, target, visitTargetNode, value);
		this.feature = feature;
	}
	
	public EStructuralFeature getFeature() {
		return feature;
	}

}

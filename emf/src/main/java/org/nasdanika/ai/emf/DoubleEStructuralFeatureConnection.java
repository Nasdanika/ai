package org.nasdanika.ai.emf;

import org.eclipse.emf.ecore.EStructuralFeature;
import org.nasdanika.ai.emf.similarity.EStructuralFeatureConnection;
import org.nasdanika.graph.emf.EObjectNode;

public class DoubleEStructuralFeatureConnection extends EStructuralFeatureConnection<Double> {

	public DoubleEStructuralFeatureConnection(
			EObjectNode source, 
			EObjectNode target, 
			boolean visitTargetNode, 
			Double value,
			EStructuralFeature feature) {
		super(source, target, visitTargetNode, value, feature);
	}

}

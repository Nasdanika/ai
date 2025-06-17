package org.nasdanika.ai.emf.similarity;

import org.eclipse.emf.ecore.EStructuralFeature;
import org.nasdanika.graph.emf.EObjectNode;

public class FloatEStructuralFeatureConnection extends EStructuralFeatureConnection<Float> {

	public FloatEStructuralFeatureConnection(
			EObjectNode source, 
			EObjectNode target, 
			boolean visitTargetNode, 
			Float value,
			EStructuralFeature feature) {
		super(source, target, visitTargetNode, value, feature);
	}

}

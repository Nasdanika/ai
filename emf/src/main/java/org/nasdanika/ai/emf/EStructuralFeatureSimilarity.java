package org.nasdanika.ai.emf;

import org.eclipse.emf.ecore.EStructuralFeature;

/**
 * Value (e.g. integer or float) similarity.  
 * @param <T>
 */
public interface EStructuralFeatureSimilarity<F extends EStructuralFeature, T extends EStructuralFeatureSimilarity<F,?>> extends Similarity<T> {
	
	F getEStructuralFeature();

}

package org.nasdanika.ai.emf;

import java.util.Map;
import java.util.function.Supplier;

import org.eclipse.emf.ecore.EStructuralFeature;

/**
 * Similarity with an aggregated value (can be null) and per-feature values.
 * @param <V>
 */
public interface EStructuralFeatureSimilarity<V> extends Supplier<V> {

	Map<EStructuralFeature,V> getFeatureSimilarities();
	
}

package org.nasdanika.ai.emf;

import org.eclipse.emf.ecore.EObject;

public abstract class EObjectSimilarity<E extends EObject, T extends EObjectSimilarity<E,?>> implements Similarity<T> {
	
	protected E eObj;

	protected EObjectSimilarity(E eObj) {
		this.eObj = eObj;
	}
	
	public E getEObject() {
		return eObj;
	}
	
	
//	protected abstract Collection<EStructuralFeatureSimilarity> - 
	/*
	 * TODO - structural feature similarity type (e.g. float), aggregation
	 * builder for eobject similarity or factory - registers feature similarity functions, creates similarity instances for registered and set features, an option to ignore isSet
	 * embeddings similarity for string attributes, caching embeddings along the way including storing to disk
	 * reference similarity - use graph, resource/resource set to the factory
	 * inheritance/type similarity too
	 * then similarity index
	 */

}

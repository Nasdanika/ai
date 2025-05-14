package org.nasdanika.ai.emf;

/**
 * Value (e.g. integer or float) similarity.  
 * @param <T>
 */
public interface ValueSimilarity<V extends Comparable<V>, T extends ValueSimilarity<V,?>> extends Similarity<T> {
	
	V getValue();
	
	@Override
	default int compareTo(T o) {
		if (o == null) {
			return 1;
		}
		if (o == this) {
			return 0;
		}
		return getValue().compareTo(o.getValue());
	}

}

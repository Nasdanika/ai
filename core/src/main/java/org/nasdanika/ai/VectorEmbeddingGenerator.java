package org.nasdanika.ai;

import java.util.List;

public interface VectorEmbeddingGenerator<S,E> extends EmbeddingGenerator<S,List<List<E>>> {
	
	/**
	 * @return number of dimensions, -1 if unknown
	 */
	default int getDimensions() {
		return -1;
	}

}

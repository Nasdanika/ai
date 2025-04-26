package org.nasdanika.ai;

public interface SearchResult<D extends Comparable<D>> extends Comparable<SearchResult<D>> {
	
	/**
	 * Resource URI
	 * @return
	 */
	String getUri();
	
	/**
	 * Embedding position
	 * @return
	 */
	int getIndex();
	
	/**
	 * Distance from the query vector
	 * @return
	 */
	D getDistance();
	
	@Override
	default int compareTo(SearchResult<D> o) {
		return getDistance().compareTo(o.getDistance());
	}
	
}
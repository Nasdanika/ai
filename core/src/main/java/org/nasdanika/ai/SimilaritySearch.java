package org.nasdanika.ai;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import reactor.core.publisher.Mono;

public interface SimilaritySearch<T,D extends Comparable<D>> {
	
	/**
	 * Finds items closest to the query
	 * @param query
	 * @param numberOfItems Number of items to return
	 * @return
	 */
	List<SearchResult<D>> find(T query, int numberOfItems);
	
	/**
	 * Finds items closest to the query
	 * @param query
	 * @param numberOfItems Number of items to return
	 * @return
	 */
	Mono<List<SearchResult<D>>> findAsync(T query, int numberOfItems);	
	
	default <U> SimilaritySearch<U,D> adapt(Function<U,T> mapper, Function<U, Mono<T>> asyncMapper) {
		return new SimilaritySearch<U,D>() {

			@Override
			public List<SearchResult<D>> find(U query, int numberOfItems) {
				return SimilaritySearch.this.find(mapper.apply(query), numberOfItems);
			}

			@Override
			public Mono<List<SearchResult<D>>> findAsync(U query, int numberOfItems) {
				return asyncMapper
					.apply(query)
					.flatMap(mappedQuery -> SimilaritySearch.this.findAsync(mappedQuery, numberOfItems));
			}
			
		};
	}

	/**
	 * Computes embeddings and uses them for similarity search in a multi-vector search.
	 * @param <D>
	 * @param multiVectorSearch
	 * @param embeddings
	 * @return
	 */
	static <D extends Comparable<D>> SimilaritySearch<String,D> embeddingsSearch(SimilaritySearch<List<List<Float>>,D> multiVectorSearch, Embeddings embeddings) {
		return multiVectorSearch.adapt(
				embeddings::generate, 
				embeddings::generateAsync);
	}
		
	/**
	 * Adapts a single vector search to multi-vector search
	 */
	static <D extends Comparable<D>> SimilaritySearch<List<List<Float>>,D> adapt(SimilaritySearch<List<Float>,D> vectorSearch) {
		return new SimilaritySearch<List<List<Float>>, D>() {

			@Override
			public List<SearchResult<D>> find(List<List<Float>> query, int numberOfItems) {
				List<SearchResult<D>> ret = new ArrayList<>();
				for (List<Float> qe: query) {
					ret.addAll(vectorSearch.find(qe, numberOfItems));
				}
				Collections.sort(ret);
				return ret.size() > numberOfItems ? ret.subList(0, numberOfItems) : ret;
			}

			@SuppressWarnings("unchecked")
			@Override
			public Mono<List<SearchResult<D>>> findAsync(List<List<Float>> query, int numberOfItems) {
				Collection<Mono<List<SearchResult<D>>>> results = new ArrayList<>();
				for (List<Float> qe: query) {
					results.add(vectorSearch.findAsync(qe, numberOfItems));
				}
				return Mono.zip(results, ra -> {
					List<SearchResult<D>> ret = new ArrayList<>();
					for (List<Float> qe: (List<Float>[]) ra) {
						ret.addAll(vectorSearch.find(qe, numberOfItems));
					}
					Collections.sort(ret);
					return ret.size() > numberOfItems ? ret.subList(0, numberOfItems) : ret;
				});
			}
			
		}; 
	}
	

}

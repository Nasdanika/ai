package org.nasdanika.ai;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import com.github.jelmerk.hnswlib.core.Index;
import com.github.jelmerk.hnswlib.core.Item;

import reactor.core.publisher.Mono;

/**
 * 
 * @param <T> Search result and query type, e.g. <code>String</code>
 * @param <D> Distance type, e.g. <code>Float</code>
 */
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
	static <D extends Comparable<D>> SimilaritySearch<String,D> embeddingsSearch(
			SimilaritySearch<List<List<Float>>,D> multiVectorSearch, 
			Embeddings embeddings) {
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
					for (Object qe: (Object[]) ra) {
						ret.addAll((List<SearchResult<D>>) qe);
					}
					Collections.sort(ret);
					return ret.size() > numberOfItems ? ret.subList(0, numberOfItems) : ret;
				});
			}
			
		}; 
	}
		
	/**
	 * Index id - item URI and embedding vector index for URIs with multiple vectors/chunks.
	 */
	record IndexId(String uri, int index) implements Serializable {}
	
	/**
	 * Vector index item
	 */
	record EmbeddingsItem(IndexId id, float[] vector, int dimensions) implements Item<IndexId,float[]> {}
	
	static SimilaritySearch<List<Float>, Float> from(Index<IndexId, float[], EmbeddingsItem, Float> index) {
		return from(index, Function.identity());
	}
	
	static SimilaritySearch<List<Float>, Float> from(
			Index<IndexId, float[], EmbeddingsItem, Float> index,
			Function<float[], float[]> normalizer) {
		
		return new SimilaritySearch<List<Float>, Float>() {
					
			@Override
			public Mono<List<SearchResult<Float>>> findAsync(List<Float> query, int numberOfItems) {
				return Mono.just(find(query, numberOfItems));
			}
			
			@Override
			public List<SearchResult<Float>> find(List<Float> query, int numberOfItems) {
				float[] fVector = new float[query.size()];
				for (int j = 0; j < fVector.length; ++j) {
					fVector[j] = query.get(j);
				}
				if (normalizer != null) {
					fVector = normalizer.apply(fVector);
				}
				List<SearchResult<Float>> ret = new ArrayList<>();
				for (com.github.jelmerk.hnswlib.core.SearchResult<EmbeddingsItem, Float> nearest: index.findNearest(fVector, numberOfItems)) {
					String uri = nearest.item().id().uri();
					int index = nearest.item().id().index();
					Float distance = nearest.distance();
					
					ret.add(new SearchResult<Float>() {
						
						@Override
						public String getUri() {
							return uri;
						}
						
						@Override
						public int getIndex() {
							return index;
						}
						
						@Override
						public Float getDistance() {
							return distance;
						}
						
					});
				}
				return ret;
			}
			
		};		
	}
			
}

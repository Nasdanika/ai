package org.nasdanika.ai;

import java.util.Map;
import java.util.function.Function;

/**
 * Caches image embeddings in a map which can be loaded and saved between runs
 * Uses image digest as caching key
 */
public abstract class MapCachingEmbeddingGenerator<S,E,K> extends CachingEmbeddingGenerator<S,E,K> {
	
	protected Map<K, E> cache;
	
	/**
	 * Computes caching key from source.
	 * @param source
	 * @return
	 */
	protected abstract K computeKey(S source);

	protected MapCachingEmbeddingGenerator(EmbeddingGenerator<S, E> target, Map<K,E> cache) {
		super(target);
		this.cache = cache;
	}
	
	/**
	 * Creates an instance which uses the provided key computer.
	 * @param <S>
	 * @param <E>
	 * @param <K>
	 * @param target
	 * @param cache
	 * @param keyComputer
	 * @return
	 */
	public static <S,E,K> MapCachingEmbeddingGenerator<S,E,K> create(EmbeddingGenerator<S, E> target, Map<K,E> cache, Function<S,K> keyComputer) {
		return new MapCachingEmbeddingGenerator<S, E, K>(target, cache) {

			@Override
			protected K computeKey(S source) {
				return keyComputer.apply(source);
			}
			
		};
	}
	
	@Override
	protected E get(K key) {
		synchronized (cache) {
			return cache.get(key);
		}
	}

	@Override
	protected void put(K key, E value) {
		synchronized (cache) {
			cache.putIfAbsent(key, value);
		}
	}	

	@Override
	public E generate(S input) {
		synchronized (cache) {
			return cache.computeIfAbsent(computeKey(input), d -> target.generate(input));
		}
	}
	
}

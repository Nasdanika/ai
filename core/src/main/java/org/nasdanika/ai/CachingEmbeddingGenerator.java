package org.nasdanika.ai;

import reactor.core.publisher.Mono;

/**
 * Caches image embeddings in a map which can be loaded and saved between runs
 * Uses image digest as caching key
 */
public abstract class CachingEmbeddingGenerator<S,E,K> implements EmbeddingGenerator<S,E> {
	
	protected EmbeddingGenerator<S,E> target;
	
	/**
	 * Computes caching key from source.
	 * @param source
	 * @return
	 */
	protected abstract K computeKey(S source);
	
	protected abstract E get(K key);
	
	protected abstract void put(K key, E value);

	protected CachingEmbeddingGenerator(EmbeddingGenerator<S, E> target) {
		this.target = target;
	}

	@Override
	public Mono<E> generateAsync(S input) {
		K key = computeKey(input);
		return Mono
			.fromSupplier(() -> get(key))
			.switchIfEmpty(target.generateAsync(input).doOnNext(result -> put(key, result)));
	}
	
}

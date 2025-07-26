package org.nasdanika.ai;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Predicate;

import reactor.core.publisher.Mono;

/**
 * Generates an embedding from source. 
 * @param <S>
 * @param <E>
 */
public interface EmbeddingGenerator<S,E> {
	
	/**
	 * {@link EmbeddingGenerator} requirement.
	 * Predicates can be null.
	 */
	record Requirement(
		Class<?> sourceType,
		Class<?> embeddingType,
		Predicate<Class<? extends EmbeddingGenerator<?,?>>> typePredicate,		
		Predicate<EmbeddingGenerator<?,?>> predicate) {}	
	
	default E generate(S input) {
		return generateAsync(input).block();
	}
	
	Mono<E> generateAsync(S input);
	
	/**
	 * Batch generation
	 */
	default Map<S, E> generate(List<S> input) {
		return generateAsync(input).block();
	}
	
	/**
	 * Asynchronous batch generation
	 */
	default Mono<Map<S, E>> generateAsync(List<S> input) {
		List<Mono<Entry<S,E>>> monos = input
			.stream()
			.map(ie -> {
				Mono<E> embMono = generateAsync(ie);
				return embMono.map(emb -> Map.entry(ie, emb));
			})
			.toList();
		
		return Mono.zip(monos, this::combine);
	}
		
	private Map<S, E> combine(Object[] elements) {
		Map<S, E> ret = new LinkedHashMap<>();
		for (Object el: elements) {
			@SuppressWarnings("unchecked")
			Entry<S,E> e = (Entry<S,E>) el;
			ret.put(e.getKey(), e.getValue());
		}		
		return ret;
	}	
	
	default <F> EmbeddingGenerator<S,F> then(EmbeddingGenerator<E,F> next) {
		return new EmbeddingGenerator<S, F>() {

			@Override
			public Mono<F> generateAsync(S source) {
				return EmbeddingGenerator.this.generateAsync(source).flatMap(next::generateAsync);
			}
			
		};
		
	}
	
	default <V> EmbeddingGenerator<V,E> adapt(Function<V,Mono<S>> mapper) {
		
		return new EmbeddingGenerator<V, E>() {

			@Override
			public Mono<E> generateAsync(V source) {
				return mapper.apply(source).flatMap(EmbeddingGenerator.this::generateAsync);
			}
			
		};
		
	}	

}

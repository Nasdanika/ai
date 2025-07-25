package org.nasdanika.ai;

import java.util.function.Function;

import reactor.core.publisher.Mono;

/**
 * Computes pair-wise similarity
 * @param <T>
 * @param <S>
 */
public interface SimilarityComputer<T,S> {
	
	default S compute(T a, T b) {
		return computeAsync(a, b).block();
	}
	
	Mono<S> computeAsync(T a, T b);
		
	default Mono<S> computeAsync(Mono<T> a, Mono<T> b) {
		return Mono.zip(a, b).flatMap(t -> computeAsync(t.getT1(), t.getT2()));
	}
	
	default <V> SimilarityComputer<V,S> adapt(Function<V,Mono<T>> mapper) {
		
		return new SimilarityComputer<V, S>() {

			@Override
			public Mono<S> computeAsync(V a, V b) {
				Mono<T> ma = mapper.apply(a);
				Mono<T> mb = mapper.apply(b);
				return SimilarityComputer.this.computeAsync(ma, mb);
			}
			
			@Override
			public Mono<S> computeAsync(Mono<V> a, Mono<V> b) {
				Mono<T> ma = a.flatMap(mapper);
				Mono<T> mb = b.flatMap(mapper);
				return SimilarityComputer.this.computeAsync(ma, mb);
			}
			
		};
		
	}

}

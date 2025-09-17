package org.nasdanika.ai;

import java.util.Collection;
import java.util.function.Function;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Predictor which can be updated (fine tuned)
 * @param <F>
 * @param <L>
 * @param <E>
 */
public interface UpdatableFittedPredictor<F,L,E> extends FittedPredictor<F,L,E> {
	
	/**
	 * Creates a predictor by fitting a collection of samples.
	 * @param <S>
	 * @param samples
	 * @param featureMapper
	 * @param labelMapper
	 * @return
	 */
	default <S> E update(
			S sample, 
			Function<S,F> featureMapper, 
			Function<S,L> labelMapper) {
		
		return updateAsync(
			Mono.just(sample),
			s -> Mono.fromSupplier(() -> featureMapper.apply(s)),
			s -> Mono.fromSupplier(() -> labelMapper.apply(s))).block();
	}
	
	/**
	 * Creates a predictor by fitting a collection of samples.
	 * @param <S>
	 * @param samples
	 * @param featureMapper
	 * @param labelMapper
	 * @return
	 */
	<S> Mono<E> updateAsync(
			Mono<S> sample, 
			Function<S,Mono<F>> featureMapper, 
			Function<S,Mono<L>> labelMapper);	
	
	// Batch updates
	
	/**
	 * Creates a predictor by fitting a collection of samples.
	 * @param <S>
	 * @param samples
	 * @param featureMapper
	 * @param labelMapper
	 * @return
	 */
	default <S> E update(
			Collection<S> samples, 
			Function<S,F> featureMapper, 
			Function<S,L> labelMapper) {
		
		return updateAsync(
			Flux.fromIterable(samples),
			s -> Mono.fromSupplier(() -> featureMapper.apply(s)),
			s -> Mono.fromSupplier(() -> labelMapper.apply(s))).block();
	}
	
	/**
	 * Creates a predictor by fitting a collection of samples.
	 * @param <S>
	 * @param samples
	 * @param featureMapper
	 * @param labelMapper
	 * @return
	 */
	<S> Mono<E> updateAsync(
			Flux<S> samples, 
			Function<S,Mono<F>> featureMapper, 
			Function<S,Mono<L>> labelMapper);		
	
	// TODO - adapt methods returning updateable fitted predictor

}

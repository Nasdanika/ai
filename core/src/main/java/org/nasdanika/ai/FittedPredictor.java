package org.nasdanika.ai;

import java.util.Collection;
import java.util.function.Function;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * A predictor which is fitted (trained) 
 * @param <F>
 * @param <L>
 * @param <E>
 */
public interface FittedPredictor<F,L,E> extends Predictor<F,L> {
	
	interface Fitter<F,L,E> {
		
		/**
		 * Creates a predictor by fitting a collection of samples.
		 * @param <S>
		 * @param samples
		 * @param featureMapper
		 * @param labelMapper
		 * @return
		 */
		default <S> FittedPredictor<F,L,E> fit(
				Collection<S> samples, 
				Function<S,F> featureMapper, 
				Function<S,L> labelMapper) {
			
			return fitAsync(
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
		<S> Mono<FittedPredictor<F,L,E>> fitAsync(
				Flux<S> samples, 
				Function<S,Mono<F>> featureMapper, 
				Function<S,Mono<L>> labelMapper);
		
		// TODO - adapt methods
		
		// TODO - stacking/composition binary operators to add/subtract labels. 
		// Fit this one, fit the next one on label and prediction difference. 
		// Predict by adding this prediciton to the next prediction.
				
	}	
	
	/**
	 * @return Fitting residual error
	 */
	E getError();
	
	
	// TODO - adapt methods returning fitted predictor, adapt error

}

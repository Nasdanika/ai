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
		 * Creates a predictor by fitting a flux of samples.
		 * @param <S>
		 * @param samples
		 * @param featureMapper
		 * @param labelMapper
		 * @return A mono providing a predictor. 
		 * The mono can publish a predictor before the flux is finished and update the predictor with new
		 * samples from the flux.  
		 */
		<S> Mono<FittedPredictor<F,L,E>> fitAsync(
				Flux<S> samples, 
				Function<S,Mono<F>> featureMapper, 
				Function<S,Mono<L>> labelMapper);
				
		default <G> Fitter<G,L,E> adaptFeature(Function<G,F> featureMapper) {
			
			return new Fitter<G,L,E>() {

				@Override
				public <S> Mono<FittedPredictor<G, L, E>> fitAsync(
						Flux<S> samples, 
						Function<S, Mono<G>> theFeatureMapper,
						Function<S, Mono<L>> labelMapper) {
					
					Function<S, Mono<F>> featureMapperChain = theFeatureMapper.andThen(m -> m.map(featureMapper));
					Mono<FittedPredictor<F, L, E>> fResult = Fitter.this.fitAsync(samples, featureMapperChain, labelMapper);
					return fResult.map(fp -> fp.adaptFeature(featureMapper));
				}
				
				@Override
				public <S> FittedPredictor<G, L, E> fit(
						Collection<S> samples, 
						Function<S, G> theFeatureMapper,
						Function<S, L> labelMapper) {
					
					FittedPredictor<F, L, E> predictor = Fitter.this.fit(samples, theFeatureMapper.andThen(featureMapper), labelMapper);
					return predictor.adaptFeature(featureMapper);
				}
				
			};
						
		}	
			
		default <G> Fitter<G,L,E> adaptFeatureAsync(Function<G,Mono<F>> featureMapper) {			
			
			return new Fitter<G,L,E>() {

				@Override
				public <S> Mono<FittedPredictor<G, L, E>> fitAsync(
						Flux<S> samples, 
						Function<S, Mono<G>> theFeatureMapper,
						Function<S, Mono<L>> labelMapper) {
					
					Function<S, Mono<F>> featureMapperChain = theFeatureMapper.andThen(m -> m.flatMap(featureMapper));
					Mono<FittedPredictor<F, L, E>> fResult = Fitter.this.fitAsync(samples, featureMapperChain, labelMapper);
					return fResult.map(fp -> fp.adaptFeatureAsync(featureMapper));
				}
				
			};
			
		}	
		
		default <M> Fitter<F,M,E> adaptLabel(Function<M,L> fitMapper, Function<L,M> predictMapper) {
			
			return new Fitter<F,M,E>() {
				
				@Override
				public <S> FittedPredictor<F, M, E> fit(
						Collection<S> samples, 
						Function<S, F> featureMapper,
						Function<S, M> labelMapper) {
					
					Function<S, L> fitMapperChain = labelMapper.andThen(fitMapper);
					FittedPredictor<F, L, E> result = Fitter.this.fit(samples, featureMapper, fitMapperChain);
					return result.adaptLabel(predictMapper);
				}

				@Override
				public <S> Mono<FittedPredictor<F, M, E>> fitAsync(
						Flux<S> samples, 
						Function<S, Mono<F>> featureMapper,
						Function<S, Mono<M>> labelMapper) {
					
					Function<S, Mono<L>> fitMapperChain = labelMapper.andThen(m -> m.map(fitMapper));
					Mono<FittedPredictor<F, L, E>> result = Fitter.this.fitAsync(samples, featureMapper, fitMapperChain);
					return result.map(fp -> fp.adaptLabel(predictMapper));
				}
				
			};
						
		}	
			
		default <M> Fitter<F,M,E> adaptLabelAsync(Function<M,Mono<L>> fitMapper, Function<L,Mono<M>> predictMapper) {
			
			return new Fitter<F,M,E>() {

				@Override
				public <S> Mono<FittedPredictor<F, M, E>> fitAsync(
						Flux<S> samples, 
						Function<S, Mono<F>> featureMapper,
						Function<S, Mono<M>> labelMapper) {
					
					Function<S, Mono<L>> fitMapperChain = labelMapper.andThen(m -> m.flatMap(fitMapper));
					Mono<FittedPredictor<F, L, E>> result = Fitter.this.fitAsync(samples, featureMapper, fitMapperChain);
					return result.map(fp -> fp.adaptLabelAsync(predictMapper));
				}
				
			};
			
		}	
				
	}	
	
	/**
	 * @return Fitting residual error
	 */
	E getError();
	
	
	@Override
	default <G> FittedPredictor<G,L,E> adaptFeature(Function<G,F> featureMapper) {
		
		return new FittedPredictor<G,L,E>() {
			
			@Override
			public L predict(G feature) {
				return FittedPredictor.this.predict(featureMapper.apply(feature));
			}

			@Override
			public Mono<L> predictAsync(G feature) {
				return FittedPredictor.this.predictAsync(featureMapper.apply(feature));
			}
			
			@Override
			public E getError() {
				return FittedPredictor.this.getError();
			}
			
		};
		
	}	
		
	default <G> FittedPredictor<G,L,E> adaptFeatureAsync(Function<G,Mono<F>> featureMapper) {
		
		return new FittedPredictor<G,L,E>() {

			@Override
			public Mono<L> predictAsync(G feature) {
				return featureMapper.apply(feature).flatMap(FittedPredictor.this::predictAsync);
			}
			
			@Override
			public E getError() {
				return FittedPredictor.this.getError();
			}
			
		};
		
	}	
	
	default <M> FittedPredictor<F,M,E> adaptLabel(Function<L,M> labelMapper) {
		
		return new FittedPredictor<F,M,E>() {
			
			@Override
			public M predict(F feature) {
				return labelMapper.apply(FittedPredictor.this.predict(feature));
			}

			@Override
			public Mono<M> predictAsync(F feature) {
				return FittedPredictor.this.predictAsync(feature).map(labelMapper);
			}
			
			@Override
			public E getError() {
				return FittedPredictor.this.getError();
			}
			
		};
		
	}	
		
	default <M> FittedPredictor<F,M,E> adaptLabelAsync(Function<L,Mono<M>> labelMapper) {
		
		return new FittedPredictor<F,M,E>() {

			@Override
			public Mono<M> predictAsync(F feature) {				
				return FittedPredictor.this.predictAsync(feature).flatMap(labelMapper);
			}
			
			@Override
			public E getError() {
				return FittedPredictor.this.getError();
			}
			
		};
		
	}	
	
	// TODO Adapt error

}

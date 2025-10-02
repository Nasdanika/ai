package org.nasdanika.ai;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Predicts output from input 
 * @param <F> feature(s) - predictor input
 * @param <L> label - predictor output
 */
public interface Predictor<F,L> {
	
	
	default L predict(F feature) {
		return predictAsync(feature).block();
	}
	
	Mono<L> predictAsync(F input);
	
	record Sample<F,L>(F feature, L label) {}
	
	/**
	 * Batch prediction
	 */
	default List<Sample<F, L>> predict(Collection<F> input) {
		return predictAsync(Flux.fromIterable(input)).block();
	}
	
	/**
	 * Asynchronous batch generation
	 */
	default Mono<List<Sample<F, L>>> predictAsync(Flux<F> input) {
		return input
			.flatMap(ie -> {
				Mono<L> embMono = predictAsync(ie);
				return embMono.map(emb -> new Sample<>(ie, emb));
			})
			.collectList();
	}
	
	default <G> Predictor<G,L> adaptFeature(Function<G,F> featureMapper) {
		
		return new Predictor<G,L>() {
			
			@Override
			public L predict(G feature) {
				return Predictor.this.predict(featureMapper.apply(feature));
			}

			@Override
			public Mono<L> predictAsync(G feature) {
				return Predictor.this.predictAsync(featureMapper.apply(feature));
			}
			
		};
		
	}	
		
	default <G> Predictor<G,L> adaptFeatureAsync(Function<G,Mono<F>> featureMapper) {
		
		return new Predictor<G,L>() {

			@Override
			public Mono<L> predictAsync(G feature) {
				return featureMapper.apply(feature).flatMap(Predictor.this::predictAsync);
			}
			
		};
		
	}	
	
	default <M> Predictor<F,M> adaptLabel(Function<L,M> labelMapper) {
		
		return new Predictor<F,M>() {
			
			@Override
			public M predict(F feature) {
				return labelMapper.apply(Predictor.this.predict(feature));
			}

			@Override
			public Mono<M> predictAsync(F feature) {
				return Predictor.this.predictAsync(feature).map(labelMapper);
			}
			
		};
		
	}	
		
	default <M> Predictor<F,M> adaptLabelAsync(Function<L,Mono<M>> labelMapper) {
		
		return new Predictor<F,M>() {

			@Override
			public Mono<M> predictAsync(F feature) {				
				return Predictor.this.predictAsync(feature).flatMap(labelMapper);
			}
			
		};
		
	}	

}

package org.nasdanika.ai.smile;

import java.util.Collection;
import java.util.function.Function;

import org.nasdanika.ai.FittedPredictor;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class OLSPredictorFitter implements FittedPredictor.Fitter<double[], Double, Double> {

	@Override
	public <S> Mono<FittedPredictor<double[], Double, Double>> fitAsync(
			Flux<S> samples,
			Function<S, Mono<double[]>> featureMapper, 
			Function<S, Mono<Double>> labelMapper) {
		
		// TODO - rewrite properly - mapping instead of blocking
		return Mono.just(fit(
			samples.collectList().block(),	
			s -> featureMapper.apply(s).block(),
			s -> labelMapper.apply(s).block()));
	}
	
	@Override
	public <S> FittedPredictor<double[], Double, Double> fit(
			Collection<S> samples, 
			Function<S, double[]> featureMapper,
			Function<S, Double> labelMapper) {
		
		// TODO Auto-generated method stub
		return FittedPredictor.Fitter.super.fit(samples, featureMapper, labelMapper);
	}

}

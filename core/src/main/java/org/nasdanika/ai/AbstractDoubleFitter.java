package org.nasdanika.ai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.BinaryOperator;
import java.util.function.Function;

import org.nasdanika.ai.FittedPredictor.ErrorComputer;
import org.nasdanika.ai.FittedPredictor.Fitter;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Collects all samples to double[][] features and double[][] labels, 
 * then calls fit(double[][] features, double[][] labels) to fit {@link Function}&lt;double[][],double[][]&gt; 
 * which makes predictions for multiple features at once. 
 * This allows to use a single base class for multiple types of predictors
 */
public abstract class AbstractDoubleFitter implements FittedPredictor.Fitter<double[], double[], Double> {

	private record FeaturesMapped<S>(double[] features, S sample) {}	
	private record Data(double[] features, double[] labels) {}

	@Override
	public <S> Mono<FittedPredictor<double[], double[], Double>> fitAsync(
			Flux<S> samples,
			Function<S, Mono<double[]>> featureMapper, 
			Function<S, Mono<double[]>> labelMapper) {
		
		return samples
			.flatMap(s -> featureMapper.apply(s).map(f -> new FeaturesMapped<>(f,s)))
			.flatMap(fm -> featureMapper.apply(fm.sample()).map(l -> new Data(fm.features(),l)))
			.collectList()
			.map(dataList -> {
				double[][] features = new double[dataList.size()][];
				double[][] labels = new double[dataList.size()][];
				int idx = 0;
				for (Data data: dataList) {
					features[idx] = data.features();
					labels[idx++] = data.labels();
				}
				
				Function<double[][],double[][]> predictor = fit(features, labels);				
				return createPredictor(predictor, features, labels);				
			});
	}
	
	@Override
	public <S> FittedPredictor<double[], double[], Double> fit(
			Collection<S> samples, 
			Function<S, double[]> featureMapper,
			Function<S, double[]> labelMapper) {
		
		double[][] features = new double[samples.size()][];
		double[][] labels = new double[samples.size()][];
		int idx = 0;
		for (S sample: samples) {
			features[idx] = featureMapper.apply(sample);
			labels[idx++] = labelMapper.apply(sample);
		}
		
		Function<double[][],double[][]> predictor = fit(features, labels);
		return createPredictor(predictor, features, labels); 
	}
	
	protected abstract Function<double[][],double[][]> fit(double[][] features, double[][] labels);
	
	protected FittedPredictor<double[], double[], Double> createPredictor(
			Function<double[][],double[][]> predictor, 
			double[][] features,
			double[][] labels) {
		
		Double error = computeError(predictor, features, labels);
		return new FittedPredictor<double[], double[], Double>() {
						
			@Override
			public double[] predict(double[] feature) {
				return predictor.apply(new double[][] { feature })[0];
			}
			
			@Override
			public List<Sample<double[], double[]>> predict(Collection<double[]> input) {
				double[][] inputArray = input.toArray(double[][]::new);
				double[][] results = predictor.apply(inputArray);
				List<Sample<double[], double[]>> ret = new ArrayList<>();
				for (int i = 0; i < inputArray.length; ++i) {
					ret.add(new Sample<>(inputArray[i], results[i]));
				}
				return ret;
			}

			@Override
			public Mono<double[]> predictAsync(double[] input) {
				return Mono.fromSupplier(() -> predict(input));
			}

			@Override
			public Double getError() {
				return error; 
			}
			
		};
		
	}
	
	protected static Double computeError(
			Function<double[][],double[][]> predictor, 
			double[][] features,
			double[][] labels) {
		
		double[][] prediction = predictor.apply(features);
		if (prediction == null) {
			return null;
		}
		double total = 0.0;
		int count = 0;
		for (int i = 0; i < labels.length; ++i) {
			double[] pe = prediction[i];
			if (pe != null) {
				for (int j = 0; j < labels[j].length; ++j) {
					double delta = pe[j] - labels[i][j];
					total += delta * delta;
					++count;
				}
			}
		}
		
		if (count == 0) {
			return null;
		}
		
		return total / count;
	}	
	
	public static Function<double[][], double[]> wrap(Function<double[], Double> predictor) {
		return input -> {
			double[] output = new double[input.length];
			for (int i = 0; i < input.length; ++i) {
				output[i] = predictor.apply(input[i]);
			}
			return output;
		};		
	}
	
	public Fitter<double[], double[], Double> compose(Fitter<double[], double[], Double> other) {
		BinaryOperator<double[]> add = (a,b) -> {
			double[] aCopy = Arrays.copyOf(a, a.length);
			for (int i = 0; i < a.length; ++i) {
				aCopy[i] += b[i];
			}
			return aCopy;
		};
		
		BinaryOperator<double[]> subtract = (a,b) -> {
			double[] aCopy = Arrays.copyOf(a, a.length);
			for (int i = 0; i < a.length; ++i) {
				aCopy[i] -= b[i];
			}
			return aCopy;			
		};
		
		ErrorComputer<double[], double[], Double> errorComputer = new ErrorComputer<double[], double[], Double>() {

			@Override
			public <S> Double computeError(
					Predictor<double[], double[]> predictor, 
					Collection<S> samples,
					Function<S, double[]> featureMapper, 
					Function<S, double[]> labelMapper) {
								
				double[][] features = new double[samples.size()][];				
				double[][] labels = new double[samples.size()][];
				
				int idx = 0;
				for (S sample: samples) {
					features[idx] = featureMapper.apply(sample);
					labels[idx++] = labelMapper.apply(sample);
				}				
				
				Function<double[][], double[][]> errorPredictor = input -> {
					double[][] output = new double[input.length][];
					for (int i = 0; i < input.length; ++i) {
						output[i] = predictor.predict(input[i]);
					}
					return output;
				};
				return AbstractDoubleFitter.computeError(
						errorPredictor, 
						features, 
						labels);
			}
			
		};
		
		return compose(other, add, subtract, errorComputer);
	}
	
		
	// TODO - stacking/composition binary operators to add/subtract labels. 
	// Fit this one, fit the next one on label and prediction difference. 
	// Predict by adding this prediciton to the next prediction.
	// Adapters to a single double result with support of stacking/composition too?
	
}

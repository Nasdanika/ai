package org.nasdanika.ai;

import java.util.Arrays;
import java.util.function.Function;

/**
 * Creates a Function&lt;double[][],double[]> predictor for each label element with features for later elements including
 * labels for earlier. 
 * During prediction earlier predictors outputs are used as inputs for later predictors. 
 * This is essentially how autoregression works. 
 */
public abstract class AbstractRecursiveDoubleFitter extends AbstractDoubleFitter {

	@Override
	protected final Function<double[][], double[][]> fit(double[][] features, double[][] labels) {
		@SuppressWarnings("unchecked")
		Function<double[][], double[]>[] predictors = new Function[labels[0].length];
		for (int i = 0; i < predictors.length; ++i) {
			double[] pLabels = new double[labels.length];
			double[][] pFeatures = new double[features.length][];
			for (int j = 0; j < labels.length; ++j) {
				pLabels[j] = labels[j][i];
				if (i == 0) {
					pFeatures[j] = features[j];
				} else {
					pFeatures[j] = Arrays.copyOf(features[j], features[j].length + i);
					System.arraycopy(labels[j], 0, pFeatures[j], features[j].length, i);
				}
			}
			
			predictors[i] = fit(pFeatures, pLabels);
		}
		
		return input -> {
			double[][] predictions = new double[predictors.length][]; // To be transposed to output
			for (int i = 0; i < predictors.length; ++i) {
				if (i == 0) {
					predictions[i] = predictors[i].apply(input);
				} else {
					double[][] pInput = new double[input.length][];
					for (int j = 0; j < input.length; ++j) {
						pInput[j] = Arrays.copyOf(input[j], input[j].length + i);
						for (int k = 0; k < i; ++k) {
							pInput[j][input[j].length + k] = predictions[k][j];
						}	
					}
					predictions[i] = predictors[i].apply(pInput);
				}
			}
						
			double[][] output = new double[input.length][predictors.length];
			 for (int i = 0; i < output.length; ++i) {
			        for (int j = 0; j < predictors.length; ++j) {
			            output[i][j] = predictions[j][i];
			        }
			    }			
			return output;
		};
	}
	
	protected abstract Function<double[][], double[]> fit(double[][] features, double[] labels);

}

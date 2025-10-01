package org.nasdanika.ai;

import java.util.function.Function;

/**
 * Creates a Function&lt;double[][],double[]> predictor for each label element,
 * predicts label elements using provided predictors and then combines 
 */
public abstract class AbstractMapReduceDoubleFitter extends AbstractDoubleFitter {

	@Override
	protected final Function<double[][], double[][]> fit(double[][] features, double[][] labels) {
		@SuppressWarnings("unchecked")
		Function<double[][], double[]>[] predictors = new Function[labels[0].length];
		for (int i = 0; i < predictors.length; ++i) {
			double[] pLabels = new double[labels.length];
			for (int j = 0; j < labels.length; ++j) {
				pLabels[j] = labels[j][i];
			}
			predictors[i] = fit(features, pLabels);
		}
		
		return input -> {
			double[][] predictions = new double[predictors.length][]; // To be transposed to output
			for (int i = 0; i < predictors.length; ++i) {
				predictions[i] = predictors[i].apply(input);
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

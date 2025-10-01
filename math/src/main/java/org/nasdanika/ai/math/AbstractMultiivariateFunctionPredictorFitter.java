package org.nasdanika.ai.math;

import java.util.function.Function;

import org.apache.commons.math3.analysis.MultivariateFunction;
import org.nasdanika.ai.AbstractMapReduceDoubleFitter;

/**
 * Features and labels of size 1 - only the first element is taken
 */
public abstract class AbstractMultiivariateFunctionPredictorFitter extends AbstractMapReduceDoubleFitter {
	
	protected abstract MultivariateFunction fitFunction(double[][] features, double[] labels);

	@Override
	protected final Function<double[][], double[]> fit(double[][] features, double[] labels) {
		MultivariateFunction func = fitFunction(features, labels);
		
		if (func == null) {
			return null;
		}
		
		return input -> {
			double[] output = new double[input.length];
			for (int i = 0; i < input.length; ++i) {
				output[i] = func.value(input[i]);
			}
			return output;
		};
	}
	
}

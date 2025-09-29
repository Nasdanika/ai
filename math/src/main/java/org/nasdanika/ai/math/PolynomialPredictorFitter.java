package org.nasdanika.ai.math;

import java.util.function.Function;

import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
import org.nasdanika.ai.AbstractDoubleFitter;

/**
 * Features and labels of size 1 - only the first element is taken
 */
public class PolynomialPredictorFitter extends AbstractDoubleFitter {

	@Override
	protected Function<double[][], double[][]> fit(double[][] features, double[][] labels) {
		WeightedObservedPoints wobs = new WeightedObservedPoints();
		for (int i = 0; i < features.length; ++i) {
			wobs.add(features[i][0], labels[i][0]);
		}
		
		PolynomialCurveFitter fitter = PolynomialCurveFitter.create(2);
		double[] params = fitter.fit(wobs.toList());
		PolynomialFunction func = new PolynomialFunction(params);
		
		return input -> {
			double[][] output = new double[input.length][];
			for (int i = 0; i < input.length; ++i) {
				output[i] = new double[] { func.value(input[i][0]) };
			}
			return output;
		};
	}
	
}

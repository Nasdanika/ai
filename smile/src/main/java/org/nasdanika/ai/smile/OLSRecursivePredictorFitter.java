package org.nasdanika.ai.smile;

import java.util.Arrays;
import java.util.function.Function;

import org.nasdanika.ai.AbstractRecursiveDoubleFitter;

import smile.data.DataFrame;
import smile.data.formula.Formula;
import smile.regression.LinearModel;
import smile.regression.OLS;

/**
 * Uses all features and first label
 */
public class OLSRecursivePredictorFitter extends AbstractRecursiveDoubleFitter {

	@Override
	protected Function<double[][], double[]> fit(double[][] features, double[] labels) {
		double[][] data = new double[features.length][];
		for (int i = 0; i < features.length; ++i) {
			data[i] = Arrays.copyOf(features[i], features[i].length + 1);
			data[i][features[i].length] = labels[i];
		}
		
		DataFrame df = DataFrame.of(data);
		Formula formula = Formula.lhs("V" + data[0].length);
		LinearModel ols = OLS.fit(formula, df);
		
		return wrap(ols::predict);
	}
	
}

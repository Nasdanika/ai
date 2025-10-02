package org.nasdanika.ai.smile;

import java.util.Arrays;
import java.util.function.Function;

import org.nasdanika.ai.AbstractDoubleFitter;

import smile.data.DataFrame;
import smile.data.formula.Formula;
import smile.regression.RandomForest;

/**
 * Uses all features and first label
 */
public class RandomForestPredictorFitter  extends AbstractDoubleFitter {

	@Override
	protected Function<double[][], double[][]> fit(double[][] features, double[][] labels) {
		double[][] data = new double[features.length][];
		for (int i = 0; i < features.length; ++i) {
			data[i] = Arrays.copyOf(features[i], features[i].length + 1);
			data[i][features[i].length] = labels[i][0];
		}
		
		DataFrame df = DataFrame.of(data);
		Formula formula = Formula.lhs("V" + data[0].length);
		RandomForest randomForest = RandomForest.fit(formula, df);
		
		return input -> {
			double[] result = randomForest.predict(DataFrame.of(input));
			double[][] output = new double[result.length][];
			for (int i = 0; i < input.length; ++i) {
				output[i] = new double[] { result[i] };
			}
			return output;
		};
	}
}

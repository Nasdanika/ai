package org.nasdanika.ai.smile;

import java.util.Properties;
import java.util.function.Function;

import org.nasdanika.ai.AbstractDoubleFitter;

import smile.regression.MLP;

public class MLPPredictorFitter  extends AbstractDoubleFitter {

	@Override
	protected Function<double[][], double[][]> fit(double[][] features, double[][] labels) {
		double[] label = new double[labels.length];
		for (int i = 0; i < labels.length; ++i) {
			label[i] = labels[i][0];
		}
		
		MLP mlp = MLP.fit(features, label, getProperties());
		
		return input -> {
			double[][] output = new double[input.length][];
			for (int i = 0; i < input.length; ++i) {
				output[i] = new double[] { mlp.predict(input[i]) };
			}
			return output;
		};
	}
	
	protected Properties getProperties() {
		return new Properties();
	}
}

package org.nasdanika.ai.math;

import java.util.List;
import java.util.function.Function;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.fitting.WeightedObservedPoint;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
import org.nasdanika.ai.AbstractMapReduceDoubleFitter;

/**
 * Features and labels of size 1 - only the first element is taken
 */
public abstract class AbstractUnivariateFunctionPredictorFitter extends AbstractMapReduceDoubleFitter {
	
	protected abstract UnivariateFunction fit(List<WeightedObservedPoint> points);

	@Override
	protected final Function<double[][], double[]> fit(double[][] features, double[] labels) {
		WeightedObservedPoints wobs = new WeightedObservedPoints();
		for (int i = 0; i < features.length; ++i) {
			double[] fi = features[i];
			if (fi.length != 1) {
				throw new IllegalArgumentException("Features array shall be of size 1");
			}
			wobs.add(fi[0], labels[i]);
		}
		
		UnivariateFunction func = fit(wobs.toList());
		
		if (func == null) {
			return null;
		}
		
		return wrap(input -> {
			if (input.length != 1) {
				throw new IllegalArgumentException("Input shall be of size 1");
			}
			return func.value(input[0]);
		});
	}
	
}

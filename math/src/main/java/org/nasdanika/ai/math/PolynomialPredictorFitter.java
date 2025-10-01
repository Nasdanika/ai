package org.nasdanika.ai.math;

import java.util.List;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoint;

/**
 * Features and labels of size 1 - only the first element is taken
 */
public class PolynomialPredictorFitter extends AbstractUnivariateFunctionPredictorFitter {
	
	private int degree;

	public PolynomialPredictorFitter(int degree) {
		this.degree = degree;
	}

	@Override
	protected UnivariateFunction fit(List<WeightedObservedPoint> points) {
		PolynomialCurveFitter fitter = PolynomialCurveFitter.create(degree);
		double[] params = fitter.fit(points);
		return new PolynomialFunction(params);
	}
	
}

package org.nasdanika.ai.math.tests;

import java.util.Arrays;

import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
import org.junit.jupiter.api.Test;

public class TestFitting {
	
	@Test
	public void testPolynomialCurveFitter() {
		WeightedObservedPoints wobs = new WeightedObservedPoints();
		wobs.add(1, 2);
		wobs.add(2, 3);
		wobs.add(3, 4);
		wobs.add(4, 5);
		wobs.add(5, 6);
		
		PolynomialCurveFitter fitter = PolynomialCurveFitter.create(2);
		double[] params = fitter.fit(wobs.toList());
		System.out.println(Arrays.toString(params));
		PolynomialFunction func = new PolynomialFunction(params);
		System.out.println(func.value(6));
	}

}

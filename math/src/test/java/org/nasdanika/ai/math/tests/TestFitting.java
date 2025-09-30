package org.nasdanika.ai.math.tests;

import java.util.Arrays;

import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
import org.junit.jupiter.api.Test;
import org.nasdanika.ai.FittedPredictor;
import org.nasdanika.ai.math.PolynomialPredictorFitter;

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
	
	@Test
	public void testPolynomialPredictorFitter() {
		PolynomialPredictorFitter ppf = new PolynomialPredictorFitter();
		
		WeightedObservedPoints wobs = new WeightedObservedPoints();
		wobs.add(1, 2);
		wobs.add(2, 2.9);
		wobs.add(3, 4.1);
		wobs.add(4, 5);
		wobs.add(5, 6);
				
		FittedPredictor<double[], double[], Double> predictor = ppf.fit(wobs.toList(), p -> new double[] { p.getX() }, p -> new double[] { p.getY() });		
		System.out.println(predictor.getError());
		
		double[] prediction = predictor.predict(new double[] { 6.0  });				
		System.out.println(prediction[0]);
	}

}

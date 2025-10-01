package org.nasdanika.ai.tests;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.nasdanika.ai.AbstractRecursiveDoubleFitter;
import org.nasdanika.ai.FittedPredictor;

public class TestPredictors {
	
	@Test
	public void testRecursiveDoubleFitter() {
		// Ignores labels, just sums-up features to produce label		
		AbstractRecursiveDoubleFitter fitter = new AbstractRecursiveDoubleFitter() {
			
			@Override
			protected Function<double[][], double[]> fit(double[][] features, double[] labels) {
				System.out.println("Fit:");
				System.out.println(TestPredictors.toString(features, labels));
				System.out.println();
				
				return wrap(input -> {
					double result = 0;
					for (int j = 0; j < input.length; ++j) {
							result += input[j];
					}
					System.out.println("Predict: " + Arrays.toString(input) + " -> " + result);
					return result;
				});
			}			
			
		};
		
		double[][] data = {
			{ 1, 2, 3, 4.1, 5, 6, 7 },
			{ 2, 3, 4, 4.9, 6, 7, 8 },
			{ 3, 4, 5, 6.1, 7, 8, 9 },
			{ 4, 5, 6, 6.9, 8, 9, 10 },
			{ 5, 6, 7, 8.1, 9, 10, 11 }			
		};				
		
		int labels = 3;

		List<Object> dataList = org.assertj.core.util.Arrays.asList(data);
		FittedPredictor<double[], double[], Double> predictor = fitter.fit(
				dataList, 
				e -> { 
					double[] s = (double[]) e;
					double[] f = new double[s.length - labels];
					System.arraycopy(s, 0, f, 0, f.length);
					return f;
				},
				e -> { 
					double[] s = (double[]) e;
					double[] l = new double[labels];
					System.arraycopy(s, s.length - labels, l, 0, l.length);
					return l;
				});		
		
		System.out.println(predictor.getError());
		
		double[] prediction = predictor.predict(new double[] { 6, 7, 8 });
		System.out.println(prediction[0]);
		
	}
	
	private static String toString(double[][] features, double[] labels) {
		StringBuilder ret = new StringBuilder();
		for (int i = 0; i < features.length; ++i) {
			ret
				.append(Arrays.toString(features[i]))
				.append(" -> " )
				.append(labels[i])
				.append(System.lineSeparator());
		}
		return ret.toString();
	}

}

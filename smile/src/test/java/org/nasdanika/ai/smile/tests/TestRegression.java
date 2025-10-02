package org.nasdanika.ai.smile.tests;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.nasdanika.ai.AbstractDoubleFitter;
import org.nasdanika.ai.FittedPredictor;
import org.nasdanika.ai.FittedPredictor.Fitter;
import org.nasdanika.ai.smile.MLPPredictorFitter;
import org.nasdanika.ai.smile.OLSPredictorFitter;
import org.nasdanika.ai.smile.OLSRecursivePredictorFitter;
import org.nasdanika.ai.smile.RandomForestPredictorFitter;
import org.nasdanika.ai.smile.RegressionTreePredictorFitter;

import smile.base.mlp.Layer;
import smile.base.mlp.OutputFunction;
import smile.data.DataFrame;
import smile.data.formula.Formula;
import smile.math.MathEx;
import smile.regression.LinearModel;
import smile.regression.MLP;
import smile.regression.OLS;
import smile.regression.RandomForest;
import smile.regression.RegressionTree;
import smile.util.function.TimeFunction;

public class TestRegression {
	
	protected double[][] createData() {
		return new double[][] {
			{ 1, 2, 3, 4.1 },
			{ 2, 3, 4, 4.9 },
			{ 3, 4, 5, 6.1 },
			{ 4, 5, 6, 6.9 },
			{ 5, 6, 7, 8.1 }			
		};				
	}
	
	@Test
	public void testRandomForest() {
		DataFrame df = DataFrame.of(createData());
		Formula formula = Formula.lhs("V4");
		RandomForest randomForest = RandomForest.fit(formula, df);
		DataFrame input = DataFrame.of(new double[][] { { 6, 7, 8 } });
		double[] prediction = randomForest.predict(input);
		System.out.println(prediction[0]);		
	}
	
	@Test
	public void testRandomForestPredictorFitter() {
		double[][] data = createData();
		List<Object> dataList = Arrays.asList(data);
		RandomForestPredictorFitter rfpf = new RandomForestPredictorFitter();
		FittedPredictor<double[], double[], Double> predictor = rfpf.fit(
				dataList, 
				e -> { 
					double[] s = (double[]) e;
					double[] f = new double[s.length -1];
					System.arraycopy(s, 0, f, 0, f.length);
					return f;
				},
				e -> new double[] { ((double[]) e)[((double[]) e).length - 1] });		
		
		System.out.println(predictor.getError());
		double[] prediction = predictor.predict(new double[] { 6, 7, 8 });
		System.out.println(prediction[0]);
	}	
	
	@Test
	public void testOLS() {
		DataFrame df = DataFrame.of(createData());
		Formula formula = Formula.lhs("V4");
		LinearModel ols = OLS.fit(formula, df);
		DataFrame input = DataFrame.of(new double[][] { { 6, 7, 8 } });
		
		double[] prediction = ols.predict(input);
		System.out.println(prediction[0]);
		
		System.out.println(ols.predict(new double[] { 7, 8, 9 }));
	}
	
	@Test
	public void testOLSPredictorFitter() {
		double[][] data = createData();
		List<Object> dataList = Arrays.asList(data);
		OLSPredictorFitter olspf = new OLSPredictorFitter();
		FittedPredictor<double[], double[], Double> predictor = olspf.fit(
				dataList, 
				e -> { 
					double[] s = (double[]) e;
					double[] f = new double[s.length -1];
					System.arraycopy(s, 0, f, 0, f.length);
					return f;
				},
				e -> new double[] { ((double[]) e)[((double[]) e).length - 1] });		
		
		System.out.println(predictor.getError());
		
		double[] prediction = predictor.predict(new double[] { 6, 7, 8 });
		System.out.println(prediction[0]);
	}
	
	@Test
	public void testOLSPredictorFitterComposition() {
		double[][] data = createData();
		List<Object> dataList = Arrays.asList(data);
		OLSPredictorFitter olspf = new OLSPredictorFitter();
		
		Fitter<double[], double[], Double> other = new AbstractDoubleFitter() {
			
			@Override
			protected Function<double[][], double[][]> fit(double[][] features, double[][] labels) {
				return input -> {
					double[][] result = new double[labels.length][];
					for (int i = 0; i < result.length; ++i) {
						result[i] = new double[labels[i].length];
						result[i][0] = 0.22;
					}
					return result;
				};
			}
			
		};
		
		FittedPredictor.Fitter<double[], double[], Double> composite = olspf.compose(other);
		
		FittedPredictor<double[], double[], Double> predictor = composite.fit(
				dataList, 
				e -> { 
					double[] s = (double[]) e;
					double[] f = new double[s.length -1];
					System.arraycopy(s, 0, f, 0, f.length);
					return f;
				},
				e -> new double[] { ((double[]) e)[((double[]) e).length - 1] });		
		
		System.out.println(predictor.getError());
		
		double[] prediction = predictor.predict(new double[] { 6, 7, 8 });
		System.out.println(prediction[0]);
	}
		
	@Test
	public void testOLSRecursiveDoubleFitter() {
		OLSRecursivePredictorFitter fitter = new OLSRecursivePredictorFitter();
		
		double[][] data = {
			{ 1, 2, 3, 4.1, 5, 6, 7 },
			{ 2, 3, 4, 4.9, 6, 7, 8 },
			{ 3, 4, 5, 6.1, 7, 8, 9 },
			{ 4, 5, 6, 6.9, 8, 9, 10 },
			{ 5, 6, 7, 8.1, 9, 10, 11 },			
			{ 6, 7, 8, 9.1, 10, 11, 12 },			
			{ 7, 8, 9.1, 10, 11, 12, 13 },			
			{ 8, 9.1, 10, 11, 12, 13, 14 }			
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
		
		double[] prediction = predictor.predict(new double[] { 6, 7, 8, 9 });
		System.out.println(prediction[0]);		
	}	
		
	@Test
	public void testOLSPredictorFitterAdapt() {
		Map<String,Double> ageMap = Map.of(
			"Alice", 25.0,
			"Bob", 30.0,
			"Eve", 35.0,
			"Mallory", 40.0
		);
		
		Map<String,Double> weightMap = Map.of(
				"Alice", 100.0,
				"Bob", 120.0,
				"Eve", 140.0,
				"Mallory", 0.0
			);
				
		OLSPredictorFitter olspf = new OLSPredictorFitter();
		Fitter<String, double[], Double> featureAdapted = olspf.adaptFeature(name -> new double[] { ageMap.get(name) });
		Fitter<String, Double, Double> labelAdapted = featureAdapted.adaptLabel(
				weight -> new double[] { weight },
				wa -> wa[0]
		);
		
		FittedPredictor<String, Double, Double> predictor = labelAdapted.fit(
				List.of("Alice", "Bob", "Eve"),
				Function.identity(),
				weightMap::get);		
		
		System.out.println(predictor.getError());
		
		double prediction = predictor.predict("Mallory");
		System.out.println(prediction);
	}	

	@Test
	@Disabled
	public void testRegressionTree() {
		DataFrame df = DataFrame.of(createData());
		Formula formula = Formula.lhs("V4");
		RegressionTree regressionTree = RegressionTree.fit(formula, df);
		DataFrame input = DataFrame.of(new double[][] { { 6, 7, 8 } });
		double[] prediction = regressionTree.predict(input);
		System.out.println(prediction[0]);		
	}
	
	@Test
	@Disabled
	public void testRegressionTreePredictorFitter() {
		double[][] data = createData();
		List<Object> dataList = Arrays.asList(data);
		RegressionTreePredictorFitter rtpf = new RegressionTreePredictorFitter();
		FittedPredictor<double[], double[], Double> predictor = rtpf.fit(
				dataList, 
				e -> { 
					double[] s = (double[]) e;
					double[] f = new double[s.length -1];
					System.arraycopy(s, 0, f, 0, f.length);
					return f;
				},
				e -> new double[] { ((double[]) e)[((double[]) e).length - 1] });		
		
		double[] prediction = predictor.predict(new double[] { 6, 7, 8 });
		System.out.println(prediction[0]);
	}	
	
	@Test
	public void testMLP() {
		MLP mlp = MLP.fit(
				createData(), 
				new double[] { 5, 6, 7, 8, 9}, 
				new Properties());
		
		double prediction = mlp.predict( new double[] { 6, 7, 8, 9 });
		System.out.println(prediction);		
	}
	
	@Test
	public void testMLPPredictorFitter() {
		double[][] data = createData();
		List<Object> dataList = Arrays.asList(data);
		MLPPredictorFitter mlppf = new MLPPredictorFitter();
		FittedPredictor<double[], double[], Double> predictor = mlppf.fit(
				dataList, 
				e -> { 
					double[] s = (double[]) e;
					double[] f = new double[s.length -1];
					System.arraycopy(s, 0, f, 0, f.length);
					return f;
				},
				e -> new double[] { ((double[]) e)[((double[]) e).length - 1] });		
		
		System.out.println(predictor.getError());
		double[] prediction = predictor.predict(new double[] { 6, 7, 8 });
		System.out.println(prediction[0]);
	}	
	
	@Test
	public void testMLPLowLevel() {
		double[][] features = createData();
		
		MLP mlp = new MLP(
				Layer.input(features[0].length),
				Layer.rectifier(features[0].length * 2),
				Layer.mse(1, OutputFunction.LINEAR));

		try (mlp) {
			mlp.setLearningRate(TimeFunction.constant(0.01));
			mlp.setLearningRate(TimeFunction.constant(0.1));
			
			double[] labels = new double[] { 5, 6, 7, 8, 9};
			
			for (int epoch = 0; epoch < 100; ++epoch) {
				for (int i: MathEx.permutate(features.length)) {
					mlp.update(features[i], labels[i]);
				}
			}
			
			double prediction = mlp.predict( new double[] { 6, 7, 8, 9 });
			System.out.println(prediction);
		}
	}	
	
}

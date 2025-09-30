package org.nasdanika.ai.smile.tests;

import java.util.List;
import java.util.Properties;

import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.nasdanika.ai.FittedPredictor;
import org.nasdanika.ai.smile.MLPPredictorFitter;
import org.nasdanika.ai.smile.OLSPredictorFitter;
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

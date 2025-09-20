package org.nasdanika.ai.smile.tests;

import java.util.Properties;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

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
			{ 1, 2, 3, 4 },
			{ 2, 3, 4, 5 },
			{ 3, 4, 5, 6 },
			{ 4, 5, 6, 7 },
			{ 5, 6, 7, 8 }			
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
	public void testMLP() {
		MLP mlp = MLP.fit(
				createData(), 
				new double[] { 5, 6, 7, 8, 9}, 
				new Properties());
		
		double prediction = mlp.predict( new double[] { 6, 7, 8, 9 });
		System.out.println(prediction);		
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

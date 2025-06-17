package org.nasdanika.ai.emf.similarity;

import org.nasdanika.ai.emf.EObjectGraphMessageProcessor;
import org.nasdanika.ai.emf.EObjectGraphMessageProcessor.Message;
import org.nasdanika.graph.emf.EObjectNode;

/**
 * Collects similarity values from messages and then creates connections.
 */
public class DoubleMessageCollectorSimilarityConnectionFactory extends MessageCollectorSimilarityConnectionFactory<Double,double[],DoubleSimilarityConnection> {

	@Override
	protected double[] createAccumulator(EObjectNode source, EObjectNode target) {
		return new double[] { 0.0 };
	}

	@Override
	protected void add(EObjectNode source, EObjectNode target, Message<Double> input, double[] accumulator) {
		synchronized (accumulator) {
			accumulator[0] += input.value();
		}
	}

	@Override
	protected DoubleSimilarityConnection createConnection(
			EObjectNode source, 
			EObjectNode target, 
			Double initialValue,
			double[] accumulator) {

		double value = accumulator[0];
		if (value == 0.0) {
			return null;
		}
		
		if (initialValue != null && initialValue != 0.0) {
			value = value / initialValue;
		}
		
		return new DoubleSimilarityConnection(source, target, false, value);
	}

}

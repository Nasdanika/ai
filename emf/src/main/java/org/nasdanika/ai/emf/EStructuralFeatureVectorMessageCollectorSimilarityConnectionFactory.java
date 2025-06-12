package org.nasdanika.ai.emf;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.nasdanika.ai.emf.EObjectGraphMessageProcessor.Message;
import org.nasdanika.graph.emf.EObjectNode;
import org.nasdanika.graph.emf.EReferenceConnection;

public abstract class EStructuralFeatureVectorMessageCollectorSimilarityConnectionFactory<
		V,
		A,
		S extends EStructuralFeatureSimilarity<V>, 		
		C extends EStructuralFeatureVectorSimilarityConnection<V,S>> 
	extends MessageCollectorSimilarityConnectionFactory<V,EStructuralFeatureVectorMessageCollectorSimilarityConnectionFactory.Accumulator<A>,C> {
	
	protected record Accumulator<A>(A accumulator, Map<EStructuralFeature,A> featureAccumulators) { }
	
	@Override
	protected Accumulator<A> createAccumulator(EObjectNode source, EObjectNode target) {
		return new Accumulator<A>(createAccumulator(), new ConcurrentHashMap<>());
	}
	
	protected abstract A createAccumulator();
	
	protected abstract void add(V value, A accumulator);
	
	@Override
	protected void add(EObjectNode source, EObjectNode target, Message<V> input, Accumulator<A> accumulator) {
		synchronized (accumulator) {
			add(input.value(), accumulator.accumulator());
			for (Message<V> msg = input; msg != null; msg = msg.parent()) {
				if (msg.sender() instanceof EReferenceConnection) {
					EReference eRef = ((EReferenceConnection) msg.sender()).getReference();
					A featureAccumulator = accumulator.featureAccumulators().computeIfAbsent(eRef, f -> createAccumulator());
					add(msg.value(), featureAccumulator);
				}
			}
		}		
	}

}

package org.nasdanika.ai.emf.similarity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.nasdanika.ai.emf.EObjectGraphMessageProcessor;
import org.nasdanika.ai.emf.EObjectGraphMessageProcessor.Collector;
import org.nasdanika.ai.emf.EObjectGraphMessageProcessor.Message;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.graph.Connection;
import org.nasdanika.graph.Element;
import org.nasdanika.graph.Node;
import org.nasdanika.graph.emf.EObjectNode;

/**
 * Creates similarity connections from information accumulated from messages
 */
public abstract class MessageCollectorSimilarityConnectionFactory<V,A,C extends SimilarityConnection<?>> implements Collector<V> {
			
	Map<EObjectNode, Map<EObjectNode,A>> accumulators = new ConcurrentHashMap<>();
	Map<EObjectNode, V> initialValues = new ConcurrentHashMap<>();
	
	protected abstract A createAccumulator(EObjectNode source, EObjectNode target);
	
	protected abstract void add(EObjectNode source, EObjectNode target, Message<V> input, A accumulator);
	
	@Override
	public void outgoing(Node node, Connection connection, Message<V> input, ProgressMonitor progressMonitor) {
		Element rootSender = input.rootSender().sender();
		if (rootSender instanceof EObjectNode && node instanceof EObjectNode) {
			A accumulator = accumulators.computeIfAbsent(
					(EObjectNode) rootSender, 
					e -> new ConcurrentHashMap<>()).computeIfAbsent((EObjectNode) node, t -> createAccumulator((EObjectNode) rootSender, (EObjectNode) t));
			
			if (accumulator != null) {
				add((EObjectNode) rootSender, (EObjectNode) node, input, accumulator);
			}
		}
	}
	
	@Override
	public void initial(Node node, V value) {
		if (node instanceof EObjectNode && value != null) {
			initialValues.put((EObjectNode) node, value);
		}
	}
	
	@Override
	public void incoming(Node node, Connection connection, Message<V> input, ProgressMonitor progressMonitor) {
		Element rootSender = input.rootSender().sender();
		if (rootSender instanceof EObjectNode && node instanceof EObjectNode) {
			A accumulator = accumulators.computeIfAbsent(
					(EObjectNode) rootSender, 
					e -> new ConcurrentHashMap<>()).computeIfAbsent((EObjectNode) node, t -> createAccumulator((EObjectNode) rootSender, (EObjectNode) t));
						
			if (accumulator != null) {
				add((EObjectNode) rootSender, (EObjectNode) node, input, accumulator);
			}			
		}
	}

	/**
	 * Creates similarity connections from collected values.
	 * @return
	 */
	public Collection<C> createSimilarityConnections() {
		Collection<C> ret = new ArrayList<>();
		for (Entry<EObjectNode, Map<EObjectNode, A>> ae: accumulators.entrySet()) {
			for (Entry<EObjectNode, A> te: ae.getValue().entrySet()) {
				C connection = createConnection(ae.getKey(), te.getKey(), initialValues.get(ae.getKey()), te.getValue());
				if (connection != null) {
					ret.add(connection);
				}
			}
		}
		return ret;
	}
	
	protected abstract C createConnection(EObjectNode source, EObjectNode target, V initialValue, A accumulator);

}

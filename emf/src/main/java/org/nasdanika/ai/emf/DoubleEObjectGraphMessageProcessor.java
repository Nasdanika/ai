package org.nasdanika.ai.emf;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.graph.Connection;
import org.nasdanika.graph.Element;
import org.nasdanika.graph.Node;
import org.nasdanika.graph.emf.EReferenceConnection;
import org.nasdanika.graph.processor.NodeProcessorConfig;
import org.nasdanika.graph.processor.ProcessorInfo;

public class DoubleEObjectGraphMessageProcessor<CS> extends EObjectGraphMessageProcessor<Double,BiFunction<Connection,Boolean,Double>,CS> {
	
	protected DoubleEObjectGraphMessageProcessor(
			boolean parallel, 
			Collection<? extends EObject> entryPoints,
			ProgressMonitor progressMonitor) {
		super(parallel, entryPoints, progressMonitor);
	}

	protected DoubleEObjectGraphMessageProcessor(
			boolean parallel, 
			ResourceSet resourceSet,
			ProgressMonitor progressMonitor) {
		super(parallel, resourceSet, progressMonitor);		
	}
	
	@Override
	protected BiFunction<Connection,Boolean,Double> createNodeProcessorState(
			NodeProcessorConfig<BiFunction<Message<Double>, ProgressMonitor, Void>, BiFunction<Message<Double>, ProgressMonitor, Void>, Object> nodeProcessorConfig,
			boolean parallel,
			BiConsumer<Element, BiConsumer<ProcessorInfo<BiFunction<Message<Double>, ProgressMonitor, Void>, BiFunction<Message<Double>, ProgressMonitor, Void>, Object, BiFunction<Message<Double>, ProgressMonitor, Void>>, ProgressMonitor>> infoProvider,
			Consumer<CompletionStage<?>> endpointWiringStageConsumer,
			Map<Connection, BiFunction<Message<Double>, ProgressMonitor, Void>> incomingEndpoints,
			Map<Connection, BiFunction<Message<Double>, ProgressMonitor, Void>> outgoingEndpoints,
			ProgressMonitor progressMonitor) {
		
		Map<Connection, Double> incomingConnectionWeights = new HashMap<>();
		Map<Connection, Double> outgoingConnectionWeights = new HashMap<>();
		
		Node node = nodeProcessorConfig.getElement();

		Map<EReference, List<EReferenceConnection>> outgoingEReferenceConnections = node
			.getOutgoingConnections()
			.stream()
			.filter(EReferenceConnection.class::isInstance)
			.map(EReferenceConnection.class::cast)
			.collect(Collectors.groupingBy(EReferenceConnection::getReference));
		
		for (Entry<EReference, List<EReferenceConnection>> outgoingEReferenceConnectionEntry: outgoingEReferenceConnections.entrySet()) {
			Double eRefWeight = getOutgoingEReferenceWeight(outgoingEReferenceConnectionEntry.getKey());
			if (eRefWeight != null) {
				double cWeight = eRefWeight / outgoingEReferenceConnectionEntry.getValue().size();
				outgoingEReferenceConnectionEntry.getValue().forEach(c -> outgoingConnectionWeights.put(c, cWeight));
			}
		}
		
		for (Connection oc: node.getOutgoingConnections()) {
			if (!EReferenceConnection.class.isInstance(oc)) {
				Double weight = getOutgoingConnectionWeight(oc);
				if (weight != null) {
					outgoingConnectionWeights.put(oc, weight);
				}
			}
		}
		
		Map<EReference, List<EReferenceConnection>> incomingEReferenceConnections = node
			.getIncomingConnections()
			.stream()
			.filter(EReferenceConnection.class::isInstance)
			.map(EReferenceConnection.class::cast)
			.collect(Collectors.groupingBy(EReferenceConnection::getReference));
		
		for (Entry<EReference, List<EReferenceConnection>> incomingEReferenceConnectionEntry: incomingEReferenceConnections.entrySet()) {
			Double eRefWeight = getOutgoingEReferenceWeight(incomingEReferenceConnectionEntry.getKey());
			if (eRefWeight != null) {
				double cWeight = eRefWeight / incomingEReferenceConnectionEntry.getValue().size();
				incomingEReferenceConnectionEntry.getValue().forEach(c -> incomingConnectionWeights.put(c, cWeight));
			}
		}
						
		for (Connection ic: node.getIncomingConnections()) {
			if (!EReferenceConnection.class.isInstance(ic)) {
				Double weight = getIncomingConnectionWeight(ic);
				if (weight != null) {
					incomingConnectionWeights.put(ic, weight);
				}
			}
		}
		
		double totalWeight = 
				incomingConnectionWeights.values().stream().mapToDouble(Double::doubleValue).sum()
				+ outgoingConnectionWeights.values().stream().mapToDouble(Double::doubleValue).sum(); 
		
		return (c,i) -> {
			Double cw = (i ? incomingConnectionWeights : outgoingConnectionWeights).get(c);
			if (cw == null) {
				return cw;
			}
			return cw / totalWeight;
		};
	}
	
	protected Double getOutgoingConnectionWeight(Connection connection) {
		return 1.0;
	}
	
	protected Double getIncomingConnectionWeight(Connection connection) {
		return 1.0;
	}
	
	protected Double getOutgoingEReferenceWeight(EReference eReference) {
		return 1.0;
	}
	
	protected Double getIncomingEReferenceWeight(EReference eReference) {
		return 1.0;
	}
	
	@Override
	protected Double getConnectionMessageValue(
			BiFunction<Connection,Boolean,Double> state,
			Connection activator, 
			boolean incomingActivator, 
			Node sender,
			Connection recipient, 
			boolean incomingRrecipient, 
			Message<Double> parent, 
			ProgressMonitor progressMonitor) {

		Double connectionWeight = state.apply(recipient, incomingRrecipient);
		return connectionWeight == null ? null : parent.value() * connectionWeight;
	}

}

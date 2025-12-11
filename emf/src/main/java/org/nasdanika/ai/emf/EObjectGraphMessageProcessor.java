package org.nasdanika.ai.emf;

import java.util.Collection;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.common.Transformer;
import org.nasdanika.graph.Connection;
import org.nasdanika.graph.Element;
import org.nasdanika.graph.Node;
import org.nasdanika.graph.emf.EObjectGraphFactory;
import org.nasdanika.graph.emf.EObjectNode;
import org.nasdanika.graph.emf.EReferenceConnection;
import org.nasdanika.graph.emf.EReferenceConnectionQualifier;
import org.nasdanika.graph.processor.ConnectionProcessorConfig;
import org.nasdanika.graph.processor.HandlerType;
import org.nasdanika.graph.processor.NodeProcessorConfig;
import org.nasdanika.graph.processor.ProcessorConfig;
import org.nasdanika.graph.processor.ProcessorConfigFactory;
import org.nasdanika.graph.processor.ProcessorInfo;
import org.nasdanika.graph.processor.function.MessageProcessorFactory;

/**
 * Sends messages between graph nodes. 
 * Can be used to compute similarity.
 * @param <V> Message value type
 */
public class EObjectGraphMessageProcessor<V,NS,CS> {
	
	public record Message<V>(
		Element sender,
		Element recipient,
		V value, 
		Message<V> parent) {
		
		public int depth() {
			return parent == null ? 0 : parent.depth() + 1;
		}
		
		public Message<V> root() {
			return parent == null ? this : parent.root();
		}
				
		/**
		 * @return The first message from the root with non-null sender
		 */
		public Message<V> rootSender() {
			return parent == null || parent.sender() == null ? this : parent.rootSender();
		}		
		
	}
	
	protected Map<EObject, EObjectNode> registry;
	
	public EObjectGraphMessageProcessor(
			boolean parallel, 
			ResourceSet resourceSet, 
			ProgressMonitor progressMonitor) {
		this(
			parallel,
			resourceSet
				.getResources()
				.stream()
				.flatMap(r -> r.getContents().stream())
				.toList(),
			progressMonitor);
	}
	
	/**
	 * Creates a graph and graph processors from entry point objects.
	 * The processors are then used to compute similarity.
	 * @param parallel If true, graph construction is performed in parallel using common pool. 
	 * @param entryPoints
	 * @param progressMonitor
	 */
	public EObjectGraphMessageProcessor(
			boolean parallel, 
			Collection<? extends EObject> entryPoints, 
			ProgressMonitor progressMonitor) {
		
		EObjectGraphFactory eObjectGraphFactory = new EObjectGraphFactory();  
		Transformer<EObject,EObjectNode> graphFactory = new Transformer<>(eObjectGraphFactory); // Reflective node creation using @ElementFactory annotation
		registry = graphFactory.transform(entryPoints, parallel, progressMonitor);
	}

	/**
	 * Endpoint-level test of a message whether to pass it further.
	 * For example, messages with a value below some threshold may be dropped 
	 * or messages with the number of hops higher than some number
	 * @param message
	 * @return
	 */
	protected boolean test(Message<V> message, ProgressMonitor progressMonitor) {
		return true;
	}
	
	/**
	 * Collects similarity updates
	 */
	public interface Collector<V> {
		
		void initial(Node node, V value);
		
		void incoming(Node node, Connection connection, Message<V> input, ProgressMonitor progressMonitor);

		void outgoing(Node node, Connection connection, Message<V> input, ProgressMonitor progressMonitor);
						
	}
	
	protected Message<V> createSourceMessage(
			CS state,
			Connection sender, 
			Message<V> parent, 
			CompletionStage<Void> result,
			ProgressMonitor progressMonitor) {
		
		if (sender instanceof EReferenceConnection) {
			EReferenceConnectionQualifier eRefQ = ((EReferenceConnection) sender).get();
			if (eRefQ.reference().getEOpposite() != null) {
				return null; // One-way for ERefrences with opposites
			}
		}
		
		for (Message<V> ancestor = parent; ancestor != null; ancestor = ancestor.parent()) {
			if (ancestor.sender() == sender && ancestor.recipient() == sender.getSource()) {
				return null; // No double-traversing
			}
		}
		
		V value = getSourceMessageValue(state, sender, parent, progressMonitor);
		return value == null ? null : new Message<>(sender, sender.getSource(), value, parent);
	}
	
	protected V getSourceMessageValue(
			CS state,
			Connection sender, 
			Message<V> parent, 
			ProgressMonitor progressMonitor) {
		
		return parent.value(); // Passing the value AS-IS
	}

	protected Message<V> createTargetMessage(
			CS state,
			Connection sender, 
			Message<V> parent, 
			CompletionStage<Void> result,
			ProgressMonitor progressMonitor) {
				
		for (Message<V> ancestor = parent; ancestor != null; ancestor = ancestor.parent()) {
			if (ancestor.sender() == sender && ancestor.recipient() == sender.getTarget()) {
				return null; // No double-traversing
			}
		}
		
		V value = getTargetMessageValue(state, sender, parent, progressMonitor);
		return value == null ? null : new Message<>(sender, sender.getTarget(), value, parent);
	}
	
	protected V getTargetMessageValue(
			CS state,
			Connection sender, 
			Message<V> parent, 
			ProgressMonitor progressMonitor) {
		
		return parent.value(); // Passing the value AS-IS
	}

	protected Message<V> createConnectionMessage(
			NS state,
			Connection activator, 
			boolean incomingActivator, 
			Node sender,
			Connection recipient, 
			boolean incomingRecipient, 
			Message<V> parent, 
			CompletionStage<Void> result,
			ProgressMonitor progressMonitor) {
		
		for (Message<V> ancestor = parent; ancestor != null; ancestor = ancestor.parent()) {
			if (ancestor.sender() == sender) {
				return null; // No double-traversing
			}
		}	
		
		V value = getConnectionMessageValue(
				state,
				activator, 
				incomingActivator, 
				sender, 
				recipient, 
				incomingRecipient, 
				parent, 
				progressMonitor);
		return value == null ? null : new Message<>(sender, recipient, value, parent);
		
	}
	
	protected V getConnectionMessageValue(
			NS state,
			Connection activator, 
			boolean incomingActivator, 
			Node sender,
			Connection recipient, 
			boolean incomingRrecipient, 
			Message<V> parent, 
			ProgressMonitor progressMonitor) {
		
			return parent.value(); // Passing the value AS-IS		
	}	
	
	protected NS createNodeProcessorState(
			NodeProcessorConfig<BiFunction<Message<V>, ProgressMonitor, Void>, BiFunction<Message<V>, ProgressMonitor, Void>, Object> nodeProcessorConfig,
			boolean parallel,
			BiConsumer<Element, BiConsumer<ProcessorInfo<BiFunction<Message<V>, ProgressMonitor, Void>, BiFunction<Message<V>, ProgressMonitor, Void>, Object, BiFunction<Message<V>, ProgressMonitor, Void>>, ProgressMonitor>> infoProvider,
			Consumer<CompletionStage<?>> endpointWiringStageConsumer,
			Map<Connection, BiFunction<Message<V>, ProgressMonitor, Void>> incomingEndpoints,
			Map<Connection, BiFunction<Message<V>, ProgressMonitor, Void>> outgoingEndpoints,
			ProgressMonitor progressMonitor) {
		
		return null;
	}
	
	protected CS createConnectionProcessorState(
			ConnectionProcessorConfig<BiFunction<Message<V>, ProgressMonitor, Void>, BiFunction<Message<V>, ProgressMonitor, Void>, Object> connectionProcessorConfig,
			boolean parallel,
			BiConsumer<Element, BiConsumer<ProcessorInfo<BiFunction<Message<V>, ProgressMonitor, Void>, BiFunction<Message<V>, ProgressMonitor, Void>, Object, BiFunction<Message<V>, ProgressMonitor, Void>>, ProgressMonitor>> infoProvider,
			Consumer<CompletionStage<?>> endpointWiringStageConsumer, ProgressMonitor progressMonitor) {
		
		return null;
	}	
		
	/**
	 * 
	 * @param parallel
	 * @param rootMessageValue
	 * @param selector
	 * @param messageFilter Can modify the message - return null to stop further processing, create a message with a different value. Can be null.
	 * @param executor
	 * @param collector
	 * @param progressMonitor
	 */
	public void process(
			boolean parallel, 
			V rootMessageValue,
			Function<
				Map<
					Element, 
					ProcessorInfo<
						BiFunction<Message<V>, ProgressMonitor, Void>, 
						BiFunction<Message<V>, ProgressMonitor, Void>, 
						Object, 
						BiFunction<Message<V>, ProgressMonitor, Void>>>, 
				Stream<BiFunction<Message<V>, ProgressMonitor, Void>>> selector,	
			BiFunction<Message<V>, ProgressMonitor, Message<V>> messageFilter,
			Consumer<Runnable> executor,
			Collector<V> collector,
			ProgressMonitor progressMonitor) {		
		
		ProcessorConfigFactory<BiFunction<Message<V>, ProgressMonitor, Void>, BiFunction<Message<V>, ProgressMonitor, Void>, Object> processorConfigFactory = new ProcessorConfigFactory<>() {
			
			@Override
			protected boolean isPassThrough(org.nasdanika.graph.Connection connection) {
				return false;
			}
			
			@Override
			public BiFunction<Message<V>, ProgressMonitor, Void> createEndpoint(
					Element element, 
					BiFunction<Message<V>, ProgressMonitor, Void> handler,
					HandlerType type) {
				
				return (m, p) -> {
					Message<V> msg = messageFilter == null ? m : messageFilter.apply(m, progressMonitor);
					if (test(msg,p)) {
						executor.accept(() -> handler.apply(msg, p));
					}
					return null;
				};
			}

		};	
		
		Transformer<Element, ProcessorConfig<BiFunction<Message<V>, ProgressMonitor, Void>, BiFunction<Message<V>, ProgressMonitor, Void>, Object>> processorConfigTransformer = new Transformer<>(processorConfigFactory);
		Map<Element, ProcessorConfig<BiFunction<Message<V>, ProgressMonitor, Void>, BiFunction<Message<V>, ProgressMonitor, Void>, Object>> configs = processorConfigTransformer.transform(registry.values(), parallel, progressMonitor);
		
		MessageProcessorFactory<Message<V>,Void,Message<V>,Void,NS,CS,Object> processorFactory = new MessageProcessorFactory<>() {

			@Override
			protected Message<V> createSourceMessage(
					CS state,
					Connection sender, 
					Message<V> parent, 
					CompletionStage<Void> result,
					ProgressMonitor progressMonitor) {
				return EObjectGraphMessageProcessor.this.createSourceMessage(state, sender, parent, result, progressMonitor);
			}

			@Override
			protected Message<V> createTargetMessage(
					CS state,
					Connection sender, 
					Message<V> parent, 
					CompletionStage<Void> result,
					ProgressMonitor progressMonitor) {
				return EObjectGraphMessageProcessor.this.createTargetMessage(state, sender, parent, result, progressMonitor);
			}

			@Override
			protected Message<V> toEndpointArgument(Message<V> message) {
				return message;
			}

			@Override
			protected Void createConnectionResult(Void endpointResult) {
				return null;
			}

			@Override
			protected NS createNodeProcessorState(
					NodeProcessorConfig<BiFunction<Message<V>, ProgressMonitor, Void>, BiFunction<Message<V>, ProgressMonitor, Void>, Object> nodeProcessorConfig,
					boolean parallel,
					BiConsumer<Element, BiConsumer<ProcessorInfo<BiFunction<Message<V>, ProgressMonitor, Void>, BiFunction<Message<V>, ProgressMonitor, Void>, Object, BiFunction<Message<V>, ProgressMonitor, Void>>, ProgressMonitor>> infoProvider,
					Consumer<CompletionStage<?>> endpointWiringStageConsumer,
					Map<Connection, BiFunction<Message<V>, ProgressMonitor, Void>> incomingEndpoints,
					Map<Connection, BiFunction<Message<V>, ProgressMonitor, Void>> outgoingEndpoints,
					ProgressMonitor progressMonitor) {
							
				return EObjectGraphMessageProcessor.this.createNodeProcessorState(
						nodeProcessorConfig, 
						parallel, 
						infoProvider, 
						endpointWiringStageConsumer,
						incomingEndpoints, 
						outgoingEndpoints, 
						progressMonitor);
			}

			@Override
			protected CS createConnectionProcessorState(
					ConnectionProcessorConfig<BiFunction<Message<V>, ProgressMonitor, Void>, BiFunction<Message<V>, ProgressMonitor, Void>, Object> connectionProcessorConfig,
					boolean parallel,
					BiConsumer<Element, BiConsumer<ProcessorInfo<BiFunction<Message<V>, ProgressMonitor, Void>, BiFunction<Message<V>, ProgressMonitor, Void>, Object, BiFunction<Message<V>, ProgressMonitor, Void>>, ProgressMonitor>> infoProvider,
					Consumer<CompletionStage<?>> endpointWiringStageConsumer, ProgressMonitor progressMonitor) {

				return EObjectGraphMessageProcessor.this.createConnectionProcessorState(
						connectionProcessorConfig, 
						parallel, 
						infoProvider,
						endpointWiringStageConsumer, 
						progressMonitor);
			}

			@Override
			protected Message<V> createConnectionMessage(
					NS state,
					Connection activator, 
					boolean incomingActivator, 
					Node sender,
					Connection recipient, 
					boolean incomingRecipient, 
					Message<V> parent, 
					CompletionStage<Void> result,
					ProgressMonitor progressMonitor) {
				
				return EObjectGraphMessageProcessor.this.createConnectionMessage(
						state,
						activator, 
						incomingActivator, 
						sender, 
						recipient, 
						incomingRecipient, 
						parent, 
						result, 
						progressMonitor);
			}

			@Override
			protected Void createNodeResult(
					Map<Connection, Void> incomingResults,
					Map<Connection, Void> outgoingResults) {
				return null;
			}
			
			@Override
			protected void onApply(Node node, Message<V> input, ProgressMonitor progressMonitor) {
				collector.initial(node, input.value());
			};
			
			@Override
			protected void onApplyIncoming(Node node, Connection connection, Message<V> input, ProgressMonitor progressMonitor) {
				collector.incoming(node, connection, input, progressMonitor);
			};
			
			@Override
			protected void onApplyOutgoing(Node node, Connection connection, Message<V> input, ProgressMonitor progressMonitor) {
				collector.outgoing(node, connection, input, progressMonitor);
			};
			
		};
		
		Map<Element, ProcessorInfo<BiFunction<Message<V>, ProgressMonitor, Void>, BiFunction<Message<V>, ProgressMonitor, Void>, Object, BiFunction<Message<V>, ProgressMonitor, Void>>> processors = processorFactory.createProcessors(configs.values(), parallel, progressMonitor);		
		Stream<BiFunction<Message<V>, ProgressMonitor, Void>> ps;
		if (selector == null) {		
			ps = processors
					.values()
					.stream()
					.map(pr -> pr.getProcessor());
		} else {
			ps = selector.apply(processors);
		}
		
		if (parallel) {
			ps = ps.parallel();
		}
				
		ps.forEach(mh -> {
			mh.apply(
				new Message<V>(
					null,
					null,
					rootMessageValue, 
					null), 
				progressMonitor);
		});						
		
	}
	
	/**
	 * Processes messages in the calling thread. 
	 * @param rootMessageValue
	 * @param selector
	 * @param messageFilter Can modify the message - return null to stop further processing, create a message with a different value. Can be null.
	 * @param collector
	 * @param progressMonitor
	 */
	public void processes(
			V rootMessageValue,
			Function<Map<Element, ProcessorInfo<BiFunction<Message<V>, ProgressMonitor, Void>, BiFunction<Message<V>, ProgressMonitor, Void>, Object, BiFunction<Message<V>, ProgressMonitor, Void>>>, Stream<BiFunction<Message<V>, ProgressMonitor, Void>>> selector,			
			BiFunction<Message<V>, ProgressMonitor, Message<V>> messageFilter,
			Collector<V> collector, 
			ProgressMonitor progressMonitor) {		
		
		Stack<Runnable> workItems = new Stack<>();		
		process(
				false,
				rootMessageValue,
				selector,
				messageFilter,
				workItems::push, 
				collector, 
				progressMonitor);
		
		while (!workItems.isEmpty()) {
			workItems.pop().run();
		}				
	}
		
	/**
	 * Processes messages in multiple threads. 
	 * @param rootMessageValue
	 * @param selector
	 * @param messageFilter Can modify the message - return null to stop further processing, create a message with a different value. Can be null.
	 * @param collector
	 * @param parallel
	 * @param threads
	 * @param progressMonitor
	 */
	public void process(
			V rootMessageValue,
			Function<Map<Element, ProcessorInfo<BiFunction<Message<V>, ProgressMonitor, Void>, BiFunction<Message<V>, ProgressMonitor, Void>, Object, BiFunction<Message<V>, ProgressMonitor, Void>>>, Stream<BiFunction<Message<V>, ProgressMonitor, Void>>> selector,			
			BiFunction<Message<V>, ProgressMonitor, Message<V>> messageFilter,
			Collector<V> collector,
			boolean parallel,
			int threads,			
			ProgressMonitor progressMonitor) {
				
		BlockingQueue<Runnable> processingQueue = new PriorityBlockingQueue<>();		
		ExecutorService executorService = new ThreadPoolExecutor(0, threads, 60L, TimeUnit.SECONDS, processingQueue) {
			
			AtomicInteger taskCounter = new AtomicInteger();
			
			class ComparableFutureTask<T> extends FutureTask<T> implements Comparable<ComparableFutureTask<T>> {
				
				int id = taskCounter.incrementAndGet();

				@Override
				public int compareTo(ComparableFutureTask<T> o) {
					return o.id - id; // tasks which were submitted later shall execute first - depth first
				}

				protected ComparableFutureTask(Callable<T> callable) {
					super(callable);
				}

				protected ComparableFutureTask(Runnable runnable, T result) {
					super(runnable, result);
				}
				
			}				
						
			@Override
			protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {								
				return new ComparableFutureTask<T>(runnable, value);
			}
			
			@Override
			protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
				return new ComparableFutureTask<T>(callable);
			}
			
		};						
		
		process(
				false,
				rootMessageValue,
				selector,
				messageFilter,
				executorService::submit, 
				collector, 
				progressMonitor);
		
		Runnable workItem;
		while ((workItem = processingQueue.poll()) != null) {
			workItem.run();
		}				
		
		executorService.shutdown();		
	}

}

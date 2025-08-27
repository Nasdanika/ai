package org.nasdanika.ai.drawio;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.nasdanika.drawio.Connection;
import org.nasdanika.drawio.Element;
import org.nasdanika.drawio.LayerElement;
import org.nasdanika.drawio.ModelElement;
import org.nasdanika.graph.processor.NodeProcessorInfo;
import org.nasdanika.graph.processor.ProcessorInfo;
import org.nasdanika.graph.processor.RegistryEntry;

public abstract class LayerElementProcessor<T extends LayerElement> extends LinkTargetProcessor<T> {
	
	protected Map<ModelElement, ProcessorInfo<BaseProcessor<?>>> childInfos = new ConcurrentHashMap<>();
	
	protected Map<Connection, CompletableFuture<ConnectionProcessor>> outgoingEndpoints = new ConcurrentHashMap<>();	
		
	@RegistryEntry("#element.linkTarget == #this")
	public BaseProcessor<?> linkTargetProcessor;		
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void addReferrer(ModelElement referrer) {
		super.addReferrer(referrer);		
		for (Element child: referrer.getChildren()) {
			if (child instanceof ModelElement) {
				ProcessorInfo<BaseProcessor<?>> ci = registry.get(child);
				if (ci != null /* && ci.getProcessor() != null */) {
					childInfos.put((ModelElement) child, ci);
				}
			}
		}		
		
		ProcessorInfo<BaseProcessor<?>> referrerInfo = registry.get(referrer);
		if (referrerInfo instanceof NodeProcessorInfo) {
			NodeProcessorInfo<BaseProcessor<?>, BaseProcessor<?>, BaseProcessor<?>> npi = (NodeProcessorInfo<BaseProcessor<?>, BaseProcessor<?>, BaseProcessor<?>>) referrerInfo;
			outgoingEndpoints.putAll((Map) npi.getOutgoingEndpoints());			
		}
	}
	
	public LayerElementProcessor(DrawioProcessorFactory factory) {
		super(factory);
	}
	
	// TODO - linking - link target

}

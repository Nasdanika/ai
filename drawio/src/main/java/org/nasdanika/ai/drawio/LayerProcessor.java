package org.nasdanika.ai.drawio;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.nasdanika.common.MapCompoundSupplier;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.common.Supplier;
import org.nasdanika.common.Util;
import org.nasdanika.drawio.Connection;
import org.nasdanika.drawio.Layer;
import org.nasdanika.drawio.LayerElement;
import org.nasdanika.drawio.Node;
import org.nasdanika.graph.processor.ChildProcessors;
import org.nasdanika.graph.processor.ProcessorElement;
import org.nasdanika.graph.processor.ProcessorInfo;
import org.nasdanika.models.app.Label;
import org.nasdanika.models.app.graph.WidgetFactory;

public class LayerProcessor extends BaseProcessor<Layer> {
	
	public LayerProcessor(DrawioProcessorFactory factory) {
		super(factory);
	}

	@ChildProcessors
	public Map<LayerElement, ProcessorInfo<WidgetFactory>> childInfos;
	
	
	protected boolean isLogicalChild(LayerElement layerElement) {
		if (layerElement instanceof Node) {
			return true;
		}
		if (layerElement instanceof Connection) {
			Node source = ((Connection) layerElement).getSource();
			if (source != null) {
				return source == element;
			}
			return element == layerElement.getParent();
		}
		return false;
	}		
		
	@SuppressWarnings("resource")
	@Override
	public Supplier<Collection<Label>> createLabelsSupplier() {
		MapCompoundSupplier<LayerElement, Collection<Label>> childLabelsSupplier = new MapCompoundSupplier<>("Child labels supplier");
		for (Entry<LayerElement, ProcessorInfo<WidgetFactory>> ce: childInfos.entrySet()) {
			if (isLogicalChild(ce.getKey())) {
				WidgetFactory processor = ce.getValue().getProcessor();
				if (processor != null) {
					childLabelsSupplier.put(ce.getKey(), processor.createLabelsSupplier());
				}
			}
		}
		
		return childLabelsSupplier.then(this::createLayerLabels);
	}
	
	@ProcessorElement
	@Override
	public void setElement(Layer element) {
		super.setElement(element);
		uri = URI.createURI(Util.isBlank(element.getLabel()) ? getIndexName() : element.getId() + "/" + getIndexName());
	}
	
	@Override
	public void resolve(URI base, ProgressMonitor progressMonitor) {
		super.resolve(base, progressMonitor);
		for (ProcessorInfo<WidgetFactory> cpi: childInfos.values()) {
			WidgetFactory processor = cpi.getProcessor();
			if (processor != null) {
				processor.resolve(uri, progressMonitor);
			}
		}
	}
	
	@Override
	public URI getActionURI(ProgressMonitor progressMonitor) {
		Collection<EObject> documentation = getDocumentation(progressMonitor);
		if (documentation.isEmpty()) {
			return null;
		}
		return uri;
	}
	
	protected Collection<Label> createLayerLabels(Map<LayerElement, Collection<Label>> childLabelsMap, ProgressMonitor progressMonitor) {
		List<Label> childLabels = new ArrayList<>(childLabelsMap.values().stream().flatMap(Collection::stream).toList());
		return createLabels(childLabels, progressMonitor);
	}	

}

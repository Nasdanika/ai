package org.nasdanika.ai.drawio;

import java.util.Map;

import org.nasdanika.drawio.Connection;
import org.nasdanika.drawio.Layer;
import org.nasdanika.drawio.LayerElement;
import org.nasdanika.drawio.Node;
import org.nasdanika.graph.processor.ChildProcessors;
import org.nasdanika.graph.processor.ProcessorElement;
import org.nasdanika.graph.processor.ProcessorInfo;

public class LayerProcessor extends BaseProcessor<Layer> {
	
	// TODO - if background (no title) then merge with the root/page/model
	
	public LayerProcessor(DrawioProcessorFactory factory) {
		super(factory);
	}

	@ChildProcessors
	public Map<LayerElement, ProcessorInfo<LayerElementProcessor<?>>> childInfos;	
	
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
	
	@ProcessorElement
	@Override
	public void setElement(Layer element) {
		super.setElement(element);
	}

}

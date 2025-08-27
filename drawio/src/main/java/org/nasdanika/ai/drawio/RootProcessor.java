package org.nasdanika.ai.drawio;

import java.util.Map;
import java.util.function.Consumer;

import org.nasdanika.ai.Section;
import org.nasdanika.drawio.Layer;
import org.nasdanika.drawio.Root;
import org.nasdanika.graph.processor.ChildProcessors;
import org.nasdanika.graph.processor.ProcessorInfo;
import org.nasdanika.graph.processor.RegistryEntry;

public class RootProcessor extends BaseProcessor<Root> {
	
	// TODO - background color and background image from the model
		
	@RegistryEntry("#element.model.page == #this")
	public PageProcessor pageProcessor;
	
	public RootProcessor(DrawioProcessorFactory factory) {
		super(factory);
	}

	@ChildProcessors
	public Map<Layer, ProcessorInfo<LayerProcessor>> layerProcessorInfos;
	
	@Override
	public void configureSection(Section section) {
		super.configureSection(section);
		section.setTitle(element.getModel().getPage().getName());
		// TODO - layer list if more than one. number of elements?
		// TODO - logically merge with the background layer if it doesn't have a name
	}
	
	@Override
	protected Message createMessage(int depth) {
		return new Message(this, depth) {
			
			@Override
			void process(Consumer<Message> publisher) {
				for (ProcessorInfo<LayerProcessor> lp: layerProcessorInfos.values()) {
					publisher.accept(lp.getProcessor().createMessage(depth + 1));
				}
			}
			
		};
	}
	
}

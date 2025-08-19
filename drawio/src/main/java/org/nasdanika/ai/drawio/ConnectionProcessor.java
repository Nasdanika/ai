package org.nasdanika.ai.drawio;

import org.eclipse.emf.common.util.URI;
import org.nasdanika.ai.Section;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.common.Util;
import org.nasdanika.drawio.Connection;
import org.nasdanika.graph.processor.ProcessorElement;
import org.nasdanika.graph.processor.SourceHandler;
import org.nasdanika.models.app.Label;

public class ConnectionProcessor extends LayerElementProcessor<Connection> {

	public ConnectionProcessor(DrawioProcessorFactory factory) {
		super(factory);
	}
	
	
	@Override
	protected Section doCreateSection() {
		Section section = super.doCreateSection();
		
		// TODO - style, geometry (direction), start and end decorations
		
		return section;
	}
	

	@ProcessorElement
	@Override
	public void setElement(Connection element) {
		super.setElement(element);
		uri = URI.createURI(element.getId() + "/" + getIndexName());
	}
	
	@Override
	public void configureLabel(Label label, ProgressMonitor progressMonitor) {
		super.configureLabel(label, progressMonitor);
		if (Util.isBlank(label.getIcon())) {
			label.setIcon("fas fa-long-arrow-alt-right");
		}
	}	
	
	@SourceHandler
	public ConnectionProcessor getSourceHandler() {
		return this;
	}

}

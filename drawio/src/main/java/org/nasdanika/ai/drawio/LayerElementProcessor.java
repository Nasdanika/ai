package org.nasdanika.ai.drawio;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.nasdanika.common.MapCompoundSupplier;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.common.Supplier;
import org.nasdanika.common.Util;
import org.nasdanika.drawio.Connection;
import org.nasdanika.drawio.Document;
import org.nasdanika.drawio.Element;
import org.nasdanika.drawio.LayerElement;
import org.nasdanika.drawio.LinkTarget;
import org.nasdanika.drawio.ModelElement;
import org.nasdanika.drawio.Node;
import org.nasdanika.drawio.Page;
import org.nasdanika.drawio.Root;
import org.nasdanika.exec.content.ContentFactory;
import org.nasdanika.exec.content.Text;
import org.nasdanika.graph.processor.NodeProcessorInfo;
import org.nasdanika.graph.processor.ProcessorInfo;
import org.nasdanika.models.app.Label;
import org.nasdanika.models.app.graph.WidgetFactory;

public class LayerElementProcessor<T extends LayerElement> extends LinkTargetProcessor<T> {
	
	protected Map<ModelElement, ProcessorInfo<WidgetFactory>> childInfos = new ConcurrentHashMap<>();
	
	protected Map<Connection, CompletableFuture<ConnectionProcessor>> outgoingEndpoints = new ConcurrentHashMap<>();	
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void addReferrer(ModelElement referrer) {
		super.addReferrer(referrer);		
		for (Element child: referrer.getChildren()) {
			if (child instanceof ModelElement) {
				ProcessorInfo<WidgetFactory> ci = registry.get(child);
				if (ci != null /* && ci.getProcessor() != null */) {
					childInfos.put((ModelElement) child, ci);
				}
			}
		}		
		
		ProcessorInfo<WidgetFactory> referrerInfo = registry.get(referrer);
		if (referrerInfo instanceof NodeProcessorInfo) {
			NodeProcessorInfo<WidgetFactory, WidgetFactory, WidgetFactory> npi = (NodeProcessorInfo<WidgetFactory, WidgetFactory, WidgetFactory>) referrerInfo;
			outgoingEndpoints.putAll((Map) npi.getOutgoingEndpoints());			
		}
	}
	
	public LayerElementProcessor(DrawioProcessorFactory factory) {
		super(factory);
	}

	public Collection<ModelElement> referrers = new ArrayList<>();	

	/**
	 * Has documentation or has a page link (which implies having documentation)
	 */
	@Override
	public URI getActionURI(ProgressMonitor progressMonitor) {
		LinkTarget linkTarget = element.getLinkTarget();
		if (linkTarget instanceof Page) {
			ProcessorInfo<WidgetFactory> ppi = registry.get(linkTarget);
			if (ppi != null) {
				PageProcessor pageProcessor = (PageProcessor) ppi.getProcessor();
				if (pageProcessor != null) {
					return uri;
				}
			}
		}
		
		Collection<EObject> documentation = super.getDocumentation(progressMonitor);
		if (Util.isBlank(element.getLabel()) || (documentation.isEmpty() && !hasNonChildRoles())) {			
			return null;
		}
		return uri;
	}	
	
	@Override
	public void resolve(URI base, ProgressMonitor progressMonitor) {
		super.resolve(base, progressMonitor);
		for (Entry<ModelElement, ProcessorInfo<WidgetFactory>> cpe: childInfos.entrySet()) {
			if (cpe.getKey() instanceof Node || isLogicalChildConnection(cpe.getKey())) {
				cpe.getValue().getProcessor().resolve(uri, progressMonitor);
			}
		}		
		for (Entry<Connection, CompletableFuture<ConnectionProcessor>> oe: outgoingEndpoints.entrySet()) {
			oe.getValue().thenAccept(cp -> cp.resolve(uri, progressMonitor));
		}	
		if (element.isTargetLink()) {
			LinkTarget linkTarget = element.getLinkTarget();
			if (linkTarget instanceof Page) {
				ProcessorInfo<WidgetFactory> ppi = registry.get(linkTarget);
				if (ppi != null) {
					ppi.getProcessor().resolve(uri, progressMonitor);
				}
			}
		}
	}	
	
	@Override
	protected Collection<EObject> getDocumentation(ProgressMonitor progressMonitor) {
		List<EObject> ret = new ArrayList<>();		
		LinkTarget linkTarget = element.getLinkTarget();
		if (linkTarget instanceof Page) {
			ProcessorInfo<WidgetFactory> ppi = registry.get(linkTarget);
			if (ppi != null) {
				PageProcessor pageProcessor = (PageProcessor) ppi.getProcessor();
				Text representationText = ContentFactory.eINSTANCE.createText(); // Interpolate with element properties?
				try {
					Document representation = pageProcessor.createRepresentation(progressMonitor);
					representationText.setContent(representation.toHtml(true, configuration.getViewer()));
					ret.addAll(configuration.createRepresentationContent(representation, registry, progressMonitor));
				} catch (TransformerException | IOException | ParserConfigurationException e) {
					representationText.setContent("<div class=\"alert alert-danger\" role=\"alert\">Error creating representation:" + e + "</div>");
				}
				ret.add(representationText);
			}
		}
				
		ret.addAll(super.getDocumentation(progressMonitor));
		
		// linked documentation (root)
		if (linkTarget instanceof Page) {
			Root root = ((Page) linkTarget).getModel().getRoot();
			ProcessorInfo<WidgetFactory> rpi = registry.get(root);
			RootProcessor rootProcessor = (RootProcessor) rpi.getProcessor();
			ret.addAll(rootProcessor.getDocumentation(progressMonitor));			
		}
		
		return ret;
	}
		
	@SuppressWarnings("resource")
	@Override
	public Supplier<Collection<Label>> createLabelsSupplier() {
		MapCompoundSupplier<ModelElement, Collection<Label>> childLabelsSupplier = new MapCompoundSupplier<>("Child labels supplier");

		String parentProperty = configuration.getParentProperty();
		String targetKey = configuration.getTargetKey();
		String sourceKey = configuration.getSourceKey();

		// Own child nodes not linked to other nodes
		C: for (Entry<ModelElement, ProcessorInfo<WidgetFactory>> ce: childInfos.entrySet()) {
			ModelElement child = ce.getKey();
			if (child instanceof Connection && ((Connection) child).getSource() != null) {
				continue;
			}
			if (child instanceof Node) {
				Node childNode = (Node) child;
				if (!Util.isBlank(parentProperty)) {
					if (!Util.isBlank(targetKey)) {
						for (Connection cnoc: childNode.getOutgoingConnections()) {
							String cParent = cnoc.getProperty(parentProperty);
							if (targetKey.equals(cParent)) {
								continue C; // Logical child of connection's target
							}
						}
					}
					if (!Util.isBlank(sourceKey)) {
						for (Connection cnoc: childNode.getIncomingConnections()) {
							String cParent = cnoc.getProperty(parentProperty);
							if (sourceKey.equals(cParent)) {
								continue C; // Logical child of connection's source
							}
						}
					}
				}
				
			}
			childLabelsSupplier.put(child, ce.getValue().getProcessor().createLabelsSupplier());
		}
		
		// Connections without parent property
		for (Entry<Connection, CompletableFuture<ConnectionProcessor>> ce: outgoingEndpoints.entrySet()) {
			Connection conn = ce.getKey();
			if (!Util.isBlank(parentProperty) &&  !Util.isBlank(conn.getProperty(parentProperty))) {
				continue;
			}
			if (isLogicalChildConnection(conn)) {
				childLabelsSupplier.put(ce.getKey(), ce.getValue().join().createLabelsSupplier());
			}
		}
		
		// Nodes linked by connections with parent property
		if (!Util.isBlank(parentProperty) && element instanceof Node) {
			Node node = (Node) element;
			if (!Util.isBlank(sourceKey)) {
				for (Connection oc: node.getOutgoingConnections()) {
					Node target = oc.getTarget();
					if (sourceKey.equals(oc.getProperty(parentProperty)) && target != null) {
						ProcessorInfo<WidgetFactory> childInfo = registry.get(target);
						if (childInfo != null) {
							WidgetFactory processor = childInfo.getProcessor();
							if (processor != null) {
								childLabelsSupplier.put(target, processor.createLabelsSupplier());
							}
						}
					}
				}
			}
			if (!Util.isBlank(targetKey)) {
				for (Connection ic: node.getIncomingConnections()) {
					Node source = ic.getSource();
					if (targetKey.equals(ic.getProperty(parentProperty)) && source != null) {
						ProcessorInfo<WidgetFactory> childInfo = registry.get(source);
						if (childInfo != null) {
							WidgetFactory processor = childInfo.getProcessor();
							if (processor != null) {
								childLabelsSupplier.put(source, processor.createLabelsSupplier());
							}
						}
					}
				}
			}			
		}
						
		Supplier<Collection<Label>> pageLabelSupplier = Supplier.empty();		
		if (element.isTargetLink()) {
			LinkTarget linkTarget = element.getLinkTarget();
			if (linkTarget instanceof Page) {
				ProcessorInfo<WidgetFactory> ppi = registry.get(linkTarget);
				pageLabelSupplier = ppi.getProcessor().createLabelsSupplier();
			}
		}	
		
		return childLabelsSupplier.then(pageLabelSupplier.asFunction(this::createLayerElementLabels));
	}
	
	protected Collection<Label> createLayerElementLabels(
			Map<ModelElement, Collection<Label>> childLabelsMap, 
			Collection<Label> pageLabels,
			ProgressMonitor progressMonitor) {

		List<Label> childLabels = new ArrayList<>(childLabelsMap.values().stream().flatMap(Collection::stream).toList());
		if (pageLabels != null) {
			childLabels.addAll(pageLabels);
		}
		return createLabels(childLabels, progressMonitor);
	}

}

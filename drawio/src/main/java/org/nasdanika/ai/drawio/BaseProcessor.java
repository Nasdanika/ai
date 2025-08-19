package org.nasdanika.ai.drawio;

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.jsoup.Jsoup;
import org.nasdanika.ai.Content;
import org.nasdanika.ai.Section;
import org.nasdanika.ai.SectionReference;
import org.nasdanika.common.DefaultConverter;
import org.nasdanika.common.Description;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.common.Status;
import org.nasdanika.common.SupplierFactory;
import org.nasdanika.common.Util;
import org.nasdanika.drawio.Element;
import org.nasdanika.drawio.LinkTarget;
import org.nasdanika.drawio.ModelElement;
import org.nasdanika.drawio.Page;
import org.nasdanika.drawio.Root;
import org.nasdanika.graph.processor.ProcessorElement;
import org.nasdanika.graph.processor.ProcessorInfo;
import org.nasdanika.graph.processor.Registry;
import org.nasdanika.persistence.ObjectLoader;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Base class for processors
 */
public class BaseProcessor<T extends Element> {
	
	private static final String PLAIN_TEXT = "text/plain";

	protected Configuration configuration;
	
	public BaseProcessor(DrawioProcessorFactory configuration) {
		this.configuration = configuration;
	}
		
	protected T element;
	
	@ProcessorElement	
	public void setElement(T element) {
		this.element = element;
		if (element instanceof ModelElement) {
			id = ((ModelElement) element).getId();					
		}
	}
	
	@Registry 
	public Map<Element, ProcessorInfo<BaseProcessor<?>>> registry;
	
	protected String id;
	
	/**
	 * @return Section id if this processor creates a section, null otherwise
	 */
	public String getId() {
		return id;
	}
	
	protected Section doCreateSection() {
		SectionReference sectionReference = createSectionReference();
		Section section = new Section(sectionReference.getTitle(), sectionReference.getId(), element);
		Content docContent = getDocumentation();
		if (docContent != null) {
			Section docSection = new Section("Documentation", null, null);
			docSection.getContents().add(docContent);
			section.getChildren().add(docSection);
		}
		
		if (element instanceof ModelElement) {
			ModelElement modelElement = (ModelElement) element;
			String tooltip = modelElement.getTooltip();
			if (!Util.isBlank(tooltip)) {
				Section tooltipSection = new Section("Tooltip", null, null);				
				tooltipSection.getContents().add(new Content(tooltip, PLAIN_TEXT));
				section.getChildren().add(tooltipSection);				
			}
		}		
		
		
		// 
		
		return section;
	}
	
//	String iconProperty = configuration.getIconProperty();
//	if (!Util.isBlank(iconProperty)) {
//		label.setIcon(modelElement.getProperty(iconProperty));
//	}
//	if (Util.isBlank(label.getTooltip())) {
//		label.setTooltip(modelElement.getTooltip());
//	}	
	
	public Flux<Section> createSectionsAsync() {
		return Flux.just(doCreateSection());
	}
	
	public Iterable<Section> createSections() {
		return createSectionsAsync().toIterable();
	}		

	public SectionReference createSectionReference() {
		SectionReference ret = new SectionReference();
		if (element instanceof ModelElement) {
			ModelElement modelElement = (ModelElement) element;
			String label = modelElement.getLabel();
			ret.setTitle(label);			
			String titleProperty = configuration.getTitleProperty();
			if (!Util.isBlank(titleProperty)) {
				String title = modelElement.getProperty(titleProperty);
				if (!Util.isBlank(title)) {
					ret.setTitle(titleProperty);
				}
			}
			if (Util.isBlank(ret.getTitle()) && !Util.isBlank(label)) {
				String labelText = Jsoup.parse(label).text();
				ret.setTitle(labelText);
			}
		}		
		
		return ret;
	}

	public void dedupIds(Predicate<String> idPredicate) {
		if (id != null && !idPredicate.test(id)) {
			for (int i = 1; i < 1000; ++i) {
				String newId = id + "-" + Integer.toString(i, Character.MAX_RADIX);
				if (idPredicate.test(newId)) {
					id = newId;
				}
			}
		}
	}
	
	private static String getContentType(String extension) {
		return switch (extension) {
			case "html", "htm" -> "text/html";
			case "txt" -> "text/plain";		
			default -> Description.MARKDOWN_FORMAT;
		};				
	}
			
	protected Content getDocumentation() {
		if (element instanceof ModelElement) {		
			ModelElement modelElement = (ModelElement) element;
			Function<String, String> tokenSource = key -> {
				String value = modelElement.getProperty(key);
				return Util.isBlank(value) ? null : value;
			};
			URI refBaseUri = configuration.getRefBaseURI(modelElement.getModel().getPage().getDocument().getURI());
			String docProperty = configuration.getDocumentationProperty();
			if (!Util.isBlank(docProperty)) {
				String doc = modelElement.getProperty(docProperty);
				if (!Util.isBlank(doc)) {
					String docFormatStr = null;
					String docFormatProperty = configuration.getDocFormatProperty();
					if (!Util.isBlank(docFormatProperty)) {
						docFormatStr = modelElement.getProperty(docFormatProperty);
					}
					if (Util.isBlank(docFormatStr)) {
						docFormatStr = Description.MARKDOWN_FORMAT;
					}
					if (tokenSource != null) {
						doc = Util.interpolate(doc, tokenSource);
					}
					return new Content(doc, docFormatStr);
				}
			}
				
			try {
				String docRefProperty = configuration.getDocRefProperty();
				if (!Util.isBlank(docRefProperty)) {
					String docRefStr = modelElement.getProperty(docRefProperty);
					if (!Util.isBlank(docRefStr)) {
						String docFormatStr = null;
						String docFormatProperty = configuration.getDocFormatProperty();
						if (!Util.isBlank(docFormatProperty)) {
							docFormatStr = modelElement.getProperty(docFormatProperty);
						}
						URI docRefURI = URI.createURI(docRefStr);
						if (refBaseUri != null && !refBaseUri.isRelative()) {
							docRefURI = docRefURI.resolve(refBaseUri);
						}
						
						if (docFormatStr == null) {
							int dotIdx = docRefStr.lastIndexOf('.');
							docFormatStr = dotIdx == -1 ? Description.MARKDOWN_FORMAT : getContentType(docRefStr.substring(dotIdx + 1).toLowerCase());
						}
						
						InputStream docStream = DefaultConverter.INSTANCE.toInputStream(docRefURI);
						String doc = DefaultConverter.INSTANCE.toString(docStream);	
						if (tokenSource != null) {
							doc = Util.interpolate(doc, tokenSource);
						}
						return new Content(doc, docFormatStr);
					}
				}
			} catch (Exception e) {
				return new Content("Error retrieving documentation: " + e, "text/plain");
			}
		}
		return null;
	}
	
	/**
	 * Set icon, tooltip, ...
	 * @param label
	 * @param progressMonitor 
	 */
	public void configureLabel(Label label, ProgressMonitor progressMonitor) {
		if (element instanceof ModelElement) {
			ModelElement modelElement = (ModelElement) element;
			if (Util.isBlank(label.getIcon())) {
				String iconProperty = configuration.getIconProperty();
				if (!Util.isBlank(iconProperty)) {
					label.setIcon(modelElement.getProperty(iconProperty));
				}
			}
			if (Util.isBlank(label.getTooltip())) {
				label.setTooltip(modelElement.getTooltip());
			}
			String titleProperty = configuration.getTitleProperty();
			if (!Util.isBlank(titleProperty)) {
				String title = modelElement.getProperty(titleProperty);
				if (!Util.isBlank(title)) {
					label.setText(title);
				}
			}
			if (Util.isBlank(label.getText()) && element instanceof ModelElement) {
				label.setText(((ModelElement) element).getId());
			}
		}
	}

	@SuppressWarnings("unchecked")
	protected Label getPrototype(ProgressMonitor progressMonitor) {
		ModelElement modelElement = (ModelElement) element;
		if (element instanceof ModelElement) {		
			try {
				EObjectLoader eObjectLoader = new EObjectLoader((ObjectLoader) null) {
					
					@Override
					public ResolutionResult resolveEClass(String type) {
						EClass eClass = (EClass) configuration.getType(type, (ModelElement) element);
						return new ResolutionResult(eClass, null);
					}
					
					@Override
					public ResourceSet getResourceSet() {
						return configuration.getResourceSet();
					}
					
				};
				
				URI refBaseUri = configuration.getRefBaseURI(modelElement.getModel().getPage().getDocument().getURI());
				String prototypeProperty = configuration.getPrototypeProperty();
				if (!Util.isBlank(prototypeProperty)) {
					String prototypeSpec = modelElement.getProperty(prototypeProperty);
					if (!Util.isBlank(prototypeSpec)) {
						Object obj = eObjectLoader.loadYaml(
								prototypeSpec, 
								refBaseUri, 
								null, 
								progressMonitor);
						
						if (obj instanceof SupplierFactory) {
							obj = ((SupplierFactory<Object>) obj).create(configuration.getContext()).call(progressMonitor, configuration::onDiagnostic);
						} 
						
						return (Label) obj;
					}
				}
				
				String protoRefProperty = configuration.getProtoRefProperty();
				if (!Util.isBlank(protoRefProperty)) {
					String protoRefStr = modelElement.getProperty(protoRefProperty);
					if (!Util.isBlank(protoRefStr)) {
						URI[] protoRefURI = { URI.createURI(protoRefStr) };
						if (refBaseUri != null && !refBaseUri.isRelative()) {
							protoRefURI[0] = protoRefURI[0].resolve(refBaseUri);
						}
						
						return (Label) configuration.getResourceSet().getEObject(protoRefURI[0], true);
					}
				}
			} catch (Exception e) {
				Action errorAction = AppFactory.eINSTANCE.createAction();				
				Text text = ContentFactory.eINSTANCE.createText(); // Interpolate with element properties?
				text.setContent("<div class=\"nsd-error\">Error loading prototype: " + e + "</div>");
				errorAction.getContent().add(text);
				return errorAction;
			}
			
			LinkTarget linkTarget = modelElement.getLinkTarget();
			// linked documentation (root)
			if (linkTarget instanceof Page) {
				Root root = ((Page) linkTarget).getModel().getRoot();
				ProcessorInfo<WidgetFactory> rpi = registry.get(root);
				RootProcessor rootProcessor = (RootProcessor) rpi.getProcessor();
				return rootProcessor.getPrototype(progressMonitor);
			}
		}
		return null;		
	}	
	
	protected void addError(Action action, String error) {
		Text errorText = ContentFactory.eINSTANCE.createText(); 
		errorText.setContent("<div class=\"alert alert-danger\" role=\"alert\">" + error + "</div>");
		action.getContent().add(errorText);
	}
	
	protected String getLabelRole(Label label) {
		for (Adapter a: label.eAdapters()) {
			if (a instanceof ModelElementAdapter) {
				return ((ModelElementAdapter) a).getRole();
			}
		}
		return configuration.getChildRole();
	}
	
	protected String getLabelSortKey(Label label) {
		for (Adapter a: label.eAdapters()) {
			if (a instanceof ModelElementAdapter) {
				return ((ModelElementAdapter) a).getSortKey();
			}
		}
		return null;
	}
			
	protected int compareLabelsBySortKeyAndText(Label a, Label b) {
		String aKey = getLabelSortKey(a);
		String bKey = getLabelSortKey(b);
		if (Util.isBlank(aKey)) {
			if (!Util.isBlank(bKey)) {
				return 1;
			}
		} else if (Util.isBlank(bKey)) {
			if (!Util.isBlank(aKey)) {
				return -1;
			}				
		} else {
			int cmp = aKey.compareTo(bKey);
			if (cmp != 0) {
				return cmp;
			}
		}
		if (Util.isBlank(a.getText())) {
			return Util.isBlank(b.getText()) ? a.hashCode() - b.hashCode() : 1;
		}
		if (Util.isBlank(b.getText())) {
			return -1;
		}
		
		return Jsoup.parse(a.getText()).text().compareTo(Jsoup.parse(b.getText()).text());
	}	
	
	/**
	 * Groups by role, sorts by sort key and label text
	 * @param label
	 * @param children
	 */
	protected void addChildLabels(Label label, Collection<Label> childLabels, ProgressMonitor progressMonitor) {
		Map<String, List<Label>> groupedByRole = Util.groupBy(childLabels, this::getLabelRole);
		groupedByRole.values().forEach(labels -> Collections.sort(labels, this::compareLabelsBySortKeyAndText));
		
		String childRole = configuration.getChildRole();
		if (!Util.isBlank(childRole)) {
			List<Label> children = groupedByRole.remove(childRole);
			if (children != null) {
				label.getChildren().addAll(children);				
			}
		}		
		if (label instanceof Action) {
			Action action = (Action) label;
		
			String anonymousRole = configuration.getAnonymousRole();
			if (!Util.isBlank(anonymousRole)) {
				List<Label> anonymous = groupedByRole.remove(anonymousRole);
				if (anonymous != null) {
					for (Label aLabel: anonymous) {
						if (aLabel instanceof Action) {
							action.getAnonymous().add((Action) aLabel);
						} else {
							addError(action, "Not an action, cannot add to anonymous: " + aLabel.getText());
						}
					}
				}
			}
			
			String navigationRole = configuration.getNavigationRole();
			if (!Util.isBlank(navigationRole)) {
				List<Label> navs = groupedByRole.remove(navigationRole);
				if (navs != null) {
					action.getNavigation().addAll(navs);				
				}
			}		
			
			String sectionRole = configuration.getSectionRole();
			if (!Util.isBlank(sectionRole)) {
				List<Label> sections = groupedByRole.remove(sectionRole);
				if (sections != null) {
					for (Label sLabel: sections) {
						if (sLabel instanceof Action) {
							action.getSections().add((Action) sLabel);
						} else {
							addError(action, "Not an action, cannot add to sections: " + sLabel.getText());
						}
					}
				}
			}
			
			if (!groupedByRole.isEmpty()) {
				addError(action, "Unsupported roles: " + groupedByRole.keySet());				
			}						
		} else if (!groupedByRole.isEmpty()) {
			progressMonitor.worked(Status.ERROR, 1, "Unsupported roles: " + groupedByRole.keySet(), label);				
		}				
	}
	
	protected String getSortKey() {
		if (element instanceof ModelElement) {
			String sortKeyProperty = configuration.getSortKeyProperty();		
			if (!Util.isBlank(sortKeyProperty)) {
				return ((ModelElement) element).getProperty(sortKeyProperty);
			}
		}
		return null;
	}	
	
	protected boolean hasNonChildRoles;
	
	protected boolean hasNonChildRoles() {
		return hasNonChildRoles;
	}
		
	/**
	 * Common functionality for layer and layer element
	 * @param childLabelsMap
	 * @param progressMonitor
	 * @return
	 */
	protected Collection<Label> createLabels(List<Label> childLabels, ProgressMonitor progressMonitor) {		
		String label = element instanceof ModelElement ? ((ModelElement) element).getLabel() : null;
		if (Util.isBlank(label)) {
			return childLabels;
		}
		
		Collection<EObject> documentation = getDocumentation(progressMonitor);
		
		if (documentation.isEmpty() && childLabels.isEmpty()) {
			// No child labels, no documentation
			return Collections.emptyList();
		}
							
		Label prototype = getPrototype(progressMonitor);
		Label mLabel;
		if (prototype == null) {		
			hasNonChildRoles = hasNonChildRoles(childLabels);
			mLabel = documentation.isEmpty() && !hasNonChildRoles ? AppFactory.eINSTANCE.createLabel() : AppFactory.eINSTANCE.createAction();
		} else {
			mLabel = EcoreUtil.copy(prototype);
		}
		
		if (Util.isBlank(mLabel.getText())) {
			String labelText = Jsoup.parse(label).text();
			mLabel.setText(labelText);
		}
		
		if (element instanceof ModelElement) {
			mLabel.eAdapters().add(new ModelElementAdapter(mLabel, (ModelElement) element, getRole(), getSortKey()));
		}
		configureLabel(mLabel, progressMonitor);
				
		if (mLabel instanceof Action) {
			Action mAction = (Action) mLabel;
			if (!documentation.isEmpty() ) {
				mAction.getContent().addAll(documentation);
			}
			String sectionRole = configuration.getSectionRole();
			String labelRole = getLabelRole(mLabel);
			if (Util.isBlank(sectionRole) || !sectionRole.equals(labelRole)) {
				if (Util.isBlank(mAction.getLocation())) { // Link?
					mAction.setLocation(uri.toString());
				}
			}
		}		
		addChildLabels(mLabel, childLabels, progressMonitor);
		if (mLabel instanceof Action) {
			Action mAction = (Action) mLabel;
			if (!Util.isBlank(mAction.getLocation())) {
				URI mActionLocation = URI.createURI(mAction.getLocation());
				childLabels.forEach(cl -> cl.rebase(null, mActionLocation));
			}
		}
		
		return Collections.singleton(mLabel);			
	}

	protected boolean hasNonChildRoles(List<Label> childLabels) {
		String childRole = configuration.getChildRole();
		for (Label childLabel: childLabels) {
			if (!childRole.equals(getLabelRole(childLabel))) {
				return true;
			}
		}
		return false;
	}	
	
	protected String getIndexName() {
		return configuration.getIndexName();
	}
	
}

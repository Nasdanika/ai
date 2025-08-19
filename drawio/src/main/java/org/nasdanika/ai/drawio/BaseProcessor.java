package org.nasdanika.ai.drawio;

import java.io.InputStream;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import org.eclipse.emf.common.util.URI;
import org.jsoup.Jsoup;
import org.nasdanika.ai.Content;
import org.nasdanika.ai.Section;
import org.nasdanika.ai.SectionReference;
import org.nasdanika.common.DefaultConverter;
import org.nasdanika.common.Description;
import org.nasdanika.common.Util;
import org.nasdanika.drawio.Element;
import org.nasdanika.drawio.ModelElement;
import org.nasdanika.graph.processor.ProcessorElement;
import org.nasdanika.graph.processor.ProcessorInfo;
import org.nasdanika.graph.processor.Registry;

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
	
	/**
	 * Override to synchronously customize section configuration.
	 * Override createSectionAsync for asynchronous customization.
	 * @return
	 */
	protected Section doCreateSection() {
		SectionReference sectionReference = createSectionReference();
		Section section = new Section(sectionReference.getTitle(), sectionReference.getId());
		Content docContent = getDocumentation();
		if (docContent != null) {
			Section docSection = new Section("Documentation", null);
			docSection.getContents().add(docContent);
			section.getChildren().add(docSection);
		}
		
		if (element instanceof ModelElement) {
			ModelElement modelElement = (ModelElement) element;
			String tooltip = modelElement.getTooltip();
			if (!Util.isBlank(tooltip)) {
				Section tooltipSection = new Section("Tooltip", null);				
				tooltipSection.getContents().add(new Content(tooltip, PLAIN_TEXT));
				section.getChildren().add(tooltipSection);				
			}
		}		
		
		return section;
	}
	
//	String iconProperty = configuration.getIconProperty();
//	if (!Util.isBlank(iconProperty)) {
//		label.setIcon(modelElement.getProperty(iconProperty));
//	}
//	if (Util.isBlank(label.getTooltip())) {
//		label.setTooltip(modelElement.getTooltip());
//	}	
	
	/**
	 * Asynchronously creates a section for the underlying diagram element. 
	 * The section does not contain sections for child elements - they shall be obtained through the registry 
	 * @return
	 */
	public Mono<Section> createSectionAsync() {
		return Mono.just(doCreateSection());
	}
	
	/**
	 * Synchronously creates a section.
	 * This implementation calls creasteSectionAsync().block();
	 * To synchronously customize section creation override <code>doCreateSection</code>
	 * @return
	 */
	public Section createSection() {
		return createSectionAsync().block();
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
	
}

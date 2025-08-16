package org.nasdanika.ai.drawio;

import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.imaging.common.GenericImageMetadata.GenericImageMetadataItem;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.common.ImageMetadata.ImageMetadataItem;
import org.nasdanika.ai.ImageMetadataNarrator;
import org.nasdanika.ai.Narrator;
import org.nasdanika.drawio.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import reactor.core.publisher.Mono;

public abstract class DocumentImageMetadataNarrator<S> extends ImageMetadataNarrator<S> {
	
	private static Logger LOGGER  = LoggerFactory.getLogger(DocumentImageMetadataNarrator.class);
	
	protected Narrator<Document> documentNarrator;

	/**
	 * @param documentNarrator Narrates document if it is found in the image metadata. 
	 */
	protected DocumentImageMetadataNarrator(Narrator<Document> documentNarrator) {
		this.documentNarrator = documentNarrator;
	}
	
	// TODO - document narrator
	
	@Override
	public Mono<String> generateAsync(S input) {		
		return narrateMetadataAsync(getImageMetadata(input));
	}
	
	@Override
	public String generate(S input) {
		return narrateMetadata(getImageMetadata(input));
	}
	
	protected abstract ImageMetadata getImageMetadata(S source);
	
	protected String narrateMetadata(ImageMetadata metadata) {
		if (metadata != null) {
			List<? extends ImageMetadataItem> items = metadata.getItems();
			if (items != null) {
				for (ImageMetadataItem item: items) {
					if (item instanceof GenericImageMetadataItem) {
						GenericImageMetadataItem gItem = (GenericImageMetadataItem) item;
						if ("mxfile".equals(gItem.getKeyword())) {
							try {
								Document document = Document.load(gItem.getText(), null);
								return documentNarrator.generate(document);
							} catch (ParserConfigurationException | SAXException | IOException e) {
								LOGGER.error("Could not narrate document from metadata: " + e, e);
							}							
						}
					}
				}
			}			
		}
		return super.narrateMetadata(metadata);
	}
	
	protected Mono<String> narrateMetadataAsync(ImageMetadata metadata) {
		return Mono.just(narrateMetadata(metadata));
	}	
	
}

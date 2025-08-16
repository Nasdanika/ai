package org.nasdanika.ai;

import org.apache.commons.imaging.common.ImageMetadata;

import java.util.List;

import org.apache.commons.imaging.common.GenericImageMetadata.GenericImageMetadataItem;
import org.apache.commons.imaging.common.ImageMetadata.ImageMetadataItem;

import reactor.core.publisher.Mono;

public abstract class ImageMetadataNarrator<S> implements Narrator<S> {
	
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
						if (getDescriptionKeyword().equals(gItem.getKeyword())) {
							return gItem.getText();
						}
					}
				}
			}			
		}
		return null;
	}
	
	protected String getDescriptionKeyword() {
		return "Description";
	}
	
	protected Mono<String> narrateMetadataAsync(ImageMetadata metadata) {
		return Mono.just(narrateMetadata(metadata));
	}	
	
}

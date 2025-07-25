package org.nasdanika.ai;

import reactor.core.publisher.Flux;

/**
 * A collection of strings pre-computed embeddings, e.g. web site contents.
 */
public interface TextFloatVectorEmbeddingResource {
	
	String getMimeType();
	
	Flux<TextFloatVectorEmbeddingResourceContents> getContents();	
	
}

package org.nasdanika.ai;

import reactor.core.publisher.Flux;

/**
 * A collection of strings pre-computed embeddings, e.g. web site contents.
 */
public interface EmbeddingsResource {
	
	Flux<EmbeddingsResourceContents> getResources();	
	
}

package org.nasdanika.ai;

import reactor.core.publisher.Flux;

/**
 * A collection of pre-computed embeddings, e.g. web site contents.
 */
public interface EmbeddingsResourceSet {
	
	Flux<EmbeddingsResource> getResources();	
	
}

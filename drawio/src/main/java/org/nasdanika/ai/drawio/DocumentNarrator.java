package org.nasdanika.ai.drawio;

import org.nasdanika.ai.Narrator;
import org.nasdanika.drawio.Document;

import reactor.core.publisher.Mono;

public class DocumentNarrator implements Narrator<Document> {
	
	// TODO - image narrator to use for image nodes
	
	// TODO - element tracker to traverse only once

	@Override
	public Mono<String> generateAsync(Document input) {
		// TODO Pages - one page or many
		return null;
	}

}

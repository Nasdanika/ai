package org.nasdanika.ai.drawio;

import org.nasdanika.ai.Narrator;
import org.nasdanika.drawio.Page;

import reactor.core.publisher.Mono;

public class PageNarrator implements Narrator<Page> {
	
	// TODO - image narrator to use for image nodes
	
	// TODO - element tracker to traverse only once

	@Override
	public Mono<String> generateAsync(Page input) {
		// TODO Pages - one page or many
		return null;
	}

}

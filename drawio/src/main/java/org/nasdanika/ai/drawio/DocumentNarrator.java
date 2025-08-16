package org.nasdanika.ai.drawio;

import java.util.function.Predicate;
import java.util.function.Supplier;

import org.eclipse.emf.common.util.URI;
import org.nasdanika.ai.Narrator;
import org.nasdanika.drawio.Document;
import org.nasdanika.drawio.Element;

public class DocumentNarrator extends ElementNarrator<Document> {
	
	private Supplier<Narrator<URI>> imageNarratorSupplier;

	/**
	 * @param imageNarratorSupplier Supplier of image narrator to narrate images of diagram elements.
	 * Supplier is used because the supplied image narrator may fall back to this narrator for 
	 * images with Draw.io metadata. Image narrator takes image URI including data URIs for embedded images
	 * 
	 */
	public DocumentNarrator(Supplier<Narrator<URI>> imageNarratorSupplier) {
		this.imageNarratorSupplier = imageNarratorSupplier;
	}

	/**
	 * @param imageNarrator image narrator to narrate images of diagram elements.
	 */
	public DocumentNarrator(Narrator<URI> imageNarrator) {
		this(() -> imageNarrator);
	}

	@Override
	public String generate(Document input, int headerLevel, Predicate<? super Element> traversalPredicate) {
		// TODO Number of pages, page names, total number of pages and elements on them 
		return "TODO";
	}

	

}

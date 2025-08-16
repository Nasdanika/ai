package org.nasdanika.ai.drawio;

import java.io.IOException;
import java.net.URL;

import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.nasdanika.ai.Narrator;
import org.nasdanika.common.NasdanikaException;
import org.nasdanika.drawio.Document;

public class UrlDocumentImageMetadataNarrator extends DocumentImageMetadataNarrator<URL> {

	public UrlDocumentImageMetadataNarrator(Narrator<Document> documentNarrator) {
		super(documentNarrator);
	}

	@Override
	protected ImageMetadata getImageMetadata(URL source) {
		try {
			return Imaging.getMetadata(source.openStream(),source.getFile());
		} catch (IOException e) {
			throw new NasdanikaException(e);
		}
	}

}

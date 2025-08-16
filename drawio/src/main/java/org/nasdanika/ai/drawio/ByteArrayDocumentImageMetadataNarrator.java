package org.nasdanika.ai.drawio;

import java.io.IOException;

import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.nasdanika.ai.Narrator;
import org.nasdanika.common.NasdanikaException;
import org.nasdanika.drawio.Document;

public class ByteArrayDocumentImageMetadataNarrator extends DocumentImageMetadataNarrator<byte[]> {

	public ByteArrayDocumentImageMetadataNarrator(Narrator<Document> documentNarrator) {
		super(documentNarrator);
	}

	@Override
	protected ImageMetadata getImageMetadata(byte[] source) {
		try {
			return Imaging.getMetadata(source);
		} catch (IOException e) {
			throw new NasdanikaException(e);
		}
	}

}

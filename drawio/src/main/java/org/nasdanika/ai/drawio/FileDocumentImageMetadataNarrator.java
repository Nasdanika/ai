package org.nasdanika.ai.drawio;

import java.io.File;
import java.io.IOException;

import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.nasdanika.ai.Narrator;
import org.nasdanika.common.NasdanikaException;
import org.nasdanika.drawio.Document;

public class FileDocumentImageMetadataNarrator extends DocumentImageMetadataNarrator<File> {

	public FileDocumentImageMetadataNarrator(Narrator<Document> documentNarrator) {
		super(documentNarrator);
	}

	@Override
	protected ImageMetadata getImageMetadata(File source) {
		try {
			return Imaging.getMetadata(source);
		} catch (IOException e) {
			throw new NasdanikaException(e);
		}
	}

}

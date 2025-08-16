package org.nasdanika.ai.drawio;

import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.eclipse.emf.common.util.URI;
import org.nasdanika.ai.Narrator;
import org.nasdanika.common.NasdanikaException;
import org.nasdanika.drawio.Document;

public class UriDocumentImageMetadataNarrator extends DocumentImageMetadataNarrator<URI> {

	public UriDocumentImageMetadataNarrator(Narrator<Document> documentNarrator) {
		super(documentNarrator);
	}

	@Override
	protected ImageMetadata getImageMetadata(URI source) {
		try {
			String uriStr = source.toString();
			if (uriStr.startsWith("data:")) {
			    int commaIndex = uriStr.indexOf(',');
			    String metadata = uriStr.substring(5, commaIndex);
			    String dataPart = uriStr.substring(commaIndex + 1);

			    byte[] bytes;
			    if (metadata.contains(";base64")) {
			        bytes = Base64.getDecoder().decode(dataPart);
			    } else {
			        bytes = URLDecoder.decode(dataPart, StandardCharsets.UTF_8).getBytes(StandardCharsets.UTF_8);
			    }
				return Imaging.getMetadata(bytes);		
			}
			return Imaging.getMetadata(new URL(uriStr).openStream(),source.lastSegment());
		} catch (IOException e) {
			throw new NasdanikaException(e);
		}
	}

}

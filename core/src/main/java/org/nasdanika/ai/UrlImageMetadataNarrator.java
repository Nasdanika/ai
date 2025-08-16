package org.nasdanika.ai;

import java.io.IOException;
import java.net.URL;

import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.nasdanika.common.NasdanikaException;

public class UrlImageMetadataNarrator extends ImageMetadataNarrator<URL> {

	@Override
	protected ImageMetadata getImageMetadata(URL source) {
		try {
			return Imaging.getMetadata(source.openStream(),source.getFile());
		} catch (IOException e) {
			throw new NasdanikaException(e);
		}
	}

}

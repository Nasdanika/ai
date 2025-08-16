package org.nasdanika.ai;

import java.io.IOException;

import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.nasdanika.common.NasdanikaException;

public class ByteArrayImageMetadataNarrator extends ImageMetadataNarrator<byte[]> {

	@Override
	protected ImageMetadata getImageMetadata(byte[] source) {
		try {
			return Imaging.getMetadata(source);
		} catch (IOException e) {
			throw new NasdanikaException(e);
		}
	}

}

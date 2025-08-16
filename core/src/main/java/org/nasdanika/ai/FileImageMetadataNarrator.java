package org.nasdanika.ai;

import java.io.File;
import java.io.IOException;

import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.nasdanika.common.NasdanikaException;

public class FileImageMetadataNarrator extends ImageMetadataNarrator<File> {

	@Override
	protected ImageMetadata getImageMetadata(File source) {
		try {
			return Imaging.getMetadata(source);
		} catch (IOException e) {
			throw new NasdanikaException(e);
		}
	}

}

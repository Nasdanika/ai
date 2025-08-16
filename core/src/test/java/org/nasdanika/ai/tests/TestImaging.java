package org.nasdanika.ai.tests;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.List;

import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.GenericImageMetadata.GenericImageMetadataItem;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.common.ImageMetadata.ImageMetadataItem;
import org.apache.commons.imaging.formats.png.AbstractPngText;
import org.apache.commons.imaging.formats.png.PngImageMetadata;
import org.apache.commons.imaging.formats.png.PngImagingParameters;
import org.apache.commons.imaging.formats.png.PngWriter;
import org.junit.jupiter.api.Test;
import org.nasdanika.ai.UrlImageMetadataNarrator;

public class TestImaging {
	
	@Test
	public void testReadMetadata() throws Exception {
		String resourceName = "nasdanika-logo-with-metadata.png";
		URL imageResource = getClass().getResource(resourceName);
		ImageMetadata metadata = Imaging.getMetadata(imageResource.openStream(), resourceName);
		if (metadata != null) {
			for (ImageMetadataItem item: metadata.getItems()) {
				System.out.println(item);
			}
			PngImageMetadata pngMetadata = (PngImageMetadata) metadata;
			ImageMetadata textMetadata = pngMetadata.getTextualInformation();
			for (ImageMetadataItem item: textMetadata.getItems()) {
				GenericImageMetadataItem gItem = (GenericImageMetadataItem) item;
				System.out.println(gItem.getKeyword() + ": " + gItem.getText());
			}
		}				
	}
		
	@Test
	public void testUrlImageMetadataNarrator() throws Exception {
		String resourceName = "nasdanika-logo-with-metadata.png";
		URL imageResource = getClass().getResource(resourceName);
		UrlImageMetadataNarrator narrator = new UrlImageMetadataNarrator();
		System.out.print(narrator.generate(imageResource));
		System.out.print(narrator.generateAsync(imageResource).block());
	}	
	
	@Test
	public void testWriteMetadata() throws Exception {
		String resourceName = "nasdanika-logo.png";
		URL imageResource = getClass().getResource(resourceName);
		
		File outputFile = new File("target/nasdanika-logo-with-metadata.png");

		PngImagingParameters imagingParameters = new PngImagingParameters();
		imagingParameters.setTextChunks(List.of(
				new AbstractPngText.Text("Author", "Nasdanika"),
				new AbstractPngText.Text("Description", "Nasdanika logo")));

		try (OutputStream fos = new FileOutputStream(outputFile)) {
			PngWriter pngWriter = new PngWriter();
			pngWriter.writeImage(
			    Imaging.getBufferedImage(imageResource.openStream()),
			    fos,
			    imagingParameters,
			    null // No additional write parameters
			);
		}
	}	

}

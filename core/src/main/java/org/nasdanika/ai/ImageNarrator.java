package org.nasdanika.ai;

import java.awt.image.BufferedImage;

/**
 * Converts image to text (String).
 */
public interface ImageNarrator extends ImageEmbeddingGenerator<String>, Narrator<BufferedImage> {

}

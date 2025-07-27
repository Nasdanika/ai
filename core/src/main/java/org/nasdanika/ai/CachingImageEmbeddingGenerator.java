package org.nasdanika.ai;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import javax.imageio.ImageIO;

/**
 * Caches image embeddings in a map which can be loaded and saved between runs
 * Uses image digest as caching key
 */
public class CachingImageEmbeddingGenerator<E> extends MapCachingEmbeddingGenerator<BufferedImage, E, String> implements ImageEmbeddingGenerator<E> {
	
	private String algorithm;

	public CachingImageEmbeddingGenerator(ImageEmbeddingGenerator<E> target, Map<String, E> cache, String algorithm) {
		super(target, cache);
		this.algorithm = algorithm;
	}

	/**
	 * Uses SHA-512 algorithm
	 * @param cache
	 */
	public CachingImageEmbeddingGenerator(ImageEmbeddingGenerator<E> target, Map<String, E> cache) {
		this(target, cache, "SHA-512");
	}
	
	protected String computeKey(BufferedImage input) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try (baos) {
				ImageIO.write(input, "png", baos);
			}
			byte[] imageBytes = baos.toByteArray();
			MessageDigest digest = MessageDigest.getInstance(algorithm);
			byte[] dBytes = digest.digest(imageBytes);
			
		    StringBuilder sb = new StringBuilder();
		    for (byte b : dBytes) {
		        sb.append(String.format("%02x", b));
		    }
		    return sb.toString();
		} catch (IOException | NoSuchAlgorithmException e) {
			throw new IllegalArgumentException("Error computing image digest: " + e, e);
		}
	}
	
}

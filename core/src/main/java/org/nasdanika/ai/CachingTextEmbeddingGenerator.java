package org.nasdanika.ai;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

/**
 * Caches text embeddings in a map which can be loaded and saved between runs
 * Uses text digest as caching key
 */
public class CachingTextEmbeddingGenerator<E> extends MapCachingEmbeddingGenerator<String, E, String> implements TextEmbeddingGenerator<E> {
	
	private String algorithm;

	public CachingTextEmbeddingGenerator(TextEmbeddingGenerator<E> target, Map<String, E> cache, String algorithm) {
		super(target, cache);
		this.algorithm = algorithm;
	}

	/**
	 * Uses SHA-512 algorithm
	 * @param cache
	 */
	public CachingTextEmbeddingGenerator(TextEmbeddingGenerator<E> target, Map<String, E> cache) {
		this(target, cache, "SHA-512");
	}
	
	protected String computeKey(String input) {
		try {
			MessageDigest digest = MessageDigest.getInstance(algorithm);
			byte[] dBytes = digest.digest(input.getBytes(getCharset()));
			
		    StringBuilder sb = new StringBuilder();
		    for (byte b : dBytes) {
		        sb.append(String.format("%02x", b));
		    }
		    return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalArgumentException("Error computing text digest: " + e, e);
		}
	}

	protected Charset getCharset() {
		return StandardCharsets.UTF_8;
	}
	
}

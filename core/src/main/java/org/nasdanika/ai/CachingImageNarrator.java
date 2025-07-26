package org.nasdanika.ai;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import javax.imageio.ImageIO;

import reactor.core.publisher.Mono;

/**
 * Caches image narrations in a map which can be loaded and saved between runs
 * Uses image digest as caching key
 */
public class CachingImageNarrator implements ImageNarrator {
	
	private Map<String, String> cache;
	private String algorithm;
	private ImageNarrator narrator;

	public CachingImageNarrator(ImageNarrator narrator, Map<String, String> cache, String algorithm) {
		this.narrator = narrator;
		this.cache = cache;
		this.algorithm = algorithm;
	}

	/**
	 * Uses SHA-512 algorithm
	 * @param cache
	 */
	public CachingImageNarrator(ImageNarrator narrator, Map<String, String> cache) {
		this(narrator, cache, "SHA-512");
	}

	@Override
	public Mono<String> generateAsync(BufferedImage input) {
		String digest = digest(input);
		synchronized (cache) {
			if (cache.containsKey(digest)) {
				return Mono.just(cache.get(digest));
			}
		}
		
		return narrator.generateAsync(input).map(r -> {
			synchronized (cache) {
				cache.putIfAbsent(digest, r);
			}
			return r;
		});
	}

	@Override
	public String generate(BufferedImage input) {
		synchronized (cache) {
			return cache.computeIfAbsent(digest(input), d -> narrator.generate(input));
		}
	}
	
	protected String digest(BufferedImage input) {
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

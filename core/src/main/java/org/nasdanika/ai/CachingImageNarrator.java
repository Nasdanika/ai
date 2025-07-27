package org.nasdanika.ai;

import java.util.Map;

public class CachingImageNarrator extends CachingImageEmbeddingGenerator<String> implements ImageNarrator {

	public CachingImageNarrator(
			ImageEmbeddingGenerator<String> target, 
			Map<String, String> cache,
			String algorithm) {
		super(target, cache, algorithm);
	}

	public CachingImageNarrator(ImageEmbeddingGenerator<String> target, Map<String, String> cache) {
		super(target, cache);
	}
	
}

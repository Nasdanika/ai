package org.nasdanika.ai;

import java.util.ArrayList;
import java.util.List;

import reactor.core.publisher.Mono;

/**
 * 
 * @param <T> a container of tokens, e.g. int[] or char[] or List&lt;Integer&gt;
 */
public abstract class TextFloatVectorChunkingEmbeddingModel<T> implements TextFloatVectorEmbeddingModel {
	
	private TextFloatVectorEmbeddingModel target;
	private int chunkSize;
	private int overlap;

	/**
	 * 
	 * @param target
	 * @param chunkSize Chunk size, if non-positive, then target max input tokens is used as chunk size
	 * @param overlap
	 */
	protected TextFloatVectorChunkingEmbeddingModel(
			TextFloatVectorEmbeddingModel target,
			int chunkSize, 
			int overlap) {
		this.target = target;
		this.chunkSize = chunkSize > 0 ? chunkSize : target.getMaxInputTokens();
		this.overlap = overlap;
	}

	@Override
	public String getProvider() {
		return target.getProvider();
	}

	@Override
	public String getName() {
		return target.getName();
	}

	@Override
	public String getVersion() {
		return target.getVersion();
	}

	@Override
	public int getMaxInputTokens() {
		return -1;
	}

	@Override
	public boolean isTooLong(String input) {
		return false;
	}

	@Override
	public int getDimensions() {
		return target.getDimensions();
	}
	
	public List<String> chunk(String input) {
		List<String> result = new ArrayList<>();
		T tokens = encode(input);
		for (int i = 0, l = size(tokens); i < l; i += chunkSize) {
			if (i > overlap) {
				i -= overlap;
			}
			T slice = slice(tokens, i, chunkSize);
			result.add(decode(slice));
		}
		return result;		
	}

	@Override
	public Mono<List<List<Float>>> generateAsync(String input) {
		List<String> chunks = chunk(input);
		return target.generateAsync(chunks).map(chunkMap -> {
			List<List<Float>> result = new ArrayList<>();
			for (String chunk: chunks) {
				result.addAll(chunkMap.get(chunk));
			}
			return result;
		});
	}
	
	@Override
	public List<List<Float>> generate(String input) {
		List<List<Float>> result = new ArrayList<>();
		for (String chunk: chunk(input)) {		
			result.addAll(target.generate(chunk));
		}
		return result;
	}
	
	/**
	 * Encodes a string into tokens
	 * @param input
	 * @return
	 */
	protected abstract T encode(String input);
	
	/**
	 * Decodes a string from an array of tokens
	 * @param tokens
	 * @return
	 */
	protected abstract String decode(T tokens);
	
	protected abstract int size(T tokens);
	
	protected abstract T slice(T tokens, int offset, int length);
	
}

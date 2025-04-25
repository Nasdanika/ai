package org.nasdanika.ai;

import java.util.ArrayList;
import java.util.List;

import reactor.core.publisher.Mono;

/**
 * 
 * @param <T> a container of tokens, e.g. int[] or char[] or List&lt;Integer&gt;
 */
public abstract class ChunkingEmbeddings<T> implements Embeddings {
	
	private Embeddings target;
	private int chunkSize;
	private int overlap;

	protected ChunkingEmbeddings(
			Embeddings target,
			int chunkSize, 
			int overlap) {
		this.target = target;
		this.chunkSize = chunkSize;
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

	@Override
	public Mono<List<List<Float>>> generateAsync(String input) {
		List<String> chunks = new ArrayList<>();
		T tokens = encode(input);
		for (int i = 0, l = size(tokens); i < l; i += chunkSize) {
			T slice = slice(tokens, i > overlap ? i - overlap : i, chunkSize);
			chunks.add(decode(slice));
		}

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
		T tokens = encode(input);
		for (int i = 0, l = size(tokens); i < l; i += chunkSize) {
			if (i > overlap) {
				i -= overlap;
			}
			T slice = slice(tokens, i, chunkSize);
			String chunk = decode(slice);
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

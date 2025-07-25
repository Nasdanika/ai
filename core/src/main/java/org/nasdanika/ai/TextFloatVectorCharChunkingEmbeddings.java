package org.nasdanika.ai;

import java.util.Arrays;

/**
 * A simple implementation which treats a character as a token.
 * Can be used for testing and when an encoder is unknown or not available.  
 */
public class TextFloatVectorCharChunkingEmbeddings extends TextFloatVectorChunkingEmbeddings<char[]> {

	public TextFloatVectorCharChunkingEmbeddings(TextFloatVectorEmbeddingModel target, int chunkSize, int overlap) {
		super(target, chunkSize, overlap);
	}

	@Override
	protected char[] encode(String input) {
		return input.toCharArray();
	}

	@Override
	protected String decode(char[] tokens) {
		return String.valueOf(tokens);
	}

	@Override
	protected int size(char[] tokens) {
		return tokens.length;
	}

	@Override
	protected char[] slice(char[] tokens, int offset, int length) {
		return Arrays.copyOfRange(tokens, offset, Math.min(tokens.length, offset + length));
	}

}

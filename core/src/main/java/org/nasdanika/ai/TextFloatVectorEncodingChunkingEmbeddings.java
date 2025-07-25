package org.nasdanika.ai;

import java.util.Arrays;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import com.knuddels.jtokkit.api.IntArrayList;

public class TextFloatVectorEncodingChunkingEmbeddings extends TextFloatVectorChunkingEmbeddings<IntArrayList> {
	
	private Encoding encoding;

	public TextFloatVectorEncodingChunkingEmbeddings(
			TextFloatVectorEmbeddingModel target,
			int chunkSize, 
			int overlap,
			Encoding encoding) {
		
		super(target, chunkSize, overlap);
		this.encoding = encoding;
	}

	public TextFloatVectorEncodingChunkingEmbeddings(
			TextFloatVectorEmbeddingModel target,
			int chunkSize, 
			int overlap,
			EncodingType encodingType) {
		
		super(target, chunkSize, overlap);
		EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
		encoding = registry.getEncoding(encodingType);
	}
	
	@Override
	protected IntArrayList encode(String input) {
		return encoding.encode(input);
	}

	@Override
	protected String decode(IntArrayList tokens) {
		return encoding.decode(tokens);
	}

	@Override
	protected int size(IntArrayList tokens) {
		return tokens.size();
	}

	@Override
	protected IntArrayList slice(IntArrayList tokens, int offset, int length) {
		int[] ia = tokens.toArray();
		IntArrayList slice = new IntArrayList(); 
		for (int i: Arrays.copyOfRange(ia, offset, Math.min(ia.length, offset + length))) {
			slice.add(i);
		}
		return slice;
	}

}

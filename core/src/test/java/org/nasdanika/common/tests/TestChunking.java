package org.nasdanika.common.tests;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.nasdanika.ai.CharChunkingEmbeddings;
import org.nasdanika.ai.Embeddings;
import org.nasdanika.ai.EncodingChunkingEmbeddings;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;

import reactor.core.publisher.Mono;

public class TestChunking {
	
	Embeddings fakeEmbeddings = new Embeddings() {

		@Override
		public String getProvider() {
			return null;
		}

		@Override
		public String getName() {
			return null;
		}

		@Override
		public String getVersion() {
			return null;
		}

		@Override
		public int getMaxInputTokens() {
			return 0;
		}

		@Override
		public boolean isTooLong(String input) {
			return false;
		}

		@Override
		public int getDimensions() {
			return 0;
		}

		@Override
		public Mono<List<List<Float>>> generateAsync(String input) {
			System.out.println(input);
			List<List<Float>> result = new ArrayList<>();
			List<Float> re = new ArrayList<>();
			result.add(re);
			input.chars().forEach(ch -> re.add((float) ch));
			return Mono.just(result);
		}
		
	};
	
	@Test
	public void testCharChunking() {
		CharChunkingEmbeddings cce = new CharChunkingEmbeddings(fakeEmbeddings, 5, 2);		
		List<List<Float>> vectors = cce.generate("1234567890");
		
		for (List<Float> vector: vectors) {
			System.out.println(vector);
		}
		
	}
	
	@Test
	public void testEncodingChunking() {
		EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
		Encoding enc = registry.getEncoding(EncodingType.CL100K_BASE);
		Embeddings ece = new EncodingChunkingEmbeddings(fakeEmbeddings, 5, 1, enc);		
		List<List<Float>> vectors = ece.generate("A quick brown fox jumps over a sleepy dog");
		
		for (List<Float> vector: vectors) {
			System.out.println(vector);
		}
		
	}	

}

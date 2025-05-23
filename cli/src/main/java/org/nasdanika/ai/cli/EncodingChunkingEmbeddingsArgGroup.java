package org.nasdanika.ai.cli;

import org.nasdanika.ai.Embeddings;
import org.nasdanika.ai.EncodingChunkingEmbeddings;

import com.knuddels.jtokkit.api.EncodingType;
import com.knuddels.jtokkit.api.IntArrayList;

import io.opentelemetry.api.trace.Span;
import picocli.CommandLine.Option;

public class EncodingChunkingEmbeddingsArgGroup extends ChunkingEmbeddingsArgGroup<IntArrayList> {

	@Option( 
			names = "--chunk-encoding-type",
			description = {
					"Chunk encoding type",
					"Valid values: ${COMPLETION-CANDIDATES}",
					"Default value: CL100K_BASE"
			})	
	protected EncodingType encodingType = EncodingType.CL100K_BASE;	
	
	@Override
	public EncodingChunkingEmbeddings createChunkingEmbeddings(Embeddings target) {
		return new EncodingChunkingEmbeddings(target, chunkSize, chunksOverlap, encodingType);
	}
	
	@Override
	public void setSpanAttributes(Span span) {
		span.setAttribute("chunk.encoding", encodingType.name());		
	}

}

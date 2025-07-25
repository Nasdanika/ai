package org.nasdanika.ai.cli;

import org.nasdanika.ai.TextFloatVectorChunkingEmbeddings;
import org.nasdanika.ai.TextFloatVectorEmbeddingModel;

import io.opentelemetry.api.trace.Span;
import picocli.CommandLine.Option;

public abstract class TextFloatVectorChunkingEmbeddingsArgGroup<T> {
	
	@Option( 
			names = "--chunk-size",
			description = "Chunk size in tokens")
	protected int chunkSize;
	
	@Option( 
			names = "--chunks-overlap",
			description = "Chunks overlap in tokens")
	protected int chunksOverlap;
	
	public abstract TextFloatVectorChunkingEmbeddings<T> createChunkingEmbeddings(TextFloatVectorEmbeddingModel target); 
	
	public void setSpanAttributes(Span span) {
		span.setAttribute("chunk.size", chunkSize);
		span.setAttribute("chunk.overlap", chunksOverlap);
	}

}

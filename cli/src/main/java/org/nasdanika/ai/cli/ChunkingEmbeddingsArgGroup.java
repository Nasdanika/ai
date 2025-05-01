package org.nasdanika.ai.cli;

import org.nasdanika.ai.ChunkingEmbeddings;
import org.nasdanika.ai.Embeddings;

import picocli.CommandLine.Option;

public abstract class ChunkingEmbeddingsArgGroup<T> {
	
	@Option( 
			names = "--chunk-size",
			description = "Chunk size in tokens")
	protected int chunkSize;
	
	@Option( 
			names = "--chunks-overlap",
			description = "Chunks overlap in tokens")
	protected int chunksOverlap;
	
	public abstract ChunkingEmbeddings<T> createChunkingEmbeddings(Embeddings target); 

}

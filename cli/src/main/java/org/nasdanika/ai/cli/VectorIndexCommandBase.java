package org.nasdanika.ai.cli;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.nasdanika.ai.Embeddings;
import org.nasdanika.ai.EncodingChunkingEmbeddings;
import org.nasdanika.capability.CapabilityLoader;
import org.nasdanika.cli.ProgressMonitorMixIn;
import org.nasdanika.cli.TelemetryCommand;
import org.nasdanika.common.ProgressMonitor;

import com.github.jelmerk.hnswlib.core.Item;
import com.github.jelmerk.hnswlib.core.hnsw.HnswIndex;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Parameters;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Base command for creating vector index files by generating embeddings.
 */
public abstract class VectorIndexCommandBase extends TelemetryCommand {
	
	/**
	 * Index id - item URI and embedding vector index for URIs with multiple vectors/chunks.
	 */
	public record IndexId(String uri, int index) implements Serializable {}
	
	/**
	 * Vector index item
	 */
	public record EmbeddingsItem(IndexId id, float[] vector, int dimensions) implements Item<IndexId,float[]> {}	
	

	public VectorIndexCommandBase(OpenTelemetry openTelemetry, CapabilityLoader capabilityLoader) {
		super(openTelemetry, capabilityLoader);
	}
		
	@Parameters(
		index =  "0",	
		arity = "1",
		description = "Index output file")
	private File output;
	
	@ArgGroup(
			heading = "Progress monitor%n",
			exclusive = false)
	private ProgressMonitorMixIn progressMonitorMixIn;
	
	@ArgGroup(
			heading = "Chunking%n",
			exclusive = false)
	private EncodingChunkingEmbeddingsArgGroup encodingChunkingEmbeddingsArgGroup;
	
	@ArgGroup(
			heading = "Embeddings%n",
			exclusive = false)
	private EmbeddingsArgGroup embeddingsArgGroup;
		
	@ArgGroup(
			heading = "Vector index%n",
			exclusive = false)
	private HnswIndexBuilderFloatArgGroup vectorIndexArgGroup;	
		
	@Override
	public Integer execute(Span commandSpan) throws Exception {
		if (progressMonitorMixIn == null) {
			progressMonitorMixIn = new ProgressMonitorMixIn();
		}
		try (ProgressMonitor progressMonitor = progressMonitorMixIn.createProgressMonitor(1)) {
			if (encodingChunkingEmbeddingsArgGroup == null) {
				encodingChunkingEmbeddingsArgGroup = new EncodingChunkingEmbeddingsArgGroup();
			}
			encodingChunkingEmbeddingsArgGroup.setSpanAttributes(commandSpan);
			if (embeddingsArgGroup == null) {
				embeddingsArgGroup = new EmbeddingsArgGroup();
			}
			embeddingsArgGroup.setSpanAttributes(commandSpan);
			if (vectorIndexArgGroup == null) {
				vectorIndexArgGroup = new HnswIndexBuilderFloatArgGroup();
			}
			vectorIndexArgGroup.setSpanAttributes(commandSpan);
			
			Embeddings embeddings = embeddingsArgGroup.loadOne(getCapabilityLoader(), progressMonitor);
			EncodingChunkingEmbeddings chunkingEmbeddings = encodingChunkingEmbeddingsArgGroup.createChunkingEmbeddings(embeddings);
	
			Function<Map.Entry<String,String>, Flux<EmbeddingsItem>> mapper = entry -> {
				Mono<List<List<Float>>> vectorsMono = chunkingEmbeddings.generateAsync(entry.getValue());
				return vectorsMono.map(vectors -> {
					List<EmbeddingsItem> result = new ArrayList<>();
					int idx = 0;
					for (List<Float> vector: vectors) {
						float[] fVector = new float[vector.size()];
						for (int j = 0; j < fVector.length; ++j) {
							fVector[j] = vector.get(j);
						}
						result.add(new EmbeddingsItem(
								new IndexId(entry.getKey(), idx++), 
								vectorIndexArgGroup.normalize(fVector), 
								vector.size()));						
					}
					return result;
				}).flatMapIterable(Function.identity());
			};
			
			List<EmbeddingsItem> items = getItems(commandSpan)
				.flatMap(mapper)
				.collect(Collectors.toList())
				.block();
			
			commandSpan.addEvent(
					"items-loaded", 
					Attributes
						.builder()
						.put("size", items.size())
						.build());
	
			HnswIndex<IndexId, float[], EmbeddingsItem, Float> index = vectorIndexArgGroup.buildAndAddAll(embeddings.getDimensions(), items, commandSpan);
			index.save(output);		
			
			return 0;
		}
	}
	
	/**
	 * A {@link Flux} of items which are mapped to {@link EmbeddingsItem} and then stored to the index. 
	 * @param commandSpan
	 * @return
	 */
	protected abstract Flux<Map.Entry<String,String>> getItems(Span commandSpan);

}

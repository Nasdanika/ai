package org.nasdanika.ai.cli;

import org.nasdanika.common.Description;

import com.github.jelmerk.hnswlib.core.DistanceFunction;
import com.github.jelmerk.hnswlib.core.hnsw.HnswIndex;
import com.github.jelmerk.hnswlib.core.hnsw.HnswIndex.Builder;

import picocli.CommandLine.Option;

public abstract class HnswIndexBuilderArgGroup<TVector, TDistance extends Comparable<TDistance>> {
		
	@Option( 
			names = "--hnsw-ef",
			description = {
					"Chunk encoding type",
					"Valid values: ${COMPLETION-CANDIDATES}",
					"Default value: ${DEFAULT-VALUE}"
			},
			defaultValue = "200")	
	@Description(
			"""
			
			""")
	protected int ef;	
	
	@Option( 
			names = "--hnsw-ef-contruction",
			description = {
					"Chunk encoding type",
					"Valid values: ${COMPLETION-CANDIDATES}",
					"Default value: ${DEFAULT-VALUE}"
			},
			defaultValue = "200")	
	@Description(
			"""
			
			""")
	protected int efConstruction;	
	
	@Option( 
			names = "--hnsw-ef-contruction",
			description = {
					"Chunk encoding type",
					"Valid values: ${COMPLETION-CANDIDATES}",
					"Default value: ${DEFAULT-VALUE}"
			},
			defaultValue = "16")	
	@Description(
			"""
			
			""")
	protected int m;	
	
	@Option( 
		names = "--hnsw-remove-enabled",
		description = {
				"Chunk encoding type",
				"Valid values: ${COMPLETION-CANDIDATES}",
				"Default value: ${DEFAULT-VALUE}"
		})	
	protected boolean removeEnabled;	
	
	public HnswIndex.Builder<TVector, TDistance> createIndexBuilder(int dimensions, int maxItemCount) {
		Builder<TVector, TDistance> builder = HnswIndex.newBuilder(1536, getDistanceFunction(), maxItemCount)
		.withM(m)
		.withEf(ef)
		.withEfConstruction(efConstruction);
		
		if (removeEnabled) {
			builder.withRemoveEnabled();
		}
		
		return builder;
	}

	protected abstract DistanceFunction<TVector, TDistance> getDistanceFunction();

}

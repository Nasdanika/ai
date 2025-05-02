package org.nasdanika.ai.cli;

import java.util.Collection;

import org.nasdanika.common.Description;

import com.github.jelmerk.hnswlib.core.DistanceFunction;
import com.github.jelmerk.hnswlib.core.Index;
import com.github.jelmerk.hnswlib.core.Item;
import com.github.jelmerk.hnswlib.core.ProgressListener;
import com.github.jelmerk.hnswlib.core.hnsw.HnswIndex;
import com.github.jelmerk.hnswlib.core.hnsw.HnswIndex.Builder;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.Span;
import picocli.CommandLine.Option;

public abstract class HnswIndexBuilderArgGroup<TVector, TDistance extends Comparable<TDistance>> {
		
	private static final int AVAILABLE_PROCESSORS = Runtime.getRuntime().availableProcessors();

	@Option( 
			names = "--hnsw-ef",
			description = {
					"Size of the dynamic list for the nearest neighbors",
					"Default value: ${DEFAULT-VALUE}"
			},
			defaultValue = "200")	
	@Description(
			"""
			The size of the dynamic list for the nearest neighbors (used during the search). 
			Higher ``ef`` leads to more accurate but slower search. 
			The value ef of can be anything between ``k`` (number of items to return from search) and the size of the dataset.
			
			[^ef-javadoc]: [ef javadoc](https://javadoc.io/static/com.github.jelmerk/hnswlib-core/1.2.0/com/github/jelmerk/hnswlib/core/hnsw/HnswIndex.BuilderBase.html#withEf(int)) 									
			""")
	protected int ef;	
	
	@Option( 
			names = "--hnsw-ef-contruction",
			description = {
					"Controls the index time / index precision",
					"Default value: ${DEFAULT-VALUE}"
			},
			defaultValue = "200")	
	@Description(
			"""
			The option has the same meaning as ``--hnsw-ef``, but controls the index time / index precision. 
			Bigger ``ef-construction`` leads to longer construction, but better index quality. 
			At some point, increasing ``ef-construction`` does not improve the quality of the index. 
			One way to check if the selection of ``ef-construction`` was ok is to measure a recall for 
			``M`` nearest neighbor search when ``ef = ef-construction``: if the recall is lower than ``0.9``,
			then there is room for improvement.
			
			[^ef-construction-javadoc]: [ef-construction javadoc](https://javadoc.io/static/com.github.jelmerk/hnswlib-core/1.2.0/com/github/jelmerk/hnswlib/core/hnsw/HnswIndex.BuilderBase.html#withEfConstruction(int)) 									
			""")
	protected int efConstruction;	
	
	@Option( 
			names = "--hnsw-m",
			description = {
					"The number of bi-directional links created",
					"for every new element during construction",
					"Default value: ${DEFAULT-VALUE}"
			},
			defaultValue = "16")	
	@Description(
			"""
			Sets the number of bi-directional links created for every new element during construction.
			Reasonable range for m is ``2-100``. Higher m work better on datasets with high intrinsic dimensionality and/or high recall,
			while low m work better for datasets with low intrinsic dimensionality and/or low recalls.
			The parameter also determines the algorithm's memory consumption.
			As an example for ``d = 4`` random vectors optimal ``m`` for search is somewhere around ``6``,
			while for high dimensional datasets (word embeddings, good face descriptors), 
			higher ``m`` are required (e.g. ``m = 48, 64``) for optimal	performance at high recall. 
			The range ``m = 12-48`` is ok for the most of the use cases. 
			When ``m`` is changed one has to update the other parameters. 
			Nonetheless, ``ef`` and ``efConstruction`` parameters can be roughly estimated by
			assuming that ``m``  ``efConstruction`` is a constant[^m-javadoc].
			
			[^m-javadoc]: [m javadoc](https://javadoc.io/static/com.github.jelmerk/hnswlib-core/1.2.0/com/github/jelmerk/hnswlib/core/hnsw/HnswIndex.BuilderBase.html#withM(int)) 
			""")
	protected int m;	
	
	@Option( 
		names = "--hnsw-remove-enabled",
		description = "If true, removal from the index is enabled"
			)	
	protected boolean removeEnabled;

	
	@Option( 
		names = "--hnsw-threads",
		description = {
				"Number of threads to use for parallel indexing",
				"Default to the number of available processors"
		})	
	private int threads = AVAILABLE_PROCESSORS;

	@Option( 
			names = "--hnsw-progress-update-interval",
			description = {
					"After indexing this many items progress will be",
					"reported. The last element will always be",
					"reported regardless of this setting. ",
					"Default value: " + Index.DEFAULT_PROGRESS_UPDATE_INTERVAL
			})		
	private int progressUpdateInterval = Index.DEFAULT_PROGRESS_UPDATE_INTERVAL;	
	
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
	
	public void setSpanAttributes(Span span) {
		span.setAttribute("hnsw.ef", ef);
		span.setAttribute("hnsw.ef-construction", efConstruction);
		span.setAttribute("hnsw.m", m);
		span.setAttribute("hnsw.remove-enabled", removeEnabled);		
	}
		
	public <TId, TItem extends Item<TId, TVector>> HnswIndex<TId, TVector, TItem, TDistance> buildAndAddAll(
			int dimensions, 
			Collection<TItem> items,
			Span span) throws InterruptedException {
		HnswIndex<TId, TVector, TItem, TDistance> index = createIndexBuilder(dimensions, items.size()).build();
		ProgressListener progressListener = (workDone, max) -> {
			AttributesBuilder ab = Attributes.builder();
			ab.put("done", workDone);
			ab.put("total", max);
			ab.put("percent", (100L * workDone) / max);			
			span.addEvent("hnsw.progress", ab.build());
		};
		index.addAll(items, threads, progressListener, progressUpdateInterval);
		return index;		
	}
	
}

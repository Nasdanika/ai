package org.nasdanika.ai.cli;

import com.github.jelmerk.hnswlib.core.DistanceFunction;
import com.github.jelmerk.hnswlib.core.DistanceFunctions;
//import com.github.jelmerk.hnswlib.jdk17.Jdk17DistanceFunctions;
import com.github.jelmerk.hnswlib.util.VectorUtils;

import io.opentelemetry.api.trace.Span;
import picocli.CommandLine.Option;

public class HnswIndexBuilderFloatArgGroup extends HnswIndexBuilderArgGroup<float[], Float> {
	
	public enum Distance {
		
		BRAY_CURTIS(DistanceFunctions.FLOAT_BRAY_CURTIS_DISTANCE),
		CANBERRA(DistanceFunctions.FLOAT_CANBERRA_DISTANCE),
		CORRELATION(DistanceFunctions.FLOAT_CORRELATION_DISTANCE),
		COSINE(DistanceFunctions.FLOAT_COSINE_DISTANCE),
		EUCLIDEAN(DistanceFunctions.FLOAT_EUCLIDEAN_DISTANCE),
		INNER_PRODUCT(DistanceFunctions.FLOAT_INNER_PRODUCT),
		MANHATTAN(DistanceFunctions.FLOAT_MANHATTAN_DISTANCE);
		
		// JDK 17
		
//		VECTOR_FLOAT_128_BRAY_CURTIS(Jdk17DistanceFunctions.VECTOR_FLOAT_128_BRAY_CURTIS_DISTANCE),
//		VECTOR_FLOAT_128_CANBERRA(Jdk17DistanceFunctions.VECTOR_FLOAT_128_CANBERRA_DISTANCE),
//		VECTOR_FLOAT_128_COSINE(Jdk17DistanceFunctions.VECTOR_FLOAT_128_COSINE_DISTANCE),
//		VECTOR_FLOAT_128_EUCLIDEAN(Jdk17DistanceFunctions.VECTOR_FLOAT_128_EUCLIDEAN_DISTANCE),
//		VECTOR_FLOAT_128_INNER_PRODUCT(Jdk17DistanceFunctions.VECTOR_FLOAT_128_INNER_PRODUCT),
//		VECTOR_FLOAT_128_MANHATTAN(Jdk17DistanceFunctions.VECTOR_FLOAT_128_MANHATTAN_DISTANCE),
//		VECTOR_FLOAT_256_BRAY_CURTIS(Jdk17DistanceFunctions.VECTOR_FLOAT_256_BRAY_CURTIS_DISTANCE),
//		VECTOR_FLOAT_256_CANBERRA(Jdk17DistanceFunctions.VECTOR_FLOAT_256_CANBERRA_DISTANCE),
//		VECTOR_FLOAT_256_COSINE(Jdk17DistanceFunctions.VECTOR_FLOAT_256_COSINE_DISTANCE),
//		VECTOR_FLOAT_256_EUCLIDEAN(Jdk17DistanceFunctions.VECTOR_FLOAT_256_EUCLIDEAN_DISTANCE),
//		VECTOR_FLOAT_256_INNER_PRODUCT(Jdk17DistanceFunctions.VECTOR_FLOAT_256_INNER_PRODUCT),
//		VECTOR_FLOAT_256_MANHATTAN(Jdk17DistanceFunctions.VECTOR_FLOAT_256_MANHATTAN_DISTANCE);					
		
		public final DistanceFunction<float[], Float> distanceFunction;
		
		Distance(DistanceFunction<float[], Float> distanceFunction) {
			this.distanceFunction = distanceFunction;
		}
		
	}
	
	@Option( 
			names = "--hnsw-distance-function",
			description = {
					"Vector distance function",
					"Valid values: ${COMPLETION-CANDIDATES}",
					"Default value: COSINE"
			})	
	protected Distance distanceFunction = Distance.COSINE;	
	
	@Option( 
			names = "--hnsw-normalize",
			description = "If true, vectors are normalized"
			)	
	protected boolean normalize;		

	@Override
	protected DistanceFunction<float[], Float> getDistanceFunction() {
		return distanceFunction.distanceFunction;
	}
		
	/**
	 * Normalizes the argument vector if normalize option is true
	 * @param vector
	 * @return
	 */
	public float[] normalize(float[] vector) {
		return normalize ? VectorUtils.normalize(vector) : vector;
	}	
	
	@Override
	public void setSpanAttributes(Span span) {
		span.setAttribute("hnsw.distance-function", distanceFunction.name());
		span.setAttribute("hnsw.normalize", normalize);
	}	

}

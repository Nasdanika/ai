package org.nasdanika.ai;

import java.util.List;

import reactor.core.publisher.Mono;

public interface FloatVectorSimilarityComputer extends VectorSimilarityComputer<Float, Float> {
	
    /**
     * Calculates the cosine similarity.
     */
    static FloatVectorSimilarityComputer COSINE_SIMILARITY_COMPUTER = new FloatVectorSimilarityComputer() {
		
		@Override
		public Mono<Float> computeAsync(List<Float> a, List<Float> b) {
            float dot = 0.0f;
            float nru = 0.0f;
            float nrv = 0.0f;
            for (int i = 0; i < a.size(); i++) {
                dot += a.get(i) * b.get(i);
                nru += a.get(i) * a.get(i);
                nrv += b.get(i) * b.get(i);
            }

            return Mono.just(dot / (float)(Math.sqrt(nru) * Math.sqrt(nrv)));
		}
	};

//    INNER_PRODUCT_SIMILARITY
//    EUCLIDEAN_SIMILARITY
//    CANBERRA_SIMILARITY
//    BRAY_CURTIS_SIMILARITY 
//    CORRELATION_SIMILARITY
//    MANHATTAN_SIMILARITY

}

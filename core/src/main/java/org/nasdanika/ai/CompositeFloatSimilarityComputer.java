package org.nasdanika.ai;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import reactor.core.publisher.Mono;

public class CompositeFloatSimilarityComputer<T> implements SimilarityComputer<T,Float> {
	
	protected Collection<Map.Entry<SimilarityComputer<? super T, Float>,Float>> computers = Collections.synchronizedCollection(new ArrayList<>());
	protected float totalWeight;
	
	public synchronized void addComputer(SimilarityComputer<? super T, Float> computer, float weight) {
		if (weight != 0) {
			computers.add(Map.entry(computer, weight));
			totalWeight += weight;
		}
	}

	@Override
	public Mono<Float> computeAsync(T a, T b) {
		if (computers.isEmpty() || totalWeight == 0) {
			return Mono.just(0.0f);
		}
		List<Mono<Float>> results = computers
			.stream()
			.map(e -> e.getKey().computeAsync(a, b).map(r -> r * e.getValue()))
			.toList();
		
		return Mono.zip(results, ra -> {
			float total = 0;
			for (int i = 0; i < ra.length; ++i) {
				total += (Float) ra[i];
			}
			return total / totalWeight;
		});		
	}

}

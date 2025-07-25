package org.nasdanika.ai;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import reactor.core.publisher.Mono;

/**
 * Splits input by whitespace, lowercases and then computes frequency of each word
 */
public class BagOfWordsGenerator implements TextEmbeddingGenerator<Map<String,Integer>> {

	@Override
	public Mono<Map<String, Integer>> generateAsync(String source) {
		if (source == null || source.trim().length() == 0) {
			return Mono.just(Collections.emptyMap());
		}
		Map<String, Integer> result = new TreeMap<>();
		Stream
			.of(source.split("\\s+"))
			.map(String::toLowerCase)
			.collect(Collectors.groupingBy(Function.identity()))
			.entrySet()
			.forEach(e -> result.put(e.getKey(), e.getValue().size()));
		return Mono.just(result);
	}

}

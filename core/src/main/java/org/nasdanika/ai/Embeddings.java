package org.nasdanika.ai;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import reactor.core.publisher.Mono;

/**
 * Embeddings "business" interface focusing on ease of use and leaving
 * token usage reporting to implementations.
 */
public interface Embeddings extends Model {
	
	/**
	 * Embeddings requirement.
	 * String attributes match any value if null.
	 */
	record Requirement(
		String provider,
		String model,
		String version) {}
	
	/**
	 * 
	 * @param input
	 * @return true if the input is too long for a given model
	 */
	boolean isTooLong(String input);
	
	/**
	 * @return number of dimentions
	 */
	int getDimensions();
	
	/**
	 * Generates embeddings for a single string
	 * @param model
	 * @param input
	 * @return
	 */
	default List<List<Float>> generate(String input) {
		return generateAsync(input).block();
	}
	
	/**
	 * Asynchronously generates embeddings for a single string
	 * @param model
	 * @param input
	 * @return
	 */
	Mono<List<List<Float>>> generateAsync(String input);
	
	/**
	 * Batch generation
	 * @param input a list of input strings
	 * @return 
	 */
	default Map<String, List<List<Float>>> generate(List<String> input) {
		return generateAsync(input).block();
	}
	
	/**
	 * Asynchronous batch generation
	 * @param input a list of input strings
	 * @return 
	 */
	default Mono<Map<String, List<List<Float>>>> generateAsync(List<String> input) {
		List<Mono<Entry<String,List<List<Float>>>>> monos = input
			.stream()
			.map(ie -> {
				Mono<List<List<Float>>> emb = generateAsync(ie);
				return emb.map(vector -> Map.entry(ie, vector));
			})
			.toList();
		
		return Mono.zip(monos, this::combine);
	}
	
	private Map<String, List<List<Float>>> combine(Object[] elements) {
		Map<String, List<List<Float>>> ret = new LinkedHashMap<>();
		for (Object el: elements) {
			@SuppressWarnings("unchecked")
			Entry<String,List<List<Float>>> e = (Entry<String,List<List<Float>>>) el;
			ret.put(e.getKey(), e.getValue());
		}		
		return ret;
	}

}

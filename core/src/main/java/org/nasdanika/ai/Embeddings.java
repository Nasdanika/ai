package org.nasdanika.ai;

import java.util.List;
import java.util.Map;

import reactor.core.publisher.Mono;

/**
 * Embeddings "business" interface focusing on ease of use and leaving
 * token usage reporting to implementations.
 */
public interface Embeddings extends Model {
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
	default List<Float> generate(String input) {
		return generateAsync(input).block();
	}
	
	/**
	 * Asynchronously generates embeddings for a single string
	 * @param model
	 * @param input
	 * @return
	 */
	Mono<List<Float>> generateAsync(String input);
	
	/**
	 * Batch generation
	 * @param input a list of input strings
	 * @return 
	 */
	default Map<String, List<Float>> generate(List<String> input) {
		return generateAsync(input).block();
	}
	
	/**
	 * Asynchronous batch generation
	 * @param input a list of input strings
	 * @return 
	 */
	Mono<Map<String, List<Float>>> generateAsync(List<String> input);

}

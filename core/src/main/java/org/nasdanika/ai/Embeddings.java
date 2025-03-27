package org.nasdanika.ai;

import java.util.List;
import java.util.Map;

/**
 * Embeddings "business" interface focusing on ease of use and leaving
 * token usage reporting to implementations.
 */
public interface Embeddings {
	
	/**
	 * Provider name - OpenAI, Ollama, ...
	 * @return
	 */
	String getProvider();
	
	/**
	 * Model name
	 * @return
	 */
	String getModel();
	
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
	List<Float> generate(String input);
	
	/**
	 * Batch generation
	 * @param input a list of input strings
	 * @return 
	 */
	Map<String, List<Float>> generate(List<String> input);	

}

package org.nasdanika.ai;

/**
 * Base interface for interfaces to work with (large language) models.
 */
public interface Model {
		
	/**
	 * Provider name - OpenAI, Ollama, ...
	 * @return
	 */
	String getProvider();
	
	/**
	 * Model name
	 * @return
	 */
	String getName();
	
	String getVersion();
		
	int getMaxInputTokens();
	
}

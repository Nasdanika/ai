package org.nasdanika.ai;

/**
 * Model coordinates (identifier)
 */
public interface Coordinates {
		
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
	
}

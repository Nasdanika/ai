package org.nasdanika.ai;

/**
 * Base interface for interfaces to work with (large language) models.
 */
public interface Model extends Coordinates {
				
	int getMaxInputTokens();
	
}

package org.nasdanika.ai;

import java.util.List;

/**
 * A pre-computed embeddings
 */
public interface EmbeddingsResourceContents extends Coordinates {
	
	String getUri();
	
	String getContent();
	
	int getDimensions();
	
	List<List<Float>> getEmbeddings();
	
}

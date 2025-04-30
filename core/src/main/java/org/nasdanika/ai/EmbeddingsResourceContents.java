package org.nasdanika.ai;

import java.util.List;

/**
 * A pre-computed embeddings
 */
public interface EmbeddingsResourceContents extends Coordinates {
	
	String getUri();
	
	String getMimeType();
	
	String getContents();
	
	int getDimensions();
	
	List<List<Float>> getEmbeddings();
	
}

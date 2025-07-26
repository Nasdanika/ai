package org.nasdanika.ai;

import java.util.List;

/**
 * TextFloatVectorEmbeddingModel "business" interface focusing on ease of use and leaving
 * token usage reporting to implementations.
 */
public interface TextFloatVectorEmbeddingModel extends EmbeddingModel<String, List<List<Float>>>, TextEmbeddingGenerator<List<List<Float>>>, FloatVectorEmbeddingGenerator<String>, Model {
	
	/**
	 * Creates TextFloatVectorEmbeddingModel requirement.
	 * String attributes match any value if null.
	 */
	static Requirement createRequirement(
			String provider,
			String model,
			String version) {
		
		return new Requirement(
				String.class, 
				List.class,
				TextFloatVectorEmbeddingModel.class::isAssignableFrom,
				em -> {
					TextFloatVectorEmbeddingModel tfvem = (TextFloatVectorEmbeddingModel) em;
					if (model != null && !model.equals(tfvem.getName())) {
						return false;
					}
					if (provider != null && !provider.equals(tfvem.getProvider())) {
						return false;
					}
					return version == null || version.equals(tfvem.getVersion());
				});		
	}
	
	/**
	 * 
	 * @param input
	 * @return true if the input is too long for a given model
	 */
	boolean isTooLong(String input);

}

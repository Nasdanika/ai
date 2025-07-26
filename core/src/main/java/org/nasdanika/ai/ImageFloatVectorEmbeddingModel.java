package org.nasdanika.ai;

import java.awt.image.BufferedImage;
import java.util.List;

public interface ImageFloatVectorEmbeddingModel extends EmbeddingModel<BufferedImage, List<List<Float>>>, ImageEmbeddingGenerator<List<List<Float>>>, FloatVectorEmbeddingGenerator<BufferedImage> {
	
	/**
	 * Creates ImageFloatVectorEmbeddingModel requirement.
	 * String attributes match any value if null.
	 */
	static Requirement createRequirement(
			String provider,
			String model,
			String version) {
		
		return new Requirement(
				String.class, 
				List.class,
				ImageFloatVectorEmbeddingModel.class::isAssignableFrom,
				em -> {
					ImageFloatVectorEmbeddingModel tfvem = (ImageFloatVectorEmbeddingModel) em;
					if (model != null && !model.equals(tfvem.getName())) {
						return false;
					}
					if (provider != null && !provider.equals(tfvem.getProvider())) {
						return false;
					}
					return version == null || version.equals(tfvem.getVersion());
				});		
	}

}

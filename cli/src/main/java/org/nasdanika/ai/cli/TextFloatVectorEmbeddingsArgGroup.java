package org.nasdanika.ai.cli;

import org.nasdanika.ai.TextFloatVectorEmbeddingModel;
import org.nasdanika.capability.ServiceCapabilityFactory;
import org.nasdanika.capability.ServiceRequirementProvider;
import org.nasdanika.common.Util;

import io.opentelemetry.api.trace.Span;
import picocli.CommandLine.Option;

/**
 * This arguments group is used to 
 * create a requirement for embeddings.
 */
public class TextFloatVectorEmbeddingsArgGroup implements ServiceRequirementProvider<TextFloatVectorEmbeddingModel.Requirement, TextFloatVectorEmbeddingModel> {
	
	@Option( 
			names = "--embeddings-provider",
			description = "TextFloatVectorEmbeddingModel provider")
	private String embeddingsProvider;
	
	@Option( 
			names = "--embeddings-model",
			description = "TextFloatVectorEmbeddingModel model")
	private String embeddingsModel;
	
	@Option( 
			names = "--embeddings-version",
			description = "TextFloatVectorEmbeddingModel version")
	private String embeddingsVersion;
	
	/**
	 * @return An instance of requirement
	 */
	public TextFloatVectorEmbeddingModel.Requirement getEmbeddingsRequirement() {
		return TextFloatVectorEmbeddingModel.createRequirement(embeddingsProvider, embeddingsModel, embeddingsVersion);
	}
	
	@Override
	public ServiceCapabilityFactory.Requirement<TextFloatVectorEmbeddingModel.Requirement, TextFloatVectorEmbeddingModel> getServiceRequirement() {
		return ServiceCapabilityFactory.createRequirement(TextFloatVectorEmbeddingModel.class, null, getEmbeddingsRequirement());				
	}
	
	public void setSpanAttributes(Span span) {
		if (!Util.isBlank(embeddingsProvider)) {
			span.setAttribute("embedings.provider", embeddingsProvider);
		}
		if (!Util.isBlank(embeddingsModel)) {
			span.setAttribute("embedings.model", embeddingsModel);
		}
		if (!Util.isBlank(embeddingsVersion)) {
			span.setAttribute("embedings.version", embeddingsVersion);
		}		
	}

}

package org.nasdanika.ai.cli;

import org.nasdanika.ai.Embeddings;
import org.nasdanika.capability.ServiceCapabilityFactory;
import org.nasdanika.capability.ServiceRequirementProvider;

import picocli.CommandLine.Option;

/**
 * This arguments group is used to 
 * create a requirement for embeddings.
 */
public class EmbeddingsArgGroup implements ServiceRequirementProvider<Embeddings.Requirement, Embeddings> {
	
	@Option( 
			names = "--embeddings-provider",
			description = "Embeddings provider")
	private String embeddingsProvider;
	
	@Option( 
			names = "--embeddings-model",
			description = "Embeddings model")
	private String embeddingsModel;
	
	@Option( 
			names = "--embeddings-version",
			description = "Embeddings version")
	private String embeddingsVersion;
	
	/**
	 * @return An instance of requirement
	 */
	public Embeddings.Requirement getEmbeddingsRequirement() {
		return new Embeddings.Requirement(embeddingsProvider, embeddingsModel, embeddingsVersion);
	}
	
	@Override
	public ServiceCapabilityFactory.Requirement<Embeddings.Requirement, Embeddings> getServiceRequirement() {
		return ServiceCapabilityFactory.createRequirement(Embeddings.class, null, getEmbeddingsRequirement());				
	}

}

package org.nasdanika.ai.tests;

import java.util.concurrent.CompletionStage;

import org.nasdanika.ai.Embeddings;
import org.nasdanika.ai.openai.OpenAIEmbeddings;
import org.nasdanika.capability.CapabilityProvider;
import org.nasdanika.capability.ServiceCapabilityFactory;
import org.nasdanika.common.ProgressMonitor;

import com.azure.ai.openai.OpenAIClientBuilder;
import com.knuddels.jtokkit.api.EncodingType;

import io.opentelemetry.api.OpenTelemetry;

/**
 * Creates and configures {@link OpenAIClientBuilder}
 */
public class OpenAIAdaEmbeddingsCapabilityFactory extends ServiceCapabilityFactory<Void, Embeddings> {

	@Override
	public boolean isFor(Class<?> type, Object requirement) {
		return Embeddings.class == type && requirement == null;
	}

	@Override
	protected CompletionStage<Iterable<CapabilityProvider<Embeddings>>> createService(
			Class<Embeddings> serviceType,
			Void serviceRequirement, 
			Loader loader, 
			ProgressMonitor progressMonitor) {
		
		
		Requirement<Object, OpenTelemetry> openTelemetryRequirement = ServiceCapabilityFactory.createRequirement(OpenTelemetry.class);
		CompletionStage<OpenTelemetry> openTelemetryCS = loader.loadOne(openTelemetryRequirement, progressMonitor);
		
		Requirement<String, OpenAIClientBuilder> openAIClientBuilderRequirement = ServiceCapabilityFactory.createRequirement(
				OpenAIClientBuilder.class,
				null,
				"https://api.openai.com/v1/");
		
		CompletionStage<OpenAIClientBuilder> openAIClientBuilderCS = loader.loadOne(openAIClientBuilderRequirement, progressMonitor);
		
		return wrapCompletionStage(openAIClientBuilderCS.thenCombine(openTelemetryCS, this::createEmbeddings));
	}
		
	protected Embeddings createEmbeddings(OpenAIClientBuilder openAIClientBuilder, OpenTelemetry openTelemetry) {
		return new OpenAIEmbeddings(
				openAIClientBuilder.buildClient(),
				openAIClientBuilder.buildAsyncClient(),
				"OpenAI",
				"text-embedding-ada-002",
				null,
				1536,
				EncodingType.CL100K_BASE,
				8191,
				openTelemetry);
	}
	

}

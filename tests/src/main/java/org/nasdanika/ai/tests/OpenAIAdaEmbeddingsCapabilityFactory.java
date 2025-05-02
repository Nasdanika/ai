package org.nasdanika.ai.tests;

import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;

import org.nasdanika.ai.Embeddings;
import org.nasdanika.ai.openai.OpenAIEmbeddings;
import org.nasdanika.capability.CapabilityProvider;
import org.nasdanika.capability.ServiceCapabilityFactory;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.common.Util;

import com.azure.ai.openai.OpenAIClientBuilder;
import com.knuddels.jtokkit.api.EncodingType;

import io.opentelemetry.api.OpenTelemetry;

public class OpenAIAdaEmbeddingsCapabilityFactory extends ServiceCapabilityFactory<Embeddings.Requirement, Embeddings> {

	private static final String MODEL = "text-embedding-ada-002";
	private static final String PROVIDER = "OpenAI";

	@Override
	public boolean isFor(Class<?> type, Object requirement) {
		if (Embeddings.class == type) {
			if (requirement == null) {
				return true;
			}
			if (requirement instanceof Embeddings.Requirement) {			
				Embeddings.Requirement eReq = (Embeddings.Requirement) requirement;
				if (!Util.isBlank(eReq.provider()) && !PROVIDER.equals(eReq.provider())) {
					return false;
				}
				return Util.isBlank(eReq.model()) || MODEL.equals(eReq.model());
			}
		}
		return false;
	}

	@Override
	protected CompletionStage<Iterable<CapabilityProvider<Embeddings>>> createService(
			Class<Embeddings> serviceType,
			Embeddings.Requirement serviceRequirement, 
			Loader loader, 
			ProgressMonitor progressMonitor) {
				
		Requirement<Object, OpenTelemetry> openTelemetryRequirement = ServiceCapabilityFactory.createRequirement(OpenTelemetry.class);
		CompletionStage<OpenTelemetry> openTelemetryCS = loader.loadOne(openTelemetryRequirement, progressMonitor);
		
		Requirement<String, OpenAIClientBuilder> openAIClientBuilderRequirement = ServiceCapabilityFactory.createRequirement(
				OpenAIClientBuilder.class,
				null,
				"https://api.openai.com/v1/");
		
		CompletionStage<OpenAIClientBuilder> openAIClientBuilderCS = loader.loadOne(openAIClientBuilderRequirement, progressMonitor);
		
		BiFunction<OpenAIClientBuilder, OpenTelemetry, Embeddings> combiner = (openAIClientBuilder, openTelemetry) -> createEmbeddings(openAIClientBuilder, openTelemetry);
		return wrapCompletionStage(openAIClientBuilderCS.thenCombine(openTelemetryCS, combiner));
	}
		
	protected Embeddings createEmbeddings(
			OpenAIClientBuilder openAIClientBuilder, 
			OpenTelemetry openTelemetry) {
		return new OpenAIEmbeddings(
				openAIClientBuilder.buildClient(),
				openAIClientBuilder.buildAsyncClient(),
				PROVIDER,
				MODEL,
				null,
				1536,
				EncodingType.CL100K_BASE,
				8191,
				50,
				openTelemetry);
	}
	
}

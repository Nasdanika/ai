package org.nasdanika.ai.tests;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;

import org.nasdanika.ai.EmbeddingGenerator;
import org.nasdanika.ai.TextFloatVectorEmbeddingModel;
import org.nasdanika.ai.openai.TextFloatVectorOpenAIEmbeddings;
import org.nasdanika.capability.CapabilityProvider;
import org.nasdanika.capability.ServiceCapabilityFactory;
import org.nasdanika.common.ProgressMonitor;

import com.azure.ai.openai.OpenAIClientBuilder;
import com.knuddels.jtokkit.api.EncodingType;

import io.opentelemetry.api.OpenTelemetry;
import reactor.core.publisher.Mono;

public class OpenAIAdaEmbeddingsCapabilityFactory extends ServiceCapabilityFactory<EmbeddingGenerator.Requirement, TextFloatVectorEmbeddingModel> {

	private static final int DIMENSIONS = 1536;
	private static final int MAX_INPUT_TOKENS = 8191;
	private static final String MODEL = "text-embedding-ada-002";
	private static final String PROVIDER = "OpenAI";

	/**
	 * Prototype instance for testing provider and model.
	 */
	private static final TextFloatVectorEmbeddingModel PROTOTYPE = new TextFloatVectorEmbeddingModel() {

		@Override
		public Mono<List<List<Float>>> generateAsync(String source) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getProvider() {
			return PROVIDER;
		}

		@Override
		public String getName() {
			return MODEL;
		}

		@Override
		public String getVersion() {
			return null;
		}

		@Override
		public int getMaxInputTokens() {
			return MAX_INPUT_TOKENS;
		}

		@Override
		public boolean isTooLong(String input) {
			throw new UnsupportedOperationException();
		}
		
		@Override
		public int getDimensions() {
			return DIMENSIONS;
		};
		
	};

	@Override
	public boolean isFor(Class<?> type, Object requirement) {
		if (TextFloatVectorEmbeddingModel.class == type) {
			if (requirement == null) {
				return true;
			}
			if (requirement instanceof EmbeddingGenerator.Requirement) {			
				EmbeddingGenerator.Requirement eReq = (EmbeddingGenerator.Requirement) requirement;
				if (eReq.typePredicate() == null  || eReq.typePredicate().test(TextFloatVectorOpenAIEmbeddings.class)) {
					return eReq.predicate() == null || eReq.predicate().test(PROTOTYPE);
				}
			}
		}
		return false;
	}

	@Override
	protected CompletionStage<Iterable<CapabilityProvider<TextFloatVectorEmbeddingModel>>> createService(
			Class<TextFloatVectorEmbeddingModel> serviceType,
			EmbeddingGenerator.Requirement serviceRequirement, 
			Loader loader, 
			ProgressMonitor progressMonitor) {
				
		Requirement<Object, OpenTelemetry> openTelemetryRequirement = ServiceCapabilityFactory.createRequirement(OpenTelemetry.class);
		CompletionStage<OpenTelemetry> openTelemetryCS = loader.loadOne(openTelemetryRequirement, progressMonitor);
		
		Requirement<String, OpenAIClientBuilder> openAIClientBuilderRequirement = ServiceCapabilityFactory.createRequirement(
				OpenAIClientBuilder.class,
				null,
				"https://api.openai.com/v1/");
		
		CompletionStage<OpenAIClientBuilder> openAIClientBuilderCS = loader.loadOne(openAIClientBuilderRequirement, progressMonitor);
		
		BiFunction<OpenAIClientBuilder, OpenTelemetry, TextFloatVectorEmbeddingModel> combiner = (openAIClientBuilder, openTelemetry) -> createEmbeddings(openAIClientBuilder, openTelemetry);
		return wrapCompletionStage(openAIClientBuilderCS.thenCombine(openTelemetryCS, combiner));
	}
		
	protected TextFloatVectorEmbeddingModel createEmbeddings(
			OpenAIClientBuilder openAIClientBuilder, 
			OpenTelemetry openTelemetry) {
		return new TextFloatVectorOpenAIEmbeddings(
				openAIClientBuilder.buildClient(),
				openAIClientBuilder.buildAsyncClient(),
				PROVIDER,
				MODEL,
				null,
				DIMENSIONS,
				EncodingType.CL100K_BASE,
				MAX_INPUT_TOKENS,
				50,
				openTelemetry);
	}
	
}

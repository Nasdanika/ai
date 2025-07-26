package org.nasdanika.ai.tests;

import java.util.List;
import java.util.concurrent.CompletionStage;

import org.nasdanika.ai.EmbeddingGenerator;
import org.nasdanika.ai.TextFloatVectorEmbeddingModel;
import org.nasdanika.ai.ollama.TextFloatVectorOllamaEmbeddings;
import org.nasdanika.capability.CapabilityProvider;
import org.nasdanika.capability.ServiceCapabilityFactory;
import org.nasdanika.common.ProgressMonitor;

import io.opentelemetry.api.OpenTelemetry;
import reactor.core.publisher.Mono;

public class SnowflakeArcticEmbedOllamatCapabilityFactory extends ServiceCapabilityFactory<EmbeddingGenerator.Requirement, TextFloatVectorEmbeddingModel> {

	private static final String MODEL = "snowflake-arctic-embed";
	private static final String PROVIDER = "Ollama";
	private static final int DIMENSIONS = 1024;
	private static final int MAX_INPUT_TOKENS = 8191;


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
				if (eReq.typePredicate() == null  || eReq.typePredicate().test(TextFloatVectorOllamaEmbeddings.class)) {
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
		
		return wrapCompletionStage(openTelemetryCS.thenApply(openTelemetry -> createEmbeddings(openTelemetry)));
	}
			
	protected TextFloatVectorEmbeddingModel createEmbeddings(OpenTelemetry openTelemetry) {
		return new TextFloatVectorOllamaEmbeddings(
				"http://localhost:11434/api/", 
				PROVIDER, 
				MODEL, 
				null, 
				DIMENSIONS,
				null, // unknown
				MAX_INPUT_TOKENS, 
				openTelemetry);
	}

}

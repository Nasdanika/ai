package org.nasdanika.ai.tests;

import java.util.concurrent.CompletionStage;

import org.nasdanika.ai.Embeddings;
import org.nasdanika.ai.ollama.OllamaEmbeddings;
import org.nasdanika.capability.CapabilityProvider;
import org.nasdanika.capability.ServiceCapabilityFactory;
import org.nasdanika.common.ProgressMonitor;

import io.opentelemetry.api.OpenTelemetry;

public class SnowflakeArcticEmbedOllamatCapabilityFactory extends ServiceCapabilityFactory<Void, Embeddings> {

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
		
		return wrapCompletionStage(openTelemetryCS.thenApply(this::createEmbeddings));
	}
		
	protected Embeddings createEmbeddings(OpenTelemetry openTelemetry) {
		return new OllamaEmbeddings(
				"http://localhost:11434/api/", 
				"Ollama", 
				"snowflake-arctic-embed", 
				null, 
				1024,
				null, // unknown
				8192, 
				openTelemetry);
	}	

}

package org.nasdanika.ai.tests;

import java.util.concurrent.CompletionStage;

import org.nasdanika.ai.Embeddings;
import org.nasdanika.ai.ollama.OllamaEmbeddings;
import org.nasdanika.capability.CapabilityProvider;
import org.nasdanika.capability.ServiceCapabilityFactory;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.common.Util;

import io.opentelemetry.api.OpenTelemetry;

public class SnowflakeArcticEmbedOllamatCapabilityFactory extends ServiceCapabilityFactory<Embeddings.Requirement, Embeddings> {

	private static final String MODEL = "snowflake-arctic-embed";
	private static final String PROVIDER = "Ollama";

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
		
		return wrapCompletionStage(openTelemetryCS.thenApply(openTelemetry -> createEmbeddings(openTelemetry)));
	}
		
	protected Embeddings createEmbeddings(
			OpenTelemetry openTelemetry) {
		return new OllamaEmbeddings(
				"http://localhost:11434/api/", 
				PROVIDER, 
				MODEL, 
				null, 
				1024,
				null, // unknown
				8192, 
				openTelemetry);
	}	

}

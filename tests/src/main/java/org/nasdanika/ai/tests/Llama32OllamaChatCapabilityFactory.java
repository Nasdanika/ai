package org.nasdanika.ai.tests;

import java.util.concurrent.CompletionStage;

import org.nasdanika.ai.Chat;
import org.nasdanika.ai.ollama.OllamaChat;
import org.nasdanika.capability.CapabilityProvider;
import org.nasdanika.capability.ServiceCapabilityFactory;
import org.nasdanika.common.ProgressMonitor;

import io.opentelemetry.api.OpenTelemetry;

public class Llama32OllamaChatCapabilityFactory extends ServiceCapabilityFactory<Void, Chat> {

	@Override
	public boolean isFor(Class<?> type, Object requirement) {
		return Chat.class == type && requirement == null;
	}

	@Override
	protected CompletionStage<Iterable<CapabilityProvider<Chat>>> createService(
			Class<Chat> serviceType,
			Void serviceRequirement, 
			Loader loader, 
			ProgressMonitor progressMonitor) {
				
		Requirement<Object, OpenTelemetry> openTelemetryRequirement = ServiceCapabilityFactory.createRequirement(OpenTelemetry.class);
		CompletionStage<OpenTelemetry> openTelemetryCS = loader.loadOne(openTelemetryRequirement, progressMonitor);
		
		return wrapCompletionStage(openTelemetryCS.thenApply(this::createChat));
	}
		
	protected Chat createChat(OpenTelemetry openTelemetry) {
		return new OllamaChat(
				"http://localhost:11434/api/", 
				"Ollama", 
				"llama3.2", 
				null, 
				128000, 
				2048, 
				openTelemetry);
	}	

}

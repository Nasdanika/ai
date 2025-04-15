package org.nasdanika.ai.tests;

import java.util.concurrent.CompletionStage;

import org.nasdanika.ai.Chat;
import org.nasdanika.ai.ollama.OllamaChat;
import org.nasdanika.capability.CapabilityProvider;
import org.nasdanika.capability.ServiceCapabilityFactory;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.common.Util;

import io.opentelemetry.api.OpenTelemetry;

public class Llama32OllamaChatCapabilityFactory extends ServiceCapabilityFactory<Chat.Requirement, Chat> {

	private static final String MODEL = "llama3.2";
	private static final String PROVIDER = "Ollama";

	@Override
	public boolean isFor(Class<?> type, Object requirement) {
		if (Chat.class == type) {
			if (requirement == null) {
				return true;
			}
			if (requirement instanceof Chat.Requirement) {			
				Chat.Requirement cReq = (Chat.Requirement) requirement;
				if (!Util.isBlank(cReq.provider()) && !PROVIDER.equals(cReq.provider())) {
					return false;
				}
				return Util.isBlank(cReq.model()) || MODEL.equals(cReq.model());
			}
		}
		return false;
	}

	@Override
	protected CompletionStage<Iterable<CapabilityProvider<Chat>>> createService(
			Class<Chat> serviceType,
			Chat.Requirement serviceRequirement, 
			Loader loader, 
			ProgressMonitor progressMonitor) {
				
		Requirement<Object, OpenTelemetry> openTelemetryRequirement = ServiceCapabilityFactory.createRequirement(OpenTelemetry.class);
		CompletionStage<OpenTelemetry> openTelemetryCS = loader.loadOne(openTelemetryRequirement, progressMonitor);
		
		return wrapCompletionStage(openTelemetryCS.thenApply(this::createChat));
	}
		
	protected Chat createChat(OpenTelemetry openTelemetry) {
		return new OllamaChat(
				"http://localhost:11434/api/", 
				PROVIDER, 
				MODEL, 
				null, 
				128000, 
				2048, 
				openTelemetry);
	}	

}

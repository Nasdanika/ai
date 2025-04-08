package org.nasdanika.ai.tests;

import java.util.concurrent.CompletionStage;

import org.nasdanika.ai.Chat;
import org.nasdanika.ai.openai.OpenAIChat;
import org.nasdanika.capability.CapabilityProvider;
import org.nasdanika.capability.ServiceCapabilityFactory;
import org.nasdanika.common.ProgressMonitor;

import com.azure.ai.openai.OpenAIClientBuilder;

import io.opentelemetry.api.OpenTelemetry;

public class OpenAIGpt35TurboChatCapabilityFactory extends ServiceCapabilityFactory<Void, Chat> {

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
		
		Requirement<String, OpenAIClientBuilder> openAIClientBuilderRequirement = ServiceCapabilityFactory.createRequirement(
				OpenAIClientBuilder.class,
				null,
				"https://api.openai.com/v1/");
		
		CompletionStage<OpenAIClientBuilder> openAIClientBuilderCS = loader.loadOne(openAIClientBuilderRequirement, progressMonitor);
		
		return wrapCompletionStage(openAIClientBuilderCS.thenCombine(openTelemetryCS, this::createChat));
	}
		
	protected Chat createChat(OpenAIClientBuilder openAIClientBuilder, OpenTelemetry openTelemetry) {
		return new OpenAIChat(
			openAIClientBuilder.buildClient(),
			openAIClientBuilder.buildAsyncClient(),
			"OpenAI",
			"gpt-3.5-turbo",
			null,
			16385,
			4096,
			openTelemetry);
	}	

}

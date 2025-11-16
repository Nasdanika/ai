package org.nasdanika.ai.tests;

import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;

import org.nasdanika.ai.Chat;
import org.nasdanika.ai.openai.OpenAIChat;
import org.nasdanika.capability.CapabilityProvider;
import org.nasdanika.capability.ServiceCapabilityFactory;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.common.Util;

import com.azure.ai.openai.OpenAIClientBuilder;

import io.opentelemetry.api.OpenTelemetry;

public class OpenAIGpt4oChatCapabilityFactoryEx extends ServiceCapabilityFactory<OpenAIChat.Requirement, Chat> {

	private static final String MODEL = "gpt-4o";
	private static final String PROVIDER = "OpenAI";

	@Override
	public boolean isFor(Class<?> type, Object requirement) {
		if (Chat.class == type) {
			if (requirement == null) {
				return true;
			}
			if (requirement instanceof OpenAIChat.Requirement) {			
				OpenAIChat.Requirement cReq = (OpenAIChat.Requirement) requirement;
				return Util.isBlank(cReq.model()) || MODEL.equals(cReq.model());
			}
		}
		return false;
	}

	@Override
	protected CompletionStage<Iterable<CapabilityProvider<Chat>>> createService(
			Class<Chat> serviceType,
			OpenAIChat.Requirement serviceRequirement, 
			Loader loader, 
			ProgressMonitor progressMonitor) {
		
		Requirement<Object, OpenTelemetry> openTelemetryRequirement = ServiceCapabilityFactory.createRequirement(OpenTelemetry.class);
		CompletionStage<OpenTelemetry> openTelemetryCS = loader.loadOne(openTelemetryRequirement, progressMonitor);
		
		Requirement<String, OpenAIClientBuilder> openAIClientBuilderRequirement = ServiceCapabilityFactory.createRequirement(
				OpenAIClientBuilder.class,
				null,
				"https://api.openai.com/v1/");
		
		CompletionStage<OpenAIClientBuilder> openAIClientBuilderCS = loader.loadOne(openAIClientBuilderRequirement, progressMonitor);
		
		return wrapCompletionStage(openAIClientBuilderCS.thenCombine(openTelemetryCS, (c,t) -> createChat(c, t, serviceRequirement.usageConsumer())));
	}
		
	protected Chat createChat(
			OpenAIClientBuilder openAIClientBuilder, 
			OpenTelemetry openTelemetry,
			BiConsumer<Integer,Integer> usageConsumer) {
		return new OpenAIChat(
			openAIClientBuilder.buildClient(),
			openAIClientBuilder.buildAsyncClient(),
			PROVIDER,
			MODEL,
			null,
			16385,
			4096,
			openTelemetry,
			usageConsumer);
	}	

}

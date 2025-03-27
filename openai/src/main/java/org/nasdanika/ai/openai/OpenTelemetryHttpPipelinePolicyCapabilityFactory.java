package org.nasdanika.ai.openai;

import java.util.concurrent.CompletionStage;

import org.nasdanika.capability.CapabilityProvider;
import org.nasdanika.capability.ServiceCapabilityFactory;
import org.nasdanika.common.ProgressMonitor;

import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.core.http.policy.HttpPipelinePolicy;

import io.opentelemetry.api.OpenTelemetry;

/**
 * Creates and configures {@link OpenAIClientBuilder}
 */
public class OpenTelemetryHttpPipelinePolicyCapabilityFactory extends ServiceCapabilityFactory<String, HttpPipelinePolicy> {

	@Override
	public boolean isFor(Class<?> type, Object requirement) {
		return HttpPipelinePolicy.class == type && (requirement == null || requirement instanceof String);
	}

	@Override
	protected CompletionStage<Iterable<CapabilityProvider<HttpPipelinePolicy>>> createService(
			Class<HttpPipelinePolicy> serviceType, 
			String endpoint, 
			Loader loader,
			ProgressMonitor progressMonitor) {
		
		Requirement<Object, OpenTelemetry> requirement = ServiceCapabilityFactory.createRequirement(OpenTelemetry.class);
		CompletionStage<OpenTelemetry> openTelemetryCS = loader.loadOne(requirement, progressMonitor);		
		return wrapCompletionStage(openTelemetryCS.thenApply(ot -> createOpenTelemetryHttpPipelinePolicy(ot, endpoint)));
	}
	
	protected OpenTelemetryHttpPipelinePolicy createOpenTelemetryHttpPipelinePolicy(OpenTelemetry openTelemetry, String endpoint) {
		return new OpenTelemetryHttpPipelinePolicy(openTelemetry, endpoint);
	}

}

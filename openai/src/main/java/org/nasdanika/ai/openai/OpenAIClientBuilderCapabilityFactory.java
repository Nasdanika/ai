package org.nasdanika.ai.openai;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

import org.nasdanika.capability.CapabilityProvider;
import org.nasdanika.capability.ServiceCapabilityFactory;
import org.nasdanika.common.ProgressMonitor;

import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.ai.openai.OpenAIServiceVersion;
import com.azure.core.credential.KeyCredential;
import com.azure.core.credential.TokenCredential;
import com.azure.core.http.HttpClient;
import com.azure.core.http.HttpPipeline;
import com.azure.core.http.policy.HttpLogOptions;
import com.azure.core.http.policy.HttpPipelinePolicy;
import com.azure.core.http.policy.RetryOptions;
import com.azure.core.http.policy.RetryPolicy;
import com.azure.core.util.ClientOptions;
import com.azure.core.util.Configuration;

/**
 * Creates and configures {@link OpenAIClientBuilder}
 */
public class OpenAIClientBuilderCapabilityFactory extends ServiceCapabilityFactory<Object, OpenAIClientBuilder> {

	@Override
	public boolean isFor(Class<?> type, Object requirement) {
		return OpenAIClientBuilder.class == type && (requirement == null || requirement instanceof String);
	}

	@Override
	protected CompletionStage<Iterable<CapabilityProvider<OpenAIClientBuilder>>> createService(
			Class<OpenAIClientBuilder> serviceType, 
			Object serviceRequirement, 
			Loader loader,
			ProgressMonitor progressMonitor) {
		
		String endpoint = (String) serviceRequirement; 
		CompletionStage<OpenAIClientBuilder> builderCS = CompletableFuture.completedStage(createBuilder(serviceRequirement)); 
		
		builderCS = addHttpPipelinePolicies(builderCS, loader, endpoint, progressMonitor);
		builderCS = setClientOptions(builderCS, loader, endpoint, progressMonitor);
		builderCS = setConfiguration(builderCS, loader, endpoint, progressMonitor);
		builderCS = setKeyCredential(builderCS, loader, endpoint, progressMonitor);
		builderCS = setTokenCredential(builderCS, loader, endpoint, progressMonitor);
		builderCS = setHttpClient(builderCS, loader, endpoint, progressMonitor);
		builderCS = setHttpLogOptions(builderCS, loader, endpoint, progressMonitor);
		builderCS = setHttpPipeline(builderCS, loader, endpoint, progressMonitor);
		builderCS = setRetryOptions(builderCS, loader, endpoint, progressMonitor);
		builderCS = setRetryPolicy(builderCS, loader, endpoint, progressMonitor);
		builderCS = setServiceVersion(builderCS, loader, endpoint, progressMonitor);
		return wrapCompletionStage(builderCS);
	}
	
	protected CompletionStage<OpenAIClientBuilder> setHttpClient(
			CompletionStage<OpenAIClientBuilder> builderCS,
			Loader loader, 
			String endpoint, 
			ProgressMonitor progressMonitor) {

		Requirement<String, HttpClient> requirement = ServiceCapabilityFactory.createRequirement(
				HttpClient.class, 
				null, 
				endpoint);
		
		CompletionStage<Iterable<CapabilityProvider<HttpClient>>> cs = loader.load(requirement, progressMonitor);
		return cs.thenCombine(builderCS, (capabilityProviders, builder) -> {
			for (CapabilityProvider<HttpClient> capabilityProvider: capabilityProviders) {
				capabilityProvider.getPublisher().filter(Objects::nonNull).collectList().block().forEach(builder::httpClient);
			}
				
			return builder;
		});
	}	
	
	protected CompletionStage<OpenAIClientBuilder> setHttpLogOptions(
			CompletionStage<OpenAIClientBuilder> builderCS,
			Loader loader, 
			String endpoint, 
			ProgressMonitor progressMonitor) {

		Requirement<String, HttpLogOptions> requirement = ServiceCapabilityFactory.createRequirement(
				HttpLogOptions.class, 
				null, 
				endpoint);
		
		CompletionStage<Iterable<CapabilityProvider<HttpLogOptions>>> cs = loader.load(requirement, progressMonitor);
		return cs.thenCombine(builderCS, (capabilityProviders, builder) -> {
			for (CapabilityProvider<HttpLogOptions> capabilityProvider: capabilityProviders) {
				capabilityProvider.getPublisher().filter(Objects::nonNull).collectList().block().forEach(builder::httpLogOptions);
			}
				
			return builder;
		});
	}	
	
	protected CompletionStage<OpenAIClientBuilder> setHttpPipeline(
			CompletionStage<OpenAIClientBuilder> builderCS,
			Loader loader, 
			String endpoint, 
			ProgressMonitor progressMonitor) {

		Requirement<String, HttpPipeline> requirement = ServiceCapabilityFactory.createRequirement(
				HttpPipeline.class, 
				null, 
				endpoint);
		
		CompletionStage<Iterable<CapabilityProvider<HttpPipeline>>> cs = loader.load(requirement, progressMonitor);
		return cs.thenCombine(builderCS, (capabilityProviders, builder) -> {
			for (CapabilityProvider<HttpPipeline> capabilityProvider: capabilityProviders) {
				capabilityProvider.getPublisher().filter(Objects::nonNull).collectList().block().forEach(builder::pipeline);
			}
				
			return builder;
		});
	}	
	
	protected CompletionStage<OpenAIClientBuilder> setRetryOptions(
			CompletionStage<OpenAIClientBuilder> builderCS,
			Loader loader, 
			String endpoint, 
			ProgressMonitor progressMonitor) {

		Requirement<String, RetryOptions> requirement = ServiceCapabilityFactory.createRequirement(
				RetryOptions.class, 
				null, 
				endpoint);
		
		CompletionStage<Iterable<CapabilityProvider<RetryOptions>>> cs = loader.load(requirement, progressMonitor);
		return cs.thenCombine(builderCS, (capabilityProviders, builder) -> {
			for (CapabilityProvider<RetryOptions> capabilityProvider: capabilityProviders) {
				capabilityProvider.getPublisher().filter(Objects::nonNull).collectList().block().forEach(builder::retryOptions);
			}
				
			return builder;
		});
	}	
	
	protected CompletionStage<OpenAIClientBuilder> setRetryPolicy(
			CompletionStage<OpenAIClientBuilder> builderCS,
			Loader loader, 
			String endpoint, 
			ProgressMonitor progressMonitor) {

		Requirement<String, RetryPolicy> requirement = ServiceCapabilityFactory.createRequirement(
				RetryPolicy.class, 
				null, 
				endpoint);
		
		CompletionStage<Iterable<CapabilityProvider<RetryPolicy>>> cs = loader.load(requirement, progressMonitor);
		return cs.thenCombine(builderCS, (capabilityProviders, builder) -> {
			for (CapabilityProvider<RetryPolicy> capabilityProvider: capabilityProviders) {
				capabilityProvider.getPublisher().filter(Objects::nonNull).collectList().block().forEach(builder::retryPolicy);
			}
				
			return builder;
		});
	}	
	
	protected CompletionStage<OpenAIClientBuilder> setServiceVersion(
			CompletionStage<OpenAIClientBuilder> builderCS,
			Loader loader, 
			String endpoint, 
			ProgressMonitor progressMonitor) {

		Requirement<String, OpenAIServiceVersion> requirement = ServiceCapabilityFactory.createRequirement(
				OpenAIServiceVersion.class, 
				null, 
				endpoint);
		
		CompletionStage<Iterable<CapabilityProvider<OpenAIServiceVersion>>> cs = loader.load(requirement, progressMonitor);
		return cs.thenCombine(builderCS, (capabilityProviders, builder) -> {
			for (CapabilityProvider<OpenAIServiceVersion> capabilityProvider: capabilityProviders) {
				capabilityProvider.getPublisher().filter(Objects::nonNull).collectList().block().forEach(builder::serviceVersion);
			}
				
			return builder;
		});
	}	
	
	protected CompletionStage<OpenAIClientBuilder> addHttpPipelinePolicies(
			CompletionStage<OpenAIClientBuilder> builderCS,
			Loader loader, 
			String endpoint, 
			ProgressMonitor progressMonitor) {

		Requirement<String, HttpPipelinePolicy> requirement = ServiceCapabilityFactory.createRequirement(
				HttpPipelinePolicy.class, 
				null, 
				endpoint);
		
		CompletionStage<Iterable<CapabilityProvider<HttpPipelinePolicy>>> cs = loader.load(requirement, progressMonitor);
		return cs.thenCombine(builderCS, (capabilityProviders, builder) -> {
			for (CapabilityProvider<HttpPipelinePolicy> capabilityProvider: capabilityProviders) {
				capabilityProvider.getPublisher().filter(Objects::nonNull).collectList().block().forEach(builder::addPolicy);
			}
				
			return builder;
		});
	}	
	
	protected CompletionStage<OpenAIClientBuilder> setClientOptions(
			CompletionStage<OpenAIClientBuilder> builderCS,
			Loader loader, 
			String endpoint, 
			ProgressMonitor progressMonitor) {

		Requirement<String, ClientOptions> requirement = ServiceCapabilityFactory.createRequirement(
				ClientOptions.class, 
				null, 
				endpoint);
		
		CompletionStage<Iterable<CapabilityProvider<ClientOptions>>> cs = loader.load(requirement, progressMonitor);
		return cs.thenCombine(builderCS, (capabilityProviders, builder) -> {
			for (CapabilityProvider<ClientOptions> capabilityProvider: capabilityProviders) {
				capabilityProvider.getPublisher().filter(Objects::nonNull).collectList().block().forEach(builder::clientOptions);
			}
				
			return builder;
		});
	}	
	
	protected CompletionStage<OpenAIClientBuilder> setConfiguration(
			CompletionStage<OpenAIClientBuilder> builderCS,
			Loader loader, 
			String endpoint, 
			ProgressMonitor progressMonitor) {

		Requirement<String, Configuration> requirement = ServiceCapabilityFactory.createRequirement(
				Configuration.class, 
				null, 
				endpoint);
		
		CompletionStage<Iterable<CapabilityProvider<Configuration>>> cs = loader.load(requirement, progressMonitor);
		return cs.thenCombine(builderCS, (capabilityProviders, builder) -> {
			for (CapabilityProvider<Configuration> capabilityProvider: capabilityProviders) {
				capabilityProvider.getPublisher().filter(Objects::nonNull).collectList().block().forEach(builder::configuration);
			}
				
			return builder;
		});
	}	

	protected CompletionStage<OpenAIClientBuilder> setKeyCredential(
			CompletionStage<OpenAIClientBuilder> builderCS,
			Loader loader, 
			String endpoint, 
			ProgressMonitor progressMonitor) {

		Requirement<String, KeyCredential> requirement = ServiceCapabilityFactory.createRequirement(
				KeyCredential.class, 
				null, 
				endpoint);
		
		CompletionStage<Iterable<CapabilityProvider<KeyCredential>>> cs = loader.load(requirement, progressMonitor);
		return cs.thenCombine(builderCS, (capabilityProviders, builder) -> {
			for (CapabilityProvider<KeyCredential> capabilityProvider: capabilityProviders) {
				capabilityProvider.getPublisher().filter(Objects::nonNull).collectList().block().forEach(builder::credential);
			}
				
			return builder;
		});
	}

	protected CompletionStage<OpenAIClientBuilder> setTokenCredential(
			CompletionStage<OpenAIClientBuilder> builderCS,
			Loader loader, 
			String endpoint, 
			ProgressMonitor progressMonitor) {

		Requirement<String, TokenCredential> requirement = ServiceCapabilityFactory.createRequirement(
				TokenCredential.class, 
				null, 
				endpoint);
		
		CompletionStage<Iterable<CapabilityProvider<TokenCredential>>> cs = loader.load(requirement, progressMonitor);
		return cs.thenCombine(builderCS, (capabilityProviders, builder) -> {
			for (CapabilityProvider<TokenCredential> capabilityProvider: capabilityProviders) {
				capabilityProvider.getPublisher().filter(Objects::nonNull).collectList().block().forEach(builder::credential);
			}
				
			return builder;
		});
	}
		
	@SuppressWarnings("unchecked")
	protected OpenAIClientBuilder createBuilder(Object requirement) {
		OpenAIClientBuilder builder = new OpenAIClientBuilder();
		if (requirement instanceof Consumer) {
			((Consumer<OpenAIClientBuilder>) requirement).accept(builder);
		} else if (requirement instanceof String) {
			builder.endpoint((String) requirement);
		}		
		return builder;
	}
	
}

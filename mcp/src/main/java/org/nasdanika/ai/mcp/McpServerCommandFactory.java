package org.nasdanika.ai.mcp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import org.nasdanika.capability.CapabilityProvider;
import org.nasdanika.capability.ServiceCapabilityFactory;
import org.nasdanika.cli.SubCommandCapabilityFactory;
import org.nasdanika.common.ProgressMonitor;

import io.modelcontextprotocol.server.McpServerFeatures.AsyncPromptSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.AsyncResourceSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.AsyncToolSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.SyncPromptSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.SyncResourceSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.opentelemetry.api.OpenTelemetry;
import picocli.CommandLine;

public class McpServerCommandFactory extends SubCommandCapabilityFactory<McpServerCommand> {

	@Override
	protected Class<McpServerCommand> getCommandType() {
		return McpServerCommand.class;
	}
	
	private record ServerConfig(OpenTelemetry openTelemetry,			
			Collection<SyncPromptSpecification> syncPromptSpecifications,
			Collection<AsyncPromptSpecification> asyncPromptSpecifications,			
			Collection<SyncResourceSpecification> syncResourceSpecifications,
			Collection<AsyncResourceSpecification> asyncResourceSpecifications,
			Collection<SyncToolSpecification> syncToolSpecifications,
			Collection<AsyncToolSpecification> asyncToolSpecifications) {
		
		public ServerConfig(OpenTelemetry openTelemetry) {
			this(
				openTelemetry,
				Collections.synchronizedList(new ArrayList<>()),
				Collections.synchronizedList(new ArrayList<>()),
				Collections.synchronizedList(new ArrayList<>()),
				Collections.synchronizedList(new ArrayList<>()),
				Collections.synchronizedList(new ArrayList<>()),
				Collections.synchronizedList(new ArrayList<>()));
		}
	}	
	
	@Override
	protected CompletionStage<McpServerCommand> doCreateCommand(
			List<CommandLine> parentPath,
			Loader loader,
			ProgressMonitor progressMonitor) {
		
		Requirement<Object, OpenTelemetry> openTelemetryRequirement = ServiceCapabilityFactory.createRequirement(OpenTelemetry.class);
		CompletionStage<OpenTelemetry> openTelemetryCS = loader.loadOne(openTelemetryRequirement, progressMonitor);
		
		return openTelemetryCS
				.thenApply(ServerConfig::new)
				.thenCompose(config -> syncPromptSpecifications(config, loader, progressMonitor))
				.thenCompose(config -> asyncPromptSpecifications(config, loader, progressMonitor))
				.thenCompose(config -> syncResourceSpecifications(config, loader, progressMonitor))
				.thenCompose(config -> asyncResourceSpecifications(config, loader, progressMonitor))
				.thenCompose(config -> syncToolSpecifications(config, loader, progressMonitor))
				.thenCompose(config -> asyncToolSpecifications(config, loader, progressMonitor))
				.thenApply(this::createCommand);
	}
	
	protected CompletionStage<ServerConfig> syncPromptSpecifications(
			ServerConfig config,
			Loader loader, 
			ProgressMonitor progressMonitor) {

		Requirement<Void, SyncPromptSpecification> requirement = ServiceCapabilityFactory.createRequirement(SyncPromptSpecification.class);
		CompletionStage<Iterable<CapabilityProvider<SyncPromptSpecification>>> cs = loader.load(requirement, progressMonitor);
		return cs.thenApply(capabilityProviders -> {
			for (CapabilityProvider<SyncPromptSpecification> capabilityProvider: capabilityProviders) {
				config.syncPromptSpecifications().addAll(capabilityProvider.getPublisher().collect(Collectors.toList()).block());
			}				
			return config;
		});
	}		
	
	protected CompletionStage<ServerConfig> asyncPromptSpecifications(
			ServerConfig config,
			Loader loader, 
			ProgressMonitor progressMonitor) {

		Requirement<Void, AsyncPromptSpecification> requirement = ServiceCapabilityFactory.createRequirement(AsyncPromptSpecification.class);
		CompletionStage<Iterable<CapabilityProvider<AsyncPromptSpecification>>> cs = loader.load(requirement, progressMonitor);
		return cs.thenApply(capabilityProviders -> {
			for (CapabilityProvider<AsyncPromptSpecification> capabilityProvider: capabilityProviders) {
				config.asyncPromptSpecifications().addAll(capabilityProvider.getPublisher().collect(Collectors.toList()).block());
			}				
			return config;
		});
	}		
	
	protected CompletionStage<ServerConfig> syncResourceSpecifications(
			ServerConfig config,
			Loader loader, 
			ProgressMonitor progressMonitor) {

		Requirement<Void, SyncResourceSpecification> requirement = ServiceCapabilityFactory.createRequirement(SyncResourceSpecification.class);
		CompletionStage<Iterable<CapabilityProvider<SyncResourceSpecification>>> cs = loader.load(requirement, progressMonitor);
		return cs.thenApply(capabilityProviders -> {
			for (CapabilityProvider<SyncResourceSpecification> capabilityProvider: capabilityProviders) {
				config.syncResourceSpecifications().addAll(capabilityProvider.getPublisher().collect(Collectors.toList()).block());
			}				
			return config;
		});
	}		
	
	protected CompletionStage<ServerConfig> asyncResourceSpecifications(
			ServerConfig config,
			Loader loader, 
			ProgressMonitor progressMonitor) {

		Requirement<Void, AsyncResourceSpecification> requirement = ServiceCapabilityFactory.createRequirement(AsyncResourceSpecification.class);
		CompletionStage<Iterable<CapabilityProvider<AsyncResourceSpecification>>> cs = loader.load(requirement, progressMonitor);
		return cs.thenApply(capabilityProviders -> {
			for (CapabilityProvider<AsyncResourceSpecification> capabilityProvider: capabilityProviders) {
				config.asyncResourceSpecifications().addAll(capabilityProvider.getPublisher().collect(Collectors.toList()).block());
			}				
			return config;
		});
	}		
	
	protected CompletionStage<ServerConfig> syncToolSpecifications(
			ServerConfig config,
			Loader loader, 
			ProgressMonitor progressMonitor) {

		Requirement<Void, SyncToolSpecification> requirement = ServiceCapabilityFactory.createRequirement(SyncToolSpecification.class);
		CompletionStage<Iterable<CapabilityProvider<SyncToolSpecification>>> cs = loader.load(requirement, progressMonitor);
		return cs.thenApply(capabilityProviders -> {
			for (CapabilityProvider<SyncToolSpecification> capabilityProvider: capabilityProviders) {
				config.syncToolSpecifications().addAll(capabilityProvider.getPublisher().collect(Collectors.toList()).block());
			}				
			return config;
		});
	}		
	
	protected CompletionStage<ServerConfig> asyncToolSpecifications(
			ServerConfig config,
			Loader loader, 
			ProgressMonitor progressMonitor) {

		Requirement<Void, AsyncToolSpecification> requirement = ServiceCapabilityFactory.createRequirement(AsyncToolSpecification.class);
		CompletionStage<Iterable<CapabilityProvider<AsyncToolSpecification>>> cs = loader.load(requirement, progressMonitor);
		return cs.thenApply(capabilityProviders -> {
			for (CapabilityProvider<AsyncToolSpecification> capabilityProvider: capabilityProviders) {
				config.asyncToolSpecifications().addAll(capabilityProvider.getPublisher().collect(Collectors.toList()).block());
			}				
			return config;
		});
	}		
		
	private McpServerCommand createCommand(ServerConfig config) {

		McpServerCommand server = new McpServerCommand(
				config.openTelemetry(), 
				config.syncPromptSpecifications(), 
				config.asyncPromptSpecifications(), 
				config.syncResourceSpecifications(), 
				config.asyncResourceSpecifications(), 
				config.syncToolSpecifications(), 
				config.asyncToolSpecifications());
		
		return server.isEmpty() ? null : server;
	}

}

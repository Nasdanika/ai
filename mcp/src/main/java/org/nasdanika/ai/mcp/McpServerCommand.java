package org.nasdanika.ai.mcp;

import java.util.ArrayList;
import java.util.Collection;

import org.nasdanika.cli.ParentCommands;
import org.nasdanika.cli.RootCommand;
import org.nasdanika.common.Description;

import io.modelcontextprotocol.server.McpServerFeatures.AsyncPromptSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.AsyncResourceSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.AsyncToolSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.SyncPromptSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.SyncResourceSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.opentelemetry.api.OpenTelemetry;
import picocli.CommandLine.Command;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Command(
		description = "MCP server",
		versionProvider = ModuleVersionProvider.class,		
		mixinStandardHelpOptions = true,
		name = "mcp-server")
@ParentCommands(RootCommand.class)
@Description(icon = "https://docs.nasdanika.org/images/mcp.png")
public class McpServerCommand extends McpServerCommandBase {
	
	private Collection<AsyncResourceSpecification> resourceSpecifications = new ArrayList<>();
	private Collection<AsyncToolSpecification> toolSpecifications = new ArrayList<>();
	private Collection<AsyncPromptSpecification> promptSpecifications = new ArrayList<>();

	public McpServerCommand(
			OpenTelemetry openTelemetry,			
			Collection<SyncPromptSpecification> syncPromptSpecifications,
			Collection<AsyncPromptSpecification> asyncPromptSpecifications,			
			Collection<SyncResourceSpecification> syncResourceSpecifications,
			Collection<AsyncResourceSpecification> asyncResourceSpecifications,
			Collection<SyncToolSpecification> syncToolSpecifications,
			Collection<AsyncToolSpecification> asyncToolSpecifications) {
		super(openTelemetry);
		
		if (syncPromptSpecifications != null) {
			for (SyncPromptSpecification syncPromptSpecification: syncPromptSpecifications) {
				this.promptSpecifications.add(new AsyncPromptSpecification(
					syncPromptSpecification.prompt(),
					(exchange, req) -> Mono
						.fromCallable(() -> syncPromptSpecification.promptHandler().apply(new McpSyncServerExchange(exchange), req))
						.subscribeOn(Schedulers.boundedElastic())));
			}
		}
		if (asyncPromptSpecifications != null) {
			promptSpecifications.addAll(asyncPromptSpecifications);
		}
		
		if (syncResourceSpecifications != null) {
			for (SyncResourceSpecification syncResourceSpecification: syncResourceSpecifications) {
				this.resourceSpecifications.add(new AsyncResourceSpecification(
					syncResourceSpecification.resource(),
					(exchange, req) -> Mono
						.fromCallable(() -> syncResourceSpecification.readHandler().apply(new McpSyncServerExchange(exchange), req))
						.subscribeOn(Schedulers.boundedElastic())));
			}
		}
		if (asyncResourceSpecifications != null) {
			resourceSpecifications.addAll(asyncResourceSpecifications);
		}
		
		if (syncToolSpecifications != null) {
			for (SyncToolSpecification syncToolSpecification: syncToolSpecifications) {
				this.toolSpecifications.add(new AsyncToolSpecification(
					syncToolSpecification.tool(),
					(exchange, req) -> Mono
						.fromCallable(() -> syncToolSpecification.call().apply(new McpSyncServerExchange(exchange), req))
						.subscribeOn(Schedulers.boundedElastic())));
			}
		}
		if (asyncToolSpecifications != null) {
			toolSpecifications.addAll(asyncToolSpecifications);
		}
	}
	
	public boolean isEmpty() {
		return resourceSpecifications.isEmpty() && toolSpecifications.isEmpty() && promptSpecifications.isEmpty();
	}

	@Override
	protected Collection<AsyncResourceSpecification> getResourceSpecifications() {
		return resourceSpecifications;
	}

	@Override
	protected Collection<AsyncToolSpecification> getToolSpecifications() {
		return toolSpecifications;
	}

	@Override
	protected Collection<AsyncPromptSpecification> getPromptSpecifications() {
		return promptSpecifications;
	}

}

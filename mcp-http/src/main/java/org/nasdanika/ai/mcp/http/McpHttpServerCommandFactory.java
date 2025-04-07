package org.nasdanika.ai.mcp.http;

import java.util.List;
import java.util.concurrent.CompletionStage;

import org.nasdanika.capability.ServiceCapabilityFactory;
import org.nasdanika.cli.SubCommandCapabilityFactory;
import org.nasdanika.common.ProgressMonitor;

import io.opentelemetry.api.OpenTelemetry;
import picocli.CommandLine;

public class McpHttpServerCommandFactory extends SubCommandCapabilityFactory<McpHttpServerCommand> {

	@Override
	protected Class<McpHttpServerCommand> getCommandType() {
		return McpHttpServerCommand.class;
	}
	
	@Override
	protected CompletionStage<McpHttpServerCommand> doCreateCommand(
			List<CommandLine> parentPath,
			Loader loader,
			ProgressMonitor progressMonitor) {
		
		Requirement<Object, OpenTelemetry> requirement = ServiceCapabilityFactory.createRequirement(OpenTelemetry.class);
		CompletionStage<OpenTelemetry> openTelemetryCS = loader.loadOne(requirement, progressMonitor);		
		return openTelemetryCS.thenApply(ot -> new McpHttpServerCommand(ot, loader.getCapabilityLoader()));
	}

}

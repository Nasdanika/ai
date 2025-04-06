package org.nasdanika.ai.mcp;

import java.util.List;
import java.util.concurrent.CompletionStage;

import org.nasdanika.capability.ServiceCapabilityFactory;
import org.nasdanika.cli.SubCommandCapabilityFactory;
import org.nasdanika.common.ProgressMonitor;

import io.opentelemetry.api.OpenTelemetry;
import picocli.CommandLine;

public class McpServerCommandFactory extends SubCommandCapabilityFactory<McpServerCommand> {

	@Override
	protected Class<McpServerCommand> getCommandType() {
		return McpServerCommand.class;
	}
	
	@Override
	protected CompletionStage<McpServerCommand> doCreateCommand(
			List<CommandLine> parentPath,
			Loader loader,
			ProgressMonitor progressMonitor) {
		
		Requirement<Object, OpenTelemetry> requirement = ServiceCapabilityFactory.createRequirement(OpenTelemetry.class);
		CompletionStage<OpenTelemetry> openTelemetryCS = loader.loadOne(requirement, progressMonitor);		
		return openTelemetryCS.thenApply(ot -> new McpServerCommand(ot, loader.getCapabilityLoader()));
	}

}

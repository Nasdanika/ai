package org.nasdanika.ai.mcp.sse;

import java.util.List;
import java.util.concurrent.CompletionStage;

import org.nasdanika.capability.ServiceCapabilityFactory;
import org.nasdanika.cli.SubCommandCapabilityFactory;
import org.nasdanika.common.ProgressMonitor;

import io.opentelemetry.api.OpenTelemetry;
import picocli.CommandLine;

public class SseTransportCommandFactory extends SubCommandCapabilityFactory<SseTransportCommand> {

	@Override
	protected Class<SseTransportCommand> getCommandType() {
		return SseTransportCommand.class;
	}
	
	@Override
	protected CompletionStage<SseTransportCommand> doCreateCommand(
			List<CommandLine> parentPath,
			Loader loader,
			ProgressMonitor progressMonitor) {
		
		Requirement<Object, OpenTelemetry> openTelemetryRequirement = ServiceCapabilityFactory.createRequirement(OpenTelemetry.class);
		CompletionStage<OpenTelemetry> openTelemetryCS = loader.loadOne(openTelemetryRequirement, progressMonitor);		
		return openTelemetryCS.thenApply(SseTransportCommand::new);
	}

}

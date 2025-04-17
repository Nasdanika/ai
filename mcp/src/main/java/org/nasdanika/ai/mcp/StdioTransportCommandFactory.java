package org.nasdanika.ai.mcp;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.nasdanika.cli.SubCommandCapabilityFactory;
import org.nasdanika.common.ProgressMonitor;

import picocli.CommandLine;

public class StdioTransportCommandFactory extends SubCommandCapabilityFactory<StdioTransportCommand> {

	@Override
	protected Class<StdioTransportCommand> getCommandType() {
		return StdioTransportCommand.class;
	}
	
	@Override
	protected CompletionStage<StdioTransportCommand> doCreateCommand(
			List<CommandLine> parentPath,
			Loader loader,
			ProgressMonitor progressMonitor) {
		return CompletableFuture.completedStage(new StdioTransportCommand());
	}

}

package org.nasdanika.ai.mcp;

import org.nasdanika.cli.CommandBase;
import org.nasdanika.cli.ParentCommands;
import org.nasdanika.common.Description;

import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

@Command(
		description = "MCP STDIO Transport",
		versionProvider = ModuleVersionProvider.class,		
		mixinStandardHelpOptions = true,
		name = "stdio")
@ParentCommands(McpAsyncServerProvider.class)
@Description(icon = "https://docs.nasdanika.org/images/transfers.svg")
public class StdioTransportCommand extends CommandBase {
	
	@ParentCommand
	private McpAsyncServerProvider asyncServerProvider;

	@Override
	public Integer call() throws Exception {
		StdioServerTransportProvider transportProvider = new StdioServerTransportProvider();		
		McpAsyncServer asyncServer = asyncServerProvider.createServer(transportProvider);
		Thread.sleep(1000); // Just in case
		asyncServer.closeGracefully().block();
		return 0;
	}	
	
}

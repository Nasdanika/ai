package org.nasdanika.ai.mcp;

import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.spec.McpServerTransportProvider;

/**
 * Transport provider sub-commands bind to this interface
 */
public interface McpAsyncServerProvider {
	
	McpAsyncServer createServer(McpServerTransportProvider transportProvider);	

}

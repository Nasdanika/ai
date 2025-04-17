import org.nasdanika.ai.mcp.McpServerCommandFactory;
import org.nasdanika.ai.mcp.StdioTransportCommandFactory;
import org.nasdanika.capability.CapabilityFactory;

module org.nasdanika.ai.mcp {
	
	exports org.nasdanika.ai.mcp;
	
	requires transitive io.modelcontextprotocol.sdk.mcp;
	requires transitive org.nasdanika.cli;
	requires transitive io.opentelemetry.context;
	requires transitive java.net.http;
	requires com.fasterxml.jackson.core;
	requires transitive com.fasterxml.jackson.databind;
	requires org.slf4j;
	requires io.opentelemetry.api;
	
	opens org.nasdanika.ai.mcp;
	
	provides CapabilityFactory with 
		StdioTransportCommandFactory,
		McpServerCommandFactory; 
		
}
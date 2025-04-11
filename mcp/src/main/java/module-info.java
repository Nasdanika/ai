import org.nasdanika.ai.mcp.McpServerCommandFactory;
import org.nasdanika.capability.CapabilityFactory;

module org.nasdanika.ai.mcp {
	
	exports org.nasdanika.ai.mcp;
	
	requires transitive io.modelcontextprotocol.sdk.mcp;
	requires transitive org.nasdanika.cli;
	requires io.opentelemetry.context;
	
	opens org.nasdanika.ai.mcp;
	
	provides CapabilityFactory with McpServerCommandFactory; 
		
}
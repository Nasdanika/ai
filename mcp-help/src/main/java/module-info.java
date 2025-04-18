import org.nasdanika.ai.mcp.help.McpServerCapabilitiesHelpContributorFactory;
import org.nasdanika.capability.CapabilityFactory;

module org.nasdanika.ai.mcp.help {
	
	exports org.nasdanika.ai.mcp.help;
	
	requires transitive org.nasdanika.models.app.cli;
	requires org.nasdanika.ai.mcp;
	
	provides CapabilityFactory with McpServerCapabilitiesHelpContributorFactory; 
		
}
import org.nasdanika.ai.mcp.http.SseTransportCommandFactory;
import org.nasdanika.capability.CapabilityFactory;

module org.nasdanika.ai.mcp.http {
	
	exports org.nasdanika.ai.mcp.http;
	
	requires transitive io.modelcontextprotocol.sdk.mcp;
	requires transitive org.nasdanika.cli;
	requires transitive org.nasdanika.http;
	requires org.slf4j;
	requires transitive com.fasterxml.jackson.core;
	requires transitive com.fasterxml.jackson.databind;
	requires reactor.netty.http;
	requires io.netty.buffer;
	requires io.opentelemetry.context;
	requires transitive org.nasdanika.ai.mcp;
	
	opens org.nasdanika.ai.mcp.http;
	
	provides CapabilityFactory with SseTransportCommandFactory; 
		
}
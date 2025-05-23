import org.nasdanika.ai.mcp.sse.SseTransportCommandFactory;
import org.nasdanika.capability.CapabilityFactory;

module org.nasdanika.ai.mcp.sse {
	
	exports org.nasdanika.ai.mcp.sse;
	
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
	
	opens org.nasdanika.ai.mcp.sse;
	
	provides CapabilityFactory with SseTransportCommandFactory; 
		
}
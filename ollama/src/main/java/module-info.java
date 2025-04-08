module org.nasdanika.ai.ollama {
	
	requires transitive org.nasdanika.ai;
	requires transitive org.nasdanika.telemetry;
	requires transitive jtokkit;
	requires io.opentelemetry.context;
	requires org.slf4j;
	requires reactor.netty.http;
	requires reactor.netty.core;
	requires io.netty.codec.http;
	
	exports org.nasdanika.ai.ollama;
		
}

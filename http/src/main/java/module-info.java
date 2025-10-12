module org.nasdanika.ai.http {
	
	exports org.nasdanika.ai.http;
	
	requires transitive org.nasdanika.http;
	requires org.slf4j;
	requires reactor.netty.http;
	requires io.netty.buffer;
	requires io.opentelemetry.context;
	requires transitive org.nasdanika.html.http;
	requires transitive org.nasdanika.ai;
		
}
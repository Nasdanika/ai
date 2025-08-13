module org.nasdanika.ai {
	
	requires transitive reactor.core;
	requires transitive org.reactivestreams;
	requires transitive jtokkit;
	requires transitive hnswlib.core;
	requires transitive hnswlib.utils;
	requires transitive java.desktop;
	requires org.nasdanika.common;
	//requires transitive hnswlib.core.jdk17;
		
	exports org.nasdanika.ai;		
	
}
module org.nasdanika.ai {
	
	requires transitive reactor.core;
	requires transitive org.reactivestreams;
	requires transitive jtokkit;
	requires transitive hnswlib.core;
	requires transitive hnswlib.utils;
	requires transitive java.desktop;
	requires transitive org.nasdanika.common;
	requires transitive org.apache.commons.imaging;
		
	exports org.nasdanika.ai;		
	
}
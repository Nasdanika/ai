module org.nasdanika.ai.cli {
	
	exports org.nasdanika.ai.cli;
	
	requires transitive org.nasdanika.cli;
	requires transitive org.nasdanika.ai;
	requires transitive io.opentelemetry.context;
	requires io.opentelemetry.api;
	requires jdk.incubator.vector;

	opens org.nasdanika.ai.cli;	
		
}
import org.nasdanika.ai.openai.OpenAIClientBuilderCapabilityFactory;
import org.nasdanika.ai.openai.OpenTelemetryHttpPipelinePolicyCapabilityFactory;
import org.nasdanika.capability.CapabilityFactory;

module org.nasdanika.ai.openai {
	
	requires transitive org.nasdanika.ai;
	requires transitive com.azure.ai.openai;
	requires transitive org.nasdanika.telemetry;
	requires io.opentelemetry.context;
	
	exports org.nasdanika.ai.openai;
	opens org.nasdanika.ai.openai to org.nasdanika.capability;
	
	provides CapabilityFactory with 
		OpenAIClientBuilderCapabilityFactory,
		OpenTelemetryHttpPipelinePolicyCapabilityFactory;
	
}
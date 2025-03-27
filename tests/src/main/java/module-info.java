import org.nasdanika.ai.tests.EnvironmentVariableKeyCredentialCapabilityFactory;
import org.nasdanika.capability.CapabilityFactory;

module org.nasdanika.ai.openai.tests {
	
	requires transitive org.nasdanika.ai.openai;
	requires io.opentelemetry.api;
	requires io.opentelemetry.context;
	
	provides CapabilityFactory with EnvironmentVariableKeyCredentialCapabilityFactory;
	
}
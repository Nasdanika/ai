import org.nasdanika.ai.tests.EnvironmentVariableKeyCredentialCapabilityFactory;
import org.nasdanika.ai.tests.OpenAIAdaEmbeddingsCapabilityFactory;
import org.nasdanika.ai.tests.OpenAIGpt35TurboChatCapabilityFactory;
import org.nasdanika.capability.CapabilityFactory;

module org.nasdanika.ai.openai.tests {
	
	requires transitive org.nasdanika.ai.openai;
	requires io.opentelemetry.api;
	requires io.opentelemetry.context;
	requires jtokkit;
	
	provides CapabilityFactory with 
		EnvironmentVariableKeyCredentialCapabilityFactory,
		OpenAIAdaEmbeddingsCapabilityFactory,
		OpenAIGpt35TurboChatCapabilityFactory;
	
}
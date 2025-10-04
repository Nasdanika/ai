import org.nasdanika.ai.tests.EnvironmentVariableKeyCredentialCapabilityFactory;
import org.nasdanika.ai.tests.Llama32OllamaChatCapabilityFactory;
import org.nasdanika.ai.tests.LlavaOllamaChatCapabilityFactory;
import org.nasdanika.ai.tests.OpenAIAdaEmbeddingsCapabilityFactory;
import org.nasdanika.ai.tests.OpenAIGpt35TurboChatCapabilityFactory;
import org.nasdanika.ai.tests.OpenAIGpt4oChatCapabilityFactory;
import org.nasdanika.ai.tests.OpenAIGpt5ChatCapabilityFactory;
import org.nasdanika.ai.tests.SnowflakeArcticEmbedOllamatCapabilityFactory;
import org.nasdanika.capability.CapabilityFactory;

module org.nasdanika.ai.openai.tests {
	
	requires transitive org.nasdanika.ai.openai;
	requires transitive org.nasdanika.ai.ollama;
	requires io.opentelemetry.api;
	requires io.opentelemetry.context;
	requires jtokkit;
	requires org.jline;
	requires org.nasdanika.ai.http;
	
	provides CapabilityFactory with 
		EnvironmentVariableKeyCredentialCapabilityFactory,
		OpenAIAdaEmbeddingsCapabilityFactory,
		OpenAIGpt35TurboChatCapabilityFactory,
		OpenAIGpt4oChatCapabilityFactory,
		OpenAIGpt5ChatCapabilityFactory,
		Llama32OllamaChatCapabilityFactory,
		LlavaOllamaChatCapabilityFactory,		
		SnowflakeArcticEmbedOllamatCapabilityFactory;
	
}
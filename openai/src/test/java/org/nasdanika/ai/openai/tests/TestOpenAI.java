package org.nasdanika.ai.openai.tests;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.nasdanika.capability.CapabilityLoader;
import org.nasdanika.capability.ServiceCapabilityFactory;
import org.nasdanika.capability.ServiceCapabilityFactory.Requirement;
import org.nasdanika.common.PrintStreamProgressMonitor;
import org.nasdanika.common.ProgressMonitor;

import com.azure.ai.openai.OpenAIClientBuilder;

public class TestOpenAI {
	
	@Test
	public void testClienBuilderCapability() {
		CapabilityLoader capabilityLoader = new CapabilityLoader();
		ProgressMonitor progressMonitor = new PrintStreamProgressMonitor();
		Requirement<String, OpenAIClientBuilder> requirement = ServiceCapabilityFactory.createRequirement(
				OpenAIClientBuilder.class,
				null,
				"https://api.openai.com/v1/chat/completions");
		
		OpenAIClientBuilder builder = capabilityLoader.loadOne(requirement, progressMonitor);
		assertNotNull(builder);		
	}
	
}

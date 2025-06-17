package org.nasdanika.ai.tests;

import java.util.concurrent.CompletionStage;

import org.nasdanika.capability.CapabilityProvider;
import org.nasdanika.capability.ServiceCapabilityFactory;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.common.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.azure.core.credential.KeyCredential;

/**
 * Creates a key credential from OPENAI_API_KEY environment variable if it is present
 */
public class EnvironmentVariableKeyCredentialCapabilityFactory extends ServiceCapabilityFactory<String, KeyCredential> {
	
	private static Logger LOGGER = LoggerFactory.getLogger(EnvironmentVariableKeyCredentialCapabilityFactory.class);

	@Override
	protected CompletionStage<Iterable<CapabilityProvider<KeyCredential>>> createService(
			Class<KeyCredential> serviceType, String serviceRequirement, 
			Loader loader,
			ProgressMonitor progressMonitor) {

		String environmentVariableName = getEnvironmentVariableName();
		String key = System.getenv(environmentVariableName);
		if (Util.isBlank(key)) {			
			LOGGER.warn("Cannot create environment variable " + KeyCredential.class + " service because " + environmentVariableName + " is not set");
			return empty();
		}
		
		KeyCredential credential = new KeyCredential(key);
		LOGGER.info("Created " + KeyCredential.class + " service using the value of " + environmentVariableName + " environment variable");
		return wrap(credential);		
	}

	protected String getEnvironmentVariableName() {
		return "OPENAI_API_KEY";
	}

	@Override
	public boolean isFor(Class<?> type, Object serviceRequirement) {
		return type == KeyCredential.class;
	}	

}

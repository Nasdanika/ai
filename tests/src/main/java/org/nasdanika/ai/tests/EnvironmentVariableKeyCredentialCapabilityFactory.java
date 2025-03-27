package org.nasdanika.ai.tests;

import java.util.concurrent.CompletionStage;

import org.nasdanika.capability.CapabilityProvider;
import org.nasdanika.capability.ServiceCapabilityFactory;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.common.Util;

import com.azure.core.credential.KeyCredential;

/**
 * Creates a key credential from OPENAI_API_KEY environment variable if it is present
 */
public class EnvironmentVariableKeyCredentialCapabilityFactory extends ServiceCapabilityFactory<String, KeyCredential> {

	@Override
	protected CompletionStage<Iterable<CapabilityProvider<KeyCredential>>> createService(
			Class<KeyCredential> serviceType, String serviceRequirement, 
			Loader loader,
			ProgressMonitor progressMonitor) {
		
		String key = System.getenv("OPENAI_API_KEY");
		if (Util.isBlank(key)) {				
			return empty();
		}
		
		KeyCredential credential = new KeyCredential(key);
		return wrap(credential);		
	}

	@Override
	public boolean isFor(Class<?> type, Object serviceRequirement) {
		return type == KeyCredential.class;
	}	

}

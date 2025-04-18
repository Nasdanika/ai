package org.nasdanika.ai.mcp.help;

import java.util.concurrent.CompletionStage;

import org.nasdanika.capability.CapabilityProvider;
import org.nasdanika.capability.ServiceCapabilityFactory;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.models.app.cli.ActionHelpMixIn;

public class McpServerCapabilitiesHelpContributorFactory extends ServiceCapabilityFactory<Void, ActionHelpMixIn.Contributor> {
	
	@Override
	public boolean isFor(Class<?> type, Object requirement) {
		return ActionHelpMixIn.Contributor.class == type;
	}

	@Override
	protected CompletionStage<Iterable<CapabilityProvider<ActionHelpMixIn.Contributor>>> createService(
			Class<ActionHelpMixIn.Contributor> serviceType, 
			Void serviceRequirement, 
			Loader loader, 
			ProgressMonitor progressMonitor) {
		
		return wrap(new McpServerCapabilitiesHelpContributor());
	}

}

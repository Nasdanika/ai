package org.nasdanika.ai.mcp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.nasdanika.capability.CapabilityLoader;
import org.nasdanika.cli.CommandGroup;

import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures.AsyncPromptSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.AsyncResourceSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.AsyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities.Builder;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import reactor.core.publisher.Mono;

/**
 * Base class for MCP server commands. 
 * This class does nothing - you'd need to override one or more of 
 * getResourceSpecifications(), getToolSpecifications(), or getPromptSpecifications() methods
 * to provide server capabilities.
 */
public class McpServerCommandBase extends CommandGroup implements McpAsyncServerProvider {

	private OpenTelemetry openTelemetry;

	public McpServerCommandBase(OpenTelemetry openTelemetry) {
		super();
		this.openTelemetry = openTelemetry;
	}

	public McpServerCommandBase(CapabilityLoader capabilityLoader, OpenTelemetry openTelemetry) {
		super(capabilityLoader);
		this.openTelemetry = openTelemetry;
	}
	
	protected Collection<AsyncResourceSpecification> getResourceSpecifications() {
		return Collections.emptyList();
	}
	
	protected Collection<AsyncToolSpecification> getToolSpecifications() {
		return Collections.emptyList();
	}
	
	protected Collection<AsyncPromptSpecification> getPromptSpecifications() {
		return Collections.emptyList();
	}
	
	protected boolean isLogging() {
		return true;
	}
	
	protected void measureDuration(String name, long duration) {
		
	}
	
	@Override
	public McpAsyncServer createServer(McpServerTransportProvider transportProvider) {
		Builder capabilitiesBuilder = ServerCapabilities.builder();
		Collection<AsyncResourceSpecification> resourceSpecifications = getResourceSpecifications();
		if (!resourceSpecifications.isEmpty()) {
			capabilitiesBuilder.resources(true, true);
		}
		Collection<AsyncToolSpecification> toolSpecifications = getToolSpecifications();
		if (!toolSpecifications.isEmpty()) {
			capabilitiesBuilder.tools(true);
		}
		Collection<AsyncPromptSpecification> promptSpecifications = getPromptSpecifications();
		if (!promptSpecifications.isEmpty()) {
			capabilitiesBuilder.prompts(true);
		}
		if (isLogging()) {
			capabilitiesBuilder.logging();
		}		
		
		McpAsyncServer asyncServer = McpServer.async(transportProvider)
				.serverInfo(getName(), getVersion())
				.capabilities(capabilitiesBuilder.build())
				.build();
		
		Tracer tracer = openTelemetry.getTracer(getInstrumentationScopeName());
		McpTelemetryFilter mcpTelemetryFilter = new McpTelemetryFilter(tracer, this::measureDuration);		
		
		List<Mono<Void>> registrations = new ArrayList<>();
		for (AsyncResourceSpecification rSpec: resourceSpecifications) {
			registrations.add(asyncServer.addResource(mcpTelemetryFilter.filter(rSpec)));
		}
		for (AsyncToolSpecification tSpec: toolSpecifications) {
			registrations.add(asyncServer.addTool(mcpTelemetryFilter.filter(tSpec)));
		}
		for (AsyncPromptSpecification pSpec: promptSpecifications) {
			registrations.add(asyncServer.addPrompt(mcpTelemetryFilter.filter(pSpec)));
		}
		
		return Mono.zip(registrations, ra -> asyncServer).block();
	}

	protected String getInstrumentationScopeName() {
		return getClass().getName();
	}

	/**
	 * MCP Server name, this implementation returns command name.
	 */
	protected String getName() {
		return spec.name();
	}
	
	/**
	 * MCP Server name, this implementation returns command name.
	 */
	protected String getVersion() {
		String[] version = spec.version();
		if (version == null) {
			return "(unknown)";
		}
		if (version.length == 1) {
			return version[0];
		}
		return String.join(" ", version);
	}

}

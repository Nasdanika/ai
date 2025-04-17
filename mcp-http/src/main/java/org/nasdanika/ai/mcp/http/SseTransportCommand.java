package org.nasdanika.ai.mcp.http;

import org.nasdanika.ai.mcp.McpAsyncServerProvider;
import org.nasdanika.cli.ParentCommands;
import org.nasdanika.common.Description;
import org.nasdanika.http.AbstractHttpServerCommand;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.opentelemetry.api.OpenTelemetry;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;
import reactor.netty.http.server.HttpServerRoutes;

@Command(
		description = "MCP SSE Transport",
		versionProvider = ModuleVersionProvider.class,		
		mixinStandardHelpOptions = true,
		name = "sse")
@ParentCommands(McpAsyncServerProvider.class)
@Description(icon = "https://docs.nasdanika.org/images/http.svg")
public class SseTransportCommand extends AbstractHttpServerCommand {
	
	private OpenTelemetry openTelemetry;

	public SseTransportCommand(OpenTelemetry openTelemetry) {
		this.openTelemetry = openTelemetry;
	}
	
	protected String getInstrumentationScopeName() {
		return getClass().getName();
	}
	
	@ParentCommand
	private McpAsyncServerProvider asyncServerProvider;
		
	@Option(names = "--base-path", defaultValue = "")
	private String basePath = "";
	
	@Option(
			names = "--resolve-remote-host-name",
			negatable = true,
			defaultValue = "true")
	private boolean resolveRemoteHostName;	
		
	@Option(
			names = "--message-endpoint", 
			description = "Defaults to ${DEFAULT-VALUE}",
			defaultValue = "/messages")
	private String messageEndpoint;	
	
	@Option(
			names = "--sse-endpoint", 
			description = "Defaults to ${DEFAULT-VALUE}",
			defaultValue = "/sse")
	private String sseEndpoint;	
	
	protected void buildRoutes(HttpServerRoutes routes) {
		HttpServerRoutesTransportProvider transportProvider =
			HttpServerRoutesTransportProvider.builder()			
				.propagator(openTelemetry.getPropagators().getTextMapPropagator())
				.tracer(openTelemetry.getTracer(getInstrumentationScopeName()))
				.basePath(basePath)
				.resolveRemoteHostName(resolveRemoteHostName)
				.sseEndpoint(sseEndpoint)
				.messageEndpoint(messageEndpoint)
				.objectMapper(new ObjectMapper())
				.build(routes);
		
		asyncServerProvider.createServer(transportProvider);
	}
	
	@Override
	public Integer call() throws Exception {
		startServer(this::buildRoutes);
		return 0;
	}
	
}

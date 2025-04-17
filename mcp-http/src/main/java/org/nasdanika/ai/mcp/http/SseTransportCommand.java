package org.nasdanika.ai.mcp.http;

import org.nasdanika.ai.mcp.McpAsyncServerProvider;
import org.nasdanika.cli.ParentCommands;
import org.nasdanika.common.Description;
import org.nasdanika.http.AbstractHttpServerCommand;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.opentelemetry.api.OpenTelemetry;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;
import reactor.netty.http.server.HttpServerRoutes;

@Command(
		description = "Routes HTTP requests to a diagram element processor",
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
	
	protected void buildRoutes(HttpServerRoutes routes) {
		HttpServerRoutesTransportProvider transportProvider =
			HttpServerRoutesTransportProvider.builder()
				.propagator(openTelemetry.getPropagators().getTextMapPropagator())
				.tracer(openTelemetry.getTracer(getInstrumentationScopeName()))
				.resolveRemoteHostName(true)
				.messageEndpoint("/messages")
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

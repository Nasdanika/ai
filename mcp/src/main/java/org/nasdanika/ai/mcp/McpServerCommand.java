package org.nasdanika.ai.mcp;

import java.util.ArrayList;
import java.util.List;

import org.nasdanika.capability.CapabilityLoader;
import org.nasdanika.cli.ParentCommands;
import org.nasdanika.cli.RootCommand;
import org.nasdanika.cli.TelemetryCommand;
import org.nasdanika.common.Description;

import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.McpServerFeatures.SyncPromptSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.SyncResourceSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Content;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.LoggingLevel;
import io.modelcontextprotocol.spec.McpSchema.LoggingMessageNotification;
import io.modelcontextprotocol.spec.McpSchema.Prompt;
import io.modelcontextprotocol.spec.McpSchema.PromptArgument;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.Resource;
import io.modelcontextprotocol.spec.McpSchema.ResourceContents;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import io.opentelemetry.api.OpenTelemetry;
import picocli.CommandLine.Command;

@Command(
		description = "MCP server",
		versionProvider = ModuleVersionProvider.class,		
		mixinStandardHelpOptions = true,
		name = "mcp-server")
@ParentCommands(RootCommand.class)
@Description(icon = "https://docs.nasdanika.org/images/mcp.png")
public class McpServerCommand extends TelemetryCommand {

	public McpServerCommand(OpenTelemetry openTelemetry, CapabilityLoader capabilityLoader) {
		super(openTelemetry, capabilityLoader);
	}

	@Override
	protected Integer execute() throws Exception {
		StdioServerTransportProvider transportProvider = new StdioServerTransportProvider();		
		
		McpSyncServer syncServer = McpServer.sync(transportProvider) 
			.serverInfo("my-server", "1.0.0") // TODO - module version
			.capabilities(ServerCapabilities.builder()
				.resources(true, true)
				.tools(true)
				.prompts(true)
				.logging()
				.build())
			.build();
		
		String schema = """
			{
              "type" : "object",
              "id" : "urn:jsonschema:Operation",
              "properties" : {
                "operation" : {
                  "type" : "string"
                },
                "a" : {
                  "type" : "number"
                },
                "b" : {
                  "type" : "number"
                }
              }
            }
			""";
		
		SyncToolSpecification syncToolSpecification = new McpServerFeatures.SyncToolSpecification(
			new Tool("calculator", "Basic calculator", schema), 
			(exchange, arguments) -> {
				List<Content> result = new ArrayList<>();
				
				return new CallToolResult(result, false);
			}
		);
		
		syncServer.addTool(syncToolSpecification);
		
		SyncResourceSpecification syncResourceSpecification = new McpServerFeatures.SyncResourceSpecification(
			new Resource("custom://resource", "name", "description", "mime-type", null), 
			(exchange, request) -> {
				List<ResourceContents> contents = new ArrayList<>();
				
				return new ReadResourceResult(contents);
			}
		);
		
		syncServer.addResource(syncResourceSpecification);
		
		SyncPromptSpecification syncPromptSpecification = new McpServerFeatures.SyncPromptSpecification(
			new Prompt("greeting", "description", List.of(
					new PromptArgument("name", "description", true)
			)), 
			(exchange, request) -> {
				String description = "My prompt description";
				List<PromptMessage> messages = new ArrayList<>();
				
				return new GetPromptResult(description, messages);
			}
		);
		
		syncServer.addPrompt(syncPromptSpecification);
		
		syncServer.loggingNotification(LoggingMessageNotification.builder()
			.level(LoggingLevel.INFO)
			.logger("custom-logger")
			.data("Server initialized")
			.build()
		);	
		
		syncServer.closeGracefully();
		return 0;
	}

}

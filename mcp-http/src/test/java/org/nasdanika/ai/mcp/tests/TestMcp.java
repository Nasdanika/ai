package org.nasdanika.ai.mcp.tests;

import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.nasdanika.ai.mcp.http.HttpServerRoutesTransportProvider;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpServerFeatures.SyncPromptSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.SyncResourceSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ClientCapabilities;
import io.modelcontextprotocol.spec.McpSchema.Content;
import io.modelcontextprotocol.spec.McpSchema.CreateMessageResult;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.ListResourcesResult;
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
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.HttpServerRoutes;

public class TestMcp {
	
	@Test
	@Disabled
	public void testSyncServer() throws Exception {
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
	}
	
	@Test
//	@Disabled
	public void testSseServer() {
		DisposableServer server =
				HttpServer.create()
					.port(8080)
					.route(this::routeMcp)
				    .bindNow();

		server.onDispose().block();
	}	
		
	private static final String INDEX = """
			<!DOCTYPE html>
			<html>
				<body>
				
				<h1>Getting Server Updates</h1>
				
				<div id="result"></div>
				
				<script>
				const x = document.getElementById("result");
				if(typeof(EventSource) !== "undefined") {
				  var source = new EventSource("sse");
				  source.onmessage = function(event) {
				    x.innerHTML += event.data + "<br>";
				  };
				} else {
				  x.innerHTML = "Sorry, no support for server-sent events.";
				}
				</script>
				
				</body>
			</html>						
			""";	
	
	private void routeMcp(HttpServerRoutes routes) {
		routes.get("/index.html", (request, response) -> response.sendString(Mono.just(INDEX)));
		
		HttpServerRoutesTransportProvider transportProvider = new HttpServerRoutesTransportProvider(
				new ObjectMapper(),
				"/messages",
				routes,
				null);
		
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
	}	
	
	@Test
//	@Disabled
	public void testClient() throws Exception {
		ServerParameters params = ServerParameters.builder("C:\\Users\\pavel\\Apps\\nsd-mcp-server\\nsd-mcp-server.bat")
			    .args("mcp-server")
			    .build();
		McpClientTransport transport = new StdioClientTransport(params);

		McpSyncClient client = McpClient.sync(transport)
			    .requestTimeout(Duration.ofSeconds(10))
			    .capabilities(ClientCapabilities.builder()
			        .roots(true)      // Enable roots capability
			        .sampling()       // Enable sampling capability
			        .build())
			    .sampling(request -> {			    	
			    	CreateMessageResult result = null;
					return result;
			    })
			    .build();		
		
		client.initialize();
		ListResourcesResult resources = client.listResources();
		System.out.println(resources);
		
		client.closeGracefully();
		
	}
	
	@Test
	@Disabled
	public void testCallNsd() throws Exception {
		ProcessBuilder processBuilder = new ProcessBuilder("C:\\Users\\pavel\\Apps\\nsd-cli\\nsd.bat", "mcp-server");
		Process process = processBuilder.start();

		InputStream input = process.getInputStream();

        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = input.read(buffer)) != -1) {
            System.err.write(buffer, 0, bytesRead);
        }
    }

}

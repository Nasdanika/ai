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
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpServerFeatures.AsyncPromptSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.AsyncResourceSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.AsyncToolSpecification;
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
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.HttpServerRoutes;
import org.nasdanika.ai.mcp.McpTelemetryFilter;

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
			new Tool("calculator", "Nasdanika calculator of all great things", schema), 
			(exchange, arguments) -> {
				List<Content> result = new ArrayList<>();
				result.add(new TextContent("Result: " + arguments));
				
				return new CallToolResult(result, false);
			}
		);
		
		syncServer.addTool(syncToolSpecification);
		
		SyncResourceSpecification syncResourceSpecification = new McpServerFeatures.SyncResourceSpecification(
			new Resource(
					"nasdanika://drawio", 
					"Drawio site", 
					"Describes how to generate a web site from a drawio diagram", 
					"text/markdown", 
					null), 
			(exchange, request) -> {
				List<ResourceContents> contents = new ArrayList<>();
				contents.add(new TextResourceContents(
						"https://docs.nasdanika.org/core/mapping/index.html", 
						"text/markdown", 
						"""
						Here comes a description on how to generate a web site from a Drawio diagram:
						
						* Do this
						* Then do that
						* Lather
						* Rinse
						* Repeat 
						"""));
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
	}
	
	@Test
//	@Disabled
	public void testSseSyncServer() {
		DisposableServer server =
				HttpServer.create()
					.port(8080)
					.route(this::routeSyncMcp)
				    .bindNow();

		server.onDispose().block();
	}	
	
	@Test
//	@Disabled
	public void testSseAsyncServer() {
		DisposableServer server =
				HttpServer.create()
					.port(8080)
					.route(this::routeAsyncMcp)
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
	
	private void routeSyncMcp(HttpServerRoutes routes) {
		routes.get("/index.html", (request, response) -> response.sendString(Mono.just(INDEX)));
		
		OpenTelemetry openTelemetry = GlobalOpenTelemetry.get();
		
		HttpServerRoutesTransportProvider transportProvider =
			HttpServerRoutesTransportProvider.builder()
				.propagator(openTelemetry.getPropagators().getTextMapPropagator())
				.tracer(openTelemetry.getTracer(TestMcp.class.getName() + ".routeSyncMcp"))
				.resolveRemoteHostName(true)
				.messageEndpoint("/messages")
				.objectMapper(new ObjectMapper())
				.build(routes);
		
		McpSyncServer syncServer = McpServer.sync(transportProvider) 
			.serverInfo("nasdanika-mcp-sync-server", "1.0.0") // TODO - module version
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
		
		McpTelemetryFilter mcpTelemetryFilter = new McpTelemetryFilter(openTelemetry.getTracer(TestMcp.class.getName()), null);
				
		SyncToolSpecification syncToolSpecification = new McpServerFeatures.SyncToolSpecification(
			new Tool("calculator", "Nasdanika calculator of all great things", schema), 
			(exchange, arguments) -> {
				List<Content> result = new ArrayList<>();
				result.add(new TextContent("Result: " + arguments));
				
				return new CallToolResult(result, false);
			}
		);
		
		syncServer.addTool(mcpTelemetryFilter.filter(syncToolSpecification));
		
		SyncResourceSpecification syncResourceSpecification = new McpServerFeatures.SyncResourceSpecification(
			new Resource(
					"nasdanika://drawio", 
					"Drawio site", 
					"Describes how to generate a web site from a drawio diagram", 
					"text/markdown", 
					null), 
			(exchange, request) -> {
				List<ResourceContents> contents = new ArrayList<>();
				contents.add(new TextResourceContents(
						"https://docs.nasdanika.org/core/mapping/index.html", 
						"text/markdown", 
						"""
						Here comes a description on how to generate a web site from a Drawio diagram:
						
						* Do this
						* Then do that
						* Lather
						* Rinse
						* Repeat 
						"""));
				return new ReadResourceResult(contents);
			}
		);
		
		syncServer.addResource(mcpTelemetryFilter.filter(syncResourceSpecification));
		
		
		SyncResourceSpecification syncResourceSpecification2 = new McpServerFeatures.SyncResourceSpecification(
			new Resource(
					"nasdanika://telemetry", 
					"Drawio site", 
					"Describes how to use OpenTelemetry for observability in Java", 
					"text/plain", 
					null), 
			(exchange, request) -> {
				List<ResourceContents> contents = new ArrayList<>();
				contents.add(new TextResourceContents(
						"https://docs.nasdanika.org/core/telemetry/index.html", 
						"text/plain", 
						"""
						This module provides an instance of OpenTelemetry as a capability. The instance is obtained from GlobalOpenTelemetry. The capability factory takes care of installing a logback appender to bridge OpenTelemetry with logging frameworks.

						If you are new to OpenTelemetry, check out Open Telemetry Quick Reference (Java) for general information.
						
						This page focuses on Nasdanika-specific functionality.
						
						Configuration
						By default auto-configuration is disabled. Set otel.java.global-autoconfigure.enabled to true to enable auto-configuration. Then use Environment variables and system properties to configure the global instance.
						
						This is an example of Java command line properties to configure telemetry repoting to a collector over OTLP protocol: -Dotel.java.global-autoconfigure.enabled=true -Dotel.metrics.exporter=otlp -Dotel.logs.exporter=otlp -Dotel.traces.exporter=otlp -Dotel.exporter.otlp.endpoint=http://<VM external IP>:4317 -Dotel.service.name=<service name>.
						
						Logging
						Below is a sample logback.xml file/resource:
						
						<?xml version="1.0" encoding="UTF-8"?>
						<configuration>
						    <appender name="file" class="ch.qos.logback.core.FileAppender">
						        <file>nsd.log</file>
						        <append>true</append>
						        <encoder>
						            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg %kvp{DOUBLE}%n</pattern>
						        </encoder>
						    </appender>    
						    <appender name="OpenTelemetry"
						              class="io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender">
						        <captureExperimentalAttributes>true</captureExperimentalAttributes>
						        <captureKeyValuePairAttributes>true</captureKeyValuePairAttributes>
						    </appender>
						    <root level="INFO">
						        <appender-ref ref="file"/>
						        <appender-ref ref="OpenTelemetry"/>
						    </root>
						</configuration>
						Obtain a capability
						From a non-capability code
						ProgressMonitor progressMonitor = new PrintStreamProgressMonitor();
						CapabilityLoader capabilityLoader = new CapabilityLoader();
						try {
						    Requirement<Object, OpenTelemetry> requirement = ServiceCapabilityFactory.createRequirement(OpenTelemetry.class);
						    OpenTelemetry openTelemetry = capabilityLoader.loadOne(requirement, progressMonitor);
						    
						    ...
						    
						    } finally {
						        parentSpan.end();
						    }
						} finally {             
						    capabilityLoader.close(progressMonitor);
						}
						See Capability for more details.
						
						From another capability provider
						If an instance of OpenTelemetry is required by another capability provider, use CapabilityFactory.Loader instead of CapabilityLoader and chain capability completion stages with thenApply and thenCombine().
						
						thenApply()
						If your capability depends just on the OpenTelemetry capability then use thenApply() as shown below:
						
						public class MyCapabilityFactory extends ServiceCapabilityFactory<MyRequirement, MyCapability> {
						
						    @Override
						    public boolean isFor(Class<?> type, Object requirement) {
						        return MyCapability.class == type && (requirement == null || requirement instanceof MyRequirement);
						    }
						
						    @Override
						    protected CompletionStage<Iterable<CapabilityProvider<MyCapability>>> createService(
						            Class<MyCapability> serviceType, 
						            MyRequirement requirement, 
						            Loader loader,
						            ProgressMonitor progressMonitor) {
						        
						        Requirement<Object, OpenTelemetry> openTelemetryRequirement = ServiceCapabilityFactory.createRequirement(OpenTelemetry.class);
						        CompletionStage<OpenTelemetry> openTelemetryCS = loader.loadOne(openTelemetryRequirement, progressMonitor);      
						        return wrapCompletionStage(openTelemetryCS.thenApply(openTelemetry -> createMyCapability(openTelemetry, requirement)));
						    }
						    
						    protected MyCapability createMyCapability(OpenTelemetry openTelemetry, MyRequirement requirement) {
						        return new MyCapabilityImpl(openTelemetry, requirement);
						    }
						
						}
						thenCombine()
						If your capability depends on the OpenTelemetry capability and other capabilities, then use thenCombine() as shown below:
						
						public class MyCapabilityFactory extends ServiceCapabilityFactory<Void, MyCapability> {
						
						    @Override
						    public boolean isFor(Class<?> type, Object requirement) {
						        return MyCapability.class == type && requirement == null;
						    }
						
						    @Override
						    protected CompletionStage<Iterable<CapabilityProvider<MyCapability>>> createService(
						            Class<MyCapability> serviceType,
						            Void serviceRequirement, 
						            Loader loader, 
						            ProgressMonitor progressMonitor) {
						        
						        
						        Requirement<Object, OpenTelemetry> openTelemetryRequirement = ServiceCapabilityFactory.createRequirement(OpenTelemetry.class);
						        CompletionStage<OpenTelemetry> openTelemetryCS = loader.loadOne(openTelemetryRequirement, progressMonitor);
						        
						        Requirement<String, OpenAIClientBuilder> openAIClientBuilderRequirement = ServiceCapabilityFactory.createRequirement(
						                OpenAIClientBuilder.class,
						                null,
						                "https://api.openai.com/v1/");
						        
						        CompletionStage<OpenAIClientBuilder> openAIClientBuilderCS = loader.loadOne(openAIClientBuilderRequirement, progressMonitor);
						        
						        return wrapCompletionStage(openAIClientBuilderCS.thenCombine(openTelemetryCS, this::createEmbeddings));
						    }
						        
						    protected MyCapability createMyCapability(OpenAIClientBuilder openAIClientBuilder, OpenTelemetry openTelemetry) {
						        return new MyCapabilityImpl(openAIClientBuilder, openTelemetry);
						    }
						    
						}
						"""));
				return new ReadResourceResult(contents);
			}
		);
		
		syncServer.addResource(syncResourceSpecification2);
		
		
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
	
	private void routeAsyncMcp(HttpServerRoutes routes) {
		routes.get("/index.html", (request, response) -> response.sendString(Mono.just(INDEX)));
		
		OpenTelemetry openTelemetry = GlobalOpenTelemetry.get();
		
		HttpServerRoutesTransportProvider transportProvider =
			HttpServerRoutesTransportProvider.builder()
				.propagator(openTelemetry.getPropagators().getTextMapPropagator())
				.tracer(openTelemetry.getTracer(TestMcp.class.getName() + ".routeSyncMcp"))
				.resolveRemoteHostName(true)
				.messageEndpoint("/messages")
				.objectMapper(new ObjectMapper())
				.build(routes);
		
		McpAsyncServer asyncServer = McpServer.async(transportProvider) 
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
		
		McpTelemetryFilter mcpTelemetryFilter = new McpTelemetryFilter(openTelemetry.getTracer(TestMcp.class.getName()), null);
				
		AsyncToolSpecification asyncToolSpecification = new McpServerFeatures.AsyncToolSpecification(
			new Tool("calculator", "Nasdanika calculator of all great things", schema), 
			(exchange, arguments) -> {
				List<Content> result = new ArrayList<>();
				result.add(new TextContent("Result: " + arguments));
				
				return Mono.just(new CallToolResult(result, false));
			}
		);
		
		List<Mono<Void>> registrations = new ArrayList<>();
		
		registrations.add(asyncServer.addTool(mcpTelemetryFilter.filter(asyncToolSpecification)));
		
		AsyncResourceSpecification asyncResourceSpecification = new McpServerFeatures.AsyncResourceSpecification(
			new Resource(
					"nasdanika://drawio", 
					"Drawio site", 
					"Describes how to generate a web site from a drawio diagram", 
					"text/markdown", 
					null), 
			(exchange, request) -> {
				List<ResourceContents> contents = new ArrayList<>();
				contents.add(new TextResourceContents(
						"https://docs.nasdanika.org/core/mapping/index.html", 
						"text/markdown", 
						"""
						Here comes a description on how to generate a web site from a Drawio diagram:
						
						* Do this
						* Then do that
						* Lather
						* Rinse
						* Repeat 
						"""));
				return Mono.just(new ReadResourceResult(contents));
			}
		);
		
		registrations.add(asyncServer.addResource(mcpTelemetryFilter.filter(asyncResourceSpecification)));
		
		
		AsyncResourceSpecification asyncResourceSpecification2 = new McpServerFeatures.AsyncResourceSpecification(
			new Resource(
					"nasdanika://telemetry", 
					"Drawio site", 
					"Describes how to use OpenTelemetry for observability in Java", 
					"text/plain", 
					null), 
			(exchange, request) -> {
				List<ResourceContents> contents = new ArrayList<>();
				contents.add(new TextResourceContents(
						"https://docs.nasdanika.org/core/telemetry/index.html", 
						"text/plain", 
						"""
						This module provides an instance of OpenTelemetry as a capability. The instance is obtained from GlobalOpenTelemetry. The capability factory takes care of installing a logback appender to bridge OpenTelemetry with logging frameworks.

						If you are new to OpenTelemetry, check out Open Telemetry Quick Reference (Java) for general information.
						
						This page focuses on Nasdanika-specific functionality.
						
						Configuration
						By default auto-configuration is disabled. Set otel.java.global-autoconfigure.enabled to true to enable auto-configuration. Then use Environment variables and system properties to configure the global instance.
						
						This is an example of Java command line properties to configure telemetry repoting to a collector over OTLP protocol: -Dotel.java.global-autoconfigure.enabled=true -Dotel.metrics.exporter=otlp -Dotel.logs.exporter=otlp -Dotel.traces.exporter=otlp -Dotel.exporter.otlp.endpoint=http://<VM external IP>:4317 -Dotel.service.name=<service name>.
						
						Logging
						Below is a sample logback.xml file/resource:
						
						<?xml version="1.0" encoding="UTF-8"?>
						<configuration>
						    <appender name="file" class="ch.qos.logback.core.FileAppender">
						        <file>nsd.log</file>
						        <append>true</append>
						        <encoder>
						            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg %kvp{DOUBLE}%n</pattern>
						        </encoder>
						    </appender>    
						    <appender name="OpenTelemetry"
						              class="io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender">
						        <captureExperimentalAttributes>true</captureExperimentalAttributes>
						        <captureKeyValuePairAttributes>true</captureKeyValuePairAttributes>
						    </appender>
						    <root level="INFO">
						        <appender-ref ref="file"/>
						        <appender-ref ref="OpenTelemetry"/>
						    </root>
						</configuration>
						Obtain a capability
						From a non-capability code
						ProgressMonitor progressMonitor = new PrintStreamProgressMonitor();
						CapabilityLoader capabilityLoader = new CapabilityLoader();
						try {
						    Requirement<Object, OpenTelemetry> requirement = ServiceCapabilityFactory.createRequirement(OpenTelemetry.class);
						    OpenTelemetry openTelemetry = capabilityLoader.loadOne(requirement, progressMonitor);
						    
						    ...
						    
						    } finally {
						        parentSpan.end();
						    }
						} finally {             
						    capabilityLoader.close(progressMonitor);
						}
						See Capability for more details.
						
						From another capability provider
						If an instance of OpenTelemetry is required by another capability provider, use CapabilityFactory.Loader instead of CapabilityLoader and chain capability completion stages with thenApply and thenCombine().
						
						thenApply()
						If your capability depends just on the OpenTelemetry capability then use thenApply() as shown below:
						
						public class MyCapabilityFactory extends ServiceCapabilityFactory<MyRequirement, MyCapability> {
						
						    @Override
						    public boolean isFor(Class<?> type, Object requirement) {
						        return MyCapability.class == type && (requirement == null || requirement instanceof MyRequirement);
						    }
						
						    @Override
						    protected CompletionStage<Iterable<CapabilityProvider<MyCapability>>> createService(
						            Class<MyCapability> serviceType, 
						            MyRequirement requirement, 
						            Loader loader,
						            ProgressMonitor progressMonitor) {
						        
						        Requirement<Object, OpenTelemetry> openTelemetryRequirement = ServiceCapabilityFactory.createRequirement(OpenTelemetry.class);
						        CompletionStage<OpenTelemetry> openTelemetryCS = loader.loadOne(openTelemetryRequirement, progressMonitor);      
						        return wrapCompletionStage(openTelemetryCS.thenApply(openTelemetry -> createMyCapability(openTelemetry, requirement)));
						    }
						    
						    protected MyCapability createMyCapability(OpenTelemetry openTelemetry, MyRequirement requirement) {
						        return new MyCapabilityImpl(openTelemetry, requirement);
						    }
						
						}
						thenCombine()
						If your capability depends on the OpenTelemetry capability and other capabilities, then use thenCombine() as shown below:
						
						public class MyCapabilityFactory extends ServiceCapabilityFactory<Void, MyCapability> {
						
						    @Override
						    public boolean isFor(Class<?> type, Object requirement) {
						        return MyCapability.class == type && requirement == null;
						    }
						
						    @Override
						    protected CompletionStage<Iterable<CapabilityProvider<MyCapability>>> createService(
						            Class<MyCapability> serviceType,
						            Void serviceRequirement, 
						            Loader loader, 
						            ProgressMonitor progressMonitor) {
						        
						        
						        Requirement<Object, OpenTelemetry> openTelemetryRequirement = ServiceCapabilityFactory.createRequirement(OpenTelemetry.class);
						        CompletionStage<OpenTelemetry> openTelemetryCS = loader.loadOne(openTelemetryRequirement, progressMonitor);
						        
						        Requirement<String, OpenAIClientBuilder> openAIClientBuilderRequirement = ServiceCapabilityFactory.createRequirement(
						                OpenAIClientBuilder.class,
						                null,
						                "https://api.openai.com/v1/");
						        
						        CompletionStage<OpenAIClientBuilder> openAIClientBuilderCS = loader.loadOne(openAIClientBuilderRequirement, progressMonitor);
						        
						        return wrapCompletionStage(openAIClientBuilderCS.thenCombine(openTelemetryCS, this::createEmbeddings));
						    }
						        
						    protected MyCapability createMyCapability(OpenAIClientBuilder openAIClientBuilder, OpenTelemetry openTelemetry) {
						        return new MyCapabilityImpl(openAIClientBuilder, openTelemetry);
						    }
						    
						}
						"""));
				return Mono.just(new ReadResourceResult(contents));
			}
		);
		
		registrations.add(asyncServer.addResource(asyncResourceSpecification2));
		
		AsyncPromptSpecification asyncPromptSpecification = new McpServerFeatures.AsyncPromptSpecification(
			new Prompt("greeting", "description", List.of(
					new PromptArgument("name", "description", true)
			)), 
			(exchange, request) -> {
				String description = "My prompt description";
				List<PromptMessage> messages = new ArrayList<>();
				
				return Mono.just(new GetPromptResult(description, messages));
			}
		);
		
		registrations.add(asyncServer.addPrompt(asyncPromptSpecification));
		
		registrations.add(asyncServer.loggingNotification(LoggingMessageNotification.builder()
			.level(LoggingLevel.INFO)
			.logger("custom-logger")
			.data("Server initialized")
			.build()
		));
		
		registrations.forEach(mono -> mono.block());
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

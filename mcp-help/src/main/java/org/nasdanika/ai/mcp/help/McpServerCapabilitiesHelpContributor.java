package org.nasdanika.ai.mcp.help;

import java.util.Collection;

import org.nasdanika.ai.mcp.McpServerCommandBase;
import org.nasdanika.common.DocumentationFactory;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.models.app.Action;
import org.nasdanika.models.app.AppFactory;
import org.nasdanika.models.app.Label;
import org.nasdanika.models.app.cli.ActionHelpMixIn;
import org.nasdanika.models.app.gen.DynamicTableBuilder;

import io.modelcontextprotocol.server.McpServerFeatures.AsyncPromptSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.AsyncResourceSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.AsyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema;
import picocli.CommandLine;

public class McpServerCapabilitiesHelpContributor implements ActionHelpMixIn.Contributor  {

	@Override
	public Label build(
			Label label, 
			CommandLine commandLine, 
			Collection<DocumentationFactory> documentationFactories,
			ProgressMonitor progressMonitor) {
		
		Object userObj = commandLine.getCommandSpec().userObject();
		if (label instanceof Action && userObj instanceof McpServerCommandBase) {
			McpServerCommandBase mcpServerCommandBase = (McpServerCommandBase) userObj;
			Action helpAction = (Action) label;			
			
			Collection<AsyncPromptSpecification> promptSpecs = mcpServerCommandBase.getPromptSpecifications();
			if (!promptSpecs.isEmpty()) {
				Action promptsSection = AppFactory.eINSTANCE.createAction();
				helpAction.getSections().add(promptsSection);
				promptsSection.setText("Prompts");
				promptsSection.setName("prompts");
				promptsSection.setIcon("https://docs.nasdanika.org/images/chatbot.svg");
				DynamicTableBuilder<McpSchema.Prompt> promptsTableBuilder = new DynamicTableBuilder<>("nsd-table");
				promptsTableBuilder.addStringColumnBuilder("name", true, true, "Name", McpSchema.Prompt::name);
				promptsTableBuilder.addStringColumnBuilder("description", true, false, "Description", McpSchema.Prompt::description);
				
				org.nasdanika.models.html.Tag promptsTable = promptsTableBuilder.build(
						promptSpecs
							.stream()
							.map(AsyncPromptSpecification::prompt)
							.sorted((a,b) -> a.name().compareTo(b.name()))
							.toList(),  
						"prompts", 
						"prompts-table", 
						progressMonitor);
				
				promptsSection.getContent().add(promptsTable);				
			}
			
			Collection<AsyncResourceSpecification> resourceSpecs = mcpServerCommandBase.getResourceSpecifications();
			if (!resourceSpecs.isEmpty()) {
				Action resourcesSection = AppFactory.eINSTANCE.createAction();
				helpAction.getSections().add(resourcesSection);
				resourcesSection.setText("Resources");
				resourcesSection.setName("resources");
				resourcesSection.setIcon("https://docs.nasdanika.org/images/digital-asset.svg");
				DynamicTableBuilder<McpSchema.Resource> resourcesTableBuilder = new DynamicTableBuilder<>("nsd-table");
				resourcesTableBuilder.addStringColumnBuilder("name", true, true, "Name", McpSchema.Resource::name);
				resourcesTableBuilder.addStringColumnBuilder("description", true, false, "Description", McpSchema.Resource::description);
				
				org.nasdanika.models.html.Tag resourcesTable = resourcesTableBuilder.build(
						resourceSpecs
							.stream()
							.map(AsyncResourceSpecification::resource)
							.sorted((a,b) -> a.name().compareTo(b.name()))
							.toList(),  
						"resources", 
						"resources-table", 
						progressMonitor);
				
				resourcesSection.getContent().add(resourcesTable);				
			}			
			
			Collection<AsyncToolSpecification> toolSpecs = mcpServerCommandBase.getToolSpecifications();
			if (!toolSpecs.isEmpty()) {
				Action toolsSection = AppFactory.eINSTANCE.createAction();
				helpAction.getSections().add(toolsSection);
				toolsSection.setText("Tools");
				toolsSection.setName("tools");
				toolsSection.setIcon("https://docs.nasdanika.org/images/toolbox.svg");
				DynamicTableBuilder<McpSchema.Tool> toolsTableBuilder = new DynamicTableBuilder<>("nsd-table");
				toolsTableBuilder.addStringColumnBuilder("name", true, true, "Name", McpSchema.Tool::name);
				toolsTableBuilder.addStringColumnBuilder("description", true, false, "Description", McpSchema.Tool::description);
				
				org.nasdanika.models.html.Tag toolsTable = toolsTableBuilder.build(
						toolSpecs
							.stream()
							.map(AsyncToolSpecification::tool)
							.sorted((a,b) -> a.name().compareTo(b.name()))
							.toList(),  
						"tools", 
						"tools-table", 
						progressMonitor);
				
				toolsSection.getContent().add(toolsTable);				
			}
			
		}
		
		return label;
	}

}

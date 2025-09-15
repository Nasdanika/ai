package org.nasdanika.ai.drawio.tests;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.nasdanika.common.PrintStreamProgressMonitor;
import org.nasdanika.common.Section;
import org.nasdanika.drawio.Document;
//import org.nasdanika.drawio.gen.section.DrawioSectionGenerator;

import reactor.core.publisher.Flux;

public class Tests {
	
//	@Test
//	public void testSectionGenerator() throws Exception {
//		Document document = Document.load(getClass().getResource("alice-bob.drawio"));
//		DrawioSectionGenerator sectionGenerator = new DrawioSectionGenerator();
//		Flux<Section> sectionFlux = sectionGenerator.creatSectionsAsync(document, new PrintStreamProgressMonitor());
//		List<Section> sections = sectionFlux.collectList().block();
//		System.out.println(sections.size());
//		sections.forEach(section -> {
//			System.out.println(section.getTitle() + ": " + section.getId() + " " + section.getContents() + " " + section.getChildren());
//		});
//	}
		
}

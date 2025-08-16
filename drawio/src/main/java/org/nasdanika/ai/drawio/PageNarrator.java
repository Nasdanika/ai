package org.nasdanika.ai.drawio;

import java.util.function.Predicate;

import org.nasdanika.drawio.Element;
import org.nasdanika.drawio.Page;

public class PageNarrator extends LinkTargetNarrator<Page> {

	@Override
	public String generate(Page input, int headerLevel, Predicate<? super Element> traversalPredicate) {
		// TODO Page name as a header, delegate to root
		return "TODO";
	}
	

}

package org.nasdanika.ai.drawio;

import java.util.function.Predicate;

import org.nasdanika.drawio.Element;
import org.nasdanika.drawio.Model;

public class ModelNarrator extends ElementNarrator<Model> {

	@Override
	public String generate(Model input, int headerLevel, Predicate<? super Element> traversalPredicate) {
		// TODO - delegate to root narrator
		return "TODO";
	}
	

}

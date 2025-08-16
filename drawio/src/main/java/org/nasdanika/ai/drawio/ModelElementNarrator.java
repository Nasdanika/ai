package org.nasdanika.ai.drawio;

import java.util.function.Predicate;

import org.nasdanika.drawio.Element;
import org.nasdanika.drawio.ModelElement;

public abstract class ModelElementNarrator<E extends ModelElement> extends LinkTargetNarrator<E> {
	
	@Override
	public String generate(E input, int headerLevel, Predicate<? super Element> traversalPredicate) {
		// TODO Auto-generated method stub
		return "TODO";
	}
	
	// Style narration - color - named colors, closest in the cylindrical coordinates and then lighter/darker, more/less saturated
	
}

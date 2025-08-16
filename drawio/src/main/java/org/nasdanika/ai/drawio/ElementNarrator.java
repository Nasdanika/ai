package org.nasdanika.ai.drawio;

import java.util.HashSet;
import java.util.function.Predicate;

import org.nasdanika.ai.Narrator;
import org.nasdanika.drawio.Element;

import reactor.core.publisher.Mono;

public abstract class ElementNarrator<E extends Element> implements Narrator<E> {
	
	@Override
	public String generate(E input) {
		return generate(input, 0, new HashSet<>()::add);
	}
	
	@Override
	public Mono<String> generateAsync(E input) {
		return Mono.just(generate(input));
	}
	
	public abstract String generate(E input, int headerLevel, Predicate<? super Element> traversalPredicate);
	
	// TODO - getting a narrator from factory - similar to document site generation
	
}

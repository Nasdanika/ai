package org.nasdanika.ai;

/**
 * Converts source to text (String).
 * For example, generates a description of a model object.
 * Text can be further generated into vectors for RAG and 
 * be used in prompts.
 * @param <S>
 */
public interface Narrator<S> extends EmbeddingGenerator<S, String> {

}

package org.nasdanika.ai.drawio;

import org.nasdanika.drawio.LinkTarget;

/**
 * Base interface for diagram elements which can be linked from {@link ModelElement}s - {@link Page} and ModelElement.
 */
public abstract class LinkTargetNarrator<E extends LinkTarget> extends ElementNarrator<E> {

}

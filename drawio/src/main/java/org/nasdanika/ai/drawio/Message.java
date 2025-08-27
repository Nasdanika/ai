package org.nasdanika.ai.drawio;

import java.util.function.Consumer;

public abstract class Message {
	
	private BaseProcessor<?> processor;
	private int depth;

	public Message(BaseProcessor<?> processor, int depth) {
		this.processor = processor;
		this.depth = depth;
	}
	
	public int getDepth() {
		return depth;
	}
	
	public BaseProcessor<?> getProcessor() {
		return processor;
	}
	
	abstract void process(Consumer<Message> publisher);
	
}	

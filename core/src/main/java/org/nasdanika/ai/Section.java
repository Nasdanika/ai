package org.nasdanika.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A section of structured narration
 */
public class Section extends SectionReference {
	
	private List<Section> children = Collections.synchronizedList(new ArrayList<>());
	private List<Content> contents = Collections.synchronizedList(new ArrayList<>());
	private Object source;
	
	public Section() {
		
	}
	
	public Section(String title, String id, Object source) {
		super(title, id);
		this.source = source;
	}
	
	public Object getSource() {
		return source;
	}
	
	public void setSource(Object source) {
		this.source = source;
	}
	
	public List<Section> getChildren() {
		return children;
	}
	
	public List<Content> getContents() {
		return contents;
	}

}

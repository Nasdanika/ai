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
	
	public Section() {
		
	}
	
	public Section(String title, String id) {
		super(title, id);
	}
	
	public List<Section> getChildren() {
		return children;
	}
	
	public List<Content> getContents() {
		return contents;
	}

}

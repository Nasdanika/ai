package org.nasdanika.ai;

public class SectionReference {
	
	private String title;
	private String id;
	
	public SectionReference() {
		
	}
	
	public SectionReference(String title, String id) {
		this.title = title;
		this.id = id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	public String getId() {
		return id;
	}
	
	public String getTitle() {
		return title;
	}

}

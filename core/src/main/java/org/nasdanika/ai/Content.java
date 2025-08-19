package org.nasdanika.ai;

/**
 * A unit of text content with content type. For example, text/plain, text/markdown, text/html.
 */
public class Content {
	
	private String content;
	private String contentType;
	
	public Content() {
		
	}

	public Content(String content, String contentType) {
		this.content = content;
		this.contentType = contentType;
	}
	
	public String getContent() {
		return content;
	}
	
	public String getContentType() {
		return contentType;
	}
	
	public void setContent(String content) {
		this.content = content;
	}
	
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

}

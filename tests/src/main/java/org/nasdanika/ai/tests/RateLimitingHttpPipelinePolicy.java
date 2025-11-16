package org.nasdanika.ai.tests;

import com.azure.core.http.HttpPipelineCallContext;
import com.azure.core.http.HttpPipelineNextPolicy;
import com.azure.core.http.HttpResponse;
import com.azure.core.http.policy.HttpPipelinePolicy;

import reactor.core.publisher.Mono;

public class RateLimitingHttpPipelinePolicy implements HttpPipelinePolicy {
	
	@Override
	public Mono<HttpResponse> process(HttpPipelineCallContext context, HttpPipelineNextPolicy next) {
		return Mono.deferContextual(contextView -> {
			System.out.println(context.getHttpRequest().getUrl());	        
			Mono<HttpResponse> result = next.process();
			return result;
		});
	}
	
}

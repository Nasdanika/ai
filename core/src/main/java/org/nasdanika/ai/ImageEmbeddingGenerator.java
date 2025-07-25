package org.nasdanika.ai;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.function.Function;

import javax.imageio.ImageIO;

import reactor.core.publisher.Mono;

public interface ImageEmbeddingGenerator<E> extends EmbeddingGenerator<BufferedImage, E> {
		
	default EmbeddingGenerator<InputStream,E> asInputStreamEmbeddingGenerator() {
		Function<InputStream, Mono<BufferedImage>> mapper = in -> {
			try {
				return Mono.just(ImageIO.read(in));
			} catch (IOException e) {
				throw new IllegalArgumentException("Cannot read image from input stream: " + e, e);
			}
		};
		
		return adapt(mapper);
	}
		
	default EmbeddingGenerator<URL,E> asUrlEmbeddingGenerator() {
		Function<URL, Mono<BufferedImage>> mapper = url -> {
			try {
				return Mono.just(ImageIO.read(url));
			} catch (IOException e) {
				throw new IllegalArgumentException("Cannot read image from '" + url + "': " + e, e);
			}
		};
		
		return adapt(mapper);
	}	
	
	default EmbeddingGenerator<File,E> asFileEmbeddingGenerator() {
		Function<File, Mono<BufferedImage>> mapper = file -> {
			try {
				return Mono.just(ImageIO.read(file));
			} catch (IOException e) {
				throw new IllegalArgumentException("Cannot read image from '" + file.getAbsolutePath() + "': " + e, e);
			}
		};
		
		return adapt(mapper);
	}	
	

}

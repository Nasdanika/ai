package org.nasdanika.ai;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.nasdanika.common.Composable;

import reactor.core.publisher.Mono;

/**
 * Generates an embedding from source. 
 * @param <S>
 * @param <E>
 */
public interface EmbeddingGenerator<S,E> extends Composable<EmbeddingGenerator<S,E>> {
	
	/**
	 * {@link EmbeddingGenerator} requirement.
	 * Predicates can be null.
	 */
	record Requirement(
		Class<?> sourceType,
		Class<?> embeddingType,
		Predicate<Class<? extends EmbeddingGenerator<?,?>>> typePredicate,		
		Predicate<EmbeddingGenerator<?,?>> predicate) {}	
	
	default E generate(S input) {
		return generateAsync(input).block();
	}
	
	Mono<E> generateAsync(S input);
	
	/**
	 * Batch generation
	 */
	default Map<S, E> generate(Collection<S> input) {
		return generateAsync(input).block();
	}
	
	/**
	 * Asynchronous batch generation
	 */
	default Mono<Map<S, E>> generateAsync(Collection<S> input) {
		List<Mono<Entry<S,E>>> monos = input
			.stream()
			.map(ie -> {
				Mono<E> embMono = generateAsync(ie);
				return embMono.map(emb -> Map.entry(ie, emb));
			})
			.toList();
		
		return Mono.zip(monos, this::combine);
	}
		
	private Map<S, E> combine(Object[] elements) {
		Map<S, E> ret = new LinkedHashMap<>();
		for (Object el: elements) {
			@SuppressWarnings("unchecked")
			Entry<S,E> e = (Entry<S,E>) el;
			ret.put(e.getKey(), e.getValue());
		}		
		return ret;
	}	
	
	default <F> EmbeddingGenerator<S,F> then(EmbeddingGenerator<E,F> next) {
		return new EmbeddingGenerator<S, F>() {

			@Override
			public Mono<F> generateAsync(S source) {
				return EmbeddingGenerator.this.generateAsync(source).flatMap(next::generateAsync);
			}
			
			@Override
			public F generate(S input) {
				return next.generate(EmbeddingGenerator.this.generate(input));
			}
			
		};
		
	}
	
	default <V> EmbeddingGenerator<V,E> adapt(Function<V,Mono<S>> mapper) {
		
		return new EmbeddingGenerator<V, E>() {

			@Override
			public Mono<E> generateAsync(V source) {
				return mapper.apply(source).flatMap(EmbeddingGenerator.this::generateAsync);
			}
			
		};
		
	}	
	
	/**
	 * Calls this embedding generator and returns its return value if it is not null and composer is null.
	 * Otherwise calls the other embedding generator and then composes results by calling the composer argument. 
	 * @param other
	 * @param
	 * @return
	 */
	default EmbeddingGenerator<S,E> compose(EmbeddingGenerator<? super S,? extends E> other, BinaryOperator<E> composer) {
		if (other == null) {
			return this;
		}
		
		return new EmbeddingGenerator<S, E>() {

			@Override
			public Mono<E> generateAsync(S input) {
				Mono<E> thisResult = EmbeddingGenerator.this.generateAsync(input);
				if (composer == null) {					
					return thisResult.switchIfEmpty(other.generateAsync(input));
				}
				
				Mono<? extends E> otherResult = other.generateAsync(input);				
		        Function<E, Mono<E>> transformer = a -> otherResult.map(b -> composer.apply(a, b)).defaultIfEmpty(a);
		        
				return thisResult
		        		.flatMap(transformer)
		        		.switchIfEmpty(otherResult);
			}
			
			@Override
			public E generate(S input) {
				E thisResult = EmbeddingGenerator.this.generate(input);
				if (composer == null) {
					return thisResult == null ? other.generate(input) : thisResult;
				}
				
				return composer.apply(thisResult, other.generate(input));
			}
			
		};
	}
	
	/**
	 * Calls this embedding generator and returns its return value if it is not null and composer is null.
	 * Otherwise calls the other embedding generator and then composes results by calling the composer argument. 
	 * @param other
	 * @param
	 * @return
	 */
	default EmbeddingGenerator<S,E> composeAsync(EmbeddingGenerator<? super S,? extends E> other, BiFunction<? super E, ? super E, Mono<E>> composer) {
		if (other == null) {
			return this;
		}
		
		return new EmbeddingGenerator<S, E>() {

			@Override
			public Mono<E> generateAsync(S input) {
				Mono<E> thisResult = EmbeddingGenerator.this.generateAsync(input);
				if (composer == null) {					
					return thisResult.switchIfEmpty(other.generateAsync(input));
				}
				
				Mono<? extends E> otherResult = other.generateAsync(input);				
		        Function<E, Mono<E>> transformer = a -> otherResult.flatMap(b -> composer.apply(a, b)).defaultIfEmpty(a);
		        
				return thisResult
		        		.flatMap(transformer)
		        		.switchIfEmpty(otherResult);
			}
			
		};
	}
	
	@Override
	default EmbeddingGenerator<S, E> compose(EmbeddingGenerator<S, E> other) {
		return compose(other, null);
	}
	
	/**
	 * @param <T> Instances of T shall implement {@link Composable}.
	 * @return Composing operator which can be use in reducing streams of {@link Composable}s to a single composeable.
	 */
	static <S,E> EmbeddingGenerator<S,E> compose(EmbeddingGenerator<S,E> a, EmbeddingGenerator<S,E> b, BinaryOperator<E> composer) {	
		if (a == null) {
			return b;
		}
		if (b == null) {
			return a;
		}
		return a.compose(b, composer);
	}	
	
	static <S,E> Optional<EmbeddingGenerator<S, E>> reduce(Stream<EmbeddingGenerator<S, E>> stream, BinaryOperator<E> composer) {
		return stream.reduce((a, b) -> compose(a, b, composer));
	}

}

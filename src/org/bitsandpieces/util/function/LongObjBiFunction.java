/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.function;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Represents a function that accepts two arguments and produces a result. This
 * is a two-arity specialization of {@link Function}.
 *
 * <p>
 * This is a functional interface whose functional method is
 * {@link #apply(long, Object)}.
 *
 * @param <U> the type of the second argument to the function
 * @param <R> the type of the result of the function
 *
 * @see Function
 */
@FunctionalInterface
public interface LongObjBiFunction<U, R> extends BiFunction<Long, U, R> {

	@Override
	default R apply(Long t, U u) {
		return apply(t.longValue(), u);
	}

	/**
	 * Applies this function to the given arguments.
	 *
	 * @param t the first function argument
	 * @param u the second function argument
	 * @return the function result
	 */
	R apply(long t, U u);

	@Override
	default <V> LongObjBiFunction<U, V> andThen(Function<? super R, ? extends V> after) {
		Objects.requireNonNull(after);
		return (long t, U u) -> after.apply(apply(t, u));
	}
}

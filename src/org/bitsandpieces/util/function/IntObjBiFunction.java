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
 * {@link #apply(int, Object)}.
 *
 * @param <U> the type of the second argument to the function
 * @param <R> the type of the result of the function
 *
 * @see Function
 */
@FunctionalInterface
public interface IntObjBiFunction<U, R> extends BiFunction<Integer, U, R> {

	@Override
	default R apply(Integer t, U u) {
		return apply(t.intValue(), u);
	}

	/**
	 * Applies this function to the given arguments.
	 *
	 * @param t the first function argument
	 * @param u the second function argument
	 * @return the function result
	 */
	R apply(int t, U u);

	@Override
	default <V> IntObjBiFunction<U, V> andThen(Function<? super R, ? extends V> after) {
		Objects.requireNonNull(after);
		return (int t, U u) -> after.apply(apply(t, u));
	}
}

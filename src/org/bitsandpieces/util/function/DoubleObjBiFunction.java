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
 * {@link #apply(double, Object)}.
 *
 * @param <U> the type of the second argument to the function
 * @param <R> the type of the result of the function
 *
 * @see Function
 */
@FunctionalInterface
public interface DoubleObjBiFunction<U, R> extends BiFunction<Double, U, R> {

	@Override
	default R apply(Double t, U u) {
		return apply(t.doubleValue(), u);
	}

	/**
	 * Applies this function to the given arguments.
	 *
	 * @param t the first function argument
	 * @param u the second function argument
	 * @return the function result
	 */
	R apply(double t, U u);

	@Override
	default <V> DoubleObjBiFunction<U, V> andThen(Function<? super R, ? extends V> after) {
		Objects.requireNonNull(after);
		return (double t, U u) -> after.apply(apply(t, u));
	}
}

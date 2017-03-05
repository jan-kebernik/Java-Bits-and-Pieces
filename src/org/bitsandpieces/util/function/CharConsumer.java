/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.function;

import java.util.Objects;

/**
 * Represents an operation that accepts a single {@code char}-valued argument
 * and returns no result. This is the primitive type specialization of
 * {@link Consumer} for {@code char}. Unlike most other functional interfaces,
 * {@code CharConsumer} is expected to operate via side-effects.
 *
 * <p>
 * This is a whose functional method is {@link #accept(char)}.
 *
 * @see Consumer
 */
@FunctionalInterface
public interface CharConsumer {

	/**
	 * Performs this operation on the given argument.
	 *
	 * @param value the input argument
	 */
	void accept(char value);

	/**
	 * Returns a composed {@code CharConsumer} that performs, in sequence, this
	 * operation followed by the {@code after} operation. If performing either
	 * operation throws an exception, it is relayed to the caller of the
	 * composed operation. If performing this operation throws an exception, the
	 * {@code after} operation will not be performed.
	 *
	 * @param after the operation to perform after this operation
	 * @return a composed {@code CharConsumer} that performs in sequence this
	 * operation followed by the {@code after} operation
	 * @throws NullPointerException if {@code after} is null
	 */
	default CharConsumer andThen(CharConsumer after) {
		Objects.requireNonNull(after);
		return (char t) -> {
			accept(t);
			after.accept(t);
		};
	}
}

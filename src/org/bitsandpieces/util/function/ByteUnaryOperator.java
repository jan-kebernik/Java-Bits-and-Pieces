/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.function;

import java.util.Objects;

/**
 * Represents an operation on a single {@code byte}-valued operand that produces
 * a {@code byte}-valued result. This is the primitive type specialization of
 * {@link UnaryOperator} for {@code byte}.
 *
 * <p>
 * This is a <em>functional interface</em>
 * whose functional method is {@link #applyAsByte(byte)}.
 *
 * @see UnaryOperator
 */
@FunctionalInterface
public interface ByteUnaryOperator {

	/**
	 * Applies this operator to the given operand.
	 *
	 * @param operand the operand
	 * @return the operator result
	 */
	byte applyAsByte(byte operand);

	/**
	 * Returns a composed operator that first applies the {@code before}
	 * operator to its input, and then applies this operator to the result. If
	 * evaluation of either operator throws an exception, it is relayed to the
	 * caller of the composed operator.
	 *
	 * @param before the operator to apply before this operator is applied
	 * @return a composed operator that first applies the {@code before}
	 * operator and then applies this operator
	 * @throws NullPointerException if before is null
	 *
	 * @see #andThen(ByteUnaryOperator)
	 */
	default ByteUnaryOperator compose(ByteUnaryOperator before) {
		Objects.requireNonNull(before);
		return (byte v) -> applyAsByte(before.applyAsByte(v));
	}

	/**
	 * Returns a composed operator that first applies this operator to its
	 * input, and then applies the {@code after} operator to the result. If
	 * evaluation of either operator throws an exception, it is relayed to the
	 * caller of the composed operator.
	 *
	 * @param after the operator to apply after this operator is applied
	 * @return a composed operator that first applies this operator and then
	 * applies the {@code after} operator
	 * @throws NullPointerException if after is null
	 *
	 * @see #compose(ByteUnaryOperator)
	 */
	default ByteUnaryOperator andThen(ByteUnaryOperator after) {
		Objects.requireNonNull(after);
		return (byte t) -> after.applyAsByte(applyAsByte(t));
	}

	/**
	 * Returns a unary operator that always returns its input argument.
	 *
	 * @return a unary operator that always returns its input argument
	 */
	static ByteUnaryOperator identity() {
		return t -> t;
	}
}

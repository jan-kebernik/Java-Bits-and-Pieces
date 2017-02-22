/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.collection.primitive;

import java.util.Objects;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

/**
 *
 * @author Jan Kebernik
 */
public interface PrimitiveIterable<T, T_CONS, T_ITR extends PrimitiveIterator<T, T_CONS>, T_SPLITR extends Spliterator.OfPrimitive<T, T_CONS, T_SPLITR>> extends Iterable<T> {

	void forEach(T_CONS action);

	@Override
	T_SPLITR spliterator();

	@Override
	T_ITR iterator();

	public static interface IntIterable extends PrimitiveIterable<Integer, IntConsumer, PrimitiveIterator.OfInt, Spliterator.OfInt> {

		@Override
		public default void forEach(Consumer<? super Integer> action) {
			if (action instanceof IntConsumer) {
				forEach((IntConsumer) action);
				return;
			}
			Objects.requireNonNull(action);
			forEach((IntConsumer) action::accept);
		}

		@Override
		public default void forEach(IntConsumer action) {
			// avoid unboxing
			iterator().forEachRemaining(action);
		}

		@Override
		public default Spliterator.OfInt spliterator() {
			return Spliterators.spliteratorUnknownSize(iterator(), 0);
		}
	}

	public static interface LongIterable extends PrimitiveIterable<Long, LongConsumer, PrimitiveIterator.OfLong, Spliterator.OfLong> {

		@Override
		public default void forEach(Consumer<? super Long> action) {
			if (action instanceof LongConsumer) {
				forEach((LongConsumer) action);
				return;
			}
			Objects.requireNonNull(action);
			forEach((LongConsumer) action::accept);
		}

		@Override
		public default void forEach(LongConsumer action) {
			// avoid unboxing
			iterator().forEachRemaining(action);
		}

		@Override
		public default Spliterator.OfLong spliterator() {
			return Spliterators.spliteratorUnknownSize(iterator(), 0);
		}
	}

	public static interface DoubleIterable extends PrimitiveIterable<Double, DoubleConsumer, PrimitiveIterator.OfDouble, Spliterator.OfDouble> {

		@Override
		public default void forEach(Consumer<? super Double> action) {
			if (action instanceof DoubleConsumer) {
				forEach((DoubleConsumer) action);
				return;
			}
			Objects.requireNonNull(action);
			forEach((DoubleConsumer) action::accept);
		}

		@Override
		public default void forEach(DoubleConsumer action) {
			// avoid unboxing
			iterator().forEachRemaining(action);
		}

		@Override
		public default Spliterator.OfDouble spliterator() {
			return Spliterators.spliteratorUnknownSize(iterator(), 0);
		}
	}
}


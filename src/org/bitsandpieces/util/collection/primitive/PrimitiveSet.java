/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.collection.primitive;

import java.util.Collection;
import java.util.PrimitiveIterator;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

/**
 *
 * @author pp
 */
public interface PrimitiveSet<T, T_CONS, T_ITR extends PrimitiveIterator<T, T_CONS>, T_SPLITR extends Spliterator.OfPrimitive<T, T_CONS, T_SPLITR>, T_COLL>
		extends PrimitiveCollection<T, T_CONS, T_ITR, T_SPLITR, T_COLL>, Set<T> {

	@Override
	T_SPLITR spliterator();

	public static interface IntSet extends PrimitiveSet<Integer, IntConsumer, PrimitiveIterator.OfInt, Spliterator.OfInt, IntCollection>, IntCollection {

		@Override
		public default Spliterator.OfInt spliterator() {
			return Spliterators.spliteratorUnknownSize(iterator(), Spliterator.DISTINCT);
		}

		@Override
		default boolean contains(Object o) {
			return IntCollection.super.contains(o);
		}

		@Override
		default boolean add(Integer e) {
			return IntCollection.super.add(e);
		}

		@Override
		default boolean remove(Object o) {
			return IntCollection.super.remove(o);
		}

		@Override
		default boolean containsAll(Collection<?> c) {
			return IntCollection.super.containsAll(c);
		}
	}

	public static interface LongSet extends PrimitiveSet<Long, LongConsumer, PrimitiveIterator.OfLong, Spliterator.OfLong, LongCollection>, LongCollection {

		@Override
		public default Spliterator.OfLong spliterator() {
			return Spliterators.spliteratorUnknownSize(iterator(), Spliterator.DISTINCT);
		}

		@Override
		default boolean contains(Object o) {
			return LongCollection.super.contains(o);
		}

		@Override
		default boolean add(Long e) {
			return LongCollection.super.add(e);
		}

		@Override
		default boolean remove(Object o) {
			return LongCollection.super.remove(o);
		}

		@Override
		default boolean containsAll(Collection<?> c) {
			return LongCollection.super.containsAll(c);
		}
	}

	public static interface DoubleSet extends PrimitiveSet<Double, DoubleConsumer, PrimitiveIterator.OfDouble, Spliterator.OfDouble, DoubleCollection>, DoubleCollection {

		@Override
		public default Spliterator.OfDouble spliterator() {
			return Spliterators.spliteratorUnknownSize(iterator(), Spliterator.DISTINCT);
		}

		@Override
		default boolean contains(Object o) {
			return DoubleCollection.super.contains(o);
		}

		@Override
		default boolean add(Double e) {
			return DoubleCollection.super.add(e);
		}

		@Override
		default boolean remove(Object o) {
			return DoubleCollection.super.remove(o);
		}

		@Override
		default boolean containsAll(Collection<?> c) {
			return DoubleCollection.super.containsAll(c);
		}
	}
}

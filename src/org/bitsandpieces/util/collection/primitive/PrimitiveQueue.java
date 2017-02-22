/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.collection.primitive;

import java.util.PrimitiveIterator;
import java.util.Queue;
import java.util.Spliterator;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

/**
 *
 * @author Jan Kebernik
 */
public interface PrimitiveQueue<T, T_CONS, T_ITR extends PrimitiveIterator<T, T_CONS>, T_SPLITR extends Spliterator.OfPrimitive<T, T_CONS, T_SPLITR>, T_COLL>
		extends PrimitiveCollection<T, T_CONS, T_ITR, T_SPLITR, T_COLL>, Queue<T> {

	public static interface IntQueue extends PrimitiveQueue<Integer, IntConsumer, PrimitiveIterator.OfInt, Spliterator.OfInt, IntCollection>, IntCollection {

		@Override
		default boolean add(Integer e) {
			return IntCollection.super.add(e);
		}

		@Override
		default boolean offer(Integer e) {
			return offerInt(e);
		}

		boolean offerInt(int e);

		@Override
		default Integer remove() {
			return removeInt();
		}

		int removeInt();

		@Override
		default Integer poll() {
			return pollInt();
		}

		// return -1 if empty
		int pollInt();

		@Override
		default Integer element() {
			return elementInt();
		}

		int elementInt();

		@Override
		default Integer peek() {
			return peekInt();
		}

		int peekInt();
	}

	public static interface LongQueue extends PrimitiveQueue<Long, LongConsumer, PrimitiveIterator.OfLong, Spliterator.OfLong, LongCollection>, LongCollection {

		@Override
		default boolean add(Long e) {
			return LongCollection.super.add(e);
		}

		@Override
		default boolean offer(Long e) {
			return offerLong(e);
		}

		boolean offerLong(long e);

		@Override
		default Long remove() {
			return removeLong();
		}

		long removeLong();

		@Override
		default Long poll() {
			return pollLong();
		}

		long pollLong();

		@Override
		default Long element() {
			return elementLong();
		}

		long elementLong();

		@Override
		default Long peek() {
			return peekLong();
		}

		long peekLong();
	}

	public static interface DoubleQueue extends PrimitiveQueue<Double, DoubleConsumer, PrimitiveIterator.OfDouble, Spliterator.OfDouble, DoubleCollection>, DoubleCollection {

		@Override
		default boolean add(Double e) {
			return DoubleCollection.super.add(e);
		}

		@Override
		default boolean offer(Double e) {
			return offerDouble(e);
		}

		boolean offerDouble(double e);

		@Override
		default Double remove() {
			return removeDouble();
		}

		double removeDouble();

		@Override
		default Double poll() {
			return pollDouble();
		}

		double pollDouble();

		@Override
		default Double element() {
			return elementDouble();
		}

		double elementDouble();

		@Override
		default Double peek() {
			return peekDouble();
		}

		double peekDouble();
	}
}

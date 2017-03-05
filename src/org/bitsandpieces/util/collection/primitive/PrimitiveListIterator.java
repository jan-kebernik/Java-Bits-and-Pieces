/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.collection.primitive;

import java.util.ListIterator;
import java.util.PrimitiveIterator;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

/**
 *
 * @author pp
 */
public interface PrimitiveListIterator<T, T_CONS> extends ListIterator<T>, PrimitiveIterator<T, T_CONS> {

	public static interface IntListIterator extends PrimitiveListIterator<Integer, IntConsumer>, PrimitiveIterator.OfInt {

		@Override
		default Integer next() {
			return PrimitiveIterator.OfInt.super.next();
		}

		@Override
		default Integer previous() {
			return previousInt();
		}

		int previousInt();

		@Override
		default void set(Integer e) {
			setInt(e);
		}

		void setInt(int e);

		@Override
		default void add(Integer e) {
			addInt(e);
		}

		void addInt(int e);
	}

	public static interface LongListIterator extends PrimitiveListIterator<Long, LongConsumer>, PrimitiveIterator.OfLong {

		@Override
		default Long next() {
			return PrimitiveIterator.OfLong.super.next();
		}

		@Override
		default Long previous() {
			return previousLong();
		}

		long previousLong();

		@Override
		default void set(Long e) {
			setLong(e);
		}

		void setLong(long e);

		@Override
		default void add(Long e) {
			addLong(e);
		}

		void addLong(long e);
	}

	public static interface DoubleListIterator extends PrimitiveListIterator<Double, DoubleConsumer>, PrimitiveIterator.OfDouble {

		@Override
		default Double next() {
			return PrimitiveIterator.OfDouble.super.next();
		}

		@Override
		default Double previous() {
			return previousDouble();
		}

		double previousDouble();

		@Override
		default void set(Double e) {
			setDouble(e);
		}

		void setDouble(double e);

		@Override
		default void add(Double e) {
			addDouble(e);
		}

		void addDouble(double e);
	}
}

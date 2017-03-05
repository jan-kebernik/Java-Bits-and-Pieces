/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.collection.primitive;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleUnaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntUnaryOperator;
import java.util.function.LongConsumer;
import java.util.function.LongUnaryOperator;
import java.util.function.UnaryOperator;
import org.bitsandpieces.util.ArrayUtil;
import org.bitsandpieces.util.collection.primitive.PrimitiveComparator.DoubleComparator;
import org.bitsandpieces.util.collection.primitive.PrimitiveComparator.IntComparator;
import org.bitsandpieces.util.collection.primitive.PrimitiveComparator.LongComparator;
import org.bitsandpieces.util.collection.primitive.PrimitiveListIterator.DoubleListIterator;
import org.bitsandpieces.util.collection.primitive.PrimitiveListIterator.IntListIterator;
import org.bitsandpieces.util.collection.primitive.PrimitiveListIterator.LongListIterator;

/**
 *
 * @author Jan Kebernik
 */
public interface PrimitiveList<T, T_CONS, T_ITR extends PrimitiveIterator<T, T_CONS>, T_SPLITR extends Spliterator.OfPrimitive<T, T_CONS, T_SPLITR>, T_COLL, T_LITR extends PrimitiveListIterator<T, T_CONS>, T_LIST extends PrimitiveList<T, T_CONS, T_ITR, T_SPLITR, T_COLL, T_LITR, T_LIST>>
		extends PrimitiveCollection<T, T_CONS, T_ITR, T_SPLITR, T_COLL>, List<T> {

	@Override
	T_SPLITR spliterator();

	@Override
	T_LITR listIterator();

	@Override
	T_LITR listIterator(int index);

	@Override
	T_LIST subList(int fromIndex, int toIndex);

	public boolean addAll(int index, T_COLL c);

	public static interface IntList
			extends PrimitiveList<Integer, IntConsumer, PrimitiveIterator.OfInt, Spliterator.OfInt, IntCollection, IntListIterator, IntList>, IntCollection {

		@Override
		public default Spliterator.OfInt spliterator() {
			return Spliterators.spliterator(iterator(), size(), Spliterator.ORDERED);
		}

		@Override
		default boolean remove(Object o) {
			return IntCollection.super.remove(o);
		}

		@Override
		default Integer remove(int index) {
			return removeIntAt(index);
		}

		int removeIntAt(int index);

		@Override
		default boolean add(Integer e) {
			return IntCollection.super.add(e);
		}

		@Override
		default void add(int index, Integer element) {
			addInt(index, element);
		}

		void addInt(int index, int element);

		@Override
		default boolean contains(Object o) {
			return IntCollection.super.contains(o);
		}

		@Override
		default void replaceAll(UnaryOperator<Integer> operator) {
			Objects.requireNonNull(operator);
			replaceAll((IntUnaryOperator) operator::apply);
		}

		default void replaceAll(IntUnaryOperator operator) {
			Objects.requireNonNull(operator);
			final IntListIterator it = this.listIterator();
			while (it.hasNext()) {
				it.setInt(operator.applyAsInt(it.nextIndex()));
			}
		}

		@Override
		default void sort(Comparator<? super Integer> c) {
			if (c instanceof IntComparator || c == null) {
				sort((IntComparator) c);
				return;
			}
			sort((IntComparator) c::compare);
		}

		default void sort(IntComparator c) {
			int[] a = ArrayUtil.sort(toIntArray(), c);
			IntListIterator it = listIterator();
			for (int i : a) {
				it.nextInt();
				it.setInt(i);
			}
		}

		@Override
		default int indexOf(Object o) {
			if (o instanceof Integer) {
				return indexOfInt((int) o);
			}
			return -1;
		}

		int indexOfInt(int o);

		@Override
		default int lastIndexOf(Object o) {
			if (o instanceof Integer) {
				return lastIndexOfInt((int) o);
			}
			return -1;
		}

		int lastIndexOfInt(int o);

		@Override
		default Integer set(int index, Integer element) {
			return setInt(index, element);
		}

		int setInt(int index, int element);

		@Override
		default Integer get(int index) {
			return getInt(index);
		}

		int getInt(int index);

		@Override
		public default boolean containsAll(Collection<?> c) {
			return IntCollection.super.containsAll(c);
		}
	}

	public static interface LongList
			extends PrimitiveList<Long, LongConsumer, PrimitiveIterator.OfLong, Spliterator.OfLong, LongCollection, LongListIterator, LongList>, LongCollection {

		@Override
		public default Spliterator.OfLong spliterator() {
			return Spliterators.spliterator(iterator(), size(), Spliterator.ORDERED);
		}

		@Override
		default boolean remove(Object o) {
			return LongCollection.super.remove(o);
		}

		@Override
		default Long remove(int index) {
			return removeLongAt(index);
		}

		long removeLongAt(int index);

		@Override
		default boolean add(Long e) {
			return LongCollection.super.add(e);
		}

		@Override
		default void add(int index, Long element) {
			addLong(index, element);
		}

		void addLong(int index, long element);

		@Override
		default boolean contains(Object o) {
			return LongCollection.super.contains(o);
		}

		@Override
		default void replaceAll(UnaryOperator<Long> operator) {
			Objects.requireNonNull(operator);
			replaceAll((LongUnaryOperator) operator::apply);
		}

		default void replaceAll(LongUnaryOperator operator) {
			Objects.requireNonNull(operator);
			final LongListIterator it = this.listIterator();
			while (it.hasNext()) {
				it.setLong(operator.applyAsLong(it.nextLong()));
			}
		}

		@Override
		default void sort(Comparator<? super Long> c) {
			if (c instanceof LongComparator || c == null) {
				sort((LongComparator) c);
				return;
			}
			sort((LongComparator) c::compare);
		}

		default void sort(LongComparator c) {
			long[] a = ArrayUtil.sort(toLongArray(), c);
			LongListIterator it = listIterator();
			for (long i : a) {
				it.nextLong();
				it.setLong(i);
			}
		}

		@Override
		default int indexOf(Object o) {
			if (o instanceof Long) {
				return indexOfLong((long) o);
			}
			return -1;
		}

		int indexOfLong(long o);

		@Override
		default int lastIndexOf(Object o) {
			if (o instanceof Long) {
				return lastIndexOfLong((long) o);
			}
			return -1;
		}

		int lastIndexOfLong(long o);

		@Override
		default Long set(int index, Long element) {
			return setLong(index, element);
		}

		long setLong(int index, long element);

		@Override
		default Long get(int index) {
			return getLong(index);
		}

		long getLong(int index);

		@Override
		public default boolean containsAll(Collection<?> c) {
			return LongCollection.super.containsAll(c);
		}
	}

	public static interface DoubleList
			extends PrimitiveList<Double, DoubleConsumer, PrimitiveIterator.OfDouble, Spliterator.OfDouble, DoubleCollection, DoubleListIterator, DoubleList>, DoubleCollection {

		@Override
		public default Spliterator.OfDouble spliterator() {
			return Spliterators.spliterator(iterator(), size(), Spliterator.ORDERED);
		}

		@Override
		default boolean remove(Object o) {
			return DoubleCollection.super.remove(o);
		}

		@Override
		default Double remove(int index) {
			return removeDoubleAt(index);
		}

		double removeDoubleAt(int index);

		@Override
		default boolean add(Double e) {
			return DoubleCollection.super.add(e);
		}

		@Override
		default void add(int index, Double element) {
			addDouble(index, element);
		}

		void addDouble(int index, double element);

		@Override
		default boolean contains(Object o) {
			return DoubleCollection.super.contains(o);
		}

		@Override
		default void replaceAll(UnaryOperator<Double> operator) {
			Objects.requireNonNull(operator);
			replaceAll((DoubleUnaryOperator) operator::apply);
		}

		default void replaceAll(DoubleUnaryOperator operator) {
			Objects.requireNonNull(operator);
			final DoubleListIterator it = this.listIterator();
			while (it.hasNext()) {
				it.setDouble(operator.applyAsDouble(it.nextDouble()));
			}
		}

		@Override
		default void sort(Comparator<? super Double> c) {
			if (c instanceof DoubleComparator || c == null) {
				sort((DoubleComparator) c);
				return;
			}
			sort((DoubleComparator) c::compare);
		}

		default void sort(DoubleComparator c) {
			double[] a = ArrayUtil.sort(toDoubleArray(), c);
			DoubleListIterator it = listIterator();
			for (double i : a) {
				it.nextDouble();
				it.setDouble(i);
			}
		}

		@Override
		default int indexOf(Object o) {
			if (o instanceof Double) {
				return indexOfDouble((double) o);
			}
			return -1;
		}

		int indexOfDouble(double o);

		@Override
		default int lastIndexOf(Object o) {
			if (o instanceof Double) {
				return lastIndexOfDouble((double) o);
			}
			return -1;
		}

		int lastIndexOfDouble(double o);

		@Override
		default Double set(int index, Double element) {
			return setDouble(index, element);
		}

		double setDouble(int index, double element);

		@Override
		default Double get(int index) {
			return getDouble(index);
		}

		double getDouble(int index);

		@Override
		public default boolean containsAll(Collection<?> c) {
			return DoubleCollection.super.containsAll(c);
		}
	}
}

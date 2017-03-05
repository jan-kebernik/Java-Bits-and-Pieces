/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.collection.primitive;

import java.util.Collection;
import java.util.Objects;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.function.DoubleConsumer;
import java.util.function.DoublePredicate;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import java.util.function.LongConsumer;
import java.util.function.LongPredicate;
import java.util.function.Predicate;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 *
 * @author Jan Kebernik
 */
public interface PrimitiveCollection<T, T_CONS, T_ITR extends PrimitiveIterator<T, T_CONS>, T_SPLITR extends Spliterator.OfPrimitive<T, T_CONS, T_SPLITR>, T_COLL>
		extends PrimitiveIterable<T, T_CONS, T_ITR, T_SPLITR>, Collection<T> {

	@Override
	T_SPLITR spliterator();

	public boolean addAll(T_COLL c);

	public boolean containsAll(T_COLL c);

	public boolean removeAll(T_COLL c);

	public boolean retainAll(T_COLL c);

	public static interface IntCollection extends PrimitiveCollection<Integer, IntConsumer, PrimitiveIterator.OfInt, Spliterator.OfInt, IntCollection>,
			IntIterable {

		@Override
		public default Spliterator.OfInt spliterator() {
			return IntIterable.super.spliterator();
		}

		@Override
		public default Stream<Integer> stream() {
			return StreamSupport.stream(spliterator(), false);
		}

		public default IntStream intStream() {
			return StreamSupport.intStream(spliterator(), false);
		}

		@Override
		public default Stream<Integer> parallelStream() {
			return StreamSupport.stream(spliterator(), true);
		}

		public default IntStream parallelIntStream() {
			return StreamSupport.intStream(spliterator(), true);
		}

		public int[] toIntArray();

		public int[] toIntArray(int[] a);

		@Override
		default boolean removeIf(Predicate<? super Integer> filter) {
			Objects.requireNonNull(filter);
			return removeIf((IntPredicate) filter::test);
		}

		default boolean removeIf(IntPredicate filter) {
			Objects.requireNonNull(filter);
			boolean removed = false;
			final PrimitiveIterator.OfInt each = iterator();
			while (each.hasNext()) {
				if (filter.test(each.nextInt())) {
					each.remove();
					removed = true;
				}
			}
			return removed;
		}

		@Override
		default boolean contains(Object o) {
			if (o instanceof Integer) {
				return containsInt((int) o);
			}
			return false;
		}

		boolean containsInt(int o);

		@Override
		default boolean add(Integer e) {
			return addInt(e);
		}

		public boolean addInt(int e);

		@Override
		default boolean remove(Object o) {
			if (o instanceof Integer) {
				return removeInt((int) o);
			}
			return false;
		}

		boolean removeInt(int o);

		@Override
		default boolean containsAll(Collection<?> c) {
			if (c instanceof IntCollection) {
				return containsAll((IntCollection) c);
			}
			return c.stream().allMatch(this::contains);
		}

		@Override
		default boolean containsAll(IntCollection c) {
			return c.intStream().allMatch(this::containsInt);
		}
	}

	public static interface LongCollection extends PrimitiveCollection<Long, LongConsumer, PrimitiveIterator.OfLong, Spliterator.OfLong, LongCollection>,
			LongIterable {

		@Override
		public default Spliterator.OfLong spliterator() {
			return LongIterable.super.spliterator();
		}

		@Override
		public default Stream<Long> stream() {
			return StreamSupport.stream(spliterator(), false);
		}

		public default LongStream longStream() {
			return StreamSupport.longStream(spliterator(), false);
		}

		@Override
		public default Stream<Long> parallelStream() {
			return StreamSupport.stream(spliterator(), true);
		}

		public default LongStream parallelLongStream() {
			return StreamSupport.longStream(spliterator(), true);
		}

		public long[] toLongArray();

		public long[] toLongArray(long[] a);

		@Override
		default boolean removeIf(Predicate<? super Long> filter) {
			Objects.requireNonNull(filter);
			return removeIf((LongPredicate) filter::test);
		}

		default boolean removeIf(LongPredicate filter) {
			Objects.requireNonNull(filter);
			boolean removed = false;
			final PrimitiveIterator.OfLong each = iterator();
			while (each.hasNext()) {
				if (filter.test(each.nextLong())) {
					each.remove();
					removed = true;
				}
			}
			return removed;
		}

		@Override
		default boolean contains(Object o) {
			if (o instanceof Long) {
				return containsLong((long) o);
			}
			return false;
		}

		boolean containsLong(long o);

		@Override
		default boolean add(Long e) {
			return addLong(e);
		}

		public boolean addLong(long e);

		@Override
		default boolean remove(Object o) {
			if (o instanceof Long) {
				return removeLong((long) o);
			}
			return false;
		}

		boolean removeLong(long o);

		@Override
		default boolean containsAll(Collection<?> c) {
			if (c instanceof LongCollection) {
				return containsAll((LongCollection) c);
			}
			return c.stream().allMatch(this::contains);
		}

		@Override
		default boolean containsAll(LongCollection c) {
			return c.longStream().allMatch(this::containsLong);
		}
	}

	public static interface DoubleCollection extends PrimitiveCollection<Double, DoubleConsumer, PrimitiveIterator.OfDouble, Spliterator.OfDouble, DoubleCollection>,
			DoubleIterable {

		@Override
		public default Spliterator.OfDouble spliterator() {
			return DoubleIterable.super.spliterator();
		}

		@Override
		public default Stream<Double> stream() {
			return StreamSupport.stream(spliterator(), false);
		}

		public default DoubleStream doubleStream() {
			return StreamSupport.doubleStream(spliterator(), false);
		}

		@Override
		public default Stream<Double> parallelStream() {
			return StreamSupport.stream(spliterator(), true);
		}

		public default DoubleStream parallelDoubleStream() {
			return StreamSupport.doubleStream(spliterator(), true);
		}

		public double[] toDoubleArray();

		public double[] toDoubleArray(double[] a);

		@Override
		default boolean removeIf(Predicate<? super Double> filter) {
			Objects.requireNonNull(filter);
			return removeIf((DoublePredicate) filter::test);
		}

		default boolean removeIf(DoublePredicate filter) {
			Objects.requireNonNull(filter);
			boolean removed = false;
			final PrimitiveIterator.OfDouble each = iterator();
			while (each.hasNext()) {
				if (filter.test(each.nextDouble())) {
					each.remove();
					removed = true;
				}
			}
			return removed;
		}

		@Override
		default boolean contains(Object o) {
			if (o instanceof Double) {
				return containsDouble((double) o);
			}
			return false;
		}

		boolean containsDouble(double o);

		@Override
		default boolean add(Double e) {
			return addDouble(e);
		}

		public boolean addDouble(double e);

		@Override
		default boolean remove(Object o) {
			if (o instanceof Double) {
				return removeDouble((double) o);
			}
			return false;
		}

		boolean removeDouble(double o);

		@Override
		default boolean containsAll(Collection<?> c) {
			if (c instanceof DoubleCollection) {
				return containsAll((DoubleCollection) c);
			}
			return c.stream().allMatch(this::contains);
		}

		@Override
		default boolean containsAll(DoubleCollection c) {
			return c.doubleStream().allMatch(this::containsDouble);
		}
	}
	
}

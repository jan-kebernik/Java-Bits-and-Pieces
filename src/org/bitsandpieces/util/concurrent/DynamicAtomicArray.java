/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.concurrent;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * Atomic array of variable size.
 *
 * @param <E> the type of the objects stored in this array
 *
 * @author Jan Kebernik
 */
public class DynamicAtomicArray<E> implements Iterable<E> {

	private final AtomicReference<AtomicReference<E>[]> ref;

	public DynamicAtomicArray(int initialCapacity) {
		AtomicReference<E>[] a = newArray(initialCapacity);
		for (int i = 0; i < initialCapacity; i++) {
			a[i] = new AtomicReference<>();
		}
		this.ref = new AtomicReference<>(a);
	}

	private static <E> AtomicReference<E>[] newArray(int length) {
		return (AtomicReference<E>[]) new AtomicReference<?>[length];
	}

	private static void checkIndex(int index) {
		if (index < 0) {
			throw new IndexOutOfBoundsException("index must be non-negative: " + index);
		}
	}

	public E get(int index) {
		checkIndex(index);
		AtomicReference<E>[] a = this.ref.get();
		return index < a.length ? a[index].get() : null;
	}

	public void set(int index, E element) {
		checkIndex(index);
		AtomicReference<E>[] a = this.ref.get();
		while (true) {
			if (index < a.length) {
				a[index].set(element);
				return;
			}
			if (element == null) {
				return;
			}
			// only one thread may resize
			a = tryGrow(index, a);
		}
	}

	public boolean compareAndSet(int index, E expect, E update) {
		checkIndex(index);
		AtomicReference<E>[] a = this.ref.get();
		if (expect == null) {
			while (true) {
				if (index < a.length) {
					return a[index].compareAndSet(null, update);
				}
				if (update == null) {
					// as expected
					return true;
				}
				a = tryGrow(index, a);
			}
		}
		if (index < a.length) {
			return a[index].compareAndSet(expect, update);
		}
		// expected non-null
		return false;
	}

	public E getAndSet(int index, E element) {
		checkIndex(index);
		AtomicReference<E>[] a = this.ref.get();
		if (element == null) {
			if (index < a.length) {
				return a[index].getAndSet(element);
			}
			return null;
		}
		while (true) {
			if (index < a.length) {
				return a[index].getAndSet(element);
			}
			// only one thread may resize
			a = tryGrow(index, a);
		}
	}

	public E getAndUpdate(int index, UnaryOperator<E> updateFunction) {
		checkIndex(index);
		AtomicReference<E>[] a = this.ref.get();
		while (true) {
			if (index < a.length) {
				return a[index].getAndUpdate(updateFunction);
			}
			E next = updateFunction.apply(null);
			if (next == null) {
				return null;
			}
			a = tryGrow(index, a);
		}
	}

	public E updateAndGet(int index, UnaryOperator<E> updateFunction) {
		checkIndex(index);
		AtomicReference<E>[] a = this.ref.get();
		while (true) {
			if (index < a.length) {
				return a[index].updateAndGet(updateFunction);
			}
			E next = updateFunction.apply(null);
			if (next == null) {
				return null;
			}
			a = tryGrow(index, a);
		}
	}

	public final E getAndAccumulate(int index, E updateValue, BinaryOperator<E> accumulatorFunction) {
		checkIndex(index);
		AtomicReference<E>[] a = this.ref.get();
		while (true) {
			if (index < a.length) {
				return a[index].getAndAccumulate(updateValue, accumulatorFunction);
			}
			E next = accumulatorFunction.apply(null, updateValue);
			if (next == null) {
				return null;
			}
			a = tryGrow(index, a);
		}
	}

	public final E accumulateAndGet(int index, E updateValue, BinaryOperator<E> accumulatorFunction) {
		checkIndex(index);
		AtomicReference<E>[] a = this.ref.get();
		while (true) {
			if (index < a.length) {
				return a[index].accumulateAndGet(updateValue, accumulatorFunction);
			}
			E next = accumulatorFunction.apply(null, updateValue);
			if (next == null) {
				return null;
			}
			a = tryGrow(index, a);
		}
	}

	// must synchronize in order to avoid a really horrible worst case scenario,
	// where every thread losing a race would do way too much work, take longer
	// and thus increase the chances of losing races more often, etc.
	private synchronized AtomicReference<E>[] tryGrow(int index, AtomicReference<E>[] a) {
		{
			AtomicReference<E>[] b = this.ref.get();
			if (b != a) {
				// was updated by another thread. retry
				return b;
			}
		}
		// won thread race
		int minCap = index + 1;
		if (minCap < 0) {
			throw new OutOfMemoryError();
		}
		AtomicReference<E>[] b = newArray(Math.max(minCap, a.length + (a.length >>> 1) + 2));
		System.arraycopy(a, 0, b, 0, a.length);
		for (int i = a.length; i < b.length; i++) {
			b[i] = new AtomicReference<>();
		}
		this.ref.set(b);
		return b;
	}

	@Override
	public void forEach(Consumer<? super E> action) {
		AtomicReference<E>[] a = this.ref.get();
		for (int i = 0;;) {
			for (; i < a.length; i++) {
				action.accept(a[i].get());
			}
			AtomicReference<E>[] b = this.ref.get();
			if (i >= (a = b).length) {
				return;
			}
		}
	}

	@Override
	public String toString() {
		AtomicReference<E>[] a = this.ref.get();
		if (a.length == 0) {
			return "[]";
		}
		StringBuilder sb = new StringBuilder();
		sb.append('[');
		for (int i = 0;;) {
			for (; i < a.length; i++) {
				sb.append(a[i].get()).append(", ");
			}
			AtomicReference<E>[] b = this.ref.get();
			if (i >= (a = b).length) {
				sb.deleteCharAt(sb.length() - 1).setCharAt(sb.length() - 1, ']');
				return sb.toString();
			}
		}
	}

	// current capacity
	public int size() {
		return this.ref.get().length;
	}

	// adapts to array growth during or inbetween iteration.
	// still, results must always be considered out-of-date immediately.
	@Override
	public Iterator<E> iterator() {
		return new Iterator<E>() {

			private AtomicReference<E>[] arr = DynamicAtomicArray.this.ref.get();
			private int index = 0;
			private int ret = -1;

			@Override
			public boolean hasNext() {
				int i = this.index;
				AtomicReference<E>[] a = this.arr;
				// try updating array if end reached
				return i < a.length || i < (this.arr = DynamicAtomicArray.this.ref.get()).length;
			}

			// basically binds to state of hasNext()
			@Override
			public E next() {
				int i = this.index;
				AtomicReference<E>[] a = this.arr;
				if (i >= a.length) {
					throw new NoSuchElementException();
				}
				this.index = i + 1;
				return a[this.ret = i].get();
			}

			@Override
			public void forEachRemaining(Consumer<? super E> action) {
				AtomicReference<E>[] a = this.arr;
				for (int i = this.index;;) {
					for (; i < a.length; i++) {
						action.accept(a[i].get());
					}
					AtomicReference<E>[] b = DynamicAtomicArray.this.ref.get();
					if (i >= (a = b).length) {
						this.arr = a;
						this.ret = i - 1;
						return;
					}
				}
			}

			@Override
			public void remove() {
				// TODO add CAS logic based on snapshot of last next(). Maybe.
				// or disable entirely.
				int r = this.ret;
				if (r < 0) {
					throw new NoSuchElementException();
				}
				AtomicReference<E>[] a = this.arr;
				if (r < a.length) {
					a[r].set(null);
				}
				this.ret = -1;
			}
		};
	}
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.collection;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.PrimitiveIterator;
import java.util.Queue;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import org.bitsandpieces.util.collection.primitive.PrimitiveQueue.IntQueue;

/**
 *
 * @author Jan Kebernik
 */
public class IntStack implements IntQueue {

	private static final int[] EMPTY = {};

	private int index = 0;
	private int[] array = EMPTY;

	@Override
	public boolean addInt(int element) {
		int i = this.index;
		int minCap = i + 1;
		int[] a = this.array;
		if (i == a.length) {
			if (minCap < 0) {
				throw new OutOfMemoryError();
			}
			int[] b = new int[Math.max(minCap, a.length + (a.length >>> 1))];
			System.arraycopy(a, 0, b, 0, a.length);
			a = (this.array = b);
		}
		a[i] = element;
		this.index = minCap;
		return true;
	}

	@Override
	public boolean offerInt(int e) {
		return addInt(e);
	}

	@Override
	public int pollInt() {
		int i = this.index;
		if (i == 0) {
			return -1;
		}
		int[] a = this.array;
		return a[(this.index = i - 1)];
	}

	@Override
	public int removeInt() {
		int i = this.index;
		if (i == 0) {
			throw new NoSuchElementException();
		}
		int[] a = this.array;
		return a[(this.index = i - 1)];
	}

	@Override
	public int peekInt() {
		int i = this.index;
		if (i == 0) {
			return -1;
		}
		int[] a = this.array;
		return a[i - 1];
	}

	@Override
	public int elementInt() {
		int i = this.index;
		if (i == 0) {
			throw new NoSuchElementException();
		}
		int[] a = this.array;
		return a[i - 1];
	}

	@Override
	public void clear() {
		this.index = 0;
	}

	@Override
	public boolean isEmpty() {
		return this.index == 0;
	}

	@Override
	public int size() {
		return this.index;
	}

	@Override
	public void forEach(IntConsumer action) {
		Objects.requireNonNull(action);
		int i = this.index;
		int[] a = this.array;
		for (int j = 0; j < i; j++) {
			action.accept(a[j]);
		}
	}

	@Override
	public String toString() {
		int i = this.index;
		if (i == 0) {
			return "[]";
		}
		StringBuilder sb = new StringBuilder();
		sb.append('[');
		int[] a = this.array;
		for (int j = 0; j < i - 1; j++) {
			sb.append(a[j]).append(", ");
		}
		sb.append(a[i - 1]).append(']');
		return sb.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Queue)) {
			return false;
		}
		Queue<?> q = (Queue<?>) o;
		int i = this.index;
		int s = q.size();
		if (i != s) {
			return false;
		}
		int[] a = this.array;
		if (q instanceof IntQueue) {
			PrimitiveIterator.OfInt it = ((IntQueue) q).iterator();
			for (int j = 0; j < i; j++) {
				if (a[j] != it.nextInt()) {
					return false;
				}
			}
			return !it.hasNext();
		}

		Iterator<?> it = q.iterator();
		for (int j = 0; j < i; j++) {
			Object k = it.next();
			if (!(k instanceof Integer) || a[j] != (int) k) {
				return false;
			}
		}
		return !it.hasNext();
	}

	@Override
	public int hashCode() {
		int i = this.index;
		int[] a = this.array;
		int h = 1;
		for (int j = 0; j < i; j++) {
			h = h * 31 + a[j];
		}
		return h;
	}

	@Override
	public boolean containsInt(int e) {
		int i = this.index;
		int[] a = this.array;
		for (int j = 0; j < i; j++) {
			if (a[j] == e) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean retainAll(IntCollection c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeAll(IntCollection c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeIf(IntPredicate filter) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeInt(int o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addAll(IntCollection c) {
		if (c.isEmpty()) {
			return false;
		}
		int s = c.size();
		int i = this.index;
		int minCap = i + s;
		if (minCap < 0) {
			throw new OutOfMemoryError();
		}
		int[] a = this.array;
		if (minCap > a.length) {
			int[] b = new int[Math.max(minCap, a.length + (a.length >>> 1))];
			System.arraycopy(a, 0, b, 0, a.length);
			a = (this.array = b);
		}
		PrimitiveIterator.OfInt it = c.iterator();
		for (int j = i; j < minCap; j++) {
			a[i] = it.nextInt();
		}
		this.index = minCap;
		return true;
	}

	@Override
	public boolean addAll(Collection<? extends Integer> c) {
		if (c instanceof IntCollection) {
			return addAll((IntCollection) c);
		}
		if (c.isEmpty()) {
			return false;
		}
		int s = c.size();
		int i = this.index;
		int minCap = i + s;
		if (minCap < 0) {
			throw new OutOfMemoryError();
		}
		int[] a = this.array;
		if (minCap > a.length) {
			int[] b = new int[Math.max(minCap, a.length + (a.length >>> 1))];
			System.arraycopy(a, 0, b, 0, a.length);
			a = (this.array = b);
		}
		Iterator<? extends Integer> it = c.iterator();
		for (int j = i; j < minCap; j++) {
			a[i] = it.next();
		}
		this.index = minCap;
		return true;
	}

	@Override
	public Object[] toArray() {
		int i = this.index;
		int[] a = this.array;
		Object[] b = new Object[i];
		for (int j = 0; j < i; j++) {
			b[j] = a[j];
		}
		return b;
	}

	@Override
	public <T> T[] toArray(T[] a) {
		int i = this.index;
		if (a.length < i) {
			Class<?> clazz = a.getClass();
			a = (T[]) ((Object) clazz == (Object) Object[].class
					? new Object[i]
					: Array.newInstance(clazz.getComponentType(), i));
		}
		int[] x = this.array;
		for (int j = 0; j < i; j++) {
			Object xxx = x[j];
			a[j] = (T) xxx;
		}
		return a;
	}

	@Override
	public int[] toIntArray() {
		int i = this.index;
		int[] a = this.array;
		int[] b = new int[i];
		System.arraycopy(a, 0, b, 0, i);
		return b;
	}

	@Override
	public int[] toIntArray(int[] a) {
		int i = this.index;
		int[] x = this.array;
		if (a.length < i) {
			a = new int[i];
		}
		System.arraycopy(x, 0, a, 0, i);
		return a;
	}

	@Override
	public PrimitiveIterator.OfInt iterator() {
		return new PrimitiveIterator.OfInt() {

			int idx = 0;

			@Override
			public boolean hasNext() {
				return this.idx < IntStack.this.index;
			}

			@Override
			public int nextInt() {
				int i = this.idx;
				int m = IntStack.this.index;
				if (i >= m) {
					throw new IllegalStateException();
				}
				int[] a = IntStack.this.array;
				return a[this.idx = i + 1];
			}

			@Override
			public void forEachRemaining(IntConsumer action) {
				int i = this.idx;
				int m = IntStack.this.index;
				if (i >= m) {
					return;
				}
				for (int[] a = IntStack.this.array; i < m; i++) {
					action.accept(a[i]);
				}
				this.idx = m;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}
}

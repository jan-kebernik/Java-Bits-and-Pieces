/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.collection.view;

import java.util.Arrays;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 *
 * @author pp
 */
@SuppressWarnings("EqualsAndHashcode")
class ArrayListBase<E> implements Iterable<E> {

	protected final E[] array;
	protected final int start, end;

	ArrayListBase(E[] array, int start, int end) {
		this.array = array;
		this.start = start;
		this.end = end;
	}

	protected final boolean arrayEquals(Object obj) {
		ArrayListBase<?> other = (ArrayListBase<?>) obj;
		if (this.size() != other.size()) {
			return false;
		}
		if (this.array == other.array && this.start == other.start) {
			// identical array range
			return true;
		}
		for (int i = this.start, j = other.start; i < this.end; i++, j++) {
			if (!Objects.equals(this.array[i], other.array[j])) {
				return false;
			}
		}
		return true;
	}

	protected final boolean iterEquals(Iterator<?> it) {
		for (int i = this.start; i < this.end; i++) {
			if (!it.hasNext() || !Objects.equals(this.array[i], it.next())) {
				return false;
			}
		}
		return !it.hasNext();
	}

	@Override
	public final int hashCode() {
		// don't simply cache value, because underlying data-structure
		// may alter array contents without our knowledge
		int result = 1;
		for (int i = this.start; i < this.end; i++) {
			E e = this.array[i];
			result = 31 * result + (e == null ? 0 : e.hashCode());
		}
		return result;
	}

	@Override
	public final String toString() {
		if (this.start == this.end) {
			return "[]";
		}
		StringBuilder sb = new StringBuilder("[");
		sb.append(this.array[this.start]);
		for (int i = this.start + 1; i < this.end; i++) {
			sb.append(", ").append(this.array[i]);
		}
		sb.append("]");
		return sb.toString();
	}

	@Override
	public final void forEach(Consumer<? super E> action) {
		Objects.requireNonNull(action);
		for (int i = this.start; i < this.end; i++) {
			action.accept(this.array[i]);
		}
	}

	@Override
	public final Spliterator<E> spliterator() {
		// will be marked as immutable. not sure if really OK.
		return Arrays.spliterator(this.array, this.start, this.end);
	}

	public final E get(int index) {
		if (index < 0 || index > size()) {
			throw new IndexOutOfBoundsException();
		}
		return this.array[this.start + index];
	}

	public final int indexOf(Object o) {
		if (o == null) {
			for (int i = this.start; i < this.end; i++) {
				if (this.array[i] == null) {
					return i - this.start;
				}
			}
		} else {
			for (int i = this.start; i < this.end; i++) {
				if (o.equals(this.array[i])) {
					return i - this.start;
				}
			}
		}
		return -1;
	}

	public final int lastIndexOf(Object o) {
		if (o == null) {
			for (int i = this.end - 1; i >= this.start; i--) {
				if (this.array[i] == null) {
					return i - this.start;
				}
			}
		} else {
			for (int i = this.end - 1; i >= this.start; i--) {
				if (o.equals(this.array[i])) {
					return i - this.start;
				}
			}
		}
		return -1;
	}

	public final <T> T[] toArray(T[] a) {
		int s = this.size();
		if (s > a.length) {
			return Arrays.copyOfRange(this.array, this.start, this.end, (Class<? extends T[]>) a.getClass());
		}
		System.arraycopy(this.array, this.start, a, 0, s);
		if (s < a.length) {
			a[s] = null;
		}
		return a;
	}

	public final Object[] toArray() {
		return Arrays.copyOfRange(this.array, this.start, this.end);
	}

	public final boolean contains(Object o) {
		if (o == null) {
			for (int i = this.start; i < this.end; i++) {
				if (this.array[i] == null) {
					return true;
				}
			}
		} else {
			for (int i = this.start; i < this.end; i++) {
				if (o.equals(this.array[i])) {
					return true;
				}
			}
		}
		return false;
	}

	public final int size() {
		return this.end - this.start;
	}

	public final boolean isEmpty() {
		return this.start == this.end;
	}

	@Override
	public final Iterator<E> iterator() {
		return new ArrayIterator<>(this.array, this.start, this.end);
	}

	public final ListIterator<E> listIterator() {
		return new ArrayIterator(this.array, this.start, this.end);
	}

	public final ListIterator<E> listIterator(int index) {
		if (index < 0 || index > size()) {
			throw new IndexOutOfBoundsException();
		}
		return new ArrayIterator(this.array, this.start, this.end, this.start + index);
	}

	public final Stream<E> stream() {
		return StreamSupport.stream(spliterator(), false);
	}

	public final Stream<E> parallelStream() {
		return StreamSupport.stream(spliterator(), true);
	}

	static final class ArrayIterator<E> implements ListIterator<E> {

		private final E[] array;
		private final int end, start;
		private int off;

		ArrayIterator(E[] array, int start, int end) {
			this(array, start, end, start);
		}

		ArrayIterator(E[] array, int start, int end, int off) {
			this.array = array;
			this.start = start;
			this.end = end;
			this.off = off;
		}

		@Override
		public boolean hasNext() {
			return this.off < this.end;
		}

		@Override
		public E next() {
			int i = this.off;
			if (i >= this.end) {
				throw new NoSuchElementException();
			}
			this.off = i + 1;
			return this.array[i];
		}

		@Override
		public boolean hasPrevious() {
			return this.off > this.start;
		}

		@Override
		public E previous() {
			int i = this.off;
			if (i <= this.start) {
				throw new NoSuchElementException();
			}
			return this.array[this.off = i - 1];
		}

		@Override
		public int nextIndex() {
			return this.off - this.start;
		}

		@Override
		public int previousIndex() {
			return this.off - this.start - 1;
		}

		@Override
		public void forEachRemaining(Consumer<? super E> action) {
			Objects.requireNonNull(action);
			int i = this.off;
			try {
				for (; i < this.end; i++) {
					action.accept(this.array[i]);
				}
			} finally {
				this.off = i;
			}
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException(Views.ERROR_MESSAGE);
		}

		@Override
		public void set(E e) {
			throw new UnsupportedOperationException(Views.ERROR_MESSAGE);
		}

		@Override
		public void add(E e) {
			throw new UnsupportedOperationException(Views.ERROR_MESSAGE);
		}
	}
}

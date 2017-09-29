/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.collection.view;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 *
 * @author pp
 */
@SuppressWarnings("EqualsAndHashcode")
final class ArrayList<E> extends ArrayListBase<E> implements List<E>, CollectionToView.ToListView<E>, RandomAccess {

	ArrayList(E[] array, int start, int end) {
		super(array, start, end);
	}

	@Override
	public final boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof List)) {
			return false;
		}
		if (obj.getClass() == ArrayList.class) {
			return arrayEquals(obj);
		}
		return iterEquals(((List<?>) obj).iterator());
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		if (c == this) {
			return true;
		}
		Objects.requireNonNull(c);
		if (c.getClass() == ArrayList.class) {
			ArrayList<?> other = (ArrayList<?>) c;
			if (this.array == other.array
					&& this.start <= other.start
					&& this.end >= other.end) {
				// this view fully contains the other view
				return true;
			}
			// no iterator, at least
			for (int i = other.start; i < other.end; i++) {
				if (!this.contains(other.array[i])) {
					return false;
				}
			}
			return true;
		}
		for (Object p : c) {
			if (!this.contains(p)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public List<E> subList(int fromIndex, int toIndex) {
		int len = toIndex - fromIndex;
		if (fromIndex < 0 || len < 0 || fromIndex > size() - len) {
			throw new IndexOutOfBoundsException();
		}
		return new ArrayList<>(this.array, this.start + fromIndex, this.start + toIndex);
	}

	@Override
	public final ListView<E> toView() {
		return new ArrayListView<>(this.array, this.start, this.end);
	}

	@Override
	public void sort(Comparator<? super E> c) {
		throw new UnsupportedOperationException(Views.ERROR_MESSAGE);
	}

	@Override
	public void replaceAll(UnaryOperator<E> operator) {
		throw new UnsupportedOperationException(Views.ERROR_MESSAGE);
	}

	@Override
	public boolean removeIf(Predicate<? super E> filter) {
		throw new UnsupportedOperationException(Views.ERROR_MESSAGE);
	}

	@Override
	public boolean add(E e) {
		throw new UnsupportedOperationException(Views.ERROR_MESSAGE);
	}

	@Override
	public boolean remove(Object o) {
		throw new UnsupportedOperationException(Views.ERROR_MESSAGE);
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		throw new UnsupportedOperationException(Views.ERROR_MESSAGE);
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> c) {
		throw new UnsupportedOperationException(Views.ERROR_MESSAGE);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException(Views.ERROR_MESSAGE);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException(Views.ERROR_MESSAGE);
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException(Views.ERROR_MESSAGE);
	}

	@Override
	public E set(int index, E element) {
		throw new UnsupportedOperationException(Views.ERROR_MESSAGE);
	}

	@Override
	public void add(int index, E element) {
		throw new UnsupportedOperationException(Views.ERROR_MESSAGE);
	}

	@Override
	public E remove(int index) {
		throw new UnsupportedOperationException(Views.ERROR_MESSAGE);
	}
}

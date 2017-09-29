/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.collection.view;

import java.util.List;
import java.util.Objects;
import java.util.RandomAccess;

/**
 *
 * @author pp
 */
@SuppressWarnings("EqualsAndHashcode")
final class ArrayListView<E> extends ArrayListBase<E> implements ListView<E>, RandomAccess {

	ArrayListView(E[] array, int start, int end) {
		super(array, start, end);
	}

	@Override
	public final boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof View)) {
			return false;
		}
		if (obj.getClass() == ArrayListView.class) {
			return arrayEquals(obj);
		}
		return iterEquals(((View<?>) obj).iterator());
	}

	@Override
	public boolean containsAll(View<?> c) {
		if (c == this) {
			return true;
		}
		Objects.requireNonNull(c);
		if (c.getClass() == ArrayListView.class) {
			ArrayListView<?> other = (ArrayListView<?>) c;
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
	public ListView<E> subList(int fromIndex, int toIndex) {
		int len = toIndex - fromIndex;
		if (fromIndex < 0 || len < 0 || fromIndex > size() - len) {
			throw new IndexOutOfBoundsException();
		}
		return new ArrayListView<>(this.array, this.start + fromIndex, this.start + toIndex);
	}

	@Override
	public List<E> asCollection() {
		return new ArrayList<>(this.array, this.start, this.end);
	}
}

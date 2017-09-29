/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.collection.view;

import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.RandomAccess;

/**
 *
 * @author pp
 */
@SuppressWarnings("EqualsAndHashcode")
class DelegatingListView<E> extends DelegatingView<E, List<E>> implements ListView<E> {

	DelegatingListView(List<E> delegate) {
		super(delegate);
	}

	@Override
	public List<E> asCollection() {
		return new DelegatingList<>(this.delegate);
	}

	@Override
	public final boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof ListView)) {
			return false;
		}
		if (obj instanceof DelegatingListView) {
			return this.delegate.equals(((DelegatingListView<?>) obj).delegate);
		}
		return this.delegate.equals(((ListView<?>) obj).asCollection());
	}

	@Override
	public final E get(int index) {
		return this.delegate.get(index);
	}

	@Override
	public final int indexOf(Object o) {
		return this.delegate.indexOf(o);
	}

	@Override
	public final int lastIndexOf(Object o) {
		return this.delegate.lastIndexOf(o);
	}

	@Override
	public final ListIterator<E> listIterator() {
		return new DelegatingListIterator<>(Objects.requireNonNull(this.delegate.listIterator()));
	}

	@Override
	public final ListIterator<E> listIterator(int index) {
		return new DelegatingListIterator<>(Objects.requireNonNull(this.delegate.listIterator(index)));
	}

	@Override
	public ListView<E> subList(int fromIndex, int toIndex) {
		return new DelegatingListView<>(Objects.requireNonNull(this.delegate.subList(fromIndex, toIndex)));
	}

	static final class Random<E> extends DelegatingListView<E> implements RandomAccess {

		public Random(List<E> delegate) {
			super(delegate);
		}

		@Override
		public List<E> asCollection() {
			return new DelegatingList.Random<>(this.delegate);
		}

		@Override
		public ListView<E> subList(int fromIndex, int toIndex) {
			return new Random<>(this.delegate.subList(fromIndex, toIndex));
		}
	}
}

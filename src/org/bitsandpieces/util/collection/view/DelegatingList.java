/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.collection.view;

import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.RandomAccess;

/**
 *
 * @author pp
 */
class DelegatingList<E> extends DelegatingCollection<E, List<E>> implements List<E>, CollectionToView.ToListView<E> {

	DelegatingList(List<E> delegate) {
		super(delegate);
	}
	
	@Override
	public ListView<E> toView() {
		return new DelegatingListView<>(this.delegate);
	}

	@Override
	public final boolean addAll(int index, Collection<? extends E> c) {
		throw new UnsupportedOperationException(Views.ERROR_MESSAGE);
	}

	@Override
	public final E set(int index, E element) {
		throw new UnsupportedOperationException(Views.ERROR_MESSAGE);
	}

	@Override
	public final void add(int index, E element) {
		throw new UnsupportedOperationException(Views.ERROR_MESSAGE);
	}

	@Override
	public final E remove(int index) {
		throw new UnsupportedOperationException(Views.ERROR_MESSAGE);
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
	public List<E> subList(int fromIndex, int toIndex) {
		return new DelegatingList<>(Objects.requireNonNull(this.delegate.subList(fromIndex, toIndex)));
	}
	
	static final class Random<E> extends DelegatingList<E> implements RandomAccess {
		
		Random(List<E> delegate) {
			super(delegate);
		}

		@Override
		public ListView<E> toView() {
			return new DelegatingListView.Random<>(this.delegate);
		}
		
		@Override
		public List<E> subList(int fromIndex, int toIndex) {
			return new Random<>(this.delegate.subList(fromIndex, toIndex));
		}
	}
}

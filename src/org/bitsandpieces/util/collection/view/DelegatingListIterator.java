/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.collection.view;

import java.util.ListIterator;

/**
 *
 * @author pp
 */
final class DelegatingListIterator<E> extends DelegatingIterator<E, ListIterator<E>> implements ListIterator<E> {

	DelegatingListIterator(ListIterator<E> delegate) {
		super(delegate);
	}

	@Override
	public boolean hasPrevious() {
		return this.delegate.hasPrevious();
	}

	@Override
	public E previous() {
		return this.delegate.previous();
	}

	@Override
	public int nextIndex() {
		return this.delegate.nextIndex();
	}

	@Override
	public int previousIndex() {
		return this.delegate.previousIndex();
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

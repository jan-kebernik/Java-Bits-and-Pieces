/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.collection.view;

import java.util.Iterator;
import java.util.function.Consumer;

/**
 *
 * @author pp
 */
class DelegatingIterator<E, D extends Iterator<E>> implements Iterator<E> {

	protected final D delegate;

	DelegatingIterator(D delegate) {
		this.delegate = delegate;
	}

	DelegatingIterator() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public final boolean hasNext() {
		return this.delegate.hasNext();
	}

	@Override
	public E next() {
		return this.delegate.next();
	}

	@Override
	public final void forEachRemaining(Consumer<? super E> action) {
		this.delegate.forEachRemaining(action);
	}

	@Override
	public final void remove() {
		throw new UnsupportedOperationException(Views.ERROR_MESSAGE);
	}

	@Override
	public final String toString() {
		return this.delegate.toString();
	}
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.collection.view;

import java.util.Collection;
import java.util.function.Predicate;

/**
 *
 * @author pp
 */
class DelegatingCollection<E, D extends Collection<E>> extends DelegatingBaseImpl<E, D> implements Collection<E>, CollectionToView.ToView<E> {

	DelegatingCollection(D delegate) {
		super(delegate);
	}

	@Override
	public View<E> toView() {
		return new DelegatingView<>(this.delegate);
	}

	@Override
	public final boolean removeIf(Predicate filter) {
		throw new UnsupportedOperationException(Views.ERROR_MESSAGE);
	}

	@Override
	public final boolean add(Object e) {
		throw new UnsupportedOperationException(Views.ERROR_MESSAGE);
	}

	@Override
	public final boolean remove(Object o) {
		throw new UnsupportedOperationException(Views.ERROR_MESSAGE);
	}

	@Override
	public final boolean containsAll(Collection c) {
		throw new UnsupportedOperationException(Views.ERROR_MESSAGE);
	}

	@Override
	public final boolean addAll(Collection c) {
		throw new UnsupportedOperationException(Views.ERROR_MESSAGE);
	}

	@Override
	public final boolean removeAll(Collection c) {
		throw new UnsupportedOperationException(Views.ERROR_MESSAGE);
	}

	@Override
	public final boolean retainAll(Collection c) {
		throw new UnsupportedOperationException(Views.ERROR_MESSAGE);
	}

	@Override
	public final void clear() {
		throw new UnsupportedOperationException(Views.ERROR_MESSAGE);
	}
}

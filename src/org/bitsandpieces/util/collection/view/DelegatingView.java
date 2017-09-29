/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.collection.view;

import java.util.Collection;

/**
 *
 * @author pp
 */
@SuppressWarnings("EqualsAndHashcode")
class DelegatingView<E, D extends Collection<E>> extends DelegatingBaseImpl<E, D> implements View<E> {

	DelegatingView(D delegate) {
		super(delegate);
	}

	@Override
	public Collection<E> asCollection() {
		return new DelegatingCollection<>(this.delegate);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof View)) {
			return false;
		}
		if (obj instanceof DelegatingView) {
			return this.delegate.equals(((DelegatingView<?, Collection<?>>) obj).delegate);
		}
		return this.delegate.equals(((View<?>) obj).asCollection());
	}

	@Override
	public final boolean containsAll(View<?> c) {
		return this.delegate.containsAll(
				(c instanceof DelegatingView)
						? ((DelegatingView) c).delegate
						: c.asCollection()
		);
	}
}

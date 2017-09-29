/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.collection.view;

import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;

/**
 *
 * @author pp
 */
@SuppressWarnings("EqualsAndHashcode")
class DelegatingSetView<E, D extends Set<E>> extends DelegatingView<E, D> implements SetView<E> {

	DelegatingSetView(D delegate) {
		super(delegate);
	}

	@Override
	public final boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof SetView)) {
			return false;
		}
		if (obj instanceof DelegatingSetView) {
			return this.delegate.equals(((DelegatingSetView<?, Set<?>>) obj).delegate);
		}
		return this.delegate.equals(((SetView<?>) obj).asCollection());
	}

	@Override
	public Set<E> asCollection() {
		return new DelegatingSet<>(this.delegate);
	}

	static class Sorted<E, D extends SortedSet<E>> extends DelegatingSetView<E, D> implements SortedSetView<E> {

		Sorted(D delegate) {
			super(delegate);
		}

		@Override
		public SortedSet<E> asCollection() {
			return new DelegatingSet.Sorted<>(this.delegate);
		}

		@Override
		public final Comparator<? super E> comparator() {
			return this.delegate.comparator();
		}

		@Override
		public final SortedSetView<E> subSet(E fromElement, E toElement) {
			return new Sorted<>(Objects.requireNonNull(this.delegate.subSet(fromElement, toElement)));
		}

		@Override
		public final SortedSetView<E> headSet(E toElement) {
			return new Sorted<>(Objects.requireNonNull(this.delegate.headSet(toElement)));
		}

		@Override
		public final SortedSetView<E> tailSet(E fromElement) {
			return new Sorted<>(Objects.requireNonNull(this.delegate.tailSet(fromElement)));
		}

		@Override
		public final E first() {
			return this.delegate.first();
		}

		@Override
		public final E last() {
			return this.delegate.last();
		}
	}

	static final class Navigable<E> extends Sorted<E, NavigableSet<E>> implements NavigableSetView<E> {

		Navigable(NavigableSet<E> delegate) {
			super(delegate);
		}
		
		@Override
		public NavigableSet<E> asCollection() {
			return new DelegatingSet.Navigable<>(this.delegate);
		}
		
		@Override
		public E lower(E e) {
			return this.delegate.lower(e);
		}

		@Override
		public E floor(E e) {
			return this.delegate.floor(e);
		}

		@Override
		public E ceiling(E e) {
			return this.delegate.ceiling(e);
		}

		@Override
		public E higher(E e) {
			return this.delegate.higher(e);
		}
		
		@Override
		public NavigableSetView<E> descendingSet() {
			return new Navigable<>(Objects.requireNonNull(this.delegate.descendingSet()));
		}

		@Override
		public NavigableSetView<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
			return new Navigable<>(Objects.requireNonNull(this.delegate.subSet(fromElement, fromInclusive, toElement, toInclusive)));
		}

		@Override
		public NavigableSetView<E> headSet(E toElement, boolean inclusive) {
			return new Navigable<>(Objects.requireNonNull(this.delegate.headSet(toElement, inclusive)));
		}

		@Override
		public NavigableSetView<E> tailSet(E fromElement, boolean inclusive) {
			return new Navigable<>(Objects.requireNonNull(this.delegate.tailSet(fromElement, inclusive)));
		}
		
		// TODO test this
		@Override
		public Iterator<E> descendingIterator() {
			return descendingSet().iterator();
		}
	}
}

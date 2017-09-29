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
class DelegatingSet<E, D extends Set<E>> extends DelegatingCollection<E, D> implements Set<E>, CollectionToView.ToSetView<E> {

	DelegatingSet(D delegate) {
		super(delegate);
	}

	@Override
	public SetView<E> toView() {
		return new DelegatingSetView<>(this.delegate);
	}

	static class Sorted<E, D extends SortedSet<E>> extends DelegatingSet<E, D> implements SortedSet<E>, CollectionToView.ToSortedSetView<E> {

		Sorted(D delegate) {
			super(delegate);
		}

		@Override
		public SortedSetView<E> toView() {
			return new DelegatingSetView.Sorted<>(this.delegate);
		}

		@Override
		public Comparator<? super E> comparator() {
			return this.delegate.comparator();
		}

		@Override
		public final SortedSet<E> subSet(E fromElement, E toElement) {
			return new Sorted<>(Objects.requireNonNull(this.delegate.subSet(fromElement, toElement)));
		}

		@Override
		public final SortedSet<E> headSet(E toElement) {
			return new Sorted<>(Objects.requireNonNull(this.delegate.headSet(toElement)));
		}

		@Override
		public final SortedSet<E> tailSet(E fromElement) {
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

	static final class Navigable<E> extends Sorted<E, NavigableSet<E>> implements NavigableSet<E>, CollectionToView.ToNavigableSetView<E> {

		Navigable(NavigableSet<E> delegate) {
			super(delegate);
		}

		@Override
		public NavigableSetView<E> toView() {
			return new DelegatingSetView.Navigable<>(this.delegate);
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
		public E pollFirst() {
			throw new UnsupportedOperationException(Views.ERROR_MESSAGE);
		}

		@Override
		public E pollLast() {
			throw new UnsupportedOperationException(Views.ERROR_MESSAGE);
		}

		@Override
		public NavigableSet<E> descendingSet() {
			return new Navigable<>(Objects.requireNonNull(this.delegate.descendingSet()));
		}

		@Override
		public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
			return new Navigable<>(Objects.requireNonNull(this.delegate.subSet(fromElement, fromInclusive, toElement, toInclusive)));
		}

		@Override
		public NavigableSet<E> headSet(E toElement, boolean inclusive) {
			return new Navigable<>(Objects.requireNonNull(this.delegate.headSet(toElement, inclusive)));
		}

		@Override
		public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
			return new Navigable<>(Objects.requireNonNull(this.delegate.tailSet(fromElement, inclusive)));
		}

		// TODO test this
		@Override
		public Iterator<E> descendingIterator() {
			return descendingSet().iterator();
		}
	}
}

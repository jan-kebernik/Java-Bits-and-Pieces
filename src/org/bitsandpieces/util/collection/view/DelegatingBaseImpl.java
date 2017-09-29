/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.collection.view;

import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 *
 * @author pp
 */
class DelegatingBaseImpl<E, D extends Collection<E>> implements Iterable<E> {

	protected final D delegate;

	DelegatingBaseImpl(D delegate) {
		this.delegate = Objects.requireNonNull(delegate);
	}

	public final int size() {
		return this.delegate.size();
	}

	public final boolean isEmpty() {
		return this.delegate.isEmpty();
	}

	@SuppressWarnings("element-type-mismatch")
	public boolean contains(Object o) {
		return this.delegate.contains(o);
	}

	@Override
	public Iterator<E> iterator() {
		return new DelegatingIterator<>(Objects.requireNonNull(this.delegate.iterator()));
	}

	public Object[] toArray() {
		return this.delegate.toArray();
	}

	public <T> T[] toArray(T[] a) {
		return this.delegate.toArray(a);
	}

	public Stream<E> parallelStream() {
		return this.delegate.parallelStream();
	}

	public Stream<E> stream() {
		return this.delegate.stream();
	}

	@Override
	public Spliterator<E> spliterator() {
		return this.delegate.spliterator();
	}

	@Override
	public void forEach(Consumer<? super E> action) {
		this.delegate.forEach(action);
	}

	@Override
	public final String toString() {
		return this.delegate.toString();
	}

	@Override
	@SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		return this.delegate.equals(obj);
	}

	@Override
	public final int hashCode() {
		return this.delegate.hashCode();
	}
}

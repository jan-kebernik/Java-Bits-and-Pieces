/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.concurrent;

import java.lang.reflect.Array;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * The simplest possible concurrent lock-free data structure. This is a LIFO
 * (last in, first out) {@link Queue}, all bulk operations and iterations will
 * be performed in this order. Iterators for this class, as well as the
 * {@link #size() size()} and {@link #forEach(Consumer) forEach(Consumer)}
 * methods, perform on a snap-shot created at the time of their
 * instantiation/invokation and do not reflect modifications made to the
 * underlying collection during iteration. This collection does not accept
 * {@code null} elements. The removal methods {@link #remove(Object o) remove(Object)},
 * {@link #removeAll(Collection) removeAll(Collection)}, {@link #retainAll(Collection) retainAll(Collection)}
 * and {@link #removeIf(Predicate) removeIf(Predicate)} are unsupported.
 *
 * @param <E> the type of elements in this collection
 *
 * @author Jan Kebernik
 */
public class ConcurrentStack<E> extends AbstractCollection<E> implements Iterable<E>, Collection<E>, Queue<E> {

	private final AtomicReference<Node<E>> head = new AtomicReference<>();

	@Override
	public boolean add(E element) {
		if (element == null) {
			throw new NullPointerException("This collection does not accept null values.");
		}
		while (true) {
			Node<E> p = this.head.get();
			if (this.head.compareAndSet(p, new Node<>(element, p))) {
				return true;
			}
		}
	}

	@Override
	public boolean offer(E e) {
		return add(e);
	}

	@Override
	public E poll() {
		while (true) {
			Node<E> p = this.head.get();
			if (p == null) {
				// stack is empty
				return null;
			}
			if (this.head.compareAndSet(p, p.previous)) {
				// node replaced with predecessor (possibly null)
				return p.element;
			}
		}
	}

	@Override
	public E remove() {
		while (true) {
			Node<E> p = this.head.get();
			if (p == null) {
				// stack is empty
				throw new NoSuchElementException();
			}
			if (this.head.compareAndSet(p, p.previous)) {
				// node replaced with predecessor (possibly null)
				return p.element;
			}
		}
	}

	@Override
	public E peek() {
		Node<E> p = this.head.get();
		return p == null ? null : p.element;
	}

	@Override
	public E element() {
		Node<E> p = this.head.get();
		if (p == null) {
			// stack is empty
			throw new NoSuchElementException();
		}
		return p.element;
	}

	@Override
	public void clear() {
		this.head.set(null);
	}

	@Override
	public boolean isEmpty() {
		return this.head.get() == null;
	}

	@Override
	public int size() {
		int n = 0;
		for (Node<E> p = this.head.get(); p != null; p = p.previous, n++) {
		}
		return n;
	}

	@Override
	public Iterator<E> iterator() {
		return new Iter<>(this.head.get());
	}

	@Override
	public void forEach(Consumer<? super E> action) {
		Objects.requireNonNull(action);
		forEach0(this.head.get(), action);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		// technically, it can be done by swapping the entire stack
		// with a modified copy.
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		// technically, it can be done by swapping the entire stack
		// with a modified copy.
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeIf(Predicate<? super E> filter) {
		// technically, it can be done by swapping the entire stack
		// with a modified copy.
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean contains(Object o) {
		if (o == null) {
			return false;
		}
		for (Node<E> p = this.head.get(); p != null; p = p.previous) {
			if (o.equals(p.element)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public <T> T[] toArray(T[] a) {
		Objects.requireNonNull(a);
		int n = 0;
		Node<E> q = this.head.get();
		for (Node<E> p = q; p != null; p = p.previous, n++) {
		}
		if (a.length < n) {
			Class<?> type = a.getClass();
			a = (T[]) (type == Object[].class ? new Object[n] : Array.newInstance(type.getComponentType(), n));
		}
		n = 0;
		for (Node<E> p = q; p != null; p = p.previous, n++) {
			a[n] = (T) p.element;
		}
		return a;
	}

	@Override
	public Object[] toArray() {
		int n = 0;
		Node<E> q = this.head.get();
		for (Node<E> p = q; p != null; p = p.previous, n++) {
		}
		Object[] a = new Object[n];
		n = 0;
		for (Node<E> p = q; p != null; p = p.previous, n++) {
			a[n] = p.element;
		}
		return a;
	}

	@Override
	public boolean remove(Object o) {
		throw new UnsupportedOperationException();
	}

	private static <E> void forEach0(Node<E> p, Consumer<? super E> action) {
		for (; p != null; p = p.previous) {
			action.accept(p.element);
		}
	}

	private static final class Node<E> {

		private final E element;
		private final Node<E> previous;

		public Node(E element, Node<E> previous) {
			this.element = element;
			this.previous = previous;
		}
	}

	private static final class Iter<E> implements Iterator<E> {

		private Node<E> next;

		public Iter(Node<E> next) {
			this.next = next;
		}

		@Override
		public boolean hasNext() {
			return this.next != null;
		}

		@Override
		public E next() {
			Node<E> p = this.next;
			if (p == null) {
				throw new IllegalStateException();
			}
			E elem = p.element;
			this.next = p.previous;
			return elem;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void forEachRemaining(Consumer<? super E> action) {
			Objects.requireNonNull(action);
			forEach0(this.next, action);
			this.next = null;
		}
	}
}

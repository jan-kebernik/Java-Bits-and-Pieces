/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.io;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Very simple thread-safe cache for a pool of (for all intents and purposes)
 * <em>equal</em> instances (equals() method is not actually used). Will return
 * a cached instance when available, otherwise use the installed factory method
 * to create a new instance.
 * <p>
 * Releasing instances back into the cache is not strictly necessary, but is
 * encouraged. The same instance must not be released into the cache more than
 * once at the same time, as that would lead to undefined behaviour.
 * <p>
 * This class is useful for avoiding expensive object allocation, e.g. for
 * re-usable arrays of the same length.
 * <p>
 * It is the programmer's responsibility to ensure that all instances returned
 * by the installed factory method are new non-{@code null} instances, or at
 * the very least, have the characteristics of a new instance (not currently
 * in use by any thread, no risk of being leaked to other threads, etc...).
 * <p>
 * Objects in the cache are only weakly reachable. Stale references are
 * automatically cleaned up when the cache is accessed.
 *
 * @param <T> the runtime type of objects stored in and retrieved from this
 * cache.
 *
 * @author Jan Kebernik
 */
class ObjectCache<T> {

	// uses a very simple concurrent linked stack as the internal data strucure.
	// 
	private final Supplier<T> factory;
	private final AtomicReference<Node<T>> stack;

	/**
	 * Creates a new {@code ObjectCache} with the specified factory method
	 * installed.
	 *
	 * @param factory the factory method used to create new instances.
	 */
	public ObjectCache(Supplier<T> factory) {
		if (factory == null) {
			throw new NullPointerException();
		}
		this.factory = factory;
		this.stack = new AtomicReference<>();
	}

	/**
	 * Returns the installed factory method.
	 *
	 * @return the installed factory method.
	 */
	public Supplier<T> factory() {
		return this.factory;
	}

	/**
	 * Returns a cached or new instance.
	 *
	 * @return a cached or new instance.
	 * @throws NullPointerException if the installed factory method returned
	 * {@code null}.
	 */
	public T requestInstance() {
		// removes nodes until either
		// a) the removed node is not stale, returning its referent
		// b) the stack is empty, returning a new instance
		while (true) {
			Node<T> n = this.stack.get();
			if (n == null) {
				// stack is empty
				T instance = this.factory.get();
				if (instance == null) {
					throw new NullPointerException("Factory method must not return null.");
				}
				return instance;
			}
			if (this.stack.compareAndSet(n, n.prev)) {
				// head unqueued by current thread
				T b = n.get();
				if (b != null) {
					// not stale, head already unqueued. done.
					return b;
				}
				// stale, retry.
			}
		}
	}

	/**
	 * Releases the specified object into the pool of cached instances. Never
	 * release an instance that may already be in the pool! No steps are taken
	 * to test whether the object is already in the cache.
	 *
	 * @param instance the instance to cache.
	 */
	public void releaseInstance(T instance) {
		// adds the instance as a new node, removing all encountered stale
		// nodes from the stack in the process
		if (instance == null) {
			throw new NullPointerException();
		}
		Node<T> n = this.stack.get();
		while (true) {
			if (n == null || n.get() != null) {
				// no head or head not stale
				if (this.stack.compareAndSet(n, new Node<>(instance, n))) {
					// re-queued head as child of new head. done.
					return;
				}
				// lost thread-race. retry with fresh snapshot.
			} else if (this.stack.compareAndSet(n, n.prev)) {
				// stale head de-queued. retry with current snapshot. 
				n = n.prev;
				continue;
			}
			// update snapshot.
			n = this.stack.get();
		}
	}

	// stack node
	private static final class Node<T> extends WeakReference<T> {

		private final Node<T> prev;

		private Node(T value, Node<T> prev) {
			super(value);
			this.prev = prev;
		}
	}
}

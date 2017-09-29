/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.collection.view;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 *
 * @author pp
 */
class DelegatingMapBase<K, V, D extends Map<K, V>> {

	protected final D delegate;
	private transient Set<K> delegateKeySet;
	private transient Collection<V> delegateValues;
	private transient Set<Map.Entry<K, V>> delegateEntrySet;
	
	DelegatingMapBase(D delegate) {
		this.delegate = delegate;
	}
	
	protected final Set<K> delegateKeySet() {
		Set<K> d = this.delegateKeySet;
		if (d == null) {
			this.delegateKeySet = this.delegate.keySet();
		}
		return d;
	}

	protected final Collection<V> delegateValues() {
		Collection<V> d = this.delegateValues;
		if (d == null) {
			this.delegateValues = this.delegate.values();
		}
		return d;
	}
	
	
	protected final Set<Map.Entry<K, V>> delegateEntrySet() {
		Set<Map.Entry<K, V>> d = this.delegateEntrySet;
		if (d == null) {
			this.delegateEntrySet = this.delegate.entrySet();
		}
		return d;
	}
	
	public final int size() {
		return this.delegate.size();
	}

	public final boolean isEmpty() {
		return this.delegate.isEmpty();
	}

	@SuppressWarnings("element-type-mismatch")
	public final boolean containsKey(Object key) {
		return this.delegate.containsKey(key);
	}

	@SuppressWarnings("element-type-mismatch")
	public final boolean containsValue(Object value) {
		return this.delegate.containsValue(value);
	}

	@SuppressWarnings("element-type-mismatch")
	public final V get(Object key) {
		return this.delegate.get(key);
	}

	public final V getOrDefault(Object key, V defaultValue) {
		return this.delegate.getOrDefault(key, defaultValue);
	}

	public final void forEach(BiConsumer<? super K, ? super V> action) {
		this.delegate.forEach(action);
	}

	@Override
	public final int hashCode() {
		return this.delegate.hashCode();
	}

	@Override
	@SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		return this.delegate.equals(o);
	}

	@Override
	public final String toString() {
		return this.delegate.toString();
	}

	private static <K, V> Consumer<Map.Entry<K, V>> consumer(Consumer<? super Map.Entry<K, V>> cons) {
		return e -> cons.accept(delegateOrNull(e));
	}
	
	private static <K, V> Map.Entry<K, V> delegate(Map.Entry<K, V> e) {
		return new DelegatingEntry<>(e);
	}
	
	static final <K, V> Map.Entry<K, V> delegateOrNull(Map.Entry<K, V> e) {
		return e == null ? null : delegate(e);
	}
	
	private static final class DelegatingEntry<K, V> implements Map.Entry<K, V> {

		private final Map.Entry<K, V> delegate;

		private DelegatingEntry(Map.Entry<K, V> delegate) {
			this.delegate = delegate;
		}

		@Override
		public K getKey() {
			return this.delegate.getKey();
		}

		@Override
		public V getValue() {
			return this.delegate.getValue();
		}

		@Override
		public V setValue(V value) {
			throw new UnsupportedOperationException(Views.ERROR_MESSAGE);
		}

		@Override
		public String toString() {
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
		public int hashCode() {
			return this.delegate.hashCode();
		}
	}

	static class DelegatingEntrySetBase<K, V> extends DelegatingBaseImpl<Map.Entry<K, V>, Set<Map.Entry<K, V>>> {

		DelegatingEntrySetBase(Set<Map.Entry<K, V>> delegate) {
			super(delegate);
		}

		@Override
		public final void forEach(Consumer<? super Map.Entry<K, V>> action) {
			this.delegate.forEach(consumer(Objects.requireNonNull(action)));
		}

		@Override
		public final Object[] toArray() {
			Object[] a = this.delegate.toArray();
			for (int i = 0; i < a.length; i++) {
				a[i] = delegateOrNull((Map.Entry<? extends K, ? extends V>) a[i]);
			}
			return a;
		}

		// TODO this is not pretty.
		@Override
		public final <T> T[] toArray(T[] a) {
			@SuppressWarnings("SuspiciousToArrayCall")
			Object[] b = this.delegate.toArray(a.length == 0 ? a : Arrays.copyOf(a, 0));
			for (int i = 0; i < b.length; i++) {
				b[i] = delegateOrNull((Map.Entry<? extends K, ? extends V>) b[i]);
			}
			if (a.length < b.length) {
				return (T[]) b;
			}
			System.arraycopy(b, 0, a, 0, b.length);
			if (a.length > b.length) {
				a[b.length] = null;
			}
			return a;
		}

		@Override
		@SuppressWarnings("element-type-mismatch")
		public final boolean contains(Object o) {
			if (!(o instanceof Map.Entry)) {
				return false;
			}
			return this.delegate.contains(delegate((Map.Entry<?, ?>) o));
		}

		@Override
		public final Iterator<Map.Entry<K, V>> iterator() {
			return new DelegatingIteratorImpl<>(Objects.requireNonNull(this.delegate.iterator()));
		}

		@Override
		public final Spliterator<Map.Entry<K, V>> spliterator() {
			return new DelegatingSpliterator<>(Objects.requireNonNull(this.delegate.spliterator()));
		}

		// ensure wrapping spliterators are used
		@Override
		public final Stream<Map.Entry<K, V>> stream() {
			return StreamSupport.stream(spliterator(), false);
		}

		@Override
		public final Stream<Map.Entry<K, V>> parallelStream() {
			return StreamSupport.stream(spliterator(), true);
		}
	}

	private static final class DelegatingIteratorImpl<K, V> extends DelegatingIterator<Map.Entry<K, V>, Iterator<Map.Entry<K, V>>> {

		DelegatingIteratorImpl(Iterator<Map.Entry<K, V>> delegate) {
			super(delegate);
		}

		@Override
		public Map.Entry<K, V> next() {
			return delegate(Objects.requireNonNull(this.delegate.next()));
		}
	}

	private static final class DelegatingSpliterator<K, V> implements
			Spliterator<Map.Entry<K, V>> {

		private final Spliterator<Map.Entry<K, V>> delegate;

		DelegatingSpliterator(Spliterator<Map.Entry<K, V>> delegate) {
			this.delegate = delegate;
		}

		@Override
		public boolean tryAdvance(Consumer<? super Map.Entry<K, V>> action) {
			return this.delegate.tryAdvance(consumer(Objects.requireNonNull(action)));
		}

		@Override
		public Spliterator<Map.Entry<K, V>> trySplit() {
			Spliterator<Map.Entry<K, V>> s = this.delegate.trySplit();
			return s == null ? null : new DelegatingSpliterator<>(s);
		}

		@Override
		public long estimateSize() {
			return this.delegate.estimateSize();
		}

		@Override
		public int characteristics() {
			return this.delegate.characteristics();
		}

		@Override
		public Comparator<? super Map.Entry<K, V>> getComparator() {
			return this.delegate.getComparator();
		}

		@Override
		public boolean hasCharacteristics(int characteristics) {
			return this.delegate.hasCharacteristics(characteristics);
		}

		@Override
		public long getExactSizeIfKnown() {
			return this.delegate.getExactSizeIfKnown();
		}

		@Override
		public void forEachRemaining(Consumer<? super Map.Entry<K, V>> action) {
			this.delegate.forEachRemaining(consumer(Objects.requireNonNull(action)));
		}
	}
}

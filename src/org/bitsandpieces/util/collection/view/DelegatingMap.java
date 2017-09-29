/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.collection.view;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 *
 * @author pp
 */
class DelegatingMap<K, V, D extends Map<K, V>> extends DelegatingMapBase<K, V, D> implements Map<K, V>, CollectionToView.ToMapView<K, V> {

	DelegatingMap(D delegate) {
		super(delegate);
	}

	@Override
	public final V put(K key, V value) {
		throw new UnsupportedOperationException(Views.ERROR_MESSAGE);
	}

	@Override
	public final V remove(Object key) {
		throw new UnsupportedOperationException(Views.ERROR_MESSAGE);
	}

	@Override
	public final void putAll(Map<? extends K, ? extends V> m) {
		throw new UnsupportedOperationException(Views.ERROR_MESSAGE);
	}

	@Override
	public final void clear() {
		throw new UnsupportedOperationException(Views.ERROR_MESSAGE);
	}

	@Override
	public final void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
		throw new UnsupportedOperationException(Views.ERROR_MESSAGE);
	}

	@Override
	public final V putIfAbsent(K key, V value) {
		throw new UnsupportedOperationException(Views.ERROR_MESSAGE);
	}

	@Override
	public final boolean remove(Object key, Object value) {
		throw new UnsupportedOperationException(Views.ERROR_MESSAGE);
	}

	@Override
	public final boolean replace(K key, V oldValue, V newValue) {
		throw new UnsupportedOperationException(Views.ERROR_MESSAGE);
	}

	@Override
	public final V replace(K key, V value) {
		throw new UnsupportedOperationException(Views.ERROR_MESSAGE);
	}

	@Override
	public final V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
		throw new UnsupportedOperationException(Views.ERROR_MESSAGE);
	}

	@Override
	public final V computeIfPresent(K key,
			BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
		throw new UnsupportedOperationException(Views.ERROR_MESSAGE);
	}

	@Override
	public final V compute(K key,
			BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
		throw new UnsupportedOperationException(Views.ERROR_MESSAGE);
	}

	@Override
	public final V merge(K key, V value,
			BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
		throw new UnsupportedOperationException(Views.ERROR_MESSAGE);
	}

	private transient Set<K> keys;
	private transient Collection<V> values;
	private transient Set<Map.Entry<K, V>> entries;

	@Override
	public Set<K> keySet() {
		Set<K> d = this.keys;
		if (d == null) {
			return this.keys = new DelegatingSet<>(Objects.requireNonNull(delegateKeySet()));
		}
		return d;
	}

	@Override
	public Collection<V> values() {
		Collection<V> d = this.values;
		if (d == null) {
			return this.values = new DelegatingCollection<>(Objects.requireNonNull(delegateValues()));
		}
		return d;
	}

	@Override
	public Set<Map.Entry<K, V>> entrySet() {
		Set<Map.Entry<K, V>> d = this.entries;
		if (d == null) {
			return this.entries = new DelegatingEntrySet<>(Objects.requireNonNull(delegateEntrySet()));
		}
		return d;
	}

	@Override
	public MapView<K, V> toView() {
		return new DelegatingMapView<>(this.delegate);
	}

	@SuppressWarnings("EqualsAndHashcode")
	static final class DelegatingEntrySet<K, V> extends DelegatingEntrySetBase<K, V>
			implements Set<Map.Entry<K, V>>, CollectionToView.ToSetView<Map.Entry<K, V>> {

		DelegatingEntrySet(Set<Entry<K, V>> delegate) {
			super(delegate);
		}

		@Override
		public SetView<Entry<K, V>> toView() {
			return new DelegatingMapView.DelegatingEntrySetView<>(this.delegate);
		}

		@Override
		public boolean containsAll(Collection<?> c) {
			for (Object e : c) {
				if (!contains(e)) {
					return false;
				}
			}
			return true;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			}
			if (!(obj instanceof Set)) {
				return false;
			}
			Set<?> s = (Set<?>) obj;
			if (s.size() != this.size()) {
				return false;
			}
			return containsAll(s);
		}

		@Override
		public boolean add(Entry<K, V> e) {
			throw new UnsupportedOperationException(Views.ERROR_MESSAGE);
		}

		@Override
		public boolean remove(Object o) {
			throw new UnsupportedOperationException(Views.ERROR_MESSAGE);
		}

		@Override
		public boolean addAll(Collection<? extends Entry<K, V>> c) {
			throw new UnsupportedOperationException(Views.ERROR_MESSAGE);
		}

		@Override
		public boolean retainAll(Collection<?> c) {
			throw new UnsupportedOperationException(Views.ERROR_MESSAGE);
		}

		@Override
		public boolean removeAll(Collection<?> c) {
			throw new UnsupportedOperationException(Views.ERROR_MESSAGE);
		}

		@Override
		public void clear() {
			throw new UnsupportedOperationException(Views.ERROR_MESSAGE);
		}

		@Override
		public boolean removeIf(Predicate<? super Entry<K, V>> filter) {
			throw new UnsupportedOperationException(Views.ERROR_MESSAGE);
		}
	}

	static class Sorted<K, V, D extends SortedMap<K, V>> extends DelegatingMap<K, V, D> implements SortedMap<K, V>, CollectionToView.ToSortedMapView<K, V> {

		Sorted(D delegate) {
			super(delegate);
		}

		@Override
		public SortedMapView<K, V> toView() {
			return new DelegatingMapView.Sorted<>(this.delegate);
		}

		@Override
		public final Comparator<? super K> comparator() {
			return this.delegate.comparator();
		}

		@Override
		public final SortedMap<K, V> subMap(K fromKey, K toKey) {
			return new Sorted<>(Objects.requireNonNull(this.delegate.subMap(fromKey, toKey)));
		}

		@Override
		public final SortedMap<K, V> headMap(K toKey) {
			return new Sorted<>(Objects.requireNonNull(this.delegate.headMap(toKey)));
		}

		@Override
		public final SortedMap<K, V> tailMap(K fromKey) {
			return new Sorted<>(Objects.requireNonNull(this.delegate.tailMap(fromKey)));
		}

		@Override
		public final K firstKey() {
			return this.delegate.firstKey();
		}

		@Override
		public final K lastKey() {
			return this.delegate.lastKey();
		}
	}

	static final class Navigable<K, V> extends Sorted<K, V, NavigableMap<K, V>> implements NavigableMap<K, V>, CollectionToView.ToNavigableMapView<K, V> {

		Navigable(NavigableMap<K, V> delegate) {
			super(delegate);
		}

		@Override
		public NavigableMapView<K, V> toView() {
			return new DelegatingMapView.Navigable<>(this.delegate);
		}

		@Override
		public K lowerKey(K key) {
			return this.delegate.lowerKey(key);
		}

		@Override
		public K floorKey(K key) {
			return this.delegate.floorKey(key);
		}

		@Override
		public K ceilingKey(K key) {
			return this.delegate.ceilingKey(key);
		}

		@Override
		public K higherKey(K key) {
			return this.delegate.higherKey(key);
		}

		@Override
		public Map.Entry<K, V> lowerEntry(K key) {
			return delegateOrNull(this.delegate.lowerEntry(key));
		}

		@Override
		public Map.Entry<K, V> floorEntry(K key) {
			return delegateOrNull(this.delegate.floorEntry(key));
		}

		@Override
		public Map.Entry<K, V> ceilingEntry(K key) {
			return delegateOrNull(this.delegate.ceilingEntry(key));
		}

		@Override
		public Map.Entry<K, V> higherEntry(K key) {
			return delegateOrNull(this.delegate.higherEntry(key));
		}

		@Override
		public Map.Entry<K, V> firstEntry() {
			return delegateOrNull(this.delegate.firstEntry());
		}

		@Override
		public Map.Entry<K, V> lastEntry() {
			return delegateOrNull(this.delegate.lastEntry());
		}

		@Override
		public Map.Entry<K, V> pollFirstEntry() {
			throw new UnsupportedOperationException(Views.ERROR_MESSAGE);
		}

		@Override
		public Map.Entry<K, V> pollLastEntry() {
			throw new UnsupportedOperationException(Views.ERROR_MESSAGE);
		}

		@Override
		public NavigableMap<K, V> descendingMap() {
			return new Navigable<>(Objects.requireNonNull(this.delegate.descendingMap()));
		}

		@Override
		public NavigableSet<K> navigableKeySet() {
			return new DelegatingSet.Navigable<>(Objects.requireNonNull(this.delegate.navigableKeySet()));
		}

		@Override
		public NavigableSet<K> descendingKeySet() {
			return new DelegatingSet.Navigable<>(Objects.requireNonNull(this.delegate.descendingKeySet()));
		}

		@Override
		public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
			return new Navigable<>(Objects.requireNonNull(this.delegate.subMap(fromKey, fromInclusive, toKey, toInclusive)));
		}

		@Override
		public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
			return new Navigable<>(Objects.requireNonNull(this.delegate.headMap(toKey, inclusive)));
		}

		@Override
		public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
			return new Navigable<>(Objects.requireNonNull(this.delegate.tailMap(fromKey, inclusive)));
		}
	}
}

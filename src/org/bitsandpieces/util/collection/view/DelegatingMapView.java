/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.collection.view;

import java.util.Comparator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;

/**
 *
 * @author pp
 */
class DelegatingMapView<K, V, D extends Map<K, V>> extends DelegatingMapBase<K, V, D> implements MapView<K, V> {

	DelegatingMapView(D delegate) {
		super(delegate);
	}

	private transient SetView<K> keys;
	private transient View<V> values;
	private transient SetView<Map.Entry<K, V>> entries;

	@Override
	public SetView<K> keySet() {
		SetView<K> d = this.keys;
		if (d == null) {
			return this.keys = new DelegatingSetView<>(Objects.requireNonNull(delegateKeySet()));
		}
		return d;
	}

	@Override
	public View<V> values() {
		View<V> d = this.values;
		if (d == null) {
			return this.values = new DelegatingView<>(Objects.requireNonNull(delegateValues()));
		}
		return d;
	}

	@Override
	public SetView<Map.Entry<K, V>> entrySet() {
		SetView<Map.Entry<K, V>> d = this.entries;
		if (d == null) {
			return this.entries = new DelegatingEntrySetView<>(Objects.requireNonNull(delegateEntrySet()));
		}
		return d;
	}

	@Override
	public Map<K, V> toMap() {
		// never null
		return new DelegatingMap<>(this.delegate);
	}

	@SuppressWarnings("EqualsAndHashcode")
	static final class DelegatingEntrySetView<K, V> extends DelegatingEntrySetBase<K, V> implements SetView<Map.Entry<K, V>> {

		DelegatingEntrySetView(Set<Map.Entry<K, V>> delegate) {
			super(delegate);
		}

		@Override
		public Set<Map.Entry<K, V>> asCollection() {
			// never null
			return new DelegatingMap.DelegatingEntrySet<>(this.delegate);
		}

		@Override
		public boolean containsAll(View<?> c) {
			for (Object e : c) {
				if (!contains(e)) {
					return false;
				}
			}
			return true;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (!(obj instanceof SetView)) {
				return false;
			}
			SetView<?> s = (SetView<?>) obj;
			if (s.size() != this.size()) {
				return false;
			}
			return containsAll(s);
		}
	}

	static class Sorted<K, V, D extends SortedMap<K, V>> extends DelegatingMapView<K, V, D> implements SortedMapView<K, V> {

		Sorted(D delegate) {
			super(delegate);
		}

		@Override
		public SortedMap<K, V> toMap() {
			// never null
			return new DelegatingMap.Sorted<>(this.delegate);
		}

		@Override
		public final Comparator<? super K> comparator() {
			return this.delegate.comparator();
		}

		@Override
		public final SortedMapView<K, V> subMap(K fromKey, K toKey) {
			return new Sorted<>(Objects.requireNonNull(this.delegate.subMap(fromKey, toKey)));
		}

		@Override
		public final SortedMapView<K, V> headMap(K toKey) {
			return new Sorted<>(Objects.requireNonNull(this.delegate.headMap(toKey)));
		}

		@Override
		public final SortedMapView<K, V> tailMap(K fromKey) {
			return new Sorted<>(Objects.requireNonNull(this.delegate.tailMap(fromKey)));
		}

		@Override
		public final  K firstKey() {
			return this.delegate.firstKey();
		}

		@Override
		public final K lastKey() {
			return this.delegate.lastKey();
		}
	}

	static final class Navigable<K, V> extends Sorted<K, V, NavigableMap<K, V>> implements NavigableMapView<K, V> {

		Navigable(NavigableMap<K, V> delegate) {
			super(delegate);
		}

		@Override
		public NavigableMap<K, V> toMap() {
			// never null
			return new DelegatingMap.Navigable<>(this.delegate);
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
		public NavigableMapView<K, V> descendingMap() {
			return new Navigable<>(Objects.requireNonNull(this.delegate.descendingMap()));
		}

		@Override
		public NavigableSetView<K> navigableKeySet() {
			return new DelegatingSetView.Navigable<>(Objects.requireNonNull(this.delegate.navigableKeySet()));
		}

		@Override
		public NavigableSetView<K> descendingKeySet() {
			return new DelegatingSetView.Navigable<>(Objects.requireNonNull(this.delegate.descendingKeySet()));
		}

		@Override
		public NavigableMapView<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
			return new Navigable<>(Objects.requireNonNull(this.delegate.subMap(fromKey, fromInclusive, toKey, toInclusive)));
		}

		@Override
		public NavigableMapView<K, V> headMap(K toKey, boolean inclusive) {
			return new Navigable<>(Objects.requireNonNull(this.delegate.headMap(toKey, inclusive)));
		}

		@Override
		public NavigableMapView<K, V> tailMap(K fromKey, boolean inclusive) {
			return new Navigable<>(Objects.requireNonNull(this.delegate.tailMap(fromKey, inclusive)));
		}
	}
}

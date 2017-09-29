/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.collection.view;

import java.util.Map;
import java.util.NavigableMap;

/**
 * A {@code NavigableMapView} represents an unmodifiable group of ordered
 * key-object mappings with additional search functionality, usually backed by a
 * {@link java.util.NavigableMap}. Generally speaking, whether changes to the
 * backing data-structure are reflected by the {@code MapView} (and its
 * iterators, spliterators and streams) depends on whether the backing
 * data-structure behaves the same way.
 *
 * @param <K> the type of keys maintained by this {@code NavigableMapView}
 * @param <V> the type of mapped values
 *
 * @author Jan Kebernik
 */
public interface NavigableMapView<K, V> extends SortedMapView<K, V> {

	/**
	 * Returns a key-value mapping associated with the greatest key strictly
	 * less than the given key, or {@code null} if there is no such key.
	 *
	 * @param key the key
	 * @return an entry with the greatest key less than {@code key}, or
	 * {@code null} if there is no such key
	 * @throws ClassCastException if the specified key cannot be compared with
	 * the keys currently in the {@code MapView}
	 * @throws NullPointerException if the specified key is {@code null} and
	 * this {@code MapView} does not permit {@code null} keys
	 */
	Map.Entry<K, V> lowerEntry(K key);

	/**
	 * Returns the greatest key strictly less than the given key, or
	 * {@code null} if there is no such key.
	 *
	 * @param key the key
	 * @return the greatest key less than {@code key}, or {@code null} if there
	 * is no such key
	 * @throws ClassCastException if the specified key cannot be compared with
	 * the keys currently in the {@code MapView}
	 * @throws NullPointerException if the specified key is {@code null} and
	 * this {@code MapView} does not permit {@code null} keys
	 */
	K lowerKey(K key);

	/**
	 * Returns a key-value mapping associated with the greatest key less than or
	 * equal to the given key, or {@code null} if there is no such key.
	 *
	 * @param key the key
	 * @return an entry with the greatest key less than or equal to {@code key},
	 * or {@code null} if there is no such key
	 * @throws ClassCastException if the specified key cannot be compared with
	 * the keys currently in the {@code MapView}
	 * @throws NullPointerException if the specified key is {@code null} and
	 * this {@code MapView} does not permit {@code null} keys
	 */
	Map.Entry<K, V> floorEntry(K key);

	/**
	 * Returns the greatest key less than or equal to the given key, or
	 * {@code null} if there is no such key.
	 *
	 * @param key the key
	 * @return the greatest key less than or equal to {@code key}, or
	 * {@code null} if there is no such key
	 * @throws ClassCastException if the specified key cannot be compared with
	 * the keys currently in the {@code MapView}
	 * @throws NullPointerException if the specified key is {@code null} and
	 * this {@code MapView} does not permit {@code null} keys
	 */
	K floorKey(K key);

	/**
	 * Returns a key-value mapping associated with the least key greater than or
	 * equal to the given key, or {@code null} if there is no such key.
	 *
	 * @param key the key
	 * @return an entry with the least key greater than or equal to {@code key},
	 * or {@code null} if there is no such key
	 * @throws ClassCastException if the specified key cannot be compared with
	 * the keys currently in the {@code MapView}
	 * @throws NullPointerException if the specified key is {@code null} and
	 * this {@code MapView} does not permit {@code null} keys
	 */
	Map.Entry<K, V> ceilingEntry(K key);

	/**
	 * Returns the least key greater than or equal to the given key, or
	 * {@code null} if there is no such key.
	 *
	 * @param key the key
	 * @return the least key greater than or equal to {@code key}, or
	 * {@code null} if there is no such key
	 * @throws ClassCastException if the specified key cannot be compared with
	 * the keys currently in the {@code MapView}
	 * @throws NullPointerException if the specified key is {@code null} and
	 * this {@code MapView} does not permit {@code null} keys
	 */
	K ceilingKey(K key);

	/**
	 * Returns a key-value mapping associated with the least key strictly
	 * greater than the given key, or {@code null} if there is no such key.
	 *
	 * @param key the key
	 * @return an entry with the least key greater than {@code key}, or
	 * {@code null} if there is no such key
	 * @throws ClassCastException if the specified key cannot be compared with
	 * the keys currently in the {@code MapView}
	 * @throws NullPointerException if the specified key is {@code null} and
	 * this {@code MapView} does not permit {@code null} keys
	 */
	Map.Entry<K, V> higherEntry(K key);

	/**
	 * Returns the least key strictly greater than the given key, or
	 * {@code null} if there is no such key.
	 *
	 * @param key the key
	 * @return the least key greater than {@code key}, or {@code null} if there
	 * is no such key
	 * @throws ClassCastException if the specified key cannot be compared with
	 * the keys currently in the {@code MapView}
	 * @throws NullPointerException if the specified key is {@code null} and
	 * this {@code MapView} does not permit {@code null} keys
	 */
	K higherKey(K key);

	/**
	 * Returns a key-value mapping associated with the least key in this
	 * {@code MapView}, or {@code null} if the {@code MapView} is empty.
	 *
	 * @return an entry with the least key, or {@code null} if this
	 * {@code MapView} is empty
	 */
	Map.Entry<K, V> firstEntry();

	/**
	 * Returns a key-value mapping associated with the greatest key in this
	 * {@code MapView}, or {@code null} if the {@code MapView} is empty.
	 *
	 * @return an entry with the greatest key, or {@code null} if this
	 * {@code MapView} is empty
	 */
	Map.Entry<K, V> lastEntry();

	/**
	 * Returns a reverse order {@code NavigableMapView} of the mappings
	 * contained in this {@code MapView}. The descending {@code MapView} is
	 * backed by this {@code MapView}, so changes to this {@code MapView} are
	 * reflected in the returned descending {@code MapView}, and vice-versa. If
	 * this {@code MapView} is modified while an iteration over a {@code View}
	 * of either {@code MapView} is in progress, the results of the iteration
	 * are undefined.
	 *
	 * <p>
	 * The expression {@code m.descendingMap().descendingMap()} returns a
	 * {@code MapView} of {@code m} essentially equivalent to {@code m}.
	 *
	 * @return a reverse order view of this {@code MapView}
	 */
	NavigableMapView<K, V> descendingMap();

	/**
	 * Returns a {@code NavigableSetView} of the keys contained in this
	 * {@code MapView}. The {@code SetView}'s iterator returns the keys in
	 * ascending order. The {@code SetView} is backed by the {@code MapView}, so
	 * changes to the {@code MapView} are reflected in the set. If the
	 * {@code MapView} is modified while an iteration over the {@code SetView}
	 * is in progress, the results of the iteration are undefined.
	 *
	 * @return a navigable {@code SetView} of the keys in this {@code MapView}
	 */
	NavigableSetView<K> navigableKeySet();

	/**
	 * Returns a reverse order {@code NavigableSetView} of the keys contained in
	 * this {@code MapView}. The {@code SetView}'s iterator returns the keys in
	 * descending order. The {@code SetView} is backed by the {@code MapView},
	 * so changes to the {@code MapView} are reflected in the {@code SetView}.
	 * If the {@code MapView} is modified while an iteration over the
	 * {@code SetView} is in progress, the results of the iteration are
	 * undefined.
	 *
	 * @return a reverse order navigable {@code SetView} of the keys in this
	 * {@code MapView}
	 */
	NavigableSetView<K> descendingKeySet();

	/**
	 * Returns a {@code MapView} of the portion of this {@code MapView} whose
	 * keys range from {@code fromKey} to {@code toKey}. If {@code fromKey} and
	 * {@code toKey} are equal, the returned {@code MapView} is empty unless
	 * {@code fromInclusive} and {@code toInclusive} are both true. The returned
	 * {@code MapView} is backed by this {@code MapView}, so changes in this
	 * {@code MapView} are reflected in the returned {@code MapView}. The
	 * returned {@code MapView} supports all optional {@code MapView} operations
	 * that this {@code MapView} supports.
	 *
	 * <p>
	 * The returned {@code MapView} will throw an
	 * {@code IllegalArgumentException} on an attempt to construct a submap
	 * either of whose endpoints lie outside its range.
	 *
	 * @param fromKey low endpoint of the keys in the returned {@code MapView}
	 * @param fromInclusive {@code true} if the low endpoint is to be included
	 * in the returned {@code MapView}
	 * @param toKey high endpoint of the keys in the returned {@code MapView}
	 * @param toInclusive {@code true} if the high endpoint is to be included in
	 * the returned {@code MapView}
	 * @return a {@code MapView} of the portion of this {@code MapView} whose
	 * keys range from {@code fromKey} to {@code toKey}
	 * @throws ClassCastException if {@code fromKey} and {@code toKey} cannot be
	 * compared to one another using this {@code MapView}'s comparator (or, if
	 * the {@code MapView} has no comparator, using natural ordering).
	 * Implementations may, but are not required to, throw this exception if
	 * {@code fromKey} or {@code toKey} cannot be compared to keys currently in
	 * the {@code MapView}.
	 * @throws NullPointerException if {@code fromKey} or {@code toKey} is
	 * {@code null} and this {@code MapView} does not permit {@code null} keys
	 * @throws IllegalArgumentException if {@code fromKey} is greater than
	 * {@code toKey}; or if this {@code MapView} itself has a restricted range,
	 * and {@code fromKey} or {@code toKey} lies outside the bounds of the range
	 */
	NavigableMapView<K, V> subMap(K fromKey, boolean fromInclusive,
			K toKey, boolean toInclusive);

	/**
	 * Returns a {@code MapView} of the portion of this {@code MapView} whose
	 * keys are less than (or equal to, if {@code inclusive} is true)
	 * {@code toKey}. The returned {@code MapView} is backed by this
	 * {@code MapView}, so changes in this {@code MapView} are reflected in the
	 * returned {@code MapView}. The returned {@code MapView} supports all
	 * optional {@code MapView} operations that this {@code MapView} supports.
	 *
	 * @param toKey high endpoint of the keys in the returned {@code MapView}
	 * @param inclusive {@code true} if the high endpoint is to be included in
	 * the returned {@code MapView}
	 * @return a {@code MapView} of the portion of this {@code MapView} whose
	 * keys are less than (or equal to, if {@code inclusive} is true)
	 * {@code toKey}
	 * @throws ClassCastException if {@code toKey} is not compatible with this
	 * {@code MapView}'s comparator (or, if the {@code MapView} has no
	 * comparator, if {@code toKey} does not implement {@link Comparable}).
	 * Implementations may, but are not required to, throw this exception if
	 * {@code toKey} cannot be compared to keys currently in the
	 * {@code MapView}.
	 * @throws NullPointerException if {@code toKey} is {@code null} and this
	 * {@code MapView} does not permit {@code null} keys
	 * @throws IllegalArgumentException if this {@code MapView} itself has a
	 * restricted range, and {@code toKey} lies outside the bounds of the range
	 */
	NavigableMapView<K, V> headMap(K toKey, boolean inclusive);

	/**
	 * Returns a {@code MapView} of the portion of this {@code MapView} whose
	 * keys are greater than (or equal to, if {@code inclusive} is true)
	 * {@code fromKey}. The returned {@code MapView} is backed by this
	 * {@code MapView}, so changes in this {@code MapView} are reflected in the
	 * returned {@code MapView}. The returned {@code MapView} supports all
	 * optional {@code MapView} operations that this {@code MapView} supports.
	 *
	 * @param fromKey low endpoint of the keys in the returned {@code MapView}
	 * @param inclusive {@code true} if the low endpoint is to be included in
	 * the returned {@code MapView}
	 * @return a {@code MapView} of the portion of this {@code MapView} whose
	 * keys are greater than (or equal to, if {@code inclusive} is true)
	 * {@code fromKey}
	 * @throws ClassCastException if {@code fromKey} is not compatible with this
	 * {@code MapView}'s comparator (or, if the {@code MapView} has no
	 * comparator, if {@code fromKey} does not implement {@link Comparable}).
	 * Implementations may, but are not required to, throw this exception if
	 * {@code fromKey} cannot be compared to keys currently in the
	 * {@code MapView}.
	 * @throws NullPointerException if {@code fromKey} is {@code null} and this
	 * {@code MapView} does not permit {@code null} keys
	 * @throws IllegalArgumentException if this {@code MapView} itself has a
	 * restricted range, and {@code fromKey} lies outside the bounds of the
	 * range
	 */
	NavigableMapView<K, V> tailMap(K fromKey, boolean inclusive);

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * Equivalent to {@code subMap(fromKey, true, toKey, false)}.
	 *
	 * @throws ClassCastException {@inheritDoc}
	 * @throws NullPointerException {@inheritDoc}
	 * @throws IllegalArgumentException {@inheritDoc}
	 */
	@Override
	SortedMapView<K, V> subMap(K fromKey, K toKey);

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * Equivalent to {@code headMap(toKey, false)}.
	 *
	 * @throws ClassCastException {@inheritDoc}
	 * @throws NullPointerException {@inheritDoc}
	 * @throws IllegalArgumentException {@inheritDoc}
	 */
	@Override
	SortedMapView<K, V> headMap(K toKey);

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * Equivalent to {@code tailMap(fromKey, true)}.
	 *
	 * @throws ClassCastException {@inheritDoc}
	 * @throws NullPointerException {@inheritDoc}
	 * @throws IllegalArgumentException {@inheritDoc}
	 */
	@Override
	SortedMapView<K, V> tailMap(K fromKey);

	/**
	 * Returns an unmodifiable {@code NavigableMap} containing the same mappings
	 * as this {@code NavigableMapView}.
	 *
	 * @return an unmodifiable {@code NavigableMap} containing the same mappings
	 * as this {@code NavigableMapView}
	 *
	 * @see Views#asView(NavigableMap)
	 */
	@Override
	public NavigableMap<K, V> toMap();
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.collection.view;

import java.util.Comparator;
import java.util.Map;
import java.util.SortedMap;

/**
 * A {@code SortedMapView} represents an unmodifiable group of ordered
 * key-object mappings, usually backed by a {@link java.util.SortedMap}.
 * Generally speaking, whether changes to the backing data-structure are
 * reflected by the {@code MapView} (and its iterators, spliterators and
 * streams) depends on whether the backing data-structure behaves the same way.
 *
 * @param <K> the type of keys maintained by this {@code SortedMapView}
 * @param <V> the type of mapped values
 *
 * @author Jan Kebernik
 */
public interface SortedMapView<K, V> extends MapView<K, V> {

	/**
	 * Returns the comparator used to order the keys in this
	 * {@code SortedMapView}, or {@code null} if this {@code SortedMapView} uses
	 * the {@linkplain Comparable
	 * natural ordering} of its keys.
	 *
	 * @return the comparator used to order the keys in this
	 * {@code SortedMapView}, or {@code null} if this {@code SortedMapView} uses
	 * the natural ordering of its keys
	 */
	Comparator<? super K> comparator();

	/**
	 * Returns a view of the portion of this {@code SortedMapView} whose keys
	 * range from {@code fromKey}, inclusive, to {@code toKey}, exclusive. (If
	 * {@code fromKey} and {@code toKey} are equal, the returned map is empty.)
	 * The returned {@code SortedMapView} is backed by this
	 * {@code SortedMapView}, so changes in this {@code SortedMapView} are
	 * reflected int the returned {@code SortedMapView}. The returned
	 * {@code SortedMapView} supports all optional {@code MapView} operations
	 * that this {@code MapView} supports.
	 *
	 * @param fromKey low endpoint (inclusive) of the keys in the returned
	 * {@code SortedMapView}
	 * @param toKey high endpoint (exclusive) of the keys in the returned
	 * {@code SortedMapView}
	 * @return a {@code SortedMapView} of the portion of this
	 * {@code SortedMapView} whose keys range from {@code fromKey}, inclusive,
	 * to {@code toKey}, exclusive
	 * @throws ClassCastException if {@code fromKey} and {@code toKey} cannot be
	 * compared to one another using this {@code SortedMapView}'s comparator
	 * (or, if the {@code SortedMapView} has no comparator, using natural
	 * ordering). Implementations may, but are not required to, throw this
	 * exception if {@code fromKey} or {@code toKey} cannot be compared to keys
	 * currently in the {@code SortedMapView}.
	 * @throws NullPointerException if {@code fromKey} or {@code toKey} is {@code null}
	 * and this {@code SortedMapView} does not permit {@code null} keys
	 * @throws IllegalArgumentException if {@code fromKey} is greater than
	 * {@code toKey}; or if this {@code SortedMapView} itself has a restricted
	 * range, and {@code fromKey} or {@code toKey} lies outside the bounds of
	 * the range
	 */
	SortedMapView<K, V> subMap(K fromKey, K toKey);

	/**
	 * Returns a {@code SortedMapView} of the portion of this
	 * {@code SortedMapView} whose keys are strictly less than {@code toKey}.
	 * The returned {@code SortedMapView} is backed by this
	 * {@code SortedMapView}, so changes in this {@code SortedMapView} are
	 * reflected in the returned {@code SortedMapView}. The returned
	 * {@code MapView} supports all optional {@code MapView} operations that
	 * this {@code MapView} supports.
	 *
	 * @param toKey high endpoint (exclusive) of the keys in the returned
	 * {@code SortedMapView}
	 * @return a view of the portion of this {@code SortedMapView} whose keys
	 * are strictly less than {@code toKey}
	 * @throws ClassCastException if {@code toKey} is not compatible with this
	 * {@code SortedMapView}'s comparator (or, if the {@code SortedMapView} has
	 * no comparator, if {@code toKey} does not implement {@link Comparable}).
	 * Implementations may, but are not required to, throw this exception if
	 * {@code toKey} cannot be compared to keys currently in the
	 * {@code SortedMapView}.
	 * @throws NullPointerException if {@code toKey} is {@code null} and this
	 * {@code SortedMapView} does not permit {@code null} keys
	 * @throws IllegalArgumentException if this {@code SortedMapView} itself has
	 * a restricted range, and {@code toKey} lies outside the bounds of the
	 * range
	 */
	SortedMapView<K, V> headMap(K toKey);

	/**
	 * Returns a {@code SortedMapView} of the portion of this
	 * {@code SortedMapView} whose keys are greater than or equal to
	 * {@code fromKey}. The returned {@code SortedMapView} is backed by this
	 * {@code SortedMapView}, so changes in this {@code SortedMapView} are
	 * reflected in the returned {@code SortedMapView}. The returned
	 * {@code MapView} supports all optional {@code MapView} operations that
	 * this {@code MapView} supports.
	 *
	 * @param fromKey low endpoint (inclusive) of the keys in the returned
	 * {@code SortedMapView}
	 * @return a view of the portion of this {@code SortedMapView} whose keys
	 * are greater than or equal to {@code fromKey}
	 * @throws ClassCastException if {@code fromKey} is not compatible with this
	 * {@code SortedMapView}'s comparator (or, if the {@code SortedMapView} has
	 * no comparator, if {@code fromKey} does not implement {@link Comparable}).
	 * Implementations may, but are not required to, throw this exception if
	 * {@code fromKey} cannot be compared to keys currently in the
	 * {@code SortedMapView}.
	 * @throws NullPointerException if {@code fromKey} is {@code null} and this
	 * {@code SortedMapView} does not permit {@code null} keys
	 * @throws IllegalArgumentException if this {@code SortedMapView} itself has
	 * a restricted range, and {@code fromKey} lies outside the bounds of the
	 * range
	 */
	SortedMapView<K, V> tailMap(K fromKey);

	/**
	 * Returns the first (lowest) key currently in this {@code SortedMapView}.
	 *
	 * @return the first (lowest) key currently in this {@code SortedMapView}
	 * @throws java.util.NoSuchElementException if this {@code SortedMapView} is
	 * empty
	 */
	K firstKey();

	/**
	 * Returns the last (highest) key currently in this {@code SortedMapView}.
	 *
	 * @return the last (highest) key currently in this {@code SortedMapView}
	 * @throws java.util.NoSuchElementException if this {@code SortedMapView} is
	 * empty
	 */
	K lastKey();

	/**
	 * Returns a {@code SetView} of the keys contained in this
	 * {@code SortedMapView}. The {@code SetView}'s iterator returns the keys in
	 * ascending order. The {@code SetView} is backed by the {@code MapView}, so
	 * changes to the {@code MapView} are reflected in the set. If the
	 * {@code MapView} is modified while an iteration over the {@code SetView}
	 * is in progress, the results of the iteration are undefined.
	 *
	 * @return a {@code SetView} of the keys contained in this
	 * {@code SortedMapView}, sorted in ascending order
	 */
	@Override
	SetView<K> keySet();

	/**
	 * Returns a {@code View} of the values contained in this
	 * {@code SortedMapView}. The {@code View}'s iterator returns the values in
	 * ascending order of the corresponding keys. The {@code View} is backed by
	 * the {@code MapView}, so changes to the {@code MapView} are reflected in
	 * the {@code View}. If the {@code MapView} is modified while an iteration
	 * over the {@code View} is in progress, the results of the iteration are
	 * undefined.
	 *
	 * @return a {@code View} of the values contained in this
	 * {@code SortedMapView}, sorted in ascending key order
	 */
	@Override
	View<V> values();

	/**
	 * Returns a {@code SetView} of the mappings contained in this
	 * {@code SortedMapView}. The {@code SetView}'s iterator returns the entries
	 * in ascending key order. The {@code SetView} is backed by the
	 * {@code MapView}, so changes to the {@code MapView} are reflected in the
	 * {@code SetView}. If the {@code MapView} is modified while an iteration
	 * over the {@code SetView} is in progress, the results of the iteration are
	 * undefined.
	 *
	 * @return a {@code SetView} of the mappings contained in this
	 * {@code SortedMapView}, sorted in ascending key order
	 */
	@Override
	SetView<Map.Entry<K, V>> entrySet();

	/**
	 * Returns an unmodifiable {@code SortedMap} containing the same mappings as
	 * this {@code SortedMapView}.
	 *
	 * @return an unmodifiable {@code SortedMap} containing the same mappings as
	 * this {@code SortedMapView}
	 *
	 * @see Views#asView(SortedMap)
	 */
	@Override
	public SortedMap<K, V> toMap();
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.collection.view;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;

/**
 * Provides a bridge between the {@code Views} API, the {@code Collections} API
 * and array-based APIs.
 *
 * @author Jan Kebernik
 */
public final class Views {

	static final String ERROR_MESSAGE = "Direct modification not supported.";

	private Views() {
	}

	/**
	 * Wraps the specified objects in a new {@code ListView}, which implements
	 * {RandomAccess}.
	 * <p>
	 * Note that the returned {@code View} will reflect changes made to the
	 * specified array.
	 *
	 * @param <T> the type of elements contained in the new {@code View}.
	 * @param elements the array to be returned as a {@code View}.
	 * @return new {@code View} backed by the specified array.
	 */
	public static <T> ListView<T> asView(T... elements) {
		return new ArrayListView(elements, 0, elements.length);
	}

	/**
	 * Wraps the specified array range in a new {@code ListView}, which
	 * implements {RandomAccess}.
	 * <p>
	 * Note that the returned {@code View} will reflect changes made to the
	 * specified array.
	 *
	 * @param <T> the type of elements contained in the new {@code View}.
	 * @param elements the array to be returned as a {@code View}.
	 * @param fromIndex the index of the first element (inclusive) to be
	 * wrapped.
	 * @param toIndex the index of the last element (exclusive) to be wrapped.
	 * @return new {@code View} backed by the specified array range.
	 */
	public static <T> ListView<T> asView(T[] elements, int fromIndex, int toIndex) {
		int len = toIndex - fromIndex;
		if (fromIndex < 0 || len < 0 || fromIndex > elements.length - len) {
			throw new IndexOutOfBoundsException();
		}
		return new ArrayListView(elements, fromIndex, toIndex);
	}

	/**
	 * Wraps the specified {@code Collection} in a new {@code View}. If the
	 * specified {@code Collection} is a {@code Set} or {@code List}, a
	 * {@code View} with the same capabilities will be returned, where
	 * {@code Set} takes precedence over {@code List}.
	 *
	 * @param <T> the type of elements contained in the new {@code View}.
	 * @param collection the {@code Collection} to be returned as a
	 * {@code View}.
	 * @return new {@code View} backed by the specified {@code Collection}.
	 */
	public static <T> View<T> asView(Collection<T> collection) {
		if (collection instanceof CollectionToView.ToView) {
			return ((CollectionToView.ToView) collection).toView();
		}
		if (collection instanceof Set) {
			return _asSetView((Set<T>) collection);
		}
		if (collection instanceof List) {
			return _asListView((List<T>) collection);
		}
		return new DelegatingView<>(Objects.requireNonNull(collection));
	}

	/**
	 * Wraps the specified {@code Set} in a new {@code SetView}. If the
	 * specified {@code Set} is a {@code SortedSet} or {@code NavigableSet}, a
	 * {@code SetView} with the same capabilities will be returned.
	 *
	 * @param <T> the type of elements contained in the new {@code SetView}.
	 * @param set the {@code Set} to be returned as a {@code SetView}.
	 * @return new {@code SetView} backed by the specified {@code Set}.
	 */
	public static <T> SetView<T> asView(Set<T> set) {
		if (set instanceof CollectionToView.ToSetView) {
			return ((CollectionToView.ToSetView) set).toView();
		}
		return _asSetView(set);
	}

	private static <T> SetView<T> _asSetView(Set<T> set) {
		if (set instanceof NavigableSet) {
			return new DelegatingSetView.Navigable<>((NavigableSet<T>) set);
		}
		if (set instanceof SortedSet) {
			return new DelegatingSetView.Sorted<>((SortedSet<T>) set);
		}
		return new DelegatingSetView<>(Objects.requireNonNull(set));
	}

	/**
	 * Wraps the specified {@code SortedSet} in a new {@code SortedSetView}. If
	 * the specified {@code Set} is a {@code NavigableSet}, a {@code SetView}
	 * with the same capabilities will be returned.
	 *
	 * @param <T> the type of elements contained in the new
	 * {@code SortedSetView}.
	 * @param set the {@code SortedSet} to be returned as a
	 * {@code SortedSetView}.
	 * @return new {@code SortedSetView} backed by the specified
	 * {@code SortedSet}.
	 */
	public static <T> SortedSetView<T> asView(SortedSet<T> set) {
		if (set instanceof CollectionToView.ToSortedSetView) {
			return ((CollectionToView.ToSortedSetView) set).toView();
		}
		if (set instanceof NavigableSet) {
			return new DelegatingSetView.Navigable<>((NavigableSet<T>) set);
		}
		return new DelegatingSetView.Sorted<>(Objects.requireNonNull(set));
	}

	/**
	 * Wraps the specified {@code NavigableSet} in a new
	 * {@code NavigableSetView}.
	 *
	 * @param <T> the type of elements contained in the new
	 * {@code NavigableSetView}.
	 * @param set the {@code NavigableSet} to be returned as a
	 * {@code NavigableSetView}.
	 * @return new {@code NavigableSetView} backed by the specified
	 * {@code NavigableSet}.
	 */
	public static <T> NavigableSetView<T> asView(NavigableSet<T> set) {
		if (set instanceof CollectionToView.ToNavigableSetView) {
			return ((CollectionToView.ToNavigableSetView) set).toView();
		}
		return new DelegatingSetView.Navigable<>(Objects.requireNonNull(set));
	}

	/**
	 * Wraps the specified {@code List} in a new {@code ListView}. If the
	 * specified {@code List} implements
	 * {@link java.util.RandomAccess RandomAccess}, the returned
	 * {@code ListView} will also implement {@code RandomAccess} and its
	 * {@link ListView#asCollection() asCollection()} method will produce
	 * {@code List}s implementing {@code RandomAccess}.
	 *
	 * @param <T> the type of elements contained in the new {@code ListView}.
	 * @param list the {@code List} to be returned as a {@code ListView}.
	 * @return new {@code ListView} backed by the specified {@code List}.
	 */
	public static <T> ListView<T> asView(List<T> list) {
		if (list instanceof CollectionToView.ToListView) {
			return ((CollectionToView.ToListView) list).toView();
		}
		return _asListView(list);
	}

	private static <T> ListView<T> _asListView(List<T> list) {
		if (list instanceof RandomAccess) {
			return new DelegatingListView.Random<>(Objects.requireNonNull(list));
		}
		return new DelegatingListView<>(Objects.requireNonNull(list));
	}

	/**
	 * Wraps the specified {@code Map} in a new {@code MapView}. If the
	 * specified {@code Map} is a {@code SortedMap} or {@code NavigableMap}, a
	 * {@code MapView} with the same capabilities will be returned.
	 *
	 * @param <K> the type of keys contained in the new {@code MapView}.
	 * @param <V> the type of values contained in the new {@code MapView}.
	 * @param map the {@code Map} to be returned as a {@code MapView}.
	 * @return new {@code MapView} backed by the specified {@code Map}.
	 */
	public static <K, V> MapView<K, V> asView(Map<K, V> map) {
		if (map instanceof CollectionToView.ToMapView) {
			return ((CollectionToView.ToMapView) map).toView();
		}
		if (map instanceof NavigableMap) {
			return new DelegatingMapView.Navigable<>((NavigableMap<K, V>) map);
		}
		if (map instanceof SortedMap) {
			return new DelegatingMapView.Sorted<>((SortedMap<K, V>) map);
		}
		return new DelegatingMapView<>(Objects.requireNonNull(map));
	}

	/**
	 * Wraps the specified {@code SortedMap} in a new {@code SortedMapView}. If
	 * the specified {@code Map} is a {@code NavigableMap}, a {@code MapView}
	 * with the same capabilities will be returned.
	 *
	 * @param <K> the type of keys contained in the new {@code SortedMapView}.
	 * @param <V> the type of values contained in the new {@code SortedMapView}.
	 * @param map the {@code SortedMap} to be returned as a
	 * {@code SortedMapView}.
	 * @return new {@code SortedMapView} backed by the specified
	 * {@code SortedMap}.
	 */
	public static <K, V> SortedMapView<K, V> asView(SortedMap<K, V> map) {
		if (map instanceof CollectionToView.ToSortedMapView) {
			return ((CollectionToView.ToSortedMapView) map).toView();
		}
		if (map instanceof NavigableMap) {
			return new DelegatingMapView.Navigable<>((NavigableMap<K, V>) map);
		}
		return new DelegatingMapView.Sorted<>(Objects.requireNonNull(map));
	}

	/**
	 * Wraps the specified {@code NavigableMap} in a new
	 * {@code NavigableMapView}.
	 *
	 * @param <K> the type of keys contained in the new
	 * {@code NavigableMapView}.
	 * @param <V> the type of values contained in the new
	 * {@code NavigableMapView}.
	 * @param map the {@code NavigableMap} to be returned as a
	 * {@code NavigableMapView}.
	 * @return new {@code NavigableMapView} backed by the specified
	 * {@code NavigableMap}.
	 */
	public static <K, V> NavigableMapView<K, V> asView(NavigableMap<K, V> map) {
		if (map instanceof CollectionToView.ToNavigableMapView) {
			return ((CollectionToView.ToNavigableMapView) map).toView();
		}
		return new DelegatingMapView.Navigable<>(Objects.requireNonNull(map));
	}
}

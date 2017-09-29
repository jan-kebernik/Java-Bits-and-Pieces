/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.collection.view;

import java.util.ConcurrentModificationException;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * A {@code MapView} represents an unmodifiable group of key-object mappings,
 * usually backed by a {@link java.util.Map}. Generally speaking, whether changes
 * to the backing data-structure are reflected by the {@code MapView} (and its
 * iterators, spliterators and streams) depends on whether the backing
 * data-structure behaves the same way.
 *
 * @param <K> the type of keys maintained by this {@code MapView}
 * @param <V> the type of mapped values
 *
 * @author Jan Kebernik
 */
public interface MapView<K, V> {

	/**
	 * Returns the number of key-value mappings in this {@code MapView}. If the
	 * {@code MapView} contains more than {@code Integer.MAX_VALUE} elements,
	 * returns {@code Integer.MAX_VALUE}.
	 *
	 * @return the number of key-value mappings in this {@code MapView}
	 */
	int size();

	/**
	 * Returns {@code true} if this {@code MapView} contains no key-value
	 * mappings.
	 *
	 * @return {@code true} if this {@code MapView} contains no key-value
	 * mappings
	 */
	boolean isEmpty();

	/**
	 * Returns {@code true} if this {@code MapView} contains a mapping for the
	 * specified key. More formally, returns {@code true} if and only if this
	 * {@code MapView} contains a mapping for a key {@code k} such that
	 * {@code (key == null ? k == null : key.equals(k))}. (There can be at most
	 * one such mapping.)
	 *
	 * @param key key whose presence in this {@code MapView} is to be tested
	 * @return {@code true} if this map contains a mapping for the specified key
	 * @throws ClassCastException if the key is of an inappropriate type for
	 * this {@code MapView} (optional)
	 * @throws NullPointerException if the specified key is null and this map
	 * does not permit null keys (optional)
	 */
	boolean containsKey(Object key);

	/**
	 * Returns {@code true} if this {@code MapView} maps one or more keys to the
	 * specified value. More formally, returns {@code true} if and only if this
	 * {@code MapView} contains at least one mapping to a value {@code v} such
	 * that {@code (value == null ? v == null : value.equals(v))}. This
	 * operation will probably require time linear in the {@code MapView} size
	 * for most implementations of the {@code MapView} interface.
	 *
	 * @param value value whose presence in this {@code MapView} is to be tested
	 * @return {@code true} if this {@code MapView} maps one or more keys to the
	 * specified value
	 * @throws ClassCastException if the value is of an inappropriate type for
	 * this {@code MapView} (optional)
	 * @throws NullPointerException if the specified value is null and this map
	 * does not permit null values (optional)
	 */
	boolean containsValue(Object value);

	/**
	 * Returns the value to which the specified key is mapped, or {@code null}
	 * if this {@code MapView} contains no mapping for the key.
	 *
	 * <p>
	 * More formally, if this {@code MapView} contains a mapping from a key
	 * {@code k} to a value {@code v} such that {@code (key==null ? k==null :
	 * key.equals(k))}, then this method returns {@code v}; otherwise it returns
	 * {@code null}. (There can be at most one such mapping.)
	 *
	 * <p>
	 * If this {@code MapView} permits null values, then a return value of
	 * {@code null} does not <i>necessarily</i> indicate that the
	 * {@code MapView} contains no mapping for the key; it's also possible that
	 * the {@code MapView} explicitly maps the key to {@code null}. The {@link #containsKey
	 * containsKey} operation may be used to distinguish these two cases.
	 *
	 * @param key the key whose associated value is to be returned
	 * @return the value to which the specified key is mapped, or {@code null}
	 * if this {@code MapView} contains no mapping for the key
	 * @throws ClassCastException if the key is of an inappropriate type for
	 * this {@code MapView} (optional)
	 * @throws NullPointerException if the specified key is null and this
	 * {@code MapView} does not permit null keys (optional)
	 */
	V get(Object key);

	/**
	 * Returns a {@link SetView} of the keys contained in this {@code MapView}.
	 * The {@code SetView} is backed by the {@code MapView}, so changes to the
	 * {@code MapView} are reflected in the {@code SetView}. If the
	 * {@code MapView} is modified while an iteration over the {@code SetView}
	 * is in progress, the results of the iteration are undefined.
	 *
	 * @return a {@code SetView} of the keys contained in this {@code MapView}
	 */
	SetView<K> keySet();

	/**
	 * Returns a {@link View} of the values contained in this {@code MapView}.
	 * The collection is backed by the {@code MapView}, so changes to the
	 * {@code MapView} are reflected in the collection. If the {@code MapView}
	 * is modified while an iteration over the collection is in progress, the
	 * results of the iteration are undefined.
	 *
	 * @return a {@link View} of the values contained in this {@code MapView}
	 */
	View<V> values();

	/**
	 * Returns a {@link SetView} of the mappings contained in this
	 * {@code MapView}. The {@code SetView} is backed by the {@code MapView}, so
	 * changes to the {@code MapView} are reflected in the {@code SetView}. If
	 * the {@code MapView} is modified while an iteration over the
	 * {@code SetView} is in progress, the results of the iteration are
	 * undefined.
	 *
	 * @return a {@code SetView} of the mappings contained in this
	 * {@code MapView}
	 */
	SetView<Map.Entry<K, V>> entrySet();

	/**
	 * Compares the specified object with this {@code MapView} for equality.
	 * Returns {@code true} if the given object is also a {@code MapView} and
	 * the two {@code MapView}s represent the same mappings. More formally, two
	 * {@code MapView}s {@code m1} and {@code m2} represent the same mappings if
	 * {@code m1.entrySet().equals(m2.entrySet())}. This ensures that the
	 * {@code equals} method works properly across different implementations of
	 * the {@code MapView} interface.
	 *
	 * @param o object to be compared for equality with this {@code MapView}
	 * @return {@code true} if the specified object is equal to this
	 * {@code MapView}
	 */
	@Override
	boolean equals(Object o);

	/**
	 * Returns the hash code value for this {@code MapView}. The hash code of a
	 * {@code MapView} is defined to be the sum of the hash codes of each entry
	 * in the {@code MapView}'s {@code entrySet()} view. This ensures that
	 * {@code m1.equals(m2)} implies that {@code m1.hashCode() == m2.hashCode()}
	 * for any two maps {@code m1} and {@code m2}, as required by the general
	 * contract of {@link Object#hashCode}.
	 *
	 * @return the hash code value for this {@code MapView}
	 * @see Map.Entry#hashCode()
	 * @see Object#equals(Object)
	 * @see #equals(Object)
	 */
	@Override
	int hashCode();

	/**
	 * Returns the value to which the specified key is mapped, or
	 * {@code defaultValue} if this {@code MapView} contains no mapping for the
	 * key.
	 *
	 * @implSpec The default implementation makes no guarantees about
	 * synchronization or atomicity properties of this method. Any
	 * implementation providing atomicity guarantees must override this method
	 * and document its concurrency properties.
	 *
	 * @param key the key whose associated value is to be returned
	 * @param defaultValue the default mapping of the key
	 * @return the value to which the specified key is mapped, or
	 * {@code defaultValue} if this {@code MapView} contains no mapping for the
	 * key
	 * @throws ClassCastException if the key is of an inappropriate type for
	 * this {@code MapView} (optional)
	 * @throws NullPointerException if the specified key is null and this
	 * {@code MapView} does not permit null keys (optional)
	 */
	default V getOrDefault(Object key, V defaultValue) {
		V v;
		return (((v = get(key)) != null) || containsKey(key))
				? v
				: defaultValue;
	}

	/**
	 * Performs the given action for each entry in this {@code MapView} until
	 * all entries have been processed or the action throws an exception. Unless
	 * otherwise specified by the implementing class, actions are performed in
	 * the order of entry set iteration (if an iteration order is specified.)
	 * Exceptions thrown by the action are relayed to the caller.
	 *
	 * @implSpec The default implementation is equivalent to, for this
	 * {@code MapView}: 	 <pre> {@code
	 * for (Map.Entry<K, V> entry : mapView.entrySet())
	 *     action.accept(entry.getKey(), entry.getValue());
	 * }</pre>
	 *
	 * The default implementation makes no guarantees about synchronization or
	 * atomicity properties of this method. Any implementation providing
	 * atomicity guarantees must override this method and document its
	 * concurrency properties.
	 *
	 * @param action The action to be performed for each entry
	 * @throws NullPointerException if the specified action is null
	 * @throws ConcurrentModificationException if an entry is found to be
	 * removed during iteration
	 */
	default void forEach(BiConsumer<? super K, ? super V> action) {
		Objects.requireNonNull(action);
		for (Map.Entry<K, V> entry : entrySet()) {
			K k;
			V v;
			try {
				k = entry.getKey();
				v = entry.getValue();
			} catch (IllegalStateException ise) {
				// this usually means the entry is no longer in the map.
				throw new ConcurrentModificationException(ise);
			}
			action.accept(k, v);
		}
	}

	/**
	 * Returns an unmodifiable {@code Map} containing the same mappings as this
	 * {@code MapView}.
	 *
	 * @return an unmodifiable {@code Map} containing the same mappings as this
	 * {@code MapView}
	 *
	 * @see Views#asView(Map)
	 */
	public Map<K, V> toMap();
}

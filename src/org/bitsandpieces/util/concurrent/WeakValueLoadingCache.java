/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.concurrent;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 *
 * @author Jan Kebernik
 */
public class WeakValueLoadingCache<K, V> implements LoadingCache<K, V> {

	private final ReferenceQueue<? super V> queue = new ReferenceQueue<>();
	private final ConcurrentMap<K, Ref<K, V>> map = new ConcurrentHashMap<>();
	private final Loader<K, V> loader;

	public WeakValueLoadingCache(Loader<K, V> loader) {
		Objects.requireNonNull(loader);
		this.loader = loader;
	}

	@Override
	public V load(K key) {
		for (Object p; (p = queue.poll()) != null;) {
			// remove known stale entries if still mapped.
			this.map.remove(((Ref<K, V>) p).key, p);
		}
		while (true) {
			V val = this.map.compute(key, (K k, Ref<K, V> r) -> {
				if (r != null) {
					V v = r.get();
					if (v != null) {
						// ref is not stale. for now.
						return r;
					}
				}
				// no mapping or stale. put new ref.
				return new Ref<>(k, this.loader.load(k), this.queue);
			}).get();
			if (val != null) {
				return val;
			}
			// ref stale after all. retry.
		}
	}

	@Override
	public Loader<K, V> loader() {
		return this.loader;
	}

	private static final class Ref<K, V> extends WeakReference<V> {

		private final K key;

		public Ref(K key, V value, ReferenceQueue<? super V> q) {
			super(value, q);
			this.key = key;
		}
	}
}

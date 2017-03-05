/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.concurrent;

/**
 *
 * @author Jan Kebernik
 */
public interface LoadingCache<K, V> {

	public Loader<K, V> loader();

	public V load(K key);

	public static interface Loader<K, V> {

		public V load(K key);
	}
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.collection;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;
import org.bitsandpieces.util.Format;
import org.bitsandpieces.util.Formats;
import org.bitsandpieces.util.function.IntObjBiFunction;

/**
 * An array-like like construct that allows for storing up to 64 elements while
 * only requiring space to be allocated for the actual elements stored, whithout
 * sacrificing random access. Not thread-safe.
 *
 * @param <V> the type of element stored by this {@code CompactArray64}
 * @author Jan Kebernik
 */
public class CompactArray64<V> implements Iterable<V> {

	private static final Object[] EMPTY = {};

	/*
	 * The table is adressed via a 16-bit bitmap defining the presence of
	 * elements for each index. The returned 64-bit long contains 16 4-bit
	 * indices into the actual array. Wether a specified index is contained in
	 * the array then only requires this sub-index to be tested against the
	 * array size. For example, the translation for bitmap
	 * 0b1010_1101_0101_0111 would simply be 
	 * 0x9F8F_76F5_F4F3_F210. 
	 * Each 4 bits thus qualify as a direct index into the array
	 *  0  1  2  3  4  5  6   7   8   9
	 * [e0,e1,e2,e4,e6,e8,e10,e11,e13,e15].
	 * Note that this idea is expanded upon below, in order support arrays of sizes up
	 * to 64 by simply maintaining a 64-bit bitmap, consisting of 4 16-bit 
	 * bitmaps, accompanied by another 64-bit value storing the offsets and limits
	 * of each of the 4 (virtual) sub-arrays. This is prudent because a 16-element
	 * array is barely ever useful, and because it only requires another 8 bytes to 
	 * accomplish.
	 */
	private static final long[] T = new long[65536];

	static {
		for (int i = 0; i != 65536; i++) {
			long l = 0L, c = 15L;
			for (int j = i, b = 0; b != 64; b += 4, j >>>= 1) {
				if ((j & 1) == 1) {
					l |= c-- << b;
				}
			}
			T[i] = ~l;
		}
	}

	private static int translate(int bitmap, int index) {
		return ((int) (T[bitmap & 0xffff] >>> (index << 2))) & 0xf;
	}

	private static long addMask(int shift) {
		return (0x00_00_00_01_01_01_01_00L << shift) & 0x00_00_00_01_01_01_01_00L;
	}

	protected static final int size(long data) {
		return ((int) (data >>> 32)) & 0xff;
	}

	protected long bitmap;
	protected long data;	// the highest 3 bytes can be freely used for other things by sub-classes
	protected Object[] array;

	public CompactArray64() {
		this(0L, 0L, EMPTY);
	}

	protected CompactArray64(long bitmap, long data, Object[] array) {
		this.bitmap = bitmap;
		this.data = data;
		this.array = array;
	}

	/**
	 * Returns the element stored at the specified {@code index} or {@code null}
	 * if no such element is stored.
	 *
	 * @param index index of the element to return
	 * @return the element stored at the specified {@code index} or {@code null}
	 * if no such element is stored.
	 * @throws IndexOutOfBoundsException if {@code index} is less than {@code 0}
	 * or greater {@code 63}.
	 */
	public V get(int index) {
		if (index < 0 || index > 63) {
			throw new IndexOutOfBoundsException();
		}
		return doGet(index);
	}

	protected V doGet(int index) {
		long bm = this.bitmap;
		long dat = this.data;
		int hi = index & 0x30;
		int off = ((int) (dat >>> (hi >>> 1))) & 0xffff;
		int idx = (off & 0xff) + translate((int) (bm >>> hi), index & 0x0f);
		return (idx < (off >>> 8)) ? (V) array[idx] : null;
	}

	/**
	 * Sets a new element at the specified {@code index}.
	 *
	 * @param index index at which to set the new element
	 * @param elem the new element to be stored
	 * @return the element previously stored at the specified {@code index} or
	 * {@code null} if no such element was stored.
	 * @throws IndexOutOfBoundsException if {@code index} is less than {@code 0}
	 * or greater {@code 63}.
	 * @throws NullPointerException if {@code elem == null}.
	 */
	public V set(int index, V elem) {
		if (index < 0 || index > 63) {
			throw new IndexOutOfBoundsException();
		}
		Objects.requireNonNull(elem);
		return doSet(index, elem);
	}

	protected V doSet(int index, V elem) {
		Object[] a = this.array;
		long bmp0 = this.bitmap;
		long dat0 = this.data;
		int lo = index & 0x0f;
		int hi = index & 0x30;
		int hix = hi >>> 1;
		int off0 = ((int) (dat0 >>> hix)) & 0xffff;
		int idx0 = (off0 & 0xff) + translate((int) (bmp0 >>> hi), lo);
		if (idx0 < (off0 >>> 8)) {
			// element exists at index
			V old = (V) a[idx0];
			a[idx0] = elem;
			return old;
		}
		_add(a, index, bmp0, dat0, lo, hi, hix, elem);
		return null;
	}

	private void _add(Object[] a, int index, long bmp0, long dat0, int lo, int hi, int hix, Object elem) {
		long bmp1 = bmp0 | (1L << index);
		long dat1 = dat0 + addMask(hix);
		int idx1 = (((int) (dat1 >>> hix)) & 0xff) + translate((int) (bmp1 >>> hi), lo);
		int max = size(dat1);
		if (max > a.length) {
			int newCap = Math.min(64, a.length + (a.length >>> 3) + 1);
			Object[] b = new Object[newCap];
			System.arraycopy(a, 0, b, 0, idx1);
			System.arraycopy(a, idx1, b, idx1 + 1, max - (idx1 + 1));
			b[idx1] = elem;
			this.array = b;
		} else {
			System.arraycopy(a, idx1, a, idx1 + 1, max - (idx1 + 1));
			a[idx1] = elem;
		}
		this.bitmap = bmp1;
		this.data = dat1;
	}

	/**
	 * Removes the element stored at the specified {@code index}, if any.
	 *
	 * @param index index of the element to remove
	 * @return the element previously stored at the specified {@code index} or
	 * {@code null} if no such element was stored.
	 * @throws IndexOutOfBoundsException if {@code index} is less than {@code 0}
	 * or greater {@code 63}.
	 */
	public V remove(int index) {
		if (index < 0 || index > 63) {
			throw new IndexOutOfBoundsException();
		}
		return doRemove(index);
	}

	protected V doRemove(int index) {
		long bmp0 = this.bitmap;
		long dat0 = this.data;
		int lo = index & 0x0f;
		int hi = index & 0x30;
		int hix = hi >>> 1;
		int off0 = ((int) (dat0 >>> hix)) & 0xffff;
		int idx0 = (off0 & 0xff) + translate((int) (bmp0 >>> hi), lo);
		if (idx0 < (off0 >>> 8)) {
			// element exists at index
			Object[] a = this.array;
			V old = (V) a[idx0];
			_remove(a, index, bmp0, dat0, idx0, hix);
			return old;
		}
		return null;
	}

	private void _remove(Object[] a, int index, long bmp0, long dat0, int idx0, int hix) {
		long bmp1 = bmp0 & ~(1L << index);
		long dat1 = dat0 - addMask(hix);
		int max = size(dat1);
		if (max <= (a.length >>> 1)) {
			// at most half the array is used
			Object[] b = new Object[max];
			System.arraycopy(a, 0, b, 0, idx0);
			System.arraycopy(a, idx0 + 1, b, idx0, max - idx0);
			this.array = b;
		} else {
			System.arraycopy(a, idx0 + 1, a, idx0, max - idx0);
			a[max] = null;	// help out GC
		}
		this.bitmap = bmp1;
		this.data = dat1;
	}

	public V compute(int index, IntObjBiFunction<? super V, ? extends V> remappingFunction) {
		if (index < 0 || index > 63) {
			throw new IndexOutOfBoundsException();
		}
		Objects.requireNonNull(remappingFunction);
		return doCompute(index, remappingFunction);
	}

	// return new associated value, or null if none
	protected V doCompute(int index, IntObjBiFunction<? super V, ? extends V> f) {
		Object[] a = this.array;
		long bmp0 = this.bitmap;
		long dat0 = this.data;
		int lo = index & 0x0f;
		int hi = index & 0x30;
		int hix = hi >>> 1;
		int off0 = ((int) (dat0 >>> hix)) & 0xffff;
		int idx0 = (off0 & 0xff) + translate((int) (bmp0 >>> hi), lo);
		if (idx0 < (off0 >>> 8)) {
			// element exists at index (never null)
			V old = (V) a[idx0];
			V val = f.apply(index, old);
			if (val == null) {
				// remove current mapping
				_remove(a, index, bmp0, dat0, idx0, hix);
				return null;
			}
			// replace current mapping
			a[idx0] = val;
			return val;
		}
		// no element mapped to index
		V val = f.apply(index, null);
		if (val != null) {
			_add(a, index, bmp0, dat0, lo, hi, hix, val);
		}
		return val;
	}

	/**
	 * Returns the number of elements currently stored in this
	 * {@code CompactArray64}.
	 *
	 * @return the number of elements currently stored in this
	 * {@code CompactArray64}.
	 */
	public int size() {
		return size(this.data);
	}

	@Override
	public Iterator<V> iterator() {
		return new Iterator<V>() {

			private int ret = -1;
			private int idx = 0;
			private int max = size();

			@Override
			public boolean hasNext() {
				return this.idx < this.max;
			}

			@Override
			public V next() {
				int i = this.idx;
				if (i < this.max) {
					return (V) CompactArray64.this.array[this.idx = i + 1];
				}
				throw new IllegalStateException();
			}

			@Override
			public void forEachRemaining(Consumer<? super V> action) {
				int i = this.idx;
				int m = this.max;
				for (; i < m; i++) {
					action.accept((V) CompactArray64.this.array[i]);
				}
				this.idx = i;
				this.ret = i - 1;
			}

			@Override
			public void remove() {
				int r = this.ret;
				if (r < 0) {
					throw new NoSuchElementException();
				}
				CompactArray64.this.doRemove(r);
				this.max--;
				this.ret = -1;
			}
		};
	}

	@Override
	public void forEach(Consumer<? super V> action) {
		Object[] a = this.array;
		int max = size(this.data);
		for (int i = 0; i < max; i++) {
			action.accept((V) a[i]);
		}
	}
	
	private static final Format BIN = Formats.BIN.untruncated();
	
	@Override
	public String toString() {
		int max = size(this.data) - 1;
		if (max < 0) {
			return BIN.toString(this.bitmap) + ": []";
		}
		StringBuilder sb = new StringBuilder();
		sb.append(BIN.toString(this.bitmap)).append(": [");
		Object[] a = this.array;
		for (int i = 0; i < max; i++) {
			sb.append(a[i]).append(", ");
		}
		sb.append(a[max]).append(']');
		return sb.toString();
	}
}

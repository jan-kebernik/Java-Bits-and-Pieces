/*
 * Copyright (c) 2009, 2013, Oracle and/or its affiliates. All rights reserved.
 * Copyright 2009 Google Inc.  All Rights Reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */
package org.bitsandpieces.util;

import java.util.Comparator;
import org.bitsandpieces.util.collection.primitive.PrimitiveComparator.BooleanComparator;
import org.bitsandpieces.util.collection.primitive.PrimitiveComparator.ByteComparator;
import org.bitsandpieces.util.collection.primitive.PrimitiveComparator.CharComparator;
import org.bitsandpieces.util.collection.primitive.PrimitiveComparator.DoubleComparator;
import org.bitsandpieces.util.collection.primitive.PrimitiveComparator.FloatComparator;
import org.bitsandpieces.util.collection.primitive.PrimitiveComparator.IntComparator;
import org.bitsandpieces.util.collection.primitive.PrimitiveComparator.LongComparator;
import org.bitsandpieces.util.collection.primitive.PrimitiveComparator.ShortComparator;

/**
 * Provides a Comparator-based sorting solution for all primitive types.
 *
 * @author Jan Kebernik
 */
abstract class PrimitiveTimSort<A, C> {

	private static final int MIN_MERGE = 32;
	private static final int MIN_GALLOP = 7;
	private static final int INITIAL_TMP_STORAGE_LENGTH = 256;

	private A tmp;
	private int tmpBase;
	private int tmpLen;

	private PrimitiveTimSort() {
		// prevent external instantiation.
	}

	abstract A newArray(A old, int length);

	abstract void assign(A a, int i1, A b, int i2);

	abstract int compare(A a, int i1, A b, int i2, C comp);

	private A ensureCapacity(int minCapacity, A old, int aLen) {
		if (tmpLen < minCapacity) {
			int newSize = minCapacity;
			newSize |= newSize >> 1;
			newSize |= newSize >> 2;
			newSize |= newSize >> 4;
			newSize |= newSize >> 8;
			newSize |= newSize >> 16;
			newSize++;
			if (newSize < 0) {
				newSize = minCapacity;
			} else {
				newSize = Math.min(newSize, aLen >>> 1);
			}
			A newArray = newArray(old, newSize);
			tmp = newArray;
			tmpLen = newSize;
			tmpBase = 0;
			return newArray;
		}
		return tmp;
	}

	private int countRunAndMakeAscending(A a, A key, int lo, int hi,
			C c) {
		int runHi = lo + 1;
		if (runHi == hi) {
			return 1;
		}
		if (compare(a, runHi++, a, lo, c) < 0) {
			while (runHi < hi && compare(a, runHi, a, runHi - 1, c) < 0) {
				runHi++;
			}
			reverseRange(a, key, lo, runHi);
		} else {
			while (runHi < hi && compare(a, runHi, a, runHi - 1, c) >= 0) {
				runHi++;
			}
		}
		return runHi - lo;
	}

	private void reverseRange(A a, A key, int lo, int hi) {
		hi--;
		while (lo < hi) {
			assign(key, 0, a, lo);
			assign(a, lo++, a, hi);
			assign(a, hi--, key, 0);
		}
	}

	private static int minRunLength(int n) {
		assert n >= 0;
		int r = 0;
		while (n >= MIN_MERGE) {
			r |= (n & 1);
			n >>= 1;
		}
		return n + r;
	}

	@SuppressWarnings({"fallthrough", "SuspiciousSystemArraycopy"})
	private void binarySort(A a, A pivot, int lo, int hi, int start,
			C c) {
		if (start == lo) {
			start++;
		}
		for (; start < hi; start++) {
			assign(pivot, 0, a, start);
			int left = lo;
			int right = start;
			while (left < right) {
				int mid = (left + right) >>> 1;
				if (compare(pivot, 0, a, mid, c) < 0) {
					right = mid;
				} else {
					left = mid + 1;
				}
			}
			int n = start - left;
			switch (n) {
				case 2:
					assign(a, left + 2, a, left + 1);
				case 1:
					assign(a, left + 1, a, left);
					break;
				default:
					System.arraycopy(a, left, a, left + 1, n);
			}
			assign(a, left, pivot, 0);
		}
	}

	private int mergeAt(A a, int aLen, C c, A key, int i, int minGallop, int[] runBase, int[] runLen, int stackSize) {
		int base1 = runBase[i];
		int len1 = runLen[i];
		int base2 = runBase[i + 1];
		int len2 = runLen[i + 1];
		runLen[i] = len1 + len2;
		if (i == stackSize - 3) {
			runBase[i + 1] = runBase[i + 2];
			runLen[i + 1] = runLen[i + 2];
		}
		assign(key, 0, a, base2);
		int k = gallopRight(key, a, base1, len1, 0, c);
		base1 += k;
		len1 -= k;
		if (len1 == 0) {
			return minGallop;
		}
		assign(key, 0, a, base1 + len1 - 1);
		len2 = gallopLeft(key, a, base2, len2, len2 - 1, c);
		return len2 == 0 ? minGallop
				: len1 <= len2
						? mergeLo(a, aLen, c, key, base1, len1, base2, len2, minGallop)
						: mergeHi(a, aLen, c, key, base1, len1, base2, len2, minGallop);
	}

	private int gallopLeft(A key, A a, int base, int len, int hint,
			C c) {
		int lastOfs = 0;
		int ofs = 1;
		if (compare(key, 0, a, base + hint, c) > 0) {
			int maxOfs = len - hint;
			while (ofs < maxOfs && compare(key, 0, a, base + hint + ofs, c) > 0) {
				lastOfs = ofs;
				ofs = (ofs << 1) + 1;
				if (ofs <= 0) {
					ofs = maxOfs;
				}
			}
			if (ofs > maxOfs) {
				ofs = maxOfs;
			}
			lastOfs += hint;
			ofs += hint;
		} else {
			final int maxOfs = hint + 1;
			while (ofs < maxOfs && compare(key, 0, a, base + hint - ofs, c) <= 0) {
				lastOfs = ofs;
				ofs = (ofs << 1) + 1;
				if (ofs <= 0) {
					ofs = maxOfs;
				}
			}
			if (ofs > maxOfs) {
				ofs = maxOfs;
			}
			int _tmp = lastOfs;
			lastOfs = hint - ofs;
			ofs = hint - _tmp;
		}
		lastOfs++;
		while (lastOfs < ofs) {
			int m = lastOfs + ((ofs - lastOfs) >>> 1);

			if (compare(key, 0, a, base + m, c) > 0) {
				lastOfs = m + 1;
			} else {
				ofs = m;
			}
		}
		return ofs;
	}

	private int gallopRight(A key, A a, int base, int len,
			int hint, C c) {
		int ofs = 1;
		int lastOfs = 0;
		if (compare(key, 0, a, base + hint, c) < 0) {
			int maxOfs = hint + 1;
			while (ofs < maxOfs && compare(key, 0, a, base + hint - ofs, c) < 0) {
				lastOfs = ofs;
				ofs = (ofs << 1) + 1;
				if (ofs <= 0) {
					ofs = maxOfs;
				}
			}
			if (ofs > maxOfs) {
				ofs = maxOfs;
			}
			int _tmp = lastOfs;
			lastOfs = hint - ofs;
			ofs = hint - _tmp;
		} else {
			int maxOfs = len - hint;
			while (ofs < maxOfs && compare(key, 0, a, base + hint + ofs, c) >= 0) {
				lastOfs = ofs;
				ofs = (ofs << 1) + 1;
				if (ofs <= 0) {
					ofs = maxOfs;
				}
			}
			if (ofs > maxOfs) {
				ofs = maxOfs;
			}
			lastOfs += hint;
			ofs += hint;
		}
		lastOfs++;
		while (lastOfs < ofs) {
			int m = lastOfs + ((ofs - lastOfs) >>> 1);
			if (compare(key, 0, a, base + m, c) < 0) {
				ofs = m;
			} else {
				lastOfs = m + 1;
			}
		}
		return ofs;
	}

	@SuppressWarnings("SuspiciousSystemArraycopy")
	private int mergeLo(A a, int aLen, C c, A key, int base1, int len1, int base2, int len2, int minGallop) {
		A _tmp = ensureCapacity(len1, a, aLen);
		int cursor1 = tmpBase;
		int cursor2 = base2;
		int dest = base1;
		System.arraycopy(a, base1, _tmp, cursor1, len1);
		assign(a, dest++, a, cursor2++);
		if (--len2 == 0) {
			System.arraycopy(_tmp, cursor1, a, dest, len1);
			return minGallop;
		}
		if (len1 == 1) {
			System.arraycopy(a, cursor2, a, dest, len2);
			assign(a, dest + len2, _tmp, cursor1);
			return minGallop;
		}
		outer:
		while (true) {
			int count1 = 0;
			int count2 = 0;
			do {
				if (compare(a, cursor2, _tmp, cursor1, c) < 0) {
					assign(a, dest++, a, cursor2++);
					count2++;
					count1 = 0;
					if (--len2 == 0) {
						break outer;
					}
				} else {
					assign(a, dest++, _tmp, cursor1++);
					count1++;
					count2 = 0;
					if (--len1 == 1) {
						break outer;
					}
				}
			} while ((count1 | count2) < minGallop);
			do {
				assign(key, 0, a, cursor2);
				count1 = gallopRight(key, _tmp, cursor1, len1, 0, c);
				if (count1 != 0) {
					System.arraycopy(_tmp, cursor1, a, dest, count1);
					dest += count1;
					cursor1 += count1;
					len1 -= count1;
					if (len1 <= 1) {
						break outer;
					}
				}
				assign(a, dest++, a, cursor2++);
				if (--len2 == 0) {
					break outer;
				}
				assign(key, 0, _tmp, cursor1);
				count2 = gallopLeft(key, a, cursor2, len2, 0, c);
				if (count2 != 0) {
					System.arraycopy(a, cursor2, a, dest, count2);
					dest += count2;
					cursor2 += count2;
					len2 -= count2;
					if (len2 == 0) {
						break outer;
					}
				}
				assign(a, dest++, _tmp, cursor1++);
				if (--len1 == 1) {
					break outer;
				}
				minGallop--;
			} while (count1 >= MIN_GALLOP | count2 >= MIN_GALLOP);
			if (minGallop < 0) {
				minGallop = 0;
			}
			minGallop += 2;
		}
		if (minGallop < 1) {
			minGallop = 1;
		}
		switch (len1) {
			case 1:
				System.arraycopy(a, cursor2, a, dest, len2);
				assign(a, dest + len2, _tmp, cursor1);
				break;
			case 0:
				throw new IllegalArgumentException(
						"Comparison method violates its general contract!");
			default:
				System.arraycopy(_tmp, cursor1, a, dest, len1);
				break;
		}
		return minGallop;
	}

	@SuppressWarnings("SuspiciousSystemArraycopy")
	private int mergeHi(A a, int aLen, C c, A key, int base1, int len1, int base2, int len2, int minGallop) {
		A _tmp = ensureCapacity(len2, a, aLen);
		int _tmpBase = this.tmpBase;
		System.arraycopy(a, base2, _tmp, _tmpBase, len2);
		int cursor1 = base1 + len1 - 1;
		int cursor2 = _tmpBase + len2 - 1;
		int dest = base2 + len2 - 1;
		assign(a, dest--, a, cursor1--);
		if (--len1 == 0) {
			System.arraycopy(_tmp, _tmpBase, a, dest - (len2 - 1), len2);
			return minGallop;
		}
		if (len2 == 1) {
			dest -= len1;
			cursor1 -= len1;
			System.arraycopy(a, cursor1 + 1, a, dest + 1, len1);
			assign(a, dest, _tmp, cursor2);
			return minGallop;
		}
		outer:
		while (true) {
			int count1 = 0;
			int count2 = 0;
			do {
				if (compare(_tmp, cursor2, a, cursor1, c) < 0) {
					assign(a, dest--, a, cursor1--);
					count1++;
					count2 = 0;
					if (--len1 == 0) {
						break outer;
					}
				} else {
					assign(a, dest--, _tmp, cursor2--);
					count2++;
					count1 = 0;
					if (--len2 == 1) {
						break outer;
					}
				}
			} while ((count1 | count2) < minGallop);
			do {
				assign(key, 0, _tmp, cursor2);
				count1 = len1 - gallopRight(key, a, base1, len1, len1 - 1, c);
				if (count1 != 0) {
					dest -= count1;
					cursor1 -= count1;
					len1 -= count1;
					System.arraycopy(a, cursor1 + 1, a, dest + 1, count1);
					if (len1 == 0) {
						break outer;
					}
				}
				assign(a, dest--, _tmp, cursor2--);
				if (--len2 == 1) {
					break outer;
				}
				assign(key, 0, a, cursor1);
				count2 = len2 - gallopLeft(key, _tmp, _tmpBase, len2, len2 - 1, c);
				if (count2 != 0) {
					dest -= count2;
					cursor2 -= count2;
					len2 -= count2;
					System.arraycopy(_tmp, cursor2 + 1, a, dest + 1, count2);
					if (len2 <= 1) {
						break outer;
					}
				}
				assign(a, dest--, a, cursor1--);
				if (--len1 == 0) {
					break outer;
				}
				minGallop--;
			} while (count1 >= MIN_GALLOP | count2 >= MIN_GALLOP);
			if (minGallop < 0) {
				minGallop = 0;
			}
			minGallop += 2;
		}
		if (minGallop < 1) {
			minGallop = 1;
		}
		switch (len2) {
			case 1:
				dest -= len1;
				cursor1 -= len1;
				System.arraycopy(a, cursor1 + 1, a, dest + 1, len1);
				assign(a, dest, _tmp, cursor2);
				break;
			case 0:
				throw new IllegalArgumentException(
						"Comparison method violates its general contract!");
			default:
				System.arraycopy(_tmp, _tmpBase, a, dest - (len2 - 1), len2);
				break;
		}
		return minGallop;
	}

	final void sort(A a, int aLen, C c, int lo, int hi, int nRemaining, A work, int wLen, int workLen, int workBase) {
		A key = newArray(a, 1);
		if (nRemaining < MIN_MERGE) {
			int initRunLen = countRunAndMakeAscending(a, key, lo, hi, c);
			binarySort(a, key, lo, hi, lo + initRunLen, c);
			return;
		}
		int tlen = (aLen < 2 * INITIAL_TMP_STORAGE_LENGTH)
				? aLen >>> 1 : INITIAL_TMP_STORAGE_LENGTH;
		if (work == null || workLen < tlen || workBase + tlen > wLen) {
			tmp = newArray(a, tlen);
			tmpBase = 0;
			tmpLen = tlen;
		} else {
			tmp = work;
			tmpBase = workBase;
			tmpLen = workLen;
		}
		int stackLen = (aLen < 120 ? 5
				: aLen < 1542 ? 10
						: aLen < 119151 ? 24 : 49);
		int stackSize = 0;
		int[] runBase = new int[stackLen];
		int[] runLen = new int[stackLen];
		int minRun = minRunLength(nRemaining);
		int minGallop = MIN_GALLOP;
		do {
			int _runLen = countRunAndMakeAscending(a, key, lo, hi, c);
			if (_runLen < minRun) {
				int force = nRemaining <= minRun ? nRemaining : minRun;
				binarySort(a, key, lo, lo + force, lo + _runLen, c);
				_runLen = force;
			}
			runBase[stackSize] = lo;
			runLen[stackSize] = _runLen;
			stackSize++;
			while (stackSize > 1) {
				int n = stackSize - 2;
				if (n > 0 && runLen[n - 1] <= runLen[n] + runLen[n + 1]) {
					if (runLen[n - 1] < runLen[n + 1]) {
						n--;
					}
					minGallop = mergeAt(a, aLen, c, key, n, minGallop, runBase, runLen, stackSize--);
				} else if (runLen[n] <= runLen[n + 1]) {
					minGallop = mergeAt(a, aLen, c, key, n, minGallop, runBase, runLen, stackSize--);
				} else {
					break;
				}
			}
			lo += _runLen;
			nRemaining -= _runLen;
		} while (nRemaining != 0);
		while (stackSize > 1) {
			int n = stackSize - 2;
			if (n > 0 && runLen[n - 1] < runLen[n + 1]) {
				n--;
			}
			minGallop = mergeAt(a, aLen, c, key, n, minGallop, runBase, runLen, stackSize--);
		}
	}

	static final <T> void sort(T[] a, int lo, int hi, Comparator<? super T> c,
			T[] work, int workBase, int workLen) {
		int nRemaining = hi - lo;
		if (nRemaining < 2) {
			return;
		}
		new PrimitiveTimSort<T[], Comparator<? super T>>() {
			@Override
			final T[] newArray(T[] old, int length) {
				return (T[]) java.lang.reflect.Array.newInstance(old.getClass().getComponentType(), length);
			}

			@Override
			final void assign(T[] a, int x, T[] b, int y) {
				a[x] = b[y];
			}

			@Override
			final int compare(T[] a, int x, T[] b, int y, Comparator<? super T> comp) {
				return comp.compare(a[x], b[y]);
			}
		}.sort(a, a.length, c, lo, hi, nRemaining, work, work == null ? 0 : work.length, workLen, workBase);
	}

	static final void sort(boolean[] a, int lo, int hi, BooleanComparator c,
			boolean[] work, int workBase, int workLen) {
		int nRemaining = hi - lo;
		if (nRemaining < 2) {
			return;
		}
		new PrimitiveTimSort<boolean[], BooleanComparator>() {
			@Override
			final boolean[] newArray(boolean[] old, int length) {
				return new boolean[length];
			}

			@Override
			final void assign(boolean[] a, int x, boolean[] b, int y) {
				a[x] = b[y];
			}

			@Override
			final int compare(boolean[] a, int x, boolean[] b, int y, BooleanComparator comp) {
				return comp.compareBoolean(a[x], b[y]);
			}
		}.sort(a, a.length, c, lo, hi, nRemaining, work, work == null ? 0 : work.length, workLen, workBase);
	}

	static final void sort(byte[] a, int lo, int hi, ByteComparator c,
			byte[] work, int workBase, int workLen) {
		int nRemaining = hi - lo;
		if (nRemaining < 2) {
			return;
		}
		new PrimitiveTimSort<byte[], ByteComparator>() {
			@Override
			final byte[] newArray(byte[] old, int length) {
				return new byte[length];
			}

			@Override
			final void assign(byte[] a, int x, byte[] b, int y) {
				a[x] = b[y];
			}

			@Override
			final int compare(byte[] a, int x, byte[] b, int y, ByteComparator comp) {
				return comp.compareByte(a[x], b[y]);
			}
		}.sort(a, a.length, c, lo, hi, nRemaining, work, work == null ? 0 : work.length, workLen, workBase);
	}

	static final void sort(char[] a, int lo, int hi, CharComparator c,
			char[] work, int workBase, int workLen) {
		int nRemaining = hi - lo;
		if (nRemaining < 2) {
			return;
		}
		new PrimitiveTimSort<char[], CharComparator>() {
			@Override
			final char[] newArray(char[] old, int length) {
				return new char[length];
			}

			@Override
			final void assign(char[] a, int x, char[] b, int y) {
				a[x] = b[y];
			}

			@Override
			final int compare(char[] a, int x, char[] b, int y, CharComparator comp) {
				return comp.compareChar(a[x], b[y]);
			}
		}.sort(a, a.length, c, lo, hi, nRemaining, work, work == null ? 0 : work.length, workLen, workBase);
	}

	static final void sort(short[] a, int lo, int hi, ShortComparator c,
			short[] work, int workBase, int workLen) {
		int nRemaining = hi - lo;
		if (nRemaining < 2) {
			return;
		}
		new PrimitiveTimSort<short[], ShortComparator>() {
			@Override
			final short[] newArray(short[] old, int length) {
				return new short[length];
			}

			@Override
			final void assign(short[] a, int x, short[] b, int y) {
				a[x] = b[y];
			}

			@Override
			final int compare(short[] a, int x, short[] b, int y, ShortComparator comp) {
				return comp.compareShort(a[x], b[y]);
			}
		}.sort(a, a.length, c, lo, hi, nRemaining, work, work == null ? 0 : work.length, workLen, workBase);
	}

	static final void sort(int[] a, int lo, int hi, IntComparator c,
			int[] work, int workBase, int workLen) {
		int nRemaining = hi - lo;
		if (nRemaining < 2) {
			return;
		}
		new PrimitiveTimSort<int[], IntComparator>() {
			@Override
			final int[] newArray(int[] old, int length) {
				return new int[length];
			}

			@Override
			final void assign(int[] a, int x, int[] b, int y) {
				a[x] = b[y];
			}

			@Override
			final int compare(int[] a, int x, int[] b, int y, IntComparator comp) {
				return comp.compareInt(a[x], b[y]);
			}
		}.sort(a, a.length, c, lo, hi, nRemaining, work, work == null ? 0 : work.length, workLen, workBase);
	}

	static final void sort(float[] a, int lo, int hi, FloatComparator c,
			float[] work, int workBase, int workLen) {
		int nRemaining = hi - lo;
		if (nRemaining < 2) {
			return;
		}
		new PrimitiveTimSort<float[], FloatComparator>() {
			@Override
			final float[] newArray(float[] old, int length) {
				return new float[length];
			}

			@Override
			final void assign(float[] a, int x, float[] b, int y) {
				a[x] = b[y];
			}

			@Override
			final int compare(float[] a, int x, float[] b, int y, FloatComparator comp) {
				return comp.compareFloat(a[x], b[y]);
			}
		}.sort(a, a.length, c, lo, hi, nRemaining, work, work == null ? 0 : work.length, workLen, workBase);
	}

	static final void sort(long[] a, int lo, int hi, LongComparator c,
			long[] work, int workBase, int workLen) {
		int nRemaining = hi - lo;
		if (nRemaining < 2) {
			return;
		}
		new PrimitiveTimSort<long[], LongComparator>() {
			@Override
			final long[] newArray(long[] old, int length) {
				return new long[length];
			}

			@Override
			final void assign(long[] a, int x, long[] b, int y) {
				a[x] = b[y];
			}

			@Override
			final int compare(long[] a, int x, long[] b, int y, LongComparator comp) {
				return comp.compareLong(a[x], b[y]);
			}
		}.sort(a, a.length, c, lo, hi, nRemaining, work, work == null ? 0 : work.length, workLen, workBase);
	}

	static final void sort(double[] a, int lo, int hi, DoubleComparator c,
			double[] work, int workBase, int workLen) {
		int nRemaining = hi - lo;
		if (nRemaining < 2) {
			return;
		}
		new PrimitiveTimSort<double[], DoubleComparator>() {
			@Override
			final double[] newArray(double[] old, int length) {
				return new double[length];
			}

			@Override
			final void assign(double[] a, int x, double[] b, int y) {
				a[x] = b[y];
			}

			@Override
			final int compare(double[] a, int x, double[] b, int y, DoubleComparator comp) {
				return comp.compareDouble(a[x], b[y]);
			}
		}.sort(a, a.length, c, lo, hi, nRemaining, work, work == null ? 0 : work.length, workLen, workBase);
	}
}

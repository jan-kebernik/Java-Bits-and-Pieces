/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.collection;

import java.lang.ref.WeakReference;
import java.util.function.Supplier;

/**
 * Provides unified gaplist semantics for both primitive- and instance-based
 * implementations. The notable difference between the two is that
 * primitive-based gap arrays need not clear any elements.
 *
 * An example: Consider a list containing elements "0" through "9", occupying an
 * array of length 16. The "gap" is always considered to be the range of
 * un-occupied elements between the "head" and the "tail" of the list. In this
 * instance, elements "0" through "3" constitute the "head" of the list, the
 * remaining elements constitute the "tail". <br>
 * This particular array can be represented in one of 16 distinct "cycles":<br>
 * <pre>
 *       0 1 2 3 4 5 6 7 8 9 a b c d e f   p      q      s      i      n
 * 0.) [|0 1 2 3 _ _ _ _ _ _ 4 5 6 7 8 9]  p:-12  q: -6  s: 10  i:  0  n: 4
 * 1.) [ 9|0 1 2 3 _ _ _ _ _ _ 4 5 6 7 8]  p:-11  q: -5  s: 10  i:  1  n: 4
 * 2.) [ 8 9|0 1 2 3 _ _ _ _ _ _ 4 5 6 7]  p:-10  q: -4  s: 10  i:  2  n: 4
 * 3.) [ 7 8 9|0 1 2 3 _ _ _ _ _ _ 4 5 6]  p: -9  q: -3  s: 10  i:  3  n: 4
 * 4.) [ 6 7 8 9|0 1 2 3 _ _ _ _ _ _ 4 5]  p: -8  q: -2  s: 10  i:  4  n: 4
 * 5.) [ 5 6 7 8 9|0 1 2 3 _ _ _ _ _ _ 4]  p: -7  q: -1  s: 10  i:  5  n: 4
 * 6.) [ 4 5 6 7 8 9|0 1 2 3 _ _ _ _ _ _]  p: -6  q:  0  s: 10  i:  6  n: 4
 * 7.) [ _ 4 5 6 7 8 9|0 1 2 3 _ _ _ _ _]  p: -5  q:  1  s: 10  i:  7  n: 4
 * 8.) [ _ _ 4 5 6 7 8 9|0 1 2 3 _ _ _ _]  p: -4  q:  2  s: 10  i:  8  n: 4
 * 9.) [ _ _ _ 4 5 6 7 8 9|0 1 2 3 _ _ _]  p: -3  q:  3  s: 10  i:  9  n: 4
 * a.) [ _ _ _ _ 4 5 6 7 8 9|0 1 2 3 _ _]  p: -2  q:  4  s: 10  i: 10  n: 4
 * b.) [ _ _ _ _ _ 4 5 6 7 8 9|0 1 2 3 _]  p: -1  q:  5  s: 10  i: 11  n: 4
 * c.) [ _ _ _ _ _ _ 4 5 6 7 8 9|0 1 2 3]  p:  0  q:  6  s: 10  i: 12  n: 4
 * d.) [ 3 _ _ _ _ _ _ 4 5 6 7 8 9|0 1 2]  p:  1  q:  7  s: 10  i: 13  n: 4
 * e.) [ 2 3 _ _ _ _ _ _ 4 5 6 7 8 9|0 1]  p:  2  q:  8  s: 10  i: 14  n: 4
 * f.) [ 1 2 3 _ _ _ _ _ _ 4 5 6 7 8 9|0]  p:  3  q:  9  s: 10  i: 15  n: 4
 * </pre> The following values are used by any cyclic array to track its state:
 * <ul>
 * <li>s = "size" of the array, ie. the number of elements it contains.</li>
 * <li>i = the physical "index" at which the first head element resides in the
 * underlying array.</li>
 * <li>n = the "length" of the head, ie. the number of elements that constitute
 * the head.</li>
 * <li>aLen = the length of the underlying array. Note that no array variable is
 * owned by this implementation in order to fully support type independent
 * implementations. As such, the array length must usually be supplied to each
 * method that requires it.</li>
 * </ul>
 *
 * Several auxiliary values can be cheaply derived from these three values,
 * together with the length of the underlying array:
 * <ul>
 * <li>tailLen = s - n. The length of the "tail"</li>
 * <li>gapLen = aLen - s. The length of the "gap".</li>
 * <li>gapOff = i + n. The logical position of the "gap". May not reside inside
 * array bounds.</li>
 * <li>p = gapOff - aLen.</li>
 * <li>q = gapOff - s.</li>
 * </ul>
 * Note that <code>p</code> and <code>q</code> are used to quickly identify what
 * kind of cycle the array is currently in:
 * <pre>
 * if (p >= 0) {
 *	// the "head" crosses the array bounds by "p" elements. Both the "tail" and the "gap" are unbroken.
 * } else if (q >= 0) {
 *	// the "gap" crosses the array bounds by "q" elements. Both the "head" and the "tail" are unbroken.
 * } else {
 *	// the "tail" crosses the array bounds by "i" elements. Both the "head" and the "gap" are unbroken.
 * }
 * </pre> This class makes heavy use of these conditions to aggressively
 * optimize gaplist operations.
 *
 * Extensive efforts were made to ensure all arithmetic used is resistant to
 * integer-overflow.
 *
 * This class performs no bounds checks of any kind, unless specifically
 * instructed to.
 *
 *
 * @author Jan Kebernik
 * @param <A>
 */
public abstract class AbstractCyclicArray<A> {

	// max array length on some VMs.
	protected static final int MAX_ARRAY_CAPACITY = Integer.MAX_VALUE - 8;

	// always enforce a custom serialization format
	protected transient int _size;
	protected transient int _idx;
	protected transient int _len;
	private transient ArrayRef<A> _helper = null;	// used to cache temp arrays.

	// Note: there is no array field, because different implementations 
	// might benefit from different keyword modifiers (final, private, etc...).
	protected AbstractCyclicArray(int _size, int _idx, int _len) {
		this._size = _size;
		this._idx = _idx;
		this._len = _len;
	}

	/**
	 * Returns a new array instance of type {@code A}.
	 *
	 * @param len the length of the new array.
	 * @return a new array instance of type {@code A}.
	 */
	protected abstract A _newArray(int len);

	// must still declare CloneNotSupportedException, in case 
	// a sub-class down the line is no longer cloneable.
	@Override
	protected Object clone() throws CloneNotSupportedException {
		AbstractCyclicArray<?> g = (AbstractCyclicArray<?>) super.clone();
		g._helper = null;	// imitate constructor
		return g;
	}

	/**
	 * Functional interface that provides direct parameterized access to the
	 * underlying array and another object, possibly also the underlying array.
	 *
	 * Usually the first reference parameter of
	 * {@link #transfer(Object, int, Object, int, int, Object) transfer()} is
	 * the underlying array, except for variants of {@link #_write(Object, int, int, int, int,
	 * int, Object, int, int, Transfer, Object) _write()} and {@link #_insert(Object, int, int, int, int,
	 * int, Object, int, int, Transfer, Object) _insert()}, where it is always
	 * the second reference parameter.
	 *
	 * @param <A> The type of the first reference.
	 * @param <B> The type of the second reference.
	 * @param <C> The type of the attachment.
	 */
	@FunctionalInterface
	protected static interface Transfer<A, B, C> {

		void transfer(A a, int aOff, B b, int bOff, int len, C attachment);
	}

	@FunctionalInterface
	protected static interface CheckedTransfer<A, B, C, X extends Throwable> {

		void transfer(A a, int aOff, B b, int bOff, int len, C attachment) throws X;
	}

	@FunctionalInterface
	protected static interface LimitedTransfer<A, B, C> {

		// return num elements transferred
		int transfer(A a, int aOff, B b, int bOff, int len, C attachment);
	}

	@FunctionalInterface
	protected static interface CheckedLimitedTransfer<A, B, C, X extends Throwable> {

		int transfer(A a, int aOff, B b, int bOff, int len, C attachment) throws X;
	}

	@FunctionalInterface
	protected static interface Evaluator<A, B> {

		// must return physical index of (firs or last) element satisfying attachments
		int apply(A array, int off, int len, B attachment1, boolean attachment2);
	}

	@FunctionalInterface
	protected static interface CheckedEvaluator<A, B, X extends Throwable> {

		// must return physical index of (firs or last) element satisfying attachments
		int apply(A array, int off, int len, B attachment1, boolean attachment2) throws X;
	}

	@FunctionalInterface
	protected static interface IntAccumulator<A> {

		int accumulate(A array, int off, int len, int currentValue);
	}

	@FunctionalInterface
	protected static interface CheckedIntAccumulator<A, X extends Throwable> {

		int accumulate(A array, int off, int len, int currentValue) throws X;
	}

	@FunctionalInterface
	protected static interface LongAccumulator<A> {

		long accumulate(A array, int off, int len, long currentValue);
	}

	@FunctionalInterface
	protected static interface CheckedLongAccumulator<A, X extends Throwable> {

		long accumulate(A array, int off, int len, long currentValue) throws X;
	}

	@FunctionalInterface
	protected static interface Sorter<A, B> {

		void sort(A array, int fromIndex, int toIndex, B comparator);
	}

	@FunctionalInterface
	protected static interface CheckedSorter<A, B, X extends Throwable> {

		void sort(A array, int fromIndex, int toIndex, B comparator) throws X;
	}

	private static final class ArrayRef<A> extends WeakReference<A> {

		private final int len;

		private ArrayRef(A array, int len) {
			super(array);
			this.len = len;
		}
	}

	private static <E extends RuntimeException> void testBounds(int aLen, int off, int len, Supplier<E> sup) throws E {
		if (off < 0 || len < 0 || off > aLen - len) {
			// bad bounds are usually considered a result of interference
			throw sup.get();
		}
	}

	// do not modify this.
	@SuppressWarnings("SuspiciousSystemArraycopy")
	private static <A> void clear(A a, int off, int len, A clear, int clearLen) {
		if (len > clearLen) {
			int clearOff = off;
			System.arraycopy(clear, 0, a, clearOff, clearLen);
			off += clearLen;
			len -= clearLen;
			while (len > clearLen) {
				System.arraycopy(a, clearOff, a, off, clearLen);
				off += clearLen;
				len -= clearLen;
				clearLen <<= 1;	// always stays in positive range
			}
			System.arraycopy(a, clearOff, a, off, len);
			return;
		}
		if (len > 0) {
			System.arraycopy(clear, 0, a, off, len);
		}
	}

	// minimal branching, no modulo, overflow-resistant
	protected static final int _physicalIndex(int aLen, int s, int i, int n, int index) {
		int x = i + index - (index < n ? aLen : s);
		return x < 0 ? x + aLen : x;
	}

	// copies elements from "a" to "b"
	// TODO stub:
	// This code analyzes the current state of the cyclic array, and basically 
	// partitions it into 1-3 sequential direct-access ranges that can be be
	// read from or written to. 
	@SuppressWarnings("SuspiciousSystemArraycopy")
	protected static final <A, B> void _read(A a, int aLen, int s, int i, int n,
			int index, B b, int off, int len) throws ArrayIndexOutOfBoundsException {
		int gapOff = i + n;
		if (index < n) {
			// target head
			int r = n - index;
			if (len > r) {
				// range bridges gap
				int p = gapOff - aLen;
				int q = gapOff - s;
				if (p >= 0) {
					int x = aLen - i;
					if (index < x) {
						// index on right side
						int y = x - index;
						System.arraycopy(a, i + index, b, off, y);
						System.arraycopy(a, 0, b, off + y, r - y);
						System.arraycopy(a, q, b, off + r, len - r);
						return;
					}
					// index on left side
					System.arraycopy(a, index - x, b, off, r);
					System.arraycopy(a, q, b, off + r, len - r);
					return;
				}
				// head is unfragmented
				int y = len - r;
				int offR = off + r;
				if (q >= 0) {
					System.arraycopy(a, i + index, b, off, r);
					System.arraycopy(a, q, b, offR, y);
					return;
				}
				if (y > -q) {
					System.arraycopy(a, i + index, b, off, r);
					System.arraycopy(a, aLen + q, b, offR, -q);
					System.arraycopy(a, 0, b, offR - q, y + q);
					return;
				}
				System.arraycopy(a, i + index, b, off, r);
				System.arraycopy(a, aLen + q, b, offR, y);
				return;
			}
			// range confined to head
			int p = gapOff - aLen;
			if (p >= 0) {
				int x = aLen - i;
				if (index < x) {
					int y = x - index;
					if (len > y) {
						System.arraycopy(a, i + index, b, off, y);
						System.arraycopy(a, 0, b, off + y, len - y);
						return;
					}
					System.arraycopy(a, i + index, b, off, len);
					return;
				}
				System.arraycopy(a, index - x, b, off, len);
				return;
			}
			System.arraycopy(a, i + index, b, off, len);
			return;
		}
		// target tail
		int x = i + index - s;
		int q = gapOff - s;
		if (q >= 0) {
			System.arraycopy(a, x, b, off, len);
			return;
		}
		if (x < 0) {
			if (len > -x) {
				System.arraycopy(a, aLen + x, b, off, -x);
				System.arraycopy(a, 0, b, off - x, len + x);
				return;
			}
			System.arraycopy(a, aLen + x, b, off, len);
			return;
		}
		System.arraycopy(a, x, b, off, len);
	}

	protected static final <A, B, C> void _read(A a, int aLen, int s, int i, int n,
			int index, B b, int off, int len, Transfer<A, B, C> t, C attachment) throws ArrayIndexOutOfBoundsException {
		int gapOff = i + n;
		if (index < n) {
			// target head
			int r = n - index;
			if (len > r) {
				// range bridges gap
				int p = gapOff - aLen;
				int q = gapOff - s;
				if (p >= 0) {
					int x = aLen - i;
					if (index < x) {
						// index on right side
						int y = x - index;
						t.transfer(a, i + index, b, off, y, attachment);	// 1
						t.transfer(a, 0, b, off + y, r - y, attachment);	// 2
						t.transfer(a, q, b, off + r, len - r, attachment);	// 3
						return;
					}
					// index on left side
					t.transfer(a, index - x, b, off, r, attachment);	// 1
					t.transfer(a, q, b, off + r, len - r, attachment);	// 2
					return;
				}
				// head is unfragmented
				int y = len - r;
				int offR = off + r;
				if (q >= 0) {
					t.transfer(a, i + index, b, off, r, attachment);	// 1
					t.transfer(a, q, b, offR, y, attachment);			// 2
					return;
				}
				if (y > -q) {
					t.transfer(a, i + index, b, off, r, attachment);	// 1
					t.transfer(a, aLen + q, b, offR, -q, attachment);	// 2
					t.transfer(a, 0, b, offR - q, y + q, attachment);	// 3
					return;
				}
				t.transfer(a, i + index, b, off, r, attachment);	// 1
				t.transfer(a, aLen + q, b, offR, y, attachment);	// 2
				return;
			}
			// range confined to head
			int p = gapOff - aLen;
			if (p >= 0) {
				int x = aLen - i;
				if (index < x) {
					int y = x - index;
					if (len > y) {
						t.transfer(a, i + index, b, off, y, attachment);	// 1
						t.transfer(a, 0, b, off + y, len - y, attachment);	// 2
						return;
					}
					t.transfer(a, i + index, b, off, len, attachment);	// 1
					return;
				}
				t.transfer(a, index - x, b, off, len, attachment);	// 1
				return;
			}
			t.transfer(a, i + index, b, off, len, attachment);	// 1
			return;
		}
		// target tail
		int x = i + index - s;
		int q = gapOff - s;
		if (q >= 0) {
			t.transfer(a, x, b, off, len, attachment);	// 1
			return;
		}
		if (x < 0) {
			if (len > -x) {
				t.transfer(a, aLen + x, b, off, -x, attachment);	// 1
				t.transfer(a, 0, b, off - x, len + x, attachment);	// 2
				return;
			}
			t.transfer(a, aLen + x, b, off, len, attachment);	// 1
			return;
		}
		t.transfer(a, x, b, off, len, attachment);	// 1
	}

	protected static final <A, B, C> void _readBackwards(A a, int aLen, int s, int i, int n,
			int index, B b, int off, int len, Transfer<A, B, C> t, C attachment) throws ArrayIndexOutOfBoundsException {
		int gapOff = i + n;
		if (index < n) {
			// target head
			int r = n - index;
			if (len > r) {
				// range bridges gap
				int p = gapOff - aLen;
				int q = gapOff - s;
				if (p >= 0) {
					int x = aLen - i;
					if (index < x) {
						// index on right side
						int y = x - index;
						t.transfer(a, q, b, off + r, len - r, attachment);	// 3
						t.transfer(a, 0, b, off + y, r - y, attachment);	// 2
						t.transfer(a, i + index, b, off, y, attachment);	// 1
						return;
					}
					// index on left side
					t.transfer(a, q, b, off + r, len - r, attachment);	// 2
					t.transfer(a, index - x, b, off, r, attachment);	// 1
					return;
				}
				// head is unfragmented
				int y = len - r;
				int offR = off + r;
				if (q >= 0) {
					t.transfer(a, q, b, offR, y, attachment);			// 2
					t.transfer(a, i + index, b, off, r, attachment);	// 1
					return;
				}
				if (y > -q) {
					t.transfer(a, 0, b, offR - q, y + q, attachment);	// 3
					t.transfer(a, aLen + q, b, offR, -q, attachment);	// 2
					t.transfer(a, i + index, b, off, r, attachment);	// 1
					return;
				}
				t.transfer(a, aLen + q, b, offR, y, attachment);	// 2
				t.transfer(a, i + index, b, off, r, attachment);	// 1
				return;
			}
			// range confined to head
			int p = gapOff - aLen;
			if (p >= 0) {
				int x = aLen - i;
				if (index < x) {
					int y = x - index;
					if (len > y) {
						t.transfer(a, 0, b, off + y, len - y, attachment);	// 2
						t.transfer(a, i + index, b, off, y, attachment);	// 1
						return;
					}
					t.transfer(a, i + index, b, off, len, attachment);	// 1
					return;
				}
				t.transfer(a, index - x, b, off, len, attachment);	// 1
				return;
			}
			t.transfer(a, i + index, b, off, len, attachment);	// 1
			return;
		}
		// target tail
		int x = i + index - s;
		int q = gapOff - s;
		if (q >= 0) {
			t.transfer(a, x, b, off, len, attachment);	// 1
			return;
		}
		if (x < 0) {
			if (len > -x) {
				t.transfer(a, 0, b, off - x, len + x, attachment);	// 2
				t.transfer(a, aLen + x, b, off, -x, attachment);	// 1
				return;
			}
			t.transfer(a, aLen + x, b, off, len, attachment);	// 1
			return;
		}
		t.transfer(a, x, b, off, len, attachment);	// 1
	}

	protected static final <A, B, C, E extends RuntimeException> void _read(A a, int aLen, int s, int i, int n,
			int index, B b, int off, int len, Transfer<A, B, C> t, C attachment, Supplier<E> sup) throws E {
		int gapOff = i + n;
		if (index < n) {
			// target head
			int p = gapOff - aLen;
			int r = n - index;
			if (len > r) {
				// range bridges gap
				int q = gapOff - s;
				int x = len - r;
				if (p >= 0) {
					int y = aLen - i;
					if (index < y) {
						// index on right side
						int z = y - index;
						int u = i + index;
						int v = r - z;
						testBounds(aLen, u, z, sup);
						testBounds(aLen, 0, v, sup);
						testBounds(aLen, q, x, sup);
						t.transfer(a, u, b, off, z, attachment);		// 1
						t.transfer(a, 0, b, off + z, v, attachment);	// 2
						t.transfer(a, q, b, off + r, x, attachment);	// 3
						return;
					}
					// index on left side
					int z = index - y;
					testBounds(aLen, z, r, sup);
					testBounds(aLen, q, x, sup);
					t.transfer(a, z, b, off, r, attachment);		// 1
					t.transfer(a, q, b, off + r, x, attachment);	// 2
					return;
				}
				// head is unfragmented
				int y = i + index;
				int offR = off + r;
				if (q >= 0) {
					testBounds(aLen, y, r, sup);
					testBounds(aLen, q, x, sup);
					t.transfer(a, y, b, off, r, attachment);	// 1
					t.transfer(a, q, b, offR, x, attachment);	// 2
					return;
				}
				int z = aLen + q;
				if (x > -q) {
					int u = x + q;
					testBounds(aLen, y, r, sup);
					testBounds(aLen, z, -q, sup);
					testBounds(aLen, 0, u, sup);
					t.transfer(a, y, b, off, r, attachment);		// 1
					t.transfer(a, z, b, offR, -q, attachment);		// 2
					t.transfer(a, 0, b, offR - q, u, attachment);	// 3
					return;
				}
				testBounds(aLen, y, r, sup);
				testBounds(aLen, z, x, sup);
				t.transfer(a, y, b, off, r, attachment);	// 1
				t.transfer(a, z, b, offR, x, attachment);	// 2
				return;
			}
			// range confined to head
			if (p >= 0) {
				int x = aLen - i;
				if (index < x) {
					int y = x - index;
					int z = i + index;
					if (len > y) {
						int u = len - y;
						testBounds(aLen, z, y, sup);
						testBounds(aLen, 0, u, sup);
						t.transfer(a, z, b, off, y, attachment);		// 1
						t.transfer(a, 0, b, off + y, u, attachment);	// 2
						return;
					}
					testBounds(aLen, z, len, sup);
					t.transfer(a, z, b, off, len, attachment);	// 1
					return;
				}
				int y = index - x;
				testBounds(aLen, y, len, sup);
				t.transfer(a, y, b, off, len, attachment);	// 1
				return;
			}
			int x = i + index;
			testBounds(aLen, x, len, sup);
			t.transfer(a, x, b, off, len, attachment);	// 1
			return;
		}
		// target tail
		int x = i + index - s;
		int q = gapOff - s;
		if (q >= 0) {
			testBounds(aLen, x, len, sup);
			t.transfer(a, x, b, off, len, attachment);	// 1
			return;
		}
		if (x < 0) {
			int y = aLen + x;
			if (len > -x) {
				int z = len + x;
				testBounds(aLen, y, -x, sup);
				testBounds(aLen, 0, z, sup);
				t.transfer(a, y, b, off, -x, attachment);		// 1
				t.transfer(a, 0, b, off - x, z, attachment);	// 2
				return;
			}
			testBounds(aLen, y, len, sup);
			t.transfer(a, y, b, off, len, attachment);	// 1
			return;
		}
		testBounds(aLen, x, len, sup);
		t.transfer(a, x, b, off, len, attachment);	// 1
	}

	protected static final <A, B, C, E extends RuntimeException> void _readBackwards(A a, int aLen, int s, int i, int n,
			int index, B b, int off, int len, Transfer<A, B, C> t, C attachment, Supplier<E> sup) throws E {
		int gapOff = i + n;
		if (index < n) {
			// target head
			int p = gapOff - aLen;
			int r = n - index;
			if (len > r) {
				// range bridges gap
				int q = gapOff - s;
				int x = len - r;
				if (p >= 0) {
					int y = aLen - i;
					if (index < y) {
						// index on right side
						int z = y - index;
						int u = i + index;
						int v = r - z;
						testBounds(aLen, u, z, sup);
						testBounds(aLen, 0, v, sup);
						testBounds(aLen, q, x, sup);
						t.transfer(a, q, b, off + r, x, attachment);	// 3
						t.transfer(a, 0, b, off + z, v, attachment);	// 2
						t.transfer(a, u, b, off, z, attachment);		// 1
						return;
					}
					// index on left side
					int z = index - y;
					testBounds(aLen, z, r, sup);
					testBounds(aLen, q, x, sup);
					t.transfer(a, q, b, off + r, x, attachment);	// 2
					t.transfer(a, z, b, off, r, attachment);		// 1
					return;
				}
				// head is unfragmented
				int y = i + index;
				int offR = off + r;
				if (q >= 0) {
					testBounds(aLen, y, r, sup);
					testBounds(aLen, q, x, sup);
					t.transfer(a, q, b, offR, x, attachment);	// 2
					t.transfer(a, y, b, off, r, attachment);	// 1
					return;
				}
				int z = aLen + q;
				if (x > -q) {
					int u = x + q;
					testBounds(aLen, y, r, sup);
					testBounds(aLen, z, -q, sup);
					testBounds(aLen, 0, u, sup);
					t.transfer(a, 0, b, offR - q, u, attachment);	// 3
					t.transfer(a, z, b, offR, -q, attachment);		// 2
					t.transfer(a, y, b, off, r, attachment);		// 1
					return;
				}
				testBounds(aLen, y, r, sup);
				testBounds(aLen, z, x, sup);
				t.transfer(a, z, b, offR, x, attachment);	// 2
				t.transfer(a, y, b, off, r, attachment);	// 1
				return;
			}
			// range confined to head
			if (p >= 0) {
				int x = aLen - i;
				if (index < x) {
					int y = x - index;
					int z = i + index;
					if (len > y) {
						int u = len - y;
						testBounds(aLen, z, y, sup);
						testBounds(aLen, 0, u, sup);
						t.transfer(a, 0, b, off + y, u, attachment);	// 2
						t.transfer(a, z, b, off, y, attachment);		// 1
						return;
					}
					testBounds(aLen, z, len, sup);
					t.transfer(a, z, b, off, len, attachment);	// 1
					return;
				}
				int y = index - x;
				testBounds(aLen, y, len, sup);
				t.transfer(a, y, b, off, len, attachment);	// 1
				return;
			}
			int x = i + index;
			testBounds(aLen, x, len, sup);
			t.transfer(a, x, b, off, len, attachment);	// 1
			return;
		}
		// target tail
		int x = i + index - s;
		int q = gapOff - s;
		if (q >= 0) {
			testBounds(aLen, x, len, sup);
			t.transfer(a, x, b, off, len, attachment);	// 1
			return;
		}
		if (x < 0) {
			int y = aLen + x;
			if (len > -x) {
				int z = len + x;
				testBounds(aLen, y, -x, sup);
				testBounds(aLen, 0, z, sup);
				t.transfer(a, 0, b, off - x, z, attachment);	// 2
				t.transfer(a, y, b, off, -x, attachment);		// 1
				return;
			}
			testBounds(aLen, y, len, sup);
			t.transfer(a, y, b, off, len, attachment);	// 1
			return;
		}
		testBounds(aLen, x, len, sup);
		t.transfer(a, x, b, off, len, attachment);	// 1
	}

	protected static final <A, B, C, X extends Throwable> void _readChecked(A a, int aLen, int s, int i, int n,
			int index, B b, int off, int len, CheckedTransfer<A, B, C, X> t, C attachment) throws ArrayIndexOutOfBoundsException, X {
		int gapOff = i + n;
		if (index < n) {
			// target head
			int r = n - index;
			if (len > r) {
				// range bridges gap
				int p = gapOff - aLen;
				int q = gapOff - s;
				if (p >= 0) {
					int x = aLen - i;
					if (index < x) {
						// index on right side
						int y = x - index;
						t.transfer(a, i + index, b, off, y, attachment);	// 1
						t.transfer(a, 0, b, off + y, r - y, attachment);	// 2
						t.transfer(a, q, b, off + r, len - r, attachment);	// 3
						return;
					}
					// index on left side
					t.transfer(a, index - x, b, off, r, attachment);	// 1
					t.transfer(a, q, b, off + r, len - r, attachment);	// 2
					return;
				}
				// head is unfragmented
				int y = len - r;
				int offR = off + r;
				if (q >= 0) {
					t.transfer(a, i + index, b, off, r, attachment);	// 1
					t.transfer(a, q, b, offR, y, attachment);			// 2
					return;
				}
				if (y > -q) {
					t.transfer(a, i + index, b, off, r, attachment);	// 1
					t.transfer(a, aLen + q, b, offR, -q, attachment);	// 2
					t.transfer(a, 0, b, offR - q, y + q, attachment);	// 3
					return;
				}
				t.transfer(a, i + index, b, off, r, attachment);	// 1
				t.transfer(a, aLen + q, b, offR, y, attachment);	// 2
				return;
			}
			// range confined to head
			int p = gapOff - aLen;
			if (p >= 0) {
				int x = aLen - i;
				if (index < x) {
					int y = x - index;
					if (len > y) {
						t.transfer(a, i + index, b, off, y, attachment);	// 1
						t.transfer(a, 0, b, off + y, len - y, attachment);	// 2
						return;
					}
					t.transfer(a, i + index, b, off, len, attachment);	// 1
					return;
				}
				t.transfer(a, index - x, b, off, len, attachment);	// 1
				return;
			}
			t.transfer(a, i + index, b, off, len, attachment);	// 1
			return;
		}
		// target tail
		int x = i + index - s;
		int q = gapOff - s;
		if (q >= 0) {
			t.transfer(a, x, b, off, len, attachment);	// 1
			return;
		}
		if (x < 0) {
			if (len > -x) {
				t.transfer(a, aLen + x, b, off, -x, attachment);	// 1
				t.transfer(a, 0, b, off - x, len + x, attachment);	// 2
				return;
			}
			t.transfer(a, aLen + x, b, off, len, attachment);	// 1
			return;
		}
		t.transfer(a, x, b, off, len, attachment);	// 1
	}

	protected static final <A, B, C, X extends Throwable> void _readCheckedBackwards(A a, int aLen, int s, int i, int n,
			int index, B b, int off, int len, CheckedTransfer<A, B, C, X> t, C attachment) throws ArrayIndexOutOfBoundsException, X {
		int gapOff = i + n;
		if (index < n) {
			// target head
			int r = n - index;
			if (len > r) {
				// range bridges gap
				int p = gapOff - aLen;
				int q = gapOff - s;
				if (p >= 0) {
					int x = aLen - i;
					if (index < x) {
						// index on right side
						int y = x - index;
						t.transfer(a, q, b, off + r, len - r, attachment);	// 3
						t.transfer(a, 0, b, off + y, r - y, attachment);	// 2
						t.transfer(a, i + index, b, off, y, attachment);	// 1
						return;
					}
					// index on left side
					t.transfer(a, q, b, off + r, len - r, attachment);	// 2
					t.transfer(a, index - x, b, off, r, attachment);	// 1
					return;
				}
				// head is unfragmented
				int y = len - r;
				int offR = off + r;
				if (q >= 0) {
					t.transfer(a, q, b, offR, y, attachment);			// 2
					t.transfer(a, i + index, b, off, r, attachment);	// 1
					return;
				}
				if (y > -q) {
					t.transfer(a, 0, b, offR - q, y + q, attachment);	// 3
					t.transfer(a, aLen + q, b, offR, -q, attachment);	// 2
					t.transfer(a, i + index, b, off, r, attachment);	// 1
					return;
				}
				t.transfer(a, aLen + q, b, offR, y, attachment);	// 2
				t.transfer(a, i + index, b, off, r, attachment);	// 1
				return;
			}
			// range confined to head
			int p = gapOff - aLen;
			if (p >= 0) {
				int x = aLen - i;
				if (index < x) {
					int y = x - index;
					if (len > y) {
						t.transfer(a, 0, b, off + y, len - y, attachment);	// 2
						t.transfer(a, i + index, b, off, y, attachment);	// 1
						return;
					}
					t.transfer(a, i + index, b, off, len, attachment);	// 1
					return;
				}
				t.transfer(a, index - x, b, off, len, attachment);	// 1
				return;
			}
			t.transfer(a, i + index, b, off, len, attachment);	// 1
			return;
		}
		// target tail
		int x = i + index - s;
		int q = gapOff - s;
		if (q >= 0) {
			t.transfer(a, x, b, off, len, attachment);	// 1
			return;
		}
		if (x < 0) {
			if (len > -x) {
				t.transfer(a, 0, b, off - x, len + x, attachment);	// 2
				t.transfer(a, aLen + x, b, off, -x, attachment);	// 1
				return;
			}
			t.transfer(a, aLen + x, b, off, len, attachment);	// 1
			return;
		}
		t.transfer(a, x, b, off, len, attachment);	// 1
	}

	protected static final <A, B, C, E extends RuntimeException, X extends Throwable> void _readChecked(A a, int aLen, int s, int i, int n,
			int index, B b, int off, int len, CheckedTransfer<A, B, C, X> t, C attachment, Supplier<E> sup) throws E, X {
		int gapOff = i + n;
		if (index < n) {
			// target head
			int p = gapOff - aLen;
			int r = n - index;
			if (len > r) {
				// range bridges gap
				int q = gapOff - s;
				int x = len - r;
				if (p >= 0) {
					int y = aLen - i;
					if (index < y) {
						// index on right side
						int z = y - index;
						int u = i + index;
						int v = r - z;
						testBounds(aLen, u, z, sup);
						testBounds(aLen, 0, v, sup);
						testBounds(aLen, q, x, sup);
						t.transfer(a, u, b, off, z, attachment);		// 1
						t.transfer(a, 0, b, off + z, v, attachment);	// 2
						t.transfer(a, q, b, off + r, x, attachment);	// 3
						return;
					}
					// index on left side
					int z = index - y;
					testBounds(aLen, z, r, sup);
					testBounds(aLen, q, x, sup);
					t.transfer(a, z, b, off, r, attachment);		// 1
					t.transfer(a, q, b, off + r, x, attachment);	// 2
					return;
				}
				// head is unfragmented
				int y = i + index;
				int offR = off + r;
				if (q >= 0) {
					testBounds(aLen, y, r, sup);
					testBounds(aLen, q, x, sup);
					t.transfer(a, y, b, off, r, attachment);	// 1
					t.transfer(a, q, b, offR, x, attachment);	// 2
					return;
				}
				int z = aLen + q;
				if (x > -q) {
					int u = x + q;
					testBounds(aLen, y, r, sup);
					testBounds(aLen, z, -q, sup);
					testBounds(aLen, 0, u, sup);
					t.transfer(a, y, b, off, r, attachment);		// 1
					t.transfer(a, z, b, offR, -q, attachment);		// 2
					t.transfer(a, 0, b, offR - q, u, attachment);	// 3
					return;
				}
				testBounds(aLen, y, r, sup);
				testBounds(aLen, z, x, sup);
				t.transfer(a, y, b, off, r, attachment);	// 1
				t.transfer(a, z, b, offR, x, attachment);	// 2
				return;
			}
			// range confined to head
			if (p >= 0) {
				int x = aLen - i;
				if (index < x) {
					int y = x - index;
					int z = i + index;
					if (len > y) {
						int u = len - y;
						testBounds(aLen, z, y, sup);
						testBounds(aLen, 0, u, sup);
						t.transfer(a, z, b, off, y, attachment);		// 1
						t.transfer(a, 0, b, off + y, u, attachment);	// 2
						return;
					}
					testBounds(aLen, z, len, sup);
					t.transfer(a, z, b, off, len, attachment);	// 1
					return;
				}
				int y = index - x;
				testBounds(aLen, y, len, sup);
				t.transfer(a, y, b, off, len, attachment);	// 1
				return;
			}
			int x = i + index;
			testBounds(aLen, x, len, sup);
			t.transfer(a, x, b, off, len, attachment);	// 1
			return;
		}
		// target tail
		int x = i + index - s;
		int q = gapOff - s;
		if (q >= 0) {
			testBounds(aLen, x, len, sup);
			t.transfer(a, x, b, off, len, attachment);	// 1
			return;
		}
		if (x < 0) {
			int y = aLen + x;
			if (len > -x) {
				int z = len + x;
				testBounds(aLen, y, -x, sup);
				testBounds(aLen, 0, z, sup);
				t.transfer(a, y, b, off, -x, attachment);		// 1
				t.transfer(a, 0, b, off - x, z, attachment);	// 2
				return;
			}
			testBounds(aLen, y, len, sup);
			t.transfer(a, y, b, off, len, attachment);	// 1
			return;
		}
		testBounds(aLen, x, len, sup);
		t.transfer(a, x, b, off, len, attachment);	// 1
	}

	protected static final <A, B, C, E extends RuntimeException, X extends Throwable> void _readCheckedBackwards(A a, int aLen, int s, int i, int n,
			int index, B b, int off, int len, CheckedTransfer<A, B, C, X> t, C attachment, Supplier<E> sup) throws E, X {
		int gapOff = i + n;
		if (index < n) {
			// target head
			int p = gapOff - aLen;
			int r = n - index;
			if (len > r) {
				// range bridges gap
				int q = gapOff - s;
				int x = len - r;
				if (p >= 0) {
					int y = aLen - i;
					if (index < y) {
						// index on right side
						int z = y - index;
						int u = i + index;
						int v = r - z;
						testBounds(aLen, u, z, sup);
						testBounds(aLen, 0, v, sup);
						testBounds(aLen, q, x, sup);
						t.transfer(a, q, b, off + r, x, attachment);	// 3
						t.transfer(a, 0, b, off + z, v, attachment);	// 2
						t.transfer(a, u, b, off, z, attachment);		// 1
						return;
					}
					// index on left side
					int z = index - y;
					testBounds(aLen, z, r, sup);
					testBounds(aLen, q, x, sup);
					t.transfer(a, q, b, off + r, x, attachment);	// 2
					t.transfer(a, z, b, off, r, attachment);		// 1
					return;
				}
				// head is unfragmented
				int y = i + index;
				int offR = off + r;
				if (q >= 0) {
					testBounds(aLen, y, r, sup);
					testBounds(aLen, q, x, sup);
					t.transfer(a, q, b, offR, x, attachment);	// 2
					t.transfer(a, y, b, off, r, attachment);	// 1
					return;
				}
				int z = aLen + q;
				if (x > -q) {
					int u = x + q;
					testBounds(aLen, y, r, sup);
					testBounds(aLen, z, -q, sup);
					testBounds(aLen, 0, u, sup);
					t.transfer(a, 0, b, offR - q, u, attachment);	// 3
					t.transfer(a, z, b, offR, -q, attachment);		// 2
					t.transfer(a, y, b, off, r, attachment);		// 1
					return;
				}
				testBounds(aLen, y, r, sup);
				testBounds(aLen, z, x, sup);
				t.transfer(a, z, b, offR, x, attachment);	// 2
				t.transfer(a, y, b, off, r, attachment);	// 1
				return;
			}
			// range confined to head
			if (p >= 0) {
				int x = aLen - i;
				if (index < x) {
					int y = x - index;
					int z = i + index;
					if (len > y) {
						int u = len - y;
						testBounds(aLen, z, y, sup);
						testBounds(aLen, 0, u, sup);
						t.transfer(a, 0, b, off + y, u, attachment);	// 2
						t.transfer(a, z, b, off, y, attachment);		// 1
						return;
					}
					testBounds(aLen, z, len, sup);
					t.transfer(a, z, b, off, len, attachment);	// 1
					return;
				}
				int y = index - x;
				testBounds(aLen, y, len, sup);
				t.transfer(a, y, b, off, len, attachment);	// 1
				return;
			}
			int x = i + index;
			testBounds(aLen, x, len, sup);
			t.transfer(a, x, b, off, len, attachment);	// 1
			return;
		}
		// target tail
		int x = i + index - s;
		int q = gapOff - s;
		if (q >= 0) {
			testBounds(aLen, x, len, sup);
			t.transfer(a, x, b, off, len, attachment);	// 1
			return;
		}
		if (x < 0) {
			int y = aLen + x;
			if (len > -x) {
				int z = len + x;
				testBounds(aLen, y, -x, sup);
				testBounds(aLen, 0, z, sup);
				t.transfer(a, 0, b, off - x, z, attachment);	// 2
				t.transfer(a, y, b, off, -x, attachment);		// 1
				return;
			}
			testBounds(aLen, y, len, sup);
			t.transfer(a, y, b, off, len, attachment);	// 1
			return;
		}
		testBounds(aLen, x, len, sup);
		t.transfer(a, x, b, off, len, attachment);	// 1
	}

	protected static final <A, B, C> int _read(A a, int aLen, int s, int i, int n,
			int index, B b, int off, int len, LimitedTransfer<A, B, C> t, C attachment) throws ArrayIndexOutOfBoundsException {
		int gapOff = i + n;
		if (index < n) {
			// target head
			int p = gapOff - aLen;
			int r = n - index;
			if (len > r) {
				// range bridges gap
				int q = gapOff - s;
				int x = len - r;
				if (p >= 0) {
					int y = aLen - i;
					if (index < y) {
						// index on right side
						int z = y - index;
						int v = r - z;
						return _limited(a, b, attachment, t,
								i + index, off, z,
								0, off + z, v,
								q, off + r, x);
					}
					// index on left side
					return _limited(a, b, attachment, t,
							index - y, off, r,
							q, off + r, x);
				}
				// head is unfragmented
				int offR = off + r;
				if (q >= 0) {
					return _limited(a, b, attachment, t,
							i + index, off, r,
							q, offR, x);
				}
				if (x > -q) {
					int u = x + q;
					return _limited(a, b, attachment, t,
							i + index, off, r,
							aLen + q, offR, -q,
							0, offR - q, u);
				}
				return _limited(a, b, attachment, t,
						i + index, off, r,
						aLen + q, offR, x);
			}
			// range confined to head
			if (p >= 0) {
				int x = aLen - i;
				if (index < x) {
					int y = x - index;
					if (len > y) {
						return _limited(a, b, attachment, t,
								i + index, off, y,
								0, off + y, len - y);
					}
					return t.transfer(a, i + index, b, off, len, attachment);
				}
				return t.transfer(a, index - x, b, off, len, attachment);
			}
			return t.transfer(a, i + index, b, off, len, attachment);
		}
		// target tail
		int x = i + index - s;
		int q = gapOff - s;
		if (q >= 0) {
			return t.transfer(a, x, b, off, len, attachment);
		}
		if (x < 0) {
			if (len > -x) {
				return _limited(a, b, attachment, t,
						aLen + x, off, -x,
						0, off - x, len + x);
			}
			return t.transfer(a, aLen + x, b, off, len, attachment);
		}
		return t.transfer(a, x, b, off, len, attachment);
	}

	protected static final <A, B, C> int _readBackwards(A a, int aLen, int s, int i, int n,
			int index, B b, int off, int len, LimitedTransfer<A, B, C> t, C attachment) throws ArrayIndexOutOfBoundsException {
		int gapOff = i + n;
		if (index < n) {
			// target head
			int p = gapOff - aLen;
			int r = n - index;
			if (len > r) {
				// range bridges gap
				int q = gapOff - s;
				int x = len - r;
				if (p >= 0) {
					int y = aLen - i;
					if (index < y) {
						// index on right side
						int z = y - index;
						int v = r - z;
						return _limitedBackwards(a, b, attachment, t,
								i + index, off, z,
								0, off + z, v,
								q, off + r, x);
					}
					// index on left side
					return _limitedBackwards(a, b, attachment, t,
							index - y, off, r,
							q, off + r, x);
				}
				// head is unfragmented
				int offR = off + r;
				if (q >= 0) {
					return _limitedBackwards(a, b, attachment, t,
							i + index, off, r,
							q, offR, x);
				}
				if (x > -q) {
					int u = x + q;
					return _limitedBackwards(a, b, attachment, t,
							i + index, off, r,
							aLen + q, offR, -q,
							0, offR - q, u);
				}
				return _limitedBackwards(a, b, attachment, t,
						i + index, off, r,
						aLen + q, offR, x);
			}
			// range confined to head
			if (p >= 0) {
				int x = aLen - i;
				if (index < x) {
					int y = x - index;
					if (len > y) {
						return _limitedBackwards(a, b, attachment, t,
								i + index, off, y,
								0, off + y, len - y);
					}
					return t.transfer(a, i + index, b, off, len, attachment);
				}
				return t.transfer(a, index - x, b, off, len, attachment);
			}
			return t.transfer(a, i + index, b, off, len, attachment);
		}
		// target tail
		int x = i + index - s;
		int q = gapOff - s;
		if (q >= 0) {
			return t.transfer(a, x, b, off, len, attachment);
		}
		if (x < 0) {
			if (len > -x) {
				return _limitedBackwards(a, b, attachment, t,
						aLen + x, off, -x,
						0, off - x, len + x);
			}
			return t.transfer(a, aLen + x, b, off, len, attachment);
		}
		return t.transfer(a, x, b, off, len, attachment);
	}

	protected static final <A, B, C, E extends RuntimeException> int _read(A a, int aLen, int s, int i, int n,
			int index, B b, int off, int len, LimitedTransfer<A, B, C> t, C attachment, Supplier<E> sup) throws E {
		int gapOff = i + n;
		if (index < n) {
			// target head
			int p = gapOff - aLen;
			int r = n - index;
			if (len > r) {
				// range bridges gap
				int q = gapOff - s;
				int x = len - r;
				if (p >= 0) {
					int y = aLen - i;
					if (index < y) {
						// index on right side
						int z = y - index;
						int u = i + index;
						int v = r - z;
						testBounds(aLen, u, z, sup);
						testBounds(aLen, 0, v, sup);
						testBounds(aLen, q, x, sup);
						return _limited(a, b, attachment, t,
								u, off, z,
								0, off + z, v,
								q, off + r, x);
					}
					// index on left side
					int z = index - y;
					testBounds(aLen, z, r, sup);
					testBounds(aLen, q, x, sup);
					return _limited(a, b, attachment, t,
							z, off, r,
							q, off + r, x);
				}
				// head is unfragmented
				int y = i + index;
				int offR = off + r;
				if (q >= 0) {
					testBounds(aLen, y, r, sup);
					testBounds(aLen, q, x, sup);
					return _limited(a, b, attachment, t,
							y, off, r,
							q, offR, x);
				}
				int z = aLen + q;
				if (x > -q) {
					int u = x + q;
					testBounds(aLen, y, r, sup);
					testBounds(aLen, z, -q, sup);
					testBounds(aLen, 0, u, sup);
					return _limited(a, b, attachment, t,
							y, off, r,
							z, offR, -q,
							0, offR - q, u);
				}
				testBounds(aLen, y, r, sup);
				testBounds(aLen, z, x, sup);
				return _limited(a, b, attachment, t,
						y, off, r,
						z, offR, x);
			}
			// range confined to head
			if (p >= 0) {
				int x = aLen - i;
				if (index < x) {
					int y = x - index;
					int z = i + index;
					if (len > y) {
						int u = len - y;
						testBounds(aLen, z, y, sup);
						testBounds(aLen, 0, u, sup);
						return _limited(a, b, attachment, t,
								z, off, y,
								0, off + y, u);
					}
					testBounds(aLen, z, len, sup);
					return t.transfer(a, z, b, off, len, attachment);
				}
				int y = index - x;
				testBounds(aLen, y, len, sup);
				return t.transfer(a, y, b, off, len, attachment);
			}
			int x = i + index;
			testBounds(aLen, x, len, sup);
			return t.transfer(a, x, b, off, len, attachment);
		}
		// target tail
		int x = i + index - s;
		int q = gapOff - s;
		if (q >= 0) {
			testBounds(aLen, x, len, sup);
			return t.transfer(a, x, b, off, len, attachment);
		}
		if (x < 0) {
			int y = aLen + x;
			if (len > -x) {
				int z = len + x;
				testBounds(aLen, y, -x, sup);
				testBounds(aLen, 0, z, sup);
				return _limited(a, b, attachment, t,
						y, off, -x,
						0, off - x, z);
			}
			testBounds(aLen, y, len, sup);
			return t.transfer(a, y, b, off, len, attachment);
		}
		testBounds(aLen, x, len, sup);
		return t.transfer(a, x, b, off, len, attachment);
	}

	protected static final <A, B, C, E extends RuntimeException> int _readBackwards(A a, int aLen, int s, int i, int n,
			int index, B b, int off, int len, LimitedTransfer<A, B, C> t, C attachment, Supplier<E> sup) throws E {
		int gapOff = i + n;
		if (index < n) {
			// target head
			int p = gapOff - aLen;
			int r = n - index;
			if (len > r) {
				// range bridges gap
				int q = gapOff - s;
				int x = len - r;
				if (p >= 0) {
					int y = aLen - i;
					if (index < y) {
						// index on right side
						int z = y - index;
						int u = i + index;
						int v = r - z;
						testBounds(aLen, u, z, sup);
						testBounds(aLen, 0, v, sup);
						testBounds(aLen, q, x, sup);
						return _limitedBackwards(a, b, attachment, t,
								u, off, z,
								0, off + z, v,
								q, off + r, x);
					}
					// index on left side
					int z = index - y;
					testBounds(aLen, z, r, sup);
					testBounds(aLen, q, x, sup);
					return _limitedBackwards(a, b, attachment, t,
							z, off, r,
							q, off + r, x);
				}
				// head is unfragmented
				int y = i + index;
				int offR = off + r;
				if (q >= 0) {
					testBounds(aLen, y, r, sup);
					testBounds(aLen, q, x, sup);
					return _limitedBackwards(a, b, attachment, t,
							y, off, r,
							q, offR, x);
				}
				int z = aLen + q;
				if (x > -q) {
					int u = x + q;
					testBounds(aLen, y, r, sup);
					testBounds(aLen, z, -q, sup);
					testBounds(aLen, 0, u, sup);
					return _limitedBackwards(a, b, attachment, t,
							y, off, r,
							z, offR, -q,
							0, offR - q, u);
				}
				testBounds(aLen, y, r, sup);
				testBounds(aLen, z, x, sup);
				return _limitedBackwards(a, b, attachment, t,
						y, off, r,
						z, offR, x);
			}
			// range confined to head
			if (p >= 0) {
				int x = aLen - i;
				if (index < x) {
					int y = x - index;
					int z = i + index;
					if (len > y) {
						int u = len - y;
						testBounds(aLen, z, y, sup);
						testBounds(aLen, 0, u, sup);
						return _limitedBackwards(a, b, attachment, t,
								z, off, y,
								0, off + y, u);
					}
					testBounds(aLen, z, len, sup);
					return t.transfer(a, z, b, off, len, attachment);
				}
				int y = index - x;
				testBounds(aLen, y, len, sup);
				return t.transfer(a, y, b, off, len, attachment);
			}
			int x = i + index;
			testBounds(aLen, x, len, sup);
			return t.transfer(a, x, b, off, len, attachment);
		}
		// target tail
		int x = i + index - s;
		int q = gapOff - s;
		if (q >= 0) {
			testBounds(aLen, x, len, sup);
			return t.transfer(a, x, b, off, len, attachment);
		}
		if (x < 0) {
			int y = aLen + x;
			if (len > -x) {
				int z = len + x;
				testBounds(aLen, y, -x, sup);
				testBounds(aLen, 0, z, sup);
				return _limitedBackwards(a, b, attachment, t,
						y, off, -x,
						0, off - x, z);
			}
			testBounds(aLen, y, len, sup);
			return t.transfer(a, y, b, off, len, attachment);
		}
		testBounds(aLen, x, len, sup);
		return t.transfer(a, x, b, off, len, attachment);
	}

	protected static final <A, B, C, X extends Throwable> int _readChecked(A a, int aLen, int s, int i, int n,
			int index, B b, int off, int len, CheckedLimitedTransfer<A, B, C, X> t, C attachment) throws ArrayIndexOutOfBoundsException, X {
		int gapOff = i + n;
		if (index < n) {
			// target head
			int p = gapOff - aLen;
			int r = n - index;
			if (len > r) {
				// range bridges gap
				int q = gapOff - s;
				int x = len - r;
				if (p >= 0) {
					int y = aLen - i;
					if (index < y) {
						// index on right side
						int z = y - index;
						int v = r - z;
						return _checkedLimited(a, b, attachment, t,
								i + index, off, z,
								0, off + z, v,
								q, off + r, x);
					}
					// index on left side
					return _checkedLimited(a, b, attachment, t,
							index - y, off, r,
							q, off + r, x);
				}
				// head is unfragmented
				int offR = off + r;
				if (q >= 0) {
					return _checkedLimited(a, b, attachment, t,
							i + index, off, r,
							q, offR, x);
				}
				if (x > -q) {
					int u = x + q;
					return _checkedLimited(a, b, attachment, t,
							i + index, off, r,
							aLen + q, offR, -q,
							0, offR - q, u);
				}
				return _checkedLimited(a, b, attachment, t,
						i + index, off, r,
						aLen + q, offR, x);
			}
			// range confined to head
			if (p >= 0) {
				int x = aLen - i;
				if (index < x) {
					int y = x - index;
					if (len > y) {
						return _checkedLimited(a, b, attachment, t,
								i + index, off, y,
								0, off + y, len - y);
					}
					return t.transfer(a, i + index, b, off, len, attachment);
				}
				return t.transfer(a, index - x, b, off, len, attachment);
			}
			return t.transfer(a, i + index, b, off, len, attachment);
		}
		// target tail
		int x = i + index - s;
		int q = gapOff - s;
		if (q >= 0) {
			return t.transfer(a, x, b, off, len, attachment);
		}
		if (x < 0) {
			if (len > -x) {
				return _checkedLimited(a, b, attachment, t,
						aLen + x, off, -x,
						0, off - x, len + x);
			}
			return t.transfer(a, aLen + x, b, off, len, attachment);
		}
		return t.transfer(a, x, b, off, len, attachment);
	}

	protected static final <A, B, C, X extends Throwable> int _readCheckedBackwards(A a, int aLen, int s, int i, int n,
			int index, B b, int off, int len, CheckedLimitedTransfer<A, B, C, X> t, C attachment) throws ArrayIndexOutOfBoundsException, X {
		int gapOff = i + n;
		if (index < n) {
			// target head
			int p = gapOff - aLen;
			int r = n - index;
			if (len > r) {
				// range bridges gap
				int q = gapOff - s;
				int x = len - r;
				if (p >= 0) {
					int y = aLen - i;
					if (index < y) {
						// index on right side
						int z = y - index;
						int v = r - z;
						return _checkedLimitedBackwards(a, b, attachment, t,
								i + index, off, z,
								0, off + z, v,
								q, off + r, x);
					}
					// index on left side
					return _checkedLimitedBackwards(a, b, attachment, t,
							index - y, off, r,
							q, off + r, x);
				}
				// head is unfragmented
				int offR = off + r;
				if (q >= 0) {
					return _checkedLimitedBackwards(a, b, attachment, t,
							i + index, off, r,
							q, offR, x);
				}
				if (x > -q) {
					int u = x + q;
					return _checkedLimitedBackwards(a, b, attachment, t,
							i + index, off, r,
							aLen + q, offR, -q,
							0, offR - q, u);
				}
				return _checkedLimitedBackwards(a, b, attachment, t,
						i + index, off, r,
						aLen + q, offR, x);
			}
			// range confined to head
			if (p >= 0) {
				int x = aLen - i;
				if (index < x) {
					int y = x - index;
					if (len > y) {
						return _checkedLimitedBackwards(a, b, attachment, t,
								i + index, off, y,
								0, off + y, len - y);
					}
					return t.transfer(a, i + index, b, off, len, attachment);
				}
				return t.transfer(a, index - x, b, off, len, attachment);
			}
			return t.transfer(a, i + index, b, off, len, attachment);
		}
		// target tail
		int x = i + index - s;
		int q = gapOff - s;
		if (q >= 0) {
			return t.transfer(a, x, b, off, len, attachment);
		}
		if (x < 0) {
			if (len > -x) {
				return _checkedLimitedBackwards(a, b, attachment, t,
						aLen + x, off, -x,
						0, off - x, len + x);
			}
			return t.transfer(a, aLen + x, b, off, len, attachment);
		}
		return t.transfer(a, x, b, off, len, attachment);
	}

	protected static final <A, B, C, E extends RuntimeException, X extends Throwable> int _readChecked(A a, int aLen, int s, int i, int n,
			int index, B b, int off, int len, CheckedLimitedTransfer<A, B, C, X> t, C attachment, Supplier<E> sup) throws E, X {
		int gapOff = i + n;
		if (index < n) {
			// target head
			int p = gapOff - aLen;
			int r = n - index;
			if (len > r) {
				// range bridges gap
				int q = gapOff - s;
				int x = len - r;
				if (p >= 0) {
					int y = aLen - i;
					if (index < y) {
						// index on right side
						int z = y - index;
						int u = i + index;
						int v = r - z;
						testBounds(aLen, u, z, sup);
						testBounds(aLen, 0, v, sup);
						testBounds(aLen, q, x, sup);
						return _checkedLimited(a, b, attachment, t,
								u, off, z,
								0, off + z, v,
								q, off + r, x);
					}
					// index on left side
					int z = index - y;
					testBounds(aLen, z, r, sup);
					testBounds(aLen, q, x, sup);
					return _checkedLimited(a, b, attachment, t,
							z, off, r,
							q, off + r, x);
				}
				// head is unfragmented
				int y = i + index;
				int offR = off + r;
				if (q >= 0) {
					testBounds(aLen, y, r, sup);
					testBounds(aLen, q, x, sup);
					return _checkedLimited(a, b, attachment, t,
							y, off, r,
							q, offR, x);
				}
				int z = aLen + q;
				if (x > -q) {
					int u = x + q;
					testBounds(aLen, y, r, sup);
					testBounds(aLen, z, -q, sup);
					testBounds(aLen, 0, u, sup);
					return _checkedLimited(a, b, attachment, t,
							y, off, r,
							z, offR, -q,
							0, offR - q, u);
				}
				testBounds(aLen, y, r, sup);
				testBounds(aLen, z, x, sup);
				return _checkedLimited(a, b, attachment, t,
						y, off, r,
						z, offR, x);
			}
			// range confined to head
			if (p >= 0) {
				int x = aLen - i;
				if (index < x) {
					int y = x - index;
					int z = i + index;
					if (len > y) {
						int u = len - y;
						testBounds(aLen, z, y, sup);
						testBounds(aLen, 0, u, sup);
						return _checkedLimited(a, b, attachment, t,
								z, off, y,
								0, off + y, u);
					}
					testBounds(aLen, z, len, sup);
					return t.transfer(a, z, b, off, len, attachment);
				}
				int y = index - x;
				testBounds(aLen, y, len, sup);
				return t.transfer(a, y, b, off, len, attachment);
			}
			int x = i + index;
			testBounds(aLen, x, len, sup);
			return t.transfer(a, x, b, off, len, attachment);
		}
		// target tail
		int x = i + index - s;
		int q = gapOff - s;
		if (q >= 0) {
			testBounds(aLen, x, len, sup);
			return t.transfer(a, x, b, off, len, attachment);
		}
		if (x < 0) {
			int y = aLen + x;
			if (len > -x) {
				int z = len + x;
				testBounds(aLen, y, -x, sup);
				testBounds(aLen, 0, z, sup);
				return _checkedLimited(a, b, attachment, t,
						y, off, -x,
						0, off - x, z);
			}
			testBounds(aLen, y, len, sup);
			return t.transfer(a, y, b, off, len, attachment);
		}
		testBounds(aLen, x, len, sup);
		return t.transfer(a, x, b, off, len, attachment);
	}

	protected static final <A, B, C, E extends RuntimeException, X extends Throwable> int _readCheckedBackwards(A a, int aLen, int s, int i, int n,
			int index, B b, int off, int len, CheckedLimitedTransfer<A, B, C, X> t, C attachment, Supplier<E> sup) throws E, X {
		int gapOff = i + n;
		if (index < n) {
			// target head
			int p = gapOff - aLen;
			int r = n - index;
			if (len > r) {
				// range bridges gap
				int q = gapOff - s;
				int x = len - r;
				if (p >= 0) {
					int y = aLen - i;
					if (index < y) {
						// index on right side
						int z = y - index;
						int u = i + index;
						int v = r - z;
						testBounds(aLen, u, z, sup);
						testBounds(aLen, 0, v, sup);
						testBounds(aLen, q, x, sup);
						return _checkedLimitedBackwards(a, b, attachment, t,
								u, off, z,
								0, off + z, v,
								q, off + r, x);
					}
					// index on left side
					int z = index - y;
					testBounds(aLen, z, r, sup);
					testBounds(aLen, q, x, sup);
					return _checkedLimitedBackwards(a, b, attachment, t,
							z, off, r,
							q, off + r, x);
				}
				// head is unfragmented
				int y = i + index;
				int offR = off + r;
				if (q >= 0) {
					testBounds(aLen, y, r, sup);
					testBounds(aLen, q, x, sup);
					return _checkedLimitedBackwards(a, b, attachment, t,
							y, off, r,
							q, offR, x);
				}
				int z = aLen + q;
				if (x > -q) {
					int u = x + q;
					testBounds(aLen, y, r, sup);
					testBounds(aLen, z, -q, sup);
					testBounds(aLen, 0, u, sup);
					return _checkedLimitedBackwards(a, b, attachment, t,
							y, off, r,
							z, offR, -q,
							0, offR - q, u);
				}
				testBounds(aLen, y, r, sup);
				testBounds(aLen, z, x, sup);
				return _checkedLimitedBackwards(a, b, attachment, t,
						y, off, r,
						z, offR, x);
			}
			// range confined to head
			if (p >= 0) {
				int x = aLen - i;
				if (index < x) {
					int y = x - index;
					int z = i + index;
					if (len > y) {
						int u = len - y;
						testBounds(aLen, z, y, sup);
						testBounds(aLen, 0, u, sup);
						return _checkedLimitedBackwards(a, b, attachment, t,
								z, off, y,
								0, off + y, u);
					}
					testBounds(aLen, z, len, sup);
					return t.transfer(a, z, b, off, len, attachment);
				}
				int y = index - x;
				testBounds(aLen, y, len, sup);
				return t.transfer(a, y, b, off, len, attachment);
			}
			int x = i + index;
			testBounds(aLen, x, len, sup);
			return t.transfer(a, x, b, off, len, attachment);
		}
		// target tail
		int x = i + index - s;
		int q = gapOff - s;
		if (q >= 0) {
			testBounds(aLen, x, len, sup);
			return t.transfer(a, x, b, off, len, attachment);
		}
		if (x < 0) {
			int y = aLen + x;
			if (len > -x) {
				int z = len + x;
				testBounds(aLen, y, -x, sup);
				testBounds(aLen, 0, z, sup);
				return _checkedLimitedBackwards(a, b, attachment, t,
						y, off, -x,
						0, off - x, z);
			}
			testBounds(aLen, y, len, sup);
			return t.transfer(a, y, b, off, len, attachment);
		}
		testBounds(aLen, x, len, sup);
		return t.transfer(a, x, b, off, len, attachment);
	}

	// copies elements from "b" to "a"
	@SuppressWarnings("SuspiciousSystemArraycopy")
	protected static final <A, B> void _write(A a, int aLen, int s, int i, int n,
			int index, B b, int off, int len) throws ArrayIndexOutOfBoundsException {
		int gapOff = i + n;
		if (index < n) {
			// target head
			int r = n - index;
			if (len > r) {
				// range bridges gap
				int p = gapOff - aLen;
				int q = gapOff - s;
				if (p >= 0) {
					int x = aLen - i;
					if (index < x) {
						// index on right side
						int y = x - index;
						System.arraycopy(b, off, a, i + index, y);
						System.arraycopy(b, off + y, a, 0, r - y);
						System.arraycopy(b, off + r, a, q, len - r);
						return;
					}
					// index on left side
					System.arraycopy(b, off, a, index - x, r);
					System.arraycopy(b, off + r, a, q, len - r);
					return;
				}
				// head is unfragmented
				int y = len - r;
				int offR = off + r;
				if (q >= 0) {
					System.arraycopy(b, off, a, i + index, r);
					System.arraycopy(b, offR, a, q, y);
					return;
				}
				if (y > -q) {
					System.arraycopy(b, off, a, i + index, r);
					System.arraycopy(b, offR, a, aLen + q, -q);
					System.arraycopy(b, offR - q, a, 0, y + q);
					return;
				}
				System.arraycopy(b, off, a, i + index, r);
				System.arraycopy(b, offR, a, aLen + q, y);
				return;
			}
			// range confined to head
			int p = gapOff - aLen;
			if (p >= 0) {
				int x = aLen - i;
				if (index < x) {
					int y = x - index;
					if (len > y) {
						System.arraycopy(b, off, a, i + index, y);
						System.arraycopy(b, off + y, a, 0, len - y);
						return;
					}
					System.arraycopy(b, off, a, i + index, len);
					return;
				}
				System.arraycopy(b, off, a, index - x, len);
				return;
			}
			System.arraycopy(b, off, a, i + index, len);
			return;
		}
		// target tail
		int x = i + index - s;
		int q = gapOff - s;
		if (q >= 0) {
			System.arraycopy(b, off, a, x, len);
			return;
		}
		if (x < 0) {
			if (len > -x) {
				System.arraycopy(b, off, a, aLen + x, -x);
				System.arraycopy(b, off - x, a, 0, len + x);
				return;
			}
			System.arraycopy(b, off, a, aLen + x, len);
			return;
		}
		System.arraycopy(b, off, a, x, len);
	}

	protected static final <A, B, C> void _write(A a, int aLen, int s, int i, int n,
			int index, B b, int off, int len, Transfer<B, A, C> t, C attachment) throws ArrayIndexOutOfBoundsException {
		int gapOff = i + n;
		if (index < n) {
			// target head
			int r = n - index;
			if (len > r) {
				// range bridges gap
				int p = gapOff - aLen;
				int q = gapOff - s;
				if (p >= 0) {
					int x = aLen - i;
					if (index < x) {
						// index on right side
						int y = x - index;
						t.transfer(b, off, a, i + index, y, attachment);	// 1
						t.transfer(b, off + y, a, 0, r - y, attachment);	// 2
						t.transfer(b, off + r, a, q, len - r, attachment);	// 3
						return;
					}
					// index on left side
					t.transfer(b, off, a, index - x, r, attachment);	// 1
					t.transfer(b, off + r, a, q, len - r, attachment);	// 2
					return;
				}
				// head is unfragmented
				int y = len - r;
				int offR = off + r;
				if (q >= 0) {
					t.transfer(b, off, a, i + index, r, attachment);	// 1
					t.transfer(b, offR, a, q, y, attachment);			// 2
					return;
				}
				if (y > -q) {
					t.transfer(b, off, a, i + index, r, attachment);	// 1
					t.transfer(b, offR, a, aLen + q, -q, attachment);	// 2
					t.transfer(b, offR - q, a, 0, y + q, attachment);	// 3
					return;
				}
				t.transfer(b, off, a, i + index, r, attachment);	// 1
				t.transfer(b, offR, a, aLen + q, y, attachment);	// 2
				return;
			}
			// range confined to head
			int p = gapOff - aLen;
			if (p >= 0) {
				int x = aLen - i;
				if (index < x) {
					int y = x - index;
					if (len > y) {
						t.transfer(b, off, a, i + index, y, attachment);	// 1
						t.transfer(b, off + y, a, 0, len - y, attachment);	// 2
						return;
					}
					t.transfer(b, off, a, i + index, len, attachment);	// 1
					return;
				}
				t.transfer(b, off, a, index - x, len, attachment);	// 1
				return;
			}
			t.transfer(b, off, a, i + index, len, attachment);	// 1
			return;
		}
		// target tail
		int x = i + index - s;
		int q = gapOff - s;
		if (q >= 0) {
			t.transfer(b, off, a, x, len, attachment);	// 1
			return;
		}
		if (x < 0) {
			if (len > -x) {
				t.transfer(b, off, a, aLen + x, -x, attachment);	// 1
				t.transfer(b, off - x, a, 0, len + x, attachment);	// 2
				return;
			}
			t.transfer(b, off, a, aLen + x, len, attachment);	// 1
			return;
		}
		t.transfer(b, off, a, x, len, attachment);	// 1
	}

	protected static final <A, B, C> void _writeBackwards(A a, int aLen, int s, int i, int n,
			int index, B b, int off, int len, Transfer<B, A, C> t, C attachment) throws ArrayIndexOutOfBoundsException {
		int gapOff = i + n;
		if (index < n) {
			// target head
			int r = n - index;
			if (len > r) {
				// range bridges gap
				int p = gapOff - aLen;
				int q = gapOff - s;
				if (p >= 0) {
					int x = aLen - i;
					if (index < x) {
						// index on right side
						int y = x - index;
						t.transfer(b, off + r, a, q, len - r, attachment);	// 3
						t.transfer(b, off + y, a, 0, r - y, attachment);	// 2
						t.transfer(b, off, a, i + index, y, attachment);	// 1
						return;
					}
					// index on left side
					t.transfer(b, off + r, a, q, len - r, attachment);	// 2
					t.transfer(b, off, a, index - x, r, attachment);	// 1
					return;
				}
				// head is unfragmented
				int y = len - r;
				int offR = off + r;
				if (q >= 0) {
					t.transfer(b, offR, a, q, y, attachment);			// 2
					t.transfer(b, off, a, i + index, r, attachment);	// 1
					return;
				}
				if (y > -q) {
					t.transfer(b, offR - q, a, 0, y + q, attachment);	// 3
					t.transfer(b, offR, a, aLen + q, -q, attachment);	// 2
					t.transfer(b, off, a, i + index, r, attachment);	// 1
					return;
				}
				t.transfer(b, offR, a, aLen + q, y, attachment);	// 2
				t.transfer(b, off, a, i + index, r, attachment);	// 1
				return;
			}
			// range confined to head
			int p = gapOff - aLen;
			if (p >= 0) {
				int x = aLen - i;
				if (index < x) {
					int y = x - index;
					if (len > y) {
						t.transfer(b, off + y, a, 0, len - y, attachment);	// 2
						t.transfer(b, off, a, i + index, y, attachment);	// 1
						return;
					}
					t.transfer(b, off, a, i + index, len, attachment);	// 1
					return;
				}
				t.transfer(b, off, a, index - x, len, attachment);	// 1
				return;
			}
			t.transfer(b, off, a, i + index, len, attachment);	// 1
			return;
		}
		// target tail
		int x = i + index - s;
		int q = gapOff - s;
		if (q >= 0) {
			t.transfer(b, off, a, x, len, attachment);	// 1
			return;
		}
		if (x < 0) {
			if (len > -x) {
				t.transfer(b, off - x, a, 0, len + x, attachment);	// 2
				t.transfer(b, off, a, aLen + x, -x, attachment);	// 1
				return;
			}
			t.transfer(b, off, a, aLen + x, len, attachment);	// 1
			return;
		}
		t.transfer(b, off, a, x, len, attachment);	// 1
	}

	protected static final <A, B, C, E extends RuntimeException> void _write(A a, int aLen, int s, int i, int n,
			int index, B b, int off, int len, Transfer<B, A, C> t, C attachment, Supplier<E> sup) throws E {
		int gapOff = i + n;
		if (index < n) {
			// target head
			int p = gapOff - aLen;
			int r = n - index;
			if (len > r) {
				// range bridges gap
				int q = gapOff - s;
				int x = len - r;
				if (p >= 0) {
					int y = aLen - i;
					if (index < y) {
						// index on right side
						int z = y - index;
						int u = i + index;
						int v = r - z;
						testBounds(aLen, u, z, sup);
						testBounds(aLen, 0, v, sup);
						testBounds(aLen, q, x, sup);
						t.transfer(b, off, a, u, z, attachment);		// 1
						t.transfer(b, off + z, a, 0, v, attachment);	// 2
						t.transfer(b, off + r, a, q, x, attachment);	// 3
						return;
					}
					// index on left side
					int z = index - y;
					testBounds(aLen, z, r, sup);
					testBounds(aLen, q, x, sup);
					t.transfer(b, off, a, z, r, attachment);		// 1
					t.transfer(b, off + r, a, q, x, attachment);	// 2
					return;
				}
				// head is unfragmented
				int y = i + index;
				int offR = off + r;
				if (q >= 0) {
					testBounds(aLen, y, r, sup);
					testBounds(aLen, q, x, sup);
					t.transfer(b, off, a, y, r, attachment);	// 1
					t.transfer(b, offR, a, q, x, attachment);	// 2
					return;
				}
				int z = aLen + q;
				if (x > -q) {
					int u = x + q;
					testBounds(aLen, y, r, sup);
					testBounds(aLen, z, -q, sup);
					testBounds(aLen, 0, u, sup);
					t.transfer(b, off, a, y, r, attachment);		// 1
					t.transfer(b, offR, a, z, -q, attachment);		// 2
					t.transfer(b, offR - q, a, 0, u, attachment);	// 3
					return;
				}
				testBounds(aLen, y, r, sup);
				testBounds(aLen, z, x, sup);
				t.transfer(b, off, a, y, r, attachment);	// 1
				t.transfer(b, offR, a, z, x, attachment);	// 2
				return;
			}
			// range confined to head
			if (p >= 0) {
				int x = aLen - i;
				if (index < x) {
					int y = x - index;
					int z = i + index;
					if (len > y) {
						int u = len - y;
						testBounds(aLen, z, y, sup);
						testBounds(aLen, 0, u, sup);
						t.transfer(b, off, a, z, y, attachment);		// 1
						t.transfer(b, off + y, a, 0, u, attachment);	// 2
						return;
					}
					testBounds(aLen, z, len, sup);
					t.transfer(b, off, a, z, len, attachment);	// 1
					return;
				}
				int y = index - x;
				testBounds(aLen, y, len, sup);
				t.transfer(b, off, a, y, len, attachment);	// 1
				return;
			}
			int x = i + index;
			testBounds(aLen, x, len, sup);
			t.transfer(b, off, a, x, len, attachment);	// 1
			return;
		}
		// target tail
		int x = i + index - s;
		int q = gapOff - s;
		if (q >= 0) {
			testBounds(aLen, x, len, sup);
			t.transfer(b, off, a, x, len, attachment);	// 1
			return;
		}
		if (x < 0) {
			int y = aLen + x;
			if (len > -x) {
				int z = len + x;
				testBounds(aLen, y, -x, sup);
				testBounds(aLen, 0, z, sup);
				t.transfer(b, off, a, y, -x, attachment);		// 1
				t.transfer(b, off - x, a, 0, z, attachment);	// 2
				return;
			}
			testBounds(aLen, y, len, sup);
			t.transfer(b, off, a, y, len, attachment);	// 1
			return;
		}
		testBounds(aLen, x, len, sup);
		t.transfer(b, off, a, x, len, attachment);	// 1
	}

	protected static final <A, B, C, E extends RuntimeException> void _writeBackwards(A a, int aLen, int s, int i, int n,
			int index, B b, int off, int len, Transfer<B, A, C> t, C attachment, Supplier<E> sup) throws E {
		int gapOff = i + n;
		if (index < n) {
			// target head
			int p = gapOff - aLen;
			int r = n - index;
			if (len > r) {
				// range bridges gap
				int q = gapOff - s;
				int x = len - r;
				if (p >= 0) {
					int y = aLen - i;
					if (index < y) {
						// index on right side
						int z = y - index;
						int u = i + index;
						int v = r - z;
						// TODO put these aggregates in extra methods for convenience
						// TODO like transfer1, transfer2 and transfer3 or w/e
						testBounds(aLen, u, z, sup);
						testBounds(aLen, 0, v, sup);
						testBounds(aLen, q, x, sup);
						t.transfer(b, off + r, a, q, x, attachment);	// 3
						t.transfer(b, off + z, a, 0, v, attachment);	// 2
						t.transfer(b, off, a, u, z, attachment);		// 1
						return;
					}
					// index on left side
					int z = index - y;
					testBounds(aLen, z, r, sup);
					testBounds(aLen, q, x, sup);
					t.transfer(b, off + r, a, q, x, attachment);	// 2
					t.transfer(b, off, a, z, r, attachment);		// 1
					return;
				}
				// head is unfragmented
				int y = i + index;
				int offR = off + r;
				if (q >= 0) {
					testBounds(aLen, y, r, sup);
					testBounds(aLen, q, x, sup);
					t.transfer(b, offR, a, q, x, attachment);	// 2
					t.transfer(b, off, a, y, r, attachment);	// 1
					return;
				}
				int z = aLen + q;
				if (x > -q) {
					int u = x + q;
					testBounds(aLen, y, r, sup);
					testBounds(aLen, z, -q, sup);
					testBounds(aLen, 0, u, sup);
					t.transfer(b, offR - q, a, 0, u, attachment);	// 3
					t.transfer(b, offR, a, z, -q, attachment);		// 2
					t.transfer(b, off, a, y, r, attachment);		// 1
					return;
				}
				testBounds(aLen, y, r, sup);
				testBounds(aLen, z, x, sup);
				t.transfer(b, offR, a, z, x, attachment);	// 2
				t.transfer(b, off, a, y, r, attachment);	// 1
				return;
			}
			// range confined to head
			if (p >= 0) {
				int x = aLen - i;
				if (index < x) {
					int y = x - index;
					int z = i + index;
					if (len > y) {
						int u = len - y;
						testBounds(aLen, z, y, sup);
						testBounds(aLen, 0, u, sup);
						t.transfer(b, off + y, a, 0, u, attachment);	// 2
						t.transfer(b, off, a, z, y, attachment);		// 1
						return;
					}
					testBounds(aLen, z, len, sup);
					t.transfer(b, off, a, z, len, attachment);	// 1
					return;
				}
				int y = index - x;
				testBounds(aLen, y, len, sup);
				t.transfer(b, off, a, y, len, attachment);	// 1
				return;
			}
			int x = i + index;
			testBounds(aLen, x, len, sup);
			t.transfer(b, off, a, x, len, attachment);	// 1
			return;
		}
		// target tail
		int x = i + index - s;
		int q = gapOff - s;
		if (q >= 0) {
			testBounds(aLen, x, len, sup);
			t.transfer(b, off, a, x, len, attachment);	// 1
			return;
		}
		if (x < 0) {
			int y = aLen + x;
			if (len > -x) {
				int z = len + x;
				testBounds(aLen, y, -x, sup);
				testBounds(aLen, 0, z, sup);
				t.transfer(b, off - x, a, 0, z, attachment);	// 2
				t.transfer(b, off, a, y, -x, attachment);		// 1
				return;
			}
			testBounds(aLen, y, len, sup);
			t.transfer(b, off, a, y, len, attachment);	// 1
			return;
		}
		testBounds(aLen, x, len, sup);
		t.transfer(b, off, a, x, len, attachment);	// 1
	}

	protected static final <A, B, C, X extends Throwable> void _writeChecked(A a, int aLen, int s, int i, int n,
			int index, B b, int off, int len, CheckedTransfer<B, A, C, X> t, C attachment) throws ArrayIndexOutOfBoundsException, X {
		int gapOff = i + n;
		if (index < n) {
			// target head
			int r = n - index;
			if (len > r) {
				// range bridges gap
				int p = gapOff - aLen;
				int q = gapOff - s;
				if (p >= 0) {
					int x = aLen - i;
					if (index < x) {
						// index on right side
						int y = x - index;
						t.transfer(b, off, a, i + index, y, attachment);	// 1
						t.transfer(b, off + y, a, 0, r - y, attachment);	// 2
						t.transfer(b, off + r, a, q, len - r, attachment);	// 3
						return;
					}
					// index on left side
					t.transfer(b, off, a, index - x, r, attachment);	// 1
					t.transfer(b, off + r, a, q, len - r, attachment);	// 2
					return;
				}
				// head is unfragmented
				int y = len - r;
				int offR = off + r;
				if (q >= 0) {
					t.transfer(b, off, a, i + index, r, attachment);	// 1
					t.transfer(b, offR, a, q, y, attachment);			// 2
					return;
				}
				if (y > -q) {
					t.transfer(b, off, a, i + index, r, attachment);	// 1
					t.transfer(b, offR, a, aLen + q, -q, attachment);	// 2
					t.transfer(b, offR - q, a, 0, y + q, attachment);	// 3
					return;
				}
				t.transfer(b, off, a, i + index, r, attachment);	// 1
				t.transfer(b, offR, a, aLen + q, y, attachment);	// 2
				return;
			}
			// range confined to head
			int p = gapOff - aLen;
			if (p >= 0) {
				int x = aLen - i;
				if (index < x) {
					int y = x - index;
					if (len > y) {
						t.transfer(b, off, a, i + index, y, attachment);	// 1
						t.transfer(b, off + y, a, 0, len - y, attachment);	// 2
						return;
					}
					t.transfer(b, off, a, i + index, len, attachment);	// 1
					return;
				}
				t.transfer(b, off, a, index - x, len, attachment);	// 1
				return;
			}
			t.transfer(b, off, a, i + index, len, attachment);	// 1
			return;
		}
		// target tail
		int x = i + index - s;
		int q = gapOff - s;
		if (q >= 0) {
			t.transfer(b, off, a, x, len, attachment);	// 1
			return;
		}
		if (x < 0) {
			if (len > -x) {
				t.transfer(b, off, a, aLen + x, -x, attachment);	// 1
				t.transfer(b, off - x, a, 0, len + x, attachment);	// 2
				return;
			}
			t.transfer(b, off, a, aLen + x, len, attachment);	// 1
			return;
		}
		t.transfer(b, off, a, x, len, attachment);	// 1
	}

	protected static final <A, B, C, X extends Throwable> void _writeCheckedBackwards(A a, int aLen, int s, int i, int n,
			int index, B b, int off, int len, CheckedTransfer<B, A, C, X> t, C attachment) throws ArrayIndexOutOfBoundsException, X {
		int gapOff = i + n;
		if (index < n) {
			// target head
			int r = n - index;
			if (len > r) {
				// range bridges gap
				int p = gapOff - aLen;
				int q = gapOff - s;
				if (p >= 0) {
					int x = aLen - i;
					if (index < x) {
						// index on right side
						int y = x - index;
						t.transfer(b, off + r, a, q, len - r, attachment);	// 3
						t.transfer(b, off + y, a, 0, r - y, attachment);	// 2
						t.transfer(b, off, a, i + index, y, attachment);	// 1
						return;
					}
					// index on left side
					t.transfer(b, off + r, a, q, len - r, attachment);	// 2
					t.transfer(b, off, a, index - x, r, attachment);	// 1
					return;
				}
				// head is unfragmented
				int y = len - r;
				int offR = off + r;
				if (q >= 0) {
					t.transfer(b, offR, a, q, y, attachment);			// 2
					t.transfer(b, off, a, i + index, r, attachment);	// 1
					return;
				}
				if (y > -q) {
					t.transfer(b, offR - q, a, 0, y + q, attachment);	// 3
					t.transfer(b, offR, a, aLen + q, -q, attachment);	// 2
					t.transfer(b, off, a, i + index, r, attachment);	// 1
					return;
				}
				t.transfer(b, offR, a, aLen + q, y, attachment);	// 2
				t.transfer(b, off, a, i + index, r, attachment);	// 1
				return;
			}
			// range confined to head
			int p = gapOff - aLen;
			if (p >= 0) {
				int x = aLen - i;
				if (index < x) {
					int y = x - index;
					if (len > y) {
						t.transfer(b, off + y, a, 0, len - y, attachment);	// 2
						t.transfer(b, off, a, i + index, y, attachment);	// 1
						return;
					}
					t.transfer(b, off, a, i + index, len, attachment);	// 1
					return;
				}
				t.transfer(b, off, a, index - x, len, attachment);	// 1
				return;
			}
			t.transfer(b, off, a, i + index, len, attachment);	// 1
			return;
		}
		// target tail
		int x = i + index - s;
		int q = gapOff - s;
		if (q >= 0) {
			t.transfer(b, off, a, x, len, attachment);	// 1
			return;
		}
		if (x < 0) {
			if (len > -x) {
				t.transfer(b, off - x, a, 0, len + x, attachment);	// 2
				t.transfer(b, off, a, aLen + x, -x, attachment);	// 1
				return;
			}
			t.transfer(b, off, a, aLen + x, len, attachment);	// 1
			return;
		}
		t.transfer(b, off, a, x, len, attachment);	// 1
	}

	protected static final <A, B, C, E extends RuntimeException, X extends Throwable> void _writeChecked(A a, int aLen, int s, int i, int n,
			int index, B b, int off, int len, CheckedTransfer<B, A, C, X> t, C attachment, Supplier<E> sup) throws E, X {
		int gapOff = i + n;
		if (index < n) {
			// target head
			int p = gapOff - aLen;
			int r = n - index;
			if (len > r) {
				// range bridges gap
				int q = gapOff - s;
				int x = len - r;
				if (p >= 0) {
					int y = aLen - i;
					if (index < y) {
						// index on right side
						int z = y - index;
						int u = i + index;
						int v = r - z;
						testBounds(aLen, u, z, sup);
						testBounds(aLen, 0, v, sup);
						testBounds(aLen, q, x, sup);
						t.transfer(b, off, a, u, z, attachment);		// 1
						t.transfer(b, off + z, a, 0, v, attachment);	// 2
						t.transfer(b, off + r, a, q, x, attachment);	// 3
						return;
					}
					// index on left side
					int z = index - y;
					testBounds(aLen, z, r, sup);
					testBounds(aLen, q, x, sup);
					t.transfer(b, off, a, z, r, attachment);		// 1
					t.transfer(b, off + r, a, q, x, attachment);	// 2
					return;
				}
				// head is unfragmented
				int y = i + index;
				int offR = off + r;
				if (q >= 0) {
					testBounds(aLen, y, r, sup);
					testBounds(aLen, q, x, sup);
					t.transfer(b, off, a, y, r, attachment);	// 1
					t.transfer(b, offR, a, q, x, attachment);	// 2
					return;
				}
				int z = aLen + q;
				if (x > -q) {
					int u = x + q;
					testBounds(aLen, y, r, sup);
					testBounds(aLen, z, -q, sup);
					testBounds(aLen, 0, u, sup);
					t.transfer(b, off, a, y, r, attachment);		// 1
					t.transfer(b, offR, a, z, -q, attachment);		// 2
					t.transfer(b, offR - q, a, 0, u, attachment);	// 3
					return;
				}
				testBounds(aLen, y, r, sup);
				testBounds(aLen, z, x, sup);
				t.transfer(b, off, a, y, r, attachment);	// 1
				t.transfer(b, offR, a, z, x, attachment);	// 2
				return;
			}
			// range confined to head
			if (p >= 0) {
				int x = aLen - i;
				if (index < x) {
					int y = x - index;
					int z = i + index;
					if (len > y) {
						int u = len - y;
						testBounds(aLen, z, y, sup);
						testBounds(aLen, 0, u, sup);
						t.transfer(b, off, a, z, y, attachment);		// 1
						t.transfer(b, off + y, a, 0, u, attachment);	// 2
						return;
					}
					testBounds(aLen, z, len, sup);
					t.transfer(b, off, a, z, len, attachment);	// 1
					return;
				}
				int y = index - x;
				testBounds(aLen, y, len, sup);
				t.transfer(b, off, a, y, len, attachment);	// 1
				return;
			}
			int x = i + index;
			testBounds(aLen, x, len, sup);
			t.transfer(b, off, a, x, len, attachment);	// 1
			return;
		}
		// target tail
		int x = i + index - s;
		int q = gapOff - s;
		if (q >= 0) {
			testBounds(aLen, x, len, sup);
			t.transfer(b, off, a, x, len, attachment);	// 1
			return;
		}
		if (x < 0) {
			int y = aLen + x;
			if (len > -x) {
				int z = len + x;
				testBounds(aLen, y, -x, sup);
				testBounds(aLen, 0, z, sup);
				t.transfer(b, off, a, y, -x, attachment);		// 1
				t.transfer(b, off - x, a, 0, z, attachment);	// 2
				return;
			}
			testBounds(aLen, y, len, sup);
			t.transfer(b, off, a, y, len, attachment);	// 1
			return;
		}
		testBounds(aLen, x, len, sup);
		t.transfer(b, off, a, x, len, attachment);	// 1
	}

	protected static final <A, B, C, E extends RuntimeException, X extends Throwable> void _writeCheckedBackwards(A a, int aLen, int s, int i, int n,
			int index, B b, int off, int len, CheckedTransfer<B, A, C, X> t, C attachment, Supplier<E> sup) throws E, X {
		int gapOff = i + n;
		if (index < n) {
			// target head
			int p = gapOff - aLen;
			int r = n - index;
			if (len > r) {
				// range bridges gap
				int q = gapOff - s;
				int x = len - r;
				if (p >= 0) {
					int y = aLen - i;
					if (index < y) {
						// index on right side
						int z = y - index;
						int u = i + index;
						int v = r - z;
						testBounds(aLen, u, z, sup);
						testBounds(aLen, 0, v, sup);
						testBounds(aLen, q, x, sup);
						t.transfer(b, off + r, a, q, x, attachment);	// 3
						t.transfer(b, off + z, a, 0, v, attachment);	// 2
						t.transfer(b, off, a, u, z, attachment);		// 1
						return;
					}
					// index on left side
					int z = index - y;
					testBounds(aLen, z, r, sup);
					testBounds(aLen, q, x, sup);
					t.transfer(b, off + r, a, q, x, attachment);	// 2
					t.transfer(b, off, a, z, r, attachment);		// 1
					return;
				}
				// head is unfragmented
				int y = i + index;
				int offR = off + r;
				if (q >= 0) {
					testBounds(aLen, y, r, sup);
					testBounds(aLen, q, x, sup);
					t.transfer(b, offR, a, q, x, attachment);	// 2
					t.transfer(b, off, a, y, r, attachment);	// 1
					return;
				}
				int z = aLen + q;
				if (x > -q) {
					int u = x + q;
					testBounds(aLen, y, r, sup);
					testBounds(aLen, z, -q, sup);
					testBounds(aLen, 0, u, sup);
					t.transfer(b, offR - q, a, 0, u, attachment);	// 3
					t.transfer(b, offR, a, z, -q, attachment);		// 2
					t.transfer(b, off, a, y, r, attachment);		// 1
					return;
				}
				testBounds(aLen, y, r, sup);
				testBounds(aLen, z, x, sup);
				t.transfer(b, offR, a, z, x, attachment);	// 2
				t.transfer(b, off, a, y, r, attachment);	// 1
				return;
			}
			// range confined to head
			if (p >= 0) {
				int x = aLen - i;
				if (index < x) {
					int y = x - index;
					int z = i + index;
					if (len > y) {
						int u = len - y;
						testBounds(aLen, z, y, sup);
						testBounds(aLen, 0, u, sup);
						t.transfer(b, off + y, a, 0, u, attachment);	// 2
						t.transfer(b, off, a, z, y, attachment);		// 1
						return;
					}
					testBounds(aLen, z, len, sup);
					t.transfer(b, off, a, z, len, attachment);	// 1
					return;
				}
				int y = index - x;
				testBounds(aLen, y, len, sup);
				t.transfer(b, off, a, y, len, attachment);	// 1
				return;
			}
			int x = i + index;
			testBounds(aLen, x, len, sup);
			t.transfer(b, off, a, x, len, attachment);	// 1
			return;
		}
		// target tail
		int x = i + index - s;
		int q = gapOff - s;
		if (q >= 0) {
			testBounds(aLen, x, len, sup);
			t.transfer(b, off, a, x, len, attachment);	// 1
			return;
		}
		if (x < 0) {
			int y = aLen + x;
			if (len > -x) {
				int z = len + x;
				testBounds(aLen, y, -x, sup);
				testBounds(aLen, 0, z, sup);
				t.transfer(b, off - x, a, 0, z, attachment);	// 2
				t.transfer(b, off, a, y, -x, attachment);		// 1
				return;
			}
			testBounds(aLen, y, len, sup);
			t.transfer(b, off, a, y, len, attachment);	// 1
			return;
		}
		testBounds(aLen, x, len, sup);
		t.transfer(b, off, a, x, len, attachment);	// 1
	}

	protected static final <A, B, C> int _write(A a, int aLen, int s, int i, int n,
			int index, B b, int off, int len, LimitedTransfer<B, A, C> t, C attachment) throws ArrayIndexOutOfBoundsException {
		int gapOff = i + n;
		if (index < n) {
			// target head
			int p = gapOff - aLen;
			int r = n - index;
			if (len > r) {
				// range bridges gap
				int q = gapOff - s;
				int x = len - r;
				if (p >= 0) {
					int y = aLen - i;
					if (index < y) {
						// index on right side
						int z = y - index;
						int v = r - z;
						return _limited(b, a, attachment, t,
								off, i + index, z,
								off + z, 0, v,
								off + r, q, x);
					}
					// index on left side
					return _limited(b, a, attachment, t,
							off, index - y, r,
							off + r, q, x);
				}
				// head is unfragmented
				int offR = off + r;
				if (q >= 0) {
					return _limited(b, a, attachment, t,
							off, i + index, r,
							offR, q, x);
				}
				if (x > -q) {
					int u = x + q;
					return _limited(b, a, attachment, t,
							off, i + index, r,
							offR, aLen + q, -q,
							offR - q, 0, u);
				}
				return _limited(b, a, attachment, t,
						off, i + index, r,
						offR, aLen + q, x);
			}
			// range confined to head
			if (p >= 0) {
				int x = aLen - i;
				if (index < x) {
					int y = x - index;
					if (len > y) {
						return _limited(b, a, attachment, t,
								off, i + index, y,
								off + y, 0, len - y);
					}
					return t.transfer(b, off, a, i + index, len, attachment);
				}
				return t.transfer(b, off, a, index - x, len, attachment);
			}
			return t.transfer(b, off, a, i + index, len, attachment);
		}
		// target tail
		int x = i + index - s;
		int q = gapOff - s;
		if (q >= 0) {
			return t.transfer(b, off, a, x, len, attachment);
		}
		if (x < 0) {
			if (len > -x) {
				return _limited(b, a, attachment, t,
						off, aLen + x, -x,
						off - x, 0, len + x);
			}
			return t.transfer(b, off, a, aLen + x, len, attachment);
		}
		return t.transfer(b, off, a, x, len, attachment);
	}

	protected static final <A, B, C> int _writeBackwards(A a, int aLen, int s, int i, int n,
			int index, B b, int off, int len, LimitedTransfer<B, A, C> t, C attachment) throws ArrayIndexOutOfBoundsException {
		int gapOff = i + n;
		if (index < n) {
			// target head
			int p = gapOff - aLen;
			int r = n - index;
			if (len > r) {
				// range bridges gap
				int q = gapOff - s;
				int x = len - r;
				if (p >= 0) {
					int y = aLen - i;
					if (index < y) {
						// index on right side
						int z = y - index;
						int v = r - z;
						return _limitedBackwards(b, a, attachment, t,
								off, i + index, z,
								off + z, 0, v,
								off + r, q, x);
					}
					// index on left side
					return _limitedBackwards(b, a, attachment, t,
							off, index - y, r,
							off + r, q, x);
				}
				// head is unfragmented
				int offR = off + r;
				if (q >= 0) {
					return _limitedBackwards(b, a, attachment, t,
							off, i + index, r,
							offR, q, x);
				}
				if (x > -q) {
					int u = x + q;
					return _limitedBackwards(b, a, attachment, t,
							off, i + index, r,
							offR, aLen + q, -q,
							offR - q, 0, u);
				}
				return _limitedBackwards(b, a, attachment, t,
						off, i + index, r,
						offR, aLen + q, x);
			}
			// range confined to head
			if (p >= 0) {
				int x = aLen - i;
				if (index < x) {
					int y = x - index;
					if (len > y) {
						return _limitedBackwards(b, a, attachment, t,
								off, i + index, y,
								off + y, 0, len - y);
					}
					return t.transfer(b, off, a, i + index, len, attachment);
				}
				return t.transfer(b, off, a, index - x, len, attachment);
			}
			return t.transfer(b, off, a, i + index, len, attachment);
		}
		// target tail
		int x = i + index - s;
		int q = gapOff - s;
		if (q >= 0) {
			return t.transfer(b, off, a, x, len, attachment);
		}
		if (x < 0) {
			if (len > -x) {
				return _limitedBackwards(b, a, attachment, t,
						off, aLen + x, -x,
						off - x, 0, len + x);
			}
			return t.transfer(b, off, a, aLen + x, len, attachment);
		}
		return t.transfer(b, off, a, x, len, attachment);
	}

	protected static final <A, B, C, E extends RuntimeException> int _write(A a, int aLen, int s, int i, int n,
			int index, B b, int off, int len, LimitedTransfer<B, A, C> t, C attachment, Supplier<E> sup) throws E {
		int gapOff = i + n;
		if (index < n) {
			// target head
			int p = gapOff - aLen;
			int r = n - index;
			if (len > r) {
				// range bridges gap
				int q = gapOff - s;
				int x = len - r;
				if (p >= 0) {
					int y = aLen - i;
					if (index < y) {
						// index on right side
						int z = y - index;
						int u = i + index;
						int v = r - z;
						testBounds(aLen, u, z, sup);
						testBounds(aLen, 0, v, sup);
						testBounds(aLen, q, x, sup);
						return _limited(b, a, attachment, t,
								off, u, z,
								off + z, 0, v,
								off + r, q, x);
					}
					// index on left side
					int z = index - y;
					testBounds(aLen, z, r, sup);
					testBounds(aLen, q, x, sup);
					return _limited(b, a, attachment, t,
							off, z, r,
							off + r, q, x);
				}
				// head is unfragmented
				int y = i + index;
				int offR = off + r;
				if (q >= 0) {
					testBounds(aLen, y, r, sup);
					testBounds(aLen, q, x, sup);
					return _limited(b, a, attachment, t,
							off, y, r,
							offR, q, x);
				}
				int z = aLen + q;
				if (x > -q) {
					int u = x + q;
					testBounds(aLen, y, r, sup);
					testBounds(aLen, z, -q, sup);
					testBounds(aLen, 0, u, sup);
					return _limited(b, a, attachment, t,
							off, y, r,
							offR, z, -q,
							offR - q, 0, u);
				}
				testBounds(aLen, y, r, sup);
				testBounds(aLen, z, x, sup);
				return _limited(b, a, attachment, t,
						off, y, r,
						offR, z, x);
			}
			// range confined to head
			if (p >= 0) {
				int x = aLen - i;
				if (index < x) {
					int y = x - index;
					int z = i + index;
					if (len > y) {
						int u = len - y;
						testBounds(aLen, z, y, sup);
						testBounds(aLen, 0, u, sup);
						return _limited(b, a, attachment, t,
								off, z, y,
								off + y, 0, u);
					}
					testBounds(aLen, z, len, sup);
					return t.transfer(b, off, a, z, len, attachment);
				}
				int y = index - x;
				testBounds(aLen, y, len, sup);
				return t.transfer(b, off, a, y, len, attachment);
			}
			int x = i + index;
			testBounds(aLen, x, len, sup);
			return t.transfer(b, off, a, x, len, attachment);
		}
		// target tail
		int x = i + index - s;
		int q = gapOff - s;
		if (q >= 0) {
			testBounds(aLen, x, len, sup);
			return t.transfer(b, off, a, x, len, attachment);
		}
		if (x < 0) {
			int y = aLen + x;
			if (len > -x) {
				int z = len + x;
				testBounds(aLen, y, -x, sup);
				testBounds(aLen, 0, z, sup);
				return _limited(b, a, attachment, t,
						off, y, -x,
						off - x, 0, z);
			}
			testBounds(aLen, y, len, sup);
			return t.transfer(b, off, a, y, len, attachment);
		}
		testBounds(aLen, x, len, sup);
		return t.transfer(b, off, a, x, len, attachment);
	}

	protected static final <A, B, C, E extends RuntimeException> int _writeBackwards(A a, int aLen, int s, int i, int n,
			int index, B b, int off, int len, LimitedTransfer<B, A, C> t, C attachment, Supplier<E> sup) throws E {
		int gapOff = i + n;
		if (index < n) {
			// target head
			int p = gapOff - aLen;
			int r = n - index;
			if (len > r) {
				// range bridges gap
				int q = gapOff - s;
				int x = len - r;
				if (p >= 0) {
					int y = aLen - i;
					if (index < y) {
						// index on right side
						int z = y - index;
						int u = i + index;
						int v = r - z;
						testBounds(aLen, u, z, sup);
						testBounds(aLen, 0, v, sup);
						testBounds(aLen, q, x, sup);
						return _limitedBackwards(b, a, attachment, t,
								off, u, z,
								off + z, 0, v,
								off + r, q, x);
					}
					// index on left side
					int z = index - y;
					testBounds(aLen, z, r, sup);
					testBounds(aLen, q, x, sup);
					return _limitedBackwards(b, a, attachment, t,
							off, z, r,
							off + r, q, x);
				}
				// head is unfragmented
				int y = i + index;
				int offR = off + r;
				if (q >= 0) {
					testBounds(aLen, y, r, sup);
					testBounds(aLen, q, x, sup);
					return _limitedBackwards(b, a, attachment, t,
							off, y, r,
							offR, q, x);
				}
				int z = aLen + q;
				if (x > -q) {
					int u = x + q;
					testBounds(aLen, y, r, sup);
					testBounds(aLen, z, -q, sup);
					testBounds(aLen, 0, u, sup);
					return _limitedBackwards(b, a, attachment, t,
							off, y, r,
							offR, z, -q,
							offR - q, 0, u);
				}
				testBounds(aLen, y, r, sup);
				testBounds(aLen, z, x, sup);
				return _limitedBackwards(b, a, attachment, t,
						off, y, r,
						offR, z, x);
			}
			// range confined to head
			if (p >= 0) {
				int x = aLen - i;
				if (index < x) {
					int y = x - index;
					int z = i + index;
					if (len > y) {
						int u = len - y;
						testBounds(aLen, z, y, sup);
						testBounds(aLen, 0, u, sup);
						return _limitedBackwards(b, a, attachment, t,
								off, z, y,
								off + y, 0, u);
					}
					testBounds(aLen, z, len, sup);
					return t.transfer(b, off, a, z, len, attachment);
				}
				int y = index - x;
				testBounds(aLen, y, len, sup);
				return t.transfer(b, off, a, y, len, attachment);
			}
			int x = i + index;
			testBounds(aLen, x, len, sup);
			return t.transfer(b, off, a, x, len, attachment);
		}
		// target tail
		int x = i + index - s;
		int q = gapOff - s;
		if (q >= 0) {
			testBounds(aLen, x, len, sup);
			return t.transfer(b, off, a, x, len, attachment);
		}
		if (x < 0) {
			int y = aLen + x;
			if (len > -x) {
				int z = len + x;
				testBounds(aLen, y, -x, sup);
				testBounds(aLen, 0, z, sup);
				return _limitedBackwards(b, a, attachment, t,
						off, y, -x,
						off - x, 0, z);
			}
			testBounds(aLen, y, len, sup);
			return t.transfer(b, off, a, y, len, attachment);
		}
		testBounds(aLen, x, len, sup);
		return t.transfer(b, off, a, x, len, attachment);
	}

	protected static final <A, B, C, X extends Throwable> int _writeChecked(A a, int aLen, int s, int i, int n,
			int index, B b, int off, int len, CheckedLimitedTransfer<B, A, C, X> t, C attachment) throws ArrayIndexOutOfBoundsException, X {
		int gapOff = i + n;
		if (index < n) {
			// target head
			int p = gapOff - aLen;
			int r = n - index;
			if (len > r) {
				// range bridges gap
				int q = gapOff - s;
				int x = len - r;
				if (p >= 0) {
					int y = aLen - i;
					if (index < y) {
						// index on right side
						int z = y - index;
						int v = r - z;
						return _checkedLimited(b, a, attachment, t,
								off, i + index, z,
								off + z, 0, v,
								off + r, q, x);
					}
					// index on left side
					return _checkedLimited(b, a, attachment, t,
							off, index - y, r,
							off + r, q, x);
				}
				// head is unfragmented
				int offR = off + r;
				if (q >= 0) {
					return _checkedLimited(b, a, attachment, t,
							off, i + index, r,
							offR, q, x);
				}
				if (x > -q) {
					int u = x + q;
					return _checkedLimited(b, a, attachment, t,
							off, i + index, r,
							offR, aLen + q, -q,
							offR - q, 0, u);
				}
				return _checkedLimited(b, a, attachment, t,
						off, i + index, r,
						offR, aLen + q, x);
			}
			// range confined to head
			if (p >= 0) {
				int x = aLen - i;
				if (index < x) {
					int y = x - index;
					if (len > y) {
						return _checkedLimited(b, a, attachment, t,
								off, i + index, y,
								off + y, 0, len - y);
					}
					return t.transfer(b, off, a, i + index, len, attachment);
				}
				return t.transfer(b, off, a, index - x, len, attachment);
			}
			return t.transfer(b, off, a, i + index, len, attachment);
		}
		// target tail
		int x = i + index - s;
		int q = gapOff - s;
		if (q >= 0) {
			return t.transfer(b, off, a, x, len, attachment);
		}
		if (x < 0) {
			if (len > -x) {
				return _checkedLimited(b, a, attachment, t,
						off, aLen + x, -x,
						off - x, 0, len + x);
			}
			return t.transfer(b, off, a, aLen + x, len, attachment);
		}
		return t.transfer(b, off, a, x, len, attachment);
	}

	protected static final <A, B, C, X extends Throwable> int _writeCheckedBackwards(A a, int aLen, int s, int i, int n,
			int index, B b, int off, int len, CheckedLimitedTransfer<B, A, C, X> t, C attachment) throws ArrayIndexOutOfBoundsException, X {
		int gapOff = i + n;
		if (index < n) {
			// target head
			int p = gapOff - aLen;
			int r = n - index;
			if (len > r) {
				// range bridges gap
				int q = gapOff - s;
				int x = len - r;
				if (p >= 0) {
					int y = aLen - i;
					if (index < y) {
						// index on right side
						int z = y - index;
						int v = r - z;
						return _checkedLimitedBackwards(b, a, attachment, t,
								off, i + index, z,
								off + z, 0, v,
								off + r, q, x);
					}
					// index on left side
					return _checkedLimitedBackwards(b, a, attachment, t,
							off, index - y, r,
							off + r, q, x);
				}
				// head is unfragmented
				int offR = off + r;
				if (q >= 0) {
					return _checkedLimitedBackwards(b, a, attachment, t,
							off, i + index, r,
							offR, q, x);
				}
				if (x > -q) {
					int u = x + q;
					return _checkedLimitedBackwards(b, a, attachment, t,
							off, i + index, r,
							offR, aLen + q, -q,
							offR - q, 0, u);
				}
				return _checkedLimitedBackwards(b, a, attachment, t,
						off, i + index, r,
						offR, aLen + q, x);
			}
			// range confined to head
			if (p >= 0) {
				int x = aLen - i;
				if (index < x) {
					int y = x - index;
					if (len > y) {
						return _checkedLimitedBackwards(b, a, attachment, t,
								off, i + index, y,
								off + y, 0, len - y);
					}
					return t.transfer(b, off, a, i + index, len, attachment);
				}
				return t.transfer(b, off, a, index - x, len, attachment);
			}
			return t.transfer(b, off, a, i + index, len, attachment);
		}
		// target tail
		int x = i + index - s;
		int q = gapOff - s;
		if (q >= 0) {
			return t.transfer(b, off, a, x, len, attachment);
		}
		if (x < 0) {
			if (len > -x) {
				return _checkedLimitedBackwards(b, a, attachment, t,
						off, aLen + x, -x,
						off - x, 0, len + x);
			}
			return t.transfer(b, off, a, aLen + x, len, attachment);
		}
		return t.transfer(b, off, a, x, len, attachment);
	}

	protected static final <A, B, C, E extends RuntimeException, X extends Throwable> int _writeChecked(A a, int aLen, int s, int i, int n,
			int index, B b, int off, int len, CheckedLimitedTransfer<B, A, C, X> t, C attachment, Supplier<E> sup) throws E, X {
		int gapOff = i + n;
		if (index < n) {
			// target head
			int p = gapOff - aLen;
			int r = n - index;
			if (len > r) {
				// range bridges gap
				int q = gapOff - s;
				int x = len - r;
				if (p >= 0) {
					int y = aLen - i;
					if (index < y) {
						// index on right side
						int z = y - index;
						int u = i + index;
						int v = r - z;
						testBounds(aLen, u, z, sup);
						testBounds(aLen, 0, v, sup);
						testBounds(aLen, q, x, sup);
						return _checkedLimited(b, a, attachment, t,
								off, u, z,
								off + z, 0, v,
								off + r, q, x);
					}
					// index on left side
					int z = index - y;
					testBounds(aLen, z, r, sup);
					testBounds(aLen, q, x, sup);
					return _checkedLimited(b, a, attachment, t,
							off, z, r,
							off + r, q, x);
				}
				// head is unfragmented
				int y = i + index;
				int offR = off + r;
				if (q >= 0) {
					testBounds(aLen, y, r, sup);
					testBounds(aLen, q, x, sup);
					return _checkedLimited(b, a, attachment, t,
							off, y, r,
							offR, q, x);
				}
				int z = aLen + q;
				if (x > -q) {
					int u = x + q;
					testBounds(aLen, y, r, sup);
					testBounds(aLen, z, -q, sup);
					testBounds(aLen, 0, u, sup);
					return _checkedLimited(b, a, attachment, t,
							off, y, r,
							offR, z, -q,
							offR - q, 0, u);
				}
				testBounds(aLen, y, r, sup);
				testBounds(aLen, z, x, sup);
				return _checkedLimited(b, a, attachment, t,
						off, y, r,
						offR, z, x);
			}
			// range confined to head
			if (p >= 0) {
				int x = aLen - i;
				if (index < x) {
					int y = x - index;
					int z = i + index;
					if (len > y) {
						int u = len - y;
						testBounds(aLen, z, y, sup);
						testBounds(aLen, 0, u, sup);
						return _checkedLimited(b, a, attachment, t,
								off, z, y,
								off + y, 0, u);
					}
					testBounds(aLen, z, len, sup);
					return t.transfer(b, off, a, z, len, attachment);
				}
				int y = index - x;
				testBounds(aLen, y, len, sup);
				return t.transfer(b, off, a, y, len, attachment);
			}
			int x = i + index;
			testBounds(aLen, x, len, sup);
			return t.transfer(b, off, a, x, len, attachment);
		}
		// target tail
		int x = i + index - s;
		int q = gapOff - s;
		if (q >= 0) {
			testBounds(aLen, x, len, sup);
			return t.transfer(b, off, a, x, len, attachment);
		}
		if (x < 0) {
			int y = aLen + x;
			if (len > -x) {
				int z = len + x;
				testBounds(aLen, y, -x, sup);
				testBounds(aLen, 0, z, sup);
				return _checkedLimited(b, a, attachment, t,
						off, y, -x,
						off - x, 0, z);
			}
			testBounds(aLen, y, len, sup);
			return t.transfer(b, off, a, y, len, attachment);
		}
		testBounds(aLen, x, len, sup);
		return t.transfer(b, off, a, x, len, attachment);
	}

	protected static final <A, B, C, E extends RuntimeException, X extends Throwable> int _writeCheckedBackwards(A a, int aLen, int s, int i, int n,
			int index, B b, int off, int len, CheckedLimitedTransfer<B, A, C, X> t, C attachment, Supplier<E> sup) throws E, X {
		int gapOff = i + n;
		if (index < n) {
			// target head
			int p = gapOff - aLen;
			int r = n - index;
			if (len > r) {
				// range bridges gap
				int q = gapOff - s;
				int x = len - r;
				if (p >= 0) {
					int y = aLen - i;
					if (index < y) {
						// index on right side
						int z = y - index;
						int u = i + index;
						int v = r - z;
						testBounds(aLen, u, z, sup);
						testBounds(aLen, 0, v, sup);
						testBounds(aLen, q, x, sup);
						return _checkedLimitedBackwards(b, a, attachment, t,
								off, u, z,
								off + z, 0, v,
								off + r, q, x);
					}
					// index on left side
					int z = index - y;
					testBounds(aLen, z, r, sup);
					testBounds(aLen, q, x, sup);
					return _checkedLimitedBackwards(b, a, attachment, t,
							off, z, r,
							off + r, q, x);
				}
				// head is unfragmented
				int y = i + index;
				int offR = off + r;
				if (q >= 0) {
					testBounds(aLen, y, r, sup);
					testBounds(aLen, q, x, sup);
					return _checkedLimitedBackwards(b, a, attachment, t,
							off, y, r,
							offR, q, x);
				}
				int z = aLen + q;
				if (x > -q) {
					int u = x + q;
					testBounds(aLen, y, r, sup);
					testBounds(aLen, z, -q, sup);
					testBounds(aLen, 0, u, sup);
					return _checkedLimitedBackwards(b, a, attachment, t,
							off, y, r,
							offR, z, -q,
							offR - q, 0, u);
				}
				testBounds(aLen, y, r, sup);
				testBounds(aLen, z, x, sup);
				return _checkedLimitedBackwards(b, a, attachment, t,
						off, y, r,
						offR, z, x);
			}
			// range confined to head
			if (p >= 0) {
				int x = aLen - i;
				if (index < x) {
					int y = x - index;
					int z = i + index;
					if (len > y) {
						int u = len - y;
						testBounds(aLen, z, y, sup);
						testBounds(aLen, 0, u, sup);
						return _checkedLimitedBackwards(b, a, attachment, t,
								off, z, y,
								off + y, 0, u);
					}
					testBounds(aLen, z, len, sup);
					return t.transfer(b, off, a, z, len, attachment);
				}
				int y = index - x;
				testBounds(aLen, y, len, sup);
				return t.transfer(b, off, a, y, len, attachment);
			}
			int x = i + index;
			testBounds(aLen, x, len, sup);
			return t.transfer(b, off, a, x, len, attachment);
		}
		// target tail
		int x = i + index - s;
		int q = gapOff - s;
		if (q >= 0) {
			testBounds(aLen, x, len, sup);
			return t.transfer(b, off, a, x, len, attachment);
		}
		if (x < 0) {
			int y = aLen + x;
			if (len > -x) {
				int z = len + x;
				testBounds(aLen, y, -x, sup);
				testBounds(aLen, 0, z, sup);
				return _checkedLimitedBackwards(b, a, attachment, t,
						off, y, -x,
						off - x, 0, z);
			}
			testBounds(aLen, y, len, sup);
			return t.transfer(b, off, a, y, len, attachment);
		}
		testBounds(aLen, x, len, sup);
		return t.transfer(b, off, a, x, len, attachment);
	}

	protected static final <A> int _accumulate(A a, int aLen, int s, int i, int n,
			int index, int len, int initialValue, IntAccumulator<A> accum) throws ArrayIndexOutOfBoundsException {
		int gapOff = i + n;
		if (index < n) {
			// target head
			int r = n - index;
			if (len > r) {
				// range bridges gap
				int p = gapOff - aLen;
				int q = gapOff - s;
				if (p >= 0) {
					int x = aLen - i;
					if (index < x) {
						// index on right side
						int y = x - index;
						return _accumulate(a, initialValue, accum,
								i + index, y, 0, r - y, q, len - r);
					}
					// index on left side
					return _accumulate(a, initialValue, accum,
							index - x, r, q, len - r);
				}
				// head is unfragmented
				int y = len - r;
				if (q >= 0) {
					return _accumulate(a, initialValue, accum,
							i + index, r, q, y);
				}
				if (y > -q) {
					return _accumulate(a, initialValue, accum,
							i + index, r, aLen + q, -q, 0, y + q);
				}
				return _accumulate(a, initialValue, accum,
						i + index, r, aLen + q, y);
			}
			// range confined to head
			int p = gapOff - aLen;
			if (p >= 0) {
				int x = aLen - i;
				if (index < x) {
					int y = x - index;
					if (len > y) {
						return _accumulate(a, initialValue, accum,
								i + index, y, 0, len - y);
					}
					return accum.accumulate(a, i + index, len, initialValue);
				}
				return accum.accumulate(a, index - x, len, initialValue);
			}
			return accum.accumulate(a, i + index, len, initialValue);
		}
		// target tail
		int x = i + index - s;
		int q = gapOff - s;
		if (q >= 0) {
			return accum.accumulate(a, x, len, initialValue);
		}
		if (x < 0) {
			if (len > -x) {
				return _accumulate(a, initialValue, accum,
						aLen + x, -x, 0, len + x);
			}
			return accum.accumulate(a, aLen + x, len, initialValue);
		}
		return accum.accumulate(a, x, len, initialValue);
	}

	protected static final <A> int _accumulateBackwards(A a, int aLen, int s, int i, int n,
			int index, int len, int initialValue, IntAccumulator<A> accum) throws ArrayIndexOutOfBoundsException {
		int gapOff = i + n;
		if (index < n) {
			// target head
			int r = n - index;
			if (len > r) {
				// range bridges gap
				int p = gapOff - aLen;
				int q = gapOff - s;
				if (p >= 0) {
					int x = aLen - i;
					if (index < x) {
						// index on right side
						int y = x - index;
						return _accumulateBackwards(a, initialValue, accum,
								i + index, y, 0, r - y, q, len - r);
					}
					// index on left side
					return _accumulateBackwards(a, initialValue, accum,
							index - x, r, q, len - r);
				}
				// head is unfragmented
				int y = len - r;
				if (q >= 0) {
					return _accumulateBackwards(a, initialValue, accum,
							i + index, r, q, y);
				}
				if (y > -q) {
					return _accumulateBackwards(a, initialValue, accum,
							i + index, r, aLen + q, -q, 0, y + q);
				}
				return _accumulateBackwards(a, initialValue, accum,
						i + index, r, aLen + q, y);
			}
			// range confined to head
			int p = gapOff - aLen;
			if (p >= 0) {
				int x = aLen - i;
				if (index < x) {
					int y = x - index;
					if (len > y) {
						return _accumulateBackwards(a, initialValue, accum,
								i + index, y, 0, len - y);
					}
					return accum.accumulate(a, i + index, len, initialValue);
				}
				return accum.accumulate(a, index - x, len, initialValue);
			}
			return accum.accumulate(a, i + index, len, initialValue);
		}
		// target tail
		int x = i + index - s;
		int q = gapOff - s;
		if (q >= 0) {
			return accum.accumulate(a, x, len, initialValue);
		}
		if (x < 0) {
			if (len > -x) {
				return _accumulateBackwards(a, initialValue, accum,
						aLen + x, -x, 0, len + x);
			}
			return accum.accumulate(a, aLen + x, len, initialValue);
		}
		return accum.accumulate(a, x, len, initialValue);
	}

	protected static final <A, E extends RuntimeException> int _accumulate(A a, int aLen, int s, int i, int n,
			int index, int len, int initialValue, IntAccumulator<A> accum, Supplier<E> sup) throws E {
		int gapOff = i + n;
		if (index < n) {
			// target head
			int p = gapOff - aLen;
			int r = n - index;
			if (len > r) {
				// range bridges gap
				int q = gapOff - s;
				int x = len - r;
				if (p >= 0) {
					int y = aLen - i;
					if (index < y) {
						// index on right side
						int z = y - index;
						int u = i + index;
						int v = r - z;
						testBounds(aLen, u, z, sup);
						testBounds(aLen, 0, v, sup);
						testBounds(aLen, q, x, sup);
						return _accumulate(a, initialValue, accum,
								u, z, 0, v, q, x);
					}
					// index on left side
					int z = index - y;
					testBounds(aLen, z, r, sup);
					testBounds(aLen, q, x, sup);
					return _accumulate(a, initialValue, accum,
							z, r, q, x);
				}
				// head is unfragmented
				int y = i + index;
				if (q >= 0) {
					testBounds(aLen, y, r, sup);
					testBounds(aLen, q, x, sup);
					return _accumulate(a, initialValue, accum,
							y, r, q, x);
				}
				int z = aLen + q;
				if (x > -q) {
					int u = x + q;
					testBounds(aLen, y, r, sup);
					testBounds(aLen, z, -q, sup);
					testBounds(aLen, 0, u, sup);
					return _accumulate(a, initialValue, accum,
							y, r, z, -q, 0, u);
				}
				testBounds(aLen, y, r, sup);
				testBounds(aLen, z, x, sup);
				return _accumulate(a, initialValue, accum,
						y, r, z, x);
			}
			// range confined to head
			if (p >= 0) {
				int x = aLen - i;
				if (index < x) {
					int y = x - index;
					int z = i + index;
					if (len > y) {
						int u = len - y;
						testBounds(aLen, z, y, sup);
						testBounds(aLen, 0, u, sup);
						return _accumulate(a, initialValue, accum,
								z, y, 0, u);
					}
					testBounds(aLen, z, len, sup);
					return accum.accumulate(a, z, len, initialValue);
				}
				int y = index - x;
				testBounds(aLen, y, len, sup);
				return accum.accumulate(a, y, len, initialValue);
			}
			int x = i + index;
			testBounds(aLen, x, len, sup);
			return accum.accumulate(a, x, len, initialValue);
		}
		// target tail
		int x = i + index - s;
		int q = gapOff - s;
		if (q >= 0) {
			testBounds(aLen, x, len, sup);
			return accum.accumulate(a, x, len, initialValue);
		}
		if (x < 0) {
			int y = aLen + x;
			if (len > -x) {
				int z = len + x;
				testBounds(aLen, y, -x, sup);
				testBounds(aLen, 0, z, sup);
				return _accumulate(a, initialValue, accum,
						y, -x, 0, z);
			}
			testBounds(aLen, y, len, sup);
			return accum.accumulate(a, y, len, initialValue);
		}
		testBounds(aLen, x, len, sup);
		return accum.accumulate(a, x, len, initialValue);
	}

	protected static final <A, E extends RuntimeException> int _accumulateBackwards(A a, int aLen, int s, int i, int n,
			int index, int len, int initialValue, IntAccumulator<A> accum, Supplier<E> sup) throws E {
		int gapOff = i + n;
		if (index < n) {
			// target head
			int p = gapOff - aLen;
			int r = n - index;
			if (len > r) {
				// range bridges gap
				int q = gapOff - s;
				int x = len - r;
				if (p >= 0) {
					int y = aLen - i;
					if (index < y) {
						// index on right side
						int z = y - index;
						int u = i + index;
						int v = r - z;
						testBounds(aLen, u, z, sup);
						testBounds(aLen, 0, v, sup);
						testBounds(aLen, q, x, sup);
						return _accumulateBackwards(a, initialValue, accum,
								u, z, 0, v, q, x);
					}
					// index on left side
					int z = index - y;
					testBounds(aLen, z, r, sup);
					testBounds(aLen, q, x, sup);
					return _accumulateBackwards(a, initialValue, accum,
							z, r, q, x);
				}
				// head is unfragmented
				int y = i + index;
				if (q >= 0) {
					testBounds(aLen, y, r, sup);
					testBounds(aLen, q, x, sup);
					return _accumulateBackwards(a, initialValue, accum,
							y, r, q, x);
				}
				int z = aLen + q;
				if (x > -q) {
					int u = x + q;
					testBounds(aLen, y, r, sup);
					testBounds(aLen, z, -q, sup);
					testBounds(aLen, 0, u, sup);
					return _accumulateBackwards(a, initialValue, accum,
							y, r, z, -q, 0, u);
				}
				testBounds(aLen, y, r, sup);
				testBounds(aLen, z, x, sup);
				return _accumulateBackwards(a, initialValue, accum,
						y, r, z, x);
			}
			// range confined to head
			if (p >= 0) {
				int x = aLen - i;
				if (index < x) {
					int y = x - index;
					int z = i + index;
					if (len > y) {
						int u = len - y;
						testBounds(aLen, z, y, sup);
						testBounds(aLen, 0, u, sup);
						return _accumulateBackwards(a, initialValue, accum,
								z, y, 0, u);
					}
					testBounds(aLen, z, len, sup);
					return accum.accumulate(a, z, len, initialValue);
				}
				int y = index - x;
				testBounds(aLen, y, len, sup);
				return accum.accumulate(a, y, len, initialValue);
			}
			int x = i + index;
			testBounds(aLen, x, len, sup);
			return accum.accumulate(a, x, len, initialValue);
		}
		// target tail
		int x = i + index - s;
		int q = gapOff - s;
		if (q >= 0) {
			testBounds(aLen, x, len, sup);
			return accum.accumulate(a, x, len, initialValue);
		}
		if (x < 0) {
			int y = aLen + x;
			if (len > -x) {
				int z = len + x;
				testBounds(aLen, y, -x, sup);
				testBounds(aLen, 0, z, sup);
				return _accumulateBackwards(a, initialValue, accum,
						y, -x, 0, z);
			}
			testBounds(aLen, y, len, sup);
			return accum.accumulate(a, y, len, initialValue);
		}
		testBounds(aLen, x, len, sup);
		return accum.accumulate(a, x, len, initialValue);
	}

	protected static final <A, X extends Throwable> int _accumulateChecked(A a, int aLen, int s, int i, int n,
			int index, int len, int initialValue, CheckedIntAccumulator<A, X> accum) throws ArrayIndexOutOfBoundsException, X {
		int gapOff = i + n;
		if (index < n) {
			// target head
			int r = n - index;
			if (len > r) {
				// range bridges gap
				int p = gapOff - aLen;
				int q = gapOff - s;
				if (p >= 0) {
					int x = aLen - i;
					if (index < x) {
						// index on right side
						int y = x - index;
						return _checkedAccumulate(a, initialValue, accum,
								i + index, y, 0, r - y, q, len - r);
					}
					// index on left side
					return _checkedAccumulate(a, initialValue, accum,
							index - x, r, q, len - r);
				}
				// head is unfragmented
				int y = len - r;
				if (q >= 0) {
					return _checkedAccumulate(a, initialValue, accum,
							i + index, r, q, y);
				}
				if (y > -q) {
					return _checkedAccumulate(a, initialValue, accum,
							i + index, r, aLen + q, -q, 0, y + q);
				}
				return _checkedAccumulate(a, initialValue, accum,
						i + index, r, aLen + q, y);
			}
			// range confined to head
			int p = gapOff - aLen;
			if (p >= 0) {
				int x = aLen - i;
				if (index < x) {
					int y = x - index;
					if (len > y) {
						return _checkedAccumulate(a, initialValue, accum,
								i + index, y, 0, len - y);
					}
					return accum.accumulate(a, i + index, len, initialValue);
				}
				return accum.accumulate(a, index - x, len, initialValue);
			}
			return accum.accumulate(a, i + index, len, initialValue);
		}
		// target tail
		int x = i + index - s;
		int q = gapOff - s;
		if (q >= 0) {
			return accum.accumulate(a, x, len, initialValue);
		}
		if (x < 0) {
			if (len > -x) {
				return _checkedAccumulate(a, initialValue, accum,
						aLen + x, -x, 0, len + x);
			}
			return accum.accumulate(a, aLen + x, len, initialValue);
		}
		return accum.accumulate(a, x, len, initialValue);
	}

	protected static final <A, X extends Throwable> int _accumulateCheckedBackwards(A a, int aLen, int s, int i, int n,
			int index, int len, int initialValue, CheckedIntAccumulator<A, X> accum) throws ArrayIndexOutOfBoundsException, X {
		int gapOff = i + n;
		if (index < n) {
			// target head
			int r = n - index;
			if (len > r) {
				// range bridges gap
				int p = gapOff - aLen;
				int q = gapOff - s;
				if (p >= 0) {
					int x = aLen - i;
					if (index < x) {
						// index on right side
						int y = x - index;
						return _checkedAccumulateBackwards(a, initialValue, accum,
								i + index, y, 0, r - y, q, len - r);
					}
					// index on left side
					return _checkedAccumulateBackwards(a, initialValue, accum,
							index - x, r, q, len - r);
				}
				// head is unfragmented
				int y = len - r;
				if (q >= 0) {
					return _checkedAccumulateBackwards(a, initialValue, accum,
							i + index, r, q, y);
				}
				if (y > -q) {
					return _checkedAccumulateBackwards(a, initialValue, accum,
							i + index, r, aLen + q, -q, 0, y + q);
				}
				return _checkedAccumulateBackwards(a, initialValue, accum,
						i + index, r, aLen + q, y);
			}
			// range confined to head
			int p = gapOff - aLen;
			if (p >= 0) {
				int x = aLen - i;
				if (index < x) {
					int y = x - index;
					if (len > y) {
						return _checkedAccumulateBackwards(a, initialValue, accum,
								i + index, y, 0, len - y);
					}
					return accum.accumulate(a, i + index, len, initialValue);
				}
				return accum.accumulate(a, index - x, len, initialValue);
			}
			return accum.accumulate(a, i + index, len, initialValue);
		}
		// target tail
		int x = i + index - s;
		int q = gapOff - s;
		if (q >= 0) {
			return accum.accumulate(a, x, len, initialValue);
		}
		if (x < 0) {
			if (len > -x) {
				return _checkedAccumulateBackwards(a, initialValue, accum,
						aLen + x, -x, 0, len + x);
			}
			return accum.accumulate(a, aLen + x, len, initialValue);
		}
		return accum.accumulate(a, x, len, initialValue);
	}

	protected static final <A, E extends RuntimeException, X extends Throwable> int _accumulateChecked(A a, int aLen, int s, int i, int n,
			int index, int len, int initialValue, CheckedIntAccumulator<A, X> accum, Supplier<E> sup) throws E, X {
		int gapOff = i + n;
		if (index < n) {
			// target head
			int p = gapOff - aLen;
			int r = n - index;
			if (len > r) {
				// range bridges gap
				int q = gapOff - s;
				int x = len - r;
				if (p >= 0) {
					int y = aLen - i;
					if (index < y) {
						// index on right side
						int z = y - index;
						int u = i + index;
						int v = r - z;
						testBounds(aLen, u, z, sup);
						testBounds(aLen, 0, v, sup);
						testBounds(aLen, q, x, sup);
						return _checkedAccumulate(a, initialValue, accum,
								u, z, 0, v, q, x);
					}
					// index on left side
					int z = index - y;
					testBounds(aLen, z, r, sup);
					testBounds(aLen, q, x, sup);
					return _checkedAccumulate(a, initialValue, accum,
							z, r, q, x);
				}
				// head is unfragmented
				int y = i + index;
				if (q >= 0) {
					testBounds(aLen, y, r, sup);
					testBounds(aLen, q, x, sup);
					return _checkedAccumulate(a, initialValue, accum,
							y, r, q, x);
				}
				int z = aLen + q;
				if (x > -q) {
					int u = x + q;
					testBounds(aLen, y, r, sup);
					testBounds(aLen, z, -q, sup);
					testBounds(aLen, 0, u, sup);
					return _checkedAccumulate(a, initialValue, accum,
							y, r, z, -q, 0, u);
				}
				testBounds(aLen, y, r, sup);
				testBounds(aLen, z, x, sup);
				return _checkedAccumulate(a, initialValue, accum,
						y, r, z, x);
			}
			// range confined to head
			if (p >= 0) {
				int x = aLen - i;
				if (index < x) {
					int y = x - index;
					int z = i + index;
					if (len > y) {
						int u = len - y;
						testBounds(aLen, z, y, sup);
						testBounds(aLen, 0, u, sup);
						return _checkedAccumulate(a, initialValue, accum,
								z, y, 0, u);
					}
					testBounds(aLen, z, len, sup);
					return accum.accumulate(a, z, len, initialValue);
				}
				int y = index - x;
				testBounds(aLen, y, len, sup);
				return accum.accumulate(a, y, len, initialValue);
			}
			int x = i + index;
			testBounds(aLen, x, len, sup);
			return accum.accumulate(a, x, len, initialValue);
		}
		// target tail
		int x = i + index - s;
		int q = gapOff - s;
		if (q >= 0) {
			testBounds(aLen, x, len, sup);
			return accum.accumulate(a, x, len, initialValue);
		}
		if (x < 0) {
			int y = aLen + x;
			if (len > -x) {
				int z = len + x;
				testBounds(aLen, y, -x, sup);
				testBounds(aLen, 0, z, sup);
				return _checkedAccumulate(a, initialValue, accum,
						y, -x, 0, z);
			}
			testBounds(aLen, y, len, sup);
			return accum.accumulate(a, y, len, initialValue);
		}
		testBounds(aLen, x, len, sup);
		return accum.accumulate(a, x, len, initialValue);
	}

	protected static final <A, E extends RuntimeException, X extends Throwable> int _accumulateCheckedBackwards(A a, int aLen, int s, int i, int n,
			int index, int len, int initialValue, CheckedIntAccumulator<A, X> accum, Supplier<E> sup) throws E, X {
		int gapOff = i + n;
		if (index < n) {
			// target head
			int p = gapOff - aLen;
			int r = n - index;
			if (len > r) {
				// range bridges gap
				int q = gapOff - s;
				int x = len - r;
				if (p >= 0) {
					int y = aLen - i;
					if (index < y) {
						// index on right side
						int z = y - index;
						int u = i + index;
						int v = r - z;
						testBounds(aLen, u, z, sup);
						testBounds(aLen, 0, v, sup);
						testBounds(aLen, q, x, sup);
						return _checkedAccumulateBackwards(a, initialValue, accum,
								u, z, 0, v, q, x);
					}
					// index on left side
					int z = index - y;
					testBounds(aLen, z, r, sup);
					testBounds(aLen, q, x, sup);
					return _checkedAccumulateBackwards(a, initialValue, accum,
							z, r, q, x);
				}
				// head is unfragmented
				int y = i + index;
				if (q >= 0) {
					testBounds(aLen, y, r, sup);
					testBounds(aLen, q, x, sup);
					return _checkedAccumulateBackwards(a, initialValue, accum,
							y, r, q, x);
				}
				int z = aLen + q;
				if (x > -q) {
					int u = x + q;
					testBounds(aLen, y, r, sup);
					testBounds(aLen, z, -q, sup);
					testBounds(aLen, 0, u, sup);
					return _checkedAccumulateBackwards(a, initialValue, accum,
							y, r, z, -q, 0, u);
				}
				testBounds(aLen, y, r, sup);
				testBounds(aLen, z, x, sup);
				return _checkedAccumulateBackwards(a, initialValue, accum,
						y, r, z, x);
			}
			// range confined to head
			if (p >= 0) {
				int x = aLen - i;
				if (index < x) {
					int y = x - index;
					int z = i + index;
					if (len > y) {
						int u = len - y;
						testBounds(aLen, z, y, sup);
						testBounds(aLen, 0, u, sup);
						return _checkedAccumulateBackwards(a, initialValue, accum,
								z, y, 0, u);
					}
					testBounds(aLen, z, len, sup);
					return accum.accumulate(a, z, len, initialValue);
				}
				int y = index - x;
				testBounds(aLen, y, len, sup);
				return accum.accumulate(a, y, len, initialValue);
			}
			int x = i + index;
			testBounds(aLen, x, len, sup);
			return accum.accumulate(a, x, len, initialValue);
		}
		// target tail
		int x = i + index - s;
		int q = gapOff - s;
		if (q >= 0) {
			testBounds(aLen, x, len, sup);
			return accum.accumulate(a, x, len, initialValue);
		}
		if (x < 0) {
			int y = aLen + x;
			if (len > -x) {
				int z = len + x;
				testBounds(aLen, y, -x, sup);
				testBounds(aLen, 0, z, sup);
				return _checkedAccumulateBackwards(a, initialValue, accum,
						y, -x, 0, z);
			}
			testBounds(aLen, y, len, sup);
			return accum.accumulate(a, y, len, initialValue);
		}
		testBounds(aLen, x, len, sup);
		return accum.accumulate(a, x, len, initialValue);
	}

	protected static final <A> long _accumulate(A a, int aLen, int s, int i, int n,
			int index, int len, long initialValue, LongAccumulator<A> accum) throws ArrayIndexOutOfBoundsException {
		int gapOff = i + n;
		if (index < n) {
			// target head
			int r = n - index;
			if (len > r) {
				// range bridges gap
				int p = gapOff - aLen;
				int q = gapOff - s;
				if (p >= 0) {
					int x = aLen - i;
					if (index < x) {
						// index on right side
						int y = x - index;
						return _accumulate(a, initialValue, accum,
								i + index, y, 0, r - y, q, len - r);
					}
					// index on left side
					return _accumulate(a, initialValue, accum,
							index - x, r, q, len - r);
				}
				// head is unfragmented
				int y = len - r;
				if (q >= 0) {
					return _accumulate(a, initialValue, accum,
							i + index, r, q, y);
				}
				if (y > -q) {
					return _accumulate(a, initialValue, accum,
							i + index, r, aLen + q, -q, 0, y + q);
				}
				return _accumulate(a, initialValue, accum,
						i + index, r, aLen + q, y);
			}
			// range confined to head
			int p = gapOff - aLen;
			if (p >= 0) {
				int x = aLen - i;
				if (index < x) {
					int y = x - index;
					if (len > y) {
						return _accumulate(a, initialValue, accum,
								i + index, y, 0, len - y);
					}
					return accum.accumulate(a, i + index, len, initialValue);
				}
				return accum.accumulate(a, index - x, len, initialValue);
			}
			return accum.accumulate(a, i + index, len, initialValue);
		}
		// target tail
		int x = i + index - s;
		int q = gapOff - s;
		if (q >= 0) {
			return accum.accumulate(a, x, len, initialValue);
		}
		if (x < 0) {
			if (len > -x) {
				return _accumulate(a, initialValue, accum,
						aLen + x, -x, 0, len + x);
			}
			return accum.accumulate(a, aLen + x, len, initialValue);
		}
		return accum.accumulate(a, x, len, initialValue);
	}

	protected static final <A> long _accumulateBackwards(A a, int aLen, int s, int i, int n,
			int index, int len, long initialValue, LongAccumulator<A> accum) throws ArrayIndexOutOfBoundsException {
		int gapOff = i + n;
		if (index < n) {
			// target head
			int r = n - index;
			if (len > r) {
				// range bridges gap
				int p = gapOff - aLen;
				int q = gapOff - s;
				if (p >= 0) {
					int x = aLen - i;
					if (index < x) {
						// index on right side
						int y = x - index;
						return _accumulateBackwards(a, initialValue, accum,
								i + index, y, 0, r - y, q, len - r);
					}
					// index on left side
					return _accumulateBackwards(a, initialValue, accum,
							index - x, r, q, len - r);
				}
				// head is unfragmented
				int y = len - r;
				if (q >= 0) {
					return _accumulateBackwards(a, initialValue, accum,
							i + index, r, q, y);
				}
				if (y > -q) {
					return _accumulateBackwards(a, initialValue, accum,
							i + index, r, aLen + q, -q, 0, y + q);
				}
				return _accumulateBackwards(a, initialValue, accum,
						i + index, r, aLen + q, y);
			}
			// range confined to head
			int p = gapOff - aLen;
			if (p >= 0) {
				int x = aLen - i;
				if (index < x) {
					int y = x - index;
					if (len > y) {
						return _accumulateBackwards(a, initialValue, accum,
								i + index, y, 0, len - y);
					}
					return accum.accumulate(a, i + index, len, initialValue);
				}
				return accum.accumulate(a, index - x, len, initialValue);
			}
			return accum.accumulate(a, i + index, len, initialValue);
		}
		// target tail
		int x = i + index - s;
		int q = gapOff - s;
		if (q >= 0) {
			return accum.accumulate(a, x, len, initialValue);
		}
		if (x < 0) {
			if (len > -x) {
				return _accumulateBackwards(a, initialValue, accum,
						aLen + x, -x, 0, len + x);
			}
			return accum.accumulate(a, aLen + x, len, initialValue);
		}
		return accum.accumulate(a, x, len, initialValue);
	}

	protected static final <A, E extends RuntimeException> long _accumulate(A a, int aLen, int s, int i, int n,
			int index, int len, long initialValue, LongAccumulator<A> accum, Supplier<E> sup) throws E {
		int gapOff = i + n;
		if (index < n) {
			// target head
			int p = gapOff - aLen;
			int r = n - index;
			if (len > r) {
				// range bridges gap
				int q = gapOff - s;
				int x = len - r;
				if (p >= 0) {
					int y = aLen - i;
					if (index < y) {
						// index on right side
						int z = y - index;
						int u = i + index;
						int v = r - z;
						testBounds(aLen, u, z, sup);
						testBounds(aLen, 0, v, sup);
						testBounds(aLen, q, x, sup);
						return _accumulate(a, initialValue, accum,
								u, z, 0, v, q, x);
					}
					// index on left side
					int z = index - y;
					testBounds(aLen, z, r, sup);
					testBounds(aLen, q, x, sup);
					return _accumulate(a, initialValue, accum,
							z, r, q, x);
				}
				// head is unfragmented
				int y = i + index;
				if (q >= 0) {
					testBounds(aLen, y, r, sup);
					testBounds(aLen, q, x, sup);
					return _accumulate(a, initialValue, accum,
							y, r, q, x);
				}
				int z = aLen + q;
				if (x > -q) {
					int u = x + q;
					testBounds(aLen, y, r, sup);
					testBounds(aLen, z, -q, sup);
					testBounds(aLen, 0, u, sup);
					return _accumulate(a, initialValue, accum,
							y, r, z, -q, 0, u);
				}
				testBounds(aLen, y, r, sup);
				testBounds(aLen, z, x, sup);
				return _accumulate(a, initialValue, accum,
						y, r, z, x);
			}
			// range confined to head
			if (p >= 0) {
				int x = aLen - i;
				if (index < x) {
					int y = x - index;
					int z = i + index;
					if (len > y) {
						int u = len - y;
						testBounds(aLen, z, y, sup);
						testBounds(aLen, 0, u, sup);
						return _accumulate(a, initialValue, accum,
								z, y, 0, u);
					}
					testBounds(aLen, z, len, sup);
					return accum.accumulate(a, z, len, initialValue);
				}
				int y = index - x;
				testBounds(aLen, y, len, sup);
				return accum.accumulate(a, y, len, initialValue);
			}
			int x = i + index;
			testBounds(aLen, x, len, sup);
			return accum.accumulate(a, x, len, initialValue);
		}
		// target tail
		int x = i + index - s;
		int q = gapOff - s;
		if (q >= 0) {
			testBounds(aLen, x, len, sup);
			return accum.accumulate(a, x, len, initialValue);
		}
		if (x < 0) {
			int y = aLen + x;
			if (len > -x) {
				int z = len + x;
				testBounds(aLen, y, -x, sup);
				testBounds(aLen, 0, z, sup);
				return _accumulate(a, initialValue, accum,
						y, -x, 0, z);
			}
			testBounds(aLen, y, len, sup);
			return accum.accumulate(a, y, len, initialValue);
		}
		testBounds(aLen, x, len, sup);
		return accum.accumulate(a, x, len, initialValue);
	}

	protected static final <A, E extends RuntimeException> long _accumulateBackwards(A a, int aLen, int s, int i, int n,
			int index, int len, long initialValue, LongAccumulator<A> accum, Supplier<E> sup) throws E {
		int gapOff = i + n;
		if (index < n) {
			// target head
			int p = gapOff - aLen;
			int r = n - index;
			if (len > r) {
				// range bridges gap
				int q = gapOff - s;
				int x = len - r;
				if (p >= 0) {
					int y = aLen - i;
					if (index < y) {
						// index on right side
						int z = y - index;
						int u = i + index;
						int v = r - z;
						testBounds(aLen, u, z, sup);
						testBounds(aLen, 0, v, sup);
						testBounds(aLen, q, x, sup);
						return _accumulateBackwards(a, initialValue, accum,
								u, z, 0, v, q, x);
					}
					// index on left side
					int z = index - y;
					testBounds(aLen, z, r, sup);
					testBounds(aLen, q, x, sup);
					return _accumulateBackwards(a, initialValue, accum,
							z, r, q, x);
				}
				// head is unfragmented
				int y = i + index;
				if (q >= 0) {
					testBounds(aLen, y, r, sup);
					testBounds(aLen, q, x, sup);
					return _accumulateBackwards(a, initialValue, accum,
							y, r, q, x);
				}
				int z = aLen + q;
				if (x > -q) {
					int u = x + q;
					testBounds(aLen, y, r, sup);
					testBounds(aLen, z, -q, sup);
					testBounds(aLen, 0, u, sup);
					return _accumulateBackwards(a, initialValue, accum,
							y, r, z, -q, 0, u);
				}
				testBounds(aLen, y, r, sup);
				testBounds(aLen, z, x, sup);
				return _accumulateBackwards(a, initialValue, accum,
						y, r, z, x);
			}
			// range confined to head
			if (p >= 0) {
				int x = aLen - i;
				if (index < x) {
					int y = x - index;
					int z = i + index;
					if (len > y) {
						int u = len - y;
						testBounds(aLen, z, y, sup);
						testBounds(aLen, 0, u, sup);
						return _accumulateBackwards(a, initialValue, accum,
								z, y, 0, u);
					}
					testBounds(aLen, z, len, sup);
					return accum.accumulate(a, z, len, initialValue);
				}
				int y = index - x;
				testBounds(aLen, y, len, sup);
				return accum.accumulate(a, y, len, initialValue);
			}
			int x = i + index;
			testBounds(aLen, x, len, sup);
			return accum.accumulate(a, x, len, initialValue);
		}
		// target tail
		int x = i + index - s;
		int q = gapOff - s;
		if (q >= 0) {
			testBounds(aLen, x, len, sup);
			return accum.accumulate(a, x, len, initialValue);
		}
		if (x < 0) {
			int y = aLen + x;
			if (len > -x) {
				int z = len + x;
				testBounds(aLen, y, -x, sup);
				testBounds(aLen, 0, z, sup);
				return _accumulateBackwards(a, initialValue, accum,
						y, -x, 0, z);
			}
			testBounds(aLen, y, len, sup);
			return accum.accumulate(a, y, len, initialValue);
		}
		testBounds(aLen, x, len, sup);
		return accum.accumulate(a, x, len, initialValue);
	}

	protected static final <A, X extends Throwable> long _accumulateChecked(A a, int aLen, int s, int i, int n,
			int index, int len, long initialValue, CheckedLongAccumulator<A, X> accum) throws ArrayIndexOutOfBoundsException, X {
		int gapOff = i + n;
		if (index < n) {
			// target head
			int r = n - index;
			if (len > r) {
				// range bridges gap
				int p = gapOff - aLen;
				int q = gapOff - s;
				if (p >= 0) {
					int x = aLen - i;
					if (index < x) {
						// index on right side
						int y = x - index;
						return _checkedAccumulate(a, initialValue, accum,
								i + index, y, 0, r - y, q, len - r);
					}
					// index on left side
					return _checkedAccumulate(a, initialValue, accum,
							index - x, r, q, len - r);
				}
				// head is unfragmented
				int y = len - r;
				if (q >= 0) {
					return _checkedAccumulate(a, initialValue, accum,
							i + index, r, q, y);
				}
				if (y > -q) {
					return _checkedAccumulate(a, initialValue, accum,
							i + index, r, aLen + q, -q, 0, y + q);
				}
				return _checkedAccumulate(a, initialValue, accum,
						i + index, r, aLen + q, y);
			}
			// range confined to head
			int p = gapOff - aLen;
			if (p >= 0) {
				int x = aLen - i;
				if (index < x) {
					int y = x - index;
					if (len > y) {
						return _checkedAccumulate(a, initialValue, accum,
								i + index, y, 0, len - y);
					}
					return accum.accumulate(a, i + index, len, initialValue);
				}
				return accum.accumulate(a, index - x, len, initialValue);
			}
			return accum.accumulate(a, i + index, len, initialValue);
		}
		// target tail
		int x = i + index - s;
		int q = gapOff - s;
		if (q >= 0) {
			return accum.accumulate(a, x, len, initialValue);
		}
		if (x < 0) {
			if (len > -x) {
				return _checkedAccumulate(a, initialValue, accum,
						aLen + x, -x, 0, len + x);
			}
			return accum.accumulate(a, aLen + x, len, initialValue);
		}
		return accum.accumulate(a, x, len, initialValue);
	}

	protected static final <A, X extends Throwable> long _accumulateCheckedBackwards(A a, int aLen, int s, int i, int n,
			int index, int len, long initialValue, CheckedLongAccumulator<A, X> accum) throws ArrayIndexOutOfBoundsException, X {
		int gapOff = i + n;
		if (index < n) {
			// target head
			int r = n - index;
			if (len > r) {
				// range bridges gap
				int p = gapOff - aLen;
				int q = gapOff - s;
				if (p >= 0) {
					int x = aLen - i;
					if (index < x) {
						// index on right side
						int y = x - index;
						return _checkedAccumulateBackwards(a, initialValue, accum,
								i + index, y, 0, r - y, q, len - r);
					}
					// index on left side
					return _checkedAccumulateBackwards(a, initialValue, accum,
							index - x, r, q, len - r);
				}
				// head is unfragmented
				int y = len - r;
				if (q >= 0) {
					return _checkedAccumulateBackwards(a, initialValue, accum,
							i + index, r, q, y);
				}
				if (y > -q) {
					return _checkedAccumulateBackwards(a, initialValue, accum,
							i + index, r, aLen + q, -q, 0, y + q);
				}
				return _checkedAccumulateBackwards(a, initialValue, accum,
						i + index, r, aLen + q, y);
			}
			// range confined to head
			int p = gapOff - aLen;
			if (p >= 0) {
				int x = aLen - i;
				if (index < x) {
					int y = x - index;
					if (len > y) {
						return _checkedAccumulateBackwards(a, initialValue, accum,
								i + index, y, 0, len - y);
					}
					return accum.accumulate(a, i + index, len, initialValue);
				}
				return accum.accumulate(a, index - x, len, initialValue);
			}
			return accum.accumulate(a, i + index, len, initialValue);
		}
		// target tail
		int x = i + index - s;
		int q = gapOff - s;
		if (q >= 0) {
			return accum.accumulate(a, x, len, initialValue);
		}
		if (x < 0) {
			if (len > -x) {
				return _checkedAccumulateBackwards(a, initialValue, accum,
						aLen + x, -x, 0, len + x);
			}
			return accum.accumulate(a, aLen + x, len, initialValue);
		}
		return accum.accumulate(a, x, len, initialValue);
	}

	protected static final <A, E extends RuntimeException, X extends Throwable> long _accumulateChecked(A a, int aLen, int s, int i, int n,
			int index, int len, long initialValue, CheckedLongAccumulator<A, X> accum, Supplier<E> sup) throws E, X {
		int gapOff = i + n;
		if (index < n) {
			// target head
			int p = gapOff - aLen;
			int r = n - index;
			if (len > r) {
				// range bridges gap
				int q = gapOff - s;
				int x = len - r;
				if (p >= 0) {
					int y = aLen - i;
					if (index < y) {
						// index on right side
						int z = y - index;
						int u = i + index;
						int v = r - z;
						testBounds(aLen, u, z, sup);
						testBounds(aLen, 0, v, sup);
						testBounds(aLen, q, x, sup);
						return _checkedAccumulate(a, initialValue, accum,
								u, z, 0, v, q, x);
					}
					// index on left side
					int z = index - y;
					testBounds(aLen, z, r, sup);
					testBounds(aLen, q, x, sup);
					return _checkedAccumulate(a, initialValue, accum,
							z, r, q, x);
				}
				// head is unfragmented
				int y = i + index;
				if (q >= 0) {
					testBounds(aLen, y, r, sup);
					testBounds(aLen, q, x, sup);
					return _checkedAccumulate(a, initialValue, accum,
							y, r, q, x);
				}
				int z = aLen + q;
				if (x > -q) {
					int u = x + q;
					testBounds(aLen, y, r, sup);
					testBounds(aLen, z, -q, sup);
					testBounds(aLen, 0, u, sup);
					return _checkedAccumulate(a, initialValue, accum,
							y, r, z, -q, 0, u);
				}
				testBounds(aLen, y, r, sup);
				testBounds(aLen, z, x, sup);
				return _checkedAccumulate(a, initialValue, accum,
						y, r, z, x);
			}
			// range confined to head
			if (p >= 0) {
				int x = aLen - i;
				if (index < x) {
					int y = x - index;
					int z = i + index;
					if (len > y) {
						int u = len - y;
						testBounds(aLen, z, y, sup);
						testBounds(aLen, 0, u, sup);
						return _checkedAccumulate(a, initialValue, accum,
								z, y, 0, u);
					}
					testBounds(aLen, z, len, sup);
					return accum.accumulate(a, z, len, initialValue);
				}
				int y = index - x;
				testBounds(aLen, y, len, sup);
				return accum.accumulate(a, y, len, initialValue);
			}
			int x = i + index;
			testBounds(aLen, x, len, sup);
			return accum.accumulate(a, x, len, initialValue);
		}
		// target tail
		int x = i + index - s;
		int q = gapOff - s;
		if (q >= 0) {
			testBounds(aLen, x, len, sup);
			return accum.accumulate(a, x, len, initialValue);
		}
		if (x < 0) {
			int y = aLen + x;
			if (len > -x) {
				int z = len + x;
				testBounds(aLen, y, -x, sup);
				testBounds(aLen, 0, z, sup);
				return _checkedAccumulate(a, initialValue, accum,
						y, -x, 0, z);
			}
			testBounds(aLen, y, len, sup);
			return accum.accumulate(a, y, len, initialValue);
		}
		testBounds(aLen, x, len, sup);
		return accum.accumulate(a, x, len, initialValue);
	}

	protected static final <A, E extends RuntimeException, X extends Throwable> long _accumulateCheckedBackwards(A a, int aLen, int s, int i, int n,
			int index, int len, long initialValue, CheckedLongAccumulator<A, X> accum, Supplier<E> sup) throws E, X {
		int gapOff = i + n;
		if (index < n) {
			// target head
			int p = gapOff - aLen;
			int r = n - index;
			if (len > r) {
				// range bridges gap
				int q = gapOff - s;
				int x = len - r;
				if (p >= 0) {
					int y = aLen - i;
					if (index < y) {
						// index on right side
						int z = y - index;
						int u = i + index;
						int v = r - z;
						testBounds(aLen, u, z, sup);
						testBounds(aLen, 0, v, sup);
						testBounds(aLen, q, x, sup);
						return _checkedAccumulateBackwards(a, initialValue, accum,
								u, z, 0, v, q, x);
					}
					// index on left side
					int z = index - y;
					testBounds(aLen, z, r, sup);
					testBounds(aLen, q, x, sup);
					return _checkedAccumulateBackwards(a, initialValue, accum,
							z, r, q, x);
				}
				// head is unfragmented
				int y = i + index;
				if (q >= 0) {
					testBounds(aLen, y, r, sup);
					testBounds(aLen, q, x, sup);
					return _checkedAccumulateBackwards(a, initialValue, accum,
							y, r, q, x);
				}
				int z = aLen + q;
				if (x > -q) {
					int u = x + q;
					testBounds(aLen, y, r, sup);
					testBounds(aLen, z, -q, sup);
					testBounds(aLen, 0, u, sup);
					return _checkedAccumulateBackwards(a, initialValue, accum,
							y, r, z, -q, 0, u);
				}
				testBounds(aLen, y, r, sup);
				testBounds(aLen, z, x, sup);
				return _checkedAccumulateBackwards(a, initialValue, accum,
						y, r, z, x);
			}
			// range confined to head
			if (p >= 0) {
				int x = aLen - i;
				if (index < x) {
					int y = x - index;
					int z = i + index;
					if (len > y) {
						int u = len - y;
						testBounds(aLen, z, y, sup);
						testBounds(aLen, 0, u, sup);
						return _checkedAccumulateBackwards(a, initialValue, accum,
								z, y, 0, u);
					}
					testBounds(aLen, z, len, sup);
					return accum.accumulate(a, z, len, initialValue);
				}
				int y = index - x;
				testBounds(aLen, y, len, sup);
				return accum.accumulate(a, y, len, initialValue);
			}
			int x = i + index;
			testBounds(aLen, x, len, sup);
			return accum.accumulate(a, x, len, initialValue);
		}
		// target tail
		int x = i + index - s;
		int q = gapOff - s;
		if (q >= 0) {
			testBounds(aLen, x, len, sup);
			return accum.accumulate(a, x, len, initialValue);
		}
		if (x < 0) {
			int y = aLen + x;
			if (len > -x) {
				int z = len + x;
				testBounds(aLen, y, -x, sup);
				testBounds(aLen, 0, z, sup);
				return _checkedAccumulateBackwards(a, initialValue, accum,
						y, -x, 0, z);
			}
			testBounds(aLen, y, len, sup);
			return accum.accumulate(a, y, len, initialValue);
		}
		testBounds(aLen, x, len, sup);
		return accum.accumulate(a, x, len, initialValue);
	}

	protected static final <A, B> int _indexOf(A a, int aLen, int s, int i, int n,
			int index, int len, boolean physical, Evaluator<A, B> ev, B attachment1, boolean attachment2) throws ArrayIndexOutOfBoundsException {
		int gapOff = i + n;
		if (index < n) {
			// target head
			int p = gapOff - aLen;
			int r = n - index;
			if (len > r) {
				// range bridges gap
				int q = gapOff - s;
				int x = len - r;
				if (p >= 0) {
					int y = aLen - i;
					if (index < y) {
						// index on right side
						int z = y - index;
						int u = i + index;
						return _indexOf(a, physical, ev, attachment1, attachment2,
								u, z, 0, r - z, q, x);
					}
					// index on left side
					int z = index - y;
					return _indexOf(a, physical, ev, attachment1, attachment2,
							z, r, q, x);
				}
				// head is unfragmented
				int y = i + index;
				if (q >= 0) {
					return _indexOf(a, physical, ev, attachment1, attachment2,
							y, r, q, x);
				}
				int z = aLen + q;
				if (x > -q) {
					int u = x + q;
					return _indexOf(a, physical, ev, attachment1, attachment2,
							y, r, z, -q, 0, u);
				}
				return _indexOf(a, physical, ev, attachment1, attachment2,
						y, r, z, x);
			}
			// range confined to head
			if (p >= 0) {
				int x = aLen - i;
				if (index < x) {
					int y = x - index;
					int z = i + index;
					if (len > y) {
						int u = len - y;
						return _indexOf(a, physical, ev, attachment1, attachment2,
								z, y, 0, u);
					}
					int k;
					if ((k = ev.apply(a, z, len, attachment1, attachment2)) >= 0) {
						return physical ? k : k - z;
					}
					return -1;
				}
				int y = index - x;
				int k;
				if ((k = ev.apply(a, y, len, attachment1, attachment2)) >= 0) {
					return physical ? k : k - y;
				}
				return -1;
			}
			int x = i + index;
			int k;
			if ((k = ev.apply(a, x, len, attachment1, attachment2)) >= 0) {
				return physical ? k : k - x;
			}
			return -1;
		}
		// target tail
		int x = i + index - s;
		int q = gapOff - s;
		if (q >= 0) {
			int k;
			if ((k = ev.apply(a, x, len, attachment1, attachment2)) >= 0) {
				return physical ? k : k - x;
			}
			return -1;
		}
		if (x < 0) {
			int y = aLen + x;
			if (len > -x) {
				return _indexOf(a, physical, ev, attachment1, attachment2,
						y, -x, 0, len + x);
			}
			int k;
			if ((k = ev.apply(a, y, len, attachment1, attachment2)) >= 0) {
				return physical ? k : k - y;
			}
			return -1;
		}
		int k;
		if ((k = ev.apply(a, x, len, attachment1, attachment2)) >= 0) {
			return physical ? k : k - x;
		}
		return -1;
	}

	protected static final <A, B> int _lastIndexOf(A a, int aLen, int s, int i, int n,
			int index, int len, boolean physical, Evaluator<A, B> ev, B attachment1, boolean attachment2) throws ArrayIndexOutOfBoundsException {
		int gapOff = i + n;
		if (index < n) {
			// target head
			int p = gapOff - aLen;
			int r = n - index;
			if (len > r) {
				// range bridges gap
				int q = gapOff - s;
				int x = len - r;
				if (p >= 0) {
					int y = aLen - i;
					if (index < y) {
						// index on right side
						int z = y - index;
						int u = i + index;
						return _indexOfBackwards(a, physical, ev, attachment1, attachment2,
								u, z, 0, r - z, q, x);
					}
					// index on left side
					int z = index - y;
					return _indexOfBackwards(a, physical, ev, attachment1, attachment2,
							z, r, q, x);
				}
				// head is unfragmented
				int y = i + index;
				if (q >= 0) {
					return _indexOfBackwards(a, physical, ev, attachment1, attachment2,
							y, r, q, x);
				}
				int z = aLen + q;
				if (x > -q) {
					int u = x + q;
					return _indexOfBackwards(a, physical, ev, attachment1, attachment2,
							y, r, z, -q, 0, u);
				}
				return _indexOfBackwards(a, physical, ev, attachment1, attachment2,
						y, r, z, x);
			}
			// range confined to head
			if (p >= 0) {
				int x = aLen - i;
				if (index < x) {
					int y = x - index;
					int z = i + index;
					if (len > y) {
						int u = len - y;
						return _indexOfBackwards(a, physical, ev, attachment1, attachment2,
								z, y, 0, u);
					}
					int k;
					if ((k = ev.apply(a, z, len, attachment1, attachment2)) >= 0) {
						return physical ? k : k - z;
					}
					return -1;
				}
				int y = index - x;
				int k;
				if ((k = ev.apply(a, y, len, attachment1, attachment2)) >= 0) {
					return physical ? k : k - y;
				}
				return -1;
			}
			int x = i + index;
			int k;
			if ((k = ev.apply(a, x, len, attachment1, attachment2)) >= 0) {
				return physical ? k : k - x;
			}
			return -1;
		}
		// target tail
		int x = i + index - s;
		int q = gapOff - s;
		if (q >= 0) {
			int k;
			if ((k = ev.apply(a, x, len, attachment1, attachment2)) >= 0) {
				return physical ? k : k - x;
			}
			return -1;
		}
		if (x < 0) {
			int y = aLen + x;
			if (len > -x) {
				return _indexOfBackwards(a, physical, ev, attachment1, attachment2,
						y, -x, 0, len + x);
			}
			int k;
			if ((k = ev.apply(a, y, len, attachment1, attachment2)) >= 0) {
				return physical ? k : k - y;
			}
			return -1;
		}
		int k;
		if ((k = ev.apply(a, x, len, attachment1, attachment2)) >= 0) {
			return physical ? k : k - x;
		}
		return -1;
	}

	protected static final <A, B, E extends RuntimeException> int _indexOf(A a, int aLen, int s, int i, int n,
			int index, int len, boolean physical, Evaluator<A, B> ev, B attachment1, boolean attachment2, Supplier<E> sup) throws E {
		int gapOff = i + n;
		if (index < n) {
			// target head
			int p = gapOff - aLen;
			int r = n - index;
			if (len > r) {
				// range bridges gap
				int q = gapOff - s;
				int x = len - r;
				if (p >= 0) {
					int y = aLen - i;
					if (index < y) {
						// index on right side
						int z = y - index;
						int u = i + index;
						int v = r - z;
						testBounds(aLen, u, z, sup);
						testBounds(aLen, 0, v, sup);
						testBounds(aLen, q, x, sup);
						return _indexOf(a, physical, ev, attachment1, attachment2,
								u, z, 0, v, q, x);
					}
					// index on left side
					int z = index - y;
					testBounds(aLen, z, r, sup);
					testBounds(aLen, q, x, sup);
					return _indexOf(a, physical, ev, attachment1, attachment2,
							z, r, q, x);
				}
				// head is unfragmented
				int y = i + index;
				if (q >= 0) {
					testBounds(aLen, y, r, sup);
					testBounds(aLen, q, x, sup);
					return _indexOf(a, physical, ev, attachment1, attachment2,
							y, r, q, x);
				}
				int z = aLen + q;
				if (x > -q) {
					int u = x + q;
					testBounds(aLen, y, r, sup);
					testBounds(aLen, z, -q, sup);
					testBounds(aLen, 0, u, sup);
					return _indexOf(a, physical, ev, attachment1, attachment2,
							y, r, z, -q, 0, u);
				}
				testBounds(aLen, y, r, sup);
				testBounds(aLen, z, x, sup);
				return _indexOf(a, physical, ev, attachment1, attachment2,
						y, r, z, x);
			}
			// range confined to head
			if (p >= 0) {
				int x = aLen - i;
				if (index < x) {
					int y = x - index;
					int z = i + index;
					if (len > y) {
						int u = len - y;
						testBounds(aLen, z, y, sup);
						testBounds(aLen, 0, u, sup);
						return _indexOf(a, physical, ev, attachment1, attachment2,
								z, y, 0, u);
					}
					testBounds(aLen, z, len, sup);
					int k;
					if ((k = ev.apply(a, z, len, attachment1, attachment2)) >= 0) {
						return physical ? k : k - z;
					}
					return -1;
				}
				int y = index - x;
				testBounds(aLen, y, len, sup);
				int k;
				if ((k = ev.apply(a, y, len, attachment1, attachment2)) >= 0) {
					return physical ? k : k - y;
				}
				return -1;
			}
			int x = i + index;
			testBounds(aLen, x, len, sup);
			int k;
			if ((k = ev.apply(a, x, len, attachment1, attachment2)) >= 0) {
				return physical ? k : k - x;
			}
			return -1;
		}
		// target tail
		int x = i + index - s;
		int q = gapOff - s;
		if (q >= 0) {
			testBounds(aLen, x, len, sup);
			int k;
			if ((k = ev.apply(a, x, len, attachment1, attachment2)) >= 0) {
				return physical ? k : k - x;
			}
			return -1;
		}
		if (x < 0) {
			int y = aLen + x;
			if (len > -x) {
				int z = len + x;
				testBounds(aLen, y, -x, sup);
				testBounds(aLen, 0, z, sup);
				return _indexOf(a, physical, ev, attachment1, attachment2,
						y, -x, 0, z);
			}
			testBounds(aLen, y, len, sup);
			int k;
			if ((k = ev.apply(a, y, len, attachment1, attachment2)) >= 0) {
				return physical ? k : k - y;
			}
			return -1;
		}
		testBounds(aLen, x, len, sup);
		int k;
		if ((k = ev.apply(a, x, len, attachment1, attachment2)) >= 0) {
			return physical ? k : k - x;
		}
		return -1;
	}

	protected static final <A, B, E extends RuntimeException> int _lastIndexOf(A a, int aLen, int s, int i, int n,
			int index, int len, boolean physical, Evaluator<A, B> ev, B attachment1, boolean attachment2, Supplier<E> sup) throws E {
		int gapOff = i + n;
		if (index < n) {
			// target head
			int p = gapOff - aLen;
			int r = n - index;
			if (len > r) {
				// range bridges gap
				int q = gapOff - s;
				int x = len - r;
				if (p >= 0) {
					int y = aLen - i;
					if (index < y) {
						// index on right side
						int z = y - index;
						int u = i + index;
						int v = r - z;
						testBounds(aLen, u, z, sup);
						testBounds(aLen, 0, v, sup);
						testBounds(aLen, q, x, sup);
						return _indexOfBackwards(a, physical, ev, attachment1, attachment2,
								u, z, 0, v, q, x);
					}
					// index on left side
					int z = index - y;
					testBounds(aLen, z, r, sup);
					testBounds(aLen, q, x, sup);
					return _indexOfBackwards(a, physical, ev, attachment1, attachment2,
							z, r, q, x);
				}
				// head is unfragmented
				int y = i + index;
				if (q >= 0) {
					testBounds(aLen, y, r, sup);
					testBounds(aLen, q, x, sup);
					return _indexOfBackwards(a, physical, ev, attachment1, attachment2,
							y, r, q, x);
				}
				int z = aLen + q;
				if (x > -q) {
					int u = x + q;
					testBounds(aLen, y, r, sup);
					testBounds(aLen, z, -q, sup);
					testBounds(aLen, 0, u, sup);
					return _indexOfBackwards(a, physical, ev, attachment1, attachment2,
							y, r, z, -q, 0, u);
				}
				testBounds(aLen, y, r, sup);
				testBounds(aLen, z, x, sup);
				return _indexOfBackwards(a, physical, ev, attachment1, attachment2,
						y, r, z, x);
			}
			// range confined to head
			if (p >= 0) {
				int x = aLen - i;
				if (index < x) {
					int y = x - index;
					int z = i + index;
					if (len > y) {
						int u = len - y;
						testBounds(aLen, z, y, sup);
						testBounds(aLen, 0, u, sup);
						return _indexOfBackwards(a, physical, ev, attachment1, attachment2,
								z, y, 0, u);
					}
					testBounds(aLen, z, len, sup);
					int k;
					if ((k = ev.apply(a, z, len, attachment1, attachment2)) >= 0) {
						return physical ? k : k - z;
					}
					return -1;
				}
				int y = index - x;
				testBounds(aLen, y, len, sup);
				int k;
				if ((k = ev.apply(a, y, len, attachment1, attachment2)) >= 0) {
					return physical ? k : k - y;
				}
				return -1;
			}
			int x = i + index;
			testBounds(aLen, x, len, sup);
			int k;
			if ((k = ev.apply(a, x, len, attachment1, attachment2)) >= 0) {
				return physical ? k : k - x;
			}
			return -1;
		}
		// target tail
		int x = i + index - s;
		int q = gapOff - s;
		if (q >= 0) {
			testBounds(aLen, x, len, sup);
			int k;
			if ((k = ev.apply(a, x, len, attachment1, attachment2)) >= 0) {
				return physical ? k : k - x;
			}
			return -1;
		}
		if (x < 0) {
			int y = aLen + x;
			if (len > -x) {
				int z = len + x;
				testBounds(aLen, y, -x, sup);
				testBounds(aLen, 0, z, sup);
				return _indexOfBackwards(a, physical, ev, attachment1, attachment2,
						y, -x, 0, z);
			}
			testBounds(aLen, y, len, sup);
			int k;
			if ((k = ev.apply(a, y, len, attachment1, attachment2)) >= 0) {
				return physical ? k : k - y;
			}
			return -1;
		}
		testBounds(aLen, x, len, sup);
		int k;
		if ((k = ev.apply(a, x, len, attachment1, attachment2)) >= 0) {
			return physical ? k : k - x;
		}
		return -1;
	}

	protected static final <A, B, X extends Throwable> int _indexOfChecked(A a, int aLen, int s, int i, int n,
			int index, int len, boolean physical, CheckedEvaluator<A, B, X> ev, B attachment1, boolean attachment2) throws ArrayIndexOutOfBoundsException, X {
		int gapOff = i + n;
		if (index < n) {
			// target head
			int p = gapOff - aLen;
			int r = n - index;
			if (len > r) {
				// range bridges gap
				int q = gapOff - s;
				int x = len - r;
				if (p >= 0) {
					int y = aLen - i;
					if (index < y) {
						// index on right side
						int z = y - index;
						int u = i + index;
						return _checkedIndexOf(a, physical, ev, attachment1, attachment2,
								u, z, 0, r - z, q, x);
					}
					// index on left side
					int z = index - y;
					return _checkedIndexOf(a, physical, ev, attachment1, attachment2,
							z, r, q, x);
				}
				// head is unfragmented
				int y = i + index;
				if (q >= 0) {
					return _checkedIndexOf(a, physical, ev, attachment1, attachment2,
							y, r, q, x);
				}
				int z = aLen + q;
				if (x > -q) {
					int u = x + q;
					return _checkedIndexOf(a, physical, ev, attachment1, attachment2,
							y, r, z, -q, 0, u);
				}
				return _checkedIndexOf(a, physical, ev, attachment1, attachment2,
						y, r, z, x);
			}
			// range confined to head
			if (p >= 0) {
				int x = aLen - i;
				if (index < x) {
					int y = x - index;
					int z = i + index;
					if (len > y) {
						int u = len - y;
						return _checkedIndexOf(a, physical, ev, attachment1, attachment2,
								z, y, 0, u);
					}
					int k;
					if ((k = ev.apply(a, z, len, attachment1, attachment2)) >= 0) {
						return physical ? k : k - z;
					}
					return -1;
				}
				int y = index - x;
				int k;
				if ((k = ev.apply(a, y, len, attachment1, attachment2)) >= 0) {
					return physical ? k : k - y;
				}
				return -1;
			}
			int x = i + index;
			int k;
			if ((k = ev.apply(a, x, len, attachment1, attachment2)) >= 0) {
				return physical ? k : k - x;
			}
			return -1;
		}
		// target tail
		int x = i + index - s;
		int q = gapOff - s;
		if (q >= 0) {
			int k;
			if ((k = ev.apply(a, x, len, attachment1, attachment2)) >= 0) {
				return physical ? k : k - x;
			}
			return -1;
		}
		if (x < 0) {
			int y = aLen + x;
			if (len > -x) {
				return _checkedIndexOf(a, physical, ev, attachment1, attachment2,
						y, -x, 0, len + x);
			}
			int k;
			if ((k = ev.apply(a, y, len, attachment1, attachment2)) >= 0) {
				return physical ? k : k - y;
			}
			return -1;
		}
		int k;
		if ((k = ev.apply(a, x, len, attachment1, attachment2)) >= 0) {
			return physical ? k : k - x;
		}
		return -1;
	}

	protected static final <A, B, X extends Throwable> int _lastIndexOfChecked(A a, int aLen, int s, int i, int n,
			int index, int len, boolean physical, CheckedEvaluator<A, B, X> ev, B attachment1, boolean attachment2) throws ArrayIndexOutOfBoundsException, X {
		int gapOff = i + n;
		if (index < n) {
			// target head
			int p = gapOff - aLen;
			int r = n - index;
			if (len > r) {
				// range bridges gap
				int q = gapOff - s;
				int x = len - r;
				if (p >= 0) {
					int y = aLen - i;
					if (index < y) {
						// index on right side
						int z = y - index;
						int u = i + index;
						return _checkedIndexOfBackwards(a, physical, ev, attachment1, attachment2,
								u, z, 0, r - z, q, x);
					}
					// index on left side
					int z = index - y;
					return _checkedIndexOfBackwards(a, physical, ev, attachment1, attachment2,
							z, r, q, x);
				}
				// head is unfragmented
				int y = i + index;
				if (q >= 0) {
					return _checkedIndexOfBackwards(a, physical, ev, attachment1, attachment2,
							y, r, q, x);
				}
				int z = aLen + q;
				if (x > -q) {
					int u = x + q;
					return _checkedIndexOfBackwards(a, physical, ev, attachment1, attachment2,
							y, r, z, -q, 0, u);
				}
				return _checkedIndexOfBackwards(a, physical, ev, attachment1, attachment2,
						y, r, z, x);
			}
			// range confined to head
			if (p >= 0) {
				int x = aLen - i;
				if (index < x) {
					int y = x - index;
					int z = i + index;
					if (len > y) {
						int u = len - y;
						return _checkedIndexOfBackwards(a, physical, ev, attachment1, attachment2,
								z, y, 0, u);
					}
					int k;
					if ((k = ev.apply(a, z, len, attachment1, attachment2)) >= 0) {
						return physical ? k : k - z;
					}
					return -1;
				}
				int y = index - x;
				int k;
				if ((k = ev.apply(a, y, len, attachment1, attachment2)) >= 0) {
					return physical ? k : k - y;
				}
				return -1;
			}
			int x = i + index;
			int k;
			if ((k = ev.apply(a, x, len, attachment1, attachment2)) >= 0) {
				return physical ? k : k - x;
			}
			return -1;
		}
		// target tail
		int x = i + index - s;
		int q = gapOff - s;
		if (q >= 0) {
			int k;
			if ((k = ev.apply(a, x, len, attachment1, attachment2)) >= 0) {
				return physical ? k : k - x;
			}
			return -1;
		}
		if (x < 0) {
			int y = aLen + x;
			if (len > -x) {
				return _checkedIndexOfBackwards(a, physical, ev, attachment1, attachment2,
						y, -x, 0, len + x);
			}
			int k;
			if ((k = ev.apply(a, y, len, attachment1, attachment2)) >= 0) {
				return physical ? k : k - y;
			}
			return -1;
		}
		int k;
		if ((k = ev.apply(a, x, len, attachment1, attachment2)) >= 0) {
			return physical ? k : k - x;
		}
		return -1;
	}

	protected static final <A, B, E extends RuntimeException, X extends Throwable> int _indexOfChecked(A a, int aLen, int s, int i, int n,
			int index, int len, boolean physical, CheckedEvaluator<A, B, X> ev, B attachment1, boolean attachment2, Supplier<E> sup) throws E, X {
		int gapOff = i + n;
		if (index < n) {
			// target head
			int p = gapOff - aLen;
			int r = n - index;
			if (len > r) {
				// range bridges gap
				int q = gapOff - s;
				int x = len - r;
				if (p >= 0) {
					int y = aLen - i;
					if (index < y) {
						// index on right side
						int z = y - index;
						int u = i + index;
						int v = r - z;
						testBounds(aLen, u, z, sup);
						testBounds(aLen, 0, v, sup);
						testBounds(aLen, q, x, sup);
						return _checkedIndexOf(a, physical, ev, attachment1, attachment2,
								u, z, 0, v, q, x);
					}
					// index on left side
					int z = index - y;
					testBounds(aLen, z, r, sup);
					testBounds(aLen, q, x, sup);
					return _checkedIndexOf(a, physical, ev, attachment1, attachment2,
							z, r, q, x);
				}
				// head is unfragmented
				int y = i + index;
				if (q >= 0) {
					testBounds(aLen, y, r, sup);
					testBounds(aLen, q, x, sup);
					return _checkedIndexOf(a, physical, ev, attachment1, attachment2,
							y, r, q, x);
				}
				int z = aLen + q;
				if (x > -q) {
					int u = x + q;
					testBounds(aLen, y, r, sup);
					testBounds(aLen, z, -q, sup);
					testBounds(aLen, 0, u, sup);
					return _checkedIndexOf(a, physical, ev, attachment1, attachment2,
							y, r, z, -q, 0, u);
				}
				testBounds(aLen, y, r, sup);
				testBounds(aLen, z, x, sup);
				return _checkedIndexOf(a, physical, ev, attachment1, attachment2,
						y, r, z, x);
			}
			// range confined to head
			if (p >= 0) {
				int x = aLen - i;
				if (index < x) {
					int y = x - index;
					int z = i + index;
					if (len > y) {
						int u = len - y;
						testBounds(aLen, z, y, sup);
						testBounds(aLen, 0, u, sup);
						return _checkedIndexOf(a, physical, ev, attachment1, attachment2,
								z, y, 0, u);
					}
					testBounds(aLen, z, len, sup);
					int k;
					if ((k = ev.apply(a, z, len, attachment1, attachment2)) >= 0) {
						return physical ? k : k - z;
					}
					return -1;
				}
				int y = index - x;
				testBounds(aLen, y, len, sup);
				int k;
				if ((k = ev.apply(a, y, len, attachment1, attachment2)) >= 0) {
					return physical ? k : k - y;
				}
				return -1;
			}
			int x = i + index;
			testBounds(aLen, x, len, sup);
			int k;
			if ((k = ev.apply(a, x, len, attachment1, attachment2)) >= 0) {
				return physical ? k : k - x;
			}
			return -1;
		}
		// target tail
		int x = i + index - s;
		int q = gapOff - s;
		if (q >= 0) {
			testBounds(aLen, x, len, sup);
			int k;
			if ((k = ev.apply(a, x, len, attachment1, attachment2)) >= 0) {
				return physical ? k : k - x;
			}
			return -1;
		}
		if (x < 0) {
			int y = aLen + x;
			if (len > -x) {
				int z = len + x;
				testBounds(aLen, y, -x, sup);
				testBounds(aLen, 0, z, sup);
				return _checkedIndexOf(a, physical, ev, attachment1, attachment2,
						y, -x, 0, z);
			}
			testBounds(aLen, y, len, sup);
			int k;
			if ((k = ev.apply(a, y, len, attachment1, attachment2)) >= 0) {
				return physical ? k : k - y;
			}
			return -1;
		}
		testBounds(aLen, x, len, sup);
		int k;
		if ((k = ev.apply(a, x, len, attachment1, attachment2)) >= 0) {
			return physical ? k : k - x;
		}
		return -1;
	}

	protected static final <A, B, E extends RuntimeException, X extends Throwable> int _lastIndexOfChecked(A a, int aLen, int s, int i, int n,
			int index, int len, boolean physical, CheckedEvaluator<A, B, X> ev, B attachment1, boolean attachment2, Supplier<E> sup) throws E, X {
		int gapOff = i + n;
		if (index < n) {
			// target head
			int p = gapOff - aLen;
			int r = n - index;
			if (len > r) {
				// range bridges gap
				int q = gapOff - s;
				int x = len - r;
				if (p >= 0) {
					int y = aLen - i;
					if (index < y) {
						// index on right side
						int z = y - index;
						int u = i + index;
						int v = r - z;
						testBounds(aLen, u, z, sup);
						testBounds(aLen, 0, v, sup);
						testBounds(aLen, q, x, sup);
						return _checkedIndexOfBackwards(a, physical, ev, attachment1, attachment2,
								u, z, 0, v, q, x);
					}
					// index on left side
					int z = index - y;
					testBounds(aLen, z, r, sup);
					testBounds(aLen, q, x, sup);
					return _checkedIndexOfBackwards(a, physical, ev, attachment1, attachment2,
							z, r, q, x);
				}
				// head is unfragmented
				int y = i + index;
				if (q >= 0) {
					testBounds(aLen, y, r, sup);
					testBounds(aLen, q, x, sup);
					return _checkedIndexOfBackwards(a, physical, ev, attachment1, attachment2,
							y, r, q, x);
				}
				int z = aLen + q;
				if (x > -q) {
					int u = x + q;
					testBounds(aLen, y, r, sup);
					testBounds(aLen, z, -q, sup);
					testBounds(aLen, 0, u, sup);
					return _checkedIndexOfBackwards(a, physical, ev, attachment1, attachment2,
							y, r, z, -q, 0, u);
				}
				testBounds(aLen, y, r, sup);
				testBounds(aLen, z, x, sup);
				return _checkedIndexOfBackwards(a, physical, ev, attachment1, attachment2,
						y, r, z, x);
			}
			// range confined to head
			if (p >= 0) {
				int x = aLen - i;
				if (index < x) {
					int y = x - index;
					int z = i + index;
					if (len > y) {
						int u = len - y;
						testBounds(aLen, z, y, sup);
						testBounds(aLen, 0, u, sup);
						return _checkedIndexOfBackwards(a, physical, ev, attachment1, attachment2,
								z, y, 0, u);
					}
					testBounds(aLen, z, len, sup);
					int k;
					if ((k = ev.apply(a, z, len, attachment1, attachment2)) >= 0) {
						return physical ? k : k - z;
					}
					return -1;
				}
				int y = index - x;
				testBounds(aLen, y, len, sup);
				int k;
				if ((k = ev.apply(a, y, len, attachment1, attachment2)) >= 0) {
					return physical ? k : k - y;
				}
				return -1;
			}
			int x = i + index;
			testBounds(aLen, x, len, sup);
			int k;
			if ((k = ev.apply(a, x, len, attachment1, attachment2)) >= 0) {
				return physical ? k : k - x;
			}
			return -1;
		}
		// target tail
		int x = i + index - s;
		int q = gapOff - s;
		if (q >= 0) {
			testBounds(aLen, x, len, sup);
			int k;
			if ((k = ev.apply(a, x, len, attachment1, attachment2)) >= 0) {
				return physical ? k : k - x;
			}
			return -1;
		}
		if (x < 0) {
			int y = aLen + x;
			if (len > -x) {
				int z = len + x;
				testBounds(aLen, y, -x, sup);
				testBounds(aLen, 0, z, sup);
				return _checkedIndexOfBackwards(a, physical, ev, attachment1, attachment2,
						y, -x, 0, z);
			}
			testBounds(aLen, y, len, sup);
			int k;
			if ((k = ev.apply(a, y, len, attachment1, attachment2)) >= 0) {
				return physical ? k : k - y;
			}
			return -1;
		}
		testBounds(aLen, x, len, sup);
		int k;
		if ((k = ev.apply(a, x, len, attachment1, attachment2)) >= 0) {
			return physical ? k : k - x;
		}
		return -1;
	}

	// copies elements from gaparray "a" to gaparray "b". "a" must not be "b", because
	// elements can be lost that way if it's an overlapping copy.
	// overlapping self-copies can be achieved by first removing the overlapped elements, 
	// then inserting the non-overlapped elements. 
	// or by simply first normalizing the gaparray, then running a System.arraycopy.
	protected static final <A, B> void _copy(
			A a, int aLen1, int s1, int i1, int n1, int index1,
			B b, int aLen2, int s2, int i2, int n2, int index2,
			int len) throws ArrayIndexOutOfBoundsException {
		int gapOff = i2 + n2;
		if (index2 < n2) {
			// target head
			int r = n2 - index2;
			if (len > r) {
				// range bridges gap
				int p = gapOff - aLen2;
				int q = gapOff - s2;
				if (p >= 0) {
					int x = aLen2 - i2;
					if (index2 < x) {
						// index on right side
						int y = x - index2;
						_read(a, aLen1, s1, i1, n1, index1, b, i2 + index2, y);
						_read(a, aLen1, s1, i1, n1, index1 + y, b, 0, r - y);
						_read(a, aLen1, s1, i1, n1, index1 + r, b, q, len - r);
						return;
					}
					// index on left side
					_read(a, aLen1, s1, i1, n1, index1, b, index2 - x, r);
					_read(a, aLen1, s1, i1, n1, index1 + r, b, q, len - r);
					return;
				}
				// head is unfragmented
				int y = len - r;
				int offR = index1 + r;
				if (q >= 0) {
					_read(a, aLen1, s1, i1, n1, index1, b, i2 + index2, r);
					_read(a, aLen1, s1, i1, n1, offR, b, q, y);
					return;
				}
				if (y > -q) {
					_read(a, aLen1, s1, i1, n1, index1, b, i2 + index2, r);
					_read(a, aLen1, s1, i1, n1, offR, b, aLen2 + q, -q);
					_read(a, aLen1, s1, i1, n1, offR - q, b, 0, y + q);
					return;
				}
				_read(a, aLen1, s1, i1, n1, index1, b, i2 + index2, r);
				_read(a, aLen1, s1, i1, n1, offR, b, aLen2 + q, y);
				return;
			}
			// range confined to head
			int p = gapOff - aLen2;
			if (p >= 0) {
				int x = aLen2 - i2;
				if (index2 < x) {
					int y = x - index2;
					if (len > y) {
						_read(a, aLen1, s1, i1, n1, index1, b, i2 + index2, y);
						_read(a, aLen1, s1, i1, n1, index1 + y, b, 0, len - y);
						return;
					}
					_read(a, aLen1, s1, i1, n1, index1, b, i2 + index2, len);
					return;
				}
				_read(a, aLen1, s1, i1, n1, index1, b, index2 - x, len);
				return;
			}
			_read(a, aLen1, s1, i1, n1, index1, b, i2 + index2, len);
			return;
		}
		// target tail
		int x = i2 + index2 - s2;
		int q = gapOff - s2;
		if (q >= 0) {
			_read(a, aLen1, s1, i1, n1, index1, b, x, len);
			return;
		}
		if (x < 0) {
			if (len > -x) {
				_read(a, aLen1, s1, i1, n1, index1, b, aLen2 + x, -x);
				_read(a, aLen1, s1, i1, n1, index1 - x, b, 0, len + x);
				return;
			}
			_read(a, aLen1, s1, i1, n1, index1, b, aLen2 + x, len);
			return;
		}
		_read(a, aLen1, s1, i1, n1, index1, b, x, len);
	}

	protected static final <A, B, C> void _intersect(
			A a, int aLen1, int s1, int i1, int n1, int index1,
			B b, int aLen2, int s2, int i2, int n2, int index2,
			int len, Transfer<A, B, C> t, C attachment) throws ArrayIndexOutOfBoundsException {
		int gapOff = i2 + n2;
		if (index2 < n2) {
			// target head
			int r = n2 - index2;
			if (len > r) {
				// range bridges gap
				int p = gapOff - aLen2;
				int q = gapOff - s2;
				if (p >= 0) {
					int x = aLen2 - i2;
					if (index2 < x) {
						// index on right side
						int y = x - index2;
						_read(a, aLen1, s1, i1, n1, index1, b, i2 + index2, y, t, attachment);
						_read(a, aLen1, s1, i1, n1, index1 + y, b, 0, r - y, t, attachment);
						_read(a, aLen1, s1, i1, n1, index1 + r, b, q, len - r, t, attachment);
						return;
					}
					// index on left side
					_read(a, aLen1, s1, i1, n1, index1, b, index2 - x, r, t, attachment);
					_read(a, aLen1, s1, i1, n1, index1 + r, b, q, len - r, t, attachment);
					return;
				}
				// head is unfragmented
				int y = len - r;
				int offR = index1 + r;
				if (q >= 0) {
					_read(a, aLen1, s1, i1, n1, index1, b, i2 + index2, r, t, attachment);
					_read(a, aLen1, s1, i1, n1, offR, b, q, y, t, attachment);
					return;
				}
				if (y > -q) {
					_read(a, aLen1, s1, i1, n1, index1, b, i2 + index2, r, t, attachment);
					_read(a, aLen1, s1, i1, n1, offR, b, aLen2 + q, -q, t, attachment);
					_read(a, aLen1, s1, i1, n1, offR - q, b, 0, y + q, t, attachment);
					return;
				}
				_read(a, aLen1, s1, i1, n1, index1, b, i2 + index2, r, t, attachment);
				_read(a, aLen1, s1, i1, n1, offR, b, aLen2 + q, y, t, attachment);
				return;
			}
			// range confined to head
			int p = gapOff - aLen2;
			if (p >= 0) {
				int x = aLen2 - i2;
				if (index2 < x) {
					int y = x - index2;
					if (len > y) {
						_read(a, aLen1, s1, i1, n1, index1, b, i2 + index2, y, t, attachment);
						_read(a, aLen1, s1, i1, n1, index1 + y, b, 0, len - y, t, attachment);
						return;
					}
					_read(a, aLen1, s1, i1, n1, index1, b, i2 + index2, len, t, attachment);
					return;
				}
				_read(a, aLen1, s1, i1, n1, index1, b, index2 - x, len, t, attachment);
				return;
			}
			_read(a, aLen1, s1, i1, n1, index1, b, i2 + index2, len, t, attachment);
			return;
		}
		// target tail
		int x = i2 + index2 - s2;
		int q = gapOff - s2;
		if (q >= 0) {
			_read(a, aLen1, s1, i1, n1, index1, b, x, len, t, attachment);
			return;
		}
		if (x < 0) {
			if (len > -x) {
				_read(a, aLen1, s1, i1, n1, index1, b, aLen2 + x, -x, t, attachment);
				_read(a, aLen1, s1, i1, n1, index1 - x, b, 0, len + x, t, attachment);
				return;
			}
			_read(a, aLen1, s1, i1, n1, index1, b, aLen2 + x, len, t, attachment);
			return;
		}
		_read(a, aLen1, s1, i1, n1, index1, b, x, len, t, attachment);
	}

	protected static final <A, B, C, E extends RuntimeException> void _intersect(
			A a, int aLen1, int s1, int i1, int n1, int index1,
			B b, int aLen2, int s2, int i2, int n2, int index2,
			int len, Transfer<A, B, C> t, C attachment, Supplier<E> sup) throws E {
		int gapOff = i2 + n2;
		if (index2 < n2) {
			// target head
			int p = gapOff - aLen2;
			int r = n2 - index2;
			if (len > r) {
				// range bridges gap
				int q = gapOff - s2;
				int x = len - r;
				if (p >= 0) {
					int y = aLen2 - i2;
					if (index2 < y) {
						// index on right side
						int z = y - index2;
						int u = i2 + index2;
						int v = r - z;
						testBounds(aLen2, u, z, sup);
						testBounds(aLen2, 0, v, sup);
						testBounds(aLen2, q, x, sup);
						_read(a, aLen1, s1, i1, n1, index1, b, u, z, t, attachment, sup);
						_read(a, aLen1, s1, i1, n1, index1 + z, b, 0, v, t, attachment, sup);
						_read(a, aLen1, s1, i1, n1, index1 + r, b, q, x, t, attachment, sup);
						return;
					}
					// index on left side
					int z = index2 - y;
					testBounds(aLen2, z, r, sup);
					testBounds(aLen2, q, x, sup);
					_read(a, aLen1, s1, i1, n1, index1, b, z, r, t, attachment, sup);
					_read(a, aLen1, s1, i1, n1, index1 + r, b, q, x, t, attachment, sup);
					return;
				}
				// head is unfragmented
				int y = i2 + index2;
				int offR = index1 + r;
				testBounds(aLen2, y, r, sup);
				if (q >= 0) {
					testBounds(aLen2, q, x, sup);
					_read(a, aLen1, s1, i1, n1, index1, b, y, r, t, attachment, sup);
					_read(a, aLen1, s1, i1, n1, offR, b, q, x, t, attachment, sup);
					return;
				}
				int z = aLen2 + q;
				if (x > -q) {
					int u = x + q;
					testBounds(aLen2, z, -q, sup);
					testBounds(aLen2, 0, u, sup);
					_read(a, aLen1, s1, i1, n1, index1, b, y, r, t, attachment, sup);
					_read(a, aLen1, s1, i1, n1, offR, b, z, -q, t, attachment, sup);
					_read(a, aLen1, s1, i1, n1, offR - q, b, 0, u, t, attachment, sup);
					return;
				}
				testBounds(aLen2, z, x, sup);
				_read(a, aLen1, s1, i1, n1, index1, b, y, r, t, attachment, sup);
				_read(a, aLen1, s1, i1, n1, offR, b, z, x, t, attachment, sup);
				return;
			}
			// range confined to head
			if (p >= 0) {
				int x = aLen2 - i2;
				if (index2 < x) {
					int y = x - index2;
					int z = i2 + index2;
					if (len > y) {
						int u = len - y;
						testBounds(aLen2, z, y, sup);
						testBounds(aLen2, 0, u, sup);
						_read(a, aLen1, s1, i1, n1, index1, b, z, y, t, attachment, sup);
						_read(a, aLen1, s1, i1, n1, index1 + y, b, 0, u, t, attachment, sup);
						return;
					}
					testBounds(aLen2, z, len, sup);
					_read(a, aLen1, s1, i1, n1, index1, b, z, len, t, attachment, sup);
					return;
				}
				int y = index2 - x;
				testBounds(aLen2, y, len, sup);
				_read(a, aLen1, s1, i1, n1, index1, b, y, len, t, attachment, sup);
				return;
			}
			int x = i2 + index2;
			testBounds(aLen2, x, len, sup);
			_read(a, aLen1, s1, i1, n1, index1, b, x, len, t, attachment, sup);
			return;
		}
		// target tail
		int x = i2 + index2 - s2;
		int q = gapOff - s2;
		if (q >= 0) {
			testBounds(aLen2, x, len, sup);
			_read(a, aLen1, s1, i1, n1, index1, b, x, len, t, attachment, sup);
			return;
		}
		if (x < 0) {
			int y = aLen2 + x;
			if (len > -x) {
				int z = len + x;
				testBounds(aLen2, y, -x, sup);
				testBounds(aLen2, 0, z, sup);
				_read(a, aLen1, s1, i1, n1, index1, b, y, -x, t, attachment, sup);
				_read(a, aLen1, s1, i1, n1, index1 - x, b, 0, z, t, attachment, sup);
				return;
			}
			testBounds(aLen2, y, len, sup);
			_read(a, aLen1, s1, i1, n1, index1, b, y, len, t, attachment, sup);
			return;
		}
		testBounds(aLen2, x, len, sup);
		_read(a, aLen1, s1, i1, n1, index1, b, x, len, t, attachment, sup);
	}

	protected static final <A, B, C> int _intersect(
			A a, int aLen1, int s1, int i1, int n1, int index1,
			B b, int aLen2, int s2, int i2, int n2, int index2,
			int len, LimitedTransfer<A, B, C> t, C attachment) throws ArrayIndexOutOfBoundsException {
		int gapOff = i2 + n2;
		if (index2 < n2) {
			// target head
			int p = gapOff - aLen2;
			int r = n2 - index2;
			if (len > r) {
				// range bridges gap
				int q = gapOff - s2;
				int x = len - r;
				if (p >= 0) {
					int y = aLen2 - i2;
					if (index2 < y) {
						// index on right side
						int z = y - index2;
						int v = r - z;
						int k;
						if ((k = _read(a, aLen1, s1, i1, n1, index1, b, i2 + index2, z, t, attachment)) != z) {
							return k;
						}
						if ((k = _read(a, aLen1, s1, i1, n1, index1 + z, b, 0, v, t, attachment)) != v) {
							return k + z;
						}
						return _read(a, aLen1, s1, i1, n1, index1 + r, b, q, x, t, attachment) + z + v;
					}
					// index on left side
					int k;
					if ((k = _read(a, aLen1, s1, i1, n1, index1, b, index2 - y, r, t, attachment)) != r) {
						return k;
					}
					return _read(a, aLen1, s1, i1, n1, index1 + r, b, q, x, t, attachment) + r;
				}
				// head is unfragmented
				int offR = index1 + r;
				if (q >= 0) {
					int k;
					if ((k = _read(a, aLen1, s1, i1, n1, index1, b, i2 + index2, r, t, attachment)) != r) {
						return k;
					}
					return _read(a, aLen1, s1, i1, n1, offR, b, q, x, t, attachment) + r;
				}
				if (x > -q) {
					int u = x + q;
					int k;
					if ((k = _read(a, aLen1, s1, i1, n1, index1, b, i2 + index2, r, t, attachment)) != r) {
						return k;
					}
					if ((k = _read(a, aLen1, s1, i1, n1, offR, b, aLen2 + q, -q, t, attachment)) != -q) {
						return k + r;
					}
					return _read(a, aLen1, s1, i1, n1, offR - q, b, 0, u, t, attachment) + r - q;
				}
				int k;
				if ((k = _read(a, aLen1, s1, i1, n1, index1, b, i2 + index2, r, t, attachment)) != r) {
					return k;
				}
				return _read(a, aLen1, s1, i1, n1, offR, b, aLen2 + q, x, t, attachment) + r;
			}
			// range confined to head
			if (p >= 0) {
				int x = aLen2 - i2;
				if (index2 < x) {
					int y = x - index2;
					if (len > y) {
						int k;
						if ((k = _read(a, aLen1, s1, i1, n1, index1, b, i2 + index2, y, t, attachment)) != y) {
							return k;
						}
						return _read(a, aLen1, s1, i1, n1, index1 + y, b, 0, len - y, t, attachment) + y;
					}
					return _read(a, aLen1, s1, i1, n1, index1, b, i2 + index2, len, t, attachment);
				}
				return _read(a, aLen1, s1, i1, n1, index1, b, index2 - x, len, t, attachment);
			}
			return _read(a, aLen1, s1, i1, n1, index1, b, i2 + index2, len, t, attachment);
		}
		// target tail
		int x = i2 + index2 - s2;
		int q = gapOff - s2;
		if (q >= 0) {
			return _read(a, aLen1, s1, i1, n1, index1, b, x, len, t, attachment);
		}
		if (x < 0) {
			if (len > -x) {
				int k;
				if ((k = _read(a, aLen1, s1, i1, n1, index1, b, aLen2 + x, -x, t, attachment)) != -x) {
					return k;
				}
				return _read(a, aLen1, s1, i1, n1, index1 - x, b, 0, len + x, t, attachment) - x;
			}
			return _read(a, aLen1, s1, i1, n1, index1, b, aLen2 + x, len, t, attachment);
		}
		return _read(a, aLen1, s1, i1, n1, index1, b, x, len, t, attachment);
	}

	protected static final <A, B, C, E extends RuntimeException> int _intersect(
			A a, int aLen1, int s1, int i1, int n1, int index1,
			B b, int aLen2, int s2, int i2, int n2, int index2,
			int len, LimitedTransfer<A, B, C> t, C attachment, Supplier<E> sup) throws E {
		int gapOff = i2 + n2;
		if (index2 < n2) {
			// target head
			int p = gapOff - aLen2;
			int r = n2 - index2;
			if (len > r) {
				// range bridges gap
				int q = gapOff - s2;
				int x = len - r;
				if (p >= 0) {
					int y = aLen2 - i2;
					if (index2 < y) {
						// index on right side
						int z = y - index2;
						int u = i2 + index2;
						int v = r - z;
						testBounds(aLen2, u, z, sup);
						testBounds(aLen2, 0, v, sup);
						testBounds(aLen2, q, x, sup);
						int k;
						if ((k = _read(a, aLen1, s1, i1, n1, index1, b, u, z, t, attachment, sup)) != z) {
							return k;
						}
						if ((k = _read(a, aLen1, s1, i1, n1, index1 + z, b, 0, v, t, attachment, sup)) != v) {
							return k + z;
						}
						return _read(a, aLen1, s1, i1, n1, index1 + r, b, q, x, t, attachment, sup) + z + v;
					}
					// index on left side
					int z = index2 - y;
					testBounds(aLen2, z, r, sup);
					testBounds(aLen2, q, x, sup);
					int k;
					if ((k = _read(a, aLen1, s1, i1, n1, index1, b, z, r, t, attachment, sup)) != r) {
						return k;
					}
					return _read(a, aLen1, s1, i1, n1, index1 + r, b, q, x, t, attachment, sup) + r;
				}
				// head is unfragmented
				int y = i2 + index2;
				int offR = index1 + r;
				testBounds(aLen2, y, r, sup);
				if (q >= 0) {
					testBounds(aLen2, q, x, sup);
					int k;
					if ((k = _read(a, aLen1, s1, i1, n1, index1, b, y, r, t, attachment, sup)) != r) {
						return k;
					}
					return _read(a, aLen1, s1, i1, n1, offR, b, q, x, t, attachment, sup) + r;
				}
				int z = aLen2 + q;
				if (x > -q) {
					int u = x + q;
					testBounds(aLen2, z, -q, sup);
					testBounds(aLen2, 0, u, sup);
					int k;
					if ((k = _read(a, aLen1, s1, i1, n1, index1, b, y, r, t, attachment, sup)) != r) {
						return k;
					}
					if ((k = _read(a, aLen1, s1, i1, n1, offR, b, z, -q, t, attachment, sup)) != -q) {
						return k + r;
					}
					return _read(a, aLen1, s1, i1, n1, offR - q, b, 0, u, t, attachment, sup) + r - q;
				}
				testBounds(aLen2, z, x, sup);
				int k;
				if ((k = _read(a, aLen1, s1, i1, n1, index1, b, y, r, t, attachment, sup)) != r) {
					return k;
				}
				return _read(a, aLen1, s1, i1, n1, offR, b, z, x, t, attachment, sup) + r;
			}
			// range confined to head
			if (p >= 0) {
				int x = aLen2 - i2;
				if (index2 < x) {
					int y = x - index2;
					int z = i2 + index2;
					if (len > y) {
						int u = len - y;
						testBounds(aLen2, z, y, sup);
						testBounds(aLen2, 0, u, sup);
						int k;
						if ((k = _read(a, aLen1, s1, i1, n1, index1, b, z, y, t, attachment, sup)) != y) {
							return k;
						}
						return _read(a, aLen1, s1, i1, n1, index1 + y, b, 0, u, t, attachment, sup) + y;
					}
					testBounds(aLen2, z, len, sup);
					return _read(a, aLen1, s1, i1, n1, index1, b, z, len, t, attachment, sup);
				}
				int y = index2 - x;
				testBounds(aLen2, y, len, sup);
				return _read(a, aLen1, s1, i1, n1, index1, b, y, len, t, attachment, sup);
			}
			int x = i2 + index2;
			testBounds(aLen2, x, len, sup);
			return _read(a, aLen1, s1, i1, n1, index1, b, x, len, t, attachment, sup);
		}
		// target tail
		int x = i2 + index2 - s2;
		int q = gapOff - s2;
		if (q >= 0) {
			testBounds(aLen2, x, len, sup);
			return _read(a, aLen1, s1, i1, n1, index1, b, x, len, t, attachment, sup);
		}
		if (x < 0) {
			int y = aLen2 + x;
			if (len > -x) {
				int z = len + x;
				testBounds(aLen2, y, -x, sup);
				testBounds(aLen2, 0, z, sup);
				int k;
				if ((k = _read(a, aLen1, s1, i1, n1, index1, b, y, -x, t, attachment, sup)) != -x) {
					return k;
				}
				return _read(a, aLen1, s1, i1, n1, index1 - x, b, 0, z, t, attachment, sup) - x;
			}
			testBounds(aLen2, y, len, sup);
			return _read(a, aLen1, s1, i1, n1, index1, b, y, len, t, attachment, sup);
		}
		testBounds(aLen2, x, len, sup);
		return _read(a, aLen1, s1, i1, n1, index1, b, x, len, t, attachment, sup);
	}

	protected static final <A, B, C, X extends Throwable> void _intersectChecked(
			A a, int aLen1, int s1, int i1, int n1, int index1,
			B b, int aLen2, int s2, int i2, int n2, int index2,
			int len, CheckedTransfer<A, B, C, X> t, C attachment) throws ArrayIndexOutOfBoundsException, X {
		int gapOff = i2 + n2;
		if (index2 < n2) {
			// target head
			int r = n2 - index2;
			if (len > r) {
				// range bridges gap
				int p = gapOff - aLen2;
				int q = gapOff - s2;
				if (p >= 0) {
					int x = aLen2 - i2;
					if (index2 < x) {
						// index on right side
						int y = x - index2;
						_readChecked(a, aLen1, s1, i1, n1, index1, b, i2 + index2, y, t, attachment);
						_readChecked(a, aLen1, s1, i1, n1, index1 + y, b, 0, r - y, t, attachment);
						_readChecked(a, aLen1, s1, i1, n1, index1 + r, b, q, len - r, t, attachment);
						return;
					}
					// index on left side
					_readChecked(a, aLen1, s1, i1, n1, index1, b, index2 - x, r, t, attachment);
					_readChecked(a, aLen1, s1, i1, n1, index1 + r, b, q, len - r, t, attachment);
					return;
				}
				// head is unfragmented
				int y = len - r;
				int offR = index1 + r;
				if (q >= 0) {
					_readChecked(a, aLen1, s1, i1, n1, index1, b, i2 + index2, r, t, attachment);
					_readChecked(a, aLen1, s1, i1, n1, offR, b, q, y, t, attachment);
					return;
				}
				if (y > -q) {
					_readChecked(a, aLen1, s1, i1, n1, index1, b, i2 + index2, r, t, attachment);
					_readChecked(a, aLen1, s1, i1, n1, offR, b, aLen2 + q, -q, t, attachment);
					_readChecked(a, aLen1, s1, i1, n1, offR - q, b, 0, y + q, t, attachment);
					return;
				}
				_readChecked(a, aLen1, s1, i1, n1, index1, b, i2 + index2, r, t, attachment);
				_readChecked(a, aLen1, s1, i1, n1, offR, b, aLen2 + q, y, t, attachment);
				return;
			}
			// range confined to head
			int p = gapOff - aLen2;
			if (p >= 0) {
				int x = aLen2 - i2;
				if (index2 < x) {
					int y = x - index2;
					if (len > y) {
						_readChecked(a, aLen1, s1, i1, n1, index1, b, i2 + index2, y, t, attachment);
						_readChecked(a, aLen1, s1, i1, n1, index1 + y, b, 0, len - y, t, attachment);
						return;
					}
					_readChecked(a, aLen1, s1, i1, n1, index1, b, i2 + index2, len, t, attachment);
					return;
				}
				_readChecked(a, aLen1, s1, i1, n1, index1, b, index2 - x, len, t, attachment);
				return;
			}
			_readChecked(a, aLen1, s1, i1, n1, index1, b, i2 + index2, len, t, attachment);
			return;
		}
		// target tail
		int x = i2 + index2 - s2;
		int q = gapOff - s2;
		if (q >= 0) {
			_readChecked(a, aLen1, s1, i1, n1, index1, b, x, len, t, attachment);
			return;
		}
		if (x < 0) {
			if (len > -x) {
				_readChecked(a, aLen1, s1, i1, n1, index1, b, aLen2 + x, -x, t, attachment);
				_readChecked(a, aLen1, s1, i1, n1, index1 - x, b, 0, len + x, t, attachment);
				return;
			}
			_readChecked(a, aLen1, s1, i1, n1, index1, b, aLen2 + x, len, t, attachment);
			return;
		}
		_readChecked(a, aLen1, s1, i1, n1, index1, b, x, len, t, attachment);
	}

	protected static final <A, B, C, E extends RuntimeException, X extends Throwable> void _intersectChecked(
			A a, int aLen1, int s1, int i1, int n1, int index1,
			B b, int aLen2, int s2, int i2, int n2, int index2,
			int len, CheckedTransfer<A, B, C, X> t, C attachment, Supplier<E> sup) throws E, X {
		int gapOff = i2 + n2;
		if (index2 < n2) {
			// target head
			int p = gapOff - aLen2;
			int r = n2 - index2;
			if (len > r) {
				// range bridges gap
				int q = gapOff - s2;
				int x = len - r;
				if (p >= 0) {
					int y = aLen2 - i2;
					if (index2 < y) {
						// index on right side
						int z = y - index2;
						int u = i2 + index2;
						int v = r - z;
						testBounds(aLen2, u, z, sup);
						testBounds(aLen2, 0, v, sup);
						testBounds(aLen2, q, x, sup);
						_readChecked(a, aLen1, s1, i1, n1, index1, b, u, z, t, attachment, sup);
						_readChecked(a, aLen1, s1, i1, n1, index1 + z, b, 0, v, t, attachment, sup);
						_readChecked(a, aLen1, s1, i1, n1, index1 + r, b, q, x, t, attachment, sup);
						return;
					}
					// index on left side
					int z = index2 - y;
					testBounds(aLen2, z, r, sup);
					testBounds(aLen2, q, x, sup);
					_readChecked(a, aLen1, s1, i1, n1, index1, b, z, r, t, attachment, sup);
					_readChecked(a, aLen1, s1, i1, n1, index1 + r, b, q, x, t, attachment, sup);
					return;
				}
				// head is unfragmented
				int y = i2 + index2;
				int offR = index1 + r;
				testBounds(aLen2, y, r, sup);
				if (q >= 0) {
					testBounds(aLen2, q, x, sup);
					_readChecked(a, aLen1, s1, i1, n1, index1, b, y, r, t, attachment, sup);
					_readChecked(a, aLen1, s1, i1, n1, offR, b, q, x, t, attachment, sup);
					return;
				}
				int z = aLen2 + q;
				if (x > -q) {
					int u = x + q;
					testBounds(aLen2, z, -q, sup);
					testBounds(aLen2, 0, u, sup);
					_readChecked(a, aLen1, s1, i1, n1, index1, b, y, r, t, attachment, sup);
					_readChecked(a, aLen1, s1, i1, n1, offR, b, z, -q, t, attachment, sup);
					_readChecked(a, aLen1, s1, i1, n1, offR - q, b, 0, u, t, attachment, sup);
					return;
				}
				testBounds(aLen2, z, x, sup);
				_readChecked(a, aLen1, s1, i1, n1, index1, b, y, r, t, attachment, sup);
				_readChecked(a, aLen1, s1, i1, n1, offR, b, z, x, t, attachment, sup);
				return;
			}
			// range confined to head
			if (p >= 0) {
				int x = aLen2 - i2;
				if (index2 < x) {
					int y = x - index2;
					int z = i2 + index2;
					if (len > y) {
						int u = len - y;
						testBounds(aLen2, z, y, sup);
						testBounds(aLen2, 0, u, sup);
						_readChecked(a, aLen1, s1, i1, n1, index1, b, z, y, t, attachment, sup);
						_readChecked(a, aLen1, s1, i1, n1, index1 + y, b, 0, u, t, attachment, sup);
						return;
					}
					testBounds(aLen2, z, len, sup);
					_readChecked(a, aLen1, s1, i1, n1, index1, b, z, len, t, attachment, sup);
					return;
				}
				int y = index2 - x;
				testBounds(aLen2, y, len, sup);
				_readChecked(a, aLen1, s1, i1, n1, index1, b, y, len, t, attachment, sup);
				return;
			}
			int x = i2 + index2;
			testBounds(aLen2, x, len, sup);
			_readChecked(a, aLen1, s1, i1, n1, index1, b, x, len, t, attachment, sup);
			return;
		}
		// target tail
		int x = i2 + index2 - s2;
		int q = gapOff - s2;
		if (q >= 0) {
			testBounds(aLen2, x, len, sup);
			_readChecked(a, aLen1, s1, i1, n1, index1, b, x, len, t, attachment, sup);
			return;
		}
		if (x < 0) {
			int y = aLen2 + x;
			if (len > -x) {
				int z = len + x;
				testBounds(aLen2, y, -x, sup);
				testBounds(aLen2, 0, z, sup);
				_readChecked(a, aLen1, s1, i1, n1, index1, b, y, -x, t, attachment, sup);
				_readChecked(a, aLen1, s1, i1, n1, index1 - x, b, 0, z, t, attachment, sup);
				return;
			}
			testBounds(aLen2, y, len, sup);
			_readChecked(a, aLen1, s1, i1, n1, index1, b, y, len, t, attachment, sup);
			return;
		}
		testBounds(aLen2, x, len, sup);
		_readChecked(a, aLen1, s1, i1, n1, index1, b, x, len, t, attachment, sup);
	}

	protected static final <A, B, C, X extends Throwable> int _intersectChecked(
			A a, int aLen1, int s1, int i1, int n1, int index1,
			B b, int aLen2, int s2, int i2, int n2, int index2,
			int len, CheckedLimitedTransfer<A, B, C, X> t, C attachment) throws ArrayIndexOutOfBoundsException, X {
		int gapOff = i2 + n2;
		if (index2 < n2) {
			// target head
			int p = gapOff - aLen2;
			int r = n2 - index2;
			if (len > r) {
				// range bridges gap
				int q = gapOff - s2;
				int x = len - r;
				if (p >= 0) {
					int y = aLen2 - i2;
					if (index2 < y) {
						// index on right side
						int z = y - index2;
						int v = r - z;
						int k;
						if ((k = _readChecked(a, aLen1, s1, i1, n1, index1, b, i2 + index2, z, t, attachment)) != z) {
							return k;
						}
						if ((k = _readChecked(a, aLen1, s1, i1, n1, index1 + z, b, 0, v, t, attachment)) != v) {
							return k + z;
						}
						return _readChecked(a, aLen1, s1, i1, n1, index1 + r, b, q, x, t, attachment) + z + v;
					}
					// index on left side
					int k;
					if ((k = _readChecked(a, aLen1, s1, i1, n1, index1, b, index2 - y, r, t, attachment)) != r) {
						return k;
					}
					return _readChecked(a, aLen1, s1, i1, n1, index1 + r, b, q, x, t, attachment) + r;
				}
				// head is unfragmented
				int offR = index1 + r;
				if (q >= 0) {
					int k;
					if ((k = _readChecked(a, aLen1, s1, i1, n1, index1, b, i2 + index2, r, t, attachment)) != r) {
						return k;
					}
					return _readChecked(a, aLen1, s1, i1, n1, offR, b, q, x, t, attachment) + r;
				}
				if (x > -q) {
					int u = x + q;
					int k;
					if ((k = _readChecked(a, aLen1, s1, i1, n1, index1, b, i2 + index2, r, t, attachment)) != r) {
						return k;
					}
					if ((k = _readChecked(a, aLen1, s1, i1, n1, offR, b, aLen2 + q, -q, t, attachment)) != -q) {
						return k + r;
					}
					return _readChecked(a, aLen1, s1, i1, n1, offR - q, b, 0, u, t, attachment) + r - q;
				}
				int k;
				if ((k = _readChecked(a, aLen1, s1, i1, n1, index1, b, i2 + index2, r, t, attachment)) != r) {
					return k;
				}
				return _readChecked(a, aLen1, s1, i1, n1, offR, b, aLen2 + q, x, t, attachment) + r;
			}
			// range confined to head
			if (p >= 0) {
				int x = aLen2 - i2;
				if (index2 < x) {
					int y = x - index2;
					if (len > y) {
						int k;
						if ((k = _readChecked(a, aLen1, s1, i1, n1, index1, b, i2 + index2, y, t, attachment)) != y) {
							return k;
						}
						return _readChecked(a, aLen1, s1, i1, n1, index1 + y, b, 0, len - y, t, attachment) + y;
					}
					return _readChecked(a, aLen1, s1, i1, n1, index1, b, i2 + index2, len, t, attachment);
				}
				return _readChecked(a, aLen1, s1, i1, n1, index1, b, index2 - x, len, t, attachment);
			}
			return _readChecked(a, aLen1, s1, i1, n1, index1, b, i2 + index2, len, t, attachment);
		}
		// target tail
		int x = i2 + index2 - s2;
		int q = gapOff - s2;
		if (q >= 0) {
			return _readChecked(a, aLen1, s1, i1, n1, index1, b, x, len, t, attachment);
		}
		if (x < 0) {
			if (len > -x) {
				int k;
				if ((k = _readChecked(a, aLen1, s1, i1, n1, index1, b, aLen2 + x, -x, t, attachment)) != -x) {
					return k;
				}
				return _readChecked(a, aLen1, s1, i1, n1, index1 - x, b, 0, len + x, t, attachment) - x;
			}
			return _readChecked(a, aLen1, s1, i1, n1, index1, b, aLen2 + x, len, t, attachment);
		}
		return _readChecked(a, aLen1, s1, i1, n1, index1, b, x, len, t, attachment);
	}

	protected static final <A, B, C, E extends RuntimeException, X extends Throwable> int _intersectChecked(
			A a, int aLen1, int s1, int i1, int n1, int index1,
			B b, int aLen2, int s2, int i2, int n2, int index2,
			int len, CheckedLimitedTransfer<A, B, C, X> t, C attachment, Supplier<E> sup) throws E, X {
		int gapOff = i2 + n2;
		if (index2 < n2) {
			// target head
			int p = gapOff - aLen2;
			int r = n2 - index2;
			if (len > r) {
				// range bridges gap
				int q = gapOff - s2;
				int x = len - r;
				if (p >= 0) {
					int y = aLen2 - i2;
					if (index2 < y) {
						// index on right side
						int z = y - index2;
						int u = i2 + index2;
						int v = r - z;
						testBounds(aLen2, u, z, sup);
						testBounds(aLen2, 0, v, sup);
						testBounds(aLen2, q, x, sup);
						int k;
						if ((k = _readChecked(a, aLen1, s1, i1, n1, index1, b, u, z, t, attachment, sup)) != z) {
							return k;
						}
						if ((k = _readChecked(a, aLen1, s1, i1, n1, index1 + z, b, 0, v, t, attachment, sup)) != v) {
							return k + z;
						}
						return _readChecked(a, aLen1, s1, i1, n1, index1 + r, b, q, x, t, attachment, sup) + z + v;
					}
					// index on left side
					int z = index2 - y;
					testBounds(aLen2, z, r, sup);
					testBounds(aLen2, q, x, sup);
					int k;
					if ((k = _readChecked(a, aLen1, s1, i1, n1, index1, b, z, r, t, attachment, sup)) != r) {
						return k;
					}
					return _readChecked(a, aLen1, s1, i1, n1, index1 + r, b, q, x, t, attachment, sup) + r;
				}
				// head is unfragmented
				int y = i2 + index2;
				int offR = index1 + r;
				testBounds(aLen2, y, r, sup);
				if (q >= 0) {
					testBounds(aLen2, q, x, sup);
					int k;
					if ((k = _readChecked(a, aLen1, s1, i1, n1, index1, b, y, r, t, attachment, sup)) != r) {
						return k;
					}
					return _readChecked(a, aLen1, s1, i1, n1, offR, b, q, x, t, attachment, sup) + r;
				}
				int z = aLen2 + q;
				if (x > -q) {
					int u = x + q;
					testBounds(aLen2, z, -q, sup);
					testBounds(aLen2, 0, u, sup);
					int k;
					if ((k = _readChecked(a, aLen1, s1, i1, n1, index1, b, y, r, t, attachment, sup)) != r) {
						return k;
					}
					if ((k = _readChecked(a, aLen1, s1, i1, n1, offR, b, z, -q, t, attachment, sup)) != -q) {
						return k + r;
					}
					return _readChecked(a, aLen1, s1, i1, n1, offR - q, b, 0, u, t, attachment, sup) + r - q;
				}
				testBounds(aLen2, z, x, sup);
				int k;
				if ((k = _readChecked(a, aLen1, s1, i1, n1, index1, b, y, r, t, attachment, sup)) != r) {
					return k;
				}
				return _readChecked(a, aLen1, s1, i1, n1, offR, b, z, x, t, attachment, sup) + r;
			}
			// range confined to head
			if (p >= 0) {
				int x = aLen2 - i2;
				if (index2 < x) {
					int y = x - index2;
					int z = i2 + index2;
					if (len > y) {
						int u = len - y;
						testBounds(aLen2, z, y, sup);
						testBounds(aLen2, 0, u, sup);
						int k;
						if ((k = _readChecked(a, aLen1, s1, i1, n1, index1, b, z, y, t, attachment, sup)) != y) {
							return k;
						}
						return _readChecked(a, aLen1, s1, i1, n1, index1 + y, b, 0, u, t, attachment, sup) + y;
					}
					testBounds(aLen2, z, len, sup);
					return _readChecked(a, aLen1, s1, i1, n1, index1, b, z, len, t, attachment, sup);
				}
				int y = index2 - x;
				testBounds(aLen2, y, len, sup);
				return _readChecked(a, aLen1, s1, i1, n1, index1, b, y, len, t, attachment, sup);
			}
			int x = i2 + index2;
			testBounds(aLen2, x, len, sup);
			return _readChecked(a, aLen1, s1, i1, n1, index1, b, x, len, t, attachment, sup);
		}
		// target tail
		int x = i2 + index2 - s2;
		int q = gapOff - s2;
		if (q >= 0) {
			testBounds(aLen2, x, len, sup);
			return _readChecked(a, aLen1, s1, i1, n1, index1, b, x, len, t, attachment, sup);
		}
		if (x < 0) {
			int y = aLen2 + x;
			if (len > -x) {
				int z = len + x;
				testBounds(aLen2, y, -x, sup);
				testBounds(aLen2, 0, z, sup);
				int k;
				if ((k = _readChecked(a, aLen1, s1, i1, n1, index1, b, y, -x, t, attachment, sup)) != -x) {
					return k;
				}
				return _readChecked(a, aLen1, s1, i1, n1, index1 - x, b, 0, z, t, attachment, sup) - x;
			}
			testBounds(aLen2, y, len, sup);
			return _readChecked(a, aLen1, s1, i1, n1, index1, b, y, len, t, attachment, sup);
		}
		testBounds(aLen2, x, len, sup);
		return _readChecked(a, aLen1, s1, i1, n1, index1, b, x, len, t, attachment, sup);
	}

	@SuppressWarnings("SuspiciousSystemArraycopy")
	protected final int _insertIndex(A a, int aLen, int s, int i, int n,
			int index) throws ArrayIndexOutOfBoundsException {
		this._size++;			// update size of array
		this._len = index + 1;	// update length of head
		int gapOff = i + n;
		int p = gapOff - aLen;
		if (index < n) {
			// target head
			int r = n - index;
			int l = s - r;
			if (r > l) {
				// rotate head left
				int q = gapOff - s;
				if (p >= 0) {
					int g = aLen - s;
					this._idx = i - g;
					int x = i + index;
					int y = x - aLen;
					if (y < 0) {
						int z = x - g;
						System.arraycopy(a, q, a, p, l);
						return z;
					}
					int z = s - p;
					System.arraycopy(a, q, a, p, z);
					if (y < g) {
						int u = x - g;
						System.arraycopy(a, 0, a, s, y);
						return u;
					}
					int u = y - g;
					System.arraycopy(a, 0, a, s, g);
					System.arraycopy(a, g, a, 0, u);
					return u;
				}
				if (q >= 0) {
					int x = i + index + s;
					int y = x - aLen;
					if (y < 0) {
						this._idx = i + s;
						System.arraycopy(a, q, a, gapOff, l);
						return x;
					}
					int g = aLen - s;
					this._idx = g > i ? i + s : i - g;
					System.arraycopy(a, q, a, gapOff, -p);
					System.arraycopy(a, g, a, 0, y);
					return y;
				}
				int g = aLen - s;
				int tailOff = gapOff + g;
				System.arraycopy(a, tailOff, a, gapOff, -q);
				int x = i + index;
				int y = x - g;
				if (y < 0) {
					this._idx = i + s;
					int z = x + s;
					System.arraycopy(a, 0, a, s, x);
					return z;
				}
				this._idx = g > i ? i + s : i - g;
				System.arraycopy(a, 0, a, s, g);
				System.arraycopy(a, g, a, 0, y);
				return y;
			}
			// rotate head right
			int x = i + index;
			if (p >= 0) {
				int g = aLen - s;
				int y = x - s;
				int z = x - aLen;
				if (z < 0) {
					System.arraycopy(a, 0, a, g, p);
					if (y < 0) {
						System.arraycopy(a, s, a, 0, g);
						System.arraycopy(a, x, a, x + g, -y);
						return x;
					}
					System.arraycopy(a, x, a, y, -z);
					return x;
				}
				System.arraycopy(a, z, a, y, r);
				return z;
			}
			int q = gapOff - s;
			if (q >= 0) {
				int y = x - s;
				if (y < 0) {
					int g = aLen - s;
					System.arraycopy(a, s, a, 0, q);
					System.arraycopy(a, x, a, x + g, -y);
					return x;
				}
				System.arraycopy(a, x, a, y, r);
				return x;
			}
			int g = aLen - s;
			System.arraycopy(a, x, a, x + g, r);
			return x;
		}
		// target tail
		int l = index - n;
		int r = s - l;
		if (r < l) {
			// rotate tail right
			// TODO reuse i - s
			int x = i + index - s;
			if (p >= 0) {
				int g = aLen - s;
				int y = x - s;
				System.arraycopy(a, 0, a, g, p);
				if (y < 0) {
					this._idx = s > i ? i + g : i - s;
					System.arraycopy(a, s, a, 0, g);
					System.arraycopy(a, x, a, x + g, -y);
					return x;
				}
				this._idx = i - s;
				int z = aLen - x;
				System.arraycopy(a, x, a, y, z);
				return x;
			}
			int q = gapOff - s;
			if (q >= 0) {
				if (s < x) {
					this._idx = i - s;
					System.arraycopy(a, x, a, x - s, r);
					return x;
				}
				int g = aLen - s;
				this._idx = s > i ? i + g : i - s;
				System.arraycopy(a, s, a, 0, q);
				System.arraycopy(a, x, a, x + g, s - x);
				return x;
			}
			int g = aLen - s;
			this._idx = i + g;
			if (x < 0) {
				System.arraycopy(a, 0, a, g, gapOff);
				int y = x + aLen;	// i + index + g
				int z = s - y;
				if (z < 0) {
					System.arraycopy(a, y, a, -z, -x);
					return y;
				}
				System.arraycopy(a, s, a, 0, g);
				System.arraycopy(a, y, a, y + g, z);
				return y;
			}
			System.arraycopy(a, x, a, x + g, r);
			return x;
		}
		// rotate tail left
		int q = gapOff - s;
		if (p >= 0) {
			int x = p + l;	// == i + index - aLen
			System.arraycopy(a, q, a, p, l);
			return x;
		}
		if (q >= 0) {
			int x = p + l;	// == i + index - aLen
			if (x < 0) {
				System.arraycopy(a, q, a, gapOff, l);
				return i + index;
			}
			int g = aLen - s;
			System.arraycopy(a, q, a, gapOff, -p);
			System.arraycopy(a, g, a, 0, x);
			return x;
		}
		int g = aLen - s;
		int tailOff = gapOff + g;
		int x = q + l;	// == i + index - s
		if (x < 0) {
			int y = i + index;
			System.arraycopy(a, tailOff, a, gapOff, l);
			return y;
		}
		int y = p + l;	// i + index - aLen
		System.arraycopy(a, tailOff, a, gapOff, -q);
		if (y < 0) {
			int z = i + index;
			System.arraycopy(a, 0, a, s, x);
			return z;
		}
		System.arraycopy(a, 0, a, s, g);
		System.arraycopy(a, g, a, 0, y);
		return y;
	}

	@SuppressWarnings("SuspiciousSystemArraycopy")
	protected final int _insertIndex(A a, int aLen, int s, int i, int n,
			int index, A clear, int clearLen) throws ArrayIndexOutOfBoundsException {
		this._size++;			// update size of array
		this._len = index + 1;	// update length of head
		int gapOff = i + n;
		int p = gapOff - aLen;
		if (index < n) {
			// target head
			int r = n - index;
			int l = s - r;
			if (r > l) {
				// rotate head left
				int q = gapOff - s;
				if (p >= 0) {
					int g = aLen - s;
					this._idx = i - g;
					int x = i + index;
					int y = x - aLen;
					if (y < 0) {
						int z = x - g;
						System.arraycopy(a, q, a, p, l);
						if (g > l) {
							clear(a, q, l, clear, clearLen);
						} else {
							clear(a, z + 1, g - 1, clear, clearLen);
						}
						return z;
					}
					int z = s - p;
					System.arraycopy(a, q, a, p, z);
					if (y < g) {
						int u = x - g;
						System.arraycopy(a, 0, a, s, y);
						clear(a, 0, y, clear, clearLen);
						if (g > l) {
							clear(a, q, z, clear, clearLen);
						} else {
							int v = u + 1;
							clear(a, v, aLen - v, clear, clearLen);
						}
						return u;
					}
					int u = y - g;
					System.arraycopy(a, 0, a, s, g);
					System.arraycopy(a, g, a, 0, u);
					clear(a, u + 1, g - 1, clear, clearLen);
					return u;
				}
				if (q >= 0) {
					// TODO can reuse i + s
					int x = i + index + s;
					int y = x - aLen;
					if (y < 0) {
						this._idx = i + s;
						System.arraycopy(a, q, a, gapOff, l);
						clear(a, q, l, clear, clearLen);
						return x;
					}
					int g = aLen - s;
					this._idx = g > i ? i + s : i - g;
					System.arraycopy(a, q, a, gapOff, -p);
					System.arraycopy(a, g, a, 0, y);
					if (g > l) {
						clear(a, q, l, clear, clearLen);
					} else {
						clear(a, y + 1, g - 1, clear, clearLen);
					}
					return y;
				}
				int g = aLen - s;
				int tailOff = gapOff + g;
				System.arraycopy(a, tailOff, a, gapOff, -q);
				int x = i + index;
				int y = x - g;
				if (y < 0) {
					this._idx = i + s;
					int z = x + s;
					System.arraycopy(a, 0, a, s, x);
					clear(a, 0, x, clear, clearLen);
					if (g > l) {
						clear(a, tailOff, -q, clear, clearLen);
					} else {
						clear(a, z + 1, -y - 1, clear, clearLen);
					}
					return z;
				}
				this._idx = g > i ? i + s : i - g;
				System.arraycopy(a, 0, a, s, g);
				System.arraycopy(a, g, a, 0, y);
				clear(a, y + 1, g - 1, clear, clearLen);
				return y;
			}
			// rotate head right
			int x = i + index;
			if (p >= 0) {
				int g = aLen - s;
				int y = x - s;
				int z = x - aLen;
				if (z < 0) {
					System.arraycopy(a, 0, a, g, p);
					if (y < 0) {
						System.arraycopy(a, s, a, 0, g);
						System.arraycopy(a, x, a, x + g, -y);
						clear(a, x + 1, g - 1, clear, clearLen);
						return x;
					}
					System.arraycopy(a, x, a, y, -z);
					clear(a, x + 1, -z - 1, clear, clearLen);
					clear(a, 0, g < r ? y : p, clear, clearLen);
					return x;
				}
				System.arraycopy(a, z, a, y, r);
				clear(a, z + 1, (g < r ? g : r) - 1, clear, clearLen);
				return z;
			}
			int q = gapOff - s;
			if (q >= 0) {
				int y = x - s;
				if (y < 0) {
					int g = aLen - s;
					System.arraycopy(a, s, a, 0, q);
					System.arraycopy(a, x, a, x + g, -y);
					clear(a, x + 1, (g < r ? g : r) - 1, clear, clearLen);
					return x;
				}
				System.arraycopy(a, x, a, y, r);
				clear(a, x + 1, r - 1, clear, clearLen);
				return x;
			}
			int g = aLen - s;
			System.arraycopy(a, x, a, x + g, r);
			clear(a, x + 1, (g < r ? g : r) - 1, clear, clearLen);
			return x;
		}
		// target tail
		int l = index - n;
		int r = s - l;
		if (r < l) {
			// rotate tail right
			int x = i + index - s;
			if (p >= 0) {
				int g = aLen - s;
				int y = x - s;
				System.arraycopy(a, 0, a, g, p);
				if (y < 0) {
					this._idx = s > i ? i + g : i - s;
					System.arraycopy(a, s, a, 0, g);
					System.arraycopy(a, x, a, x + g, -y);
					clear(a, x + 1, g - 1, clear, clearLen);
					return x;
				}
				this._idx = i - s;
				int z = aLen - x;
				System.arraycopy(a, x, a, y, z);
				clear(a, x + 1, z - 1, clear, clearLen);
				clear(a, 0, g < r ? y : p, clear, clearLen);
				return x;
			}
			int q = gapOff - s;
			if (q >= 0) {
				if (s < x) {
					this._idx = i - s;
					System.arraycopy(a, x, a, x - s, r);
					clear(a, x + 1, r - 1, clear, clearLen);
					return x;
				}
				int g = aLen - s;
				this._idx = s > i ? i + g : i - s;
				System.arraycopy(a, s, a, 0, q);
				System.arraycopy(a, x, a, x + g, s - x);
				clear(a, x + 1, (g < r ? g : r) - 1, clear, clearLen);
				return x;
			}
			int g = aLen - s;
			this._idx = i + g;
			if (x < 0) {
				System.arraycopy(a, 0, a, g, gapOff);
				int y = x + aLen;	// i + index + g
				int z = s - y;
				if (z < 0) {
					System.arraycopy(a, y, a, -z, -x);
					clear(a, y + 1, -x - 1, clear, clearLen);
					clear(a, 0, g < r ? -z : gapOff, clear, clearLen);
					return y;
				}
				System.arraycopy(a, s, a, 0, g);
				System.arraycopy(a, y, a, y + g, z);
				clear(a, y + 1, g - 1, clear, clearLen);
				return y;
			}
			System.arraycopy(a, x, a, x + g, r);
			clear(a, x + 1, (g < r ? g : r) - 1, clear, clearLen);
			return x;
		}
		// rotate tail left
		int q = gapOff - s;
		if (p >= 0) {
			int g = aLen - s;
			int x = p + l;	// == i + index - aLen
			System.arraycopy(a, q, a, p, l);
			if (g > l) {
				clear(a, q, l, clear, clearLen);
			} else {
				clear(a, x + 1, g - 1, clear, clearLen);
			}
			return x;
		}
		if (q >= 0) {
			int x = p + l;	// == i + index - aLen
			if (x < 0) {
				System.arraycopy(a, q, a, gapOff, l);
				clear(a, q, l, clear, clearLen);
				return i + index;
			}
			int g = aLen - s;
			System.arraycopy(a, q, a, gapOff, -p);
			System.arraycopy(a, g, a, 0, x);
			if (g > l) {
				clear(a, q, l, clear, clearLen);
			} else {
				clear(a, x + 1, g - 1, clear, clearLen);
			}
			return x;
		}
		int g = aLen - s;
		int tailOff = gapOff + g;
		int x = q + l;	// == i + index - s
		if (x < 0) {
			int y = i + index;
			System.arraycopy(a, tailOff, a, gapOff, l);
			if (g > l) {
				clear(a, tailOff, l, clear, clearLen);
			} else {
				clear(a, y + 1, g - 1, clear, clearLen);
			}
			return y;
		}
		int y = p + l;	// i + index - aLen
		System.arraycopy(a, tailOff, a, gapOff, -q);
		if (y < 0) {
			int z = i + index;
			System.arraycopy(a, 0, a, s, x);
			clear(a, 0, x, clear, clearLen);
			if (g > l) {
				clear(a, tailOff, -q, clear, clearLen);
			} else {
				clear(a, z + 1, -y - 1, clear, clearLen);
			}
			return z;
		}
		System.arraycopy(a, 0, a, s, g);
		System.arraycopy(a, g, a, 0, y);
		clear(a, y + 1, g - 1, clear, clearLen);
		return y;
	}

	// the parameters are never negative, so masking is not required
	// if only Java had multiple return values.
	// very circuotous way of propagating results, but better than repeating
	// thousands of identical lines of code
	private static long _toCopyBounds(int off, int len) {
		return ((long) off << 32) | len;
	}

	private static int _copyOff(long bounds) {
		return (int) (bounds >>> 32);
	}

	private static int _copyLen(long bounds) {
		return (int) bounds;
	}

	@SuppressWarnings("SuspiciousSystemArraycopy")
	protected final void _remove(A a, int aLen, int s, int i, int n,
			int index, int len) throws ArrayIndexOutOfBoundsException {
		this._size -= len;
		this._len = index;

		int gapOff = i + n;
		int p = gapOff - aLen;
		if (index < n) {
			// target head
			int r = n - index;
			if (len > r) {
				// removal bridges gap. no elements need to be moved
				return;
			}
			int l = s - r;
			int m = r - len;
			if (m > l) {
				// rotate left
				int q = gapOff - s;
				int g = aLen - s;
				if (p >= 0) {
					this._idx = i - g;
					int x = i + index;
					int z = x - aLen;
					int u = z + len;
					if (u < 0) {
						System.arraycopy(a, q, a, p, l);
						return;
					}
					if (z < 0) {
						System.arraycopy(a, q, a, p, l);
						return;
					}
					System.arraycopy(a, q, a, p, s - p);
					if (z < g) {
						System.arraycopy(a, 0, a, s, z);
						return;
					}
					int v = z - g;
					System.arraycopy(a, 0, a, s, g);
					System.arraycopy(a, g, a, 0, v);
					return;
				}
				if (q >= 0) {
					this._idx = i < g ? i + s : i - g;
					int y = i + index - g;
					if (y < 0) {
						System.arraycopy(a, q, a, gapOff, l);
						return;
					}
					System.arraycopy(a, q, a, gapOff, -p);
					System.arraycopy(a, g, a, 0, y);
					return;
				}
				int tailOff = gapOff + g;
				System.arraycopy(a, tailOff, a, gapOff, -q);
				int x = i + index;
				int y = x - g;
				if (y < 0) {
					this._idx = i + s;
					System.arraycopy(a, 0, a, s, x);
					return;
				}
				this._idx = i < g ? i + s : i - g;
				System.arraycopy(a, 0, a, s, g);
				System.arraycopy(a, g, a, 0, y);
				return;
			}
			// rotate right
			int x = i + index;
			int y = x + len;
			if (p >= 0) {
				int g = aLen - s;
				int z = aLen - y;
				if (z < 0) {
					int u = y - s;
					System.arraycopy(a, -z, a, u, m);
					return;
				}
				System.arraycopy(a, 0, a, g, p);
				if (y < s) {
					System.arraycopy(a, s, a, 0, g);
					System.arraycopy(a, y, a, y + g, s - y);
					return;
				}
				int u = y - s;
				System.arraycopy(a, y, a, u, z);
				return;
			}
			int q = gapOff - s;
			if (q >= 0) {
				if (y < s) {
					int g = aLen - s;
					System.arraycopy(a, s, a, 0, q);
					System.arraycopy(a, y, a, y + g, s - y);
					return;
				}
				System.arraycopy(a, y, a, y - s, m);
				return;
			}
			int g = aLen - s;
			System.arraycopy(a, y, a, y + g, m);
			return;
		}
		// target tail
		int l = index - n;
		int r = s - l;
		int m = r - len;
		if (m < l) {
			// rotate right
			if (p >= 0) {
				int g = aLen - s;
				int x = i + index - s;
				int y = x + len;
				int z = s - y;
				System.arraycopy(a, 0, a, g, p);
				if (z < 0) {
					this._idx = i - s;
					System.arraycopy(a, y, a, -z, aLen - y);
					return;
				}
				this._idx = i < s ? i + g : i - s;
				System.arraycopy(a, s, a, 0, g);
				System.arraycopy(a, y, a, y + g, z);
				return;
			}
			int q = gapOff - s;
			if (q >= 0) {
				int x = i + index - s;
				int y = x + len;
				int z = s - y;
				if (z < 0) {
					this._idx = i - s;
					System.arraycopy(a, y, a, -z, m);
					return;
				}
				int g = aLen - s;
				this._idx = i < s ? i + g : i - s;
				System.arraycopy(a, s, a, 0, q);
				System.arraycopy(a, y, a, y + g, z);
				return;
			}
			int g = aLen - s;
			this._idx = i + g;
			int x = i + index;
			int y = s - x;
			int z = x + len - s;
			if (y < 0) {
				int u = z + g;
				System.arraycopy(a, z, a, u, m);
				return;
			}
			if (z < 0) {
				System.arraycopy(a, 0, a, g, gapOff);
				int u = z + g;
				int v = u + s;
				if (u < 0) {
					System.arraycopy(a, s, a, 0, g);
					System.arraycopy(a, v, a, v + g, -u);
					return;
				}
				System.arraycopy(a, v, a, u, -z);
				return;
			}
			int u = z + g;
			System.arraycopy(a, z, a, u, m);
			return;
		}
		// rotate left
		int q = gapOff - s;
		if (p >= 0) {
			System.arraycopy(a, q, a, p, l);
			return;
		}
		if (q >= 0) {
			int x = l + p;
			if (x < 0) {
				System.arraycopy(a, q, a, gapOff, l);
				return;
			}
			int g = aLen - s;
			System.arraycopy(a, q, a, gapOff, -p);
			System.arraycopy(a, g, a, 0, x);
			return;
		}
		int g = aLen - s;
		int x = i + index;
		int y = x + len - s;
		int tailOff = gapOff + g;
		if (y < 0) {
			System.arraycopy(a, tailOff, a, gapOff, l);
			return;
		}
		int z = x - s;
		if (z < 0) {
			System.arraycopy(a, tailOff, a, gapOff, l);
			return;
		}
		System.arraycopy(a, tailOff, a, gapOff, -q);
		if (z < g) {
			System.arraycopy(a, 0, a, s, z);
			return;
		}
		int u = x - aLen;
		System.arraycopy(a, 0, a, s, g);
		System.arraycopy(a, g, a, 0, u);
	}

	@SuppressWarnings("SuspiciousSystemArraycopy")
	protected final void _remove(A a, int aLen, int s, int i, int n,
			int index, int len, A clear, int clearLen) throws ArrayIndexOutOfBoundsException {
		this._size -= len;
		this._len = index;

		int gapOff = i + n;
		int p = gapOff - aLen;
		if (index < n) {
			// target head
			int r = n - index;
			if (len > r) {
				// removal bridges gap. no elements need to be moved
				int x = i + index;
				int q = gapOff - s;
				if (p >= 0) {
					clear(a, q, len - r, clear, clearLen);
					int y = x - aLen;
					if (y < 0) {
						clear(a, 0, p, clear, clearLen);
						clear(a, x, -y, clear, clearLen);
						return;
					}
					clear(a, y, r, clear, clearLen);
					return;
				}
				clear(a, x, r, clear, clearLen);
				if (q >= 0) {
					clear(a, q, len - r, clear, clearLen);
					return;
				}
				int g = aLen - s;
				int y = x + len - s;
				if (y < 0) {
					clear(a, gapOff + g, len - r, clear, clearLen);
					return;
				}
				clear(a, gapOff + g, -q, clear, clearLen);
				clear(a, 0, y, clear, clearLen);
				return;
			}
			int l = s - r;
			int m = r - len;
			if (m > l) {
				// rotate left
				int q = gapOff - s;
				int g = aLen - s;
				if (p >= 0) {
					this._idx = i - g;
					int x = i + index;
					int z = x - aLen;
					int u = z + len;
					if (u < 0) {
						System.arraycopy(a, q, a, p, l);
						if (g < l) {
							clear(a, x - g, g + len, clear, clearLen);
						} else {
							clear(a, q, l + len, clear, clearLen);
						}
						return;
					}
					if (z < 0) {
						System.arraycopy(a, q, a, p, l);
						clear(a, 0, u, clear, clearLen);
						int y = x - g;
						if (g < l) {
							clear(a, y, aLen - y, clear, clearLen);
						} else {
							clear(a, q, s - p, clear, clearLen);
						}
						return;
					}
					System.arraycopy(a, q, a, p, s - p);
					if (z < g) {
						System.arraycopy(a, 0, a, s, z);
						clear(a, 0, u, clear, clearLen);
						int y = x - g;
						if (g < l) {
							clear(a, y, aLen - y, clear, clearLen);
						} else {
							clear(a, q, s - p, clear, clearLen);
						}
						return;
					}
					int v = z - g;
					System.arraycopy(a, 0, a, s, g);
					System.arraycopy(a, g, a, 0, v);
					clear(a, v, g + len, clear, clearLen);
					return;
				}
				if (q >= 0) {
					this._idx = i < g ? i + s : i - g;
					int y = i + index - g;
					if (y < 0) {
						System.arraycopy(a, q, a, gapOff, l);
						clear(a, q, l + len, clear, clearLen);
						return;
					}
					System.arraycopy(a, q, a, gapOff, -p);
					System.arraycopy(a, g, a, 0, y);
					if (g < l) {
						clear(a, y, g + len, clear, clearLen);
					} else {
						clear(a, q, l + len, clear, clearLen);
					}
					return;
				}
				int tailOff = gapOff + g;
				System.arraycopy(a, tailOff, a, gapOff, -q);
				int x = i + index;
				int y = x - g;
				if (y < 0) {
					this._idx = i + s;
					System.arraycopy(a, 0, a, s, x);
					clear(a, 0, x + len, clear, clearLen);
					if (g < l) {
						clear(a, s + x, -y, clear, clearLen);
					} else {
						clear(a, tailOff, -q, clear, clearLen);
					}
					return;
				}
				this._idx = i < g ? i + s : i - g;
				System.arraycopy(a, 0, a, s, g);
				System.arraycopy(a, g, a, 0, y);
				clear(a, y, g + len, clear, clearLen);
				return;
			}
			// rotate right
			int x = i + index;
			int y = x + len;
			if (p >= 0) {
				int g = aLen - s;
				int z = aLen - y;
				if (z < 0) {
					int u = y - s;
					int v = aLen - x;
					System.arraycopy(a, -z, a, u, m);
					if (v < 0) {
						clear(a, -v, g < m ? g + len : r, clear, clearLen);
						return;
					}
					clear(a, x, v, clear, clearLen);
					clear(a, 0, g < m ? u : p, clear, clearLen);
					return;
				}
				System.arraycopy(a, 0, a, g, p);
				if (y < s) {
					System.arraycopy(a, s, a, 0, g);
					System.arraycopy(a, y, a, y + g, s - y);
					clear(a, x, g + len, clear, clearLen);
					return;
				}
				int u = y - s;
				System.arraycopy(a, y, a, u, z);
				clear(a, x, aLen - x, clear, clearLen);
				clear(a, 0, g < m ? u : p, clear, clearLen);
				return;
			}
			int q = gapOff - s;
			if (q >= 0) {
				if (y < s) {
					int g = aLen - s;
					System.arraycopy(a, s, a, 0, q);
					System.arraycopy(a, y, a, y + g, s - y);
					clear(a, x, g < m ? g + len : r, clear, clearLen);
					return;
				}
				System.arraycopy(a, y, a, y - s, m);
				clear(a, x, r, clear, clearLen);
				return;
			}
			int g = aLen - s;
			System.arraycopy(a, y, a, y + g, m);
			clear(a, x, g < m ? g + len : r, clear, clearLen);
			return;
		}
		// target tail
		int l = index - n;
		int r = s - l;
		int m = r - len;
		if (m < l) {
			// rotate right
			if (p >= 0) {
				int g = aLen - s;
				int x = i + index - s;
				int y = x + len;
				int z = s - y;
				System.arraycopy(a, 0, a, g, p);
				if (z < 0) {
					this._idx = i - s;
					System.arraycopy(a, y, a, -z, aLen - y);
					clear(a, x, aLen - x, clear, clearLen);
					clear(a, 0, g < m ? -z : p, clear, clearLen);
					return;
				}
				this._idx = i < s ? i + g : i - s;
				System.arraycopy(a, s, a, 0, g);
				System.arraycopy(a, y, a, y + g, z);
				clear(a, x, g + len, clear, clearLen);
				return;
			}
			int q = gapOff - s;
			if (q >= 0) {
				int x = i + index - s;
				int y = x + len;
				int z = s - y;
				if (z < 0) {
					this._idx = i - s;
					System.arraycopy(a, y, a, -z, m);
					clear(a, x, r, clear, clearLen);
					return;
				}
				int g = aLen - s;
				this._idx = i < s ? i + g : i - s;
				System.arraycopy(a, s, a, 0, q);
				System.arraycopy(a, y, a, y + g, z);
				clear(a, x, g < m ? g + len : r, clear, clearLen);
				return;
			}
			int g = aLen - s;
			this._idx = i + g;
			int x = i + index;
			int y = s - x;
			int z = x + len - s;
			if (y < 0) {
				int u = z + g;
				System.arraycopy(a, z, a, u, m);
				clear(a, -y, g < m ? g + len : r, clear, clearLen);
				return;
			}
			if (z < 0) {
				System.arraycopy(a, 0, a, g, gapOff);
				int u = z + g;
				int v = u + s;
				if (u < 0) {
					System.arraycopy(a, s, a, 0, g);
					System.arraycopy(a, v, a, v + g, -u);
					clear(a, x + g, g + len, clear, clearLen);
					return;
				}
				System.arraycopy(a, v, a, u, -z);
				clear(a, x + g, y, clear, clearLen);
				clear(a, 0, g < m ? u : gapOff, clear, clearLen);
				return;
			}
			int u = z + g;
			System.arraycopy(a, z, a, u, m);
			clear(a, x + g, y, clear, clearLen);
			clear(a, 0, g < m ? u : gapOff, clear, clearLen);
			return;
		}
		// rotate left
		int q = gapOff - s;
		if (p >= 0) {
			int g = aLen - s;
			System.arraycopy(a, q, a, p, l);
			if (g < l) {
				clear(a, l + p, g + len, clear, clearLen);
			} else {
				clear(a, q, l + len, clear, clearLen);
			}
			return;
		}
		if (q >= 0) {
			int x = l + p;
			if (x < 0) {
				System.arraycopy(a, q, a, gapOff, l);
				clear(a, q, l + len, clear, clearLen);
				return;
			}
			int g = aLen - s;
			System.arraycopy(a, q, a, gapOff, -p);
			System.arraycopy(a, g, a, 0, x);
			if (g < l) {
				clear(a, x, g + len, clear, clearLen);
			} else {
				clear(a, q, l + len, clear, clearLen);
			}
			return;
		}
		int g = aLen - s;
		int x = i + index;
		int y = x + len - s;
		int tailOff = gapOff + g;
		if (y < 0) {
			System.arraycopy(a, tailOff, a, gapOff, l);
			if (g < l) {
				clear(a, x, g + len, clear, clearLen);
			} else {
				clear(a, tailOff, l + len, clear, clearLen);
			}
			return;
		}
		int z = x - s;
		if (z < 0) {
			System.arraycopy(a, tailOff, a, gapOff, l);
			clear(a, 0, y, clear, clearLen);
			if (g < l) {
				clear(a, x, aLen - x, clear, clearLen);
			} else {
				clear(a, tailOff, -q, clear, clearLen);
			}
			return;
		}
		System.arraycopy(a, tailOff, a, gapOff, -q);
		if (z < g) {
			System.arraycopy(a, 0, a, s, z);
			clear(a, 0, y, clear, clearLen);
			if (g < l) {
				clear(a, x, aLen - x, clear, clearLen);
			} else {
				clear(a, tailOff, -q, clear, clearLen);
			}
			return;
		}
		int u = x - aLen;
		System.arraycopy(a, 0, a, s, g);
		System.arraycopy(a, g, a, 0, u);
		clear(a, u, g + len, clear, clearLen);
	}

	// returns new _idx
	protected final int _growFair(A a, int aLen, A b, int bLen, int s, int i, int n) throws ArrayIndexOutOfBoundsException {
		// The gap as it currently exists is preserved.
		// However, it is moved to the borders of the array, 
		// such that no elements straddle the array borders
		// and instead reside near the middle of the array.
		// This can help retain a normalized layout (if it is already
		// normalized) when only prepending and appending, 
		// at no additional cost.
		int g = (bLen - s) >>> 1;	// new gap on the left
		return _grow(a, aLen, b, s, i, n, g);
	}

	// returns new _idx
	protected final int _growUnfair(A a, int aLen, A b, int s, int i, int n) throws ArrayIndexOutOfBoundsException {
		// The gap as it currently exists is preserved.
		// However, it is moved to the end of the array, 
		// such that no elements straddle the array borders
		// and instead reside at the start of the array.
		// This can help retain a normalized layout (if it is already
		// normalized) when only appending, 
		// at no additional cost.
		return _grow(a, aLen, b, s, i, n, 0);
	}

	@SuppressWarnings("SuspiciousSystemArraycopy")
	private int _grow(A a, int aLen, A b, int s, int i, int n, int g) throws ArrayIndexOutOfBoundsException {
		int gapOff = i + n;
		int p = gapOff - aLen;
		if (p >= 0) {
			int w = s - p;
			System.arraycopy(a, aLen - w, b, g, w);
			System.arraycopy(a, 0, b, g + w, p);
			return (this._idx = g + s - n);	// simplified "g + i - (aLen - w))"
		}
		int q = gapOff - s;
		if (q >= 0) {
			System.arraycopy(a, q, b, g, s);
			return (this._idx = g + i - q);
		}
		System.arraycopy(a, aLen + q, b, g, -q);
		System.arraycopy(a, 0, b, g - q, gapOff);
		return (this._idx = g + i - q);
	}

	@SuppressWarnings("SuspiciousSystemArraycopy")
	protected final <T> int _normalize(T a, int aLen, int s, int i, int n,
			A clear, int clearLen) throws ArrayIndexOutOfBoundsException {
		int gapOff = i + n;
		int p = gapOff - aLen;
		if (p >= 0) {
			int g = aLen - s;
			if (i >= s) {
				int x = i - s;
				int y = aLen - i;
				System.arraycopy(a, 0, a, g, p);
				System.arraycopy(a, i, a, x, y);
				clear(a, i, y, clear, clearLen);
				clear(a, 0, n < g ? p : x, clear, clearLen);
				this._len = 0;
				return x;
			}
			int x = i - g;
			if (x < g) {
				int y = x + s;
				System.arraycopy(a, gapOff - s, a, p, s - p);
				System.arraycopy(a, 0, a, s, x);
				clear(a, 0, x, clear, clearLen);
				clear(a, y, g - x, clear, clearLen);
				this._idx = y;
				this._len = 0;
				return x;
			}
			int y = aLen - x;
			if (i >= y) {
				int helperLen = s - i;
				A helper = getHelperArray(helperLen);
				System.arraycopy(a, 0, a, g, p);
				System.arraycopy(a, s, a, 0, g);
				System.arraycopy(a, i, helper, 0, helperLen);
				System.arraycopy(a, 0, a, helperLen, i);
				System.arraycopy(helper, 0, a, 0, helperLen);
				clear(a, s, g, clear, clearLen);
				this._idx = g == 0 ? 0 : s;
				this._len = 0;
				return 0;
			}
			int helperLen = x - g;
			A helper = getHelperArray(helperLen);
			System.arraycopy(a, gapOff - s, a, p, s - p);
			System.arraycopy(a, 0, a, s, g);
			System.arraycopy(a, g, helper, 0, helperLen);
			System.arraycopy(a, x, a, 0, y);
			System.arraycopy(helper, 0, a, y, helperLen);
			clear(a, s, g, clear, clearLen);
			this._idx = g == 0 ? 0 : s;
			this._len = 0;
			return 0;
		}
		int q = gapOff - s;
		if (q >= 0) {
			int t = s - n;
			if (t >= n) {
				if (i >= s) {
					int x = i - s;
					System.arraycopy(a, i, a, x, n);
					clear(a, i, n, clear, clearLen);
					this._len = 0;
					return x;
				}
				int g = aLen - s;
				if (i < g) {
					System.arraycopy(a, q, a, gapOff, t);
					clear(a, q, t, clear, clearLen);
					this._idx = i + s;
					this._len = 0;
					return i;
				}
				int x = s - i;
				if (g >= n) {
					System.arraycopy(a, s, a, 0, q);
					System.arraycopy(a, 0, a, x, s);
					System.arraycopy(a, s, a, 0, x);
					clear(a, s, q < x ? x : q, clear, clearLen);
					this._idx = s;
					this._len = 0;
					return 0;
				}
				A helper = getHelperArray(x);
				System.arraycopy(a, s, a, 0, q);
				System.arraycopy(a, i, helper, 0, x);
				System.arraycopy(a, 0, a, x, i);
				System.arraycopy(helper, 0, a, 0, x);
				clear(a, s, q, clear, clearLen);
				this._idx = s;
				this._len = 0;
				return 0;
			}
			int g = aLen - s;
			if (i < g) {
				System.arraycopy(a, q, a, gapOff, t);
				clear(a, q, t, clear, clearLen);
				this._idx = i + s;
				this._len = 0;
				return i;
			}
			if (i >= s) {
				int x = i - s;
				System.arraycopy(a, i, a, x, n);
				clear(a, i, n, clear, clearLen);
				this._len = 0;
				return x;
			}
			int x = i - g;
			if (g >= t) {
				int y = g - x;
				System.arraycopy(a, q, a, gapOff, -p);
				System.arraycopy(a, g, a, y, s);
				System.arraycopy(a, y, a, aLen - x, x);
				if (-p < x) {
					clear(a, y, x, clear, clearLen);
				} else {
					clear(a, q, -p, clear, clearLen);
				}
				this._idx = 0;
				this._len = 0;
				return g;
			}
			A helper = getHelperArray(x);
			System.arraycopy(a, q, a, gapOff, -p);
			System.arraycopy(a, g, helper, 0, x);
			System.arraycopy(a, i, a, g, n - p);
			System.arraycopy(helper, 0, a, aLen - x, x);
			clear(a, q, -p, clear, clearLen);
			this._idx = 0;
			this._len = 0;
			return g;
		}
		int g = aLen - s;
		if (i < g) {
			int tailOff = gapOff + g;
			System.arraycopy(a, tailOff, a, gapOff, -q);
			System.arraycopy(a, 0, a, s, i);
			clear(a, 0, i, clear, clearLen);
			int x = i + s;
			int y = g - i;
			if (y < -q) {
				clear(a, x, y, clear, clearLen);
			} else {
				clear(a, tailOff, -q, clear, clearLen);
			}
			this._idx = x;
			this._len = 0;
			return i;
		}
		int x = s - i;
		if (g >= x) {
			int y = i + g;
			int z = g - x;
			System.arraycopy(a, 0, a, g, gapOff);
			System.arraycopy(a, y, a, z, x);
			clear(a, y, x, clear, clearLen);
			clear(a, 0, z, clear, clearLen);
			this._idx = y;
			this._len = 0;
			return z;
		}
		if (x >= i) {
			int y = aLen - i;
			int helperLen = i - g;
			A helper = getHelperArray(helperLen);
			System.arraycopy(a, gapOff + g, a, gapOff, -q);
			System.arraycopy(a, 0, a, s, g);
			System.arraycopy(a, g, helper, 0, helperLen);
			System.arraycopy(a, i, a, 0, y);
			System.arraycopy(helper, 0, a, y, helperLen);
			clear(a, s, g, clear, clearLen);
			this._idx = g == 0 ? 0 : s;
			this._len = 0;
			return 0;
		}
		int y = i + g;
		int helperLen = x - g;
		A helper = getHelperArray(helperLen);
		System.arraycopy(a, 0, a, g, gapOff);
		System.arraycopy(a, s, a, 0, g);
		System.arraycopy(a, y, helper, 0, helperLen);
		System.arraycopy(a, 0, a, helperLen, y);
		System.arraycopy(helper, 0, a, 0, helperLen);
		clear(a, s, g, clear, clearLen);
		this._idx = g == 0 ? 0 : s;
		this._len = 0;
		return 0;
	}

	@SuppressWarnings("SuspiciousSystemArraycopy")
	protected final <T> int _normalize(T a, int aLen, int s, int i, int n)
			throws ArrayIndexOutOfBoundsException {
		int gapOff = i + n;
		int p = gapOff - aLen;
		if (p >= 0) {
			int g = aLen - s;
			if (i >= s) {
				int x = i - s;
				int y = aLen - i;
				System.arraycopy(a, 0, a, g, p);
				System.arraycopy(a, i, a, x, y);
				this._len = 0;
				return x;
			}
			int x = i - g;
			if (x < g) {
				int y = x + s;
				System.arraycopy(a, gapOff - s, a, p, s - p);
				System.arraycopy(a, 0, a, s, x);
				this._idx = y;
				this._len = 0;
				return x;
			}
			int y = aLen - x;
			if (i >= y) {
				int helperLen = s - i;
				A helper = getHelperArray(helperLen);
				System.arraycopy(a, 0, a, g, p);
				System.arraycopy(a, s, a, 0, g);
				System.arraycopy(a, i, helper, 0, helperLen);
				System.arraycopy(a, 0, a, helperLen, i);
				System.arraycopy(helper, 0, a, 0, helperLen);
				this._idx = g == 0 ? 0 : s;
				this._len = 0;
				return 0;
			}
			int helperLen = x - g;
			A helper = getHelperArray(helperLen);
			System.arraycopy(a, gapOff - s, a, p, s - p);
			System.arraycopy(a, 0, a, s, g);
			System.arraycopy(a, g, helper, 0, helperLen);
			System.arraycopy(a, x, a, 0, y);
			System.arraycopy(helper, 0, a, y, helperLen);
			this._idx = g == 0 ? 0 : s;
			this._len = 0;
			return 0;
		}
		int q = gapOff - s;
		if (q >= 0) {
			int t = s - n;
			if (t >= n) {
				if (i >= s) {
					int x = i - s;
					System.arraycopy(a, i, a, x, n);
					this._len = 0;
					return x;
				}
				int g = aLen - s;
				if (i < g) {
					System.arraycopy(a, q, a, gapOff, t);
					this._idx = i + s;
					this._len = 0;
					return i;
				}
				int x = s - i;
				if (g >= n) {
					System.arraycopy(a, s, a, 0, q);
					System.arraycopy(a, 0, a, x, s);
					System.arraycopy(a, s, a, 0, x);
					this._idx = s;
					this._len = 0;
					return 0;
				}
				A helper = getHelperArray(x);
				System.arraycopy(a, s, a, 0, q);
				System.arraycopy(a, i, helper, 0, x);
				System.arraycopy(a, 0, a, x, i);
				System.arraycopy(helper, 0, a, 0, x);
				this._idx = s;
				this._len = 0;
				return 0;
			}
			int g = aLen - s;
			if (i < g) {
				System.arraycopy(a, q, a, gapOff, t);
				this._idx = i + s;
				this._len = 0;
				return i;
			}
			if (i >= s) {
				int x = i - s;
				System.arraycopy(a, i, a, x, n);
				this._len = 0;
				return x;
			}
			int x = i - g;
			if (g >= t) {
				int y = g - x;
				System.arraycopy(a, q, a, gapOff, -p);
				System.arraycopy(a, g, a, y, s);
				System.arraycopy(a, y, a, aLen - x, x);
				this._idx = 0;
				this._len = 0;
				return g;
			}
			A helper = getHelperArray(x);
			System.arraycopy(a, q, a, gapOff, -p);
			System.arraycopy(a, g, helper, 0, x);
			System.arraycopy(a, i, a, g, n - p);
			System.arraycopy(helper, 0, a, aLen - x, x);
			this._idx = 0;
			this._len = 0;
			return g;
		}
		int g = aLen - s;
		if (i < g) {
			int tailOff = gapOff + g;
			System.arraycopy(a, tailOff, a, gapOff, -q);
			System.arraycopy(a, 0, a, s, i);
			int x = i + s;
			this._idx = x;
			this._len = 0;
			return i;
		}
		int x = s - i;
		if (g >= x) {
			int y = i + g;
			int z = g - x;
			System.arraycopy(a, 0, a, g, gapOff);
			System.arraycopy(a, y, a, z, x);
			this._idx = y;
			this._len = 0;
			return z;
		}
		if (x >= i) {
			int y = aLen - i;
			int helperLen = i - g;
			A helper = getHelperArray(helperLen);
			System.arraycopy(a, gapOff + g, a, gapOff, -q);
			System.arraycopy(a, 0, a, s, g);
			System.arraycopy(a, g, helper, 0, helperLen);
			System.arraycopy(a, i, a, 0, y);
			System.arraycopy(helper, 0, a, y, helperLen);
			this._idx = g == 0 ? 0 : s;
			this._len = 0;
			return 0;
		}
		int y = i + g;
		int helperLen = x - g;
		A helper = getHelperArray(helperLen);
		System.arraycopy(a, 0, a, g, gapOff);
		System.arraycopy(a, s, a, 0, g);
		System.arraycopy(a, y, helper, 0, helperLen);
		System.arraycopy(a, 0, a, helperLen, y);
		System.arraycopy(helper, 0, a, 0, helperLen);
		this._idx = g == 0 ? 0 : s;
		this._len = 0;
		return 0;
	}

	protected final <T, B, E extends RuntimeException> void _sortArray(T a, int aLen, int s, int i, int n,
			int index, int len, boolean allowMod, B comparator, Sorter<T, B> sorter, Supplier<E> sup,
			A clear, int clearLen) throws E {
		int x = inPlaceIndexForSort(aLen, s, i, n, index, len);
		if (x >= 0) {
			// array can be sorted without re-arranging elements or 
			// using additional storage of any kind.
			testBounds(aLen, x, len, sup);
			sorter.sort(a, x, x + len, comparator);
			return;
		}
		// range is broken up
		x = gapIndexForSort(aLen, s, i, n, len);
		if (x >= 0) {
			// range can be sorted using gap for temp storage
			testBounds(aLen, x, len, sup);
			try {
				_read(a, aLen, s, i, n, index, a, x, len);
			} catch (ArrayIndexOutOfBoundsException ex) {
				throw sup.get();
			}
			// copied range into gap
			sorter.sort(a, x, x + len, comparator);
			_write(a, aLen, s, i, n, index, a, x, len);
			clear(a, x, len, clear, clearLen);
			return;
		}

		T b = null;
		ArrayRef<A> ref = this._helper;
		if (ref != null && ref.len >= len) {
			b = (T) ref.get();
		}
		if (b == null && allowMod) {
			// no helper is available and 
			// re-arranging array is allowed
			try {
				x = index + _normalize(a, aLen, s, i, n, clear, clearLen);
			} catch (ArrayIndexOutOfBoundsException ex) {
				throw sup.get();
			}
			testBounds(aLen, x, len, sup);
			sorter.sort(a, x, x + len, comparator);
			return;
		}
		if (b == null) {
			b = (T) getHelperArray(len);
		}
		// use new temp array
		try {
			_read(a, aLen, s, i, n, index, b, 0, len);
		} catch (ArrayIndexOutOfBoundsException ex) {
			throw sup.get();
		}
		sorter.sort(b, 0, len, comparator);
		_write(a, aLen, s, i, n, index, b, 0, len);
	}

	protected final <T, B, E extends RuntimeException> void _sortArray(T a, int aLen, int s, int i, int n,
			int index, int len, boolean allowMod, B comparator, Sorter<T, B> sorter, Supplier<E> sup) throws E {
		int x = inPlaceIndexForSort(aLen, s, i, n, index, len);
		if (x >= 0) {
			// array can be sorted without re-arranging elements or 
			// using additional storage of any kind.
			testBounds(aLen, x, len, sup);
			sorter.sort(a, x, x + len, comparator);
			return;
		}
		// range is broken up
		x = gapIndexForSort(aLen, s, i, n, len);
		if (x >= 0) {
			// range can be sorted using gap for temp storage
			testBounds(aLen, x, len, sup);
			try {
				_read(a, aLen, s, i, n, index, a, x, len);
			} catch (ArrayIndexOutOfBoundsException ex) {
				throw sup.get();
			}
			// copied range into gap
			sorter.sort(a, x, x + len, comparator);
			_write(a, aLen, s, i, n, index, a, x, len);
			return;
		}
		T b = null;
		ArrayRef<A> ref = this._helper;
		if (ref != null && ref.len >= len) {
			b = (T) ref.get();
		}
		if (b == null && allowMod) {
			// no helper is available and 
			// re-arranging array is allowed
			try {
				x = index + _normalize(a, aLen, s, i, n);
			} catch (ArrayIndexOutOfBoundsException ex) {
				throw sup.get();
			}
			testBounds(aLen, x, len, sup);
			sorter.sort(a, x, x + len, comparator);
			return;
		}
		if (b == null) {
			b = (T) getHelperArray(len);
		}
		// use new temp array
		try {
			_read(a, aLen, s, i, n, index, b, 0, len);
		} catch (ArrayIndexOutOfBoundsException ex) {
			throw sup.get();
		}
		sorter.sort(b, 0, len, comparator);
		_write(a, aLen, s, i, n, index, b, 0, len);
	}

	protected final <T, B> void _sortArray(T a, int aLen, int s, int i, int n,
			int index, int len, boolean allowMod, B comparator, Sorter<T, B> sorter,
			A clear, int clearLen) throws ArrayIndexOutOfBoundsException {
		int x = inPlaceIndexForSort(aLen, s, i, n, index, len);
		if (x >= 0) {
			// array can be sorted without re-arranging elements or 
			// using additional storage of any kind.
			sorter.sort(a, x, x + len, comparator);
			return;
		}
		// range is broken up
		x = gapIndexForSort(aLen, s, i, n, len);
		if (x >= 0) {
			// range can be sorted using gap for temp storage
			_read(a, aLen, s, i, n, index, a, x, len);
			// copied range into gap
			sorter.sort(a, x, x + len, comparator);
			_write(a, aLen, s, i, n, index, a, x, len);
			clear(a, x, len, clear, clearLen);
			return;
		}
		T b = null;
		ArrayRef<A> ref = this._helper;
		if (ref != null && ref.len >= len) {
			b = (T) ref.get();
		}
		if (b == null && allowMod) {
			// no helper is available and 
			// re-arranging array is allowed
			x = index + _normalize(a, aLen, s, i, n, clear, clearLen);
			sorter.sort(a, x, x + len, comparator);
			return;
		}
		if (b == null) {
			b = (T) getHelperArray(len);
		}
		// use new temp array
		_read(a, aLen, s, i, n, index, b, 0, len);
		sorter.sort(b, 0, len, comparator);
		_write(a, aLen, s, i, n, index, b, 0, len);
	}

	protected final <T, B> void _sortArray(T a, int aLen, int s, int i, int n,
			int index, int len, boolean allowMod, B comparator, Sorter<T, B> sorter) throws ArrayIndexOutOfBoundsException {
		int x = inPlaceIndexForSort(aLen, s, i, n, index, len);
		if (x >= 0) {
			// array can be sorted without re-arranging elements or 
			// using additional storage of any kind.
			sorter.sort(a, x, x + len, comparator);
			return;
		}
		// range is broken up
		x = gapIndexForSort(aLen, s, i, n, len);
		if (x >= 0) {
			// range can be sorted using gap for temp storage
			_read(a, aLen, s, i, n, index, a, x, len);
			// copied range into gap
			sorter.sort(a, x, x + len, comparator);
			_write(a, aLen, s, i, n, index, a, x, len);
			return;
		}
		T b = null;
		ArrayRef<A> ref = this._helper;
		if (ref != null && ref.len >= len) {
			b = (T) ref.get();
		}
		if (b == null && allowMod) {
			// no helper is available and 
			// re-arranging array is allowed
			x = index + _normalize(a, aLen, s, i, n);
			sorter.sort(a, x, x + len, comparator);
			return;
		}
		if (b == null) {
			b = (T) getHelperArray(len);
		}
		// use new temp array
		_read(a, aLen, s, i, n, index, b, 0, len);
		sorter.sort(b, 0, len, comparator);
		_write(a, aLen, s, i, n, index, b, 0, len);
	}

	protected final <T, B, E extends RuntimeException, X extends Throwable> void _sortArrayChecked(T a, int aLen, int s, int i, int n,
			int index, int len, boolean allowMod, B comparator, CheckedSorter<T, B, X> sorter, Supplier<E> sup,
			A clear, int clearLen) throws E, X {
		int x = inPlaceIndexForSort(aLen, s, i, n, index, len);
		if (x >= 0) {
			// array can be sorted without re-arranging elements or 
			// using additional storage of any kind.
			testBounds(aLen, x, len, sup);
			sorter.sort(a, x, x + len, comparator);
			return;
		}
		// range is broken up
		x = gapIndexForSort(aLen, s, i, n, len);
		if (x >= 0) {
			// range can be sorted using gap for temp storage
			testBounds(aLen, x, len, sup);
			try {
				_read(a, aLen, s, i, n, index, a, x, len);
			} catch (ArrayIndexOutOfBoundsException ex) {
				throw sup.get();
			}
			// copied range into gap
			sorter.sort(a, x, x + len, comparator);
			_write(a, aLen, s, i, n, index, a, x, len);
			clear(a, x, len, clear, clearLen);
			return;
		}
		T b = null;
		ArrayRef<A> ref = this._helper;
		if (ref != null && ref.len >= len) {
			b = (T) ref.get();
		}
		if (b == null && allowMod) {
			// no helper is available and 
			// re-arranging array is allowed
			try {
				x = index + _normalize(a, aLen, s, i, n, clear, clearLen);
			} catch (ArrayIndexOutOfBoundsException ex) {
				throw sup.get();
			}
			testBounds(aLen, x, len, sup);
			sorter.sort(a, x, x + len, comparator);
			return;
		}
		if (b == null) {
			b = (T) getHelperArray(len);
		}
		// use new temp array
		try {
			_read(a, aLen, s, i, n, index, b, 0, len);
		} catch (ArrayIndexOutOfBoundsException ex) {
			throw sup.get();
		}
		sorter.sort(b, 0, len, comparator);
		_write(a, aLen, s, i, n, index, b, 0, len);
	}

	protected final <T, B, E extends RuntimeException, X extends Throwable> void _sortArrayChecked(T a, int aLen, int s, int i, int n,
			int index, int len, boolean allowMod, B comparator, CheckedSorter<T, B, X> sorter, Supplier<E> sup) throws E, X {
		int x = inPlaceIndexForSort(aLen, s, i, n, index, len);
		if (x >= 0) {
			// array can be sorted without re-arranging elements or 
			// using additional storage of any kind.
			testBounds(aLen, x, len, sup);
			sorter.sort(a, x, x + len, comparator);
			return;
		}
		// range is broken up
		x = gapIndexForSort(aLen, s, i, n, len);
		if (x >= 0) {
			// range can be sorted using gap for temp storage
			testBounds(aLen, x, len, sup);
			try {
				_read(a, aLen, s, i, n, index, a, x, len);
			} catch (ArrayIndexOutOfBoundsException ex) {
				throw sup.get();
			}
			// copied range into gap
			sorter.sort(a, x, x + len, comparator);
			_write(a, aLen, s, i, n, index, a, x, len);
			return;
		}
		T b = null;
		ArrayRef<A> ref = this._helper;
		if (ref != null && ref.len >= len) {
			b = (T) ref.get();
		}
		if (b == null && allowMod) {
			// no helper is available and 
			// re-arranging array is allowed
			try {
				x = index + _normalize(a, aLen, s, i, n);
			} catch (ArrayIndexOutOfBoundsException ex) {
				throw sup.get();
			}
			testBounds(aLen, x, len, sup);
			sorter.sort(a, x, x + len, comparator);
			return;
		}
		if (b == null) {
			b = (T) getHelperArray(len);
		}
		// use new temp array
		try {
			_read(a, aLen, s, i, n, index, b, 0, len);
		} catch (ArrayIndexOutOfBoundsException ex) {
			throw sup.get();
		}
		sorter.sort(b, 0, len, comparator);
		_write(a, aLen, s, i, n, index, b, 0, len);
	}

	protected final <T, B, X extends Throwable> void _sortArrayChecked(T a, int aLen, int s, int i, int n,
			int index, int len, boolean allowMod, B comparator, CheckedSorter<T, B, X> sorter,
			A clear, int clearLen) throws ArrayIndexOutOfBoundsException, X {
		int x = inPlaceIndexForSort(aLen, s, i, n, index, len);
		if (x >= 0) {
			// array can be sorted without re-arranging elements or 
			// using additional storage of any kind.
			sorter.sort(a, x, x + len, comparator);
			return;
		}
		// range is broken up
		x = gapIndexForSort(aLen, s, i, n, len);
		if (x >= 0) {
			// range can be sorted using gap for temp storage
			_read(a, aLen, s, i, n, index, a, x, len);
			// copied range into gap
			sorter.sort(a, x, x + len, comparator);
			_write(a, aLen, s, i, n, index, a, x, len);
			clear(a, x, len, clear, clearLen);
			return;
		}
		T b = null;
		ArrayRef<A> ref = this._helper;
		if (ref != null && ref.len >= len) {
			b = (T) ref.get();
		}
		if (b == null && allowMod) {
			// no helper is available and 
			// re-arranging array is allowed
			x = index + _normalize(a, aLen, s, i, n, clear, clearLen);
			sorter.sort(a, x, x + len, comparator);
			return;
		}
		if (b == null) {
			b = (T) getHelperArray(len);
		}
		// use new temp array
		_read(a, aLen, s, i, n, index, b, 0, len);
		sorter.sort(b, 0, len, comparator);
		_write(a, aLen, s, i, n, index, b, 0, len);
	}

	protected final <T, B, X extends Throwable> void _sortArrayChecked(T a, int aLen, int s, int i, int n,
			int index, int len, boolean allowMod, B comparator, CheckedSorter<T, B, X> sorter) throws ArrayIndexOutOfBoundsException, X {
		int x = inPlaceIndexForSort(aLen, s, i, n, index, len);
		if (x >= 0) {
			// array can be sorted without re-arranging elements or 
			// using additional storage of any kind.
			sorter.sort(a, x, x + len, comparator);
			return;
		}
		// range is broken up
		x = gapIndexForSort(aLen, s, i, n, len);
		if (x >= 0) {
			// range can be sorted using gap for temp storage
			_read(a, aLen, s, i, n, index, a, x, len);
			// copied range into gap
			sorter.sort(a, x, x + len, comparator);
			_write(a, aLen, s, i, n, index, a, x, len);
			return;
		}
		T b = null;
		ArrayRef<A> ref = this._helper;
		if (ref != null && ref.len >= len) {
			b = (T) ref.get();
		}
		if (b == null && allowMod) {
			// no helper is available and 
			// re-arranging array is allowed
			x = index + _normalize(a, aLen, s, i, n);
			sorter.sort(a, x, x + len, comparator);
			return;
		}
		if (b == null) {
			b = (T) getHelperArray(len);
		}
		// use new temp array
		_read(a, aLen, s, i, n, index, b, 0, len);
		sorter.sort(b, 0, len, comparator);
		_write(a, aLen, s, i, n, index, b, 0, len);
	}

	private static int gapIndexForSort(int aLen, int s, int i, int n, int len) {
		int g = aLen - s;
		if (g < len) {
			// gap insufficient
			return -1;
		}
		int gapOff = i + n;
		int p = gapOff - aLen;
		if (p >= 0) {
			// gap is unbroken (starts at p)
			// gap is sufficient
			return p;
		}
		int q = gapOff - s;
		if (q >= 0) {
			// gap is broken up
			if (-p >= len) {
				// partial gap on right is sufficient
				return gapOff;
			}
			if (q >= len) {
				// partial gap on left is sufficient
				return 0;
			}
			// gap insufficient
			return -1;
		}
		// gap is unbroken
		// gap is sufficient
		return gapOff;
	}

	private static int inPlaceIndexForSort(int aLen, int s, int i, int n, int index, int len) {
		int gapOff = i + n;
		int p = gapOff - aLen;
		if (index < n) {
			// target head
			int r = n - index;
			if (len > r) {
				// range bridges gap
				return -1;
			}
			// range confined to head
			if (p >= 0) {

				int x = aLen - i;
				if (index < x) {

					int y = x - index;
					if (len > y) {
						return -1;
					}
					return i + index;
				}
				return index - x;
			}
			return i + index;
		}
		// target tail
		int x = i + index - s;
		int q = gapOff - s;
		if (q >= 0) {
			return x;
		}
		if (x < 0) {
			if (len > -x) {
				return -1;
			}
			return aLen + x;
		}
		return x;
	}

	private A getHelperArray(int len) {
		ArrayRef<A> ref = this._helper;
		if (ref != null) {
			A a = ref.get();
			if (ref.len >= len) {
				// size is sufficient
				if (a != null) {
					// ref not stale
					return a;
				}
				// ref is stale
			}
			// size is insufficient
			if (a != null) {
				// access appears to be frequent. grow helper in size
				int helperLen = ref.len + (ref.len >>> 1);
				if (helperLen < 0) {
					// note: integer overflow is not possible if called by normalize().
					helperLen = len > MAX_ARRAY_CAPACITY ? Integer.MAX_VALUE : MAX_ARRAY_CAPACITY;
				}
				if (helperLen < len) {
					helperLen = len;
				}
				A b = _newArray(helperLen);
				this._helper = new ArrayRef<>(b, helperLen);
				return b;
			}
		}
		// ref is stale or size is insufficient
		A b = _newArray(len);
		this._helper = new ArrayRef<>(b, len);
		return b;
	}

	// removes all elements with indices contained in the bitset (offset by "index")
	// efficiently counts sequential bits and always removes
	// as many elements in bulk as possible
	// returns number of elements removed
	protected final int _remove(A a, int aLen, int s, int i, int n, int index, long[] bitset, A clear, int clearLen) throws ArrayIndexOutOfBoundsException {
		int sOld = s;
		int start = 0;
		int range = 0;
		for (int wordIndex = 0; wordIndex < bitset.length; wordIndex++) {
			long word = bitset[wordIndex];
			for (int bits = 64; bits != 0;) {
				// while bits != 0, find number of trailing 0s or 1s in
				// log2(bits) iterations.
				int h = bits;
				while (true) {
					long m = -1L >>> -h;	// -1L >>> (64 - h)
					long x = word & m;
					if (x == 0L) {
						// next h bits are 0s
						if (range != 0) {
							// end current run
							_remove(a, aLen, s, i, n, index + start, range, clear, clearLen);
							s = this._size;
							i = this._idx;
							n = this._len;
							index -= range;	// adjust for next removal
							range = 0;
						}
						// always stop at the latest when h == 1
						break;
					}
					if (x == m) {
						// next h bits are 1s
						if (range == 0) {
							// start a new run
							start = (wordIndex << 6) + 64 - bits;
						}
						range += h;
						// always stop at the latest when h == 1
						break;
					}
					// mixed trailing bits.
					// decrease match length.
					h -= h >>> 1;
				}
				// consume the matched bits
				word >>>= h;
				bits -= h;
			}
		}
		if (range != 0) {
			// remove remaining run
			_remove(a, aLen, s, i, n, index + start, range, clear, clearLen);
			s = this._size;
		}
		return sOld - s;
	}

	// doesn't clear
	protected final int _remove(A a, int aLen, int s, int i, int n, int index, long[] bitset) throws ArrayIndexOutOfBoundsException {
		int sOld = s;
		int start = 0;
		int range = 0;
		for (int wordIndex = 0; wordIndex < bitset.length; wordIndex++) {
			long word = bitset[wordIndex];
			for (int bits = 64; bits != 0;) {
				// while bits != 0, find number of trailing 0s or 1s in
				// log2(bits) iterations.
				int h = bits;
				while (true) {
					long m = -1L >>> -h;	// -1L >>> (64 - h)
					long x = word & m;
					if (x == 0L) {
						// next h bits are 0s
						if (range != 0) {
							// end current run
							_remove(a, aLen, s, i, n, index + start, range);
							s = this._size;
							i = this._idx;
							n = this._len;
							index -= range;	// adjust for next removal
							range = 0;
						}
						// always stop at the latest when h == 1
						break;
					}
					if (x == m) {
						// next h bits are 1s
						if (range == 0) {
							// start a new run
							start = (wordIndex << 6) + 64 - bits;
						}
						range += h;
						// always stop at the latest when h == 1
						break;
					}
					// mixed trailing bits.
					// decrease match length.
					h -= h >>> 1;
				}
				// consume the matched bits
				word >>>= h;
				bits -= h;
			}
		}
		if (range != 0) {
			// remove remaining run
			_remove(a, aLen, s, i, n, index + start, range);
			s = this._size;
		}
		return sOld - s;
	}

	private static <A, B, C> int _limited(A a, B b, C attachment, LimitedTransfer<A, B, C> t,
			int aOff1, int bOff1, int len1,
			int aOff2, int bOff2, int len2,
			int aOff3, int bOff3, int len3) {
		int k;
		if ((k = t.transfer(a, aOff1, b, bOff1, len1, attachment)) != len1) {
			return k;
		}
		if ((k = t.transfer(a, aOff2, b, bOff2, len2, attachment)) != len2) {
			return k + len1;
		}
		return t.transfer(a, aOff3, b, bOff3, len3, attachment) + len1 + len2;
	}

	private static <A, B, C> int _limited(A a, B b, C attachment, LimitedTransfer<A, B, C> t,
			int aOff1, int bOff1, int len1,
			int aOff2, int bOff2, int len2) {
		int k;
		if ((k = t.transfer(a, aOff1, b, bOff1, len1, attachment)) != len1) {
			return k;
		}
		return t.transfer(a, aOff2, b, bOff2, len2, attachment) + len1;
	}

	private static <A, B, C> int _limitedBackwards(A a, B b, C attachment, LimitedTransfer<A, B, C> t,
			int aOff1, int bOff1, int len1,
			int aOff2, int bOff2, int len2,
			int aOff3, int bOff3, int len3) {
		int k;
		if ((k = t.transfer(a, aOff3, b, bOff3, len3, attachment)) != len3) {
			return k + len1 + len2;
		}
		if ((k = t.transfer(a, aOff2, b, bOff2, len2, attachment)) != len2) {
			return k + len1;
		}
		return t.transfer(a, aOff1, b, bOff1, len1, attachment);
	}

	private static <A, B, C> int _limitedBackwards(A a, B b, C attachment, LimitedTransfer<A, B, C> t,
			int aOff1, int bOff1, int len1,
			int aOff2, int bOff2, int len2) {
		int k;
		if ((k = t.transfer(a, aOff2, b, bOff2, len2, attachment)) != len2) {
			return k + len1;
		}
		return t.transfer(a, aOff1, b, bOff1, len1, attachment);
	}

	private static <A, B, C, X extends Throwable> int _checkedLimited(A a, B b, C attachment, CheckedLimitedTransfer<A, B, C, X> t,
			int aOff1, int bOff1, int len1,
			int aOff2, int bOff2, int len2,
			int aOff3, int bOff3, int len3) throws X {
		int k;
		if ((k = t.transfer(a, aOff1, b, bOff1, len1, attachment)) != len1) {
			return k;
		}
		if ((k = t.transfer(a, aOff2, b, bOff2, len2, attachment)) != len2) {
			return k + len1;
		}
		return t.transfer(a, aOff3, b, bOff3, len3, attachment) + len1 + len2;
	}

	private static <A, B, C, X extends Throwable> int _checkedLimited(A a, B b, C attachment, CheckedLimitedTransfer<A, B, C, X> t,
			int aOff1, int bOff1, int len1,
			int aOff2, int bOff2, int len2) throws X {
		int k;
		if ((k = t.transfer(a, aOff1, b, bOff1, len1, attachment)) != len1) {
			return k;
		}
		return t.transfer(a, aOff2, b, bOff2, len2, attachment) + len1;
	}

	private static <A, B, C, X extends Throwable> int _checkedLimitedBackwards(A a, B b, C attachment, CheckedLimitedTransfer<A, B, C, X> t,
			int aOff1, int bOff1, int len1,
			int aOff2, int bOff2, int len2,
			int aOff3, int bOff3, int len3) throws X {
		int k;
		if ((k = t.transfer(a, aOff3, b, bOff3, len3, attachment)) != len3) {
			return k + len1 + len2;
		}
		if ((k = t.transfer(a, aOff2, b, bOff2, len2, attachment)) != len2) {
			return k + len1;
		}
		return t.transfer(a, aOff1, b, bOff1, len1, attachment);
	}

	private static <A, B, C, X extends Throwable> int _checkedLimitedBackwards(A a, B b, C attachment, CheckedLimitedTransfer<A, B, C, X> t,
			int aOff1, int offB1, int len1,
			int aOff2, int offB2, int len2) throws X {
		int k;
		if ((k = t.transfer(a, aOff2, b, offB2, len2, attachment)) != len2) {
			return k + len1;
		}
		return t.transfer(a, aOff1, b, offB1, len1, attachment);
	}

	private static <A, B> int _indexOf(A a, boolean physical, Evaluator<A, B> ev, B attachment1, boolean attachment2,
			int off1, int len1,
			int off2, int len2,
			int off3, int len3) {
		int k;
		if ((k = ev.apply(a, off1, len1, attachment1, attachment2)) >= 0) {
			return physical ? k : k - off1;
		}
		if ((k = ev.apply(a, off2, len2, attachment1, attachment2)) >= 0) {
			return physical ? k : k - off2 + len1;
		}
		if ((k = ev.apply(a, off3, len3, attachment1, attachment2)) >= 0) {
			return physical ? k : k - off3 + len1 + len2;
		}
		return -1;
	}

	private static <A, B> int _indexOf(A a, boolean physical, Evaluator<A, B> ev, B attachment1, boolean attachment2,
			int off1, int len1,
			int off2, int len2) {
		int k;
		if ((k = ev.apply(a, off1, len1, attachment1, attachment2)) >= 0) {
			return physical ? k : k - off1;
		}
		if ((k = ev.apply(a, off2, len2, attachment1, attachment2)) >= 0) {
			return physical ? k : k - off2 + len1;
		}
		return -1;
	}

	private static <A, B> int _indexOfBackwards(A a, boolean physical, Evaluator<A, B> ev, B attachment1, boolean attachment2,
			int off1, int len1,
			int off2, int len2,
			int off3, int len3) {
		int k;
		if ((k = ev.apply(a, off3, len3, attachment1, attachment2)) >= 0) {
			return physical ? k : k - off3 + len1 + len2;
		}
		if ((k = ev.apply(a, off2, len2, attachment1, attachment2)) >= 0) {
			return physical ? k : k - off2 + len1;
		}
		if ((k = ev.apply(a, off1, len1, attachment1, attachment2)) >= 0) {
			return physical ? k : k - off1;
		}
		return -1;
	}

	private static <A, B> int _indexOfBackwards(A a, boolean physical, Evaluator<A, B> ev, B attachment1, boolean attachment2,
			int off1, int len1,
			int off2, int len2) {
		int k;
		if ((k = ev.apply(a, off2, len2, attachment1, attachment2)) >= 0) {
			return physical ? k : k - off2 + len1;
		}
		if ((k = ev.apply(a, off1, len1, attachment1, attachment2)) >= 0) {
			return physical ? k : k - off1;
		}
		return -1;
	}

	private static <A, B, X extends Throwable> int _checkedIndexOf(A a, boolean physical, CheckedEvaluator<A, B, X> ev, B attachment1, boolean attachment2,
			int off1, int len1,
			int off2, int len2,
			int off3, int len3) throws X {
		int k;
		if ((k = ev.apply(a, off1, len1, attachment1, attachment2)) >= 0) {
			return physical ? k : k - off1;
		}
		if ((k = ev.apply(a, off2, len2, attachment1, attachment2)) >= 0) {
			return physical ? k : k - off2 + len1;
		}
		if ((k = ev.apply(a, off3, len3, attachment1, attachment2)) >= 0) {
			return physical ? k : k - off3 + len1 + len2;
		}
		return -1;
	}

	private static <A, B, X extends Throwable> int _checkedIndexOf(A a, boolean physical, CheckedEvaluator<A, B, X> ev, B attachment1, boolean attachment2,
			int off1, int len1,
			int off2, int len2) throws X {
		int k;
		if ((k = ev.apply(a, off1, len1, attachment1, attachment2)) >= 0) {
			return physical ? k : k - off1;
		}
		if ((k = ev.apply(a, off2, len2, attachment1, attachment2)) >= 0) {
			return physical ? k : k - off2 + len1;
		}
		return -1;
	}

	private static <A, B, X extends Throwable> int _checkedIndexOfBackwards(A a, boolean physical, CheckedEvaluator<A, B, X> ev, B attachment1, boolean attachment2,
			int off1, int len1,
			int off2, int len2,
			int off3, int len3) throws X {
		int k;
		if ((k = ev.apply(a, off3, len3, attachment1, attachment2)) >= 0) {
			return physical ? k : k - off3 + len1 + len2;
		}
		if ((k = ev.apply(a, off2, len2, attachment1, attachment2)) >= 0) {
			return physical ? k : k - off2 + len1;
		}
		if ((k = ev.apply(a, off1, len1, attachment1, attachment2)) >= 0) {
			return physical ? k : k - off1;
		}
		return -1;
	}

	private static <A, B, X extends Throwable> int _checkedIndexOfBackwards(A a, boolean physical, CheckedEvaluator<A, B, X> ev, B attachment1, boolean attachment2,
			int off1, int len1,
			int off2, int len2) throws X {
		int k;
		if ((k = ev.apply(a, off2, len2, attachment1, attachment2)) >= 0) {
			return physical ? k : k - off2 + len1;
		}
		if ((k = ev.apply(a, off1, len1, attachment1, attachment2)) >= 0) {
			return physical ? k : k - off1;
		}
		return -1;
	}

	private static <A> int _accumulate(A a, int initialValue, IntAccumulator<A> accum,
			int off1, int len1,
			int off2, int len2,
			int off3, int len3) {
		initialValue = accum.accumulate(a, off1, len1, initialValue);
		initialValue = accum.accumulate(a, off2, len2, initialValue);
		return accum.accumulate(a, off3, len3, initialValue);
	}

	private static <A> int _accumulate(A a, int initialValue, IntAccumulator<A> accum,
			int off1, int len1,
			int off2, int len2) {
		initialValue = accum.accumulate(a, off1, len1, initialValue);
		return accum.accumulate(a, off2, len2, initialValue);
	}

	private static <A> int _accumulateBackwards(A a, int initialValue, IntAccumulator<A> accum,
			int off1, int len1,
			int off2, int len2,
			int off3, int len3) {
		initialValue = accum.accumulate(a, off3, len3, initialValue);
		initialValue = accum.accumulate(a, off2, len2, initialValue);
		return accum.accumulate(a, off1, len1, initialValue);
	}

	private static <A> int _accumulateBackwards(A a, int initialValue, IntAccumulator<A> accum,
			int off1, int len1,
			int off2, int len2) {
		initialValue = accum.accumulate(a, off2, len2, initialValue);
		return accum.accumulate(a, off1, len1, initialValue);
	}

	private static <A, X extends Throwable> int _checkedAccumulate(A a, int initialValue, CheckedIntAccumulator<A, X> accum,
			int off1, int len1,
			int off2, int len2,
			int off3, int len3) throws X {
		initialValue = accum.accumulate(a, off1, len1, initialValue);
		initialValue = accum.accumulate(a, off2, len2, initialValue);
		return accum.accumulate(a, off3, len3, initialValue);
	}

	private static <A, X extends Throwable> int _checkedAccumulate(A a, int initialValue, CheckedIntAccumulator<A, X> accum,
			int off1, int len1,
			int off2, int len2) throws X {
		initialValue = accum.accumulate(a, off1, len1, initialValue);
		return accum.accumulate(a, off2, len2, initialValue);
	}

	private static <A, X extends Throwable> int _checkedAccumulateBackwards(A a, int initialValue, CheckedIntAccumulator<A, X> accum,
			int off1, int len1,
			int off2, int len2,
			int off3, int len3) throws X {
		initialValue = accum.accumulate(a, off3, len3, initialValue);
		initialValue = accum.accumulate(a, off2, len2, initialValue);
		return accum.accumulate(a, off1, len1, initialValue);
	}

	private static <A, X extends Throwable> int _checkedAccumulateBackwards(A a, int initialValue, CheckedIntAccumulator<A, X> accum,
			int off1, int len1,
			int off2, int len2) throws X {
		initialValue = accum.accumulate(a, off2, len2, initialValue);
		return accum.accumulate(a, off1, len1, initialValue);
	}

	private static <A> long _accumulate(A a, long initialValue, LongAccumulator<A> accum,
			int off1, int len1,
			int off2, int len2,
			int off3, int len3) {
		initialValue = accum.accumulate(a, off1, len1, initialValue);
		initialValue = accum.accumulate(a, off2, len2, initialValue);
		return accum.accumulate(a, off3, len3, initialValue);
	}

	private static <A> long _accumulate(A a, long initialValue, LongAccumulator<A> accum,
			int off1, int len1,
			int off2, int len2) {
		initialValue = accum.accumulate(a, off1, len1, initialValue);
		return accum.accumulate(a, off2, len2, initialValue);
	}

	private static <A> long _accumulateBackwards(A a, long initialValue, LongAccumulator<A> accum,
			int off1, int len1,
			int off2, int len2,
			int off3, int len3) {
		initialValue = accum.accumulate(a, off3, len3, initialValue);
		initialValue = accum.accumulate(a, off2, len2, initialValue);
		return accum.accumulate(a, off1, len1, initialValue);
	}

	private static <A> long _accumulateBackwards(A a, long initialValue, LongAccumulator<A> accum,
			int off1, int len1,
			int off2, int len2) {
		initialValue = accum.accumulate(a, off2, len2, initialValue);
		return accum.accumulate(a, off1, len1, initialValue);
	}

	private static <A, X extends Throwable> long _checkedAccumulate(A a, long initialValue, CheckedLongAccumulator<A, X> accum,
			int off1, int len1,
			int off2, int len2,
			int off3, int len3) throws X {
		initialValue = accum.accumulate(a, off1, len1, initialValue);
		initialValue = accum.accumulate(a, off2, len2, initialValue);
		return accum.accumulate(a, off3, len3, initialValue);
	}

	private static <A, X extends Throwable> long _checkedAccumulate(A a, long initialValue, CheckedLongAccumulator<A, X> accum,
			int off1, int len1,
			int off2, int len2) throws X {
		initialValue = accum.accumulate(a, off1, len1, initialValue);
		return accum.accumulate(a, off2, len2, initialValue);
	}

	private static <A, X extends Throwable> long _checkedAccumulateBackwards(A a, long initialValue, CheckedLongAccumulator<A, X> accum,
			int off1, int len1,
			int off2, int len2,
			int off3, int len3) throws X {
		initialValue = accum.accumulate(a, off3, len3, initialValue);
		initialValue = accum.accumulate(a, off2, len2, initialValue);
		return accum.accumulate(a, off1, len1, initialValue);
	}

	private static <A, X extends Throwable> long _checkedAccumulateBackwards(A a, long initialValue, CheckedLongAccumulator<A, X> accum,
			int off1, int len1,
			int off2, int len2) throws X {
		initialValue = accum.accumulate(a, off2, len2, initialValue);
		return accum.accumulate(a, off1, len1, initialValue);
	}

	///yyy
	@SuppressWarnings("SuspiciousSystemArraycopy")
	private long _prepareForInsertion(A a, int aLen, int s, int i, int n, int index, int len) throws ArrayIndexOutOfBoundsException {
		// prepare array for insertion
		// this.len must be updated twice in case "b" is "a" or backed by "a", 
		// so that it can still be read from at the moment of insertion.
		this._len = index;
		int gapOff = i + n;
		int p = gapOff - aLen;
		if (index < n) {
			// target head
			int r = n - index;
			int l = s - r;
			if (r > l) {
				// rotate head left
				int q = gapOff - s;
				int g = aLen - s;
				int x = i + index;
				int y = x - g;
				if (p >= 0) {
					this._idx = i - g;
					int z = x - aLen;
					if (z < 0) {
						System.arraycopy(a, q, a, p, l);
						return _toCopyBounds(y, len);
					}
					int tail = s - p;
					System.arraycopy(a, q, a, p, tail);
					int u = y - aLen;
					if (z > g) {
						System.arraycopy(a, 0, a, s, g);
						System.arraycopy(a, g, a, 0, u);
						return _toCopyBounds(u, len);
					}
					System.arraycopy(a, 0, a, s, z);
					int v = u + len;
					return _toCopyBounds(y, v < 0 ? len : -u);
				}
				if (q >= 0) {
					this._idx = g > i ? i + s : i - g;
					if (y < 0) {
						System.arraycopy(a, q, a, gapOff, l);
						int z = y + len;
						return _toCopyBounds(x + s, z < 0 ? len : -y);
					}
					System.arraycopy(a, q, a, gapOff, -p);
					System.arraycopy(a, g, a, 0, y);
					return _toCopyBounds(y, len);
				}
				int tailOff = gapOff + g;
				System.arraycopy(a, tailOff, a, gapOff, -q);
				if (y < 0) {
					this._idx = i + s;
					System.arraycopy(a, 0, a, s, x);
					int z = x + s;
					int u = y + len;
					return _toCopyBounds(z, u < 0 ? len : -y);
				}
				this._idx = g > i ? i + s : i - g;
				System.arraycopy(a, 0, a, s, g);
				System.arraycopy(a, g, a, 0, y);
				return _toCopyBounds(y, len);
			}
			// rotate head right
			int x = i + index;
			if (p >= 0) {
				int y = x - aLen;
				if (y < 0) {
					int g = aLen - s;
					System.arraycopy(a, 0, a, g, p);
					if (x < s) {
						System.arraycopy(a, s, a, 0, g);
						System.arraycopy(a, x, a, x + g, s - x);
						return _toCopyBounds(x, len);
					}
					int gapLeft = x - s;
					System.arraycopy(a, x, a, gapLeft, -y);
					int z = y + len;
					return _toCopyBounds(x, z < 0 ? len : -y);
				}
				System.arraycopy(a, y, a, x - s, r);
				return _toCopyBounds(y, len);
			}
			int q = gapOff - s;
			if (q >= 0) {
				if (x < s) {
					int g = aLen - s;
					System.arraycopy(a, s, a, 0, q);
					System.arraycopy(a, x, a, x + g, s - x);
					return _toCopyBounds(x, len);
				}
				System.arraycopy(a, x, a, x - s, r);
				int gapRight = aLen - x;
				return _toCopyBounds(x, len > gapRight ? gapRight : len);
			}
			int g = aLen - s;
			System.arraycopy(a, x, a, x + g, r);
			return _toCopyBounds(x, len);
		}
		// target tail
		int l = index - n;
		int r = s - l;
		if (r < l) {
			// rotate tail right
			int x = i + index;
			int y = x - s;	// q + l
			if (p >= 0) {
				int g = aLen - s;
				System.arraycopy(a, 0, a, g, p);
				int tailR = aLen - y;
				if (tailR > g) {
					this._idx = s > i ? i + g : i - s;
					System.arraycopy(a, s, a, 0, g);
					System.arraycopy(a, y, a, y + g, tailR - g);
					return _toCopyBounds(y, len);
				}
				this._idx = i - s;
				int headR = g - tailR;
				System.arraycopy(a, y, a, headR, tailR);
				return _toCopyBounds(y, len > tailR ? tailR : len);
			}
			int q = gapOff - s;
			if (q >= 0) {
				if (y < s) {
					int g = aLen - s;
					this._idx = s > i ? i + g : i - s;
					System.arraycopy(a, s, a, 0, q);
					System.arraycopy(a, y, a, y + g, s - y);
					return _toCopyBounds(y, len);
				}
				this._idx = i - s;
				System.arraycopy(a, y, a, y - s, r);
				int gapRight = aLen - y;
				return _toCopyBounds(y, len > gapRight ? gapRight : len);
			}
			int g = aLen - s;
			this._idx = i + g;
			if (y < 0) {
				System.arraycopy(a, 0, a, g, gapOff);
				int z = x + g;
				if (-y > g) {
					System.arraycopy(a, s, a, 0, g);
					System.arraycopy(a, z, a, z + g, s - z);
					return _toCopyBounds(z, len);
				}
				System.arraycopy(a, z, a, g + y, -y);
				return _toCopyBounds(z, len > -y ? -y : len);
			}
			System.arraycopy(a, y, a, g + y, r);
			return _toCopyBounds(y, len);
		}
		// rotate tail left
		int q = gapOff - s;
		int x = i + index;
		if (p >= 0) {
			int y = x - aLen;
			System.arraycopy(a, q, a, p, l);
			return _toCopyBounds(y, len);
		}
		if (q >= 0) {
			int y = x - aLen;
			if (y < 0) {
				System.arraycopy(a, q, a, gapOff, l);
				return _toCopyBounds(x, len > -y ? -y : len);
			}
			int g = aLen - s;
			System.arraycopy(a, q, a, gapOff, -p);
			System.arraycopy(a, g, a, 0, y);
			return _toCopyBounds(y, len);
		}
		int g = aLen - s;
		int tailOff = gapOff + g;
		int y = x - s;
		if (y < 0) {
			System.arraycopy(a, tailOff, a, gapOff, l);
			return _toCopyBounds(x, len);
		}
		System.arraycopy(a, tailOff, a, gapOff, -q);
		int z = x - aLen;
		if (z < 0) {
			System.arraycopy(a, 0, a, s, y);
			int u = z + len;
			return _toCopyBounds(x, u < 0 ? len : -z);
		}
		System.arraycopy(a, 0, a, s, g);
		System.arraycopy(a, g, a, 0, z);
		return _toCopyBounds(z, len);
	}

	@SuppressWarnings("SuspiciousSystemArraycopy")
	private long _prepareForInsertion(A a, int aLen, int s, int i, int n, int index, int len, A clear, int clearLen) throws ArrayIndexOutOfBoundsException {
		// prepare array for insertion
		// this.len must be updated twice in case "b" is "a" or backed by "a", 
		// so that it can still be read from at the moment of insertion.
		this._len = index;
		int gapOff = i + n;
		int p = gapOff - aLen;
		if (index < n) {
			// target head
			int r = n - index;
			int l = s - r;
			if (r > l) {
				// rotate head left
				int q = gapOff - s;
				int g = aLen - s;
				int x = i + index;
				int y = x - g;
				if (p >= 0) {
					this._idx = i - g;
					int z = x - aLen;
					if (z < 0) {
						System.arraycopy(a, q, a, p, l);
						int newGap = g - len;
						if (newGap < l) {
							clear(a, y + len, newGap, clear, clearLen);
						} else {
							clear(a, q, l, clear, clearLen);
						}
						return _toCopyBounds(y, len);
					}
					int tail = s - p;
					System.arraycopy(a, q, a, p, tail);
					int u = y - aLen;
					if (z > g) {
						System.arraycopy(a, 0, a, s, g);
						System.arraycopy(a, g, a, 0, u);
						clear(a, u + len, g - len, clear, clearLen);
						return _toCopyBounds(u, len);
					}
					System.arraycopy(a, 0, a, s, z);
					int v = u + len;
					if (v < 0) {
						clear(a, 0, z, clear, clearLen);
						if (-v < tail) {
							clear(a, y + len, -v, clear, clearLen);
						} else {
							clear(a, q, tail, clear, clearLen);
						}
						return _toCopyBounds(y, len);
					}
					clear(a, v, g - len, clear, clearLen);
					return _toCopyBounds(y, -u);
				}
				if (q >= 0) {
					this._idx = g > i ? i + s : i - g;
					if (y < 0) {
						System.arraycopy(a, q, a, gapOff, l);
						int z = y + len;
						if (z < 0) {
							clear(a, q, l, clear, clearLen);
							return _toCopyBounds(x + s, len);
						}
						int newGap = g - len;
						if (newGap < l) {
							clear(a, z, newGap, clear, clearLen);
						} else {
							clear(a, q, l, clear, clearLen);
						}
						return _toCopyBounds(x + s, -y);
					}
					System.arraycopy(a, q, a, gapOff, -p);
					System.arraycopy(a, g, a, 0, y);
					int newGap = g - len;
					if (newGap < l) {
						clear(a, y + len, newGap, clear, clearLen);
					} else {
						clear(a, q, l, clear, clearLen);
					}
					return _toCopyBounds(y, len);
				}
				int tailOff = gapOff + g;
				System.arraycopy(a, tailOff, a, gapOff, -q);
				if (y < 0) {
					this._idx = i + s;
					System.arraycopy(a, 0, a, s, x);
					int z = x + s;
					int u = y + len;
					if (u < 0) {
						clear(a, 0, x, clear, clearLen);
						if (u > q) {
							clear(a, z + len, -u, clear, clearLen);
						} else {
							clear(a, tailOff, -q, clear, clearLen);
						}
						return _toCopyBounds(z, len);
					}
					clear(a, u, g - len, clear, clearLen);
					return _toCopyBounds(z, -y);
				}
				this._idx = g > i ? i + s : i - g;
				System.arraycopy(a, 0, a, s, g);
				System.arraycopy(a, g, a, 0, y);
				clear(a, y + len, g - len, clear, clearLen);
				return _toCopyBounds(y, len);
			}
			// rotate head right
			int x = i + index;
			if (p >= 0) {
				int y = x - aLen;
				if (y < 0) {
					int g = aLen - s;
					System.arraycopy(a, 0, a, g, p);
					if (x < s) {
						System.arraycopy(a, s, a, 0, g);
						System.arraycopy(a, x, a, x + g, s - x);
						clear(a, x + len, g - len, clear, clearLen);
						return _toCopyBounds(x, len);
					}
					int gapLeft = x - s;
					System.arraycopy(a, x, a, gapLeft, -y);
					int z = y + len;
					if (z < 0) {
						clear(a, x + len, -z, clear, clearLen);
						clear(a, 0, p < gapLeft ? p : gapLeft, clear, clearLen);
						return _toCopyBounds(x, len);
					}
					//if (len < r) {
					clear(a, z, (g < r ? g : r) - len, clear, clearLen);
					//}
					return _toCopyBounds(x, -y);
				}
				System.arraycopy(a, y, a, x - s, r);
				//if (len < r) {
				int g = aLen - s;
				clear(a, y + len, (g < r ? g : r) - len, clear, clearLen);
				//}
				return _toCopyBounds(y, len);
			}
			int q = gapOff - s;
			if (q >= 0) {
				if (x < s) {
					int g = aLen - s;
					System.arraycopy(a, s, a, 0, q);
					System.arraycopy(a, x, a, x + g, s - x);
					//if (len < r) {
					clear(a, x + len, (g < r ? g : r) - len, clear, clearLen);
					//}
					return _toCopyBounds(x, len);
				}
				System.arraycopy(a, x, a, x - s, r);
				int gapRight = aLen - x;
				if (len > gapRight) {
					return _toCopyBounds(x, gapRight);
				}
				//if (len < r) {
				clear(a, x + len, r - len, clear, clearLen);
				//}
				return _toCopyBounds(x, len);
			}
			int g = aLen - s;
			System.arraycopy(a, x, a, x + g, r);
			//if (len < r) {
			clear(a, x + len, (g < r ? g : r) - len, clear, clearLen);
			//}
			return _toCopyBounds(x, len);
		}
		// target tail
		int l = index - n;
		int r = s - l;
		if (r < l) {
			// rotate tail right
			int x = i + index;
			int y = x - s;	// q + l
			if (p >= 0) {
				int g = aLen - s;
				System.arraycopy(a, 0, a, g, p);
				int tailR = aLen - y;
				if (tailR > g) {
					this._idx = s > i ? i + g : i - s;
					System.arraycopy(a, s, a, 0, g);
					System.arraycopy(a, y, a, y + g, tailR - g);
					clear(a, y + len, g - len, clear, clearLen);
					return _toCopyBounds(y, len);
				}
				this._idx = i - s;
				int headR = g - tailR;
				System.arraycopy(a, y, a, headR, tailR);
				if (len > tailR) {
					int headB = len - tailR;
					//if (len < r) {
					clear(a, headB, (g < r ? g : r) - len, clear, clearLen);
					//}
					return _toCopyBounds(y, tailR);
				}
				clear(a, y + len, tailR - len, clear, clearLen);
				clear(a, 0, headR < p ? g - tailR : p, clear, clearLen);
				return _toCopyBounds(y, len);
			}
			int q = gapOff - s;
			if (q >= 0) {
				if (y < s) {
					int g = aLen - s;
					this._idx = s > i ? i + g : i - s;
					System.arraycopy(a, s, a, 0, q);
					System.arraycopy(a, y, a, y + g, s - y);
					//if (len < r) {
					clear(a, y + len, (g < r ? g : r) - len, clear, clearLen);
					//}
					return _toCopyBounds(y, len);
				}
				this._idx = i - s;
				System.arraycopy(a, y, a, y - s, r);
				int gapRight = aLen - y;
				if (len > gapRight) {
					return _toCopyBounds(y, gapRight);
				}
				//if (len < r) {
				clear(a, y + len, r - len, clear, clearLen);
				//}
				return _toCopyBounds(y, len);
			}
			int g = aLen - s;
			this._idx = i + g;
			if (y < 0) {
				System.arraycopy(a, 0, a, g, gapOff);
				int z = x + g;
				if (-y > g) {
					System.arraycopy(a, s, a, 0, g);
					System.arraycopy(a, z, a, z + g, s - z);
					clear(a, z + len, g - len, clear, clearLen);
					return _toCopyBounds(z, len);
				}
				System.arraycopy(a, z, a, g + y, -y);
				if (len > -y) {
					int headB = len + y;
					//if (len < r) {
					clear(a, headB, (g < r ? g : r) - len, clear, clearLen);
					//}
					return _toCopyBounds(z, -y);
				}
				clear(a, z + len, -y - len, clear, clearLen);
				clear(a, 0, g < r ? g + y : gapOff, clear, clearLen);
				return _toCopyBounds(z, len);
			}
			System.arraycopy(a, y, a, g + y, r);
			//if (len < r) {
			clear(a, y + len, (g < r ? g : r) - len, clear, clearLen);
			//}
			return _toCopyBounds(y, len);
		}
		// rotate tail left
		int q = gapOff - s;
		int x = i + index;
		if (p >= 0) {
			int y = x - aLen;
			System.arraycopy(a, q, a, p, l);
			int newGap = aLen - s - len;
			if (newGap < l) {
				clear(a, y + len, newGap, clear, clearLen);
			} else {
				clear(a, q, l, clear, clearLen);
			}
			return _toCopyBounds(y, len);
		}
		if (q >= 0) {
			int y = x - aLen;
			if (y < 0) {
				System.arraycopy(a, q, a, gapOff, l);
				if (len > -y) {
					int z = len + y;
					int newGap = aLen - s - len;
					if (newGap < l) {
						clear(a, z, newGap, clear, clearLen);
					} else {
						clear(a, q, l, clear, clearLen);
					}
					return _toCopyBounds(x, -y);
				}
				clear(a, q, l, clear, clearLen);
				return _toCopyBounds(x, len);
			}
			int g = aLen - s;
			System.arraycopy(a, q, a, gapOff, -p);
			System.arraycopy(a, g, a, 0, y);
			int newGap = g - len;
			if (newGap < l) {
				clear(a, y + len, newGap, clear, clearLen);
			} else {
				clear(a, q, l, clear, clearLen);
			}
			return _toCopyBounds(y, len);
		}
		int g = aLen - s;
		int tailOff = gapOff + g;
		int y = x - s;
		if (y < 0) {
			System.arraycopy(a, tailOff, a, gapOff, l);
			int newGap = g - len;
			if (newGap < l) {
				clear(a, x + len, newGap, clear, clearLen);
			} else {
				clear(a, tailOff, l, clear, clearLen);
			}
			return _toCopyBounds(x, len);
		}
		System.arraycopy(a, tailOff, a, gapOff, -q);
		int z = x - aLen;
		if (z < 0) {
			System.arraycopy(a, 0, a, s, y);
			int u = z + len;
			if (u < 0) {
				clear(a, 0, y, clear, clearLen);
				if (u > q) {
					clear(a, x + len, -u, clear, clearLen);
				} else {
					clear(a, tailOff, -q, clear, clearLen);
				}
				return _toCopyBounds(x, len);
			}
			clear(a, u, g - len, clear, clearLen);
			return _toCopyBounds(x, -z);
		}
		System.arraycopy(a, 0, a, s, g);
		System.arraycopy(a, g, a, 0, z);
		clear(a, z + len, g - len, clear, clearLen);
		return _toCopyBounds(z, len);
	}

	// for all inserts: all transfers will receive a as b.
	protected final <B, C> void _insert(A a, int aLen, int s, int i, int n,
			int index, B b, int off, int len, Transfer<B, A, C> t, C attachment) throws ArrayIndexOutOfBoundsException {

		final long copyBounds = _prepareForInsertion(a, aLen, s, i, n, index, len);
		final int copyOff = _copyOff(copyBounds);
		final int copyLen = _copyLen(copyBounds);

		// insert elements (possibly from the underlying array itself)
		t.transfer(b, off, a, copyOff, copyLen, attachment);
		if (copyLen != len) {
			t.transfer(b, off + copyLen, a, 0, len - copyLen, attachment);
		}
		// final adjustment after insertion
		this._len += len;
		this._size += len;
	}

	protected final <B, C> void _insert(A a, int aLen, int s, int i, int n,
			int index, B b, int off, int len, Transfer<B, A, C> t, C attachment, A clear, int clearLen) throws ArrayIndexOutOfBoundsException {

		final long copyBounds = _prepareForInsertion(a, aLen, s, i, n, index, len, clear, clearLen);
		final int copyOff = _copyOff(copyBounds);
		final int copyLen = _copyLen(copyBounds);

		// insert elements (possibly from the underlying array itself)
		t.transfer(b, off, a, copyOff, copyLen, attachment);
		if (copyLen != len) {
			t.transfer(b, off + copyLen, a, 0, len - copyLen, attachment);
		}
		// final adjustment after insertion
		this._len += len;
		this._size += len;
	}

	protected final <B, C, E extends RuntimeException> void _insert(A a, int aLen, int s, int i, int n,
			int index, B b, int off, int len, Transfer<B, A, C> t, C attachment, Supplier<E> sup) throws E {

		final long copyBounds;
		try {
			copyBounds = _prepareForInsertion(a, aLen, s, i, n, index, len);
		} catch (ArrayIndexOutOfBoundsException ex) {
			throw sup.get();
		}

		final int copyOff = _copyOff(copyBounds);
		final int copyLen = _copyLen(copyBounds);
		// insert elements (possibly from the underlying array itself)
		t.transfer(b, off, a, copyOff, copyLen, attachment);
		if (copyLen != len) {
			t.transfer(b, off + copyLen, a, 0, len - copyLen, attachment);
		}
		// final adjustment after insertion
		this._len += len;
		this._size += len;
	}

	protected final <B, C, E extends RuntimeException> void _insert(A a, int aLen, int s, int i, int n,
			int index, B b, int off, int len, Transfer<B, A, C> t, C attachment, Supplier<E> sup, A clear, int clearLen) throws E {

		final long copyBounds;
		try {
			copyBounds = _prepareForInsertion(a, aLen, s, i, n, index, len, clear, clearLen);
		} catch (ArrayIndexOutOfBoundsException ex) {
			throw sup.get();
		}

		final int copyOff = _copyOff(copyBounds);
		final int copyLen = _copyLen(copyBounds);
		// insert elements (possibly from the underlying array itself)
		t.transfer(b, off, a, copyOff, copyLen, attachment);
		if (copyLen != len) {
			t.transfer(b, off + copyLen, a, 0, len - copyLen, attachment);
		}
		// final adjustment after insertion
		this._len += len;
		this._size += len;
	}

	protected final <B, C, X extends Throwable> void _insertChecked(A a, int aLen, int s, int i, int n,
			int index, B b, int off, int len, CheckedTransfer<B, A, C, X> t, C attachment) throws ArrayIndexOutOfBoundsException, X {

		final long copyBounds = _prepareForInsertion(a, aLen, s, i, n, index, len);
		final int copyOff = _copyOff(copyBounds);
		final int copyLen = _copyLen(copyBounds);
		// insert elements (possibly from the underlying array itself)
		t.transfer(b, off, a, copyOff, copyLen, attachment);
		if (copyLen != len) {
			t.transfer(b, off + copyLen, a, 0, len - copyLen, attachment);
		}
		// final adjustment after insertion
		this._len += len;
		this._size += len;
	}

	protected final <B, C, X extends Throwable> void _insertChecked(A a, int aLen, int s, int i, int n,
			int index, B b, int off, int len, CheckedTransfer<B, A, C, X> t, C attachment, A clear, int clearLen) throws ArrayIndexOutOfBoundsException, X {

		final long copyBounds = _prepareForInsertion(a, aLen, s, i, n, index, len, clear, clearLen);
		final int copyOff = _copyOff(copyBounds);
		final int copyLen = _copyLen(copyBounds);
		// insert elements (possibly from the underlying array itself)
		t.transfer(b, off, a, copyOff, copyLen, attachment);
		if (copyLen != len) {
			t.transfer(b, off + copyLen, a, 0, len - copyLen, attachment);
		}
		// final adjustment after insertion
		this._len += len;
		this._size += len;
	}

	protected final <B, C, E extends RuntimeException, X extends Throwable> void _insertChecked(A a, int aLen, int s, int i, int n,
			int index, B b, int off, int len, CheckedTransfer<B, A, C, X> t, C attachment, Supplier<E> sup) throws E, X {

		final long copyBounds;
		try {
			copyBounds = _prepareForInsertion(a, aLen, s, i, n, index, len);
		} catch (ArrayIndexOutOfBoundsException ex) {
			throw sup.get();
		}
		final int copyOff = _copyOff(copyBounds);
		final int copyLen = _copyLen(copyBounds);

		// insert elements (possibly from the underlying array itself)
		t.transfer(b, off, a, copyOff, copyLen, attachment);
		if (copyLen != len) {
			t.transfer(b, off + copyLen, a, 0, len - copyLen, attachment);
		}
		// final adjustment after insertion
		this._len += len;
		this._size += len;
	}

	protected final <B, C, E extends RuntimeException, X extends Throwable> void _insertChecked(A a, int aLen, int s, int i, int n,
			int index, B b, int off, int len, CheckedTransfer<B, A, C, X> t, C attachment, Supplier<E> sup, A clear, int clearLen) throws E, X {

		final long copyBounds;
		try {
			copyBounds = _prepareForInsertion(a, aLen, s, i, n, index, len, clear, clearLen);
		} catch (ArrayIndexOutOfBoundsException ex) {
			throw sup.get();
		}
		final int copyOff = _copyOff(copyBounds);
		final int copyLen = _copyLen(copyBounds);

		// insert elements (possibly from the underlying array itself)
		t.transfer(b, off, a, copyOff, copyLen, attachment);
		if (copyLen != len) {
			t.transfer(b, off + copyLen, a, 0, len - copyLen, attachment);
		}
		// final adjustment after insertion
		this._len += len;
		this._size += len;
	}

	protected final <B> void _insert(A a, int aLen1, int s1, int i1, int n1, int index1,
			AbstractCyclicArray<B> gap, B b, int aLen2, int s2, int index2,
			int len) throws ArrayIndexOutOfBoundsException {

		final long copyBounds = _prepareForInsertion(a, aLen1, s1, i1, n1, index1, len);
		final int copyOff = _copyOff(copyBounds);
		final int copyLen = _copyLen(copyBounds);

		int i2 = gap._idx;	// must re-read value in case a == b
		int n2 = gap._len;	// must re-read value in case a == b
		// insert elements (possibly from the underlying array itself)
		// using arraycopy
		_read(b, aLen2, s2, i2, n2, index2, a, copyOff, copyLen);
		if (copyLen != len) {
			_read(b, aLen2, s2, i2, n2, index2 + copyLen, a, 0, len - copyLen);
		}
		// final adjustment after insertion
		this._len += len;
		this._size += len;
	}

	protected final <B> void _insert(A a, int aLen1, int s1, int i1, int n1, int index1,
			AbstractCyclicArray<B> gap, B b, int aLen2, int s2, int index2,
			int len, A clear, int clearLen) throws ArrayIndexOutOfBoundsException {

		final long copyBounds = _prepareForInsertion(a, aLen1, s1, i1, n1, index1, len, clear, clearLen);
		final int copyOff = _copyOff(copyBounds);
		final int copyLen = _copyLen(copyBounds);

		int i2 = gap._idx;	// must re-read value in case a == b
		int n2 = gap._len;	// must re-read value in case a == b
		// insert elements (possibly from the underlying array itself)
		// using arraycopy
		_read(b, aLen2, s2, i2, n2, index2, a, copyOff, copyLen);
		if (copyLen != len) {
			_read(b, aLen2, s2, i2, n2, index2 + copyLen, a, 0, len - copyLen);
		}
		// final adjustment after insertion
		this._len += len;
		this._size += len;
	}

	protected final <B, C> void _insert(A a, int aLen1, int s1, int i1, int n1, int index1,
			AbstractCyclicArray<B> gap, B b, int aLen2, int s2, int index2,
			int len, Transfer<B, A, C> t, C attachment) throws ArrayIndexOutOfBoundsException {

		final long copyBounds = _prepareForInsertion(a, aLen1, s1, i1, n1, index1, len);
		final int copyOff = _copyOff(copyBounds);
		final int copyLen = _copyLen(copyBounds);

		// insert elements (possibly from the underlying array itself)
		int i2 = gap._idx;	// must re-read value in case a == b
		int n2 = gap._len;	// must re-read value in case a == b
		_read(b, aLen2, s2, i2, n2, index2, a, copyOff, copyLen, t, attachment);
		if (copyLen != len) {
			_read(b, aLen2, s2, i2, n2, index2 + copyLen, a, 0, len - copyLen, t, attachment);
		}
		// final adjustment after insertion
		this._len += len;
		this._size += len;
	}

	protected final <B, C> void _insert(A a, int aLen1, int s1, int i1, int n1, int index1,
			AbstractCyclicArray<B> gap, B b, int aLen2, int s2, int index2,
			int len, Transfer<B, A, C> t, C attachment, A clear, int clearLen) throws ArrayIndexOutOfBoundsException {

		final long copyBounds = _prepareForInsertion(a, aLen1, s1, i1, n1, index1, len, clear, clearLen);
		final int copyOff = _copyOff(copyBounds);
		final int copyLen = _copyLen(copyBounds);

		// insert elements (possibly from the underlying array itself)
		int i2 = gap._idx;	// must re-read value in case a == b
		int n2 = gap._len;	// must re-read value in case a == b
		_read(b, aLen2, s2, i2, n2, index2, a, copyOff, copyLen, t, attachment);
		if (copyLen != len) {
			_read(b, aLen2, s2, i2, n2, index2 + copyLen, a, 0, len - copyLen, t, attachment);
		}
		// final adjustment after insertion
		this._len += len;
		this._size += len;
	}

	protected final <B, C, E extends RuntimeException> void _insert(A a, int aLen1, int s1, int i1, int n1, int index1,
			AbstractCyclicArray<B> gap, B b, int aLen2, int s2, int index2,
			int len, Transfer<B, A, C> t, C attachment, Supplier<E> sup) throws E {

		final long copyBounds;
		try {
			copyBounds = _prepareForInsertion(a, aLen1, s1, i1, n1, index1, len);
		} catch (ArrayIndexOutOfBoundsException ex) {
			throw sup.get();
		}
		final int copyOff = _copyOff(copyBounds);
		final int copyLen = _copyLen(copyBounds);

		// insert elements (possibly from the underlying array itself)
		int i2 = gap._idx;	// must re-read value in case a == b
		int n2 = gap._len;	// must re-read value in case a == b
		_read(b, aLen2, s2, i2, n2, index2, a, copyOff, copyLen, t, attachment, sup);
		if (copyLen != len) {
			_read(b, aLen2, s2, i2, n2, index2 + copyLen, a, 0, len - copyLen, t, attachment, sup);
		}
		// final adjustment after insertion
		this._len += len;
		this._size += len;
	}

	protected final <B, C, E extends RuntimeException> void _insert(A a, int aLen1, int s1, int i1, int n1, int index1,
			AbstractCyclicArray<B> gap, B b, int aLen2, int s2, int index2,
			int len, Transfer<B, A, C> t, C attachment, Supplier<E> sup, A clear, int clearLen) throws E {

		final long copyBounds;
		try {
			copyBounds = _prepareForInsertion(a, aLen1, s1, i1, n1, index1, len, clear, clearLen);
		} catch (ArrayIndexOutOfBoundsException ex) {
			throw sup.get();
		}
		final int copyOff = _copyOff(copyBounds);
		final int copyLen = _copyLen(copyBounds);

		// insert elements (possibly from the underlying array itself)
		int i2 = gap._idx;	// must re-read value in case a == b
		int n2 = gap._len;	// must re-read value in case a == b
		_read(b, aLen2, s2, i2, n2, index2, a, copyOff, copyLen, t, attachment, sup);
		if (copyLen != len) {
			_read(b, aLen2, s2, i2, n2, index2 + copyLen, a, 0, len - copyLen, t, attachment, sup);
		}
		// final adjustment after insertion
		this._len += len;
		this._size += len;
	}

	protected final <B, C, X extends Throwable> void _insertChecked(A a, int aLen1, int s1, int i1, int n1, int index1,
			AbstractCyclicArray<B> gap, B b, int aLen2, int s2, int index2,
			int len, CheckedTransfer<B, A, C, X> t, C attachment) throws ArrayIndexOutOfBoundsException, X {

		final long copyBounds = _prepareForInsertion(a, aLen1, s1, i1, n1, index1, len);
		final int copyOff = _copyOff(copyBounds);
		final int copyLen = _copyLen(copyBounds);

		// insert elements (possibly from the underlying array itself)
		int i2 = gap._idx;	// must re-read value in case a == b
		int n2 = gap._len;	// must re-read value in case a == b
		_readChecked(b, aLen2, s2, i2, n2, index2, a, copyOff, copyLen, t, attachment);
		if (copyLen != len) {
			_readChecked(b, aLen2, s2, i2, n2, index2 + copyLen, a, 0, len - copyLen, t, attachment);
		}
		// final adjustment after insertion
		this._len += len;
		this._size += len;
	}

	protected final <B, C, X extends Throwable> void _insertChecked(A a, int aLen1, int s1, int i1, int n1, int index1,
			AbstractCyclicArray<B> gap, B b, int aLen2, int s2, int index2,
			int len, CheckedTransfer<B, A, C, X> t, C attachment, A clear, int clearLen) throws ArrayIndexOutOfBoundsException, X {

		final long copyBounds = _prepareForInsertion(a, aLen1, s1, i1, n1, index1, len, clear, clearLen);
		final int copyOff = _copyOff(copyBounds);
		final int copyLen = _copyLen(copyBounds);

		// insert elements (possibly from the underlying array itself)
		int i2 = gap._idx;	// must re-read value in case a == b
		int n2 = gap._len;	// must re-read value in case a == b
		_readChecked(b, aLen2, s2, i2, n2, index2, a, copyOff, copyLen, t, attachment);
		if (copyLen != len) {
			_readChecked(b, aLen2, s2, i2, n2, index2 + copyLen, a, 0, len - copyLen, t, attachment);
		}
		// final adjustment after insertion
		this._len += len;
		this._size += len;
	}

	protected final <B, C, E extends RuntimeException, X extends Throwable> void _insertChecked(A a, int aLen1, int s1, int i1, int n1, int index1,
			AbstractCyclicArray<B> gap, B b, int aLen2, int s2, int index2,
			int len, CheckedTransfer<B, A, C, X> t, C attachment, Supplier<E> sup) throws E, X {

		final long copyBounds;
		try {
			copyBounds = _prepareForInsertion(a, aLen1, s1, i1, n1, index1, len);
		} catch (ArrayIndexOutOfBoundsException ex) {
			throw sup.get();
		}
		final int copyOff = _copyOff(copyBounds);
		final int copyLen = _copyLen(copyBounds);

		// insert elements (possibly from the underlying array itself)
		int i2 = gap._idx;	// must re-read value in case a == b
		int n2 = gap._len;	// must re-read value in case a == b
		_readChecked(b, aLen2, s2, i2, n2, index2, a, copyOff, copyLen, t, attachment, sup);
		if (copyLen != len) {
			_readChecked(b, aLen2, s2, i2, n2, index2 + copyLen, a, 0, len - copyLen, t, attachment, sup);
		}
		// final adjustment after insertion
		this._len += len;
		this._size += len;
	}

	protected final <B, C, E extends RuntimeException, X extends Throwable> void _insertChecked(A a, int aLen1, int s1, int i1, int n1, int index1,
			AbstractCyclicArray<B> gap, B b, int aLen2, int s2, int index2,
			int len, CheckedTransfer<B, A, C, X> t, C attachment, Supplier<E> sup, A clear, int clearLen) throws E, X {

		final long copyBounds;
		try {
			copyBounds = _prepareForInsertion(a, aLen1, s1, i1, n1, index1, len, clear, clearLen);
		} catch (ArrayIndexOutOfBoundsException ex) {
			throw sup.get();
		}
		final int copyOff = _copyOff(copyBounds);
		final int copyLen = _copyLen(copyBounds);

		// insert elements (possibly from the underlying array itself)
		int i2 = gap._idx;	// must re-read value in case a == b
		int n2 = gap._len;	// must re-read value in case a == b
		_readChecked(b, aLen2, s2, i2, n2, index2, a, copyOff, copyLen, t, attachment, sup);
		if (copyLen != len) {
			_readChecked(b, aLen2, s2, i2, n2, index2 + copyLen, a, 0, len - copyLen, t, attachment, sup);
		}
		// final adjustment after insertion
		this._len += len;
		this._size += len;
	}
}

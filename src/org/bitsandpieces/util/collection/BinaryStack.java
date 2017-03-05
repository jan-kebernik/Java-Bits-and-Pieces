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

/**
 *
 * @author pp
 */
public class BinaryStack implements Iterable<Boolean> {

	// 2^26 * 2^5 = 2^31 bits
	// a full stack consumes almost exactly 256 MB of memory
	private static final int EXPONENT = 5;
	private static final int MASK = (1 << EXPONENT) - 1;
	private static final int MAX_ARRAY_SIZE = 1 << (31 - EXPONENT);

	private static final int[] EMPTY = {};

	private int[] data;
	private int index;

	public BinaryStack() {
		this.data = EMPTY;
	}

	public BinaryStack(int initialCapacity) {
		if (initialCapacity < 0) {
			throw new IllegalArgumentException();
		}
		int cap = (initialCapacity + 31) >>> 5;	// at most MAX_ARRAY_SIZE
		this.data = cap == 0 ? EMPTY : new int[cap];
	}

	public long size() {
		return this.index;
	}

	// returns true if successful
	public boolean addBit(boolean bit) {
		int i = this.index;
		if (i == Integer.MAX_VALUE) {
			// is full
			throw new IllegalStateException("stack is full.");
		}
		doAdd(i, bit);
		return true;
	}

	public boolean offerBit(boolean bit) {
		int i = this.index;
		if (i == Integer.MAX_VALUE) {
			// is full
			return false;
		}
		doAdd(i, bit);
		return true;
	}

	private void doAdd(int i, boolean bit) {
		int idx0 = i >>> EXPONENT;
		int idx1 = i & MASK;
		int[] dat0 = this.data;
		if (idx0 >= dat0.length) {
			// cannot overflow, before defaulting to MAX_ARRAY_SIZE
			int[] dat1 = new int[Math.max(MAX_ARRAY_SIZE, dat0.length + (dat0.length >>> 1) + 2)];
			System.arraycopy(dat0, 0, dat1, 0, dat0.length);
			this.data = (dat0 = dat1);
		}
		if (bit) {
			dat0[idx0] |= (1 << idx1);
		} else {
			dat0[idx0] &= ~(1 << idx1);
		}
		this.index = i + 1;
	}

	public boolean pollBit() {
		int i = this.index;
		if (i == 0) {
			return false;
		}
		return doRemove(i);
	}

	public boolean removeBit() {
		int i = this.index;
		if (i == 0) {
			throw new NoSuchElementException();
		}
		return doRemove(i);
	}

	private boolean doRemove(int i) {
		return doPeek(this.data, this.index = i - 1);
	}

	public boolean peekBit() {
		int i = this.index;
		if (i == 0) {
			return false;
		}
		return doPeek(this.data, i);
	}

	public boolean elementBit() {
		int i = this.index;
		if (i == 0) {
			throw new NoSuchElementException();
		}
		return doPeek(this.data, i);
	}

	private boolean doPeek(int[] dat, int i) {
		int idx0 = i >>> EXPONENT;
		int idx1 = i & MASK;
		return (dat[idx0] & (1 << idx1)) != 0;
	}

	public void clear() {
		this.index = 0;
	}

	public boolean isEmpty() {
		return this.index == 0;
	}

	@Override
	public Iterator<Boolean> iterator() {
		return new Iterator<Boolean>() {

			int idx = 0;

			@Override
			public boolean hasNext() {
				return this.idx < BinaryStack.this.index;
			}

			@Override
			public Boolean next() {
				int i = this.idx;
				int m = BinaryStack.this.index;
				if (i >= m) {
					throw new IllegalStateException();
				}
				return doPeek(BinaryStack.this.data, this.idx = i - 1);
			}

			@Override
			public void forEachRemaining(Consumer<? super Boolean> action) {
				int i = this.idx;
				int m = BinaryStack.this.index;
				if (i >= m) {
					return;
				}
				for (int[] d = BinaryStack.this.data; i < m; i++) {
					action.accept(doPeek(d, i));
				}
				this.idx = m;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	@Override
	public void forEach(Consumer<? super Boolean> action) {
		Objects.requireNonNull(action);
		int i = this.index - 1;
		if (i < 0) {
			return;
		}
		int[] d = this.data;
		int i0 = i >>> EXPONENT;
		for (int x = d[i0], j = 1 << (i & MASK); j != 0; j >>>= 1) {
			boolean b = (x & j) != 0;
			action.accept(b);
		}
		while (i0 != 0) {
			for (int x = d[--i0], j = 1 << 31; j != 0; j >>>= 1) {
				boolean b = (x & j) != 0;
				action.accept(b);
			}
		}
	}
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util;

import java.util.Objects;

/**
 * StringBuilder alternative that offers efficient support for {@link Format}s.
 *
 * @author pp
 */
public class StringMaker implements Appendable, CharSequence {

	// max array length on some VMs.
	private static final int MAX_ARRAY_CAPACITY = Integer.MAX_VALUE - 8;

	private static final char[] EMPTY = {};
	private static final Format DEFAULT = Formats.DEC;

	protected char[] chars;
	protected int size;
	private final Target target = new Target();

	public StringMaker() {
		this.chars = EMPTY;
	}

	public StringMaker(int initalCapacity) {
		this.chars = new char[initalCapacity];
	}

	public StringMaker clear() {
		this.size = 0;
		return this;
	}

	@Override
	public int length() {
		return this.size;
	}

	private char[] _ensureInternal(char[] a, int s, int minCap) {
		if (minCap < 0) {
			throw new OutOfMemoryError();
		}
		int aLen = a.length;
		if (minCap <= aLen) {
			return a;
		}
		char[] b = new char[newCap(aLen, minCap)];
		System.arraycopy(a, 0, b, 0, s);
		return (this.chars = b);
	}

	public void ensureCapacity(int minCapacity) {
		char[] a = this.chars;
		int aLen = a.length;
		if (minCapacity > aLen) {
			char[] b = new char[newCap(aLen, minCapacity)];
			System.arraycopy(a, 0, b, 0, this.size);
			this.chars = b;
		}
		// no action if minCapacity <= 0
	}

	private static int newCap(int aLen, int minCap) {
		int newCap = 2 + (aLen << 1);
		if (newCap < 0) {
			newCap = minCap > MAX_ARRAY_CAPACITY
					? Integer.MAX_VALUE
					: MAX_ARRAY_CAPACITY;
		}
		if (newCap < minCap) {
			newCap = minCap;
		}
		return newCap;
	}

	public StringMaker append(Object s) {
		int _s = this.size;
		_insert(_s, _s, String.valueOf(s));
		return this;
	}

	public StringMaker insert(int index, Object s) {
		int _s = this.size;
		if (index < 0 || index > _s) {
			throw new StringIndexOutOfBoundsException(index);
		}
		_insert(_s, index, String.valueOf(s));
		return this;
	}

	public StringMaker append(String s) {
		int _s = this.size;
		_insert(_s, _s, String.valueOf(s));
		return this;
	}

	public StringMaker insert(int index, String s) {
		int _s = this.size;
		if (index < 0 || index > _s) {
			throw new StringIndexOutOfBoundsException(index);
		}
		_insert(_s, index, String.valueOf(s));
		return this;
	}

	private void _insert(int s, int index, String x) {
		int n = x.length();
		int m = s + n;
		char[] a = _ensureInternal(this.chars, s, m);
		System.arraycopy(a, index, a, index + n, s - index);
		x.getChars(0, n, a, index);
		this.size = m;
	}

	private static void rangeCheck(int size, int start, int end) {
		if (start > end) {
			throw new IllegalArgumentException(
					"start(" + start + ") > end(" + end + ")");
		}
		if (start < 0) {
			throw new StringIndexOutOfBoundsException(start);
		}
		if (end > size) {
			throw new StringIndexOutOfBoundsException(end);
		}
	}

	public StringMaker append(char[] a) {
		int s = this.size;
		_insert(s, s, a, 0, a.length);
		return this;
	}

	public StringMaker append(char[] a, int off, int len) {
		int s = this.size;
		if (off < 0 || len < 0 || off > a.length - len) {
			throw new ArrayIndexOutOfBoundsException();
		}
		_insert(s, s, a, off, len);
		return this;
	}

	public StringMaker insert(int index, char[] a) {
		int s = this.size;
		if (index < 0 || index > s) {
			throw new StringIndexOutOfBoundsException(index);
		}
		_insert(s, index, a, 0, a.length);
		return this;
	}

	public StringMaker insert(int index, char[] a, int off, int len) {
		int s = this.size;
		if (index < 0 || index > s) {
			throw new StringIndexOutOfBoundsException(index);
		}
		if (off < 0 || len < 0 || off > a.length - len) {
			throw new ArrayIndexOutOfBoundsException();
		}
		_insert(s, index, a, off, len);
		return this;
	}

	private void _insert(int s, int index, char[] b, int off, int len) {
		int m = s + len;
		char[] a = _ensureInternal(this.chars, s, m);
		System.arraycopy(a, index, a, index + len, s - index);
		System.arraycopy(b, off, a, index, len);
		this.size = m;
	}

	@Override
	public StringMaker append(CharSequence s) {
		int _s = this.size;
		if (s == null) {
			_insert(_s, _s, "null");
			return this;
		}
		_insert(_s, _s, s, 0, s.length());
		return this;
	}

	@Override
	public StringMaker append(CharSequence s, int start, int end) {
		int _s = this.size;
		if (s == null) {
			_insert(_s, _s, "null");
			return this;
		}
		rangeCheck(s.length(), start, end);
		_insert(_s, _s, s, start, end);
		return this;
	}

	public StringMaker insert(int index, CharSequence s) {
		int _s = this.size;
		if (index < 0 || index > _s) {
			throw new StringIndexOutOfBoundsException(index);
		}
		if (s == null) {
			_insert(_s, index, "null");
			return this;
		}
		_insert(_s, index, s, 0, s.length());
		return this;
	}

	public StringMaker insert(int index, CharSequence s, int start, int end) {
		int _s = this.size;
		if (index < 0 || index > _s) {
			throw new StringIndexOutOfBoundsException(index);
		}
		if (s == null) {
			_insert(_s, index, "null");
			return this;
		}
		rangeCheck(s.length(), start, end);
		_insert(_s, index, s, start, end);
		return this;
	}

	private void _insert(int s, int index, CharSequence x, int start, int end) {
		int n = end - start;
		int m = s + n;
		@SuppressWarnings("MismatchedReadAndWriteOfArray")
		char[] a = _ensureInternal(this.chars, s, m);
		for (int max = index + n; index < max;) {
			a[index++] = x.charAt(start++);
		}
		this.size = m;
	}

	public StringMaker append(boolean i) {
		int s = this.size;
		_insert(s, s, String.valueOf(i));
		return this;
	}

	public StringMaker insert(int index, boolean i) {
		int s = this.size;
		if (index < 0 || index > s) {
			throw new StringIndexOutOfBoundsException(index);
		}
		_insert(s, index, String.valueOf(i));
		return this;
	}

	public StringMaker append(byte i, Format f) {
		Objects.requireNonNull(f);
		int s = this.size;
		_insert(s, s, i, f);
		return this;
	}

	public StringMaker insert(int index, byte i, Format f) {
		Objects.requireNonNull(f);
		int s = this.size;
		if (index < 0 || index > s) {
			throw new StringIndexOutOfBoundsException(index);
		}
		_insert(s, index, i, f);
		return this;
	}

	public StringMaker append(byte i) {
		int s = this.size;
		_insert(s, s, i, DEFAULT);
		return this;
	}

	public StringMaker insert(int index, byte i) {
		int s = this.size;
		if (index < 0 || index > s) {
			throw new StringIndexOutOfBoundsException(index);
		}
		_insert(s, index, i, DEFAULT);
		return this;
	}

	private void _insert(int s, int index, byte x, Format f) {
		this.size = s + f.copy(this.target, index, x);
	}

	public StringMaker append(char i, Format f) {
		Objects.requireNonNull(f);
		int s = this.size;
		_insert(s, s, i, f);
		return this;
	}

	public StringMaker insert(int index, char i, Format f) {
		Objects.requireNonNull(f);
		int s = this.size;
		if (index < 0 || index > s) {
			throw new StringIndexOutOfBoundsException(index);
		}
		_insert(s, index, i, f);
		return this;
	}

	private void _insert(int s, int index, char x, Format f) {
		this.size = s + f.copy(this.target, index, x);
	}

	@Override
	public StringMaker append(char i) {
		int _s = this.size;
		_insert(_s, _s, i);
		return this;
	}

	public StringMaker insert(int index, char i) {
		int _s = this.size;
		if (index < 0 || index > _s) {
			throw new StringIndexOutOfBoundsException(index);
		}
		_insert(_s, index, i);
		return this;
	}

	private void _insert(int s, int index, char x) {
		int m = s + 1;
		char[] a = _ensureInternal(this.chars, s, m);
		System.arraycopy(a, index, a, index + 1, s - index);
		a[index] = x;
		this.size = m;
	}

	public StringMaker append(short i, Format f) {
		Objects.requireNonNull(f);
		int s = this.size;
		_insert(s, s, i, f);
		return this;
	}

	public StringMaker insert(int index, short i, Format f) {
		Objects.requireNonNull(f);
		int s = this.size;
		if (index < 0 || index > s) {
			throw new StringIndexOutOfBoundsException(index);
		}
		_insert(s, index, i, f);
		return this;
	}

	public StringMaker append(short i) {
		int s = this.size;
		_insert(s, s, i, DEFAULT);
		return this;
	}

	public StringMaker insert(int index, short i) {
		int s = this.size;
		if (index < 0 || index > s) {
			throw new StringIndexOutOfBoundsException(index);
		}
		_insert(s, index, i, DEFAULT);
		return this;
	}

	private void _insert(int s, int index, short x, Format f) {
		this.size = s + f.copy(this.target, index, x);
	}

	public StringMaker append(int i, Format f) {
		Objects.requireNonNull(f);
		int s = this.size;
		_insert(s, s, i, f);
		return this;
	}

	public StringMaker insert(int index, int i, Format f) {
		Objects.requireNonNull(f);
		int s = this.size;
		if (index < 0 || index > s) {
			throw new StringIndexOutOfBoundsException(index);
		}
		_insert(s, index, i, f);
		return this;
	}

	public StringMaker append(int i) {
		int s = this.size;
		_insert(s, s, i, DEFAULT);
		return this;
	}

	public StringMaker insert(int index, int i) {
		int s = this.size;
		if (index < 0 || index > s) {
			throw new StringIndexOutOfBoundsException(index);
		}
		_insert(s, index, i, DEFAULT);
		return this;
	}

	private void _insert(int s, int index, int x, Format f) {
		this.size = s + f.copy(this.target, index, x);
	}

	public StringMaker append(long i, Format f) {
		Objects.requireNonNull(f);
		int s = this.size;
		_insert(s, s, i, f);
		return this;
	}

	public StringMaker insert(int index, long i, Format f) {
		Objects.requireNonNull(f);
		int s = this.size;
		if (index < 0 || index > s) {
			throw new StringIndexOutOfBoundsException(index);
		}
		_insert(s, index, i, f);
		return this;
	}

	public StringMaker append(long i) {
		int s = this.size;
		_insert(s, s, i, DEFAULT);
		return this;
	}

	public StringMaker insert(int index, long i) {
		int s = this.size;
		if (index < 0 || index > s) {
			throw new StringIndexOutOfBoundsException(index);
		}
		_insert(s, index, i, DEFAULT);
		return this;
	}

	private void _insert(int s, int index, long x, Format f) {
		this.size = s + f.copy(this.target, index, x);
	}

	public StringMaker append(float i) {
		int s = this.size;
		_insert(s, s, String.valueOf(i));
		return this;
	}

	public StringMaker insert(int index, float i) {
		int s = this.size;
		if (index < 0 || index > s) {
			throw new StringIndexOutOfBoundsException(index);
		}
		_insert(s, index, String.valueOf(i));
		return this;
	}

	public StringMaker append(double i) {
		int s = this.size;
		_insert(s, s, String.valueOf(i));
		return this;
	}

	public StringMaker insert(int index, double i) {
		int s = this.size;
		if (index < 0 || index > s) {
			throw new StringIndexOutOfBoundsException(index);
		}
		_insert(s, index, String.valueOf(i));
		return this;
	}

	public StringMaker delete(int index, int length) {
		int s = this.size;
		int n = s - length;
		if (index < 0 || length < 0 || index > n) {
			throw new StringIndexOutOfBoundsException();
		}
		_delete(this.chars, index, length, s, n);
		return this;
	}

	public StringMaker deleteCharAt(int index) {
		int s = this.size;
		if (index < 0 || index >= s) {
			throw new StringIndexOutOfBoundsException();
		}
		_delete(this.chars, index, 1, s, s - 1);
		return this;
	}

	private void _delete(char[] a, int off, int len, int s, int n) {
		int m = off + len;
		System.arraycopy(a, m, a, off, s - m);
		this.size = n;
	}

	private int[] work = null;

	private int[] work() {
		int[] w = this.work;
		return w == null ? (this.work = new int[256]) : ArrayUtil.fill(w, 0);
	}

	public int indexOf(String str) {
		return indexOf(str, 0);
	}

	public int indexOf(String str, int fromIndex) {
		int s = this.size;
		if (fromIndex < 0 || fromIndex >= s) {
			throw new StringIndexOutOfBoundsException(fromIndex);
		}
		if (str.length() == 0) {
			return fromIndex;
		}
		return Horspool.indexOf(this.chars, fromIndex, s - fromIndex, str, 0, str.length(), work());
	}

	public int lastIndexOf(String str) {
		return lastIndexOf(str, 0);
	}

	public int lastIndexOf(String str, int fromIndex) {
		int s = this.size;
		if (fromIndex < 0 || fromIndex >= s) {
			throw new StringIndexOutOfBoundsException(fromIndex);
		}
		if (str.length() == 0) {
			return fromIndex;
		}
		return Horspool.lastIndexOf(this.chars, fromIndex, s - fromIndex, str, 0, str.length(), work());
	}

	@Override
	public String toString() {
		return String.valueOf(this.chars, 0, this.size);
	}

	@Override
	public char charAt(int index) {
		int s = this.size;
		if (index < 0 || index > s) {
			throw new StringIndexOutOfBoundsException(index);
		}
		return this.chars[index];
	}

	@Override
	public String subSequence(int start, int end) {
		rangeCheck(this.size, start, end);
		return String.valueOf(this.chars, start, end - start);
	}

	public StringMaker reverse() {
		boolean hasSurrogates = false;
		int s = this.size;
		char[] a = this.chars;
		int n = s - 1;
		for (int j = (n - 1) >>> 1; j >= 0; j--) {
			int k = n - j;
			char cj = a[j];
			char ck = a[k];
			a[j] = ck;
			a[k] = cj;
			// no need to keep analyzing once the result is known
			if (!hasSurrogates && (Character.isSurrogate(cj) || Character.isSurrogate(ck))) {
				hasSurrogates = true;
			}
		}
		if (hasSurrogates) {
			for (int i = 0; i < s - 1; i++) {
				char c2 = a[i];
				if (Character.isLowSurrogate(c2)) {
					char c1 = a[i + 1];
					if (Character.isHighSurrogate(c1)) {
						a[i++] = c1;
						a[i] = c2;
					}
				}
			}
		}
		return this;
	}

	public StringMaker replace(int start, int end, String s) {
		Objects.requireNonNull(s);
		int _s = this.size;
		rangeCheck(_s, start, end);
		int len = end - start;
		if (len >= s.length()) {
			// more chars deleted than inserted
			int v = len - s.length();
			char[] a = this.chars;
			_delete(a, start, v, _s, _s - v);
			s.getChars(0, s.length(), a, start);
		} else {
			// more chars inserted than deleted
			int m = _s + s.length() - len;
			char[] a = _ensureInternal(this.chars, _s, m);
			System.arraycopy(a, end, a, start + s.length(), _s - end);
			s.getChars(0, s.length(), a, start);
			this.size = m;
		}
		return this;
	}

	private final class Target implements Format.ArrayTarget {

		@Override
		public char[] getArray(int off, int len) {
			int s = StringMaker.this.size;
			char[] a = _ensureInternal(StringMaker.this.chars, s, s + len);
			System.arraycopy(a, off, a, off + len, s - off);
			return a;
		}
	}
}

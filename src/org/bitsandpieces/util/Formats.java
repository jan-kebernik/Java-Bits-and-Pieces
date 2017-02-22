/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util;

import java.util.Arrays;
import java.util.Objects;

/**
 * Standard {@code Format} implementation. Derive a new Format using
 * {@link #get(String)}.
 *
 * @author Jan Kebernik
 */
/*
 * Note:
 * No arrays are allocated unless necessary (only strictly true if reflection is
 * enabled), the number of divisions is kept low without requiring excessive
 * memory. All this allows bulk printing of numbers in many different formats,
 * without needing to allocate redundant intermediate object, while also reducing
 * the amount of math involved. Each class uses an internal lookup table to
 * process as many digits at once as is prudent.
 *
 * Each format is itself a builder for other formats with the same alphabet. If
 * a build instruction would result in an equal format, the same instance may be
 * returned. 
 */
public final class Formats {

	/**
	 * Unsigned, truncated binary {@code Format} (letters '0', '1').
	 */
	public static final Format BIN = get("01").unsigned();

	/**
	 * Unsigned, truncated octal {@code Format} (letters '0' through '7').
	 */
	public static final Format OCT = get("01234567").unsigned();

	/**
	 * Signed, truncated decimal {@code Format} (letters '0' through '9').
	 */
	public static final Format DEC = get("0123456789");

	/**
	 * Unsigned, truncated hexa-decimal {@code Format} (letters '0' through
	 * 'f').
	 */
	public static final Format HEX = get("0123456789abcdef").unsigned();

	private Formats() {
		// prevent instantiation
	}

	private static final String EMPTY = "";

	// used to initialize px and sx to non-null values, which let's us skip 
	// nullity checks throughout the code.
	private static String stringData(String s) {
		return s == null ? EMPTY : s;
	}

	// injects the infix into the char array at interval "iv"
	// there is pretty much no way around this kind of logic, short of 
	// accounting for every single case for every radix.
	private static void spreadInfix(char[] a, int x, int y, char ix, int iv) {
		while ((x -= iv) != (y -= iv)) {
			System.arraycopy(a, x, a, y, iv);
			a[--y] = ix;
		}
	}

	/**
	 * Returns a signed, truncated {@code Format} for the specified alphabet.
	 *
	 * @param alphabet contains the alphabet used by the new {@code Format}.
	 * This {@code String} must have a length of at least 2 and at most 256.
	 * Note that each char in the string counts as a single letter of the
	 * alphabet, meaning that supplementary Unicode codepoints are not accounted
	 * for.
	 * @return a signed, truncated {@code Format} for the specified alphabet.
	 */
	public static final Format get(String alphabet) {
		int radix = alphabet.length();	// implicit nullity check
		if (radix < 2 || radix > 256) {
			// less is weird?
			// more isn't really printable, anyway.
			throw new IllegalArgumentException("alphabet.length() must be between 2 and 256: " + radix);
		}
		if ((radix & (radix - 1)) == 0) {
			// is a power-of-two alphabet
			if (radix == 2 || radix == 4 || radix == 16 || radix == 256) {
				// is a per-byte (power-of-power-of-two) alphabet (2=2^2^0, 4=2^2^1, 16=2^2^2, 256=2^2^3)
				return PerByte.get(alphabet);
			}
			// TODO implement non-per-byte powers-of-2 (8, 32, 64, 128)
		}
		// arbitrary alphabet
		return Arbitrary.get(alphabet);
	}

	// generates an untruncated table for the given alphabet
	static final char[][] genTable(String alphabet, int digits) {
		int s = alphabet.length();
		int p = pow(s, digits);
		char[][] x = new char[p][digits];
		int a = 1;
		int b = s;
		for (int d = digits - 1; d >= 0; d--) {
			for (int h = 0; h < s; h++) {
				char c = alphabet.charAt(h);
				for (int i = h * a; i < p; i += b) {
					int m = i + a;
					for (int v = i; v < m; v++) {
						// this way charAt needs to be  invoked only once for 
						// each table column
						x[v][d] = c;
					}
				}
			}
			a = b;
			b *= s;
		}
		return x;
	}

	// generates a truncated table for the given alphabet
	static final char[][] genTableTruncated(String alphabet, int digits) {
		final int s = alphabet.length();
		char[][] x = new char[pow(s, digits)][];
		for (int i = 0; i < s; i++) {
			// init and fill sub-arrays for one-digit permutations
			x[i] = new char[]{alphabet.charAt(i)};
		}
		for (int d = 2, t = s, b = s * s; d <= digits; d++) {
			int a = t;
			for (; t < b; t++) {
				// init sub-arrays for d
				x[t] = new char[d];
			}
			for (int g = d - 1, f = 1, r = s; g > 0; g--) {
				for (int h = 0; h < s; h++) {
					char c = alphabet.charAt(h);
					for (int i = h * f + a; i < b; i += r) {
						int m = i + f;
						for (int v = i; v < m; v++) {
							// this way charAt needs to be invoked only once for 
							// each table column
							x[v][g] = c;
						}
					}
				}
				f = r;
				r *= s;
			}
			// fill in truncated zeros
			int i = a;
			for (int h = 1; h < s; h++) {
				char c = alphabet.charAt(h);
				for (int j = i + a; i < j; i++) {
					x[i][0] = c;
				}
			}
			b *= s;
		}
		return x;
	}

	// parameters are always small enough for linear runtime to not matter
	private static int pow(int s, int d) {
		int p = s;
		for (int i = 1; i < d; i++) {
			p *= s;
		}
		return p;
	}

	// values are calibrated such that the table size remains useful.
	private static int digitsForRadix(int radix) {
		if (radix > 64) {
			return 1;
		}
		if (radix > 15) {
			return 2;
		}
		if (radix > 6) {
			return 3;	// radix == 6 to 14
		}
		if (radix > 3) {
			return 4;	// radix == 4 or 5
		}
		if (radix == 3) {
			return 6;	// radix == 3
		}
		return 8;		// radix == 2
	}

	private static abstract class AbstractFormat implements Format {

		final char[][] tr, td;
		final String px, sx;			// prefix and suffix. never null. "" if quasi-null.
		final int iv;

		final String alphabet;
		final String prefix, suffix;	// preserves nullity of paramters

		private int hash = 0;

		private AbstractFormat(char[][] tr, char[][] td, String px, String sx, String alphabet, String prefix, String suffix, int iv) {
			this.tr = tr;
			this.td = td;
			this.px = px;
			this.sx = sx;
			this.iv = iv;
			this.alphabet = alphabet;
			this.prefix = prefix;
			this.suffix = suffix;
		}

		@Override
		public final String alphabet() {
			return alphabet;
		}

		@Override
		public final String prefix() {
			return prefix;
		}

		@Override
		public final String suffix() {
			return suffix;
		}

		@Override
		public final int interval() {
			return iv;
		}

		@Override
		public final String toString(byte b) {
			return Strings.newString(toCharArray(b));
		}

		@Override
		public final String toString(char c) {
			return Strings.newString(toCharArray(c));
		}

		@Override
		public final String toString(short s) {
			return Strings.newString(toCharArray(s));
		}

		@Override
		public final String toString(int i) {
			return Strings.newString(toCharArray(i));
		}

		@Override
		public final String toString(long l) {
			return Strings.newString(toCharArray(l));
		}

		@Override
		public final boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (!(obj instanceof Format)) {
				// false if obj == null
				return false;
			}
			Format f = (Format) obj;
			return isSigned() == f.isSigned()
					&& isTruncated() == f.isTruncated()
					&& interval() == f.interval()
					&& Objects.equals(alphabet(), f.alphabet())
					&& Objects.equals(infix(), f.infix())
					&& Objects.equals(prefix(), f.prefix())
					&& Objects.equals(suffix(), f.suffix());
		}

		// lazy, but thread-safe because result is immutable.
		@Override
		public final int hashCode() {
			int h = this.hash;
			if (h == 0) {
				h = this.alphabet.hashCode();	// never null (also lazy)
				h = h * 31 + Objects.hashCode(prefix());
				h = h * 31 + Objects.hashCode(suffix());
				h = h * 31 + Objects.hashCode(infix());
				int s = isSigned() ? 1 : Integer.MIN_VALUE;
				int t = isTruncated() ? 7 : 31;
				if ((h = h ^ s + t * interval()) == 0) {
					h = 1;	// seriously?
				}
				this.hash = h;
			}
			return h;
		}
	}

	private static final class Arbitrary {

		private static Format get(String alphabet) {
			RadixData r = new RadixData(alphabet);
			return new SignedTruncatedNoInfix(genTableTruncated(alphabet, r.digits), genTable(alphabet, r.digits), r, EMPTY, EMPTY, alphabet, null, null, 1);
		}

		private static final class RadixData {

			private static final double LOG2 = Math.log(2);

			private final int radix;

			private final int digits, divisor;

			private final int s8, s16, s32, s64;
			private final int u8, u16, u32, u64;

			private final int sdiv8, sdiv16, sdiv32, sdiv64;
			private final int udiv8, udiv16, udiv32, udiv64;

			private final char[] zeros;

			private final int[] div32;
			private final long[] div64;

			private final UnsignedDivisor divi;

			private static interface UnsignedDivisor {

				long divide(long n, long d);
			}

			private RadixData(String a) {
				this.radix = a.length();
				// if the radix is even, the divisor will always be even as well.
				// thus, unsigned division can be achieved by 
				// first shifting right once, then dividing by half the divisor.
				// otherwise, we avoid slow BigInteger conversion by making use 
				// of a manual long division implementation
				this.divi = (radix & 1) != 0
						? RadixData::divideUnsigned
						: (long n, long d) -> (n >>> 1) / (d >>> 1);

				this.digits = digitsForRadix(radix);	// # digits per division/array access
				double bitsPerDigit = Math.log(radix) / LOG2;	// base-2 log of radix

				// unsigned max digits for each word-length
				this.u8 = numDigits(8, bitsPerDigit);
				this.u16 = numDigits(16, bitsPerDigit);
				this.u32 = numDigits(32, bitsPerDigit);
				this.u64 = numDigits(64, bitsPerDigit);

				// signed max digits for each word-length
				if ((radix & (radix - 1)) == 0) {
					// if the radix is a power of two, then
					// the MIN_VALUE's may actually use 
					// the entire word lengths
					// -1000000 == -128, etc...
					this.s8 = u8;
					this.s16 = u16;
					this.s32 = u32;
					this.s64 = u64;
				} else {
					this.s8 = numDigits(7, bitsPerDigit);
					this.s16 = numDigits(15, bitsPerDigit);
					this.s32 = numDigits(31, bitsPerDigit);
					this.s64 = numDigits(63, bitsPerDigit);
				}

				int d = digits - 1;
				// # index of highest division required for signed word lengths
				this.sdiv8 = ((s8 + d) / digits) - 2;
				this.sdiv16 = ((s16 + d) / digits) - 2;
				this.sdiv32 = ((s32 + d) / digits) - 2;
				this.sdiv64 = ((s64 + d) / digits) - 2;

				// # index of highest division required for unsigned word lengths
				this.udiv8 = ((u8 + d) / digits) - 2;
				this.udiv16 = ((u16 + d) / digits) - 2;
				this.udiv32 = ((u32 + d) / digits) - 2;
				this.udiv64 = ((u64 + d) / digits) - 2;

				this.divisor = divisor(radix, digits);

				Arrays.fill(this.zeros = new char[u64], a.charAt(0));

				// these contain negative divisors,
				// which allows for full 32-bit arithmetic.
				this.div32 = div32(divisor, udiv32 + 1);
				this.div64 = div64(divisor, udiv64 + 1);
			}

			private static int numDigits(int bits, double bitsPerDigit) {
				return (int) Math.ceil(bits / bitsPerDigit);
			}

			// slow, but correct long division for odd divisors. only applies to 
			// long primitives and is only performed once for each operation 
			// (subsequent division are always in the signed range)
			// This is still much cheaper than using BigInteger conversion.
			private static long divideUnsigned(long n, long d) {
				long u = d + Long.MIN_VALUE;	// for unsigned comparison
				long r = 0;
				for (int i = 0; i < 64; i++) {
					r <<= 1;
					if (n < 0L) {
						r++;
					}
					n <<= 1;
					// unsigned comparison
					if (r + Long.MIN_VALUE >= u) {
						n++;
						r -= d;
					}
				}
				return n;
			}

			private static int divisor(int radix, int digits) {
				int d = radix;
				for (int i = 1; i < digits; i++) {
					d *= radix;
				}
				return d;
			}

			private long divide(long n, long d) {
				return divi.divide(n, d);
			}

			private static int[] div32(int divisor, int length) {
				int[] div = new int[length];
				div[0] = -divisor;
				for (int i = 1; i < div.length; i++) {
					div[i] = div[i - 1] * divisor;
				}
				return div;
			}

			private static long[] div64(int divisor, int length) {
				long[] div = new long[length];
				div[0] = -divisor;
				for (int i = 1; i < div.length; i++) {
					div[i] = div[i - 1] * divisor;
				}
				return div;
			}
		}

		private static interface ArbitraryFormat extends Format {

			default int n(int d, int iv) {
				return 0;
			}

			default void spreadInfix(char[] a, int v, int n, char ix, int iv) {
			}

			default int z(int s, int d) {
				return 0;
			}

			default char[] zeros(char[] a, char[] zeros, int off, int len) {
				return a;
			}

			@Override
			default boolean isTruncated() {
				return true;
			}

			@Override
			default Format truncated() {
				return this;
			}
		}

		private static interface Infix extends ArbitraryFormat {

			@Override
			default int n(int d, int iv) {
				return (d - 1) / iv;
			}

			@Override
			default void spreadInfix(char[] a, int x, int y, char ix, int iv) {
				Formats.spreadInfix(a, x, y, ix, iv);
			}
		}

		private static interface Untruncated extends ArbitraryFormat {

			@Override
			default int z(int s, int d) {
				return s - d;
			}

			@Override
			default char[] zeros(char[] a, char[] zeros, int off, int len) {
				System.arraycopy(zeros, 0, a, off, len);
				return a;
			}

			@Override
			default boolean isTruncated() {
				return false;
			}

			@Override
			default Format untruncated() {
				return this;
			}
		}

		private static abstract class AbstractArbitraryFormat extends AbstractFormat implements ArbitraryFormat {

			final RadixData r;
			final char ix;

			final Character infix;

			private AbstractArbitraryFormat(char[][] tr, char[][] td, RadixData r, String px, char ix, String sx, String alphabet, String prefix, Character infix, String suffix, int iv) {
				super(tr, td, px, sx, alphabet, prefix, suffix, iv);
				this.r = r;
				this.ix = ix;
				this.infix = infix;
			}

			@Override
			public final Character infix() {
				return infix;
			}
		}

		private static abstract class AbstractArbitrarySignedFormat extends AbstractArbitraryFormat {

			private AbstractArbitrarySignedFormat(char[][] tr, char[][] td, RadixData r, String px, char ix, String sx, String alphabet, String prefix, Character infix, String suffix, int iv) {
				super(tr, td, r, px, ix, sx, alphabet, prefix, infix, suffix, iv);
			}

			private void c(char[] a, int off, char[] k, int e, int z, int n) {
				px.getChars(0, px.length(), a, off += e);
				zeros(a, r.zeros, off += px.length(), z);
				System.arraycopy(k, 0, a, off += z, k.length);
				int y = (off += k.length) + n;
				sx.getChars(0, sx.length(), a, y);
				spreadInfix(a, off, y, ix, iv);
			}

			private int c(ArrayTarget t, int off, char[] k, int e, int s) {
				int d = k.length;
				int z = z(s, d);
				int n = n(z + d, iv);
				int len = e + px.length() + z + d + n + sx.length();
				char[] a = t.getArray(off, len);
				a[off] = '-';	// the sign will be overriden whenever e == 0
				c(a, off, k, e, z, n);
				return len;
			}

			private char[] c(char[] k, int e, int s) {
				int d = k.length;
				int z = z(s, d);
				int n = n(z + d, iv);
				int len = e + px.length() + z + d + n + sx.length();
				char[] a = new char[len];
				c(a, 0, k, e, z, n);
				return a;
			}

			private void c32(char[] a, int off, char[] k, int i, int e, int m, int j, int z, int n) {
				px.getChars(0, px.length(), a, off += e);
				zeros(a, r.zeros, off += px.length(), z);
				System.arraycopy(k, 0, a, off += z, k.length);
				off += k.length;
				for (--j; j >= 0; j--) {
					int div = r.div32[j];
					m = i / div;
					i -= m * div;
					System.arraycopy(td[m], 0, a, off, r.digits);
					off += r.digits;
				}
				System.arraycopy(td[-i], 0, a, off, r.digits);
				int y = (off += r.digits) + n;
				sx.getChars(0, sx.length(), a, y);
				spreadInfix(a, off, y, ix, iv);
			}

			private int c32(ArrayTarget t, int off, int i, int e, int m, int j, int s) {
				char[] k = tr[m];
				int d = k.length + (j + 1) * r.digits;
				int z = z(s, d);
				int w = z + d;
				int n = n(w, iv);
				int len = e + px.length() + w + n + sx.length();
				char[] a = t.getArray(off, len);
				a[off] = '-';	// the sign will be overriden whenever e == 0
				c32(a, off, k, i, e, m, j, z, n);
				return len;
			}

			private char[] c32(int i, int e, int m, int j, int s) {
				char[] k = tr[m];
				int d = k.length + (j + 1) * r.digits;
				int z = z(s, d);
				int w = z + d;
				int n = n(w, iv);
				int len = e + px.length() + w + n + sx.length();
				char[] a = new char[len];
				c32(a, 0, k, i, e, m, j, z, n);
				return a;
			}

			private void c64(char[] a, int off, char[] k, long i, int e, long m, int j, int z, int n) {
				px.getChars(0, px.length(), a, off += e);
				zeros(a, r.zeros, off += px.length(), z);
				System.arraycopy(k, 0, a, off += z, k.length);
				off += k.length;
				for (--j; j >= 0; j--) {
					long div = r.div64[j];
					m = i / div;
					i -= m * div;
					System.arraycopy(td[(int) m], 0, a, off, r.digits);
					off += r.digits;
				}
				System.arraycopy(td[(int) -i], 0, a, off, r.digits);
				int y = (off += r.digits) + n;
				sx.getChars(0, sx.length(), a, y);
				spreadInfix(a, off, y, ix, iv);
			}

			private int c64(ArrayTarget t, int off, long i, int e, long m, int j) {
				char[] k = tr[(int) m];
				int d = k.length + (j + 1) * r.digits;
				int z = z(r.s64, d);
				int w = z + d;
				int n = n(w, iv);
				int len = e + px.length() + w + n + sx.length();
				char[] a = t.getArray(off, len);
				a[off] = '-';	// the sign will be overriden whenever e == 0
				c64(a, off, k, i, e, m, j, z, n);
				return len;
			}

			private char[] c64(long i, int e, long m, int j) {
				char[] k = tr[(int) m];
				int d = k.length + (j + 1) * r.digits;
				int z = z(r.s64, d);
				int w = z + d;
				int n = n(w, iv);
				int len = e + px.length() + w + n + sx.length();
				char[] a = new char[len];
				c64(a, 0, k, i, e, m, j, z, n);
				return a;
			}

			private int copy32(ArrayTarget t, int off, int i, int e, int j, int s) {
				for (; j >= 0; j--) {
					int div = r.div32[j];
					if (i <= div) {
						int m = i / div;
						return c32(t, off, i - m * div, e, m, j, s);
					}
				}
				char[] k = tr[-i];
				return c(t, off, k, e, s);
			}

			private char[] copy32(int i, int e, int j, int s) {
				for (; j >= 0; j--) {
					int div = r.div32[j];
					if (i <= div) {
						int m = i / div;
						return c32(i - m * div, e, m, j, s);
					}
				}
				char[] k = tr[-i];
				return c(k, e, s);
			}

			private int copy64(ArrayTarget t, int off, long i, int e) {
				for (int j = r.sdiv64; j >= 0; j--) {
					long div = r.div64[j];
					if (i <= div) {
						long m = i / div;
						return c64(t, off, i - m * div, e, m, j);
					}
				}
				char[] k = tr[(int) -i];
				return c(t, off, k, e, r.s64);
			}

			private char[] copy64(long i, int e) {
				for (int j = r.sdiv64; j >= 0; j--) {
					long div = r.div64[j];
					if (i <= div) {
						long m = i / div;
						return c64(i - m * div, e, m, j);
					}
				}
				char[] k = tr[(int) -i];
				return c(k, e, r.s64);
			}

			// positive numbers are negated such that MIN_VALUES are no longer
			// special cases that would otherwise require attention
			@Override
			public final int copy(ArrayTarget t, int off, byte b) {
				if (t == null) {
					throw new NullPointerException();
				}
				int n = b;
				if (n < 0) {
					return copy32(t, off, n, 1, r.sdiv8, r.s8);
				}
				return copy32(t, off, -n, 0, r.sdiv8, r.s8);
			}

			@Override
			public final int copy(ArrayTarget t, int off, char c) {
				if (t == null) {
					throw new NullPointerException();
				}
				int n = c;
				if (n > 32767) {
					return copy32(t, off, n - 65536, 1, r.sdiv16, r.s16);
				}
				return copy32(t, off, -n, 0, r.sdiv16, r.s16);
			}

			@Override
			public final int copy(ArrayTarget t, int off, short s) {
				if (t == null) {
					throw new NullPointerException();
				}
				int n = s;
				if (n < 0) {
					return copy32(t, off, n, 1, r.sdiv16, r.s16);
				}
				return copy32(t, off, -n, 0, r.sdiv16, r.s16);
			}

			@Override
			public final int copy(ArrayTarget t, int off, int i) {
				if (t == null) {
					throw new NullPointerException();
				}
				if (i < 0) {
					return copy32(t, off, i, 1, r.sdiv32, r.s32);
				}
				return copy32(t, off, -i, 0, r.sdiv32, r.s32);
			}

			@Override
			public final int copy(ArrayTarget t, int off, long l) {
				if (t == null) {
					throw new NullPointerException();
				}
				if (l < 0L) {
					return copy64(t, off, l, 1);
				}
				return copy64(t, off, -l, 0);
			}

			private static char[] minus(char[] a) {
				a[0] = '-';
				return a;
			}

			@Override
			public final char[] toCharArray(byte b) {
				int n = b;
				if (n < 0) {
					return minus(copy32(n, 1, r.sdiv8, r.s8));
				}
				return copy32(-n, 0, r.sdiv8, r.s8);
			}

			@Override
			public final char[] toCharArray(char c) {
				int n = c;
				if (n > 32767) {
					return minus(copy32(n - 65536, 1, r.sdiv16, r.s16));
				}
				return copy32(-n, 0, r.sdiv16, r.s16);
			}

			@Override
			public final char[] toCharArray(short s) {
				int n = s;
				if (n < 0) {
					return minus(copy32(n, 1, r.sdiv16, r.s16));
				}
				return copy32(-n, 0, r.sdiv16, r.s16);
			}

			@Override
			public final char[] toCharArray(int i) {
				if (i < 0) {
					return minus(copy32(i, 1, r.sdiv32, r.s32));
				}
				return copy32(-i, 0, r.sdiv32, r.s32);
			}

			@Override
			public final char[] toCharArray(long l) {
				if (l < 0L) {
					return minus(copy64(l, 1));
				}
				return copy64(-l, 0);
			}

			@Override
			public final boolean isSigned() {
				return true;
			}

			@Override
			public Format signed() {
				return this;
			}
		}

		private static final class SignedTruncatedInfix extends AbstractArbitrarySignedFormat implements Infix {

			private SignedTruncatedInfix(char[][] tr, char[][] td, RadixData r, String px, char ix, String sx, String alphabet, String prefix, Character infix, String suffix, int iv) {
				super(tr, td, r, px, ix, sx, alphabet, prefix, infix, suffix, iv);
			}

			@Override
			public Format unsigned() {
				return new UnsignedTruncatedInfix(tr, td, r, px, ix, sx, alphabet, prefix, infix, suffix, iv);
			}

			@Override
			public Format untruncated() {
				return new SignedUntruncatedInfix(tr, td, r, px, ix, sx, alphabet, prefix, infix, suffix, iv);
			}

			@Override
			public Format interval(int interval) {
				if (interval == this.iv) {
					return this;
				}
				if (interval < 1) {
					throw new IllegalArgumentException("interval must be positive: " + interval);
				}
				return new SignedTruncatedInfix(tr, td, r, px, ix, sx, alphabet, prefix, infix, suffix, interval);
			}

			@Override
			public Format prefix(String prefix) {
				String dat = stringData(prefix);
				return px.equals(dat) ? this : new SignedTruncatedInfix(tr, td, r, dat, ix, sx, alphabet, prefix, infix, suffix, iv);
			}

			@Override
			public Format infix(Character infix) {
				return infix == null
						? new SignedTruncatedNoInfix(tr, td, r, px, sx, alphabet, prefix, suffix, iv)
						: ix == infix ? this : new SignedTruncatedInfix(tr, td, r, px, infix, sx, alphabet, prefix, infix, suffix, iv);
			}

			@Override
			public Format suffix(String suffix) {
				String dat = stringData(suffix);
				return sx.equals(dat) ? this : new SignedTruncatedInfix(tr, td, r, px, ix, dat, alphabet, prefix, infix, suffix, iv);
			}
		}

		private static final class SignedTruncatedNoInfix extends AbstractArbitrarySignedFormat {

			private SignedTruncatedNoInfix(char[][] tr, char[][] td, RadixData r, String px, String sx, String alphabet, String prefix, String suffix, int iv) {
				super(tr, td, r, px, '\u0000', sx, alphabet, prefix, null, suffix, iv);
			}

			@Override
			public Format unsigned() {
				return new UnsignedTruncatedNoInfix(tr, td, r, px, sx, alphabet, prefix, suffix, iv);
			}

			@Override
			public Format untruncated() {
				return new SignedUntruncatedNoInfix(tr, td, r, px, sx, alphabet, prefix, suffix, iv);
			}

			@Override
			public Format interval(int interval) {
				if (interval == this.iv) {
					return this;
				}
				if (interval < 1) {
					throw new IllegalArgumentException("interval must be positive: " + interval);
				}
				return new SignedTruncatedNoInfix(tr, td, r, px, sx, alphabet, prefix, suffix, interval);
			}

			@Override
			public Format prefix(String prefix) {
				String dat = stringData(prefix);
				return px.equals(dat) ? this : new SignedTruncatedNoInfix(tr, td, r, dat, sx, alphabet, prefix, suffix, iv);
			}

			@Override
			public Format infix(Character infix) {
				return infix == null
						? this
						: new SignedTruncatedInfix(tr, td, r, px, infix, sx, alphabet, prefix, infix, suffix, iv);
			}

			@Override
			public Format suffix(String suffix) {
				String dat = stringData(suffix);
				return sx.equals(dat) ? this : new SignedTruncatedNoInfix(tr, td, r, px, dat, alphabet, prefix, suffix, iv);
			}
		}

		private static final class SignedUntruncatedInfix extends AbstractArbitrarySignedFormat implements Untruncated, Infix {

			private SignedUntruncatedInfix(char[][] tr, char[][] td, RadixData r, String px, char ix, String sx, String alphabet, String prefix, Character infix, String suffix, int iv) {
				super(tr, td, r, px, ix, sx, alphabet, prefix, infix, suffix, iv);
			}

			@Override
			public Format unsigned() {
				return new UnsignedUntruncatedInfix(tr, td, r, px, ix, sx, alphabet, prefix, infix, suffix, iv);
			}

			@Override
			public Format truncated() {
				return new SignedTruncatedInfix(tr, td, r, px, ix, sx, alphabet, prefix, infix, suffix, iv);
			}

			@Override
			public Format interval(int interval) {
				if (interval == this.iv) {
					return this;
				}
				if (interval < 1) {
					throw new IllegalArgumentException("interval must be positive: " + interval);
				}
				return new SignedUntruncatedInfix(tr, td, r, px, ix, sx, alphabet, prefix, infix, suffix, interval);
			}

			@Override
			public Format prefix(String prefix) {
				String dat = stringData(prefix);
				return px.equals(dat) ? this : new SignedUntruncatedInfix(tr, td, r, dat, ix, sx, alphabet, prefix, infix, suffix, iv);
			}

			@Override
			public Format infix(Character infix) {
				return infix == null
						? new SignedUntruncatedNoInfix(tr, td, r, px, sx, alphabet, prefix, suffix, iv)
						: ix == infix ? this : new SignedUntruncatedInfix(tr, td, r, px, infix, sx, alphabet, prefix, infix, suffix, iv);
			}

			@Override
			public Format suffix(String suffix) {
				String dat = stringData(suffix);
				return sx.equals(dat) ? this : new SignedUntruncatedInfix(tr, td, r, px, ix, dat, alphabet, prefix, infix, suffix, iv);
			}
		}

		private static final class SignedUntruncatedNoInfix extends AbstractArbitrarySignedFormat implements Untruncated {

			private SignedUntruncatedNoInfix(char[][] tr, char[][] td, RadixData r, String px, String sx, String alphabet, String prefix, String suffix, int iv) {
				super(tr, td, r, px, '\u0000', sx, alphabet, prefix, null, suffix, iv);
			}

			@Override
			public Format unsigned() {
				return new UnsignedUntruncatedNoInfix(tr, td, r, px, sx, alphabet, prefix, suffix, iv);
			}

			@Override
			public Format truncated() {
				return new SignedTruncatedNoInfix(tr, td, r, px, sx, alphabet, prefix, suffix, iv);
			}

			@Override
			public Format interval(int interval) {
				if (interval == this.iv) {
					return this;
				}
				if (interval < 1) {
					throw new IllegalArgumentException("interval must be positive: " + interval);
				}
				return new SignedUntruncatedNoInfix(tr, td, r, px, sx, alphabet, prefix, suffix, interval);
			}

			@Override
			public Format prefix(String prefix) {
				String dat = stringData(prefix);
				return px.equals(dat) ? this : new SignedUntruncatedNoInfix(tr, td, r, dat, sx, alphabet, prefix, suffix, iv);
			}

			@Override
			public Format infix(Character infix) {
				return infix == null
						? this
						: new SignedUntruncatedInfix(tr, td, r, px, infix, sx, alphabet, prefix, infix, suffix, iv);
			}

			@Override
			public Format suffix(String suffix) {
				String dat = stringData(suffix);
				return sx.equals(dat) ? this : new SignedUntruncatedNoInfix(tr, td, r, px, dat, alphabet, prefix, suffix, iv);
			}
		}

		private static abstract class AbstractArbitraryUnsignedFormat extends AbstractArbitraryFormat {

			private AbstractArbitraryUnsignedFormat(char[][] tr, char[][] td, RadixData r, String px, char ix, String sx, String alphabet, String prefix, Character infix, String suffix, int iv) {
				super(tr, td, r, px, ix, sx, alphabet, prefix, infix, suffix, iv);
			}

			private void c(char[] a, int off, char[] k, int z, int n) {
				px.getChars(0, px.length(), a, off);
				zeros(a, r.zeros, off += px.length(), z);
				System.arraycopy(k, 0, a, off += z, k.length);
				int y = (off += k.length) + n;
				sx.getChars(0, sx.length(), a, y);
				spreadInfix(a, off, y, ix, iv);
			}

			private int c(ArrayTarget t, int off, char[] k, int s) {
				int z = z(s, k.length);
				int n = n(z + k.length, iv);
				int len = px.length() + z + k.length + n + sx.length();
				char[] a = t.getArray(off, len);
				c(a, off, k, z, n);
				return len;
			}

			private char[] c(char[] k, int s) {
				int z = z(s, k.length);
				int n = n(z + k.length, iv);
				int len = px.length() + z + k.length + n + sx.length();
				char[] a = new char[len];
				c(a, 0, k, z, n);
				return a;
			}

			private void c32(char[] a, int off, char[] k, int i, int m, int j, int z, int n) {
				px.getChars(0, px.length(), a, off);
				zeros(a, r.zeros, off += px.length(), z);
				System.arraycopy(k, 0, a, off += z, k.length);
				off += k.length;
				for (--j; j >= 0; j--) {
					int div = -r.div32[j];
					m = i / div;
					i -= m * div;
					System.arraycopy(td[m], 0, a, off, r.digits);
					off += r.digits;
				}
				System.arraycopy(td[i], 0, a, off, r.digits);
				int y = (off += r.digits) + n;
				sx.getChars(0, sx.length(), a, y);
				spreadInfix(a, off, y, ix, iv);
			}

			private int c32(ArrayTarget t, int off, int i, int m, int j, int s) {
				char[] k = tr[m];
				int d = k.length + (j + 1) * r.digits;
				int z = z(s, d);
				int n = n(z + d, iv);
				int len = px.length() + z + d + n + sx.length();
				char[] a = t.getArray(off, len);
				c32(a, off, k, i, m, j, z, n);
				return len;
			}

			private char[] c32(int i, int m, int j, int s) {
				char[] k = tr[m];
				int d = k.length + (j + 1) * r.digits;
				int z = z(s, d);
				int n = n(z + d, iv);
				int len = px.length() + z + d + n + sx.length();
				char[] a = new char[len];
				c32(a, 0, k, i, m, j, z, n);
				return a;
			}

			private void c64(char[] a, int off, char[] k, long i, long m, int j, int z, int n) {
				px.getChars(0, px.length(), a, off);
				zeros(a, r.zeros, off += px.length(), z);
				System.arraycopy(k, 0, a, off += z, k.length);
				off += k.length;
				for (--j; j >= 0; j--) {
					long div = -r.div64[j];
					m = i / div;
					i -= m * div;
					System.arraycopy(td[(int) m], 0, a, off, r.digits);
					off += r.digits;
				}
				System.arraycopy(td[(int) i], 0, a, off, r.digits);
				int y = (off += r.digits) + n;
				sx.getChars(0, sx.length(), a, y);
				spreadInfix(a, off, y, ix, iv);
			}

			private int c64(ArrayTarget t, int off, long i, long m, int j) {
				char[] k = tr[(int) m];
				int d = k.length + (j + 1) * r.digits;
				int z = z(r.u64, d);
				int n = n(z + d, iv);
				int len = px.length() + z + d + n + sx.length();
				char[] a = t.getArray(off, len);
				c64(a, off, k, i, m, j, z, n);
				return len;
			}

			private char[] c64(long i, long m, int j) {
				char[] k = tr[(int) m];
				int d = k.length + (j + 1) * r.digits;
				int z = z(r.u64, d);
				int n = n(z + d, iv);
				int len = px.length() + z + d + n + sx.length();
				char[] a = new char[len];
				c64(a, 0, k, i, m, j, z, n);
				return a;
			}

			private int copy32(ArrayTarget t, int off, int i, int j, int s) {
				for (; j >= 0; j--) {
					int div = -r.div32[j];
					if (i >= div) {
						int m = i / div;
						return c32(t, off, i - m * div, m, j, s);
					}
				}
				char[] k = tr[i];
				return c(t, off, k, s);
			}

			private char[] copy32(int i, int j, int s) {
				for (; j >= 0; j--) {
					int div = -r.div32[j];
					if (i >= div) {
						int m = i / div;
						return c32(i - m * div, m, j, s);
					}
				}
				char[] k = tr[i];
				return c(k, s);
			}

			@Override
			public final int copy(ArrayTarget t, int off, byte b) {
				if (t == null) {
					throw new NullPointerException();
				}
				return copy32(t, off, b & 0xff, r.udiv8, r.u8);
			}

			@Override
			public final int copy(ArrayTarget t, int off, char c) {
				if (t == null) {
					throw new NullPointerException();
				}
				return copy32(t, off, c, r.udiv16, r.u16);
			}

			@Override
			public final int copy(ArrayTarget t, int off, short s) {
				if (t == null) {
					throw new NullPointerException();
				}
				return copy32(t, off, s & 0xffff, r.udiv16, r.u16);
			}

			@Override
			public final int copy(ArrayTarget t, int off, int i) {
				if (t == null) {
					throw new NullPointerException();
				}
				if (i < 0) {
					// in unsigned range
					long u = i & 0xffffffffL;
					for (int j = r.udiv32; j >= 0; j--) {
						int div = -r.div32[j];
						long udiv = div & 0xffffffffL;
						if (u >= udiv) {
							int m = (int) (u / udiv);
							return c32(t, off, i - m * div, m, j, r.u32);
						}
					}
					char[] k = tr[i];
					return c(t, off, k, r.u32);
				}
				// in positive signed range
				return copy32(t, off, i, r.sdiv32, r.u32);
			}

			@Override
			public final int copy(ArrayTarget t, int off, long l) {
				if (t == null) {
					throw new NullPointerException();
				}
				if (l < 0L) {
					// in unsigned range
					long u = l + Long.MIN_VALUE;
					for (int j = r.udiv64; j >= 0; j--) {
						long div = -r.div64[j];
						if (u >= div + Long.MIN_VALUE) {
							long m = r.divide(l, div);
							return c64(t, off, l - m * div, m, j);
						}
					}
					char[] k = tr[(int) l];
					return c(t, off, k, r.u64);
				}
				// in positive signed range
				for (int j = r.sdiv64; j >= 0; j--) {
					long div = -r.div64[j];
					if (l >= div) {
						long m = l / div;
						return c64(t, off, l - m * div, m, j);
					}
				}
				char[] k = tr[(int) l];
				return c(t, off, k, r.u64);
			}

			@Override
			public final char[] toCharArray(byte b) {
				return copy32(b & 0xff, r.udiv8, r.u8);
			}

			@Override
			public final char[] toCharArray(char c) {
				return copy32(c, r.udiv16, r.u16);
			}

			@Override
			public final char[] toCharArray(short s) {
				return copy32(s & 0xffff, r.udiv16, r.u16);
			}

			@Override
			public final char[] toCharArray(int i) {
				if (i < 0) {
					// in unsigned range
					long u = i & 0xffffffffL;
					for (int j = r.udiv32; j >= 0; j--) {
						int div = -r.div32[j];
						long udiv = div & 0xffffffffL;
						if (u >= udiv) {
							int m = (int) (u / udiv);
							return c32(i - m * div, m, j, r.u32);
						}
					}
					char[] k = tr[i];
					return c(k, r.u32);
				}
				// in positive signed range
				return copy32(i, r.sdiv32, r.u32);
			}

			@Override
			public final char[] toCharArray(long l) {
				if (l < 0L) {
					// in unsigned range
					long u = l + Long.MIN_VALUE;
					for (int j = r.udiv64; j >= 0; j--) {
						long div = -r.div64[j];
						if (u >= div + Long.MIN_VALUE) {
							long m = r.divide(l, div);
							return c64(l - m * div, m, j);
						}
					}
					char[] k = tr[(int) l];
					return c(k, r.u64);
				}
				// in positive signed range
				for (int j = r.sdiv64; j >= 0; j--) {
					long div = -r.div64[j];
					if (l >= div) {
						long m = l / div;
						return c64(l - m * div, m, j);
					}
				}
				char[] k = tr[(int) l];
				return c(k, r.u64);
			}

			@Override
			public final boolean isSigned() {
				return false;
			}

			@Override
			public final Format unsigned() {
				return this;
			}
		}

		private static final class UnsignedTruncatedInfix extends AbstractArbitraryUnsignedFormat implements Infix {

			private UnsignedTruncatedInfix(char[][] tr, char[][] td, RadixData r, String px, char ix, String sx, String alphabet, String prefix, Character infix, String suffix, int iv) {
				super(tr, td, r, px, ix, sx, alphabet, prefix, infix, suffix, iv);
			}

			@Override
			public Format signed() {
				return new SignedTruncatedInfix(tr, td, r, px, ix, sx, alphabet, prefix, infix, suffix, iv);
			}

			@Override
			public Format untruncated() {
				return new UnsignedUntruncatedInfix(tr, td, r, px, ix, sx, alphabet, prefix, infix, suffix, iv);
			}

			@Override
			public Format interval(int interval) {
				if (interval == this.iv) {
					return this;
				}
				if (interval < 1) {
					throw new IllegalArgumentException("interval must be positive: " + interval);
				}
				return new UnsignedTruncatedInfix(tr, td, r, px, ix, sx, alphabet, prefix, infix, suffix, interval);
			}

			@Override
			public Format prefix(String prefix) {
				String dat = stringData(prefix);
				return px.equals(dat) ? this : new UnsignedTruncatedInfix(tr, td, r, dat, ix, sx, alphabet, prefix, infix, suffix, iv);
			}

			@Override
			public Format infix(Character infix) {
				return infix == null
						? new UnsignedTruncatedNoInfix(tr, td, r, px, sx, alphabet, prefix, suffix, iv)
						: ix == infix ? this : new UnsignedTruncatedInfix(tr, td, r, px, infix, sx, alphabet, prefix, infix, suffix, iv);
			}

			@Override
			public Format suffix(String suffix) {
				String dat = stringData(suffix);
				return sx.equals(dat) ? this : new UnsignedTruncatedInfix(tr, td, r, px, ix, dat, alphabet, prefix, infix, suffix, iv);
			}
		}

		private static final class UnsignedTruncatedNoInfix extends AbstractArbitraryUnsignedFormat {

			private UnsignedTruncatedNoInfix(char[][] tr, char[][] td, RadixData r, String px, String sx, String alphabet, String prefix, String suffix, int iv) {
				super(tr, td, r, px, '\u0000', sx, alphabet, prefix, null, suffix, iv);
			}

			@Override
			public Format signed() {
				return new SignedTruncatedNoInfix(tr, td, r, px, sx, alphabet, prefix, suffix, iv);
			}

			@Override
			public Format untruncated() {
				return new UnsignedUntruncatedNoInfix(tr, td, r, px, sx, alphabet, prefix, suffix, iv);
			}

			@Override
			public Format interval(int interval) {
				if (interval == this.iv) {
					return this;
				}
				if (interval < 1) {
					throw new IllegalArgumentException("interval must be positive: " + interval);
				}
				return new UnsignedTruncatedNoInfix(tr, td, r, px, sx, alphabet, prefix, suffix, interval);
			}

			@Override
			public Format prefix(String prefix) {
				String dat = stringData(prefix);
				return px.equals(dat) ? this : new UnsignedTruncatedNoInfix(tr, td, r, dat, sx, alphabet, prefix, suffix, iv);
			}

			@Override
			public Format infix(Character infix) {
				return infix == null
						? this
						: new UnsignedTruncatedInfix(tr, td, r, px, infix, sx, alphabet, prefix, infix, suffix, iv);
			}

			@Override
			public Format suffix(String suffix) {
				String dat = stringData(suffix);
				return sx.equals(dat) ? this : new UnsignedTruncatedNoInfix(tr, td, r, px, dat, alphabet, prefix, suffix, iv);
			}
		}

		private static final class UnsignedUntruncatedInfix extends AbstractArbitraryUnsignedFormat implements Untruncated, Infix {

			private UnsignedUntruncatedInfix(char[][] tr, char[][] td, RadixData r, String px, char ix, String sx, String alphabet, String prefix, Character infix, String suffix, int iv) {
				super(tr, td, r, px, ix, sx, alphabet, prefix, infix, suffix, iv);
			}

			@Override
			public Format signed() {
				return new SignedUntruncatedInfix(tr, td, r, px, ix, sx, alphabet, prefix, infix, suffix, iv);
			}

			@Override
			public Format truncated() {
				return new UnsignedTruncatedInfix(tr, td, r, px, ix, sx, alphabet, prefix, infix, suffix, iv);
			}

			@Override
			public Format interval(int interval) {
				if (interval == this.iv) {
					return this;
				}
				if (interval < 1) {
					throw new IllegalArgumentException("interval must be positive: " + interval);
				}
				return new UnsignedUntruncatedInfix(tr, td, r, px, ix, sx, alphabet, prefix, infix, suffix, interval);
			}

			@Override
			public Format prefix(String prefix) {
				String dat = stringData(prefix);
				return px.equals(dat) ? this : new UnsignedUntruncatedInfix(tr, td, r, dat, ix, sx, alphabet, prefix, infix, suffix, iv);
			}

			@Override
			public Format infix(Character infix) {
				return infix == null
						? new UnsignedUntruncatedNoInfix(tr, td, r, px, sx, alphabet, prefix, suffix, iv)
						: ix == infix ? this : new UnsignedUntruncatedInfix(tr, td, r, px, infix, sx, alphabet, prefix, infix, suffix, iv);
			}

			@Override
			public Format suffix(String suffix) {
				String dat = stringData(suffix);
				return sx.equals(dat) ? this : new UnsignedUntruncatedInfix(tr, td, r, px, ix, dat, alphabet, prefix, infix, suffix, iv);
			}
		}

		private static final class UnsignedUntruncatedNoInfix extends AbstractArbitraryUnsignedFormat implements Untruncated {

			private UnsignedUntruncatedNoInfix(char[][] tr, char[][] td, RadixData r, String px, String sx, String alphabet, String prefix, String suffix, int iv) {
				super(tr, td, r, px, '\u0000', sx, alphabet, prefix, null, suffix, iv);
			}

			@Override
			public Format signed() {
				return new SignedUntruncatedNoInfix(tr, td, r, px, sx, alphabet, prefix, suffix, iv);
			}

			@Override
			public Format truncated() {
				return new UnsignedTruncatedNoInfix(tr, td, r, px, sx, alphabet, prefix, suffix, iv);
			}

			@Override
			public Format interval(int interval) {
				if (interval == this.iv) {
					return this;
				}
				if (interval < 1) {
					throw new IllegalArgumentException("interval must be positive: " + interval);
				}
				return new UnsignedUntruncatedNoInfix(tr, td, r, px, sx, alphabet, prefix, suffix, interval);
			}

			@Override
			public Format prefix(String prefix) {
				String dat = stringData(prefix);
				return px.equals(dat) ? this : new UnsignedUntruncatedNoInfix(tr, td, r, dat, sx, alphabet, prefix, suffix, iv);
			}

			@Override
			public Format infix(Character infix) {
				return infix == null
						? this
						: new UnsignedUntruncatedInfix(tr, td, r, px, infix, sx, alphabet, prefix, infix, suffix, iv);
			}

			@Override
			public Format suffix(String suffix) {
				String dat = stringData(suffix);
				return sx.equals(dat) ? this : new UnsignedUntruncatedNoInfix(tr, td, r, px, dat, alphabet, prefix, suffix, iv);
			}
		}
	}

	private static final class PerByte {

		private static Format get(String alphabet) {
			int dig1 = digitsForRadix(alphabet.length());
			return new SignedTruncatedNoInfix(genTableTruncated(alphabet, dig1), genTable(alphabet, dig1), EMPTY, EMPTY, alphabet, null, null, 1, dig1);
		}

		private static int copy8Bytes(char[][] td, char[] a, int off, char[] b7, int b6, int b5, int b4, int b3, int b2, int b1, int b0, int digits) {
			System.arraycopy(b7, 0, a, off, b7.length);
			System.arraycopy(td[b6], 0, a, off += b7.length, digits);
			System.arraycopy(td[b5], 0, a, off += digits, digits);
			System.arraycopy(td[b4], 0, a, off += digits, digits);
			System.arraycopy(td[b3], 0, a, off += digits, digits);
			System.arraycopy(td[b2], 0, a, off += digits, digits);
			System.arraycopy(td[b1], 0, a, off += digits, digits);
			System.arraycopy(td[b0], 0, a, off += digits, digits);
			return off + digits;
		}

		private static int copy7Bytes(char[][] td, char[] a, int off, char[] b6, int b5, int b4, int b3, int b2, int b1, int b0, int digits) {
			System.arraycopy(b6, 0, a, off, b6.length);
			System.arraycopy(td[b5], 0, a, off += b6.length, digits);
			System.arraycopy(td[b4], 0, a, off += digits, digits);
			System.arraycopy(td[b3], 0, a, off += digits, digits);
			System.arraycopy(td[b2], 0, a, off += digits, digits);
			System.arraycopy(td[b1], 0, a, off += digits, digits);
			System.arraycopy(td[b0], 0, a, off += digits, digits);
			return off + digits;
		}

		private static int copy6Bytes(char[][] td, char[] a, int off, char[] b5, int b4, int b3, int b2, int b1, int b0, int digits) {
			System.arraycopy(b5, 0, a, off, b5.length);
			System.arraycopy(td[b4], 0, a, off += b5.length, digits);
			System.arraycopy(td[b3], 0, a, off += digits, digits);
			System.arraycopy(td[b2], 0, a, off += digits, digits);
			System.arraycopy(td[b1], 0, a, off += digits, digits);
			System.arraycopy(td[b0], 0, a, off += digits, digits);
			return off + digits;
		}

		private static int copy5Bytes(char[][] td, char[] a, int off, char[] b4, int b3, int b2, int b1, int b0, int digits) {
			System.arraycopy(b4, 0, a, off, b4.length);
			System.arraycopy(td[b3], 0, a, off += b4.length, digits);
			System.arraycopy(td[b2], 0, a, off += digits, digits);
			System.arraycopy(td[b1], 0, a, off += digits, digits);
			System.arraycopy(td[b0], 0, a, off += digits, digits);
			return off + digits;
		}

		private static int copy4Bytes(char[][] td, char[] a, int off, char[] b3, int b2, int b1, int b0, int digits) {
			System.arraycopy(b3, 0, a, off, b3.length);
			System.arraycopy(td[b2], 0, a, off += b3.length, digits);
			System.arraycopy(td[b1], 0, a, off += digits, digits);
			System.arraycopy(td[b0], 0, a, off += digits, digits);
			return off + digits;
		}

		private static int copy3Bytes(char[][] td, char[] a, int off, char[] b2, int b1, int b0, int digits) {
			System.arraycopy(b2, 0, a, off, b2.length);
			System.arraycopy(td[b1], 0, a, off += b2.length, digits);
			System.arraycopy(td[b0], 0, a, off += digits, digits);
			return off + digits;
		}

		private static int copy2Bytes(char[][] td, char[] a, int off, char[] b1, int b0, int digits) {
			System.arraycopy(b1, 0, a, off, b1.length);
			System.arraycopy(td[b0], 0, a, off += b1.length, digits);
			return off + digits;
		}

		private static int copy1Byte(char[] a, int off, char[] b0) {
			System.arraycopy(b0, 0, a, off, b0.length);
			return off + b0.length;
		}

		private static interface PerByteFormat extends Format {

			int copy8(ArrayTarget t, int e, int off, int i);

			int copy16(ArrayTarget t, int e, int off, int i);

			int copy32(ArrayTarget t, int e, int off, int i);

			int copy64(ArrayTarget t, int e, int off, long i);

			char[] copy8(int e, int i);

			char[] copy16(int e, int i);

			char[] copy32(int e, int i);

			char[] copy64(int e, long i);
		}

		private static interface Signed extends PerByteFormat {

			@Override
			default int copy(ArrayTarget t, int off, byte b) {
				int n = b;
				return n < 0
						? copy8(t, 1, off, -n)
						: copy8(t, 0, off, n);
			}

			@Override
			default int copy(ArrayTarget t, int off, char c) {
				int n = c;
				return n > 32767
						? copy16(t, 1, off, 65536 - n)
						: copy16(t, 0, off, n);
			}

			@Override
			default int copy(ArrayTarget t, int off, short s) {
				int n = s;
				return n < 0
						? copy16(t, 1, off, -n)
						: copy16(t, 0, off, n);
			}

			@Override
			default int copy(ArrayTarget t, int off, int i) {
				return i < 0
						? copy32(t, 1, off, -i)
						: copy32(t, 0, off, i);
			}

			@Override
			default int copy(ArrayTarget t, int off, long l) {
				return l < 0L
						? copy64(t, 1, off, -l)
						: copy64(t, 0, off, l);
			}

			@Override
			default char[] toCharArray(byte b) {
				int n = b;
				if (n < 0) {
					char[] a = copy32(1, -n);
					a[0] = '-';
					return a;
				}
				return copy32(0, n);
			}

			@Override
			default char[] toCharArray(char c) {
				int n = c;
				if (n > 32767) {
					char[] a = copy32(1, 65536 - n);
					a[0] = '-';
					return a;
				}
				return copy32(0, n);
			}

			@Override
			default char[] toCharArray(short s) {
				int n = s;
				if (n < 0) {
					char[] a = copy32(1, -n);
					a[0] = '-';
					return a;
				}
				return copy32(0, n);
			}

			@Override
			default char[] toCharArray(int i) {
				if (i < 0) {
					char[] a = copy32(1, -i);
					a[0] = '-';
					return a;
				}
				return copy32(0, i);
			}

			@Override
			default char[] toCharArray(long l) {
				if (l < 0L) {
					char[] a = copy64(1, -l);
					a[0] = '-';
					return a;
				}
				return copy64(0, l);
			}

			@Override
			default boolean isSigned() {
				return true;
			}

			@Override
			default Format signed() {
				return this;
			}
		}

		private static interface Unsigned extends PerByteFormat {

			@Override
			default int copy(ArrayTarget t, int off, byte b) {
				return copy8(t, 0, off, b & 0xff);
			}

			@Override
			default int copy(ArrayTarget t, int off, char c) {
				return copy16(t, 0, off, c);
			}

			@Override
			default int copy(ArrayTarget t, int off, short s) {
				return copy16(t, 0, off, s & 0xffff);
			}

			@Override
			default int copy(ArrayTarget t, int off, int i) {
				return copy32(t, 0, off, i);
			}

			@Override
			default int copy(ArrayTarget t, int off, long l) {
				return copy64(t, 0, off, l);
			}

			@Override
			default char[] toCharArray(byte b) {
				return copy8(0, b & 0xff);
			}

			@Override
			default char[] toCharArray(char c) {
				return copy16(0, c);
			}

			@Override
			default char[] toCharArray(short s) {
				return copy16(0, s & 0xffff);
			}

			@Override
			default char[] toCharArray(int i) {
				return copy32(0, i);
			}

			@Override
			default char[] toCharArray(long l) {
				return copy64(0, l);
			}

			@Override
			default boolean isSigned() {
				return false;
			}

			@Override
			default Format unsigned() {
				return this;
			}
		}

		private static abstract class AbstractTruncated extends AbstractFormat implements PerByteFormat {

			final int dig1, dig2, dig3, dig4, dig5, dig6, dig7;

			private AbstractTruncated(char[][] tr, char[][] td, String px, String sx, String alphabet, String prefix, String suffix, int iv, int dig1, int dig2, int dig3, int dig4, int dig5, int dig6, int dig7) {
				super(tr, td, px, sx, alphabet, prefix, suffix, iv);
				this.dig1 = dig1;
				this.dig2 = dig2;
				this.dig3 = dig3;
				this.dig4 = dig4;
				this.dig5 = dig5;
				this.dig6 = dig6;
				this.dig7 = dig7;
			}

			private AbstractTruncated(char[][] tr, char[][] td, String px, String sx, String alphabet, String prefix, String suffix, int iv, int dig1) {
				super(tr, td, px, sx, alphabet, prefix, suffix, iv);
				this.dig1 = dig1;
				this.dig2 = dig1 << 1;
				this.dig3 = dig2 + dig1;
				this.dig4 = dig1 << 2;
				this.dig5 = dig4 + dig1;
				this.dig6 = dig3 << 1;
				this.dig7 = dig4 + dig3;
			}

			abstract char[] copy8(int e, int b7, int b6, int b5, int b4, int b3, int b2, int b1, int b0);

			abstract char[] copy7(int e, int b6, int b5, int b4, int b3, int b2, int b1, int b0);

			abstract char[] copy6(int e, int b5, int b4, int b3, int b2, int b1, int b0);

			abstract char[] copy5(int e, int b4, int b3, int b2, int b1, int b0);

			abstract char[] copy4(int e, int b3, int b2, int b1, int b0);

			abstract char[] copy3(int e, int b2, int b1, int b0);

			abstract char[] copy2(int e, int b1, int b0);

			abstract char[] copy1(int e, int b0);

			abstract int copy8(ArrayTarget t, int e, int off, int b7, int b6, int b5, int b4, int b3, int b2, int b1, int b0);

			abstract int copy7(ArrayTarget t, int e, int off, int b6, int b5, int b4, int b3, int b2, int b1, int b0);

			abstract int copy6(ArrayTarget t, int e, int off, int b5, int b4, int b3, int b2, int b1, int b0);

			abstract int copy5(ArrayTarget t, int e, int off, int b4, int b3, int b2, int b1, int b0);

			abstract int copy4(ArrayTarget t, int e, int off, int b3, int b2, int b1, int b0);

			abstract int copy3(ArrayTarget t, int e, int off, int b2, int b1, int b0);

			abstract int copy2(ArrayTarget t, int e, int off, int b1, int b0);

			abstract int copy1(ArrayTarget t, int e, int off, int b0);

			@Override
			public final char[] copy8(int e, int i) {
				return copy1(e, i);
			}

			@Override
			public final char[] copy16(int e, int i) {
				int q1 = i >>> 8;
				return q1 != 0
						? copy2(e, q1, i & 0xff)
						: copy1(e, i);
			}

			@Override
			public final char[] copy32(int e, int i) {
				int q1 = i >>> 16;
				if (q1 != 0) {
					int q0 = q1 >>> 8;
					return q0 != 0
							? copy4(e,
									q0,
									q1 & 0xff,
									(i >>> 8) & 0xff,
									i & 0xff)
							: copy3(e,
									q1,
									(i >>> 8) & 0xff,
									i & 0xff
							);
				}
				return copy16(e, i);
			}

			@Override
			public final char[] copy64(int e, long i) {
				int i1 = (int) (i >>> 32);
				int i0 = (int) i;
				if (i1 != 0) {
					int q1 = i1 >>> 16;
					if (q1 != 0) {
						int q0 = q1 >>> 8;
						return q0 != 0
								? copy8(e,
										q0,
										q1 & 0xff,
										(i1 >>> 8) & 0xff,
										i1 & 0xff,
										i0 >>> 24,
										(i0 >>> 16) & 0xff,
										(i0 >>> 8) & 0xff,
										i0 & 0xff)
								: copy7(e,
										q1,
										(i1 >>> 8) & 0xff,
										i1 & 0xff,
										i0 >>> 24,
										(i0 >>> 16) & 0xff,
										(i0 >>> 8) & 0xff,
										i0 & 0xff);
					}
					int q0 = i1 >>> 8;
					return q0 != 0
							? copy6(e,
									q0,
									i1 & 0xff,
									i0 >>> 24,
									(i0 >>> 16) & 0xff,
									(i0 >>> 8) & 0xff,
									i0 & 0xff)
							: copy5(e,
									i1,
									i0 >>> 24,
									(i0 >>> 16) & 0xff,
									(i0 >>> 8) & 0xff,
									i0 & 0xff);
				}
				return copy32(e, i0);
			}

			@Override
			public final int copy8(ArrayTarget t, int e, int off, int i) {
				return copy1(t, e, off, i);
			}

			@Override
			public final int copy16(ArrayTarget t, int e, int off, int i) {
				int q1 = i >>> 8;
				return q1 != 0
						? copy2(t, e, off, q1, i & 0xff)
						: copy1(t, e, off, i);
			}

			@Override
			public final int copy32(ArrayTarget t, int e, int off, int i) {
				int q1 = i >>> 16;
				if (q1 != 0) {
					int q0 = q1 >>> 8;
					return q0 != 0
							? copy4(t, e, off,
									q0,
									q1 & 0xff,
									(i >>> 8) & 0xff,
									i & 0xff)
							: copy3(t, e, off,
									q1,
									(i >>> 8) & 0xff,
									i & 0xff);
				}
				return copy16(t, e, off, i);
			}

			@Override
			public final int copy64(ArrayTarget t, int e, int off, long i) {
				int i1 = (int) (i >>> 32);
				int i0 = (int) i;
				if (i1 != 0) {
					int q1 = i1 >>> 16;
					if (q1 != 0) {
						int q0 = q1 >>> 8;
						return q0 != 0
								? copy8(t, e, off,
										q0,
										q1 & 0xff,
										(i1 >>> 8) & 0xff,
										i1 & 0xff,
										i0 >>> 24,
										(i0 >>> 16) & 0xff,
										(i0 >>> 8) & 0xff,
										i0 & 0xff)
								: copy7(t, e, off,
										q1,
										(i1 >>> 8) & 0xff,
										i1 & 0xff,
										i0 >>> 24,
										(i0 >>> 16) & 0xff,
										(i0 >>> 8) & 0xff,
										i0 & 0xff);
					}
					int q0 = i1 >>> 8;
					return q0 != 0
							? copy6(t, e, off,
									q0,
									i1 & 0xff,
									i0 >>> 24,
									(i0 >>> 16) & 0xff,
									(i0 >>> 8) & 0xff,
									i0 & 0xff)
							: copy5(t, e, off,
									i1,
									i0 >>> 24,
									(i0 >>> 16) & 0xff,
									(i0 >>> 8) & 0xff,
									i0 & 0xff);
				}
				return copy32(t, e, off, i0);
			}

			@Override
			public final boolean isTruncated() {
				return true;
			}

			@Override
			public final Format truncated() {
				return this;
			}
		}

		private static abstract class AbstractTruncatedInfix extends AbstractTruncated {

			final char ix;
			final Character infix;

			private AbstractTruncatedInfix(char[][] tr, char[][] td, String px, char ix, String sx, String alphabet, String prefix, Character infix, String suffix, int iv, int dig1, int dig2, int dig3, int dig4, int dig5, int dig6, int dig7) {
				super(tr, td, px, sx, alphabet, prefix, suffix, iv, dig1, dig2, dig3, dig4, dig5, dig6, dig7);
				this.ix = ix;
				this.infix = infix;
			}

			private AbstractTruncatedInfix(char[][] tr, char[][] td, String px, char ix, String sx, String alphabet, String prefix, Character infix, String suffix, int iv, int dig1) {
				super(tr, td, px, sx, alphabet, prefix, suffix, iv, dig1);
				this.ix = ix;
				this.infix = infix;
			}

			private void c8(char[] a, int off, int n, char[] c7, int b6, int b5, int b4, int b3, int b2, int b1, int b0) {
				px.getChars(0, px.length(), a, off);
				int x = PerByte.copy8Bytes(td, a, off + px.length(), c7, b6, b5, b4, b3, b2, b1, b0, dig1);
				int y = x + n;
				sx.getChars(0, sx.length(), a, y);
				Formats.spreadInfix(a, x, y, ix, iv);
			}

			private void c7(char[] a, int off, int n, char[] c6, int b5, int b4, int b3, int b2, int b1, int b0) {
				px.getChars(0, px.length(), a, off);
				int x = PerByte.copy7Bytes(td, a, off + px.length(), c6, b5, b4, b3, b2, b1, b0, dig1);
				int y = x + n;
				sx.getChars(0, sx.length(), a, y);
				Formats.spreadInfix(a, x, y, ix, iv);
			}

			private void c6(char[] a, int off, int n, char[] c5, int b4, int b3, int b2, int b1, int b0) {
				px.getChars(0, px.length(), a, off);
				int x = PerByte.copy6Bytes(td, a, off + px.length(), c5, b4, b3, b2, b1, b0, dig1);
				int y = x + n;
				sx.getChars(0, sx.length(), a, y);
				Formats.spreadInfix(a, x, y, ix, iv);
			}

			private void c5(char[] a, int off, int n, char[] c4, int b3, int b2, int b1, int b0) {
				px.getChars(0, px.length(), a, off);
				int x = PerByte.copy5Bytes(td, a, off + px.length(), c4, b3, b2, b1, b0, dig1);
				int y = x + n;
				sx.getChars(0, sx.length(), a, y);
				Formats.spreadInfix(a, x, y, ix, iv);
			}

			private void c4(char[] a, int off, int n, char[] c3, int b2, int b1, int b0) {
				px.getChars(0, px.length(), a, off);
				int x = PerByte.copy4Bytes(td, a, off + px.length(), c3, b2, b1, b0, dig1);
				int y = x + n;
				sx.getChars(0, sx.length(), a, y);
				Formats.spreadInfix(a, x, y, ix, iv);
			}

			private void c3(char[] a, int off, int n, char[] c2, int b1, int b0) {
				px.getChars(0, px.length(), a, off);
				int x = PerByte.copy3Bytes(td, a, off + px.length(), c2, b1, b0, dig1);
				int y = x + n;
				sx.getChars(0, sx.length(), a, y);
				Formats.spreadInfix(a, x, y, ix, iv);
			}

			private void c2(char[] a, int off, int n, char[] c1, int b0) {
				px.getChars(0, px.length(), a, off);
				int x = PerByte.copy2Bytes(td, a, off + px.length(), c1, b0, dig1);
				int y = x + n;
				sx.getChars(0, sx.length(), a, y);
				Formats.spreadInfix(a, x, y, ix, iv);
			}

			private void c1(char[] a, int off, int n, char[] c0) {
				px.getChars(0, px.length(), a, off);
				int x = PerByte.copy1Byte(a, off + px.length(), c0);
				int y = x + n;
				sx.getChars(0, sx.length(), a, y);
				Formats.spreadInfix(a, x, y, ix, iv);
			}

			@Override
			final int copy8(ArrayTarget t, int e, int off, int b7, int b6, int b5, int b4, int b3, int b2, int b1, int b0) {
				char[] c7 = tr[b7];
				int d = c7.length + dig7;
				int n = (d - 1) / iv;
				int len = e + px.length() + d + n + sx.length();
				char[] a = t.getArray(off, len);
				a[off] = '-';	// always overwritten if e == 0
				c8(a, off + e, n, c7, b6, b5, b4, b3, b2, b1, b0);
				return len;
			}

			@Override
			final int copy7(ArrayTarget t, int e, int off, int b6, int b5, int b4, int b3, int b2, int b1, int b0) {
				char[] c6 = tr[b6];
				int d = c6.length + dig6;
				int n = (d - 1) / iv;
				int len = e + px.length() + d + n + sx.length();
				char[] a = t.getArray(off, len);
				a[off] = '-';	// always overwritten if e == 0
				c7(a, off + e, n, c6, b5, b4, b3, b2, b1, b0);
				return len;
			}

			@Override
			final int copy6(ArrayTarget t, int e, int off, int b5, int b4, int b3, int b2, int b1, int b0) {
				char[] c5 = tr[b5];
				int d = c5.length + dig5;
				int n = (d - 1) / iv;
				int len = e + px.length() + d + n + sx.length();
				char[] a = t.getArray(off, len);
				a[off] = '-';	// always overwritten if e == 0
				c6(a, off + e, n, c5, b4, b3, b2, b1, b0);
				return len;
			}

			@Override
			final int copy5(ArrayTarget t, int e, int off, int b4, int b3, int b2, int b1, int b0) {
				char[] c4 = tr[b4];
				int d = c4.length + dig4;
				int n = (d - 1) / iv;
				int len = e + px.length() + d + n + sx.length();
				char[] a = t.getArray(off, len);
				a[off] = '-';	// always overwritten if e == 0
				c5(a, off + e, n, c4, b3, b2, b1, b0);
				return len;
			}

			@Override
			final int copy4(ArrayTarget t, int e, int off, int b3, int b2, int b1, int b0) {
				char[] c3 = tr[b3];
				int d = c3.length + dig3;
				int n = (d - 1) / iv;
				int len = e + px.length() + d + n + sx.length();
				char[] a = t.getArray(off, len);
				a[off] = '-';	// always overwritten if e == 0
				c4(a, off + e, n, c3, b2, b1, b0);
				return len;
			}

			@Override
			final int copy3(ArrayTarget t, int e, int off, int b2, int b1, int b0) {
				char[] c2 = tr[b2];
				int d = c2.length + dig2;
				int n = (d - 1) / iv;
				int len = e + px.length() + d + n + sx.length();
				char[] a = t.getArray(off, len);
				a[off] = '-';	// always overwritten if e == 0
				c3(a, off + e, n, c2, b1, b0);
				return len;
			}

			@Override
			final int copy2(ArrayTarget t, int e, int off, int b1, int b0) {
				char[] c1 = tr[b1];
				int d = c1.length + dig1;
				int n = (d - 1) / iv;
				int len = e + px.length() + d + n + sx.length();
				char[] a = t.getArray(off, len);
				a[off] = '-';	// always overwritten if e == 0
				c2(a, off + e, n, c1, b0);
				return len;
			}

			@Override
			final int copy1(ArrayTarget t, int e, int off, int b0) {
				char[] c0 = tr[b0];
				int d = c0.length;
				int n = (d - 1) / iv;
				int len = e + px.length() + d + n + sx.length();
				char[] a = t.getArray(off, len);
				a[off] = '-';	// always overwritten if e == 0
				c1(a, off + e, n, c0);
				return len;
			}

			@Override
			final char[] copy8(int e, int b7, int b6, int b5, int b4, int b3, int b2, int b1, int b0) {
				char[] c7 = tr[b7];
				int d = c7.length + dig7;
				int n = (d - 1) / iv;
				int len = e + px.length() + d + n + sx.length();
				char[] a = new char[len];
				c8(a, e, n, c7, b6, b5, b4, b3, b2, b1, b0);
				return a;
			}

			@Override
			final char[] copy7(int e, int b6, int b5, int b4, int b3, int b2, int b1, int b0) {
				char[] c6 = tr[b6];
				int d = c6.length + dig6;
				int n = (d - 1) / iv;
				int len = e + px.length() + d + n + sx.length();
				char[] a = new char[len];
				c7(a, e, n, c6, b5, b4, b3, b2, b1, b0);
				return a;
			}

			@Override
			final char[] copy6(int e, int b5, int b4, int b3, int b2, int b1, int b0) {
				char[] c5 = tr[b5];
				int d = c5.length + dig5;
				int n = (d - 1) / iv;
				int len = e + px.length() + d + n + sx.length();
				char[] a = new char[len];
				c6(a, e, n, c5, b4, b3, b2, b1, b0);
				return a;
			}

			@Override
			final char[] copy5(int e, int b4, int b3, int b2, int b1, int b0) {
				char[] c4 = tr[b4];
				int d = c4.length + dig4;
				int n = (d - 1) / iv;
				int len = e + px.length() + d + n + sx.length();
				char[] a = new char[len];
				c5(a, e, n, c4, b3, b2, b1, b0);
				return a;
			}

			@Override
			final char[] copy4(int e, int b3, int b2, int b1, int b0) {
				char[] c3 = tr[b3];
				int d = c3.length + dig3;
				int n = (d - 1) / iv;
				int len = e + px.length() + d + n + sx.length();
				char[] a = new char[len];
				c4(a, e, n, c3, b2, b1, b0);
				return a;
			}

			@Override
			final char[] copy3(int e, int b2, int b1, int b0) {
				char[] c2 = tr[b2];
				int d = c2.length + dig2;
				int n = (d - 1) / iv;
				int len = e + px.length() + d + n + sx.length();
				char[] a = new char[len];
				c3(a, e, n, c2, b1, b0);
				return a;
			}

			@Override
			final char[] copy2(int e, int b1, int b0) {
				char[] c1 = tr[b1];
				int d = c1.length + dig1;
				int n = (d - 1) / iv;
				int len = e + px.length() + d + n + sx.length();
				char[] a = new char[len];
				c2(a, e, n, c1, b0);
				return a;
			}

			@Override
			final char[] copy1(int e, int b0) {
				char[] c0 = tr[b0];
				int d = c0.length;
				int n = (d - 1) / iv;
				int len = e + px.length() + d + n + sx.length();
				char[] a = new char[len];
				c1(a, e, n, c0);
				return a;
			}

			@Override
			public final Character infix() {
				return infix;
			}
		}

		private static abstract class AbstractTruncatedNoInfix extends AbstractTruncated {

			private AbstractTruncatedNoInfix(char[][] tr, char[][] td, String px, String sx, String alphabet, String prefix, String suffix, int iv, int dig1, int dig2, int dig3, int dig4, int dig5, int dig6, int dig7) {
				super(tr, td, px, sx, alphabet, prefix, suffix, iv, dig1, dig2, dig3, dig4, dig5, dig6, dig7);
			}

			private AbstractTruncatedNoInfix(char[][] tr, char[][] td, String px, String sx, String alphabet, String prefix, String suffix, int iv, int dig1) {
				super(tr, td, px, sx, alphabet, prefix, suffix, iv, dig1);
			}

			private void c8(char[] a, int off, char[] c7, int b6, int b5, int b4, int b3, int b2, int b1, int b0) {
				px.getChars(0, px.length(), a, off);
				off = PerByte.copy8Bytes(td, a, off + px.length(), c7, b6, b5, b4, b3, b2, b1, b0, dig1);
				sx.getChars(0, sx.length(), a, off);
			}

			private void c7(char[] a, int off, char[] c6, int b5, int b4, int b3, int b2, int b1, int b0) {
				px.getChars(0, px.length(), a, off);
				off = PerByte.copy7Bytes(td, a, off + px.length(), c6, b5, b4, b3, b2, b1, b0, dig1);
				sx.getChars(0, sx.length(), a, off);
			}

			private void c6(char[] a, int off, char[] c5, int b4, int b3, int b2, int b1, int b0) {
				px.getChars(0, px.length(), a, off);
				off = PerByte.copy6Bytes(td, a, off + px.length(), c5, b4, b3, b2, b1, b0, dig1);
				sx.getChars(0, sx.length(), a, off);
			}

			private void c5(char[] a, int off, char[] c4, int b3, int b2, int b1, int b0) {
				px.getChars(0, px.length(), a, off);
				off = PerByte.copy5Bytes(td, a, off + px.length(), c4, b3, b2, b1, b0, dig1);
				sx.getChars(0, sx.length(), a, off);
			}

			private void c4(char[] a, int off, char[] c3, int b2, int b1, int b0) {
				px.getChars(0, px.length(), a, off);
				off = PerByte.copy4Bytes(td, a, off + px.length(), c3, b2, b1, b0, dig1);
				sx.getChars(0, sx.length(), a, off);
			}

			private void c3(char[] a, int off, char[] c2, int b1, int b0) {
				px.getChars(0, px.length(), a, off);
				off = PerByte.copy3Bytes(td, a, off + px.length(), c2, b1, b0, dig1);
				sx.getChars(0, sx.length(), a, off);
			}

			private void c2(char[] a, int off, char[] c1, int b0) {
				px.getChars(0, px.length(), a, off);
				off = PerByte.copy2Bytes(td, a, off + px.length(), c1, b0, dig1);
				sx.getChars(0, sx.length(), a, off);
			}

			private void c1(char[] a, int off, char[] c0) {
				px.getChars(0, px.length(), a, off);
				off = PerByte.copy1Byte(a, off + px.length(), c0);
				sx.getChars(0, sx.length(), a, off);
			}

			@Override
			final int copy8(ArrayTarget t, int e, int off, int b7, int b6, int b5, int b4, int b3, int b2, int b1, int b0) {
				char[] c7 = tr[b7];
				int len = e + px.length() + c7.length + dig7 + sx.length();
				char[] a = t.getArray(off, len);
				a[off] = '-';	// always overwritten if e == 0
				c8(a, off + e, c7, b6, b5, b4, b3, b2, b1, b0);
				return len;
			}

			@Override
			final int copy7(ArrayTarget t, int e, int off, int b6, int b5, int b4, int b3, int b2, int b1, int b0) {
				char[] c6 = tr[b6];
				int len = e + px.length() + c6.length + dig6 + sx.length();
				char[] a = t.getArray(off, len);
				a[off] = '-';	// always overwritten if e == 0
				c7(a, off + e, c6, b5, b4, b3, b2, b1, b0);
				return len;
			}

			@Override
			final int copy6(ArrayTarget t, int e, int off, int b5, int b4, int b3, int b2, int b1, int b0) {
				char[] c5 = tr[b5];
				int len = e + px.length() + c5.length + dig5 + sx.length();
				char[] a = t.getArray(off, len);
				a[off] = '-';	// always overwritten if e == 0
				c6(a, off + e, c5, b4, b3, b2, b1, b0);
				return len;
			}

			@Override
			final int copy5(ArrayTarget t, int e, int off, int b4, int b3, int b2, int b1, int b0) {
				char[] c4 = tr[b4];
				int len = e + px.length() + c4.length + dig4 + sx.length();
				char[] a = t.getArray(off, len);
				a[off] = '-';	// always overwritten if e == 0
				c5(a, off + e, c4, b3, b2, b1, b0);
				return len;
			}

			@Override
			final int copy4(ArrayTarget t, int e, int off, int b3, int b2, int b1, int b0) {
				char[] c3 = tr[b3];
				int len = e + px.length() + c3.length + dig3 + sx.length();
				char[] a = t.getArray(off, len);
				a[off] = '-';	// always overwritten if e == 0
				c4(a, off + e, c3, b2, b1, b0);
				return len;
			}

			@Override
			final int copy3(ArrayTarget t, int e, int off, int b2, int b1, int b0) {
				char[] c2 = tr[b2];
				int len = e + px.length() + c2.length + dig2 + sx.length();
				char[] a = t.getArray(off, len);
				a[off] = '-';	// always overwritten if e == 0
				c3(a, off + e, c2, b1, b0);
				return len;
			}

			@Override
			final int copy2(ArrayTarget t, int e, int off, int b1, int b0) {
				char[] c1 = tr[b1];
				int len = e + px.length() + c1.length + dig1 + sx.length();
				char[] a = t.getArray(off, len);
				a[off] = '-';	// always overwritten if e == 0
				c2(a, off + e, c1, b0);
				return len;
			}

			@Override
			final int copy1(ArrayTarget t, int e, int off, int b0) {
				char[] c0 = tr[b0];
				int len = e + px.length() + c0.length + sx.length();
				char[] a = t.getArray(off, len);
				a[off] = '-';	// always overwritten if e == 0
				c1(a, off + e, c0);
				return len;
			}

			@Override
			final char[] copy8(int e, int b7, int b6, int b5, int b4, int b3, int b2, int b1, int b0) {
				char[] c7 = tr[b7];
				int len = e + px.length() + c7.length + dig7 + sx.length();
				char[] a = new char[len];
				c8(a, e, c7, b6, b5, b4, b3, b2, b1, b0);
				return a;
			}

			@Override
			final char[] copy7(int e, int b6, int b5, int b4, int b3, int b2, int b1, int b0) {
				char[] c6 = tr[b6];
				int len = e + px.length() + c6.length + dig6 + sx.length();
				char[] a = new char[len];
				c7(a, e, c6, b5, b4, b3, b2, b1, b0);
				return a;
			}

			@Override
			final char[] copy6(int e, int b5, int b4, int b3, int b2, int b1, int b0) {
				char[] c5 = tr[b5];
				int len = e + px.length() + c5.length + dig5 + sx.length();
				char[] a = new char[len];
				c6(a, e, c5, b4, b3, b2, b1, b0);
				return a;
			}

			@Override
			final char[] copy5(int e, int b4, int b3, int b2, int b1, int b0) {
				char[] c4 = tr[b4];
				int len = e + px.length() + c4.length + dig4 + sx.length();
				char[] a = new char[len];
				c5(a, e, c4, b3, b2, b1, b0);
				return a;
			}

			@Override
			final char[] copy4(int e, int b3, int b2, int b1, int b0) {
				char[] c3 = tr[b3];
				int len = e + px.length() + c3.length + dig3 + sx.length();
				char[] a = new char[len];
				c4(a, e, c3, b2, b1, b0);
				return a;
			}

			@Override
			final char[] copy3(int e, int b2, int b1, int b0) {
				char[] c2 = tr[b2];
				int len = e + px.length() + c2.length + dig2 + sx.length();
				char[] a = new char[len];
				c3(a, e, c2, b1, b0);
				return a;
			}

			@Override
			final char[] copy2(int e, int b1, int b0) {
				char[] c1 = tr[b1];
				int len = e + px.length() + c1.length + dig1 + sx.length();
				char[] a = new char[len];
				c2(a, e, c1, b0);
				return a;
			}

			@Override
			final char[] copy1(int e, int b0) {
				char[] c0 = tr[b0];
				int len = e + px.length() + c0.length + sx.length();
				char[] a = new char[len];
				c1(a, e, c0);
				return a;
			}

			@Override
			public final Character infix() {
				return null;
			}
		}

		private static abstract class AbstractUntruncated extends AbstractFormat implements PerByteFormat {

			final int digits;
			final int n8, n16, n32, n64;
			final int len8, len16, len32, len64;

			private AbstractUntruncated(char[][] tr, char[][] td, String px, String sx, String alphabet, String prefix, String suffix, int iv, int digits, int n8, int n16, int n32, int n64) {
				super(tr, td, px, sx, alphabet, prefix, suffix, iv);
				this.digits = digits;
				int len = px.length() + sx.length();
				this.n8 = n8;
				this.n16 = n16;
				this.n32 = n32;
				this.n64 = n64;
				this.len8 = len + digits + n8;
				this.len16 = len + (digits << 1) + n16;
				this.len32 = len + (digits << 2) + n32;
				this.len64 = len + (digits << 3) + n64;
			}

			abstract void c8(char[] a, int off, int i);

			abstract void c16(char[] a, int off, int i);

			abstract void c32(char[] a, int off, int i);

			abstract void c64(char[] a, int off, int i1, int i0);

			@Override
			public final int copy8(ArrayTarget t, int e, int off, int i) {
				int len = e + len8;
				char[] a = t.getArray(off, len);
				a[off] = '-';
				c8(a, off + e, i);
				return len;
			}

			@Override
			public final int copy16(ArrayTarget t, int e, int off, int i) {
				int len = e + len16;
				char[] a = t.getArray(off, len);
				a[off] = '-';
				c16(a, off + e, i);
				return len;
			}

			@Override
			public final int copy32(ArrayTarget t, int e, int off, int i) {
				int len = e + len32;
				char[] a = t.getArray(off, len);
				a[off] = '-';
				c32(a, off + e, i);
				return len;
			}

			@Override
			public final int copy64(ArrayTarget t, int e, int off, long i) {
				int i1 = (int) (i >>> 32);
				int i0 = (int) i;
				int len = e + len64;
				char[] a = t.getArray(off, len);
				a[off] = '-';
				c64(a, off + e, i1, i0);
				return len;
			}

			@Override
			public final char[] copy8(int e, int i) {
				char[] a = new char[e + len8];
				c8(a, e, i);
				return a;
			}

			@Override
			public final char[] copy16(int e, int i) {
				char[] a = new char[e + len16];
				c16(a, e, i);
				return a;
			}

			@Override
			public final char[] copy32(int e, int i) {
				char[] a = new char[e + len32];
				c32(a, e, i);
				return a;
			}

			@Override
			public final char[] copy64(int e, long i) {
				int i1 = (int) (i >>> 32);
				int i0 = (int) i;
				char[] a = new char[e + len64];
				c64(a, e, i1, i0);
				return a;
			}

			@Override
			public final boolean isTruncated() {
				return false;
			}

			@Override
			public final Format untruncated() {
				return this;
			}
		}

		private static abstract class AbstractUntruncatedInfix extends AbstractUntruncated {

			final char ix;
			final Character infix;

			private AbstractUntruncatedInfix(char[][] tr, char[][] td, String px, char ix, String sx, String alphabet, String prefix, Character infix, String suffix, int iv, int digits) {
				super(tr, td, px, sx, alphabet, prefix, suffix, iv, digits,
						(digits - 1) / iv,
						((digits << 1) - 1) / iv,
						((digits << 2) - 1) / iv,
						((digits << 3) - 1) / iv
				);
				this.ix = ix;
				this.infix = infix;
			}

			@Override
			final void c8(char[] a, int off, int i) {
				px.getChars(0, px.length(), a, off);
				int x = PerByte.copy1Byte(a, off + px.length(), td[i]);
				int y = x + n8;
				sx.getChars(0, sx.length(), a, y);
				Formats.spreadInfix(a, x, y, ix, iv);
			}

			@Override
			final void c16(char[] a, int off, int i) {
				px.getChars(0, px.length(), a, off);
				int x = PerByte.copy2Bytes(td, a, off + px.length(),
						td[i >>> 8], i & 0xff, digits);
				int y = x + n16;
				sx.getChars(0, sx.length(), a, y);
				Formats.spreadInfix(a, x, y, ix, iv);
			}

			@Override
			final void c32(char[] a, int off, int i) {
				px.getChars(0, px.length(), a, off);
				int x = PerByte.copy4Bytes(td, a, off + px.length(),
						td[i >>> 24], (i >>> 16) & 0xff, (i >>> 8) & 0xff, i & 0xff, digits);
				int y = x + n32;
				sx.getChars(0, sx.length(), a, y);
				Formats.spreadInfix(a, x, y, ix, iv);
			}

			@Override
			final void c64(char[] a, int off, int i1, int i0) {
				px.getChars(0, px.length(), a, off);
				int x = PerByte.copy8Bytes(td, a, off + px.length(),
						td[i1 >>> 24], (i1 >>> 16) & 0xff, (i1 >>> 8) & 0xff, i1 & 0xff,
						i0 >>> 24, (i0 >>> 16) & 0xff, (i0 >>> 8) & 0xff, i0 & 0xff, digits);
				int y = x + n64;
				sx.getChars(0, sx.length(), a, y);
				Formats.spreadInfix(a, x, y, ix, iv);
			}

			@Override
			public final Character infix() {
				return infix;
			}
		}

		private static abstract class AbstractUntruncatedNoInfix extends AbstractUntruncated {

			private AbstractUntruncatedNoInfix(char[][] tr, char[][] td, String px, String sx, String alphabet, String prefix, String suffix, int iv, int digits) {
				super(tr, td, px, sx, alphabet, prefix, suffix, iv, digits, 0, 0, 0, 0);
			}

			@Override
			final void c8(char[] a, int off, int i) {
				px.getChars(0, px.length(), a, off);
				off = PerByte.copy1Byte(a, off + px.length(), td[i]);
				sx.getChars(0, sx.length(), a, off);
			}

			@Override
			final void c16(char[] a, int off, int i) {
				px.getChars(0, px.length(), a, off);
				off = PerByte.copy2Bytes(td, a, off + px.length(),
						td[i >>> 8], i & 0xff, digits);
				sx.getChars(0, sx.length(), a, off);
			}

			@Override
			final void c32(char[] a, int off, int i) {
				px.getChars(0, px.length(), a, off);
				off = PerByte.copy4Bytes(td, a, off + px.length(),
						td[i >>> 24], (i >>> 16) & 0xff, (i >>> 8) & 0xff, i & 0xff, digits);
				sx.getChars(0, sx.length(), a, off);
			}

			@Override
			final void c64(char[] a, int off, int i1, int i0) {
				px.getChars(0, px.length(), a, off);
				off = PerByte.copy8Bytes(td, a, off + px.length(),
						td[i1 >>> 24], (i1 >>> 16) & 0xff, (i1 >>> 8) & 0xff, i1 & 0xff,
						i0 >>> 24, (i0 >>> 16) & 0xff, (i0 >>> 8) & 0xff, i0 & 0xff, digits);
				sx.getChars(0, sx.length(), a, off);
			}

			@Override
			public final Character infix() {
				return null;
			}
		}

		private static final class SignedTruncatedInfix extends AbstractTruncatedInfix implements Signed {

			private SignedTruncatedInfix(char[][] tr, char[][] td, String px, char ix, String sx, String alphabet, String prefix, Character infix, String suffix, int iv, int dig1, int dig2, int dig3, int dig4, int dig5, int dig6, int dig7) {
				super(tr, td, px, ix, sx, alphabet, prefix, infix, suffix, iv, dig1, dig2, dig3, dig4, dig5, dig6, dig7);
			}

			private SignedTruncatedInfix(char[][] tr, char[][] td, String px, char ix, String sx, String alphabet, String prefix, Character infix, String suffix, int iv, int dig1) {
				super(tr, td, px, ix, sx, alphabet, prefix, infix, suffix, iv, dig1);
			}

			@Override
			public Format interval(int interval) {
				if (interval == this.iv) {
					return this;
				}
				if (interval < 1) {
					throw new IllegalArgumentException("interval must be positive: " + interval);
				}
				return new SignedTruncatedInfix(tr, td, px, ix, sx, alphabet, prefix, infix, suffix, interval, dig1, dig2, dig3, dig4, dig5, dig6, dig7);
			}

			@Override
			public Format prefix(String prefix) {
				String dat = stringData(prefix);
				return px.equals(dat) ? this : new SignedTruncatedInfix(tr, td, dat, ix, sx, alphabet, prefix, infix, suffix, iv, dig1, dig2, dig3, dig4, dig5, dig6, dig7);
			}

			@Override
			public Format suffix(String suffix) {
				String dat = stringData(suffix);
				return sx.equals(dat) ? this : new SignedTruncatedInfix(tr, td, px, ix, dat, alphabet, prefix, infix, suffix, iv, dig1, dig2, dig3, dig4, dig5, dig6, dig7);
			}

			@Override
			public Format infix(Character infix) {
				return infix == null
						? new SignedTruncatedNoInfix(tr, td, px, sx, alphabet, prefix, suffix, iv, dig1, dig2, dig3, dig4, dig5, dig6, dig7)
						: ix == infix ? this : new SignedTruncatedInfix(tr, td, px, infix, sx, alphabet, prefix, infix, suffix, iv, dig1, dig2, dig3, dig4, dig5, dig6, dig7);
			}

			@Override
			public Format unsigned() {
				return new UnsignedTruncatedInfix(tr, td, px, ix, sx, alphabet, prefix, infix, suffix, iv, dig1, dig2, dig3, dig4, dig5, dig6, dig7);
			}

			@Override
			public Format untruncated() {
				return new SignedUntruncatedInfix(tr, td, px, ix, sx, alphabet, prefix, infix, suffix, iv, dig1);
			}
		}

		private static final class SignedTruncatedNoInfix extends AbstractTruncatedNoInfix implements Signed {

			private SignedTruncatedNoInfix(char[][] tr, char[][] td, String px, String sx, String alphabet, String prefix, String suffix, int iv, int dig1, int dig2, int dig3, int dig4, int dig5, int dig6, int dig7) {
				super(tr, td, px, sx, alphabet, prefix, suffix, iv, dig1, dig2, dig3, dig4, dig5, dig6, dig7);
			}

			private SignedTruncatedNoInfix(char[][] tr, char[][] td, String px, String sx, String alphabet, String prefix, String suffix, int iv, int dig1) {
				super(tr, td, px, sx, alphabet, prefix, suffix, iv, dig1);
			}

			@Override
			public Format interval(int interval) {
				if (interval == this.iv) {
					return this;
				}
				if (interval < 1) {
					throw new IllegalArgumentException("interval must be positive: " + interval);
				}
				return new SignedTruncatedNoInfix(tr, td, px, sx, alphabet, prefix, suffix, interval, dig1, dig2, dig3, dig4, dig5, dig6, dig7);
			}

			@Override
			public Format prefix(String prefix) {
				String dat = stringData(prefix);
				return px.equals(dat) ? this : new SignedTruncatedNoInfix(tr, td, dat, sx, alphabet, prefix, suffix, iv, dig1, dig2, dig3, dig4, dig5, dig6, dig7);
			}

			@Override
			public Format suffix(String suffix) {
				String dat = stringData(suffix);
				return sx.equals(dat) ? this : new SignedTruncatedNoInfix(tr, td, px, dat, alphabet, prefix, suffix, iv, dig1, dig2, dig3, dig4, dig5, dig6, dig7);
			}

			@Override
			public Format infix(Character infix) {
				return infix == null
						? this
						: new SignedTruncatedInfix(tr, td, px, infix, sx, alphabet, prefix, infix, suffix, iv, dig1, dig2, dig3, dig4, dig5, dig6, dig7);
			}

			@Override
			public Format unsigned() {
				return new UnsignedTruncatedNoInfix(tr, td, px, sx, alphabet, prefix, suffix, iv, dig1, dig2, dig3, dig4, dig5, dig6, dig7);
			}

			@Override
			public Format untruncated() {
				return new SignedUntruncatedNoInfix(tr, td, px, sx, alphabet, prefix, suffix, iv, dig1);
			}
		}

		private static final class SignedUntruncatedInfix extends AbstractUntruncatedInfix implements Signed {

			private SignedUntruncatedInfix(char[][] tr, char[][] td, String px, char ix, String sx, String alphabet, String prefix, Character infix, String suffix, int iv, int digits) {
				super(tr, td, px, ix, sx, alphabet, prefix, infix, suffix, iv, digits);
			}

			@Override
			public Format interval(int interval) {
				if (interval == this.iv) {
					return this;
				}
				if (interval < 1) {
					throw new IllegalArgumentException("interval must be positive: " + interval);
				}
				return new SignedUntruncatedInfix(tr, td, px, ix, sx, alphabet, prefix, infix, suffix, interval, digits);
			}

			@Override
			public Format prefix(String prefix) {
				String dat = stringData(prefix);
				return px.equals(dat) ? this : new SignedUntruncatedInfix(tr, td, dat, ix, sx, alphabet, prefix, infix, suffix, iv, digits);
			}

			@Override
			public Format suffix(String suffix) {
				String dat = stringData(suffix);
				return sx.equals(dat) ? this : new SignedUntruncatedInfix(tr, td, px, ix, dat, alphabet, prefix, infix, suffix, iv, digits);
			}

			@Override
			public Format infix(Character infix) {
				return infix == null
						? new SignedUntruncatedNoInfix(tr, td, px, sx, alphabet, prefix, suffix, iv, digits)
						: ix == infix ? this : new SignedUntruncatedInfix(tr, td, px, infix, sx, alphabet, prefix, infix, suffix, iv, digits);
			}

			@Override
			public Format unsigned() {
				return new UnsignedUntruncatedInfix(tr, td, px, ix, sx, alphabet, prefix, infix, suffix, iv, digits);
			}

			@Override
			public Format truncated() {
				return new SignedTruncatedInfix(tr, td, px, ix, sx, alphabet, prefix, infix, suffix, iv, digits);
			}
		}

		private static final class SignedUntruncatedNoInfix extends AbstractUntruncatedNoInfix implements Signed {

			private SignedUntruncatedNoInfix(char[][] tr, char[][] td, String px, String sx, String alphabet, String prefix, String suffix, int iv, int digits) {
				super(tr, td, px, sx, alphabet, prefix, suffix, iv, digits);
			}

			@Override
			public Format interval(int interval) {
				if (interval == this.iv) {
					return this;
				}
				if (interval < 1) {
					throw new IllegalArgumentException("interval must be positive: " + interval);
				}
				return new SignedUntruncatedNoInfix(tr, td, px, sx, alphabet, prefix, suffix, interval, digits);
			}

			@Override
			public Format prefix(String prefix) {
				String dat = stringData(prefix);
				return px.equals(dat) ? this : new SignedUntruncatedNoInfix(tr, td, dat, sx, alphabet, prefix, suffix, iv, digits);
			}

			@Override
			public Format suffix(String suffix) {
				String dat = stringData(suffix);
				return sx.equals(dat) ? this : new SignedUntruncatedNoInfix(tr, td, px, dat, alphabet, prefix, suffix, iv, digits);
			}

			@Override
			public Format infix(Character infix) {
				return infix == null
						? this
						: new SignedUntruncatedInfix(tr, td, px, infix, sx, alphabet, prefix, infix, suffix, iv, digits);
			}

			@Override
			public Format unsigned() {
				return new UnsignedUntruncatedNoInfix(tr, td, px, sx, alphabet, prefix, suffix, iv, digits);
			}

			@Override
			public Format truncated() {
				return new SignedTruncatedNoInfix(tr, td, px, sx, alphabet, prefix, suffix, iv, digits);
			}
		}

		private static final class UnsignedTruncatedInfix extends AbstractTruncatedInfix implements Unsigned {

			private UnsignedTruncatedInfix(char[][] tr, char[][] td, String px, char ix, String sx, String alphabet, String prefix, Character infix, String suffix, int iv, int dig1, int dig2, int dig3, int dig4, int dig5, int dig6, int dig7) {
				super(tr, td, px, ix, sx, alphabet, prefix, infix, suffix, iv, dig1, dig2, dig3, dig4, dig5, dig6, dig7);
			}

			private UnsignedTruncatedInfix(char[][] tr, char[][] td, String px, char ix, String sx, String alphabet, String prefix, Character infix, String suffix, int iv, int dig1) {
				super(tr, td, px, ix, sx, alphabet, prefix, infix, suffix, iv, dig1);
			}

			@Override
			public Format interval(int interval) {
				if (interval == this.iv) {
					return this;
				}
				if (interval < 1) {
					throw new IllegalArgumentException("interval must be positive: " + interval);
				}
				return new UnsignedTruncatedInfix(tr, td, px, ix, sx, alphabet, prefix, infix, suffix, interval, dig1, dig2, dig3, dig4, dig5, dig6, dig7);
			}

			@Override
			public Format prefix(String prefix) {
				String dat = stringData(prefix);
				return px.equals(dat) ? this : new UnsignedTruncatedInfix(tr, td, dat, ix, sx, alphabet, prefix, infix, suffix, iv, dig1, dig2, dig3, dig4, dig5, dig6, dig7);
			}

			@Override
			public Format suffix(String suffix) {
				String dat = stringData(suffix);
				return sx.equals(dat) ? this : new UnsignedTruncatedInfix(tr, td, px, ix, dat, alphabet, prefix, infix, suffix, iv, dig1, dig2, dig3, dig4, dig5, dig6, dig7);
			}

			@Override
			public Format infix(Character infix) {
				return infix == null
						? new UnsignedTruncatedNoInfix(tr, td, px, sx, alphabet, prefix, suffix, iv, dig1, dig2, dig3, dig4, dig5, dig6, dig7)
						: ix == infix ? this : new UnsignedTruncatedInfix(tr, td, px, infix, sx, alphabet, prefix, infix, suffix, iv, dig1, dig2, dig3, dig4, dig5, dig6, dig7);
			}

			@Override
			public Format signed() {
				return new SignedTruncatedInfix(tr, td, px, ix, sx, alphabet, prefix, infix, suffix, iv, dig1, dig2, dig3, dig4, dig5, dig6, dig7);
			}

			@Override
			public Format untruncated() {
				return new UnsignedUntruncatedInfix(tr, td, px, ix, sx, alphabet, prefix, infix, suffix, iv, dig1);
			}
		}

		private static final class UnsignedTruncatedNoInfix extends AbstractTruncatedNoInfix implements Unsigned {

			private UnsignedTruncatedNoInfix(char[][] tr, char[][] td, String px, String sx, String alphabet, String prefix, String suffix, int iv, int dig1, int dig2, int dig3, int dig4, int dig5, int dig6, int dig7) {
				super(tr, td, px, sx, alphabet, prefix, suffix, iv, dig1, dig2, dig3, dig4, dig5, dig6, dig7);
			}

			private UnsignedTruncatedNoInfix(char[][] tr, char[][] td, String px, String sx, String alphabet, String prefix, String suffix, int iv, int dig1) {
				super(tr, td, px, sx, alphabet, prefix, suffix, iv, dig1);
			}

			@Override
			public Format interval(int interval) {
				if (interval == this.iv) {
					return this;
				}
				if (interval < 1) {
					throw new IllegalArgumentException("interval must be positive: " + interval);
				}
				return new UnsignedTruncatedNoInfix(tr, td, px, sx, alphabet, prefix, suffix, interval, dig1, dig2, dig3, dig4, dig5, dig6, dig7);
			}

			@Override
			public Format prefix(String prefix) {
				String dat = stringData(prefix);
				return px.equals(dat) ? this : new UnsignedTruncatedNoInfix(tr, td, dat, sx, alphabet, prefix, suffix, iv, dig1, dig2, dig3, dig4, dig5, dig6, dig7);
			}

			@Override
			public Format suffix(String suffix) {
				String dat = stringData(suffix);
				return sx.equals(dat) ? this : new UnsignedTruncatedNoInfix(tr, td, px, dat, alphabet, prefix, suffix, iv, dig1, dig2, dig3, dig4, dig5, dig6, dig7);
			}

			@Override
			public Format infix(Character infix) {
				return infix == null
						? this
						: new UnsignedTruncatedInfix(tr, td, px, infix, sx, alphabet, prefix, infix, suffix, iv, dig1, dig2, dig3, dig4, dig5, dig6, dig7);
			}

			@Override
			public Format signed() {
				return new SignedTruncatedNoInfix(tr, td, px, sx, alphabet, prefix, suffix, iv, dig1, dig2, dig3, dig4, dig5, dig6, dig7);
			}

			@Override
			public Format untruncated() {
				return new UnsignedUntruncatedNoInfix(tr, td, px, sx, alphabet, prefix, suffix, iv, dig1);
			}
		}

		private static final class UnsignedUntruncatedInfix extends AbstractUntruncatedInfix implements Unsigned {

			private UnsignedUntruncatedInfix(char[][] tr, char[][] td, String px, char ix, String sx, String alphabet, String prefix, Character infix, String suffix, int iv, int digits) {
				super(tr, td, px, ix, sx, alphabet, prefix, infix, suffix, iv, digits);
			}

			@Override
			public Format interval(int interval) {
				if (interval == this.iv) {
					return this;
				}
				if (interval < 1) {
					throw new IllegalArgumentException("interval must be positive: " + interval);
				}
				return new UnsignedUntruncatedInfix(tr, td, px, ix, sx, alphabet, prefix, infix, suffix, interval, digits);
			}

			@Override
			public Format prefix(String prefix) {
				String dat = stringData(prefix);
				return px.equals(dat) ? this : new UnsignedUntruncatedInfix(tr, td, dat, ix, sx, alphabet, prefix, infix, suffix, iv, digits);
			}

			@Override
			public Format suffix(String suffix) {
				String dat = stringData(suffix);
				return sx.equals(dat) ? this : new UnsignedUntruncatedInfix(tr, td, px, ix, dat, alphabet, prefix, infix, suffix, iv, digits);
			}

			@Override
			public Format infix(Character infix) {
				return infix == null
						? new UnsignedUntruncatedNoInfix(tr, td, px, sx, alphabet, prefix, suffix, iv, digits)
						: ix == infix ? this : new UnsignedUntruncatedInfix(tr, td, px, infix, sx, alphabet, prefix, infix, suffix, iv, digits);
			}

			@Override
			public Format signed() {
				return new SignedUntruncatedInfix(tr, td, px, ix, sx, alphabet, prefix, infix, suffix, iv, digits);
			}

			@Override
			public Format truncated() {
				return new UnsignedTruncatedInfix(tr, td, px, ix, sx, alphabet, prefix, infix, suffix, iv, digits);
			}
		}

		private static final class UnsignedUntruncatedNoInfix extends AbstractUntruncatedNoInfix implements Unsigned {

			private UnsignedUntruncatedNoInfix(char[][] tr, char[][] td, String px, String sx, String alphabet, String prefix, String suffix, int iv, int digits) {
				super(tr, td, px, sx, alphabet, prefix, suffix, iv, digits);
			}

			@Override
			public Format interval(int interval) {
				if (interval == this.iv) {
					return this;
				}
				if (interval < 1) {
					throw new IllegalArgumentException("interval must be positive: " + interval);
				}
				return new UnsignedUntruncatedNoInfix(tr, td, px, sx, alphabet, prefix, suffix, interval, digits);
			}

			@Override
			public Format prefix(String prefix) {
				String dat = stringData(prefix);
				return px.equals(dat) ? this : new UnsignedUntruncatedNoInfix(tr, td, dat, sx, alphabet, prefix, suffix, iv, digits);
			}

			@Override
			public Format suffix(String suffix) {
				String dat = stringData(suffix);
				return sx.equals(dat) ? this : new UnsignedUntruncatedNoInfix(tr, td, px, dat, alphabet, prefix, suffix, iv, digits);
			}

			@Override
			public Format infix(Character infix) {
				if (infix == null) {
					return this;
				}
				return new UnsignedUntruncatedInfix(tr, td, px, infix, sx, alphabet, prefix, infix, suffix, iv, digits);
			}

			@Override
			public Format signed() {
				return new SignedUntruncatedNoInfix(tr, td, px, sx, alphabet, prefix, suffix, iv, digits);
			}

			@Override
			public Format truncated() {
				return new UnsignedTruncatedNoInfix(tr, td, px, sx, alphabet, prefix, suffix, iv, digits);
			}
		}
	}
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util;

import java.lang.reflect.Constructor;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * High-performance byte-based operations, offering significant speed-up
 * potential for performance-critical code.
 * <p>
 * <strong>Always include your own bounds-checks before calling any of these
 * methods!</strong> Otherwise there is a real risk to read / write
 * out-of-bounds memory.
 * <p>
 * Uses sun.misc.Unsafe under the hood, but will fall back on an alternate
 * implementation if access fails.
 * 
 * @author Jan Kebernik
 */
@SuppressWarnings("UseSpecificCatch")
abstract class FastBytes {

	private static final sun.misc.Unsafe UNSAFE;
	private static final long BYTE_ARRAY_BASE_OFFSET;
	static final FastBytes INSTANCE;
	static {
		FastBytes bytes = null;
		sun.misc.Unsafe u = null;
		long byteOff = 0;
		try {
			// get new instance via constructor instead of relying on named field (better compatibility with various runtimes).
			Constructor<sun.misc.Unsafe> con = sun.misc.Unsafe.class.getDeclaredConstructor();
			con.setAccessible(true);
			u = con.newInstance();
			byteOff = u.arrayBaseOffset(byte[].class);
			// determine byte order used by Unsafe, in case it even matters (unsure)
			byte[] testArray = {
				(byte) 0x88, (byte) 0x77, (byte) 0x66, (byte) 0x55,
				(byte) 0x44, (byte) 0x33, (byte) 0x22, (byte) 0x11};
			long testOrder = u.getLong(testArray, byteOff);
			if (testOrder == 0x1122334455667788L) {
				bytes = new BytesUnsafeLE();
			} else if (testOrder == 0x88776655332211L) {
				// probably never ever the case. But it doesn't hurt to make sure.
				bytes = new BytesUnsafeBE();
			}
			// if "bytes" is not set to an unsafe version, then
			// "UNSAFE" and "BYTE_ARRAY_BASE_OFFSET" will never be accessed.
		} catch (Error err) {
			// don't swallow serious errors
			throw err;
		} catch (Throwable ex) {
			// recoverable
			Logger.getLogger(FastBytes.class.getName()).log(Level.WARNING, "Could not access java.misc.Unsafe. Falling back on conventional solution.", ex);
		}
		INSTANCE = bytes != null ? bytes : new BytesSafe();
		UNSAFE = u;
		BYTE_ARRAY_BASE_OFFSET = byteOff;
	}

	private FastBytes() {
	}

	abstract char getCharLE(byte[] b, int off);
	abstract char getCharBE(byte[] b, int off);

	abstract short getShortLE(byte[] b, int off);
	abstract short getShortBE(byte[] b, int off);

	abstract int getIntLE(byte[] b, int off);
	abstract int getIntBE(byte[] b, int off);

	float getFloatLE(byte[] b, int off) {
		return Float.intBitsToFloat(getIntLE(b, off));
	}
	float getFloatBE(byte[] b, int off) {
		return Float.intBitsToFloat(getIntBE(b, off));
	}

	abstract long getLongLE(byte[] b, int off);
	abstract long getLongBE(byte[] b, int off);

	double getDoubleLE(byte[] b, int off) {
		return Double.longBitsToDouble(getLongLE(b, off));
	}
	double getDoubleBE(byte[] b, int off) {
		return Double.longBitsToDouble(getLongBE(b, off));
	}

	abstract void putCharLE(char n, byte[] b, int off);
	abstract void putCharBE(char n, byte[] b, int off);

	abstract void putShortLE(short n, byte[] b, int off);
	abstract void putShortBE(short n, byte[] b, int off);

	abstract void putIntLE(int n, byte[] b, int off);
	abstract void putIntBE(int n, byte[] b, int off);

	void putFloatLE(float n, byte[] b, int off) {
		putIntLE(Float.floatToRawIntBits(n), b, off);
	}
	void putFloatBE(float n, byte[] b, int off) {
		putIntBE(Float.floatToRawIntBits(n), b, off);
	}

	abstract void putLongLE(long n, byte[] b, int off);
	abstract void putLongBE(long n, byte[] b, int off);

	void putDoubleLE(double n, byte[] b, int off) {
		putLongLE(Double.doubleToRawLongBits(n), b, off);
	}
	void putDoubleBE(double n, byte[] b, int off) {
		putLongBE(Double.doubleToRawLongBits(n), b, off);
	}

	// return number of leading matching bytes (0..len) in both arrays, starting
	// at the respective offsets. does not perform any bounds-checks!
	abstract int matchLeading(byte[] a, int offA, byte[] b, int offB, int len);

	// fills the specified array range with 0s
	abstract void fillWithZeros(byte[] a, int off, int len);

	// fills the specified array range with the specified byte value
	abstract void fill(byte[] a, int off, int len, byte n);

	private static abstract class BytesUnsafe extends FastBytes {

		// returns the number of matching leading bytes, where leading
		// means bytes with a lower index in the array a long was read from.
		// so, if the long was read as a little endian value, then the higher order
		// bits in java form the lower indexed bytes, while both simply correspond
		// if the long was read as a big endian value.
		abstract int numLeadingBytesMatch(long a, long b);

		// This is about 3-6 times faster than the alternative, 
		// approaching through-put of up to ~15 GB/s on middling hardware, 
		// at least when the the runs are small enough to fit into faster 
		// CPU cache (I guess!). 
		// Making this a boolean equality check brings almost no gains.
		// Which means that memory speed is the most likely bottleneck.
		// 
		// matches the bytes from both arrays. returns number of matching bytes.
		@Override
		final int matchLeading(byte[] a, int offA, byte[] b, int offB, int len) {
			long _offA00 = BYTE_ARRAY_BASE_OFFSET + offA;
			long _offB00 = BYTE_ARRAY_BASE_OFFSET + offB;
			long _maxA = _offA00 + len;
			// compare 64 (2*8*4) bytes in parallel (JIT generally does not unroll long-based loops)
			for (long m = _maxA - 31L; _offA00 < m; _offA00 += 32L, _offB00 += 32L) {
				// memoize offsets
				long _offA08 = _offA00 + 8L;
				long _offA16 = _offA00 + 16L;
				long _offA24 = _offA00 + 24L;
				// read 32 bytes from array a
				long _a0 = UNSAFE.getLong(a, _offA00);
				long _a1 = UNSAFE.getLong(a, _offA08);
				long _a2 = UNSAFE.getLong(a, _offA16);
				long _a3 = UNSAFE.getLong(a, _offA24);
				// read 32 bytes from array b
				long _b0 = UNSAFE.getLong(b, _offB00);
				long _b1 = UNSAFE.getLong(b, _offB00 + 8L);
				long _b2 = UNSAFE.getLong(b, _offB00 + 16L);
				long _b3 = UNSAFE.getLong(b, _offB00 + 24L);
				// compare each block
				if (_a0 != _b0) {
					return (int) (_offA00 + len - _maxA) + numLeadingBytesMatch(_a0, _b0);
				}
				if (_a1 != _b1) {
					return (int) (_offA08 + len - _maxA) + numLeadingBytesMatch(_a1, _b1);
				}
				if (_a2 != _b2) {
					return (int) (_offA16 + len - _maxA) + numLeadingBytesMatch(_a2, _b2);
				}
				if (_a3 != _b3) {
					return (int) (_offA24 + len - _maxA) + numLeadingBytesMatch(_a3, _b3);
				}
			}
			// compare 16 (2*8) bytes in parallel
			for (long m = _maxA - 7L; _offA00 < m; _offA00 += 8L, _offB00 += 8L) {
				long _a0 = UNSAFE.getLong(a, _offA00);
				long _b0 = UNSAFE.getLong(b, _offB00);
				if (_a0 != _b0) {
					return (int) (_offA00 + len - _maxA) + numLeadingBytesMatch(_a0, _b0);
				}
			}
			// compare 2 (2*1) bytes in parallel using regular array access (faster than using Unsafe).
			// restore regular offsets
			offA = (int) (_offA00 - BYTE_ARRAY_BASE_OFFSET);
			offB = (int) (_offB00 - BYTE_ARRAY_BASE_OFFSET);
			for (int m = (int) (_maxA - BYTE_ARRAY_BASE_OFFSET); offA < m; offA++, offB++) {
				if (a[offA] != b[offB]) {
					return offA + len - m;
				}
			}
			return len;
		}

		// TODO needs speed testing
		private void _fillWith(byte[] a, int off, int len, long x, byte n) {
			long _off = BYTE_ARRAY_BASE_OFFSET + off;
			long _max = _off + len;
			// fill 32 (4*8) bytes in parallel (JIT generally does not unroll long-based loops)
			for (long m = _max - 31L; _off < m; _off += 32L) {
				UNSAFE.putLong(a, _off, x);
				UNSAFE.putLong(a, _off + 8L, x);
				UNSAFE.putLong(a, _off + 16L, x);
				UNSAFE.putLong(a, _off + 24L, x);
			}
			// fill 8 (1*8) bytes
			for (long m = _max - 7L; _off < m; _off += 8L) {
				UNSAFE.putLong(a, _off, x);
			}
			// restore regular offsets and do a regular loop
			off = (int) (_off - BYTE_ARRAY_BASE_OFFSET);
			for (int m = (int) (_max - BYTE_ARRAY_BASE_OFFSET); off < m; off++) {
				a[off] = n;
			}
		}

		private void _fillNormally(byte[] a, int off, int len, byte n) {
			for (int m = off + len; off < m; off++) {
				a[off] = n;
			}
		}

		@Override
		final void fillWithZeros(byte[] a, int off, int len) {
			if (len > 63) {
				_fillWith(a, off, len, 0L, (byte) 0);
			} else {
				_fillNormally(a, off, len, (byte) 0);
			}
		}

		@Override
		final void fill(byte[] a, int off, int len, byte n) {
			if (len > 63) {
				long x = n & 0xffL;
				x |= x << 8;
				x |= x << 16;
				x |= x << 32;
				_fillWith(a, off, len, x, n);
			} else {
				_fillNormally(a, off, len, n);
			}
		}
	}

	private static final class BytesSafe extends FastBytes {

		@Override
		char getCharLE(byte[] b, int off) {
			return (char) ((b[off + 1] << 8) | (b[off + 0] & 0xff));
		}
		@Override
		char getCharBE(byte[] b, int off) {
			return (char) ((b[off + 0] << 8) | (b[off + 1] & 0xff));
		}
		@Override
		short getShortLE(byte[] b, int off) {
			return (short) ((b[off + 1] << 8) | (b[off + 0] & 0xff));
		}
		@Override
		short getShortBE(byte[] b, int off) {
			return (short) ((b[off + 0] << 8) | (b[off + 1] & 0xff));
		}
		@Override
		int getIntLE(byte[] b, int off) {
			return (b[off + 3] << 24)
					| ((b[off + 2] & 0xff) << 16)
					| ((b[off + 1] & 0xff) << 8)
					| (b[off + 0] & 0xff);
		}
		@Override
		int getIntBE(byte[] b, int off) {
			return (b[off + 0] << 24)
					| ((b[off + 1] & 0xff) << 16)
					| ((b[off + 2] & 0xff) << 8)
					| (b[off + 3] & 0xff);
		}
		@Override
		long getLongLE(byte[] b, int off) {
			return (((b[off + 7] << 8) | (b[off + 6] & 0xffL)) << 48)
					| ((b[off + 5] & 0xffL) << 40)
					| ((b[off + 4] & 0xffL) << 32)
					| ((b[off + 3] & 0xffL) << 24)
					| ((b[off + 2] & 0xffL) << 16)
					| ((b[off + 1] & 0xffL) << 8)
					| (b[off + 0] & 0xffL);
		}
		@Override
		long getLongBE(byte[] b, int off) {
			return (((b[off + 0] << 8) | (b[off + 1] & 0xffL)) << 48)
					| ((b[off + 2] & 0xffL) << 40)
					| ((b[off + 3] & 0xffL) << 32)
					| ((b[off + 4] & 0xffL) << 24)
					| ((b[off + 5] & 0xffL) << 16)
					| ((b[off + 6] & 0xffL) << 8)
					| (b[off + 7] & 0xffL);
		}
		@Override
		void putCharLE(char n, byte[] b, int off) {
			b[off + 0] = (byte) (n);
			b[off + 1] = (byte) (n >>> 8);
		}
		@Override
		void putCharBE(char n, byte[] b, int off) {
			b[off + 0] = (byte) (n >>> 8);
			b[off + 1] = (byte) (n);
		}
		@Override
		void putShortLE(short n, byte[] b, int off) {
			b[off + 0] = (byte) (n);
			b[off + 1] = (byte) (n >>> 8);
		}
		@Override
		void putShortBE(short n, byte[] b, int off) {
			b[off + 0] = (byte) (n >>> 8);
			b[off + 1] = (byte) (n);
		}
		@Override
		void putIntLE(int n, byte[] b, int off) {
			b[off + 0] = (byte) (n);
			b[off + 1] = (byte) (n >>> 8);
			b[off + 2] = (byte) (n >>> 16);
			b[off + 3] = (byte) (n >>> 24);
		}
		@Override
		void putIntBE(int n, byte[] b, int off) {
			b[off + 0] = (byte) (n >>> 24);
			b[off + 1] = (byte) (n >>> 16);
			b[off + 2] = (byte) (n >>> 8);
			b[off + 3] = (byte) (n);
		}
		@Override
		void putLongLE(long n, byte[] b, int off) {
			b[off + 0] = (byte) (n);
			b[off + 1] = (byte) (n >>> 8);
			b[off + 2] = (byte) (n >>> 16);
			b[off + 3] = (byte) (n >>> 24);
			b[off + 4] = (byte) (n >>> 32);
			b[off + 5] = (byte) (n >>> 40);
			b[off + 6] = (byte) (n >>> 48);
			b[off + 7] = (byte) (n >>> 56);
		}
		@Override
		void putLongBE(long n, byte[] b, int off) {
			b[off + 0] = (byte) (n >>> 56);
			b[off + 1] = (byte) (n >>> 48);
			b[off + 2] = (byte) (n >>> 40);
			b[off + 3] = (byte) (n >>> 32);
			b[off + 4] = (byte) (n >>> 24);
			b[off + 5] = (byte) (n >>> 16);
			b[off + 6] = (byte) (n >>> 8);
			b[off + 7] = (byte) (n);
		}
		@Override
		int matchLeading(byte[] a, int offA, byte[] b, int offB, int len) {
			// let JIT unroll int-based loop
			for (int m = offA + len; offA < m; offA++, offB++) {
				if (a[offA] != b[offB]) {
					return offA + len - m;
				}
			}
			return len;
		}
		@Override
		void fillWithZeros(byte[] a, int off, int len) {
			for (int m = off + len; off < m; off++) {
				a[off] = 0;
			}
		}
		@Override
		void fill(byte[] a, int off, int len, byte n) {
			for (int m = off + len; off < m; off++) {
				a[off] = n;
			}
		}
	}

	private static final class BytesUnsafeLE extends BytesUnsafe {

		@Override
		char getCharLE(byte[] b, int off) {
			return UNSAFE.getChar(b, BYTE_ARRAY_BASE_OFFSET + off);
		}
		@Override
		short getShortLE(byte[] b, int off) {
			return UNSAFE.getShort(b, BYTE_ARRAY_BASE_OFFSET + off);
		}
		@Override
		int getIntLE(byte[] b, int off) {
			return UNSAFE.getInt(b, BYTE_ARRAY_BASE_OFFSET + off);
		}
		@Override
		float getFloatLE(byte[] b, int off) {
			return UNSAFE.getFloat(b, BYTE_ARRAY_BASE_OFFSET + off);
		}
		@Override
		long getLongLE(byte[] b, int off) {
			return UNSAFE.getLong(b, BYTE_ARRAY_BASE_OFFSET + off);
		}
		@Override
		double getDoubleLE(byte[] b, int off) {
			return UNSAFE.getDouble(b, BYTE_ARRAY_BASE_OFFSET + off);
		}
		@Override
		void putCharLE(char n, byte[] b, int off) {
			UNSAFE.putChar(b, BYTE_ARRAY_BASE_OFFSET + off, n);
		}
		@Override
		void putShortLE(short n, byte[] b, int off) {
			UNSAFE.putShort(b, BYTE_ARRAY_BASE_OFFSET + off, n);
		}
		@Override
		void putIntLE(int n, byte[] b, int off) {
			UNSAFE.putInt(b, BYTE_ARRAY_BASE_OFFSET + off, n);
		}
		@Override
		void putFloatLE(float n, byte[] b, int off) {
			UNSAFE.putFloat(b, BYTE_ARRAY_BASE_OFFSET + off, n);
		}
		@Override
		void putLongLE(long n, byte[] b, int off) {
			UNSAFE.putLong(b, BYTE_ARRAY_BASE_OFFSET + off, n);
		}
		@Override
		void putDoubleLE(double n, byte[] b, int off) {
			UNSAFE.putDouble(b, BYTE_ARRAY_BASE_OFFSET + off, n);
		}
		@Override
		char getCharBE(byte[] b, int off) {
			return Character.reverseBytes(getCharLE(b, off));
		}
		@Override
		short getShortBE(byte[] b, int off) {
			return Short.reverseBytes(getShortLE(b, off));
		}
		@Override
		int getIntBE(byte[] b, int off) {
			return Integer.reverseBytes(getIntLE(b, off));
		}
		@Override
		long getLongBE(byte[] b, int off) {
			return Long.reverseBytes(getLongLE(b, off));
		}
		@Override
		void putCharBE(char n, byte[] b, int off) {
			putCharLE(Character.reverseBytes(n), b, off);
		}
		@Override
		void putShortBE(short n, byte[] b, int off) {
			putShortLE(Short.reverseBytes(n), b, off);
		}
		@Override
		void putIntBE(int n, byte[] b, int off) {
			putIntLE(Integer.reverseBytes(n), b, off);
		}
		@Override
		void putLongBE(long n, byte[] b, int off) {
			putLongLE(Long.reverseBytes(n), b, off);
		}
		@Override
		int numLeadingBytesMatch(long a, long b) {
			// if UNSAFE reads in little endian, must compare low bytes
			return Long.numberOfTrailingZeros(a ^ b) >>> 3;
		}
	}

	// used if Unsafe reads primitives as big endian, which is probably
	// never the case. but better safe than sorry.
	private static final class BytesUnsafeBE extends BytesUnsafe {

		@Override
		char getCharLE(byte[] b, int off) {
			return Character.reverseBytes(getCharBE(b, off));
		}
		@Override
		short getShortLE(byte[] b, int off) {
			return Short.reverseBytes(getShortBE(b, off));
		}
		@Override
		int getIntLE(byte[] b, int off) {
			return Integer.reverseBytes(getIntBE(b, off));
		}
		@Override
		long getLongLE(byte[] b, int off) {
			return Long.reverseBytes(getLongBE(b, off));
		}
		@Override
		void putCharLE(char n, byte[] b, int off) {
			putCharBE(Character.reverseBytes(n), b, off);
		}
		@Override
		void putShortLE(short n, byte[] b, int off) {
			putShortBE(Short.reverseBytes(n), b, off);
		}
		@Override
		void putIntLE(int n, byte[] b, int off) {
			putIntBE(Integer.reverseBytes(n), b, off);
		}
		@Override
		void putLongLE(long n, byte[] b, int off) {
			putLongBE(Long.reverseBytes(n), b, off);
		}
		@Override
		char getCharBE(byte[] b, int off) {
			return UNSAFE.getChar(b, BYTE_ARRAY_BASE_OFFSET + off);
		}
		@Override
		short getShortBE(byte[] b, int off) {
			return UNSAFE.getShort(b, BYTE_ARRAY_BASE_OFFSET + off);
		}
		@Override
		int getIntBE(byte[] b, int off) {
			return UNSAFE.getInt(b, BYTE_ARRAY_BASE_OFFSET + off);
		}
		@Override
		float getFloatBE(byte[] b, int off) {
			return UNSAFE.getFloat(b, BYTE_ARRAY_BASE_OFFSET + off);
		}
		@Override
		long getLongBE(byte[] b, int off) {
			return UNSAFE.getLong(b, BYTE_ARRAY_BASE_OFFSET + off);
		}
		@Override
		double getDoubleBE(byte[] b, int off) {
			return UNSAFE.getDouble(b, BYTE_ARRAY_BASE_OFFSET + off);
		}
		@Override
		void putCharBE(char n, byte[] b, int off) {
			UNSAFE.putChar(b, BYTE_ARRAY_BASE_OFFSET + off, n);
		}
		@Override
		void putShortBE(short n, byte[] b, int off) {
			UNSAFE.putShort(b, BYTE_ARRAY_BASE_OFFSET + off, n);
		}
		@Override
		void putIntBE(int n, byte[] b, int off) {
			UNSAFE.putInt(b, BYTE_ARRAY_BASE_OFFSET + off, n);
		}
		@Override
		void putFloatBE(float n, byte[] b, int off) {
			UNSAFE.putFloat(b, BYTE_ARRAY_BASE_OFFSET + off, n);
		}
		@Override
		void putLongBE(long n, byte[] b, int off) {
			UNSAFE.putLong(b, BYTE_ARRAY_BASE_OFFSET + off, n);
		}
		@Override
		void putDoubleBE(double n, byte[] b, int off) {
			UNSAFE.putDouble(b, BYTE_ARRAY_BASE_OFFSET + off, n);
		}
		@Override
		int numLeadingBytesMatch(long a, long b) {
			// if UNSAFE reads in big endian, must compare high bytes
			return Long.numberOfLeadingZeros(a ^ b) >>> 3;
		}
	}
}

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
 * <strong>Always include your own Bounds-checks before calling any of these
 * methods!</strong>
 * <p>
 * Uses sun.misc.Unsafe under the hood, but will fall back on an alternate
 * implementation if access fails.
 */
@SuppressWarnings("UseSpecificCatch")
abstract class UnsafeBytes {

	private static final sun.misc.Unsafe UNSAFE;
	private static final long BYTE_ARRAY_BASE_OFFSET;
	public static final UnsafeBytes INSTANCE;
	static {
		UnsafeBytes bytes = null;
		sun.misc.Unsafe u = null;
		long byteOff = 0;
		try {
			// get new instance via constructor instead of relying on named field.
			Constructor<sun.misc.Unsafe> con = sun.misc.Unsafe.class.getDeclaredConstructor();
			con.setAccessible(true);
			u = con.newInstance();
			byteOff = u.arrayBaseOffset(byte[].class);
			// determine byte order used by unsafe, in case it even matters
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
			Logger.getLogger(UnsafeBytes.class.getName()).log(Level.WARNING, "Could not access java.misc.Unsafe. Falling back on conventional solution.", ex);
		}
		INSTANCE = bytes != null ? bytes : new BytesSafe();
		UNSAFE = u;
		BYTE_ARRAY_BASE_OFFSET = byteOff;
	}

	private UnsafeBytes() {
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

	abstract void putCharLE(byte[] b, int off, char n);
	abstract void putCharBE(byte[] b, int off, char n);

	abstract void putShortLE(byte[] b, int off, short n);
	abstract void putShortBE(byte[] b, int off, short n);

	abstract void putIntLE(byte[] b, int off, int n);
	abstract void putIntBE(byte[] b, int off, int n);

	void putFloatLE(byte[] b, int off, float n) {
		putIntLE(b, off, Float.floatToRawIntBits(n));
	}
	void putFloatBE(byte[] b, int off, float n) {
		putIntBE(b, off, Float.floatToRawIntBits(n));
	}

	abstract void putLongLE(byte[] b, int off, long n);
	abstract void putLongBE(byte[] b, int off, long n);

	void putDoubleLE(byte[] b, int off, double n) {
		putLongLE(b, off, Double.doubleToRawLongBits(n));
	}
	void putDoubleBE(byte[] b, int off, double n) {
		putLongBE(b, off, Double.doubleToRawLongBits(n));
	}

	// return number of leading matching bytes (0..len) in both arrays, starting
	// at the respective offsets. does not perform any bounds-checks!
	abstract int matchLeading(byte[] a, int offA, byte[] b, int offB, int len);

	private static abstract class BytesUnsafe extends UnsafeBytes {

		// returns the number of matching leading bytes, where leading
		// means bytes with a lower index in the array a long was read from.
		// so, if the long was read as a little endian value, then the higher order
		// bits in java form the lower indexed bytes, while both simply correspond
		// if the long was read as a big endian value.
		abstract int numLeadingBytesMatch(long a, long b);

		// This is about 3-6 times faster than the alternative, 
		// approaching through-put of up to ~15 GB/s on regular hardware, 
		// at least when the the runs are small enough to fit into faster 
		// CPU cache. Or something like that. Whatever, the speed is nuts!
		// Making this a boolean equality check brings almost no gains.
		// Which pretty much means that memory access is the only real bottleneck.
		@Override
		int matchLeading(byte[] a, int offA, byte[] b, int offB, int len) {
			long _offA = BYTE_ARRAY_BASE_OFFSET + offA;
			long _offB = BYTE_ARRAY_BASE_OFFSET + offB;
			long _maxA = _offA + len;
			// compare 64 (2x32) bytes in parallel (JIT generally does not unroll long-based loops)
			for (long m = _maxA - 31L; _offA < m; _offA += 32L, _offB += 32L) {
				// read 32 bytes from array a
				long _a0 = UNSAFE.getLong(a, _offA);
				long _a1 = UNSAFE.getLong(a, _offA + 8L);
				long _a2 = UNSAFE.getLong(a, _offA + 16L);
				long _a3 = UNSAFE.getLong(a, _offA + 24L);
				// read 32 bytes from array b
				long _b0 = UNSAFE.getLong(b, _offB);
				long _b1 = UNSAFE.getLong(b, _offB + 8L);
				long _b2 = UNSAFE.getLong(b, _offB + 16L);
				long _b3 = UNSAFE.getLong(b, _offB + 24L);
				// compare each block
				if (_a0 != _b0) {
					return (int) (_offA + len - _maxA) + numLeadingBytesMatch(_a0, _b0);
				}
				if (_a1 != _b1) {
					return (int) (_offA + 8L + len - _maxA) + numLeadingBytesMatch(_a1, _b1);
				}
				if (_a2 != _b2) {
					return (int) (_offA + 16L + len - _maxA) + numLeadingBytesMatch(_a2, _b2);
				}
				if (_a3 != _b3) {
					return (int) (_offA + 24L + len - _maxA) + numLeadingBytesMatch(_a3, _b3);
				}
			}
			// compare 16 (2x8) bytes in parallel
			for (long m = _maxA - 7L; _offA < m; _offA += 8L, _offB += 8L) {
				long _a0 = UNSAFE.getLong(a, _offA);
				long _b0 = UNSAFE.getLong(b, _offB);
				if (_a0 != _b0) {
					return (int) (_offA + len - _maxA) + numLeadingBytesMatch(_a0, _b0);
				}
			}
			// compare 2 (2x1) bytes in parallel (faster than using UNSAFE)
			// restore regular offsets
			offA = (int) (_offA - BYTE_ARRAY_BASE_OFFSET);
			offB = (int) (_offB - BYTE_ARRAY_BASE_OFFSET);
			for (int m = (int) (_maxA - BYTE_ARRAY_BASE_OFFSET); offA < m; offA++, offB++) {
				if (a[offA] != b[offB]) {
					return offA + len - m;
				}
			}
			return len;
		}
	}

	// conventional implementation
	private static final class BytesSafe extends UnsafeBytes {

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
		void putCharLE(byte[] b, int off, char n) {
			b[off + 0] = (byte) (n);
			b[off + 1] = (byte) (n >>> 8);
		}
		@Override
		void putCharBE(byte[] b, int off, char n) {
			b[off + 0] = (byte) (n >>> 8);
			b[off + 1] = (byte) (n);
		}
		@Override
		void putShortLE(byte[] b, int off, short n) {
			b[off + 0] = (byte) (n);
			b[off + 1] = (byte) (n >>> 8);
		}
		@Override
		void putShortBE(byte[] b, int off, short n) {
			b[off + 0] = (byte) (n >>> 8);
			b[off + 1] = (byte) (n);
		}
		@Override
		void putIntLE(byte[] b, int off, int n) {
			b[off + 0] = (byte) (n);
			b[off + 1] = (byte) (n >>> 8);
			b[off + 2] = (byte) (n >>> 16);
			b[off + 3] = (byte) (n >>> 24);
		}
		@Override
		void putIntBE(byte[] b, int off, int n) {
			b[off + 0] = (byte) (n >>> 24);
			b[off + 1] = (byte) (n >>> 16);
			b[off + 2] = (byte) (n >>> 8);
			b[off + 3] = (byte) (n);
		}
		@Override
		void putLongLE(byte[] b, int off, long n) {
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
		void putLongBE(byte[] b, int off, long n) {
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
		void putCharLE(byte[] b, int off, char n) {
			UNSAFE.putChar(b, BYTE_ARRAY_BASE_OFFSET + off, n);
		}
		@Override
		void putShortLE(byte[] b, int off, short n) {
			UNSAFE.putShort(b, BYTE_ARRAY_BASE_OFFSET + off, n);
		}
		@Override
		void putIntLE(byte[] b, int off, int n) {
			UNSAFE.putInt(b, BYTE_ARRAY_BASE_OFFSET + off, n);
		}
		@Override
		void putFloatLE(byte[] b, int off, float n) {
			UNSAFE.putFloat(b, BYTE_ARRAY_BASE_OFFSET + off, n);
		}
		@Override
		void putLongLE(byte[] b, int off, long n) {
			UNSAFE.putLong(b, BYTE_ARRAY_BASE_OFFSET + off, n);
		}
		@Override
		void putDoubleLE(byte[] b, int off, double n) {
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
		void putCharBE(byte[] b, int off, char n) {
			putCharLE(b, off, Character.reverseBytes(n));
		}
		@Override
		void putShortBE(byte[] b, int off, short n) {
			putShortLE(b, off, Short.reverseBytes(n));
		}
		@Override
		void putIntBE(byte[] b, int off, int n) {
			putIntLE(b, off, Integer.reverseBytes(n));
		}
		@Override
		void putLongBE(byte[] b, int off, long n) {
			putLongLE(b, off, Long.reverseBytes(n));
		}
		@Override
		int numLeadingBytesMatch(long a, long b) {
			// if UNSAFE reads in little endian, must compare lower-value bytes
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
		void putCharLE(byte[] b, int off, char n) {
			putCharBE(b, off, Character.reverseBytes(n));
		}
		@Override
		void putShortLE(byte[] b, int off, short n) {
			putShortBE(b, off, Short.reverseBytes(n));
		}
		@Override
		void putIntLE(byte[] b, int off, int n) {
			putIntBE(b, off, Integer.reverseBytes(n));
		}
		@Override
		void putLongLE(byte[] b, int off, long n) {
			putLongBE(b, off, Long.reverseBytes(n));
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
		void putCharBE(byte[] b, int off, char n) {
			UNSAFE.putChar(b, BYTE_ARRAY_BASE_OFFSET + off, n);
		}
		@Override
		void putShortBE(byte[] b, int off, short n) {
			UNSAFE.putShort(b, BYTE_ARRAY_BASE_OFFSET + off, n);
		}
		@Override
		void putIntBE(byte[] b, int off, int n) {
			UNSAFE.putInt(b, BYTE_ARRAY_BASE_OFFSET + off, n);
		}
		@Override
		void putFloatBE(byte[] b, int off, float n) {
			UNSAFE.putFloat(b, BYTE_ARRAY_BASE_OFFSET + off, n);
		}
		@Override
		void putLongBE(byte[] b, int off, long n) {
			UNSAFE.putLong(b, BYTE_ARRAY_BASE_OFFSET + off, n);
		}
		@Override
		void putDoubleBE(byte[] b, int off, double n) {
			UNSAFE.putDouble(b, BYTE_ARRAY_BASE_OFFSET + off, n);
		}
		@Override
		int numLeadingBytesMatch(long a, long b) {
			// if UNSAFE reads in big endian, must compare higher-value bytes
			return Long.numberOfLeadingZeros(a ^ b) >>> 3;
		}
	}
}

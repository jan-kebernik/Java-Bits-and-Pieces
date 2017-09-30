/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.io;

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
abstract class FastZeros {

	private static final sun.misc.Unsafe UNSAFE;
	private static final long BYTE_ARRAY_BASE_OFFSET;
	static final FastZeros INSTANCE;
	static {
		FastZeros bytes = null;
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
				bytes = new BytesUnsafe();
			} else if (testOrder == 0x88776655332211L) {
				// probably never ever the case. But it doesn't hurt to make sure.
				bytes = new BytesUnsafe();
			}
			// if "bytes" is not set to an unsafe version, then
			// "UNSAFE" and "BYTE_ARRAY_BASE_OFFSET" will never be accessed.
		} catch (Error err) {
			// don't swallow serious errors
			throw err;
		} catch (Throwable ex) {
			// recoverable
			Logger.getLogger(FastZeros.class.getName()).log(Level.WARNING, "Could not access java.misc.Unsafe. Falling back on conventional solution.", ex);
		}
		INSTANCE = bytes != null ? bytes : new BytesSafe();
		UNSAFE = u;
		BYTE_ARRAY_BASE_OFFSET = byteOff;
	}

	private FastZeros() {
	}

	// fills the specified array range with 0s
	abstract void fillWithZeros(byte[] a, int off, int len);

	// fills the specified array range with the specified byte value
	abstract void fill(byte[] a, int off, int len, byte n);

	private static final class BytesUnsafe extends FastZeros {

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

	private static final class BytesSafe extends FastZeros {

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
}

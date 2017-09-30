/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util;

/**
 * An Endian represents a {@link #BIG Big-Endian} or
 * {@link #LITTLE Little-Endian} byte order. Endians provide an efficient and
 * hassle-free way to convert between bytes and multi-byte primitives, without
 * having to rely of the cumbersome and over-designed java.nio.ByteBuffer
 * solution. Under normal circumstances, using an Endian for byte conversion is
 * expected to be much more efficient than any type of manual conversion.
 *
 * @author Jan Kebernik
 */
public enum Endian {

	/**
	 * In the Little-Endian byte-order, the lowest (right-most) {@code 8} bits
	 * in a primitive are considered the most-significant {@code byte}, the
	 * highest (left-most) {@code 8} bits are considered the least-significant
	 * {@code byte}.
	 */
	LITTLE {
		@Override
		public char doGetChar(byte[] b, int off) {
			return INSTANCE.getCharLE(b, off);
		}
		@Override
		public short doGetShort(byte[] b, int off) {
			return INSTANCE.getShortLE(b, off);
		}
		@Override
		public int doGetInt(byte[] b, int off) {
			return INSTANCE.getIntLE(b, off);
		}
		@Override
		public float doGetFloat(byte[] b, int off) {
			return INSTANCE.getFloatLE(b, off);
		}
		@Override
		public long doGetLong(byte[] b, int off) {
			return INSTANCE.getLongLE(b, off);
		}
		@Override
		public double doGetDouble(byte[] b, int off) {
			return INSTANCE.getDoubleLE(b, off);
		}
		@Override
		public void doPutChar(char n, byte[] b, int off) {
			INSTANCE.putCharLE(n, b, off);
		}
		@Override
		public void doPutShort(short n, byte[] b, int off) {
			INSTANCE.putShortLE(n, b, off);
		}
		@Override
		public void doPutInt(int n, byte[] b, int off) {
			INSTANCE.putIntLE(n, b, off);
		}
		@Override
		public void doPutFloat(float n, byte[] b, int off) {
			INSTANCE.putFloatLE(n, b, off);
		}
		@Override
		public void doPutLong(long n, byte[] b, int off) {
			INSTANCE.putLongLE(n, b, off);
		}
		@Override
		public void doPutDouble(double n, byte[] b, int off) {
			INSTANCE.putDoubleLE(n, b, off);
		}
		@Override
		public char toChar(byte b1, byte b0) {
			return (char) ((b0 << 8) | (b1 & 0xff));
		}
		@Override
		public short toShort(byte b1, byte b0) {
			return (short) ((b0 << 8) | (b1 & 0xff));
		}
		@Override
		public int toInt(byte b3, byte b2, byte b1, byte b0) {
			return (b0 << 24)
					| ((b1 & 0xff) << 16)
					| ((b2 & 0xff) << 8)
					| (b3 & 0xff);
		}
		@Override
		public long toLong(byte b7, byte b6, byte b5, byte b4, byte b3, byte b2, byte b1, byte b0) {
			return (((b0 << 8) | (b1 & 0xffL)) << 48)
					| ((b2 & 0xffL) << 40)
					| ((b3 & 0xffL) << 32)
					| ((b4 & 0xffL) << 24)
					| ((b5 & 0xffL) << 16)
					| ((b6 & 0xffL) << 8)
					| (b7 & 0xffL);
		}
		@Override
		public Endian other() {
			return BIG;
		}
	},
	/**
	 * In the Big-Endian byte-order, the highest (left-most) {@code 8} bits in a
	 * primitive are considered the most-significant {@code byte}, the lowest
	 * (right-most) {@code 8} bits are considered the least-significant
	 * {@code byte}.
	 */
	BIG {
		@Override
		public char doGetChar(byte[] b, int off) {
			return INSTANCE.getCharBE(b, off);
		}
		@Override
		public short doGetShort(byte[] b, int off) {
			return INSTANCE.getShortBE(b, off);
		}
		@Override
		public int doGetInt(byte[] b, int off) {
			return INSTANCE.getIntBE(b, off);
		}
		@Override
		public float doGetFloat(byte[] b, int off) {
			return INSTANCE.getFloatBE(b, off);
		}
		@Override
		public long doGetLong(byte[] b, int off) {
			return INSTANCE.getLongBE(b, off);
		}
		@Override
		public double doGetDouble(byte[] b, int off) {
			return INSTANCE.getDoubleBE(b, off);
		}
		@Override
		public void doPutChar(char n, byte[] b, int off) {
			INSTANCE.putCharBE(n, b, off);
		}
		@Override
		public void doPutShort(short n, byte[] b, int off) {
			INSTANCE.putShortBE(n, b, off);
		}
		@Override
		public void doPutInt(int n, byte[] b, int off) {
			INSTANCE.putIntBE(n, b, off);
		}
		@Override
		public void doPutFloat(float n, byte[] b, int off) {
			INSTANCE.putFloatBE(n, b, off);
		}
		@Override
		public void doPutLong(long n, byte[] b, int off) {
			INSTANCE.putLongBE(n, b, off);
		}
		@Override
		public void doPutDouble(double n, byte[] b, int off) {
			INSTANCE.putDoubleBE(n, b, off);
		}
		@Override
		public char toChar(byte b1, byte b0) {
			return (char) ((b1 << 8) | (b0 & 0xff));
		}
		@Override
		public short toShort(byte b1, byte b0) {
			return (short) ((b1 << 8) | (b0 & 0xff));
		}
		@Override
		public int toInt(byte b3, byte b2, byte b1, byte b0) {
			return (b3 << 24)
					| ((b2 & 0xff) << 16)
					| ((b1 & 0xff) << 8)
					| (b0 & 0xff);
		}
		@Override
		public long toLong(byte b7, byte b6, byte b5, byte b4, byte b3, byte b2, byte b1, byte b0) {
			return (((b7 << 8) | (b6 & 0xffL)) << 48)
					| ((b5 & 0xffL) << 40)
					| ((b4 & 0xffL) << 32)
					| ((b3 & 0xffL) << 24)
					| ((b2 & 0xffL) << 16)
					| ((b1 & 0xffL) << 8)
					| (b0 & 0xffL);
		}
		@Override
		public Endian other() {
			return LITTLE;
		}
	};

	private static final FastBytes INSTANCE = FastBytes.INSTANCE;

	/**
	 * Writes the specified {@code char} to the specified byte-array, starting
	 * at the specified offset.
	 *
	 * @param n the {@code char} to be written.
	 * @param buf the byte-array into which to write.
	 * @param off the offset into the byte-array at which to start writing.
	 * @return this {@code Endian}
	 * @throws ArrayIndexOutOfBoundsException if the specified offset is
	 * negative or if less than {@code 2} {@code byte}s are available for
	 * writing at the specified offset. No {@code byte}s are written in such
	 * cases.
	 */
	public Endian putChar(char n, byte[] buf, int off) {
		if (off < 0 || off > buf.length - 2) {
			throw new ArrayIndexOutOfBoundsException(off);
		}
		doPutChar(n, buf, off);
		return this;
	}

	/**
	 * Writes the specified {@code short} to the specified byte-array, starting
	 * at the specified offset.
	 *
	 * @param n the {@code short} to be written.
	 * @param buf the byte-array into which to write.
	 * @param off the offset into the byte-array at which to start writing.
	 * @return this {@code Endian}
	 * @throws ArrayIndexOutOfBoundsException if the specified offset is
	 * negative or if less than {@code 2} {@code byte}s are available for
	 * writing at the specified offset. No {@code byte}s are written in such
	 * cases.
	 */
	public Endian putShort(short n, byte[] buf, int off) {
		if (off < 0 || off > buf.length - 2) {
			throw new ArrayIndexOutOfBoundsException(off);
		}
		doPutShort(n, buf, off);
		return this;
	}

	/**
	 * Writes the specified {@code int} to the specified byte-array, starting at
	 * the specified offset.
	 *
	 * @param n the {@code int} to be written.
	 * @param buf the byte-array into which to write.
	 * @param off the offset into the byte-array at which to start writing.
	 * @return this {@code Endian}
	 * @throws ArrayIndexOutOfBoundsException if the specified offset is
	 * negative or if less than {@code 4} {@code byte}s are available for
	 * writing at the specified offset. No {@code byte}s are written in such
	 * cases.
	 */
	public Endian putInt(int n, byte[] buf, int off) {
		if (off < 0 || off > buf.length - 4) {
			throw new ArrayIndexOutOfBoundsException(off);
		}
		doPutInt(n, buf, off);
		return this;
	}

	/**
	 * Writes the specified {@code float} to the specified byte-array, starting
	 * at the specified offset.
	 *
	 * @param n the {@code float} to be written.
	 * @param buf the byte-array into which to write.
	 * @param off the offset into the byte-array at which to start writing.
	 * @return this {@code Endian}
	 * @throws ArrayIndexOutOfBoundsException if the specified offset is
	 * negative or if less than {@code 4} {@code byte}s are available for
	 * writing at the specified offset. No {@code byte}s are written in such
	 * cases.
	 */
	public Endian putFloat(float n, byte[] buf, int off) {
		if (off < 0 || off > buf.length - 4) {
			throw new ArrayIndexOutOfBoundsException(off);
		}
		doPutFloat(n, buf, off);
		return this;
	}

	/**
	 * Writes the specified {@code long} to the specified byte-array, starting
	 * at the specified offset.
	 *
	 * @param n the {@code long} to be written.
	 * @param buf the byte-array into which to write.
	 * @param off the offset into the byte-array at which to start writing.
	 * @return this {@code Endian}
	 * @throws ArrayIndexOutOfBoundsException if the specified offset is
	 * negative or if less than {@code 8} {@code byte}s are available for
	 * writing at the specified offset. No {@code byte}s are written in such
	 * cases.
	 */
	public Endian putLong(long n, byte[] buf, int off) {
		if (off < 0 || off > buf.length - 8) {
			throw new ArrayIndexOutOfBoundsException(off);
		}
		doPutLong(n, buf, off);
		return this;
	}

	/**
	 * Writes the specified {@code double} to the specified byte-array, starting
	 * at the specified offset.
	 *
	 * @param n the {@code double} to be written.
	 * @param buf the byte-array into which to write.
	 * @param off the offset into the byte-array at which to start writing.
	 * @return this {@code Endian}
	 * @throws ArrayIndexOutOfBoundsException if the specified offset is
	 * negative or if less than {@code 8} {@code byte}s are available for
	 * writing at the specified offset. No {@code byte}s are written in such
	 * cases.
	 */
	public Endian putDouble(double n, byte[] buf, int off) {
		if (off < 0 || off > buf.length - 8) {
			throw new ArrayIndexOutOfBoundsException(off);
		}
		doPutDouble(n, buf, off);
		return this;
	}

	/**
	 * Converts {@code byte}s from the specified byte-array to a {@code char},
	 * starting at the specified offset.
	 *
	 * @param buf the byte-array from which to read {@code byte}s.
	 * @param off the offset into the byte-array at which to start reading.
	 * @return the {@code char} represented by the specified {@code byte}s in
	 * this {@code Endian}.
	 * @throws ArrayIndexOutOfBoundsException if the specified offset is
	 * negative or if less than {@code 2} {@code byte}s are available for
	 * reading at the specified offset.
	 */
	public char getChar(byte[] buf, int off) {
		if (off < 0 || off > buf.length - 2) {
			throw new ArrayIndexOutOfBoundsException(off);
		}
		return doGetChar(buf, off);
	}

	/**
	 * Converts {@code byte}s from the specified byte-array to a {@code short},
	 * starting at the specified offset.
	 *
	 * @param buf the byte-array from which to read {@code byte}s.
	 * @param off the offset into the byte-array at which to start reading.
	 * @return the {@code short} represented by the specified {@code byte}s in
	 * this {@code Endian}.
	 * @throws ArrayIndexOutOfBoundsException if the specified offset is
	 * negative or if less than {@code 2} {@code byte}s are available for
	 * reading at the specified offset.
	 */
	public short getShort(byte[] buf, int off) {
		if (off < 0 || off > buf.length - 2) {
			throw new ArrayIndexOutOfBoundsException(off);
		}
		return doGetShort(buf, off);
	}

	/**
	 * Converts {@code byte}s from the specified byte-array to an {@code int},
	 * starting at the specified offset.
	 *
	 * @param buf the byte-array from which to read {@code byte}s.
	 * @param off the offset into the byte-array at which to start reading.
	 * @return the {@code int} represented by the specified {@code byte}s in
	 * this {@code Endian}.
	 * @throws ArrayIndexOutOfBoundsException if the specified offset is
	 * negative or if less than {@code 4} {@code byte}s are available for
	 * reading at the specified offset.
	 */
	public int getInt(byte[] buf, int off) {
		if (off < 0 || off > buf.length - 4) {
			throw new ArrayIndexOutOfBoundsException(off);
		}
		return doGetInt(buf, off);
	}

	/**
	 * Converts {@code byte}s from the specified byte-array to a {@code float},
	 * starting at the specified offset.
	 *
	 * @param buf the byte-array from which to read {@code byte}s.
	 * @param off the offset into the byte-array at which to start reading.
	 * @return the {@code float} represented by the specified {@code byte}s in
	 * this {@code Endian}.
	 * @throws ArrayIndexOutOfBoundsException if the specified offset is
	 * negative or if less than {@code 4} {@code byte}s are available for
	 * reading at the specified offset.
	 */
	public float getFloat(byte[] buf, int off) {
		if (off < 0 || off > buf.length - 4) {
			throw new ArrayIndexOutOfBoundsException(off);
		}
		return doGetFloat(buf, off);
	}

	/**
	 * Converts {@code byte}s from the specified byte-array to a {@code long},
	 * starting at the specified offset.
	 *
	 * @param buf the byte-array from which to read {@code byte}s.
	 * @param off the offset into the byte-array at which to start reading.
	 * @return the {@code long} represented by the specified {@code byte}s in
	 * this {@code Endian}.
	 * @throws ArrayIndexOutOfBoundsException if the specified offset is
	 * negative or if less than {@code 8} {@code byte}s are available for
	 * reading at the specified offset.
	 */
	public long getLong(byte[] buf, int off) {
		if (off < 0 || off > buf.length - 8) {
			throw new ArrayIndexOutOfBoundsException(off);
		}
		return doGetLong(buf, off);
	}

	/**
	 * Converts {@code byte}s from the specified byte-array to a {@code double},
	 * starting at the specified offset.
	 *
	 * @param buf the byte-array from which to read {@code byte}s.
	 * @param off the offset into the byte-array at which to start reading.
	 * @return the {@code double} represented by the specified {@code byte}s in
	 * this {@code Endian}.
	 * @throws ArrayIndexOutOfBoundsException if the specified offset is
	 * negative or if less than {@code 8} {@code byte}s are available for
	 * reading at the specified offset.
	 */
	public double getDouble(byte[] buf, int off) {
		if (off < 0 || off > buf.length - 8) {
			throw new ArrayIndexOutOfBoundsException(off);
		}
		return doGetDouble(buf, off);
	}

	/**
	 * Converts the specified {@code byte}s to a {@code char}.
	 *
	 * @param b1 the most significant {@code byte}.
	 * @param b0 the least significant {@code byte}.
	 * @return the {@code char} represented by the specified {@code byte}s in
	 * this {@code Endian}.
	 */
	public abstract char toChar(byte b1, byte b0);

	/**
	 * Converts the specified {@code byte}s to a {@code short}.
	 *
	 * @param b1 the most significant {@code byte}.
	 * @param b0 the least significant {@code byte}.
	 * @return the {@code short} represented by the specified {@code byte}s in
	 * this {@code Endian}.
	 */
	public abstract short toShort(byte b1, byte b0);

	/**
	 * Converts the specified {@code byte}s to an {@code int}.
	 *
	 * @param b3 the most significant {@code byte}.
	 * @param b2 the second-most significant {@code byte}.
	 * @param b1 the third-most significant {@code byte}.
	 * @param b0 the least significant {@code byte}.
	 * @return the {@code int} represented by the specified {@code byte}s in
	 * this {@code Endian}.
	 */
	public abstract int toInt(byte b3, byte b2, byte b1, byte b0);

	/**
	 * Converts the specified {@code byte}s to a {@code float}.
	 *
	 * @param b3 the most significant {@code byte}.
	 * @param b2 the second-most significant {@code byte}.
	 * @param b1 the third-most significant {@code byte}.
	 * @param b0 the least significant {@code byte}.
	 * @return the {@code float} represented by the specified {@code byte}s in
	 * this {@code Endian}.
	 */
	public float toFloat(byte b3, byte b2, byte b1, byte b0) {
		return Float.intBitsToFloat(toInt(b3, b2, b1, b0));
	}

	/**
	 * Converts the specified {@code byte}s to a {@code long}.
	 *
	 * @param b7 the most significant {@code byte}.
	 * @param b6 the second-most significant {@code byte}.
	 * @param b5 the third-most significant {@code byte}.
	 * @param b4 the fourth-most significant {@code byte}.
	 * @param b3 the fifth-most significant {@code byte}.
	 * @param b2 the sixth-most significant {@code byte}.
	 * @param b1 the seventh-most significant {@code byte}.
	 * @param b0 the least significant {@code byte}.
	 * @return the {@code long} represented by the specified {@code byte}s in
	 * this {@code Endian}.
	 */
	public abstract long toLong(byte b7, byte b6, byte b5, byte b4, byte b3, byte b2, byte b1, byte b0);

	/**
	 * Converts the specified {@code byte}s to a {@code double}.
	 *
	 * @param b7 the most significant {@code byte}.
	 * @param b6 the second-most significant {@code byte}.
	 * @param b5 the third-most significant {@code byte}.
	 * @param b4 the fourth-most significant {@code byte}.
	 * @param b3 the fifth-most significant {@code byte}.
	 * @param b2 the sixth-most significant {@code byte}.
	 * @param b1 the seventh-most significant {@code byte}.
	 * @param b0 the least significant {@code byte}.
	 * @return the {@code double} represented by the specified {@code byte}s in
	 * this {@code Endian}.
	 */
	public double toDouble(byte b7, byte b6, byte b5, byte b4, byte b3, byte b2, byte b1, byte b0) {
		return Double.longBitsToDouble(toLong(b7, b6, b5, b4, b3, b2, b1, b0));
	}

	/**
	 * Returns the reverse {@code Endian} of this {@code Endian}.
	 *
	 * @return the reverse {@code Endian} of this {@code Endian}.
	 */
	public abstract Endian other();

	// no bounds-checks variants
	// real risk of out-of-bounds access. use caution!
	public abstract void doPutChar(char n, byte[] buf, int off);
	public abstract void doPutShort(short n, byte[] buf, int off);
	public abstract void doPutInt(int n, byte[] buf, int off);
	public abstract void doPutFloat(float n, byte[] buf, int off);
	public abstract void doPutLong(long n, byte[] buf, int off);
	public abstract void doPutDouble(double n, byte[] buf, int off);
	public abstract char doGetChar(byte[] buf, int off);
	public abstract short doGetShort(byte[] buf, int off);
	public abstract int doGetInt(byte[] buf, int off);
	public abstract float doGetFloat(byte[] buf, int off);
	public abstract long doGetLong(byte[] buf, int off);
	public abstract double doGetDouble(byte[] buf, int off);
}

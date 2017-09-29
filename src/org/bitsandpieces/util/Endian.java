/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util;

/**
 * An Endian represents a Big-Endian or Little-Endian byte order. An Endian
 * provides an efficient and hassle-free way to convert between bytes and
 * multi-byte primitives, without having to rely on the expensive and cumbersome
 * java.nio.ByteBuffer solution. Under normal circumstances, using an Endian for
 * byte conversion is expected to be much more efficient than conversion by
 * hand.
 *
 * @implNote If available, this implementation will make use of sun.misc.Unsafe
 * so severely speed up through-put.
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
		char _getChar(byte[] b, int off) {
			return BYTES.getCharLE(b, off);
		}
		@Override
		short _getShort(byte[] b, int off) {
			return BYTES.getShortLE(b, off);
		}
		@Override
		int _getInt(byte[] b, int off) {
			return BYTES.getIntLE(b, off);
		}
		@Override
		float _getFloat(byte[] b, int off) {
			return BYTES.getFloatLE(b, off);
		}
		@Override
		long _getLong(byte[] b, int off) {
			return BYTES.getLongLE(b, off);
		}
		@Override
		double _getDouble(byte[] b, int off) {
			return BYTES.getDoubleLE(b, off);
		}
		@Override
		void _putChar(byte[] b, int off, char n) {
			BYTES.putCharLE(b, off, n);
		}
		@Override
		void _putShort(byte[] b, int off, short n) {
			BYTES.putShortLE(b, off, n);
		}
		@Override
		void _putInt(byte[] b, int off, int n) {
			BYTES.putIntLE(b, off, n);
		}
		@Override
		void _putFloat(byte[] b, int off, float n) {
			BYTES.putFloatLE(b, off, n);
		}
		@Override
		void _putLong(byte[] b, int off, long n) {
			BYTES.putLongLE(b, off, n);
		}
		@Override
		void _putDouble(byte[] b, int off, double n) {
			BYTES.putDoubleLE(b, off, n);
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
		char _getChar(byte[] b, int off) {
			return BYTES.getCharBE(b, off);
		}
		@Override
		short _getShort(byte[] b, int off) {
			return BYTES.getShortBE(b, off);
		}
		@Override
		int _getInt(byte[] b, int off) {
			return BYTES.getIntBE(b, off);
		}
		@Override
		float _getFloat(byte[] b, int off) {
			return BYTES.getFloatBE(b, off);
		}
		@Override
		long _getLong(byte[] b, int off) {
			return BYTES.getLongBE(b, off);
		}
		@Override
		double _getDouble(byte[] b, int off) {
			return BYTES.getDoubleBE(b, off);
		}
		@Override
		void _putChar(byte[] b, int off, char n) {
			BYTES.putCharBE(b, off, n);
		}
		@Override
		void _putShort(byte[] b, int off, short n) {
			BYTES.putShortBE(b, off, n);
		}
		@Override
		void _putInt(byte[] b, int off, int n) {
			BYTES.putIntBE(b, off, n);
		}
		@Override
		void _putFloat(byte[] b, int off, float n) {
			BYTES.putFloatBE(b, off, n);
		}
		@Override
		void _putLong(byte[] b, int off, long n) {
			BYTES.putLongBE(b, off, n);
		}
		@Override
		void _putDouble(byte[] b, int off, double n) {
			BYTES.putDoubleBE(b, off, n);
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

	private static final UnsafeBytes BYTES = UnsafeBytes.INSTANCE;

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
	public Endian putChar(byte[] buf, int off, char n) {
		if (off < 0 || off > buf.length - 2) {
			throw new ArrayIndexOutOfBoundsException(off);
		}
		_putChar(buf, off, n);
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
	public Endian putShort(byte[] buf, int off, short n) {
		if (off < 0 || off > buf.length - 2) {
			throw new ArrayIndexOutOfBoundsException(off);
		}
		_putShort(buf, off, n);
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
	public Endian putInt(byte[] buf, int off, int n) {
		if (off < 0 || off > buf.length - 4) {
			throw new ArrayIndexOutOfBoundsException(off);
		}
		_putInt(buf, off, n);
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
	public Endian putFloat(byte[] buf, int off, float n) {
		if (off < 0 || off > buf.length - 4) {
			throw new ArrayIndexOutOfBoundsException(off);
		}
		_putFloat(buf, off, n);
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
	public Endian putLong(byte[] buf, int off, long n) {
		if (off < 0 || off > buf.length - 8) {
			throw new ArrayIndexOutOfBoundsException(off);
		}
		_putLong(buf, off, n);
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
	public Endian putDouble(byte[] buf, int off, double n) {
		if (off < 0 || off > buf.length - 8) {
			throw new ArrayIndexOutOfBoundsException(off);
		}
		_putDouble(buf, off, n);
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
		return _getChar(buf, off);
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
		return _getShort(buf, off);
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
		return _getInt(buf, off);
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
		return _getFloat(buf, off);
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
		return _getLong(buf, off);
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
		return _getDouble(buf, off);
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

	// no bounds-checks variants accessible from within this package
	abstract void _putChar(byte[] buf, int off, char n);
	abstract void _putShort(byte[] buf, int off, short n);
	abstract void _putInt(byte[] buf, int off, int n);
	abstract void _putFloat(byte[] buf, int off, float n);
	abstract void _putLong(byte[] buf, int off, long n);
	abstract void _putDouble(byte[] buf, int off, double n);
	abstract char _getChar(byte[] buf, int off);
	abstract short _getShort(byte[] buf, int off);
	abstract int _getInt(byte[] buf, int off);
	abstract float _getFloat(byte[] buf, int off);
	abstract long _getLong(byte[] buf, int off);
	abstract double _getDouble(byte[] buf, int off);
}

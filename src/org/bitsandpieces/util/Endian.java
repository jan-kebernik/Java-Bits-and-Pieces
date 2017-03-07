/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util;

/**
 * Default byte orders, independent of unwieldy nio.Buffer API.
 *
 * @author Jan Kebernik
 */
public enum Endian {

	LITTLE() {
		@Override
		public char toChar(byte b1, byte b0) {
			return _toChar(b0, b1);
		}

		@Override
		public Endian putChar(char c, byte[] buf, int off) {
			put2((byte) c, (byte) (c >>> 8), buf, off);
			return this;
		}

		@Override
		public short toShort(byte b1, byte b0) {
			return _toShort(b0, b1);
		}

		@Override
		public Endian putShort(short s, byte[] buf, int off) {
			put2((byte) s, (byte) (s >>> 8), buf, off);
			return this;
		}

		@Override
		public int toInt(byte b3, byte b2, byte b1, byte b0) {
			return _toInt(b0, b1, b2, b3);
		}

		@Override
		public Endian putInt(int i, byte[] buf, int off) {
			put4((byte) i, (byte) (i >>> 8), (byte) (i >>> 16), (byte) (i >>> 24), buf, off);
			return this;
		}

		@Override
		public long toLong(byte b7, byte b6, byte b5, byte b4, byte b3, byte b2, byte b1, byte b0) {
			return _toLong(b0, b1, b2, b3, b4, b5, b6, b7);
		}

		@Override
		public Endian putLong(long l, byte[] buf, int off) {
			put8((byte) l, (byte) (l >>> 8), (byte) (l >>> 16), (byte) (l >>> 24),
					(byte) (l >>> 32), (byte) (l >>> 40), (byte) (l >>> 48), (byte) (l >>> 56), buf, off);
			return this;
		}

		@Override
		public Endian other() {
			return BIG;
		}
	},
	BIG() {
		@Override
		public char toChar(byte b1, byte b0) {
			return _toChar(b1, b0);
		}

		@Override
		public Endian putChar(char c, byte[] buf, int off) {
			put2((byte) (c >>> 8), (byte) c, buf, off);
			return this;
		}

		@Override
		public short toShort(byte b1, byte b0) {
			return _toShort(b1, b0);
		}

		@Override
		public Endian putShort(short s, byte[] buf, int off) {
			put2((byte) (s >>> 8), (byte) s, buf, off);
			return this;
		}

		@Override
		public int toInt(byte b3, byte b2, byte b1, byte b0) {
			return _toInt(b3, b2, b1, b0);
		}

		@Override
		public Endian putInt(int i, byte[] buf, int off) {
			put4((byte) (i >>> 24), (byte) (i >>> 16), (byte) (i >>> 8), (byte) i, buf, off);
			return this;
		}

		@Override
		public long toLong(byte b7, byte b6, byte b5, byte b4, byte b3, byte b2, byte b1, byte b0) {
			return _toLong(b7, b6, b5, b4, b3, b2, b1, b0);
		}

		@Override
		public Endian putLong(long l, byte[] buf, int off) {
			put8((byte) (l >>> 56), (byte) (l >>> 48), (byte) (l >>> 40), (byte) (l >>> 32),
					(byte) (l >>> 24), (byte) (l >>> 16), (byte) (l >>> 8), (byte) l, buf, off);
			return this;
		}

		@Override
		public Endian other() {
			return LITTLE;
		}
	};
	
	public abstract Endian other();

	public abstract char toChar(byte b1, byte b0);

	public abstract short toShort(byte b1, byte b0);

	public abstract int toInt(byte b3, byte b2, byte b1, byte b0);

	public abstract long toLong(byte b7, byte b6, byte b5, byte b4, byte b3, byte b2, byte b1, byte b0);

	public float toFloat(byte b3, byte b2, byte b1, byte b0) {
		return Float.intBitsToFloat(toInt(b3, b2, b1, b0));
	}

	public double toDouble(byte b7, byte b6, byte b5, byte b4, byte b3, byte b2, byte b1, byte b0) {
		return Double.longBitsToDouble(toLong(b7, b6, b5, b4, b3, b2, b1, b0));
	}

	public char getChar(byte[] buf, int off) {
		return toChar(buf[off], buf[off + 1]);
	}

	public short getShort(byte[] buf, int off) {
		return toShort(buf[off], buf[off + 1]);
	}

	public int getInt(byte[] buf, int off) {
		return toInt(buf[off], buf[off + 1], buf[off + 2], buf[off + 3]);
	}

	public long getLong(byte[] buf, int off) {
		return toLong(buf[off], buf[off + 1], buf[off + 2], buf[off + 3], buf[off + 4], buf[off + 5], buf[off + 6], buf[off + 7]);
	}

	public float getFloat(byte[] buf, int off) {
		return Float.intBitsToFloat(getInt(buf, off));
	}

	public double getDouble(byte[] buf, int off) {
		return Double.longBitsToDouble(getLong(buf, off));
	}

	public abstract Endian putChar(char c, byte[] buf, int off);

	public abstract Endian putShort(short s, byte[] buf, int off);

	public abstract Endian putInt(int i, byte[] buf, int off);

	public abstract Endian putLong(long l, byte[] buf, int off);

	public Endian putFloat(float f, byte[] buf, int off) {
		return putInt(Float.floatToRawIntBits(f), buf, off);
	}

	public Endian putDouble(double d, byte[] buf, int off) {
		return putLong(Double.doubleToRawLongBits(d), buf, off);
	}

	private static void put2(byte b1, byte b0, byte[] buf, int off) {
		buf[off] = b1;
		buf[off + 1] = b0;
	}

	private static void put4(byte b3, byte b2, byte b1, byte b0, byte[] buf, int off) {
		buf[off] = b3;
		buf[off + 1] = b2;
		buf[off + 2] = b1;
		buf[off + 3] = b0;
	}

	private static void put8(byte b7, byte b6, byte b5, byte b4, byte b3, byte b2, byte b1, byte b0, byte[] buf, int off) {
		buf[off] = b7;
		buf[off + 1] = b6;
		buf[off + 2] = b5;
		buf[off + 3] = b4;
		buf[off + 4] = b3;
		buf[off + 5] = b2;
		buf[off + 6] = b1;
		buf[off + 7] = b0;
	}

	private static char _toChar(byte b1, byte b0) {
		return (char) ((b1 << 8) | (0xff & b0));
	}

	private static short _toShort(byte b1, byte b0) {
		return (short) ((b1 << 8) | (0xff & b0));
	}

	private static int _toInt(byte b3, byte b2, byte b1, byte b0) {
		// more parallelization
		return b3 << 24
				| ((0xff & b2) << 16)
				| ((0xff & b1) << 8)
				| (0xff & b0);
		//return (((((b3 << 8) | (0xff & b2)) << 8) | (0xff & b1)) << 8) | (0xff & b0);
	}

	private static long _toLong(byte b7, byte b6, byte b5, byte b4, byte b3, byte b2, byte b1, byte b0) {
		// more parallelization
		return (((b7 << 8) | (0xffL & b6))) << 48
				| ((0xffL & b5) << 40)
				| ((0xffL & b4) << 32)
				| ((0xffL & b3) << 24)
				| ((0xffL & b2) << 16)
				| ((0xffL & b1) << 8)
				| (0xffL & b0);
		//return (((((((((((((b7 << 8) | (0xffL & b6)) << 8)
		//		| (0xff & b5)) << 8) | (0xff & b4)) << 8)
		//		| (0xff & b3)) << 8) | (0xff & b2)) << 8)
		//		| (0xff & b1)) << 8) | (0xff & b0);
	}
}

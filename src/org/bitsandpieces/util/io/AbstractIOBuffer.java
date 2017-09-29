/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.io;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.Checksum;
import java.util.zip.Inflater;
import org.bitsandpieces.util.Endian;
import org.bitsandpieces.util.encoding.Decoder;
import org.bitsandpieces.util.encoding.Encoder;

/**
 *
 * @author pp
 */
abstract class AbstractIOBuffer implements IOBuffer {

	long pos;
	long size;
	final IOSource source;
	final AtomicInteger shared;	// -1 == source closed, else unsigned number of siblings (not counting first instance)
	private Endian endian;

	AbstractIOBuffer(IOSource source) throws IOException {
		this.pos = 0L;
		this.shared = new AtomicInteger();
		this.endian = Endian.BIG;
		this.source = source;
		this.size = source.size();
	}

	AbstractIOBuffer(IOSource source, AtomicInteger shared) throws IOException {
		while (true) {
			int c = shared.get();
			if (c == -2) {
				throw new IllegalStateException("Too many siblings open.");
			}
			if (c == -1) {
				throw new IllegalStateException("Underlying I/O source has been closed.");
			}
			if (shared.compareAndSet(c, c + 1)) {
				break;
			}
		}
		this.pos = 0L;
		this.shared = shared;
		this.endian = Endian.BIG;
		this.source = source;
		this.size = source.size();
	}

	abstract void ensureOpen() throws IOException;

	final void closeSource() throws IOException {
		while (true) {
			int c = this.shared.get();
			if (this.shared.compareAndSet(c, c - 1)) {
				// won thread-race
				if (c == 0) {
					// no siblings remain
					this.source.close();
				}
				break;
			}
		}
	}

	@Override
	public final IOAddress address() {
		return this.source.address();
	}

	@Override
	public final long bytesRemaining() {
		ensureOpen();
		return this.size - this.pos;
	}

	@Override
	public final long pos() {
		ensureOpen();
		return this.pos;
	}

	@Override
	public final IOBuffer pos(long pos) {
		ensureOpen();
		if (pos < 0L) {
			throw new IllegalArgumentException("pos < 0: " + pos);
		}
		this.pos = pos;
		return this;
	}

	@Override
	public final Endian endian() {
		return this.endian;
	}

	@Override
	public final IOBuffer endian(Endian endian) {
		if (endian == null) {
			throw new NullPointerException();
		}
		this.endian = endian;
		return this;
	}

	@Override
	public final long size() {
		ensureOpen();
		return this.size;
	}

	abstract void _truncate(long size) throws IOException;

	@Override
	public final IOBuffer truncate(long size) throws IOException {
		ensureOpen();
		if (size < 0L) {
			throw new IllegalArgumentException("size < 0: " + size);
		}
		_truncate(size);
		return this;
	}

	///////////////////////////////////////////////////////////////
	// BULK READING
	///////////////////////////////////////////////////////////////
	// s = snapshot of size
	// 0 <= pos < s
	// 0 < len <= s - pos
	abstract void _read(long pos, long pos_len, long s, byte[] buf, int off, int len) throws IOException;
	
	@Override
	public final int read(byte[] buf) throws IOException {
		ensureOpen();
		if (buf == null) {
			throw new NullPointerException();
		}
		if (buf.length == 0) {
			return 0;
		}
		long p = this.pos;
		long s = this.size;
		if (p >= s) {
			return -1;
		}
		int n = (int) Math.min(s - p, buf.length);
		long pos_len = p + n;
		_read(p, pos_len, s, buf, 0, n);
		this.pos = pos_len;
		return n;
	}

	@Override
	public final int read(byte[] buf, int off, int len) throws IOException {
		ensureOpen();
		if (buf == null) {
			throw new NullPointerException();
		}
		if (off < 0 || len < 0 || off > buf.length - len) {
			throw new ArrayIndexOutOfBoundsException();
		}
		if (len == 0) {
			return 0;
		}
		long p = this.pos;
		long s = this.size;
		if (p >= s) {
			return -1;
		}
		int n = (int) Math.min(s - p, len);
		long pos_len = p + n;
		_read(p, pos_len, s, buf, off, n);
		this.pos = pos_len;
		return n;
	}

	@Override
	public final int read(long pos, byte[] buf) throws IOException {
		ensureOpen();
		if (buf == null) {
			throw new NullPointerException();
		}
		if (pos < 0L) {
			throw new IndexOutOfBoundsException("pos < 0: " + pos);
		}
		if (buf.length == 0) {
			return 0;
		}
		long s = this.size;
		if (pos >= s) {
			return -1;
		}
		int n = (int) Math.min(s - pos, buf.length);
		_read(pos, pos + n, s, buf, 0, n);
		return n;
	}

	@Override
	public final int read(long pos, byte[] buf, int off, int len) throws IOException {
		ensureOpen();
		if (buf == null) {
			throw new NullPointerException();
		}
		if (pos < 0L) {
			throw new IndexOutOfBoundsException("pos < 0: " + pos);
		}
		if (off < 0 || len < 0 || off > buf.length - len) {
			throw new ArrayIndexOutOfBoundsException();
		}
		if (len == 0) {
			return 0;
		}
		long s = this.size;
		if (pos >= s) {
			return -1;
		}
		int n = (int) Math.min(s - pos, len);
		_read(pos, pos + n, s, buf, off, n);
		return n;
	}

	///////////////////////////////////////////////////////////////
	// BULK WRITING
	///////////////////////////////////////////////////////////////
	abstract void _write(long pos, long pos_len, byte[] buf, int off, int len) throws IOException;

	@Override
	public final int write(byte[] buf) throws IOException {
		ensureOpen();
		if (buf == null) {
			throw new NullPointerException();
		}
		long p = this.pos;
		int n = (int) Math.min(Long.MAX_VALUE - p, buf.length);
		if (n == 0) {
			return 0;
		}
		long pos_len = p + n;
		_write(p, pos_len, buf, 0, n);
		this.pos = pos_len;
		return n;
	}

	@Override
	public final int write(byte[] buf, int off, int len) throws IOException {
		ensureOpen();
		if (buf == null) {
			throw new NullPointerException();
		}
		if (off < 0 || len < 0 || off > buf.length - len) {
			throw new ArrayIndexOutOfBoundsException();
		}
		long p = this.pos;
		int n = (int) Math.min(Long.MAX_VALUE - p, len);
		if (n == 0) {
			return 0;
		}
		long pos_len = p + n;
		_write(p, pos_len, buf, off, n);
		this.pos = pos_len;
		return n;
	}

	@Override
	public final int write(long pos, byte[] buf) throws IOException {
		ensureOpen();
		if (buf == null) {
			throw new NullPointerException();
		}
		if (pos < 0L) {
			throw new IndexOutOfBoundsException("pos < 0: " + pos);
		}
		int n = (int) Math.min(Long.MAX_VALUE - pos, buf.length);
		if (n == 0) {
			return 0;
		}
		long pos_len = pos + n;
		_write(pos, pos_len, buf, 0, n);
		return n;
	}

	@Override
	public final int write(long pos, byte[] buf, int off, int len) throws IOException {
		ensureOpen();
		if (buf == null) {
			throw new NullPointerException();
		}
		if (off < 0 || len < 0 || off > buf.length - len) {
			throw new ArrayIndexOutOfBoundsException();
		}
		if (pos < 0L) {
			throw new IndexOutOfBoundsException("pos < 0: " + pos);
		}
		int n = (int) Math.min(Long.MAX_VALUE - pos, len);
		if (n == 0) {
			return 0;
		}
		long pos_len = pos + n;
		_write(pos, pos_len, buf, off, n);
		return n;
	}

	///////////////////////////////////////////////////////////////
	// PRIMITIVE READING
	///////////////////////////////////////////////////////////////
	abstract int _readFixed(long pos, int len, long pos_len, long s) throws IOException;

	// disguised array access
	abstract byte getByte(int index);

	abstract char getChar(Endian endian, int index);

	abstract short getShort(Endian endian, int index);

	abstract int getInt(Endian endian, int index);

	abstract float getFloat(Endian endian, int index);

	abstract long getLong(Endian endian, int index);

	abstract double getDouble(Endian endian, int index);

	@Override
	public final byte readByte() throws IOException {
		ensureOpen();
		long p = this.pos;
		long s = this.size;
		if (p > s - 1L) {
			throw new IndexOutOfBoundsException("position() + 1 > size(). position: " + p + ", size: " + s);
		}
		long pos_len = p + 1L;
		byte n = getByte(_readFixed(p, 1, pos_len, s));
		this.pos = pos_len;
		return n;
	}

	@Override
	public final byte readByte(long pos) throws IOException {
		ensureOpen();
		if (pos < 0L) {
			throw new IndexOutOfBoundsException("pos < 0: " + pos);
		}
		long s = this.size;
		if (pos > s - 1L) {
			throw new IndexOutOfBoundsException("pos + 1 > size(). pos: " + pos + ", size: " + s);
		}
		long pos_len = pos + 1L;
		return getByte(_readFixed(pos, 1, pos_len, s));
	}

	@Override
	public final char readChar() throws IOException {
		ensureOpen();
		long p = this.pos;
		long s = this.size;
		if (p > s - 2L) {
			throw new IndexOutOfBoundsException("position() + 2 > size(). position: " + p + ", size: " + s);
		}
		long pos_len = p + 2L;
		char n = getChar(this.endian, _readFixed(p, 2, pos_len, s));
		this.pos = pos_len;
		return n;
	}

	@Override
	public final char readChar(Endian endian) throws IOException {
		ensureOpen();
		if (endian == null) {
			throw new NullPointerException();
		}
		long p = this.pos;
		long s = this.size;
		if (p > s - 2L) {
			throw new IndexOutOfBoundsException("position() + 2 > size(). position: " + p + ", size: " + s);
		}
		long pos_len = p + 2L;
		char n = getChar(endian, _readFixed(p, 2, pos_len, s));
		this.pos = pos_len;
		return n;
	}

	@Override
	public final char readChar(long pos) throws IOException {
		ensureOpen();
		if (pos < 0L) {
			throw new IndexOutOfBoundsException("pos < 0: " + pos);
		}
		long s = this.size;
		if (pos > s - 2L) {
			throw new IndexOutOfBoundsException("pos + 2 > size(). pos: " + pos + ", size: " + s);
		}
		long pos_len = pos + 2L;
		return getChar(this.endian, _readFixed(pos, 2, pos_len, s));
	}

	@Override
	public final char readChar(long pos, Endian endian) throws IOException {
		ensureOpen();
		if (endian == null) {
			throw new NullPointerException();
		}
		if (pos < 0L) {
			throw new IndexOutOfBoundsException("pos < 0: " + pos);
		}
		long s = this.size;
		if (pos > s - 2L) {
			throw new IndexOutOfBoundsException("pos + 2 > size(). pos: " + pos + ", size: " + s);
		}
		long pos_len = pos + 2L;
		return getChar(endian, _readFixed(pos, 2, pos_len, s));
	}

	@Override
	public final short readShort() throws IOException {
		ensureOpen();
		long p = this.pos;
		long s = this.size;
		if (p > s - 2L) {
			throw new IndexOutOfBoundsException("position() + 2 > size(). position: " + p + ", size: " + s);
		}
		long pos_len = p + 2L;
		short n = getShort(this.endian, _readFixed(p, 2, pos_len, s));
		this.pos = pos_len;
		return n;
	}

	@Override
	public final short readShort(Endian endian) throws IOException {
		ensureOpen();
		if (endian == null) {
			throw new NullPointerException();
		}
		long p = this.pos;
		long s = this.size;
		if (p > s - 2L) {
			throw new IndexOutOfBoundsException("position() + 2 > size(). position: " + p + ", size: " + s);
		}
		long pos_len = p + 2L;
		short n = getShort(endian, _readFixed(p, 2, pos_len, s));
		this.pos = pos_len;
		return n;
	}

	@Override
	public final short readShort(long pos) throws IOException {
		ensureOpen();
		if (pos < 0L) {
			throw new IndexOutOfBoundsException("pos < 0: " + pos);
		}
		long s = this.size;
		if (pos > s - 2L) {
			throw new IndexOutOfBoundsException("pos + 2 > size(). pos: " + pos + ", size: " + s);
		}
		long pos_len = pos + 2L;
		return getShort(this.endian, _readFixed(pos, 2, pos_len, s));
	}

	@Override
	public final short readShort(long pos, Endian endian) throws IOException {
		ensureOpen();
		if (endian == null) {
			throw new NullPointerException();
		}
		if (pos < 0L) {
			throw new IndexOutOfBoundsException("pos < 0: " + pos);
		}
		long s = this.size;
		if (pos > s - 2L) {
			throw new IndexOutOfBoundsException("pos + 2 > size(). pos: " + pos + ", size: " + s);
		}
		long pos_len = pos + 2L;
		return getShort(endian, _readFixed(pos, 2, pos_len, s));
	}

	@Override
	public final int readInt() throws IOException {
		ensureOpen();
		long p = this.pos;
		long s = this.size;
		if (p > s - 4L) {
			throw new IndexOutOfBoundsException("position() + 4 > size(). position: " + p + ", size: " + s);
		}
		long pos_len = p + 4L;
		int n = getInt(this.endian, _readFixed(p, 4, pos_len, s));
		this.pos = pos_len;
		return n;
	}

	@Override
	public final int readInt(Endian endian) throws IOException {
		ensureOpen();
		if (endian == null) {
			throw new NullPointerException();
		}
		long p = this.pos;
		long s = this.size;
		if (p > s - 4L) {
			throw new IndexOutOfBoundsException("position() + 4 > size(). position: " + p + ", size: " + s);
		}
		long pos_len = p + 4L;
		int n = getInt(endian, _readFixed(p, 4, pos_len, s));
		this.pos = pos_len;
		return n;
	}

	@Override
	public final int readInt(long pos) throws IOException {
		ensureOpen();
		if (pos < 0L) {
			throw new IndexOutOfBoundsException("pos < 0: " + pos);
		}
		long s = this.size;
		if (pos > s - 4L) {
			throw new IndexOutOfBoundsException("pos + 4 > size(). pos: " + pos + ", size: " + s);
		}
		long pos_len = pos + 4L;
		return getInt(this.endian, _readFixed(pos, 4, pos_len, s));
	}

	@Override
	public final int readInt(long pos, Endian endian) throws IOException {
		ensureOpen();
		if (endian == null) {
			throw new NullPointerException();
		}
		if (pos < 0L) {
			throw new IndexOutOfBoundsException("pos < 0: " + pos);
		}
		long s = this.size;
		if (pos > s - 4L) {
			throw new IndexOutOfBoundsException("pos + 4 > size(). pos: " + pos + ", size: " + s);
		}
		long pos_len = pos + 4L;
		return getInt(endian, _readFixed(pos, 4, pos_len, s));
	}

	@Override
	public final float readFloat() throws IOException {
		ensureOpen();
		long p = this.pos;
		long s = this.size;
		if (p > s - 4L) {
			throw new IndexOutOfBoundsException("position() + 4 > size(). position: " + p + ", size: " + s);
		}
		long pos_len = p + 4L;
		float n = getFloat(this.endian, _readFixed(p, 4, pos_len, s));
		this.pos = pos_len;
		return n;
	}

	@Override
	public final float readFloat(Endian endian) throws IOException {
		ensureOpen();
		if (endian == null) {
			throw new NullPointerException();
		}
		long p = this.pos;
		long s = this.size;
		if (p > s - 4L) {
			throw new IndexOutOfBoundsException("position() + 4 > size(). position: " + p + ", size: " + s);
		}
		long pos_len = p + 4L;
		float n = getFloat(endian, _readFixed(p, 4, pos_len, s));
		this.pos = pos_len;
		return n;
	}

	@Override
	public final float readFloat(long pos) throws IOException {
		ensureOpen();
		if (pos < 0L) {
			throw new IndexOutOfBoundsException("pos < 0: " + pos);
		}
		long s = this.size;
		if (pos > s - 4L) {
			throw new IndexOutOfBoundsException("pos + 4 > size(). pos: " + pos + ", size: " + s);
		}
		long pos_len = pos + 4L;
		return getFloat(this.endian, _readFixed(pos, 4, pos_len, s));
	}

	@Override
	public final float readFloat(long pos, Endian endian) throws IOException {
		ensureOpen();
		if (endian == null) {
			throw new NullPointerException();
		}
		if (pos < 0L) {
			throw new IndexOutOfBoundsException("pos < 0: " + pos);
		}
		long s = this.size;
		if (pos > s - 4L) {
			throw new IndexOutOfBoundsException("pos + 4 > size(). pos: " + pos + ", size: " + s);
		}
		long pos_len = pos + 4L;
		return getFloat(endian, _readFixed(pos, 4, pos_len, s));
	}

	@Override
	public final long readLong() throws IOException {
		ensureOpen();
		long p = this.pos;
		long s = this.size;
		if (p > s - 8L) {
			throw new IndexOutOfBoundsException("position() + 8 > size(). position: " + p + ", size: " + s);
		}
		long pos_len = p + 8L;
		long n = getLong(this.endian, _readFixed(p, 8, pos_len, s));
		this.pos = pos_len;
		return n;
	}

	@Override
	public final long readLong(Endian endian) throws IOException {
		ensureOpen();
		if (endian == null) {
			throw new NullPointerException();
		}
		long p = this.pos;
		long s = this.size;
		if (p > s - 8L) {
			throw new IndexOutOfBoundsException("position() + 8 > size(). position: " + p + ", size: " + s);
		}
		long pos_len = p + 8L;
		long n = getLong(endian, _readFixed(p, 8, pos_len, s));
		this.pos = pos_len;
		return n;
	}

	@Override
	public final long readLong(long pos) throws IOException {
		ensureOpen();
		if (pos < 0L) {
			throw new IndexOutOfBoundsException("pos < 0: " + pos);
		}
		long s = this.size;
		if (pos > s - 8L) {
			throw new IndexOutOfBoundsException("pos + 8 > size(). pos: " + pos + ", size: " + s);
		}
		long pos_len = pos + 8L;
		return getLong(this.endian, _readFixed(pos, 8, pos_len, s));
	}

	@Override
	public final long readLong(long pos, Endian endian) throws IOException {
		ensureOpen();
		if (endian == null) {
			throw new NullPointerException();
		}
		if (pos < 0L) {
			throw new IndexOutOfBoundsException("pos < 0: " + pos);
		}
		long s = this.size;
		if (pos > s - 8L) {
			throw new IndexOutOfBoundsException("pos + 8 > size(). pos: " + pos + ", size: " + s);
		}
		long pos_len = pos + 8L;
		return getLong(endian, _readFixed(pos, 8, pos_len, s));
	}

	@Override
	public final double readDouble() throws IOException {
		ensureOpen();
		long p = this.pos;
		long s = this.size;
		if (p > s - 8L) {
			throw new IndexOutOfBoundsException("position() + 8 > size(). position: " + p + ", size: " + s);
		}
		long pos_len = p + 8L;
		double n = getDouble(this.endian, _readFixed(p, 8, pos_len, s));
		this.pos = pos_len;
		return n;
	}

	@Override
	public final double readDouble(Endian endian) throws IOException {
		ensureOpen();
		if (endian == null) {
			throw new NullPointerException();
		}
		long p = this.pos;
		long s = this.size;
		if (p > s - 8L) {
			throw new IndexOutOfBoundsException("position() + 8 > size(). position: " + p + ", size: " + s);
		}
		long pos_len = p + 8L;
		double n = getDouble(endian, _readFixed(p, 8, pos_len, s));
		this.pos = pos_len;
		return n;
	}

	@Override
	public final double readDouble(long pos) throws IOException {
		ensureOpen();
		if (pos < 0L) {
			throw new IndexOutOfBoundsException("pos < 0: " + pos);
		}
		long s = this.size;
		if (pos > s - 8L) {
			throw new IndexOutOfBoundsException("pos + 8 > size(). pos: " + pos + ", size: " + s);
		}
		long pos_len = pos + 8L;
		return getDouble(this.endian, _readFixed(pos, 8, pos_len, s));
	}

	@Override
	public final double readDouble(long pos, Endian endian) throws IOException {
		ensureOpen();
		if (endian == null) {
			throw new NullPointerException();
		}
		if (pos < 0L) {
			throw new IndexOutOfBoundsException("pos < 0: " + pos);
		}
		long s = this.size;
		if (pos > s - 8L) {
			throw new IndexOutOfBoundsException("pos + 8 > size(). pos: " + pos + ", size: " + s);
		}
		long pos_len = pos + 8L;
		return getDouble(endian, _readFixed(pos, 8, pos_len, s));
	}

	///////////////////////////////////////////////////////////////
	// PRIMITIVE WRITING
	///////////////////////////////////////////////////////////////
	abstract int _writeFixed(long pos, int len, long pos_len) throws IOException;

	// disguised array access
	abstract void putByte(byte n, int index);

	abstract void putChar(Endian endian, char n, int index);

	abstract void putShort(Endian endian, short n, int index);

	abstract void putInt(Endian endian, int n, int index);

	abstract void putFloat(Endian endian, float n, int index);

	abstract void putLong(Endian endian, long n, int index);

	abstract void putDouble(Endian endian, double n, int index);

	@Override
	public final IOBuffer writeByte(byte n) throws IOException {
		ensureOpen();
		long p = this.pos;
		long pos_len = p + 1L;
		if (pos_len < 0L) {
			throw new IndexOutOfBoundsException("position() + 1 > Long.MAX_VALUE: " + p);
		}
		putByte(n, _writeFixed(p, 1, pos_len));
		this.pos = pos_len;
		return this;
	}

	@Override
	public final IOBuffer writeByte(byte n, long pos) throws IOException {
		ensureOpen();
		if (pos < 0L) {
			throw new IndexOutOfBoundsException("pos < 0: " + pos);
		}
		long pos_len = pos + 1L;
		if (pos_len < 0L) {
			throw new IndexOutOfBoundsException("pos + 1 > Long.MAX_VALUE: " + pos);
		}
		putByte(n, _writeFixed(pos, 1, pos_len));
		return this;
	}

	@Override
	public final IOBuffer writeChar(char n) throws IOException {
		ensureOpen();
		long p = this.pos;
		long pos_len = p + 2L;
		if (pos_len < 0L) {
			throw new IndexOutOfBoundsException("position() + 2 > Long.MAX_VALUE: " + p);
		}
		putChar(this.endian, n, _writeFixed(p, 2, pos_len));
		this.pos = pos_len;
		return this;
	}

	@Override
	public final IOBuffer writeChar(char n, Endian endian) throws IOException {
		ensureOpen();
		if (endian == null) {
			throw new NullPointerException();
		}
		long p = this.pos;
		long pos_len = p + 2L;
		if (pos_len < 0L) {
			throw new IndexOutOfBoundsException("position() + 2 > Long.MAX_VALUE: " + p);
		}
		putChar(endian, n, _writeFixed(p, 2, pos_len));
		this.pos = pos_len;
		return this;
	}

	@Override
	public final IOBuffer writeChar(char n, long pos) throws IOException {
		ensureOpen();
		if (pos < 0L) {
			throw new IndexOutOfBoundsException("pos < 0: " + pos);
		}
		long pos_len = pos + 2L;
		if (pos_len < 0L) {
			throw new IndexOutOfBoundsException("pos + 2 > Long.MAX_VALUE: " + pos);
		}
		putChar(this.endian, n, _writeFixed(pos, 2, pos_len));
		return this;
	}

	@Override
	public final IOBuffer writeChar(char n, long pos, Endian endian) throws IOException {
		ensureOpen();
		if (endian == null) {
			throw new NullPointerException();
		}
		if (pos < 0L) {
			throw new IndexOutOfBoundsException("pos < 0: " + pos);
		}
		long pos_len = pos + 2L;
		if (pos_len < 0L) {
			throw new IndexOutOfBoundsException("pos + 2 > Long.MAX_VALUE: " + pos);
		}
		putChar(endian, n, _writeFixed(pos, 2, pos_len));
		return this;
	}

	@Override
	public final IOBuffer writeShort(short n) throws IOException {
		ensureOpen();
		long p = this.pos;
		long pos_len = p + 2L;
		if (pos_len < 0L) {
			throw new IndexOutOfBoundsException("position() + 2 > Long.MAX_VALUE: " + p);
		}
		putShort(this.endian, n, _writeFixed(p, 2, pos_len));
		this.pos = pos_len;
		return this;
	}

	@Override
	public final IOBuffer writeShort(short n, Endian endian) throws IOException {
		ensureOpen();
		if (endian == null) {
			throw new NullPointerException();
		}
		long p = this.pos;
		long pos_len = p + 2L;
		if (pos_len < 0L) {
			throw new IndexOutOfBoundsException("position() + 2 > Long.MAX_VALUE: " + p);
		}
		putShort(endian, n, _writeFixed(p, 2, pos_len));
		this.pos = pos_len;
		return this;
	}

	@Override
	public final IOBuffer writeShort(short n, long pos) throws IOException {
		ensureOpen();
		if (pos < 0L) {
			throw new IndexOutOfBoundsException("pos < 0: " + pos);
		}
		long pos_len = pos + 2L;
		if (pos_len < 0L) {
			throw new IndexOutOfBoundsException("pos + 2 > Long.MAX_VALUE: " + pos);
		}
		putShort(this.endian, n, _writeFixed(pos, 2, pos_len));
		return this;
	}

	@Override
	public final IOBuffer writeShort(short n, long pos, Endian endian) throws IOException {
		ensureOpen();
		if (endian == null) {
			throw new NullPointerException();
		}
		if (pos < 0L) {
			throw new IndexOutOfBoundsException("pos < 0: " + pos);
		}
		long pos_len = pos + 2L;
		if (pos_len < 0L) {
			throw new IndexOutOfBoundsException("pos + 2 > Long.MAX_VALUE: " + pos);
		}
		putShort(endian, n, _writeFixed(pos, 2, pos_len));
		return this;
	}

	@Override
	public final IOBuffer writeInt(int n) throws IOException {
		ensureOpen();
		long p = this.pos;
		long pos_len = p + 4L;
		if (pos_len < 0L) {
			throw new IndexOutOfBoundsException("position() + 4 > Long.MAX_VALUE: " + p);
		}
		putInt(this.endian, n, _writeFixed(p, 4, pos_len));
		this.pos = pos_len;
		return this;
	}

	@Override
	public final IOBuffer writeInt(int n, Endian endian) throws IOException {
		ensureOpen();
		if (endian == null) {
			throw new NullPointerException();
		}
		long p = this.pos;
		long pos_len = p + 4L;
		if (pos_len < 0L) {
			throw new IndexOutOfBoundsException("position() + 4 > Long.MAX_VALUE: " + p);
		}
		putInt(endian, n, _writeFixed(p, 4, pos_len));
		this.pos = pos_len;
		return this;
	}

	@Override
	public final IOBuffer writeInt(int n, long pos) throws IOException {
		ensureOpen();
		if (pos < 0L) {
			throw new IndexOutOfBoundsException("pos < 0: " + pos);
		}
		long pos_len = pos + 4L;
		if (pos_len < 0L) {
			throw new IndexOutOfBoundsException("pos + 4 > Long.MAX_VALUE: " + pos);
		}
		putInt(this.endian, n, _writeFixed(pos, 4, pos_len));
		return this;
	}

	@Override
	public final IOBuffer writeInt(int n, long pos, Endian endian) throws IOException {
		ensureOpen();
		if (endian == null) {
			throw new NullPointerException();
		}
		if (pos < 0L) {
			throw new IndexOutOfBoundsException("pos < 0: " + pos);
		}
		long pos_len = pos + 4L;
		if (pos_len < 0L) {
			throw new IndexOutOfBoundsException("pos + 4 > Long.MAX_VALUE: " + pos);
		}
		putInt(endian, n, _writeFixed(pos, 4, pos_len));
		return this;
	}

	@Override
	public final IOBuffer writeFloat(float n) throws IOException {
		ensureOpen();
		long p = this.pos;
		long pos_len = p + 4L;
		if (pos_len < 0L) {
			throw new IndexOutOfBoundsException("position() + 4 > Long.MAX_VALUE: " + p);
		}
		putFloat(this.endian, n, _writeFixed(p, 4, pos_len));
		this.pos = pos_len;
		return this;
	}

	@Override
	public final IOBuffer writeFloat(float n, Endian endian) throws IOException {
		ensureOpen();
		if (endian == null) {
			throw new NullPointerException();
		}
		long p = this.pos;
		long pos_len = p + 4L;
		if (pos_len < 0L) {
			throw new IndexOutOfBoundsException("position() + 4 > Long.MAX_VALUE: " + p);
		}
		putFloat(endian, n, _writeFixed(p, 4, pos_len));
		this.pos = pos_len;
		return this;
	}

	@Override
	public final IOBuffer writeFloat(float n, long pos) throws IOException {
		ensureOpen();
		if (pos < 0L) {
			throw new IndexOutOfBoundsException("pos < 0: " + pos);
		}
		long pos_len = pos + 4L;
		if (pos_len < 0L) {
			throw new IndexOutOfBoundsException("pos + 4 > Long.MAX_VALUE: " + pos);
		}
		putFloat(this.endian, n, _writeFixed(pos, 4, pos_len));
		return this;
	}

	@Override
	public final IOBuffer writeFloat(float n, long pos, Endian endian) throws IOException {
		ensureOpen();
		if (endian == null) {
			throw new NullPointerException();
		}
		if (pos < 0L) {
			throw new IndexOutOfBoundsException("pos < 0: " + pos);
		}
		long pos_len = pos + 4L;
		if (pos_len < 0L) {
			throw new IndexOutOfBoundsException("pos + 4 > Long.MAX_VALUE: " + pos);
		}
		putFloat(endian, n, _writeFixed(pos, 4, pos_len));
		return this;
	}

	@Override
	public final IOBuffer writeLong(long n) throws IOException {
		ensureOpen();
		long p = this.pos;
		long pos_len = p + 8L;
		if (pos_len < 0L) {
			throw new IndexOutOfBoundsException("position() + 8 > Long.MAX_VALUE: " + p);
		}
		putLong(this.endian, n, _writeFixed(p, 8, pos_len));
		this.pos = pos_len;
		return this;
	}

	@Override
	public final IOBuffer writeLong(long n, Endian endian) throws IOException {
		ensureOpen();
		if (endian == null) {
			throw new NullPointerException();
		}
		long p = this.pos;
		long pos_len = p + 8L;
		if (pos_len < 0L) {
			throw new IndexOutOfBoundsException("position() + 8 > Long.MAX_VALUE: " + p);
		}
		putLong(endian, n, _writeFixed(p, 8, pos_len));
		this.pos = pos_len;
		return this;
	}

	@Override
	public final IOBuffer writeLong(long n, long pos) throws IOException {
		ensureOpen();
		if (pos < 0L) {
			throw new IndexOutOfBoundsException("pos < 0: " + pos);
		}
		long pos_len = pos + 8L;
		if (pos_len < 0L) {
			throw new IndexOutOfBoundsException("pos + 8 > Long.MAX_VALUE: " + pos);
		}
		putLong(this.endian, n, _writeFixed(pos, 8, pos_len));
		return this;
	}

	@Override
	public final IOBuffer writeLong(long n, long pos, Endian endian) throws IOException {
		ensureOpen();
		if (endian == null) {
			throw new NullPointerException();
		}
		if (pos < 0L) {
			throw new IndexOutOfBoundsException("pos < 0: " + pos);
		}
		long pos_len = pos + 8L;
		if (pos_len < 0L) {
			throw new IndexOutOfBoundsException("pos + 8 > Long.MAX_VALUE: " + pos);
		}
		putLong(endian, n, _writeFixed(pos, 8, pos_len));
		return this;
	}

	@Override
	public final IOBuffer writeDouble(double n) throws IOException {
		ensureOpen();
		long p = this.pos;
		long pos_len = p + 8L;
		if (pos_len < 0L) {
			throw new IndexOutOfBoundsException("position() + 8 > Long.MAX_VALUE: " + p);
		}
		putDouble(this.endian, n, _writeFixed(p, 8, pos_len));
		this.pos = pos_len;
		return this;
	}

	@Override
	public final IOBuffer writeDouble(double n, Endian endian) throws IOException {
		ensureOpen();
		if (endian == null) {
			throw new NullPointerException();
		}
		long p = this.pos;
		long pos_len = p + 8L;
		if (pos_len < 0L) {
			throw new IndexOutOfBoundsException("position() + 8 > Long.MAX_VALUE: " + p);
		}
		putDouble(endian, n, _writeFixed(p, 8, pos_len));
		this.pos = pos_len;
		return this;
	}

	@Override
	public final IOBuffer writeDouble(double n, long pos) throws IOException {
		ensureOpen();
		if (pos < 0L) {
			throw new IndexOutOfBoundsException("pos < 0: " + pos);
		}
		long pos_len = pos + 8L;
		if (pos_len < 0L) {
			throw new IndexOutOfBoundsException("pos + 8 > Long.MAX_VALUE: " + pos);
		}
		putDouble(this.endian, n, _writeFixed(pos, 8, pos_len));
		return this;
	}

	@Override
	public final IOBuffer writeDouble(double n, long pos, Endian endian) throws IOException {
		ensureOpen();
		if (endian == null) {
			throw new NullPointerException();
		}
		if (pos < 0L) {
			throw new IndexOutOfBoundsException("pos < 0: " + pos);
		}
		long pos_len = pos + 8L;
		if (pos_len < 0L) {
			throw new IndexOutOfBoundsException("pos + 8 > Long.MAX_VALUE: " + pos);
		}
		putDouble(endian, n, _writeFixed(pos, 8, pos_len));
		return this;
	}

	///////////////////////////////////////////////////////////////
	// ENCODING
	///////////////////////////////////////////////////////////////
	abstract int _decode(Decoder dec, Appendable dest, int maxChars, int maxCodePoints, long pos, long end, long s) throws IOException;

	abstract int _encode(Encoder enc, int inputChars, long p, long end, int maxCodePoints) throws IOException;

	abstract String _nextLine(Decoder dec, String replace) throws IOException;

	@Override
	public final int decode(Decoder dec, int inputBytes, Appendable dest, int maxChars, int maxCodePoints) throws IOException {
		ensureOpen();
		if (dec == null) {
			throw new NullPointerException();
		}
		if (dest == null) {
			throw new NullPointerException();
		}
		if (dec.inputRemaining() != 0) {
			throw new IllegalStateException("Decoder has input remaining.");
		}
		if (inputBytes < 0) {
			throw new IllegalArgumentException("inputBytes < 0: " + inputBytes);
		}
		if (maxChars < 0) {
			throw new IllegalArgumentException("maxChars < 0: " + maxChars);
		}
		if (maxCodePoints < 0) {
			throw new IllegalArgumentException("maxCodePoints < 0: " + maxCodePoints);
		}
		long p = this.pos;
		long s = this.size;
		return _decode(dec, dest, maxChars, maxCodePoints, p, p + Math.min(inputBytes, s - p), s);
	}

	@Override
	public final int decode(Decoder dec, int inputBytes, Appendable dest) throws IOException {
		ensureOpen();
		if (dec == null) {
			throw new NullPointerException();
		}
		if (dest == null) {
			throw new NullPointerException();
		}
		if (dec.inputRemaining() != 0) {
			throw new IllegalStateException("Decoder has input remaining.");
		}
		if (inputBytes < 0) {
			throw new IllegalArgumentException("inputBytes < 0: " + inputBytes);
		}
		long p = this.pos;
		long s = this.size;
		return _decode(dec, dest, Integer.MAX_VALUE, Integer.MAX_VALUE, p, p + Math.min(inputBytes, s - p), s);
	}

	@Override
	public final int decode(Decoder dec, Appendable dest) throws IOException {
		ensureOpen();
		if (dec == null) {
			throw new NullPointerException();
		}
		if (dest == null) {
			throw new NullPointerException();
		}
		if (dec.inputRemaining() != 0) {
			throw new IllegalStateException("Decoder has input remaining.");
		}
		long p = this.pos;
		long s = this.size;
		return _decode(dec, dest, Integer.MAX_VALUE, Integer.MAX_VALUE, p, s, s);
	}

	@Override
	public final int encode(Encoder enc, int inputChars, int maxBytes, int maxCodePoints) throws IOException {
		ensureOpen();
		if (enc == null) {
			throw new NullPointerException();
		}
		if (inputChars < 0) {
			throw new IllegalArgumentException("inputChars < 0: " + inputChars);
		}
		if (maxBytes < 0) {
			throw new IllegalArgumentException("maxChars < 0: " + maxBytes);
		}
		if (maxCodePoints < 0) {
			throw new IllegalArgumentException("maxCodePoints < 0: " + maxCodePoints);
		}
		long p = this.pos;
		return _encode(enc, Math.min(inputChars, enc.inputRemaining()), p, p + Math.min(maxBytes, Long.MAX_VALUE - p), maxCodePoints);
	}

	@Override
	public final int encode(Encoder enc, int inputChars) throws IOException {
		ensureOpen();
		if (enc == null) {
			throw new NullPointerException();
		}
		if (inputChars < 0) {
			throw new IllegalArgumentException("inputChars < 0: " + inputChars);
		}
		return _encode(enc, Math.min(inputChars, enc.inputRemaining()), this.pos, Long.MAX_VALUE, Integer.MAX_VALUE);
	}

	@Override
	public final int encode(Encoder enc) throws IOException {
		ensureOpen();
		if (enc == null) {
			throw new NullPointerException();
		}
		return _encode(enc, enc.inputRemaining(), this.pos, Long.MAX_VALUE, Integer.MAX_VALUE);
	}

	@Override
	public final String nextLine(Decoder dec) throws IOException {
		ensureOpen();
		if (dec == null) {
			throw new NullPointerException();
		}
		if (dec.inputRemaining() != 0) {
			throw new IllegalStateException("Decoder has input remaining.");
		}
		if (dec.pendingOutput() != 0) {
			throw new IllegalStateException("Decoder has pending output.");
		}
		return _nextLine(dec, "\uFFFD");
	}

	@Override
	public final String nextLine(Decoder dec, String replace) throws IOException {
		ensureOpen();
		if (dec == null) {
			throw new NullPointerException();
		}
		if (replace == null) {
			throw new NullPointerException();
		}
		if (dec.inputRemaining() != 0) {
			throw new IllegalStateException("Decoder has input remaining.");
		}
		if (dec.pendingOutput() != 0) {
			throw new IllegalStateException("Decoder has pending output.");
		}
		return _nextLine(dec, replace);
	}

	///////////////////////////////////////////////////////////////
	// INFLATE
	///////////////////////////////////////////////////////////////
	abstract int _inflate(byte[] dest, int off, int len, Inflater inf, long pos, long end, long s) throws IOException, DataFormatException;

	abstract long _inflate(IOBuffer dest, long numBytesOut, Inflater inf, long pos, long end, long s) throws IOException, DataFormatException;

	@Override
	public final int inflate(long numBytesIn, byte[] dest, int off, int len, Inflater inf) throws IOException, DataFormatException {
		ensureOpen();
		if (dest == null) {
			throw new NullPointerException();
		}
		if (numBytesIn < 0L) {
			throw new IllegalArgumentException("numBytesIn < 0: " + numBytesIn);
		}
		if (off < 0 || len < 0 || off > dest.length - len) {
			throw new IndexOutOfBoundsException();
		}
		if (inf.getRemaining() != 0) {
			throw new IllegalStateException("Inflater has input remaining.");
		}
		long p = this.pos;
		long s = this.size;
		return _inflate(dest, off, len, inf, p, p + Math.min(numBytesIn, s - p), s);
	}

	@Override
	public final int inflate(byte[] dest, int off, int len, Inflater inf) throws IOException, DataFormatException {
		ensureOpen();
		if (dest == null) {
			throw new NullPointerException();
		}
		if (off < 0 || len < 0 || off > dest.length - len) {
			throw new IndexOutOfBoundsException();
		}
		if (inf.getRemaining() != 0) {
			throw new IllegalStateException("Inflater has input remaining.");
		}
		long s = this.size;
		return _inflate(dest, off, len, inf, this.pos, s, s);
	}

	@Override
	public final long inflate(long numBytesIn, IOBuffer dest, long numBytesOut, Inflater inf) throws IOException, DataFormatException {
		ensureOpen();
		if (dest == null) {
			throw new NullPointerException();
		}
		if (dest == this) {
			throw new IllegalArgumentException("Destination IOBuffer must not be this IOBuffer");
		}
		if (numBytesIn < 0L) {
			throw new IllegalArgumentException("numBytesIn < 0: " + numBytesIn);
		}
		if (numBytesOut < 0L) {
			throw new IllegalArgumentException("numBytesOut < 0: " + numBytesOut);
		}
		if (inf.getRemaining() != 0) {
			throw new IllegalStateException("Inflater has input remaining.");
		}
		long p = this.pos;
		long s = this.size;
		return _inflate(dest, numBytesOut, inf, p, p + Math.min(numBytesIn, s - p), s);
	}

	@Override
	public final long inflate(IOBuffer dest, Inflater inf) throws IOException, DataFormatException {
		ensureOpen();
		if (dest == null) {
			throw new NullPointerException();
		}
		if (dest == this) {
			throw new IllegalArgumentException("Destination IOBuffer must not be this IOBuffer");
		}
		if (inf.getRemaining() != 0) {
			throw new IllegalStateException("Inflater has input remaining.");
		}
		long s = this.size;
		return _inflate(dest, Long.MAX_VALUE, inf, this.pos, s, s);
	}

	abstract long _transfer(long numBytes, IOBuffer dest, long pos, long s) throws IOException;

	@Override
	public long transfer(long numBytes, IOBuffer dest) throws IOException {
		ensureOpen();
		if (dest == null) {
			throw new NullPointerException();
		}
		if (dest == this) {
			throw new IllegalArgumentException("Destination IOBuffer must not be this IOBuffer");
		}
		if (numBytes < 0L) {
			throw new IllegalArgumentException("numBytes < 0: " + numBytes);
		}
		long p = this.pos;
		long s = this.size;
		if (p >= s) {
			return 0L;
		}
		return _transfer(Math.min(numBytes, s - p), dest, p, s);
	}

	@Override
	public long transfer(IOBuffer dest) throws IOException {
		ensureOpen();
		if (dest == null) {
			throw new NullPointerException();
		}
		if (dest == this) {
			throw new IllegalArgumentException("Destination IOBuffer must not be this IOBuffer");
		}
		long p = this.pos;
		long s = this.size;
		if (p >= s) {
			return 0L;
		}
		return _transfer(s - p, dest, p, s);
	}

	//
	// CHECKSUMS
	// 
	abstract void _updateChecksum(long pos, Checksum sum, long end, long s);

	@Override
	public long updateChecksum(Checksum sum) throws IOException {
		ensureOpen();
		if (sum == null) {
			throw new NullPointerException();
		}
		long p = this.pos;
		long s = this.size;
		if (p >= s) {
			return -1L;
		}
		_updateChecksum(p, sum, s, s);
		this.pos = s;
		return s - p;
	}

	@Override
	public long updateChecksum(long pos, Checksum sum) throws IOException {
		if (sum == null) {
			throw new NullPointerException();
		}
		if (pos < 0L) {
			throw new IndexOutOfBoundsException("pos < 0: " + pos);
		}
		long s = this.size;
		if (pos >= s) {
			return -1L;
		}
		_updateChecksum(pos, sum, s, s);
		return s - pos;
	}

	@Override
	public long updateChecksum(Checksum sum, long len) throws IOException {
		if (sum == null) {
			throw new NullPointerException();
		}
		if (len < 0L) {
			throw new IndexOutOfBoundsException("len < 0: " + len);
		}
		if (len == 0L) {
			return 0L;
		}
		long p = this.pos;
		long s = this.size;
		long n = Math.min(s - p, len);
		if (n <= 0L) {
			return -1L;
		}
		long end = p + n;
		_updateChecksum(p, sum, end, s);
		this.pos = end;
		return n;
	}

	@Override
	public long updateChecksum(long pos, Checksum sum, long len) throws IOException {
		if (sum == null) {
			throw new NullPointerException();
		}
		if (pos < 0L) {
			throw new IndexOutOfBoundsException("pos < 0: " + pos);
		}
		if (len < 0L) {
			throw new IndexOutOfBoundsException("len < 0: " + len);
		}
		if (len == 0L) {
			return 0L;
		}
		long s = this.size;
		long n = Math.min(s - pos, len);
		if (n <= 0L) {
			// even if len == 0. makes EOF detection easier from call site
			// e.g. "do remaining while not -1" is possible this way, even if remaining == 0
			return -1L;
		}
		long end = pos + n;
		_updateChecksum(pos, sum, end, s);
		return n;
	}
}

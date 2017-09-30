/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.io;

import java.nio.channels.FileChannel;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntPredicate;
import java.util.zip.Inflater;
import org.bitsandpieces.util.Encoding.Decoder;
import org.bitsandpieces.util.Encoding.Encoder;
import org.bitsandpieces.util.Endian;

/**
 * General solution. Works for any kind of IOSource. Very efficient.
 *
 * @author Jan Kebernik
 */
final class GeneralIOBuffer extends IOBuffer {

	private static final int MIN_BUFFER_SIZE = 8;
	private static final byte[] EMPTY = {};
	private static final ObjectCache<StringBuilder> BUILDER_CACHE = new ObjectCache<>(() -> new StringBuilder());

	private final int bufferSize;
	private final byte[] buffer;
	private final StringBuilder sb;

	private final AtomicBoolean closed;

	private long bufPos;
	private int bufLen;
	private boolean bufMod;

	GeneralIOBuffer(IOSource source) throws IOException {
		super(source);
		this.closed = new AtomicBoolean();
		this.bufferSize = BufferCache.DEFAULT_SIZE;
		this.buffer = BufferCache.requestBuffer();
		this.sb = BUILDER_CACHE.requestInstance();
	}

	GeneralIOBuffer(IOSource source, int bufferSize) throws IOException {
		super(source);
		if (bufferSize < MIN_BUFFER_SIZE) {
			bufferSize = MIN_BUFFER_SIZE;
		}
		this.closed = new AtomicBoolean();
		this.bufferSize = bufferSize;
		this.buffer = BufferCache.requestBuffer(bufferSize);
		this.sb = BUILDER_CACHE.requestInstance();
	}

	GeneralIOBuffer(IOSource source, AtomicInteger shared) throws IOException {
		super(source, shared);
		this.closed = new AtomicBoolean();
		this.bufferSize = BufferCache.DEFAULT_SIZE;
		this.buffer = BufferCache.requestBuffer();
		this.sb = BUILDER_CACHE.requestInstance();
	}

	GeneralIOBuffer(IOSource source, AtomicInteger shared, int bufferSize) throws IOException {
		super(source, shared);
		if (bufferSize < MIN_BUFFER_SIZE) {
			bufferSize = MIN_BUFFER_SIZE;
		}
		this.closed = new AtomicBoolean();
		this.bufferSize = bufferSize;
		this.buffer = BufferCache.requestBuffer(bufferSize);
		this.sb = BUILDER_CACHE.requestInstance();
	}

	@Override
	public void close() throws IOException {
		// atomically close, idempotent.
		if (this.closed.compareAndSet(false, true)) {
			try {
				_flush();
			} finally {
				try {
					BUILDER_CACHE.releaseInstance(this.sb);
				} finally {
					try {
						BufferCache.releaseBuffer(this.buffer);
					} finally {
						closeSource();
					}
				}
			}
		}
	}

	@Override
	void _ensureOpen() throws IOException {
		if (this.closed.get()) {
			throw new IOException("IOBuffer was closed.");
		}
	}

	private void _flush() throws IOException {
		if (this.bufMod) {
			// rationale: save reading in two values unless bufMod true, which is rare
			_forceFlush(this.bufPos, this.bufLen);
		}
	}

	private void _flush(long bpos, int blen) throws IOException {
		if (this.bufMod) {
			_forceFlush(bpos, blen);
		}
	}

	private void _forceFlush(long bpos, int blen) throws IOException {
		this.source.write(bpos, this.buffer, 0, blen);
		// if flusing fails, the buffer remains unflushed.
		// if any operation requires a rebuffer, 
		// then it won't complete until the buffer is flushed, 
		// possibly throwing another exception as a result if 
		// flushing fails again.
		// this is the intended behaviour. the user  
		// has to determine the cause themselves (drive full/disconnected, 
		// file way too big, etc...), if the wrapped exception 
		// is not informative in of itself.
		this.bufMod = false;
	}
	@Override
	IOBuffer createSibling() throws IOException {
		_ensureOpen();	// for flush
		_flush();		// sync sibling with this buffer
		return new GeneralIOBuffer(this.source, this.shared, this.bufferSize);
	}

	@Override
	void _truncate(long size) throws IOException {
		if (size < this.size) {
			long bpos = this.bufPos;
			long bpos_blen = bpos + this.bufLen;
			if (size <= bpos) {
				// discard complete buffer 
				this.bufPos = 0L;
				this.bufLen = 0;
				this.bufMod = false;
			} else if (size < bpos_blen) {
				// discard partial buffer
				this.bufLen = (int) (size - bpos);
			}
			this.size = size;
			this.source.truncate(size);
		}
	}

	// reads "len" bytes starting at "pos" into "buf" at "off"
	private int _mustRead(long pos, byte[] buf, int off, int len) throws IOException {
		for (int n = 0; n != len;) {
			int r = this.source.read(pos + n, buf, off + n, len - n);
			if (r < 0) {
				throw new IOException("Unexpected EOF");
			}
			n += r;
		}
		return len;
	}

	// starts and fills a new buffer at "npos" and returns the offset into it
	// based on target "pos" along source.
	private int _readRebuffer(long pos, long npos, long s) throws IOException {
		this.bufLen = 0;	// required in case of errors
		int k = (int) Math.min(s - npos, this.bufferSize);
		_mustRead(npos, this.buffer, 0, k);
		this.bufPos = npos;
		this.bufLen = k;
		return (int) (pos - npos);
	}

	// used for very small reads that require the array to contain all bytes for 
	// the op. returns offset into array for the read op. immune to integer overflow.
	// favours sequential access in the same direction, meaning that the 
	// buffer moves in the trajectory of any repeated access.
	private int _readFixed(long pos, long pos_len, long s) throws IOException {
		long bpos = this.bufPos;
		int blen = this.bufLen;
		if (pos < bpos) {
			// op starts before buffer
			_flush(bpos, blen);	// requires full rebuffer
			if (pos_len <= bpos) {
				// op stops before buffer
				// try moving buffer left
				long start = bpos - Math.min(bpos, this.bufferSize);
				if (pos >= start) {
					// move buffer left
					return _readRebuffer(pos, start, s);
				}
				// moving left insufficient
			}
			// else: op stops inside buffer
			// move buffer as far left as possible (anchored to op)
			long npos = pos_len - Math.min(pos_len, this.bufferSize);
			return _readRebuffer(pos, npos, s);
		}
		// op starts inside or after buffer
		long bpos_blen = bpos + blen;
		if (pos_len > bpos_blen) {
			// op stops after buffer
			long bcap = bpos + Math.min(s - bpos, this.bufferSize);
			if (pos_len > bcap) {
				// op stops after buffer capacity
				_flush(bpos, blen);	// requires full rebuffer
				if (pos >= bpos_blen) {
					// op starts after buffer
					// try moving buffer right
					long end = bpos_blen + Math.min(Long.MAX_VALUE - bpos_blen, this.bufferSize);
					if (pos_len <= end) {
						// move buffer right
						return _readRebuffer(pos, bpos_blen, s);
					}
					// moving right insufficient
				}
				// else: op starts inside buffer
				// move buffer to pos
				return _readRebuffer(pos, pos, s);
			}
			// op starts and stops inside buffer capacity
			// fill the buffer (better buffer movement)
			this.bufLen += _mustRead(bpos_blen, this.buffer, blen, (int) (bcap - bpos_blen));
		}
		// op starts and stops inside buffer
		return (int) (pos - bpos);
	}

	// used for arbitrary reads. immune to integer overflow. 
	// favours sequential access in the same direction, meaning that the 
	// buffer moves in the trajectory of any repeated access. 
	@Override
	void _read(long pos, long pos_len, long s, byte[] buf, int off, int len) throws IOException {
		long bpos = this.bufPos;
		int blen = this.bufLen;
		if (pos < bpos) {
			// op starts before buffer
			_flush(bpos, blen);	// required in all cases
			if (pos_len <= bpos) {
				// op stops before buffer
				if (len >= this.bufferSize) {
					// don't bother buffering
					_mustRead(pos, buf, off, len);
					return;
				}
				// try moving buffer left
				long start = bpos - Math.min(bpos, this.bufferSize);
				if (pos >= start) {
					// move buffer left
					int f = _readRebuffer(pos, start, s);
					System.arraycopy(this.buffer, f, buf, off, len);
					return;
				}
				// moving left insufficient
				// move buffer as far left as possible (anchored to op)
				long npos = pos_len - Math.min(pos_len, this.bufferSize);
				int f = _readRebuffer(pos, npos, s);
				System.arraycopy(this.buffer, f, buf, off, len);
				return;
			}
			// op stops inside or after buffer
			long bpos_blen = bpos + blen;
			if (pos_len <= bpos_blen) {
				// op stops inside buffer
				// complete tail-end of op
				int x = (int) (pos_len - bpos);
				int r = len - x;
				System.arraycopy(this.buffer, 0, buf, off + r, x);
				// complete remainder of op
				if (r >= this.bufferSize) {
					// don't bother buffering
					_mustRead(pos, buf, off, r);
					return;
				}
				// move buffer left
				long npos = bpos - Math.min(bpos, this.bufferSize);
				int f = _readRebuffer(pos, npos, s);
				System.arraycopy(this.buffer, f, buf, off, r);
				return;
			}
			// op stops after buffer
			if (len >= this.bufferSize) {
				// don't bother buffering
				_mustRead(pos, buf, off, len);
				return;
			}
			// move buffer left as far as possible (anchor to op)
			long npos = pos_len - Math.min(pos_len, this.bufferSize);
			int f = _readRebuffer(pos, npos, s);
			System.arraycopy(this.buffer, f, buf, off, len);
			return;
		}
		// op starts inside or after buffer
		long bpos_blen = bpos + blen;
		if (pos_len > bpos_blen) {
			// op stops after buffer
			long bcap = bpos + Math.min(s - bpos, this.bufferSize);
			if (pos_len > bcap) {
				// op stops after buffer capacity
				_flush(bpos, blen);	// required in all cases
				if (pos >= bpos_blen) {
					// op starts after buffer
					if (len >= this.bufferSize) {
						// don't bother buffering
						_mustRead(pos, buf, off, len);
						return;
					}
					// try moving buffer right
					long end = bpos_blen + Math.min(Long.MAX_VALUE - bpos_blen, this.bufferSize);
					if (pos_len <= end) {
						// move buffer right
						int f = _readRebuffer(pos, bpos_blen, s);
						System.arraycopy(this.buffer, f, buf, off, len);
						return;
					}
					// moving right insufficient
					// move buffer to pos
					_readRebuffer(pos, pos, s);
					System.arraycopy(this.buffer, 0, buf, off, len);
					return;
				}
				// op starts inside buffer
				// complete head-end of op
				int x = (int) (pos - bpos);
				int y = blen - x;
				int r = len - y;
				System.arraycopy(this.buffer, x, buf, off, y);
				// complete remainder of op
				if (r >= this.bufferSize) {
					// don't bother buffering
					_mustRead(bpos_blen, buf, off + y, r);
					return;
				}
				// move buffer right
				_readRebuffer(bpos_blen, bpos_blen, s);
				System.arraycopy(this.buffer, 0, buf, off + y, r);
				return;
			}
			// op starts and stops inside buffer capacity
			// fill the buffer (better buffer movement)
			this.bufLen += _mustRead(bpos_blen, this.buffer, blen, (int) (bcap - bpos_blen));
		}
		// op starts and stops inside buffer
		System.arraycopy(this.buffer, (int) (pos - bpos), buf, off, len);
	}

	// assumption: at least one byte is available for reading
	// ensures that as many bytes as possible (and at least one) are 
	// available for reading in the array at "pos". But will only rebuffer
	// if no bytes are available at all.
	// returns the offset into the array.
	// favours forward sequential access.
	int _readSeek(long pos, long s) throws IOException {
		long bpos = this.bufPos;
		int blen = this.bufLen;
		if (pos < bpos) {
			// op starts before buffer
			_flush(bpos, blen);	// requires full rebuffer
			_readRebuffer(pos, pos, s);
			return 0;
		}
		// op starts inside or after buffer
		long bpos_blen = bpos + blen;
		if (pos >= bpos_blen) {
			// op stops after buffer
			long bcap = bpos + Math.min(s - bpos, this.bufferSize);
			if (pos >= bcap) {
				// op stops after buffer capacity
				_flush(bpos, blen);	// requires full rebuffer
				_readRebuffer(pos, pos, s);
				return 0;
			}
			// op starts and stops inside buffer capacity
			// fill the buffer (better buffer movement)
			this.bufLen += _mustRead(bpos_blen, this.buffer, blen, (int) (bcap - bpos_blen));
		}
		// op starts and stops inside buffer
		return (int) (pos - bpos);
	}

	// sets up a new buffer and fills it only if necessary.
	// updates bufLen and size for write op.
	// bufLen may have to be reset prior to calling this method.
	// returns given offset into array.
	private int _writeRebuffer(int off, int len, long pos_len, long npos, int blen, long s) throws IOException {
		if (off > blen) {
			// only read in bytes if a gap is created
			if (npos < s) {
				// and only if there are any bytes available at all
				int k = (int) Math.min(s - npos, this.bufferSize - blen);
				blen += _mustRead(npos, this.buffer, blen, k);
			}
			// fill remaining gap, if any, with 0s
			FastZeros.INSTANCE.fillWithZeros(this.buffer, blen, off - blen);
		}
		// update bufLen and size for the write op about to happen
		this.bufLen = Math.max(blen, off + len);
		if (pos_len > s) {
			this.size = pos_len;
		}
		this.bufMod = true;
		return off;
	}

	// used for very small writes that require the array to have space for all bytes for 
	// the op. returns offset into array for the write op. immune to integer overflow.
	// favours sequential access in the same direction, meaning that the 
	// buffer moves in the trajectory of any repeated access.
	private int _writeFixed(long pos, int len, long pos_len) throws IOException {
		long s = this.size;
		long bpos = this.bufPos;
		int blen = this.bufLen;
		if (pos < bpos) {
			// op starts before buffer
			_flush(bpos, blen);	// requires full rebuffer
			if (pos_len <= bpos) {
				// op stops before buffer
				// try moving buffer left
				long start = bpos - Math.min(bpos, this.bufferSize);
				if (pos >= start) {
					// move buffer left
					return _writeRebuffer((int) (pos - start), len, pos_len, this.bufPos = start, this.bufLen = 0, s);
				}
				// moving left insufficient
			}
			// else: op stops inside buffer
			// move buffer as far left as possible (anchored to op)
			long npos = pos_len - Math.min(pos_len, this.bufferSize);
			return _writeRebuffer((int) (pos - npos), len, pos_len, this.bufPos = npos, this.bufLen = 0, s);
		}
		// op starts inside or after buffer
		long bpos_blen = bpos + blen;
		if (pos_len > bpos_blen) {
			// op stops after buffer
			long bcap = bpos + Math.min(s - bpos, this.bufferSize);
			if (pos_len > bcap) {
				// op stops after buffer capacity
				_flush(bpos, blen);	// requires full rebuffer
				if (pos >= bpos_blen) {
					// op starts after buffer
					// try moving buffer right
					long end = bpos_blen + Math.min(Long.MAX_VALUE - bpos_blen, this.bufferSize);
					if (pos_len <= end) {
						// move buffer right
						return _writeRebuffer((int) (pos - bpos_blen), len, pos_len, this.bufPos = bpos_blen, this.bufLen = 0, s);
					}
					// moving right insufficient
				}
				// else: op starts inside buffer
				// move buffer to pos
				return _writeRebuffer(0, len, pos_len, this.bufPos = pos, this.bufLen = 0, s);
			}
			// op starts and stops inside buffer capacity
			// extend buffer
			return _writeRebuffer((int) (pos - bpos), len, pos_len, bpos_blen, blen, s);
		}
		// op starts and stop inside buffer
		this.bufMod = true;
		return (int) (pos - bpos);
	}

	@Override
	void _write(long pos, long pos_len, byte[] buf, int off, int len) throws IOException {
		long s = this.size;
		long bpos = this.bufPos;
		int blen = this.bufLen;
		if (pos < bpos) {
			// op starts before buffer
			if (pos_len <= bpos) {
				// op stops before buffer
				if (len >= this.bufferSize) {
					// don't bother buffering
					// no need to flush
					this.source.write(pos, buf, off, len);
					if (pos_len > s) {
						// can't really happen, but better safe than sorry
						this.size = pos_len;
					}
					return;
				}
				_flush(bpos, blen);
				// try moving buffer left
				long start = bpos - Math.min(bpos, this.bufferSize);
				if (pos >= start) {
					// move buffer left
					int f = _writeRebuffer((int) (pos - start), len, pos_len, this.bufPos = start, this.bufLen = 0, s);
					System.arraycopy(buf, off, this.buffer, f, len);
					return;
				}
				// moving left insufficient
				// move buffer as far left as possible (anchored to op)
				long npos = pos_len - Math.min(pos_len, this.bufferSize);
				int f = _writeRebuffer((int) (pos - npos), len, pos_len, this.bufPos = npos, this.bufLen = 0, s);
				System.arraycopy(buf, off, this.buffer, f, len);
				return;
			}
			// op stops inside or after buffer
			long bpos_blen = bpos + blen;
			if (pos_len <= bpos_blen) {
				// op stops inside buffer
				// complete tail-end of op
				int x = (int) (pos_len - bpos);
				int r = len - x;
				System.arraycopy(buf, off + r, this.buffer, 0, x);
				this.bufMod = true;
				// complete remainder of op
				if (r >= this.bufferSize) {
					// don't bother buffering
					// no need to flush
					this.source.write(pos, buf, off, r);
					if (pos_len > s) {
						this.size = pos_len;
					}
					return;
				}
				// flush buffer
				this.source.write(bpos, this.buffer, 0, blen);
				// move buffer left
				long npos = bpos - Math.min(bpos, this.bufferSize);
				int f = _writeRebuffer((int) (pos - npos), r, pos_len, this.bufPos = npos, this.bufLen = 0, s);
				System.arraycopy(buf, off, this.buffer, f, r);
				return;
			}
			// op stops after buffer
			if (len >= this.bufferSize) {
				// discard buffer entirely
				this.bufLen = 0;
				this.bufMod = false;
				// don't bother buffering
				this.source.write(pos, buf, off, len);
				if (pos_len > s) {
					this.size = pos_len;
				}
				return;
			}
			_flush(bpos, blen);
			// move buffer left as as possible (anchored to op)
			long npos = pos_len - Math.min(pos_len, this.bufferSize);
			int f = _writeRebuffer((int) (pos - npos), len, pos_len, this.bufPos = npos, this.bufLen = 0, s);
			System.arraycopy(buf, off, this.buffer, f, len);
			return;
		}
		// op starts inside or after buffer
		long bpos_blen = bpos + blen;
		if (pos_len > bpos_blen) {
			// op stops after buffer
			long bcap = bpos + Math.min(s - bpos, this.bufferSize);
			if (pos_len > bcap) {
				// op stops after buffer capacity
				if (pos >= bpos_blen) {
					// op starts after buffer
					if (len >= this.bufferSize) {
						// don't bother buffering
						// no need to flush
						this.source.write(pos, buf, off, len);
						if (pos_len > s) {
							this.size = pos_len;
						}
						return;
					}
					_flush(bpos, blen);	// requires full rebuffer
					// try moving buffer right
					long end = bpos_blen + Math.min(Long.MAX_VALUE - bpos_blen, this.bufferSize);
					if (pos_len <= end) {
						// move buffer right
						int f = _writeRebuffer((int) (pos - bpos_blen), len, pos_len, this.bufPos = bpos_blen, this.bufLen = 0, s);
						System.arraycopy(buf, off, this.buffer, f, len);
						return;
					}
					// moving right insufficient
					// move buffer to pos
					_writeRebuffer(0, len, pos_len, this.bufPos = pos, this.bufLen = 0, s);
					System.arraycopy(buf, off, this.buffer, 0, len);
					return;
				}
				// op starts inside buffer
				// complete head-end of op
				int x = (int) (pos - bpos);
				int y = blen - x;
				int r = len - y;
				System.arraycopy(buf, off, this.buffer, x, y);
				this.bufMod = true;
				// complete remainder of op
				if (r >= this.bufferSize) {
					// don't bother buffering
					// no need to flush
					this.source.write(bpos_blen, buf, off + y, r);
					if (pos_len > s) {
						this.size = pos_len;
					}
					return;
				}
				// flush buffefr
				this.source.write(bpos, this.buffer, 0, blen);
				// move buffer right
				_writeRebuffer(0, len, pos_len, this.bufPos = bpos_blen, this.bufLen = 0, s);
				System.arraycopy(buf, off + y, this.buffer, 0, r);
				return;
			}
			// ops starts and stops inside buffer capacity
			// extend buffer
			int f = _writeRebuffer((int) (pos - bpos), len, pos_len, bpos_blen, blen, s);
			System.arraycopy(buf, off, this.buffer, f, len);
			return;
		}
		// op starts and stops inside buffer
		this.bufMod = true;
		System.arraycopy(buf, off, this.buffer, (int) (pos - bpos), len);
	}

	// assumtpion: at least one byte can be written
	// prepares the array for a write request of unknown length.
	// the buffer is flushed as necessary.
	// if the request falls within the buffer capacity, 
	// but outside the currently buffered range, the array will be filled
	// and any gap between the buffer length and the returned offset is
	// filled with 0s.
	// the calling method must update bufLen and size by itself.
	int _writeSeek(long pos, long s) throws IOException {
		long bpos = this.bufPos;
		int blen = this.bufLen;
		if (pos < bpos) {
			// op starts before buffer
			_flush(bpos, blen);
			// start a new empty buffer
			this.bufPos = pos;
			this.bufLen = 0;
			return 0;
		}
		// op starts inside or after buffer
		long bpos_blen = bpos + blen;
		if (pos >= bpos_blen) {
			// op stops after buffer
			long bcap = bpos + Math.min(s - bpos, this.bufferSize);
			if (pos >= bcap) {
				// op stops after buffer capacity
				_flush(bpos, blen);
				// start a new empty buffer
				this.bufPos = pos;
				this.bufLen = 0;
				return 0;
			}
			// op starts and stops inside buffer capacity
			// fill and/or zero out existing buffer for pos
			int off = (int) (pos - bpos);	// always at least >= blen
			if (bpos_blen < s) {
				// read in more bytes (if available)
				int k = (int) Math.min(s - bpos_blen, this.bufferSize - blen);
				blen += _mustRead(bpos_blen, this.buffer, blen, k);
			}
			// fill remaining gap, if any, with 0s
			FastZeros.INSTANCE.fillWithZeros(this.buffer, blen, off - blen);
			return off;
		}
		// op starts and stops inside buffer
		return (int) (pos - bpos);
	}

	@Override
	byte _readByte(long pos, long pos_len, long s) throws IOException {
		return this.buffer[_readFixed(pos, pos_len, s)];
	}
	@Override
	char _readChar(long pos, long pos_len, long s, Endian endian) throws IOException {
		return endian.doGetChar(this.buffer, _readFixed(pos, pos_len, s));
	}
	@Override
	short _readShort(long pos, long pos_len, long s, Endian endian) throws IOException {
		return endian.doGetShort(this.buffer, _readFixed(pos, pos_len, s));
	}
	@Override
	int _readInt(long pos, long pos_len, long s, Endian endian) throws IOException {
		return endian.doGetInt(this.buffer, _readFixed(pos, pos_len, s));
	}
	@Override
	float _readFloat(long pos, long pos_len, long s, Endian endian) throws IOException {
		return endian.doGetFloat(this.buffer, _readFixed(pos, pos_len, s));
	}
	@Override
	long _readLong(long pos, long pos_len, long s, Endian endian) throws IOException {
		return endian.doGetLong(this.buffer, _readFixed(pos, pos_len, s));
	}
	@Override
	double _readDouble(long pos, long pos_len, long s, Endian endian) throws IOException {
		return endian.doGetDouble(this.buffer, _readFixed(pos, pos_len, s));
	}
	@Override
	void _writeByte(byte n, long pos, long pos_len) throws IOException {
		this.buffer[_writeFixed(pos, 1, pos_len)] = n;
	}
	@Override
	void _writeChar(char n, long pos, long pos_len, Endian endian) throws IOException {
		endian.doPutChar(n, this.buffer, _writeFixed(pos, 2, pos_len));
	}
	@Override
	void _writeShort(short n, long pos, long pos_len, Endian endian) throws IOException {
		endian.doPutShort(n, this.buffer, _writeFixed(pos, 2, pos_len));
	}
	@Override
	void _writeInt(int n, long pos, long pos_len, Endian endian) throws IOException {
		endian.doPutInt(n, this.buffer, _writeFixed(pos, 4, pos_len));
	}
	@Override
	void _writeFloat(float n, long pos, long pos_len, Endian endian) throws IOException {
		endian.doPutFloat(n, this.buffer, _writeFixed(pos, 4, pos_len));
	}
	@Override
	void _writeLong(long n, long pos, long pos_len, Endian endian) throws IOException {
		endian.doPutLong(n, this.buffer, _writeFixed(pos, 8, pos_len));
	}
	@Override
	void _writeDouble(double n, long pos, long pos_len, Endian endian) throws IOException {
		endian.doPutDouble(n, this.buffer, _writeFixed(pos, 8, pos_len));
	}

	@Override
	int _decode(Decoder dec, Appendable dest, int maxChars, int maxCodePoints, long pos, long end, long s) throws IOException {
		// resolve pending errors or output
		int k = dec.doDecode(dest, maxChars, maxCodePoints);
		if (k != 0) {
			return k;
		}
		// no error or pending output produced
		if (pos >= end) {
			// cannot provide any input
			return 0;
		}
		try {
			int f = _readSeek(pos, s);								// offset into buffer
			int m = (int) Math.min(end - this.bufPos, this.bufLen);	// length of buffer for input range
			dec.doSetInput(this.buffer, f, m - f);
			long b = dec.bytesConsumed();
			int x = dec.doDecode(dest, maxChars, maxCodePoints);
			pos += (dec.bytesConsumed() - b);
			return x;
		} finally {
			this.pos = pos;
			dec.dropInput();
		}
	}

	@Override
	int _decode(Decoder dec, Appendable dest, int maxChars, int maxCodePoints, long pos, long end, long s, IntPredicate stop) throws IOException {
		// resolve pending errors or output
		int k = dec.doDecode(dest, maxChars, maxCodePoints, stop);
		if (k != 0) {
			return k;
		}
		// no error or pending output produced
		if (pos >= end) {
			// cannot provide any input
			return 0;
		}
		try {
			int f = _readSeek(pos, s);								// offset into buffer
			int m = (int) Math.min(end - this.bufPos, this.bufLen);	// length of buffer for input range
			dec.doSetInput(this.buffer, f, m - f);
			long b = dec.bytesConsumed();
			int x = dec.doDecode(dest, maxChars, maxCodePoints, stop);
			pos += (dec.bytesConsumed() - b);
			return x;
		} finally {
			this.pos = pos;
			dec.dropInput();
		}
	}

	@Override
	int _decode(Decoder dec, char[] dest, int off, int maxChars, int maxCodePoints, long pos, long end, long s) throws IOException {
		// resolve pending errors or output
		int k = dec.doDecode(dest, off, maxChars, maxCodePoints);
		if (k != 0) {
			return k;
		}
		// no error or pending output produced
		if (pos >= end) {
			// cannot provide any input
			return 0;
		}
		try {
			int f = _readSeek(pos, s);								// offset into buffer
			int m = (int) Math.min(end - this.bufPos, this.bufLen);	// length of buffer for input range
			dec.doSetInput(this.buffer, f, m - f);
			long b = dec.bytesConsumed();
			int x = dec.doDecode(dest, off, maxChars, maxCodePoints);
			pos += (dec.bytesConsumed() - b);
			return x;
		} finally {
			this.pos = pos;
			dec.dropInput();
		}
	}

	@Override
	int _decode(Decoder dec, char[] dest, int off, int maxChars, int maxCodePoints, long pos, long end, long s, IntPredicate stop) throws IOException {
		// resolve pending errors or output
		int k = dec.doDecode(dest, off, maxChars, maxCodePoints, stop);
		if (k != 0) {
			return k;
		}
		// no error or pending output produced
		if (pos >= end) {
			// cannot provide any input
			return 0;
		}
		try {
			int f = _readSeek(pos, s);								// offset into buffer
			int m = (int) Math.min(end - this.bufPos, this.bufLen);	// length of buffer for input range
			dec.doSetInput(this.buffer, f, m - f);
			long b = dec.bytesConsumed();
			int x = dec.doDecode(dest, off, maxChars, maxCodePoints, stop);
			pos += (dec.bytesConsumed() - b);
			return x;
		} finally {
			this.pos = pos;
			dec.dropInput();
		}
	}

	@Override
	int _encode(Encoder enc, int inputChars, long pos, long end, int maxCodePoints) throws IOException {
		long s = this.size;
		int f = _writeSeek(pos, s);	// prep array for an arbitrary-length write
		int m = (int) Math.min(end - this.bufPos, this.bufferSize);
		int x = enc.doEncode(inputChars, this.buffer, f, m - f, maxCodePoints);
		if (x > 0) {
			long posx = pos + x;
			int fx = f + x;
			this.bufMod = true;
			this.pos = posx;
			if (fx > this.bufLen) {
				this.bufLen = fx;
			}
			if (posx > s) {
				this.size = posx;
			}
		}
		return x;
	}

	// I/O throughput of ~100 MB/s. that's pretty fast, actually. 
	@Override
	String _nextLine(Decoder dec, String replace) throws IOException {
		long p = this.pos;
		long s = this.size;
		if (p >= s) {
			return null;
		}
		this.sb.setLength(0);
		try {
			while (true) {
				int f = _readSeek(p, s);
				int m = (int) Math.min(this.bufLen, s - p);	// >TODO is that right?
				for (int i = f; i < m; i++) {
					switch (this.buffer[i]) {
						case '\r': {
							_decodeNextLine(dec, f, i++ - f, replace);
							// skip over full separator
							p += (i - f);
							if (p != s) {
								// more bytes are available
								if (i == m) {
									// end of buffer reached, but need to see next byte
									i = _readSeek(p, s);
								}
								if (this.buffer[i] == '\n') {
									p++;
								}
							} // else: file ends with an '\r'
							return this.sb.toString();
						}
						case '\n': {
							_decodeNextLine(dec, f, i++ - f, replace);
							p += (i - f);
							return this.sb.toString();
						}
					}
				}
				// buffer range contains no separators
				int n = m - f;
				_decodeNextLine(dec, f, n, replace);
				if ((p += n) == s) {
					// full input range processed
					if (dec.pendingInput() != 0) {
						// incomplete code point at end of input sequence
						this.sb.append('\uFFFD');
					}
					// return last line
					return this.sb.toString();
				}
				// continue 
			}
		} finally {
			this.pos = p;
			dec.dropInput();
		}
	}

	private void _decodeNextLine(Decoder dec, int off, int len, String replace) throws IOException {
		// decoder will resolve pending input on its own
		// cannot have pending output because Appendable is boundless
		dec.doSetInput(this.buffer, off, len);
		while (true) {
			int n = dec.doDecode(this.sb);
			if (n == 0) {
				// DONE.
				break;
			}
			if (n < 0) {
				// error. replace and continue
				this.sb.append(replace);
			}
		}
	}

	@Override
	int _inflate(byte[] dest, int off, int len, Inflater inf, long pos, long end, long s) throws IOException, DataFormatException {
		if (len == 0) {
			return 0;	// no bytes to be produced
		}
		try {
			int x = 0;
			do {
				int n;
				while ((n = inf.inflate(dest, off + x, len - x)) == 0) {
					if (inf.needsInput()) {
						if (pos >= end) {
							return x;	// out of input.
						}
						// feed inflater
						int f = _readSeek(pos, s);
						int b = ((int) Math.min(end - this.bufPos, this.bufLen)) - f;
						inf.setInput(this.buffer, f, b);
						pos += b;
						continue;
					}
					if (inf.finished() || inf.needsDictionary()) {
						// let calling class handle special cases
						return x;
					}
				}
				x += n;
			} while (x != len);
			return len;
		} catch (java.util.zip.DataFormatException ex) {
			throw new DataFormatException(ex);
		} finally {
			this.pos = pos - inf.getRemaining();	// adjust for discarded input
			inf.setInput(EMPTY);					// discard remaining input
		}
	}

	// with default compression level and a large enough buffer size (64 kb), 
	// this achieves around 40 MB/s file-to-file through-put. Which is not... terrible.
	// it's half the speed of 7Zip. it's useable. and certainly faster than any other
	// way Java could offer out of the box.
	@Override
	long _inflate(IOBuffer dest, long numBytesOut, Inflater inf, long pos, long end, long s) throws IOException, DataFormatException {
		if (dest.getClass() == GeneralIOBuffer.class) {
			// We have direct access to the other buffer, 
			// no additional allocation or copying is required.
			// Instead we directly inflate to its array from our array.
			// this is basically just inflateTo, except it reads in more input from
			// this buffer. plus, it's long-based.
			GeneralIOBuffer out = (GeneralIOBuffer) dest;
			long out_pos = out.pos;
			long len = Math.min(numBytesOut, Long.MAX_VALUE - out_pos);
			if (len == 0L) {
				// cannot produce output
				return 0L;
			}
			long out_s = out.size;
			try {
				long totalOld = inf.getBytesWritten();
				long totalNew = totalOld;
				long x = 0L;
				do {
					int out_off = out._writeSeek(out_pos, out_s);
					int m = (int) Math.min(len - x, out.bufferSize - out_off);
					int n = 0;
					try {
						do {
							int y;
							while ((y = inf.inflate(out.buffer, out_off + n, m - n)) == 0) {
								if (inf.needsInput()) {
									if (pos >= end) {
										// out of input
										return x + n;
									}
									int f = _readSeek(pos, s);
									int b = ((int) Math.min(end - this.bufPos, this.bufLen)) - f;
									inf.setInput(this.buffer, f, b);
									pos += b;
									continue;
								}
								if (inf.finished() || inf.needsDictionary()) {
									return x + n;
								}
							}
							n += y;
						} while (n != m);
					} catch (java.util.zip.DataFormatException ex) {
						n = (int) (inf.getBytesWritten() - totalOld);
						throw new DataFormatException(ex);
					} catch (IOException ex) {
						n = (int) (inf.getBytesWritten() - totalOld);
						throw ex;
					} catch (Throwable ex) {
						// something else went wrong
						n = (int) (inf.getBytesWritten() - totalOld);
						throw ex;
					} finally {
						if (n != 0) {
							out.bufMod = true;
							out_off += n;
							if (out_off > out.bufLen) {
								out.bufLen = out_off;
							}
							out_pos += n;
							if (out_pos > out_s) {
								out_s = out_pos;
							}
						}
					}
					totalOld = totalNew;
					totalNew += n;
					x += n;
				} while (x != len);
				return len;
			} finally {
				out.pos = out_pos;
				out.size = out_s;
				this.pos = pos - inf.getRemaining();	// adjust for discarded input
				inf.setInput(EMPTY);					// discard remaining input
			}
		}
		// general solution, requires an extra array to inflate to, which is 
		// then written to the destination buffer.
		// at least the input array can be used directly, so it's still superior
		// to a generic solution.
		long len = Math.min(numBytesOut, Long.MAX_VALUE - dest.pos());
		if (len == 0L) {
			// cannot produce output
			return 0L;
		}
		// TODO if buffer size at least 64k, then zlib will become much faster
		// by primarily using the "inflate_fast" sub-routine.
		// make sure to do that for all buffers likely to require inflation.
		// request a temp buffer.
		byte[] buf = BufferCache.requestBuffer(1 << 16);
		try {
			long totalOld = inf.getBytesWritten();
			long totalNew = totalOld;
			long x = 0L;
			do {
				int m = (int) Math.min(len - x, BufferCache.DEFAULT_SIZE);
				int n = 0;
				try {
					do {
						int y;
						while ((y = inf.inflate(buf, n, m - n)) == 0) {
							if (inf.needsInput()) {
								if (pos >= end) {
									// out of input
									return x + n;
								}
								int f = _readSeek(pos, s);
								int b = ((int) Math.min(end - this.bufPos, this.bufLen)) - f;
								inf.setInput(this.buffer, f, b);
								pos += b;
								continue;
							}
							if (inf.finished() || inf.needsDictionary()) {
								return x + n;
							}
						}
						n += y;
					} while (n != m);
				} catch (java.util.zip.DataFormatException ex) {
					n = (int) (inf.getBytesWritten() - totalOld);
					throw new DataFormatException(ex);
				} catch (IOException ex) {
					n = (int) (inf.getBytesWritten() - totalOld);
					throw ex;
				} catch (Throwable ex) {
					n = (int) (inf.getBytesWritten() - totalOld);
					throw ex;
				} finally {
					// must write even when errors occur, 
					// in order for bytes consumed and procuded
					// to stay synced. And because both impls should behave the 
					// same way.
					dest.doWrite(buf, 0, n);
				}
				totalOld = totalNew;
				totalNew += m;
				x += m;
			} while (x != len);
			return len;
		} finally {
			try {
				BufferCache.releaseBuffer(buf);
			} finally {
				this.pos = pos - inf.getRemaining();	// adjust for discarded input
				inf.setInput(EMPTY);					// discard remaining input
			}
		}
	}

	@Override
	int _inflateTo(Inflater inf, int len, long pos) throws IOException {
		if (len == 0) {
			return 0;
		}
		long s = this.size;
		try {
			long totalOld = inf.getBytesWritten();
			long totalNew = totalOld;
			int x = 0;
			do {
				int f = _writeSeek(pos, s);
				int m = Math.min(len - x, this.bufferSize - f);
				int n = 0;
				try {
					do {
						int y;
						while ((y = inf.inflate(this.buffer, f + n, m - n)) == 0) {
							if (inf.needsInput() || inf.finished() || inf.needsDictionary()) {
								return x + n;
							}
						}
						n += y;
					} while (n != m);
				} catch (java.util.zip.DataFormatException ex) {
					// get number of bytes written from inflater itself
					// so that inflater and buffer statistics stay synced.
					n = (int) (inf.getBytesWritten() - totalOld);
					throw new DataFormatException(ex);
				} catch (Throwable ex) {
					n = (int) (inf.getBytesWritten() - totalOld);
					throw ex;
				} finally {
					if (n != 0) {
						// update buffer state in all cases.
						this.bufMod = true;
						f += n;
						if (f > this.bufLen) {
							this.bufLen = f;
						}
						pos += n;
						if (pos > s) {
							s = pos;
						}
					}
				}
				totalOld = totalNew;
				totalNew += n;
				x += n;
			} while (x != len);
			return len;
		} finally {
			this.pos = pos;
			this.size = s;
		}
	}

	// If a only a small numbers of bytes are tranferred at a time, the 
	// overhead of I/O calls becomes a performance concern. 
	// Instead, use buffering to speed up repeated small transfers.
	// TODO needs testing to find a good value.
	private static final long FC_TRANSFER_THRESHOLD = 64L;

	// TODO go over this again, please.
	@Override
	long _transfer(long numBytes, IOBuffer dest, long pos, long s) throws IOException {
		if (dest.getClass() == GeneralIOBuffer.class) {
			GeneralIOBuffer out = (GeneralIOBuffer) dest;
			out._ensureOpen();	// no better place to put this line without seriously repeating myself
			long out_pos = out.pos;
			long n = Math.min(numBytes, Long.MAX_VALUE - out_pos);
			if (n == 0L) {
				return 0L;
			}
			long end = pos + n;
			if (n > FC_TRANSFER_THRESHOLD
					&& this.source.getClass() == FileIOAddress.FileIOSource.class
					&& out.source.getClass() == FileIOAddress.FileIOSource.class) {
				// can make use of FileChannel's native transfer method
				if (this.bufMod) {
					long bpos = this.bufPos;
					int blen = this.bufLen;
					if (pos < bpos + blen && end > bpos) {
						// tranfer range in buffer range. 
						// ensure modified bytes are transferred.
						_forceFlush(bpos, blen);
						// keep buffer intact
					}
				}
				if (out.bufMod) {
					long out_bpos = out.bufPos;
					int out_blen = out.bufLen;
					if (pos < out_bpos + out_blen && end > out_bpos) {
						// tranfer range in buffer range. 
						// ensure no modified bytes are swallowed.
						out._forceFlush(out_bpos, out_blen);
						// dispose of buffer, because parts are overwritten
						out.bufLen = 0;
					}
				}
				long k = 0L;
				try {
					FileChannel fcIn = ((FileIOAddress.FileIOSource) this.source).fc;
					FileChannel fcOut = ((FileIOAddress.FileIOSource) out.source).fc;
					fcOut.position(out_pos);
					do {
						k += fcIn.transferTo(pos + k, n - k, fcOut);
					} while (k != n);
					return n;
				} catch (java.io.IOException ex) {
					throw new IOException(ex);
				} finally {
					this.pos = pos + k;
					long out_pos_len = out_pos + k;
					out.pos = out_pos_len;
					if (out_pos_len > out.size) {
						out.size = out_pos_len;
					}
				}
			}
			// cannot use native transfer
			try {
				do {
					int f = _readSeek(pos, s);
					int b = ((int) Math.min(end - this.bufPos, this.bufLen)) - f;
					long out_pos_len = out_pos + b;
					out._write(out_pos, out_pos_len, this.buffer, f, b);
					out_pos = out_pos_len;
					pos += b;
				} while (pos != end);
				return n;
			} finally {
				this.pos = pos;
				out.pos = out_pos;
			}
		}
		// general solution
		long n = Math.min(numBytes, Long.MAX_VALUE - dest.pos());
		if (n == 0L) {
			return 0L;
		}
		try {
			long end = pos + n;
			do {
				int f = _readSeek(pos, s);
				int b = ((int) Math.min(end - this.bufPos, this.bufLen)) - f;
				dest.write(this.buffer, f, b);
				pos += b;
			} while (pos != end);
			return n;
		} finally {
			this.pos = pos;
		}
	}
}

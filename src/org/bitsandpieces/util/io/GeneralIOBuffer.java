/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.io;

import java.nio.channels.FileChannel;
import java.util.ConcurrentModificationException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.Checksum;
import java.util.zip.Inflater;
import org.bitsandpieces.util.Endian;
import org.bitsandpieces.util.encoding.Decoder;
import org.bitsandpieces.util.encoding.Encoder;

/**
 * Suitable for any kind of underlying source, excellent sequential access in
 * both directions.
 *
 * @author Jan Kebernik
 */
final class GeneralIOBuffer extends AbstractIOBuffer {

	// required to be at least 8 for readFixed and writeFixed to work 
	// correctly for all primitive types
	private static final int MIN_BUFFER_SIZE = 8;
	private static final byte[] EMPTY = {};
	private static final ObjectCache<StringBuilder> CB_CACHE = new ObjectCache<>(() -> new StringBuilder());

	private long bufPos;
	private int bufLen;
	private boolean bufMod;

	/*
	 * the reason we don't use buffer.length is that the cost of re-buffering
	 * directly scales with this value. buffer.length might be significantly
	 * larger than bufferSize, which, under certain circumstances, could lead to
	 * more bytes being buffered than is desired. It's a simple and cheap way to
	 * weight costs and as such certainly worth the minor redundancy.
	 */
	private final int bufferSize;
	private final byte[] buffer;
	private final StringBuilder cb;

	private final AtomicBoolean closed;

	GeneralIOBuffer(IOSource source) throws IOException {
		super(source);
		this.closed = new AtomicBoolean();
		this.bufferSize = BufferCache.DEFAULT_SIZE;
		this.buffer = BufferCache.requestBuffer();
		this.cb = CB_CACHE.requestInstance();
	}

	GeneralIOBuffer(IOSource source, int bufferSize) throws IOException {
		super(source);
		if (bufferSize < MIN_BUFFER_SIZE) {
			bufferSize = MIN_BUFFER_SIZE;
		}
		this.closed = new AtomicBoolean();
		this.bufferSize = bufferSize;
		this.buffer = BufferCache.requestBuffer(bufferSize);
		this.cb = CB_CACHE.requestInstance();
	}

	GeneralIOBuffer(IOSource source, AtomicInteger shared) throws IOException {
		super(source, shared);
		this.closed = new AtomicBoolean();
		this.bufferSize = BufferCache.DEFAULT_SIZE;
		this.buffer = BufferCache.requestBuffer();
		this.cb = CB_CACHE.requestInstance();
	}

	GeneralIOBuffer(IOSource source, AtomicInteger shared, int bufferSize) throws IOException {
		super(source, shared);
		if (bufferSize < MIN_BUFFER_SIZE) {
			bufferSize = MIN_BUFFER_SIZE;
		}
		this.closed = new AtomicBoolean();
		this.bufferSize = bufferSize;
		this.buffer = BufferCache.requestBuffer(bufferSize);
		this.cb = CB_CACHE.requestInstance();
	}

	private void checkSharedArray(byte[] buf) {
		if (buf == this.buffer) {
			// TODO use assert instead.
			throw new IllegalStateException("Leaked internal array detected.");
		}
	}

	@Override
	void ensureOpen() throws IOException {
		if (this.closed.get()) {
			throw new IOException("Buffer closed");
		}
	}

	@Override
	public void close() throws IOException {
		// atomically close, no effect if already closed (idempotent).
		if (this.closed.compareAndSet(false, true)) {
			try {
				flush();
			} finally {
				try {
					CB_CACHE.releaseInstance(this.cb);
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

	private void flush(long bpos, int blen) throws IOException {
		if (this.bufMod) {
			doFlush(bpos, blen);
		}
	}

	private void flush() throws IOException {
		if (this.bufMod) {
			doFlush(this.bufPos, this.bufLen);
		}
	}

	private void doFlush(long bpos, int blen) throws IOException {
		this.source.write(bpos, this.buffer, 0, blen);
		this.bufMod = false;
	}

	private int mustRead(long pos, byte[] buf, int off, int len) throws IOException {
		for (int n = 0; n != len;) {
			int r = this.source.read(pos + n, buf, off + n, len - n);
			if (r < 0) {
				throw new IOException("Unexpected EOF");
			}
			n += r;
		}
		return len;
	}

	private int readRebuffer(long pos, long npos, long s) throws IOException {
		this.bufLen = 0;
		int k = (int) Math.min(s - npos, this.bufferSize);
		mustRead(npos, this.buffer, 0, k);
		this.bufPos = npos;
		this.bufLen = k;
		return (int) (pos - npos);
	}

	// TODO verify correctness
	// off = offset into new buffer
	// len = extra length to be written to buffer
	// npos = new buffer position
	// blen = current buffer length (snapshot)
	// s = current size
	// if this is bad, fall back on commented-out old way. and re-think. else, great!
	private int writeRebuffer(int off, int len, long npos, int blen, long s) throws IOException {
		if (off > blen) {
			// only read in existing bytes if offset into buffer greater than buffer length (gap exists)
			if (npos < s) {
				int k = (int) Math.min(s - npos, this.bufferSize - blen);
				blen += mustRead(npos, this.buffer, blen, k);
			}
			// fill remaining gap (if any) with 0s
			for (; blen < off; blen++) {
				this.buffer[blen] = 0;
			}
		}
		// else just return offset into buffer
		// either way, update buffer length for incoming copy-write, if necessary
		this.bufLen = Math.max(blen, off + len);
		return off;
	}

//	// off = offset into new buffer
//	// len = extra length to be written to buffer
//	// npos = new buffer position
//	// blen = current buffer length (snapshot)
//	// s = current size
//	private int writeRebuffer(int off, int len, long npos, int blen, long s) throws IOException {
//		if (npos < s) {
//			int k = (int) Math.min(s - npos, this.bufferSize - blen);
//			blen += mustRead(npos, this.buffer, blen, k);
//		}
//		for (; blen < off; blen++) {
//			this.buffer[blen] = 0;
//		}
//		this.bufLen = Math.max(blen, off + len);
//		return off;
//	}
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

	@Override
	void _read(long pos, long pos_len, long s, byte[] buf, int off, int len) throws IOException {
		checkSharedArray(buf);
		long bpos = this.bufPos;
		int blen = this.bufLen;
		long bpos_blen = bpos + blen;
		if (pos < bpos) {
			// start before
			flush(bpos, blen);
			if (pos_len <= bpos) {
				// stop before
				if (len >= this.bufferSize) {
					mustRead(pos, buf, off, len);
					return;
				}
				long npos = Math.min(pos, bpos - Math.min(bpos, this.bufferSize));
				int f = readRebuffer(pos, npos, s);
				System.arraycopy(this.buffer, f, buf, off, len);
				return;
			}
			if (pos_len <= bpos_blen) {
				// stop inside
				int x = (int) (pos_len - bpos);
				int r = len - x;
				System.arraycopy(this.buffer, 0, buf, off + r, x);
				if (r >= this.bufferSize) {
					mustRead(pos, buf, off, r);
					return;
				}
				long npos = bpos - Math.min(bpos, this.bufferSize);
				int f = readRebuffer(pos, npos, s);
				System.arraycopy(this.buffer, f, buf, off, r);
				return;
			}
			// stop after
			if (len >= this.bufferSize) {
				mustRead(pos, buf, off, len);
				return;
			}
			readRebuffer(pos, pos, s);
			System.arraycopy(this.buffer, 0, buf, off, len);
			return;
		}
		if (pos_len > bpos_blen) {
			// stop after
			flush(bpos, blen);
			if (pos >= bpos_blen) {
				// start after
				if (len >= this.bufferSize) {
					mustRead(pos, buf, off, len);
					return;
				}
				long end = bpos_blen + this.bufferSize;
				if (end < 0L || end >= pos_len) {
					int f = readRebuffer(pos, bpos_blen, s);
					System.arraycopy(this.buffer, f, buf, off, len);
					return;
				}
				long npos = pos_len - this.bufferSize;
				int f = readRebuffer(pos, npos, s);
				System.arraycopy(this.buffer, f, buf, off, len);
				return;
			}
			// start inside
			int x = (int) (pos - bpos);
			int y = blen - x;
			int r = len - y;
			System.arraycopy(this.buffer, x, buf, off, y);
			if (r >= this.bufferSize) {
				mustRead(bpos_blen, buf, off + y, r);
				return;
			}
			readRebuffer(bpos_blen, bpos_blen, s);
			System.arraycopy(this.buffer, 0, buf, off + y, r);
			return;
		}
		int f = (int) (pos - bpos);
		System.arraycopy(this.buffer, f, buf, off, len);
	}

	@Override
	void _write(long pos, long pos_len, byte[] buf, int off, int len) throws IOException {
		checkSharedArray(buf);
		long s = this.size;
		long bpos = this.bufPos;
		int blen = this.bufLen;
		long bpos_blen = bpos + blen;
		if (pos < bpos) {
			// start before
			if (pos_len <= bpos) {
				// stop before
				if (len >= this.bufferSize) {
					this.source.write(pos, buf, off, len);
					if (pos_len > s) {
						this.size = pos_len;
					}
					return;
				}
				flush(bpos, blen);
				long npos = Math.min(pos, bpos - Math.min(bpos, this.bufferSize));
				int f = writeRebuffer((int) (pos - npos), len, this.bufPos = npos, this.bufLen = 0, s);
				System.arraycopy(buf, off, this.buffer, f, len);
				this.bufMod = true;
				if (pos_len > s) {
					this.size = pos_len;
				}
				return;
			}
			if (pos_len <= bpos_blen) {
				// stop inside
				int x = (int) (pos_len - bpos);
				int r = len - x;
				System.arraycopy(buf, off + r, this.buffer, 0, x);
				this.bufMod = true;
				if (r >= this.bufferSize) {
					this.source.write(pos, buf, off, r);
					if (pos_len > s) {
						this.size = pos_len;
					}
					return;
				}
				this.source.write(bpos, this.buffer, 0, blen);	// flush
				long npos = bpos - Math.min(bpos, this.bufferSize);
				int f = writeRebuffer((int) (pos - npos), r, this.bufPos = npos, this.bufLen = 0, s);
				System.arraycopy(buf, off, this.buffer, f, r);
				if (pos_len > s) {
					this.size = pos_len;
				}
				return;
			}
			// stop after
			if (len >= this.bufferSize) {
				// write greater buffer cap. invalidate buffer alltogether.
				this.bufLen = 0;
				this.bufMod = false;
				this.source.write(pos, buf, off, len);
				if (pos_len > s) {
					this.size = pos_len;
				}
				return;
			}
			flush(bpos, blen);
			writeRebuffer(0, len, this.bufPos = pos, this.bufLen = 0, s);
			System.arraycopy(buf, off, this.buffer, 0, len);
			this.bufMod = true;
			if (pos_len > s) {
				this.size = pos_len;
			}
			return;
		}
		if (pos_len > bpos_blen) {
			// stop after
			if (blen != 0 && pos_len <= bpos + Math.min(Long.MAX_VALUE - bpos, this.bufferSize)) {
				// extend buffer
				int f = writeRebuffer((int) (pos - bpos), len, bpos_blen, blen, s);
				System.arraycopy(buf, off, this.buffer, f, len);
				this.bufMod = true;
				if (pos_len > s) {
					this.size = pos_len;
				}
				return;
			}
			if (pos >= bpos_blen) {
				// start after
				if (len >= this.bufferSize) {
					this.source.write(pos, buf, off, len);
					if (pos_len > s) {
						this.size = pos_len;
					}
					return;
				}
				flush(bpos, blen);
				long end = bpos_blen + this.bufferSize;
				if (end < 0L || end >= pos_len) {
					int f = writeRebuffer((int) (pos - bpos_blen), len, this.bufPos = bpos_blen, this.bufLen = 0, s);
					System.arraycopy(buf, off, this.buffer, f, len);
					this.bufMod = true;
					if (pos_len > s) {
						this.size = pos_len;
					}
					return;
				}
				long npos = pos_len - this.bufferSize;
				int f = writeRebuffer((int) (pos - npos), len, this.bufPos = npos, this.bufLen = 0, s);
				System.arraycopy(buf, off, this.buffer, f, len);
				this.bufMod = true;
				if (pos_len > s) {
					this.size = pos_len;
				}
				return;
			}
			// start inside
			int x = (int) (pos - bpos);
			int y = blen - x;
			int r = len - y;
			System.arraycopy(buf, off, this.buffer, x, y);
			this.bufMod = true;
			if (r >= this.bufferSize) {
				this.source.write(bpos_blen, buf, off + y, r);
				if (pos_len > s) {
					this.size = pos_len;
				}
				return;
			}
			this.source.write(bpos, this.buffer, 0, blen);	// flush
			writeRebuffer(0, r, this.bufPos = bpos_blen, this.bufLen = 0, s);
			System.arraycopy(buf, off + y, this.buffer, 0, r);
			if (pos_len > s) {
				this.size = pos_len;
			}
			return;
		}
		int f = (int) (pos - bpos);
		System.arraycopy(buf, off, this.buffer, f, len);
		this.bufMod = true;
	}

	@Override
	int _readFixed(long pos, int len, long pos_len, long s) throws IOException {
		long bpos = this.bufPos;
		int blen = this.bufLen;
		if (pos < bpos) {
			// start before buffer
			flush(bpos, blen);
			if (pos_len <= bpos) {
				// stop before buffer
				long npos = Math.min(pos, bpos - Math.min(bpos, this.bufferSize));
				return readRebuffer(pos, npos, s);
			}
			// stop inside buffer
			long npos = pos_len - Math.min(pos_len, this.bufferSize);
			return readRebuffer(pos, npos, s);
		}
		long bpos_blen = bpos + blen;
		if (pos_len > bpos_blen) {
			// stop after buffer
			flush(bpos, blen);
			if (pos >= bpos_blen) {
				// start after buffer
				long end = bpos_blen + this.bufferSize;
				if (end < 0L || end >= pos_len) {
					return readRebuffer(pos, bpos_blen, s);
				}
				long npos = pos_len - this.bufferSize;
				return readRebuffer(pos, npos, s);
			}
			// start inside buffer
			return readRebuffer(pos, pos, s);
		}
		// start and stop inside buffer
		return (int) (pos - bpos);
	}

	@Override
	int _writeFixed(long pos, int len, long pos_len) throws IOException {
		long s = this.size;
		long bpos = this.bufPos;
		int blen = this.bufLen;
		if (pos < bpos) {
			// start before buffer
			flush(bpos, blen);
			if (pos_len <= bpos) {
				// stop before buffer
				long npos = Math.min(pos, bpos - Math.min(bpos, this.bufferSize));
				int n = writeRebuffer((int) (pos - npos), len, this.bufPos = npos, this.bufLen = 0, s);
				if (pos_len > s) {
					this.size = pos_len;
				}
				this.bufMod = true;
				return n;
			}
			// stop inside buffer
			long npos = pos_len - Math.min(pos_len, this.bufferSize);
			int n = writeRebuffer((int) (pos - npos), len, this.bufPos = npos, this.bufLen = 0, s);
			if (pos_len > s) {
				this.size = pos_len;
			}
			this.bufMod = true;
			return n;
		}
		long bpos_blen = bpos + blen;
		if (pos_len > bpos_blen) {
			// stop after buffer
			if (blen != 0 && pos_len <= bpos + Math.min(Long.MAX_VALUE - bpos, this.bufferSize)) {
				// extend buffer
				int n = writeRebuffer((int) (pos - bpos), len, bpos_blen, blen, s);
				if (pos_len > s) {
					this.size = pos_len;
				}
				this.bufMod = true;
				return n;
			}
			flush(bpos, blen);
			if (pos >= bpos_blen) {
				// start after buffer
				long end = bpos_blen + this.bufferSize;
				if (end < 0L || end >= pos_len) {
					int n = writeRebuffer((int) (pos - bpos_blen), len, this.bufPos = bpos_blen, this.bufLen = 0, s);
					if (pos_len > s) {
						this.size = pos_len;
					}
					this.bufMod = true;
					return n;
				}
				long npos = pos_len - this.bufferSize;
				int n = writeRebuffer((int) (pos - npos), len, this.bufPos = npos, this.bufLen = 0, s);
				if (pos_len > s) {
					this.size = pos_len;
				}
				this.bufMod = true;
				return n;
			}
			// start inside buffer
			int n = writeRebuffer(0, len, this.bufPos = pos, this.bufLen = 0, s);
			if (pos_len > s) {
				this.size = pos_len;
			}
			this.bufMod = true;
			return n;
		}
		this.bufMod = true;
		return (int) (pos - bpos);
	}

	@Override
	byte getByte(int index) {
		return this.buffer[index];
	}

	@Override
	char getChar(Endian endian, int index) {
		return endian.getChar(this.buffer, index);
	}

	@Override
	short getShort(Endian endian, int index) {
		return endian.getShort(this.buffer, index);
	}

	@Override
	int getInt(Endian endian, int index) {
		return endian.getInt(this.buffer, index);
	}

	@Override
	long getLong(Endian endian, int index) {
		return endian.getLong(this.buffer, index);
	}

	@Override
	float getFloat(Endian endian, int index) {
		return endian.getFloat(this.buffer, index);
	}

	@Override
	double getDouble(Endian endian, int index) {
		return endian.getDouble(this.buffer, index);
	}

	@Override
	void putByte(byte n, int index) {
		this.buffer[index] = n;
	}

	@Override
	void putChar(Endian endian, char n, int index) {
		endian.putChar(this.buffer, index, n);
	}

	@Override
	void putShort(Endian endian, short n, int index) {
		endian.putShort(this.buffer, index, n);
	}

	@Override
	void putInt(Endian endian, int n, int index) {
		endian.putInt(this.buffer, index, n);
	}

	@Override
	void putLong(Endian endian, long n, int index) {
		endian.putLong(this.buffer, index, n);
	}

	@Override
	void putFloat(Endian endian, float n, int index) {
		endian.putFloat(this.buffer, index, n);
	}

	@Override
	void putDouble(Endian endian, double n, int index) {
		endian.putDouble(this.buffer, index, n);
	}

	private void _decode(Decoder dec, int off, int len, String replace) {
		// decoder will resolve pending input on its own
		// cannot have pending output because Appendable is boundless
		dec.setInput(this.buffer, off, len);
		while (true) {
			int n = dec.decode(this.cb);
			if (n == 0) {
				// DONE.
				break;
			}
			if (n < 0) {
				// error. replace and continue
				this.cb.append(replace);
			}
		}
	}

	// if pos not within current buffer range, rebuffer at pos
	// return offset into buffer
	private int seek(long pos, long s) throws IOException {
		long bpos = this.bufPos;
		int blen = this.bufLen;
		long bpos_blen = bpos + blen;
		if (pos < bpos || pos >= bpos_blen) {
			flush(bpos, blen);
			readRebuffer(pos, pos, s);
			return 0;
		}
		return (int) (pos - bpos);
	}

	// sets up buffer for an arbitrary-length write at pos
	// only flushes if pos outside buffer capacity
	// else extends buffer by reading in existing bytes
	// then padding any potential gap with 0s
	// all this keeps flushing (the expensive part) at a minimum.
	private int seekForWrite(long pos, long s) throws IOException {
		long bpos = this.bufPos;
		long bcap = bpos + this.bufferSize;
		if (bcap < 0L) {
			// unlikely, but necessary
			bcap = Long.MAX_VALUE;
		}
		int blen = this.bufLen;
		if (pos < bpos || pos >= bcap) {
			// pos outside buffer capacity. start an empty buffer range.
			flush(bpos, blen);
			this.bufPos = pos;
			this.bufLen = 0;
			return 0;
		}
		int off = (int) (pos - bpos);
		if (off > blen) {
			// there is a gap
			// fill buffer
			if (bpos < s) {
				int k = (int) Math.min(s - bpos, this.bufferSize - blen);
				blen += mustRead(bpos, this.buffer, blen, k);
			}
			// pad remaining gap (if any) with 0s
			for (; blen < off; blen++) {
				this.buffer[blen] = 0;
			}
			this.bufLen = blen;	// only updated if no exceptions are thrown
		}
		// if there is no gap between offset and length, the buffer remains as is. 
		// seeing as any read bytes are likely to be overwritten shortly, it 
		// would usually be a wasted effort.
		return off;
	}

	@Override
	String _nextLine(Decoder dec, String replace) throws IOException {
		long p = this.pos;
		long s = this.size;
		if (p >= s) {
			return null;
		}
		this.cb.delete(0, this.cb.length());
		try {
			while (true) {
				int f = seek(p, s);
				//int m = (int) Math.min(_s - this.bufPos, this.bufLen);	// seek already adjusts bufLen for size
				int m = this.bufLen;
				for (int i = f; i < m; i++) {
					switch (this.buffer[i]) {
						case '\r': {
							_decode(dec, f, i++ - f, replace);
							// skip over full separator
							p += (i - f);
							if (i < m && this.buffer[i] == '\n') {
								p++;
							}
							return this.cb.toString();
						}
						case '\n': {
							_decode(dec, f, i++ - f, replace);
							p += (i - f);
							return this.cb.toString();
						}
					}
				}
				int n = m - f;
				_decode(dec, f, n, replace);
				if ((p += n) == s) {
					// full input range processed
					if (dec.pendingInput() != 0) {
						// incomplete code point at end of input sequence
						this.cb.append('\uFFFD');
					}
					// return last line
					return this.cb.toString();
				}
				// continue 
			}
		} finally {
			this.pos = p;
			dec.dropInput();
		}
	}

	@Override
	int _decode(Decoder dec, Appendable dest, int maxChars, int maxCodePoints, long pos, long end, long s) throws IOException {
		try {
			// resolve pending errors or output
			int k = dec.decode(dest, maxChars, maxCodePoints);
			if (k != 0) {
				return k;
			}
			// no error or pending output produced
			if (pos >= end) {
				// cannot provide any input
				return 0;
			}
			int f = seek(pos, s);									// offset into buffer
			int m = (int) Math.min(end - this.bufPos, this.bufLen);	// length of buffer for input range
			dec.setInput(this.buffer, f, m - f);
			long b = dec.bytesConsumed();
			int x = dec.decode(dest, maxChars, maxCodePoints);
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
		int f = seekForWrite(pos, s);
		int m = (int) Math.min(end - this.bufPos, this.bufferSize);
		int x = enc.encode(inputChars, this.buffer, f, m - f, maxCodePoints);
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

	@Override
	int _inflate(byte[] dest, int off, int len, Inflater inf, long pos, long end, long s) throws IOException, DataFormatException {
		checkSharedArray(dest);
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
						int f = seek(pos, s);
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

	private static final String ASYNC_MSG = "Inflater was closed asynchronously. Cannot determine number of bytes written. Destination buffer is compromised.";

	@Override
	@SuppressWarnings("UseSpecificCatch")
	long _inflate(IOBuffer dest, long numBytesOut, Inflater inf, long pos, long end, long s) throws IOException, DataFormatException {
		if (dest.getClass() == GeneralIOBuffer.class) {
			// We have direct access to the other buffer, 
			// no additional allocation or copying is required.
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
					int out_off = out.seekForWrite(out_pos, out_s);
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
									int f = seek(pos, s);
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
						try {
							// Determine how many bytes were actually written.
							// Cannot simply zero out buffer length, because 
							// that could delete previously committed-to writes.
							// Unless(!) the buffer is flushed at start of the 
							// method. If so, it could be discarded safely.
							// Really inefficient, though. 
							// However, current impl is safe unless the 
							// inflater is closed asynchronously, which is 
							// extremely unlikey in an application not written 
							// by Shakespearean Monkeys. 
							// In other words, this is fine.
							n = (int) (inf.getBytesWritten() - totalOld);
							// re-throw specific checked exception
							throw new DataFormatException(ex);
						} catch (NullPointerException nex) {
							// incorrect usage actually more critical than 
							// malformed input, no? input will still be bad 
							// when used correctly.
							// this will probably never happen, anyway.
							throw new ConcurrentModificationException(ASYNC_MSG, ex);
						}
					} catch (Error ex) {
						try {
							n = (int) (inf.getBytesWritten() - totalOld);
							throw ex;	// re-throw as is
						} catch (NullPointerException nex) {
							throw ex;	// errors take priority as is
						}
					} catch (Throwable ex) {
						try {
							n = (int) (inf.getBytesWritten() - totalOld);
							throw ex;	// re-throw as is
						} catch (NullPointerException nex) {
							throw new ConcurrentModificationException(ASYNC_MSG, ex);
						}
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
				try {
					this.pos = pos - inf.getRemaining();	// adjust for discarded input
				} finally {
					inf.setInput(EMPTY);					// discard remaining input
				}
			}
		}
		// general solution, requires an extra array to inflate to, which is 
		// then written to the destination buffer.
		// at least the input array can be used directly, so it's still superior
		// to any other generic solution.
		long len = Math.min(numBytesOut, Long.MAX_VALUE - dest.pos());
		if (len == 0L) {
			// cannot produce output
			return 0L;
		}
		// request buffer of default size
		byte[] buf = BufferCache.requestBuffer();
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
								int f = seek(pos, s);
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
					try {
						n = (int) (inf.getBytesWritten() - totalOld);
						throw new DataFormatException(ex);
					} catch (NullPointerException nex) {
						throw new ConcurrentModificationException(ASYNC_MSG, ex);
					}
				} catch (Error ex) {
					try {
						n = (int) (inf.getBytesWritten() - totalOld);
						throw ex;	// re-throw as is
					} catch (NullPointerException nex) {
						throw ex;	// errors take priority as is
					}
				} catch (Throwable ex) {
					try {
						n = (int) (inf.getBytesWritten() - totalOld);
						throw ex;	// re-throw as is
					} catch (NullPointerException nex) {
						throw new ConcurrentModificationException(ASYNC_MSG, ex);
					}
				} finally {
					// must write even when errors occur, 
					// in order for bytes consumed and procuded
					// to stay synced. And because both impls must behave the 
					// same way.
					dest.write(buf, 0, n);
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
				try {
					this.pos = pos - inf.getRemaining();	// adjust for discarded input
				} finally {
					inf.setInput(EMPTY);					// discard remaining input
				}
			}
		}
	}

	// If a only a small numbers of bytes are tranferred at a time, the 
	// overhead of I/O calls becomes a performance concern. 
	// Instead, use buffering to speed up repeated small transfers.
	// TODO needs testing to find optimal value.
	private static final long FC_TRANSFER_THRESHOLD = 64L;

	@Override
	long _transfer(long numBytes, IOBuffer dest, long pos, long s) throws IOException {
		if (dest.getClass() == GeneralIOBuffer.class) {
			GeneralIOBuffer out = (GeneralIOBuffer) dest;
			out.ensureOpen();
			long out_pos = out.pos;
			long n = Math.min(numBytes, Long.MAX_VALUE - out_pos);
			if (n == 0L) {
				return 0L;
			}
			long end = pos + n;
			if (n > FC_TRANSFER_THRESHOLD
					&& this.source.getClass() == FileIOAddress.FileIOSource.class
					&& out.source.getClass() == FileIOAddress.FileIOSource.class) {
				if (this.bufMod) {
					long bpos = this.bufPos;
					int blen = this.bufLen;
					if (pos < bpos + blen && end > bpos) {
						// tranfer range in buffer range. 
						// ensure modified bytes are transferred.
						doFlush(bpos, blen);
					}
				}
				if (out.bufMod) {
					long out_bpos = out.bufPos;
					int out_blen = out.bufLen;
					if (pos < out_bpos + out_blen && end > out_bpos) {
						// tranfer range in buffer range. 
						// ensure no modified bytes are swallowed.
						doFlush(out_bpos, out_blen);
						out.bufLen = 0;	// dispose of buffer
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
			try {
				do {
					int f = seek(pos, s);
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
				int f = seek(pos, s);
				int b = ((int) Math.min(end - this.bufPos, this.bufLen)) - f;
				dest.write(this.buffer, f, b);
				pos += b;
			} while (pos != end);
			return n;
		} finally {
			this.pos = pos;
		}
	}

	@Override
	public IOBuffer createSibling() throws IOException {
		ensureOpen();	// for flush
		flush();		// sync sibling with this buffer
		return new GeneralIOBuffer(this.source, this.shared, this.bufferSize);
	}

	@Override
	void _updateChecksum(long pos, Checksum sum, long end, long s) {
		// pos != end
		do {
			int f = seek(pos, s);
			int b = ((int) Math.min(end - this.bufPos, this.bufLen)) - f;
			sum.update(this.buffer, f, b);
			pos += b;
		} while (pos != end);
	}
}

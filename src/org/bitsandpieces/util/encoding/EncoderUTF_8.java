/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.encoding;

/**
 *
 * @author pp
 */
final class EncoderUTF_8 extends AbstractEncoder {

	private static final char NONE = 0;	// 0 not a high surrogate

	private int numBytesPending;
	private int bytesPending;
	private char highSurrogate = NONE;

	@Override
	int _encode(char[] src, byte[] buf, int off, int len) {
		int _offset = this.offset;
		int _limit = this.limit;
		int y = off;
		int m = off + len;
		char hs = this.highSurrogate;
		if (hs != NONE) {
			// have a high surrogate waiting. 
			// cannot have pending output (code point unresolved)
			if (_offset >= _limit) {
				return UNDERFLOW;
			}
			// have at least one input char available
			// have at least one output byte available
			this.highSurrogate = NONE;	// always consumed
			char c = src[_offset];
			if (!Character.isLowSurrogate(c)) {
				// cannot resolve current code point
				return ERROR;	// have not produced any output yet
			}
			// can resolve code point
			_offset++;	// only consume next char on successful resolution
			int cp = Character.toCodePoint(hs, c);
			int b0 = (0xf0 | ((cp >> 18)));
			int b1 = (0x80 | ((cp >> 12) & 0x3f));
			int b2 = (0x80 | ((cp >> 6) & 0x3f));
			int b3 = (0x80 | (cp & 0x3f));
			buf[y++] = (byte) b0;
			if (y == m) {
				this.numBytesPending = 3;
				this.bytesPending = (b3 << 16) | (b2 << 8) | b1;
				return 1;
			}
			buf[y++] = (byte) b1;
			if (y == m) {
				this.numBytesPending = 2;
				this.bytesPending = (b3 << 8) | b2;
				return 2;
			}
			buf[y++] = (byte) b2;
			if (y == m) {
				this.numBytesPending = 1;
				this.bytesPending = b1;
				return 3;
			}
			buf[y++] = (byte) b3;
		} else {
			// may have output pending
			int np = this.numBytesPending;
			if (np == 0) {
				if (_offset == _limit) {
					return UNDERFLOW;
				}
			} else {
				// have at least 1 ouputbut byte available
				int p = this.bytesPending;
				do {
					buf[y++] = (byte) (p & 0xff);
					p >>>= 8;
					np--;
					if (y == m) {
						this.numBytesPending = np;
						this.bytesPending = p;
						return len;
					}
				} while (np != 0);
				this.numBytesPending = 0;
				this.bytesPending = 0;
			}
		}
		// have neither a high surrogate waiting, nor any pending output
		// optimized loop without internal bounds checks or handling of pending output
		for (int maxIn = _limit - 1, maxOut = m - 3; _offset < maxIn && y < maxOut;) {
			char c = src[_offset++];
			if (c < '\u0080') {
				// 1 byte, 7 bits
				buf[y++] = (byte) c;
				continue;
			}
			if (c < '\u0800') {
				// 2 bytes, 11 bits
				buf[y++] = (byte) (0xc0 | (c >> 6));
				buf[y++] = (byte) (0x80 | (c & 0x3f));
				continue;
			}
			if (!Character.isSurrogate(c)) {
				// 3 bytes, 16 bits
				buf[y++] = (byte) (0xe0 | ((c >> 12)));
				buf[y++] = (byte) (0x80 | ((c >> 6) & 0x3f));
				buf[y++] = (byte) (0x80 | (c & 0x3f));
				continue;
			}
			// 4 bytes, 21 bits
			if (Character.isHighSurrogate(c)) {
				char d = src[_offset];
				if (Character.isLowSurrogate(d)) {
					_offset++;	// only consume next char on success
					int cp = Character.toCodePoint(c, d);
					buf[y++] = (byte) (0xf0 | ((cp >> 18)));
					buf[y++] = (byte) (0x80 | ((cp >> 12) & 0x3f));
					buf[y++] = (byte) (0x80 | ((cp >> 6) & 0x3f));
					buf[y++] = (byte) (0x80 | (cp & 0x3f));
					continue;
				}
				// malformed
			}
			// malformed
			this.offset = _offset;
			int n = y - off;
			if (n == 0) {
				return ERROR;
			}
			this.statePending = ERROR;
			return n;
		}
		// slow loop for remainder
		for (; _offset < _limit && y < m;) {
			char c = src[_offset++];
			if (c < '\u0080') {
				// 1 byte, 7 bits
				buf[y++] = (byte) c;
				continue;
			}
			if (c < '\u0800') {
				// 2 bytes, 11 bits
				buf[y++] = (byte) (0xc0 | (c >> 6));
				int b1 = 0x80 | (c & 0x3f);
				if (y == m) {
					this.bytesPending = b1;
					this.numBytesPending = 1;
					break;
				}
				buf[y++] = (byte) b1;
				continue;
			}
			if (!Character.isSurrogate(c)) {
				// 3 bytes, 16 bits
				buf[y++] = (byte) (0xe0 | ((c >> 12)));
				int b1 = 0x80 | ((c >> 6) & 0x3f);
				int b2 = 0x80 | (c & 0x3f);
				if (y == m) {
					this.numBytesPending = 2;
					this.bytesPending = (b2 << 8) | b1;
					break;
				}
				buf[y++] = (byte) b1;
				if (y == m) {
					this.numBytesPending = 1;
					this.bytesPending = b2;
					break;
				}
				buf[y++] = (byte) b2;
				continue;
			}
			// 4 bytes, 21 bits
			if (Character.isHighSurrogate(c)) {
				if (_offset == _limit) {
					this.highSurrogate = c;
					if (y == off) {
						this.offset = _offset;
						return UNDERFLOW;
					}
					break;
				}
				char d = src[_offset];
				if (Character.isLowSurrogate(d)) {
					_offset++;	// only consume next char on success
					int cp = Character.toCodePoint(c, d);
					buf[y++] = (byte) (0xf0 | ((cp >> 18)));
					int b1 = 0x80 | ((cp >> 12) & 0x3f);
					int b2 = 0x80 | ((cp >> 6) & 0x3f);
					int b3 = 0x80 | (cp & 0x3f);
					if (y == m) {
						this.numBytesPending = 3;
						this.bytesPending = (b3 << 16) | (b2 << 8) | b1;
						break;
					}
					buf[y++] = (byte) b1;
					if (y == m) {
						this.numBytesPending = 2;
						this.bytesPending = (b3 << 8) | b2;
						break;
					}
					buf[y++] = (byte) b2;
					if (y == m) {
						this.numBytesPending = 1;
						this.bytesPending = b3;
						break;
					}
					buf[y++] = (byte) b3;
					continue;
				}
				// malformed
			}
			// malformed
			if (y == off) {
				this.offset = _offset;
				return ERROR;
			}
			this.statePending = ERROR;
			break;
		}
		this.offset = _offset;
		return y - off;
	}

	@Override
	int _encode(CharSequence src, byte[] buf, int off, int len) {
		int _offset = this.offset;
		int _limit = this.limit;
		int y = off;
		int m = off + len;
		char hs = this.highSurrogate;
		if (hs != NONE) {
			// have a high surrogate waiting. 
			// cannot have pending output (code point unresolved)
			if (_offset >= _limit) {
				return UNDERFLOW;
			}
			// have at least one input char available
			// have at least one output byte available
			this.highSurrogate = NONE;	// always consumed
			char c = src.charAt(_offset);
			if (!Character.isLowSurrogate(c)) {
				// cannot resolve current code point
				return ERROR;	// have not produced any output yet
			}
			// can resolve code point
			_offset++;	// only consume next char on successful resolution
			int cp = Character.toCodePoint(hs, c);
			int b0 = (0xf0 | ((cp >> 18)));
			int b1 = (0x80 | ((cp >> 12) & 0x3f));
			int b2 = (0x80 | ((cp >> 6) & 0x3f));
			int b3 = (0x80 | (cp & 0x3f));
			buf[y++] = (byte) b0;
			if (y == m) {
				this.numBytesPending = 3;
				this.bytesPending = (b3 << 16) | (b2 << 8) | b1;
				return 1;
			}
			buf[y++] = (byte) b1;
			if (y == m) {
				this.numBytesPending = 2;
				this.bytesPending = (b3 << 8) | b2;
				return 2;
			}
			buf[y++] = (byte) b2;
			if (y == m) {
				this.numBytesPending = 1;
				this.bytesPending = b1;
				return 3;
			}
			buf[y++] = (byte) b3;
		} else {
			// may have output pending
			int np = this.numBytesPending;
			if (np == 0) {
				if (_offset == _limit) {
					return UNDERFLOW;
				}
			} else {
				// have at least 1 ouputbut byte available
				int p = this.bytesPending;
				do {
					buf[y++] = (byte) (p & 0xff);
					p >>>= 8;
					np--;
					if (y == m) {
						this.numBytesPending = np;
						this.bytesPending = p;
						return len;
					}
				} while (np != 0);
				this.numBytesPending = 0;
				this.bytesPending = 0;
			}
		}
		// have neither a high surrogate waiting, nor any pending output
		// optimized loop without internal bounds checks or handling of pending output
		for (int maxIn = _limit - 1, maxOut = m - 3; _offset < maxIn && y < maxOut;) {
			char c = src.charAt(_offset++);
			if (c < '\u0080') {
				// 1 byte, 7 bits
				buf[y++] = (byte) c;
				continue;
			}
			if (c < '\u0800') {
				// 2 bytes, 11 bits
				buf[y++] = (byte) (0xc0 | (c >> 6));
				buf[y++] = (byte) (0x80 | (c & 0x3f));
				continue;
			}
			if (!Character.isSurrogate(c)) {
				// 3 bytes, 16 bits
				buf[y++] = (byte) (0xe0 | ((c >> 12)));
				buf[y++] = (byte) (0x80 | ((c >> 6) & 0x3f));
				buf[y++] = (byte) (0x80 | (c & 0x3f));
				continue;
			}
			// 4 bytes, 21 bits
			if (Character.isHighSurrogate(c)) {
				char d = src.charAt(_offset);
				if (Character.isLowSurrogate(d)) {
					_offset++;	// only consume next char on success
					int cp = Character.toCodePoint(c, d);
					buf[y++] = (byte) (0xf0 | ((cp >> 18)));
					buf[y++] = (byte) (0x80 | ((cp >> 12) & 0x3f));
					buf[y++] = (byte) (0x80 | ((cp >> 6) & 0x3f));
					buf[y++] = (byte) (0x80 | (cp & 0x3f));
					continue;
				}
				// malformed
			}
			// malformed
			this.offset = _offset;
			int n = y - off;
			if (n == 0) {
				return ERROR;
			}
			this.statePending = ERROR;
			return n;
		}
		// slow loop for remainder
		for (; _offset < _limit && y < m;) {
			char c = src.charAt(_offset++);
			if (c < '\u0080') {
				// 1 byte, 7 bits
				buf[y++] = (byte) c;
				continue;
			}
			if (c < '\u0800') {
				// 2 bytes, 11 bits
				buf[y++] = (byte) (0xc0 | (c >> 6));
				int b1 = 0x80 | (c & 0x3f);
				if (y == m) {
					this.bytesPending = b1;
					this.numBytesPending = 1;
					break;
				}
				buf[y++] = (byte) b1;
				continue;
			}
			if (!Character.isSurrogate(c)) {
				// 3 bytes, 16 bits
				buf[y++] = (byte) (0xe0 | ((c >> 12)));
				int b1 = 0x80 | ((c >> 6) & 0x3f);
				int b2 = 0x80 | (c & 0x3f);
				if (y == m) {
					this.numBytesPending = 2;
					this.bytesPending = (b2 << 8) | b1;
					break;
				}
				buf[y++] = (byte) b1;
				if (y == m) {
					this.numBytesPending = 1;
					this.bytesPending = b2;
					break;
				}
				buf[y++] = (byte) b2;
				continue;
			}
			// 4 bytes, 21 bits
			if (Character.isHighSurrogate(c)) {
				if (_offset == _limit) {
					this.highSurrogate = c;
					if (y == off) {
						this.offset = _offset;
						return UNDERFLOW;
					}
					break;
				}
				char d = src.charAt(_offset);
				if (Character.isLowSurrogate(d)) {
					_offset++;	// only consume next char on success
					int cp = Character.toCodePoint(c, d);
					buf[y++] = (byte) (0xf0 | ((cp >> 18)));
					int b1 = 0x80 | ((cp >> 12) & 0x3f);
					int b2 = 0x80 | ((cp >> 6) & 0x3f);
					int b3 = 0x80 | (cp & 0x3f);
					if (y == m) {
						this.numBytesPending = 3;
						this.bytesPending = (b3 << 16) | (b2 << 8) | b1;
						break;
					}
					buf[y++] = (byte) b1;
					if (y == m) {
						this.numBytesPending = 2;
						this.bytesPending = (b3 << 8) | b2;
						break;
					}
					buf[y++] = (byte) b2;
					if (y == m) {
						this.numBytesPending = 1;
						this.bytesPending = b3;
						break;
					}
					buf[y++] = (byte) b3;
					continue;
				}
				// malformed
			}
			// malformed
			if (y == off) {
				this.offset = _offset;
				return ERROR;
			}
			this.statePending = ERROR;
			break;
		}
		this.offset = _offset;
		return y - off;
	}

	@Override
	public int pendingOutput() {
		return numBytesPending;
	}

	@Override
	public Encoding encoding() {
		return Encoding.UTF_8;
	}

	@Override
	public Encoder reset() {
		super.reset();
		this.numBytesPending = this.bytesPending = 0;
		this.highSurrogate = NONE;
		return this;
	}
}

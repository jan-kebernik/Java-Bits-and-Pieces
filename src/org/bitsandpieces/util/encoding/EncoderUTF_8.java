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

	private static final char NONE = 0;	// not a surrogate

	private int numBytesPending;
	private int bytesPending;
	private char highSurrogate = NONE;

	@Override
	public boolean hasPending() {
		return this.highSurrogate != NONE || this.numBytesPending != 0;
	}

	@Override
	int _encode(CharSequence src, byte[] dest, int off, int maxBytes, int maxCodePoints, int _offset, int _limit) {
		int _numCP = 0;
		int _offsetOld = _offset;
		try {
			int y = off;
			int m = off + maxBytes;
			char high = this.highSurrogate;
			if (high != NONE) {
				// pending input
				if (_offset == _limit) {
					return 0;
				}
				this.highSurrogate = NONE;	// consume pending high surrogate
				char low = src.charAt(_offset);
				if (!Character.isLowSurrogate(low)) {
					// high surrogate is at fault
					return -1;
				}
				_offset++;	// consume low surrogate
				int cp = Character.toCodePoint(high, low);
				int b1 = (0x80 | ((cp >> 12) & 0x3f));
				int b2 = (0x80 | ((cp >> 6) & 0x3f));
				int b3 = (0x80 | (cp & 0x3f));
				dest[y++] = (byte) ((0xf0 | ((cp >> 18))));	// b0
				if (y == m) {
					this.numBytesPending = 3;
					this.bytesPending = (b3 << 16) | (b2 << 8) | b1;
					this.bytes++;
					return 1;
				}
				dest[y++] = (byte) b1;
				if (y == m) {
					this.numBytesPending = 2;
					this.bytesPending = (b3 << 8) | b2;
					this.bytes += 2L;
					return 2;
				}
				dest[y++] = (byte) b2;
				if (y == m) {
					this.numBytesPending = 1;
					this.bytesPending = b1;
					this.bytes += 3L;
					return 3;
				}
				dest[y++] = (byte) b2;
				_numCP++;	// code point resolved
			} else {
				// pending output
				int n = this.numBytesPending;
				if (n != 0) {
					int p = this.bytesPending;
					switch (n) {
						case 3: {
							dest[y++] = (byte) (p & 0xff);
							p >>>= 8;
							if (y == m) {
								this.numBytesPending = 2;
								this.bytesPending = p;
								this.bytes += maxBytes;
								return maxBytes;
							} // FALL-THROUGH
						}
						case 2: {
							dest[y++] = (byte) (p & 0xff);
							p >>>= 8;
							if (y == m) {
								this.numBytesPending = 1;
								this.bytesPending = p;
								this.bytes += maxBytes;
								return maxBytes;
							} // FALL-THROUGH
						}
						default: {
							dest[y++] = (byte) (p & 0xff);
							_numCP++;	// code point resolved
							this.numBytesPending = 0;
							break;
						}
					}
				}
			}
			// fast loop
			for (int maxIn = _limit - 1, maxOut = m - 3; _offset < maxIn && y < maxOut && _numCP < maxCodePoints;) {
				char c = src.charAt(_offset++);
				if (c < '\u0080') {
					// 1 byte, 7 bits
					dest[y++] = (byte) c;
					_numCP++;
					continue;
				}
				if (c < '\u0800') {
					// 2 bytes, 11 bits
					dest[y++] = (byte) (0xc0 | (c >> 6));
					dest[y++] = (byte) (0x80 | (c & 0x3f));
					_numCP++;
					continue;
				}
				if (!Character.isSurrogate(c)) {
					// 3 bytes, 16 bits
					dest[y++] = (byte) (0xe0 | ((c >> 12)));
					dest[y++] = (byte) (0x80 | ((c >> 6) & 0x3f));
					dest[y++] = (byte) (0x80 | (c & 0x3f));
					_numCP++;
					continue;
				}
				// 4 bytes, 21 bits
				if (Character.isHighSurrogate(c)) {
					char d = src.charAt(_offset);
					if (Character.isLowSurrogate(d)) {
						_offset++;	// consume low surrogate
						int cp = Character.toCodePoint(c, d);
						dest[y++] = (byte) (0xf0 | ((cp >> 18)));
						dest[y++] = (byte) (0x80 | ((cp >> 12) & 0x3f));
						dest[y++] = (byte) (0x80 | ((cp >> 6) & 0x3f));
						dest[y++] = (byte) (0x80 | (cp & 0x3f));
						_numCP++;
						continue;
					}
				}
				// malformed code point
				if (y == off) {
					return -1;
				}
				this.pendingError = -1;
				int n = y - off;
				this.bytes += n;
				return n;
			}
			// "slow" remainder loop
			for (; _offset < _limit && y < m && _numCP < maxCodePoints;) {
				char c = src.charAt(_offset++);
				if (c < '\u0080') {
					// 1 byte, 7 bits
					dest[y++] = (byte) c;
					_numCP++;
					continue;
				}
				if (c < '\u0800') {
					// 2 bytes, 11 bits
					dest[y++] = (byte) (0xc0 | (c >> 6));
					int b1 = 0x80 | (c & 0x3f);
					if (y == m) {
						this.bytesPending = b1;
						this.numBytesPending = 1;
						break;
					}
					dest[y++] = (byte) b1;
					_numCP++;
					continue;
				}
				if (!Character.isSurrogate(c)) {
					// 3 bytes, 16 bits
					dest[y++] = (byte) (0xe0 | ((c >> 12)));
					int b1 = 0x80 | ((c >> 6) & 0x3f);
					int b2 = 0x80 | (c & 0x3f);
					if (y == m) {
						this.numBytesPending = 2;
						this.bytesPending = (b2 << 8) | b1;
						break;
					}
					dest[y++] = (byte) b1;
					if (y == m) {
						this.numBytesPending = 1;
						this.bytesPending = b2;
						break;
					}
					dest[y++] = (byte) b2;
					_numCP++;
					continue;
				}
				// 4 bytes, 21 bits
				if (Character.isHighSurrogate(c)) {
					if (_offset == _limit) {
						this.highSurrogate = c;
						if (y == off) {
							return 0;
						}
						break;
					}
					char low = src.charAt(_offset);
					if (Character.isLowSurrogate(low)) {
						_offset++;	// consume low surrogate
						int cp = Character.toCodePoint(c, low);
						dest[y++] = (byte) (0xf0 | ((cp >> 18)));
						int b1 = 0x80 | ((cp >> 12) & 0x3f);
						int b2 = 0x80 | ((cp >> 6) & 0x3f);
						int b3 = 0x80 | (cp & 0x3f);
						if (y == m) {
							this.numBytesPending = 3;
							this.bytesPending = (b3 << 16) | (b2 << 8) | b1;
							break;
						}
						dest[y++] = (byte) b1;
						if (y == m) {
							this.numBytesPending = 2;
							this.bytesPending = (b3 << 8) | b2;
							break;
						}
						dest[y++] = (byte) b2;
						if (y == m) {
							this.numBytesPending = 1;
							this.bytesPending = b3;
							break;
						}
						dest[y++] = (byte) b3;
						_numCP++;
						continue;
					}
				}
				// malformed code point
				if (y == off) {
					return -1;
				}
				this.pendingError = -1;
				break;
			}
			int n = y - off;
			this.bytes += n;
			return n;
		} finally {
			this.offset = _offset;
			this.chars += (_offset - _offsetOld);
			this.codePoints += _numCP;
		}
	}

	@Override
	int _encode(char[] src, byte[] dest, int off, int maxBytes, int maxCodePoints, int _offset, int _limit) {
		int _numCP = 0;
		int _offsetOld = _offset;
		try {
			int y = off;
			int m = off + maxBytes;
			char high = this.highSurrogate;
			if (high != NONE) {
				// pending input
				if (_offset == _limit) {
					return 0;
				}
				this.highSurrogate = NONE;	// consume pending high surrogate
				char low = src[_offset];
				if (!Character.isLowSurrogate(low)) {
					// high surrogate is at fault
					return -1;
				}
				_offset++;	// consume low surrogate
				int cp = Character.toCodePoint(high, low);
				int b1 = (0x80 | ((cp >> 12) & 0x3f));
				int b2 = (0x80 | ((cp >> 6) & 0x3f));
				int b3 = (0x80 | (cp & 0x3f));
				dest[y++] = (byte) ((0xf0 | ((cp >> 18))));	// b0
				if (y == m) {
					this.numBytesPending = 3;
					this.bytesPending = (b3 << 16) | (b2 << 8) | b1;
					this.bytes++;
					return 1;
				}
				dest[y++] = (byte) b1;
				if (y == m) {
					this.numBytesPending = 2;
					this.bytesPending = (b3 << 8) | b2;
					this.bytes += 2L;
					return 2;
				}
				dest[y++] = (byte) b2;
				if (y == m) {
					this.numBytesPending = 1;
					this.bytesPending = b1;
					this.bytes += 3L;
					return 3;
				}
				dest[y++] = (byte) b2;
				_numCP++;	// code point resolved
			} else {
				// pending output
				int n = this.numBytesPending;
				if (n != 0) {
					int p = this.bytesPending;
					switch (n) {
						case 3: {
							dest[y++] = (byte) (p & 0xff);
							p >>>= 8;
							if (y == m) {
								this.numBytesPending = 2;
								this.bytesPending = p;
								this.bytes += maxBytes;
								return maxBytes;
							} // FALL-THROUGH
						}
						case 2: {
							dest[y++] = (byte) (p & 0xff);
							p >>>= 8;
							if (y == m) {
								this.numBytesPending = 1;
								this.bytesPending = p;
								this.bytes += maxBytes;
								return maxBytes;
							} // FALL-THROUGH
						}
						default: {
							dest[y++] = (byte) (p & 0xff);
							_numCP++;	// code point resolved
							this.numBytesPending = 0;
							break;
						}
					}
				}
			}
			// fast loop
			for (int maxIn = _limit - 1, maxOut = m - 3; _offset < maxIn && y < maxOut && _numCP < maxCodePoints;) {
				char c = src[_offset++];
				if (c < '\u0080') {
					// 1 byte, 7 bits
					dest[y++] = (byte) c;
					_numCP++;
					continue;
				}
				if (c < '\u0800') {
					// 2 bytes, 11 bits
					dest[y++] = (byte) (0xc0 | (c >> 6));
					dest[y++] = (byte) (0x80 | (c & 0x3f));
					_numCP++;
					continue;
				}
				if (!Character.isSurrogate(c)) {
					// 3 bytes, 16 bits
					dest[y++] = (byte) (0xe0 | ((c >> 12)));
					dest[y++] = (byte) (0x80 | ((c >> 6) & 0x3f));
					dest[y++] = (byte) (0x80 | (c & 0x3f));
					_numCP++;
					continue;
				}
				// 4 bytes, 21 bits
				if (Character.isHighSurrogate(c)) {
					char d = src[_offset];
					if (Character.isLowSurrogate(d)) {
						_offset++;	// consume low surrogate
						int cp = Character.toCodePoint(c, d);
						dest[y++] = (byte) (0xf0 | ((cp >> 18)));
						dest[y++] = (byte) (0x80 | ((cp >> 12) & 0x3f));
						dest[y++] = (byte) (0x80 | ((cp >> 6) & 0x3f));
						dest[y++] = (byte) (0x80 | (cp & 0x3f));
						_numCP++;
						continue;
					}
				}
				// malformed code point
				if (y == off) {
					return -1;
				}
				this.pendingError = -1;
				int n = y - off;
				this.bytes += n;
				return n;
			}
			// "slow" remainder loop
			for (; _offset < _limit && y < m && _numCP < maxCodePoints;) {
				char c = src[_offset++];
				if (c < '\u0080') {
					// 1 byte, 7 bits
					dest[y++] = (byte) c;
					_numCP++;
					continue;
				}
				if (c < '\u0800') {
					// 2 bytes, 11 bits
					dest[y++] = (byte) (0xc0 | (c >> 6));
					int b1 = 0x80 | (c & 0x3f);
					if (y == m) {
						this.bytesPending = b1;
						this.numBytesPending = 1;
						break;
					}
					dest[y++] = (byte) b1;
					_numCP++;
					continue;
				}
				if (!Character.isSurrogate(c)) {
					// 3 bytes, 16 bits
					dest[y++] = (byte) (0xe0 | ((c >> 12)));
					int b1 = 0x80 | ((c >> 6) & 0x3f);
					int b2 = 0x80 | (c & 0x3f);
					if (y == m) {
						this.numBytesPending = 2;
						this.bytesPending = (b2 << 8) | b1;
						break;
					}
					dest[y++] = (byte) b1;
					if (y == m) {
						this.numBytesPending = 1;
						this.bytesPending = b2;
						break;
					}
					dest[y++] = (byte) b2;
					_numCP++;
					continue;
				}
				// 4 bytes, 21 bits
				if (Character.isHighSurrogate(c)) {
					if (_offset == _limit) {
						this.highSurrogate = c;
						break;
					}
					char low = src[_offset];
					if (Character.isLowSurrogate(low)) {
						_offset++;	// consume low surrogate
						int cp = Character.toCodePoint(c, low);
						dest[y++] = (byte) (0xf0 | ((cp >> 18)));
						int b1 = 0x80 | ((cp >> 12) & 0x3f);
						int b2 = 0x80 | ((cp >> 6) & 0x3f);
						int b3 = 0x80 | (cp & 0x3f);
						if (y == m) {
							this.numBytesPending = 3;
							this.bytesPending = (b3 << 16) | (b2 << 8) | b1;
							break;
						}
						dest[y++] = (byte) b1;
						if (y == m) {
							this.numBytesPending = 2;
							this.bytesPending = (b3 << 8) | b2;
							break;
						}
						dest[y++] = (byte) b2;
						if (y == m) {
							this.numBytesPending = 1;
							this.bytesPending = b3;
							break;
						}
						dest[y++] = (byte) b3;
						_numCP++;
						continue;
					}
				}
				// malformed code point
				if (y == off) {
					return -1;
				}
				this.pendingError = -1;
				break;
			}
			int n = y - off;
			this.bytes += n;
			return n;
		} finally {
			this.offset = _offset;
			this.chars += (_offset - _offsetOld);
			this.codePoints += _numCP;
		}
	}

	@Override
	public Encoding encoding() {
		return Encoding.UTF_8;
	}

	@Override
	public int needsInput() {
		return this.highSurrogate != NONE ? 1 : 0;
	}

	@Override
	public int pendingInput() {
		return this.highSurrogate != NONE ? 1 : 0;
	}

	@Override
	public int pendingOutput() {
		return this.numBytesPending;
	}

	@Override
	public Encoder dropPending() {
		this.chars -= pendingInput();
		this.numBytesPending = 0;
		this.highSurrogate = NONE;
		return this;
	}

	@Override
	public Encoder reset() {
		_reset();
		this.numBytesPending = 0;
		this.highSurrogate = NONE;
		return dropInput();
	}
}

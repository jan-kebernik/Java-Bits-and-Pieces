/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.encoding;

// each code point maps to exactly one output byte
// supplementary code points are unmappable
// high or low surrogates must not be mappable by impls
abstract class SingleByteEncoder extends AbstractEncoder {

	static final int INVALID_ID = 0;	// not mapped
	private static final char NONE = 0;			// not a high surrogate
	private char highSurrogate = NONE;

	int convert(int c) {
		return INVALID_ID;
	}

	// if true, c must not be a surrogate
	boolean isInDirectRange(char c) {
		return c < '\u0080';
	}

	@Override
	final int _encode(CharSequence src, byte[] dest, int off, int maxBytes, int maxCodePoints, int _offset, int _limit) {
		int _offsetOld = _offset;
		int _numCP = 0;
		try {
			char high = this.highSurrogate;
			if (high != NONE) {
				if (_offset == _limit) {
					return 0;
				}
				// consume pending high surrogate
				this.highSurrogate = NONE;
				char c = src.charAt(_offset);
				if (Character.isLowSurrogate(c)) {
					_offset++;	// consume low surrogate
					_numCP++;	// code point resolved
					return -2;
				}
				// malformed code point
				return -1;
			}
			int y = off;
			for (int m = off + maxBytes;
					_offset < _limit && y < m && _numCP < maxCodePoints;) {
				char c = src.charAt(_offset++);
				if (isInDirectRange(c)) {
					// usually ASCII range, never surrogate range
					dest[y++] = (byte) c;
					continue;
				}
				int r = convert(c);
				if (r == INVALID_ID || (r >>> 16) != c) {
					// unmappable, may be surrogate
					if (Character.isHighSurrogate(c)) {
						if (_offset == _limit) {
							// out of input
							this.highSurrogate = c;
							int n = y - off;
							this.bytes += n;
							return n;
						}
						char d = src.charAt(_offset);
						if (Character.isLowSurrogate(d)) {
							_offset++;	// consume low surrogate
							_numCP++;	// code point resolved
							if (y == off) {
								return -2;
							}
							this.pendingError = -2;
							int n = y - off;
							this.bytes += n;
							return n;
						}
					} else if (!Character.isLowSurrogate(c)) {
						// not high or low surrogate, valid single char code point
						_numCP++;
					}
					if (y == off) {
						return -1;
					}
					this.pendingError = -1;
					int n = y - off;
					this.bytes += n;
					return n;
				}
				// mappable, cannot be high or low surrogate
				dest[y++] = (byte) (r & 0xff);
				_numCP++;
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
	final int _encode(char[] src, byte[] dest, int off, int maxBytes, int maxCodePoints, int _offset, int _limit) {
		int _offsetOld = _offset;
		int _numCP = 0;
		try {
			char high = this.highSurrogate;
			if (high != NONE) {
				if (_offset == _limit) {
					return 0;
				}
				// consume pending high surrogate
				this.highSurrogate = NONE;
				char c = src[_offset];
				if (Character.isLowSurrogate(c)) {
					_offset++;	// consume low surrogate
					_numCP++;	// code point resolved
					return -2;
				}
				// malformed code point
				return -1;
			}
			int y = off;
			for (int m = off + maxBytes;
					_offset < _limit && y < m && _numCP < maxCodePoints;) {
				char c = src[_offset++];
				if (isInDirectRange(c)) {
					// usually ASCII range, never surrogate range
					dest[y++] = (byte) c;
					continue;
				}
				int r = convert(c);
				if (r == INVALID_ID || (r >>> 16) != c) {
					// unmappable, may be surrogate
					if (Character.isHighSurrogate(c)) {
						if (_offset == _limit) {
							// out of input
							this.highSurrogate = c;
							int n = y - off;
							this.bytes += n;
							return n;
						}
						char d = src[_offset];
						if (Character.isLowSurrogate(d)) {
							_offset++;	// consume low surrogate
							_numCP++;	// code point resolved
							if (y == off) {
								return -2;
							}
							this.pendingError = -2;
							int n = y - off;
							this.bytes += n;
							return n;
						}
					} else if (!Character.isLowSurrogate(c)) {
						// not a surrogate, valid single char code point
						_numCP++;
					}
					if (y == off) {
						return -1;
					}
					this.pendingError = -1;
					int n = y - off;
					this.bytes += n;
					return n;
				}
				// mappable
				dest[y++] = (byte) (r & 0xff);
				_numCP++;	// cannot be surrogate
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
	public final int needsInput() {
		return this.highSurrogate != NONE ? 1 : 0;
	}

	@Override
	public final int pendingInput() {
		return this.highSurrogate != NONE ? 1 : 0;
	}

	@Override
	public final int pendingOutput() {
		// single bytes only
		return 0;
	}

	@Override
	public boolean hasPending() {
		return this.highSurrogate != NONE;
	}

	@Override
	public final Encoder dropPending() {
		this.chars -= pendingInput();
		this.highSurrogate = NONE;
		return this;
	}

	@Override
	public final Encoder reset() {
		_reset();
		this.highSurrogate = NONE;
		return dropInput();
	}
}

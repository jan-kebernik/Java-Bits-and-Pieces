/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.encoding;

// each code point maps to exactly 1 or 2 output bytes
// supplementary code points are unmappable
// high or low surrogates must not be mappable by impls
abstract class DualByteEncoder extends AbstractEncoder {

	private static final char NO_DEF = DualByteDecoder.NO_DEF;	// UNDEFINED
	private static final char NONE = 0;							// not a surrogate

	private int bytePending;
	private char highSurrogate = NONE;
	private final char[] t;

	DualByteEncoder(char[] t) {
		this.t = t;
	}
	
	@Override
	final int _encode(CharSequence src, byte[] dest, int off, int maxBytes, int maxCodePoints, int _offset, int _limit) {
		int _numCP = 0;
		int _offsetOld = _offset;
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
			int p = this.bytePending;
			if (p != 0) {
				dest[y++] = (byte) (p & 0xff);
				_numCP++;	// code point resolved
			}
			for (int m = off + maxBytes; _offset < _limit && y < m && _numCP < maxCodePoints;) {
				char c = src.charAt(_offset++);
				char r = this.t[c];
				if (r == NO_DEF) {
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
				int lead = r >>> 8;
				if (lead == 0) {
					// single byte
					dest[y++] = (byte) r;
					_numCP++;	// code point resolved
					continue;
				}
				// dual byte
				int cont = r & 0xff;
				dest[y++] = (byte) lead;
				if (y == m) {
					this.bytePending = 0x80000000 | cont;
					this.bytes += maxBytes;
					return maxBytes;
				}
				dest[y++] = (byte) cont;
				_numCP++;	// code point resolved
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
		int _numCP = 0;
		int _offsetOld = _offset;
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
			int p = this.bytePending;
			if (p != 0) {
				dest[y++] = (byte) (p & 0xff);
				_numCP++;	// code point resolved
			}
			for (int m = off + maxBytes; _offset < _limit && y < m && _numCP < maxCodePoints;) {
				char c = src[_offset++];
				char r = this.t[c];
				if (r == NO_DEF) {
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
				int lead = r >>> 8;
				if (lead == 0) {
					// single byte
					dest[y++] = (byte) r;
					_numCP++;	// code point resolved
					continue;
				}
				// dual byte
				int cont = r & 0xff;
				dest[y++] = (byte) lead;
				if (y == m) {
					this.bytePending = 0x80000000 | cont;
					this.bytes += maxBytes;
					return maxBytes;
				}
				dest[y++] = (byte) cont;
				_numCP++;	// code point resolved
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
		// always 0 or 1 byte is pending
		return this.bytePending >>> 31;
	}

	@Override
	public boolean hasPending() {
		return this.highSurrogate != NONE || this.bytePending != 0;
	}
	
	@Override
	public final Encoder dropPending() {
		this.chars -= pendingInput();
		this.bytePending = 0;
		this.highSurrogate = NONE;
		return this;
	}

	@Override
	public final Encoder reset() {
		_reset();
		this.bytePending = 0;
		this.highSurrogate = NONE;
		return dropInput();
	}
}

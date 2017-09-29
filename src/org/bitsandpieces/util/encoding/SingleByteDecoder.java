/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.encoding;

import java.io.UncheckedIOException;

// each byte maps to exactly one code point
// decoded code points must be neither supplementary nor surrogates (single char only)
abstract class SingleByteDecoder extends AbstractDecoder {

	static final char NO_DEF = 0xFFFF;

	abstract char convert(byte b);
	
	@Override
	final int _decode(byte[] src, Appendable dest, int maxChars, int maxCodePoints, int _offset, int _limit) {
		try {
			int _offsetOld = _offset;
			int y = 0;
			for (int m = _offset + Math.min(_limit - _offset, Math.min(maxChars, maxCodePoints)); _offset < m;) {
				char r = convert(src[_offset++]);
				if (r == NO_DEF) {
					// unmappable
					if (y == 0) {
						this.bytes += (_offset - _offsetOld);
						this.offset = _offset;
						return -1;
					}
					this.pendingError = -1;
					break;
				}
				dest.append(r);
				y++;
			}
			this.bytes += (_offset - _offsetOld);
			this.offset = _offset;
			this.chars += y;
			return y;
		} catch (java.io.IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	@Override
	final int _decode(byte[] src, char[] dest, int off, int maxChars, int maxCodePoints, int _offset, int _limit) {
		int _offsetOld = _offset;
		int y = off;
		for (int m = _offset + Math.min(_limit - _offset, Math.min(maxChars, maxCodePoints)); _offset < m;) {
			char r = convert(src[_offset++]);
			if (r == NO_DEF) {
				// unmappable
				if (y == off) {
					this.bytes += (_offset - _offsetOld);
					this.offset = _offset;
					return -1;
				}
				this.pendingError = -1;
				break;
			}
			dest[y++] = r;
		}
		this.bytes += (_offset - _offsetOld);
		this.offset = _offset;
		int n = y - off;
		this.chars += n;
		return n;
	}

	@Override
	public final int needsInput() {
		return 0;
	}

	@Override
	public final int pendingInput() {
		return 0;
	}

	@Override
	public final int pendingOutput() {
		return 0;
	}

	@Override
	public boolean hasPending() {
		return false;
	}
	
	@Override
	public final Decoder dropPending() {
		return this;
	}

	@Override
	public final long codePointsResolved() {
		// each char produced is a valid code point
		return this.chars;
	}
}

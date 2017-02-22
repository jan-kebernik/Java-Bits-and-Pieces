/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.encoding;

/**
 *
 * @author Jan Kebernik
 */
abstract class SingleByteDecoder extends AbstractDecoder {

	SingleByteDecoder() {
		super(0);
	}
	// 1 byte == 1 char

	private void put(int index, char c) {
		preparePut(index)[index] = c;
	}

	// tr to 16-bit codepoint
	abstract char translate(byte inputByte);

	abstract boolean mappable(char i);

	@Override
	public long decode(byte[] buf, int off, int len) {
		if (off < 0 || len < 0 || off > buf.length - len) {
			throw new IndexOutOfBoundsException();
		}
		int _codePoints = this.codePoints;
		long _bytes = this.bytes;
		long _bytesTotal = this.bytesTotal;
		int offOld = off;
		int codePointsOld = _codePoints;
		try {
			Parser _stop = this.stop;
			ErrorHandler _err = this.err;
			//int opLim = this.outputLimit;
			long _inputLimit = this.inputLimit;
			// limit to smaller of len and remaining limit
			int p = _inputLimit < 0L ? len : (int) Math.min(len, _inputLimit - _bytesTotal);
			// output limited
			for (int m = off + p; off < m;) {
				//if (opLim >= 0 && _codePoints >= opLim) {
				//	// output limit reached
				//	return 0L;
				//}
				final int r;
				char b = translate(buf[off++]);
				if (mappable(b)) {
					put(_codePoints++, b);
					r = _stop.accept(b, _codePoints);
				} else {
					put(_codePoints++, DUMMY);
					r = _err.apply(_codePoints);
				}
				if (r >= 0) {
					_bytes -= r;
					_codePoints -= r;
					codePointsOld -= r;
					return r;
				}
			}
			return inputLimitReached(_inputLimit);
		} finally {
			this.codePoints = _codePoints;
			this.codePointsTotal += _codePoints - codePointsOld;

			int num0 = off - offOld;
			this.bytes = _bytes + num0;
			this.bytesTotal = _bytesTotal + num0;
		}
	}

	@Override
	public int chars() {
		return this.codePoints;
	}

	@Override
	public long charsTotal() {
		return this.codePointsTotal;
	}

	@Override
	public int pendingInput() {
		return 0;
	}

	@Override
	public Decoder dropPendingInput() {
		return this;
	}

	@Override
	public String result() {
		return String.valueOf(this.charBuffer, 0, this.codePoints);
	}
}

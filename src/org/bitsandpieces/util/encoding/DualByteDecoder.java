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
abstract class DualByteDecoder extends AbstractDecoder {

	// 1 or 2 bytes input == 1 char
	private static final int ONE = 0, TWO = 1;
	private static final int CNT_EXP = 5, CNT_SHIFT = 1, CNT_MASK = 1;

	private int chars;
	private long charsTotal;
	private int state;
	private int byte1;

	public DualByteDecoder() {
		super(8);
	}

	abstract char getTable0(int index);

	abstract char getTable1(int index);

	abstract boolean isLeadByte(char c);

	abstract boolean hasNoDef(char c);

	private void put(int index, char code) {
		preparePut(index)[index] = code;
	}

	private void inc(int index, int len) {
		inc(index, len, CNT_EXP, CNT_SHIFT);
	}

	private static int dec(int[] a, int index) {
		return dec(a, index, CNT_EXP, CNT_SHIFT, CNT_MASK);
	}

	@Override
	public long decode(byte[] buf, int off, int len) {
		if (off < 0 || len < 0 || off > buf.length - len) {
			throw new IndexOutOfBoundsException();
		}
		int _chars = this.chars;
		int _codePoints = this.codePoints;
		long _bytes = this.bytes;
		long _bytesTotal = this.bytesTotal;
		int s = this.state;
		int offOld = off;
		int codePointsOld = _codePoints;
		int charsOld = _chars;
		try {
			Parser _stop = this.stop;
			ErrorHandler _err = this.err;
			//int opLim = this.outputLimit;
			long _inputLimit = this.inputLimit;
			// limit to smaller of len and remaining limit
			int p = _inputLimit < 0L ? len : (int) Math.min(len, _inputLimit - _bytesTotal);
			int m = off + p;
			int r;	// amount of roll-back
			rollback:
			{
				//if (opLim >= 0 && _codePoints >= opLim) {
				//	// output limit reached
				//	return 0L;
				//}
				// handle partial code point
				if (s != 0) {
					// have lead byte, need follow-up
					if (off >= m) {
						// out of input
						return inputLimitReached(_inputLimit);
					}
					s = 0;
					char c2 = getTable1((this.byte1 << 8) | (buf[off++] & 0xff));
					if (hasNoDef(c2)) {
						// unmappable
						put(_chars++, DUMMY);
						inc(_codePoints++, TWO);	// two bytes (malformed)
						r = _err.apply(_codePoints);
					} else {
						put(_chars++, c2);
						inc(_codePoints++, TWO);	// two bytes
						r = _stop.accept(c2, _codePoints);
					}
					if (r >= 0) {
						break rollback;		// GOTO roll back
					}
				}
				// handle full code points
				// loop while there is no limit, or while it's not reached yet
				//while (opLim < 0 || _codePoints < opLim) {
				while (off < m) {
					// decode next code point
					//if (off >= m) {
					//	// out of input
					//	return inputLimitReached(_inputLimit);
					//}
					int b1 = buf[off++] & 0xff;
					char c = getTable0(b1);
					if (hasNoDef(c)) {
						// unmappable
						put(_chars++, DUMMY);
						inc(_codePoints++, ONE);	// one bytes (malformed)
						r = _err.apply(_codePoints);
						if (r >= 0) {
							break rollback;		// GOTO roll back
						}
						continue;
					}
					if (isLeadByte(c)) {
						// need second byte
						if (off >= m) {
							// out of input
							this.byte1 = b1;
							s = 1;
							return inputLimitReached(_inputLimit);
						}
						char c2 = getTable1((b1 << 8) | (buf[off++] & 0xff));
						if (hasNoDef(c2)) {
							put(_chars++, DUMMY);
							inc(_codePoints++, TWO);	// two bytes (malformed)
							r = _err.apply(_codePoints);
						} else {
							put(_chars++, c2);
							inc(_codePoints++, TWO);	// two bytes
							r = _stop.accept(c2, _codePoints);
						}
						if (r >= 0) {
							break rollback;		// GOTO roll back
						}
						continue;
					}
					// single byte value
					put(_chars++, c);
					inc(_codePoints++, ONE);		// one bytes
					r = _stop.accept(c, _codePoints);
					if (r >= 0) {
						break rollback;		// GOTO roll back
					}
				}
				return inputLimitReached(_inputLimit);

				//// no further output allowed
				//return 0L;	// done. no roll-back
			} // roll back
			if (r > _codePoints) {
				throw new IllegalStateException("Roll-back amount is too great: " + r);
			}
			int[] cnt = this.counts;
			long rb = 0L;	// input roll back amount
			for (int i = _codePoints - 1, h = _codePoints - r; i >= h; i--) {
				// for the last r code points
				rb += dec(cnt, i);				// add to count
			}
			_chars -= r;					// 1 cp == 1 ch
			charsOld -= r;
			_codePoints -= r;
			codePointsOld -= r;
			_bytes -= rb;
			return rb;
		} finally {
			this.codePoints = _codePoints;
			this.codePointsTotal += _codePoints - codePointsOld;

			this.chars = _chars;
			this.charsTotal += charsOld - _chars;

			int num0 = off - offOld;
			this.bytes = _bytes + num0;
			this.bytesTotal = _bytesTotal + num0;

			this.state = s;
		}
	}

	@Override
	public int pendingInput() {
		return this.state;
	}

	@Override
	public Decoder dropPendingInput() {
		int s = this.state;
		if (s != 0) {
			this.state = 0;
			this.bytes--;
		}
		return this;
	}

	@Override
	public int chars() {
		return this.chars;
	}

	@Override
	public long charsTotal() {
		return this.charsTotal;
	}

	@Override
	public String result() {
		return String.valueOf(this.charBuffer, 0, this.chars);
	}

	@Override
	public Decoder reset() {
		super.reset();
		this.state = 0;
		this.chars = 0;
		this.charsTotal = 0L;
		return this;
	}
}

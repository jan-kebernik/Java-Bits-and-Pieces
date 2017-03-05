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
final class EncoderISO_8859_1 extends SingleByteEncoder {

	@Override
	boolean isInDirectRange(char c) {
		return c < '\u0100';
	}

	@Override
	public Encoding encoding() {
		return Encoding.ISO_8859_1;
	}
}

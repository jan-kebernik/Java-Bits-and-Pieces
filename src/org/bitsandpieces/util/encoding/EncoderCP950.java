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
final class EncoderCP950 extends DualByteEncoder {

	private static final char[] TABLE = DecoderCP950.buildConversionTable();
	
	@Override
	char get(int i) {
		return TABLE[i];
	}

	@Override
	public Encoding encoding() {
		return Encoding.CP950;
	}
}
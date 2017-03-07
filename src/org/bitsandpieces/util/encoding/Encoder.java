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
public interface Encoder {

	public static final int UNDERFLOW = -1;
	public static final int ERROR = -2;

	int encode(byte[] buf);
	
	// return bytes written (>= 0)
	// or -1 on underflow	(not enough input left. call feedInput())
	// or -2 on error
	// if < 0, bytes written ALWAYS == 0
	int encode(byte[] buf, int off, int len);

	// num bytes of current code point still waiting to be emitted
	int pendingOutput();

	Encoder feedInput(CharSequence src);

	Encoder feedInput(CharSequence src, int off, int len);

	Encoder feedInput(char[] src);

	Encoder feedInput(char[] src, int off, int len);
	
	Encoding encoding();
	
	Encoder reset();
}

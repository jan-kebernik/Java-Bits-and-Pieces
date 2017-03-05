/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.encoding;

import java.io.UncheckedIOException;

/**
 *
 * @author pp
 */
public interface Decoder {
	
	public static final int UNDERFLOW = -1;
	public static final int ERROR = -2;
	
	int decode(char[] dest);
	
	// return chars written
	// or UNDERFLOW, if not enough input available (call feedInput())
	// or ERROR, if malformed or unmappable input
	// if < 0, chars written ALWAYS == 0
	int decode(char[] dest, int off, int len);
	
	int decode(Appendable dest) throws UncheckedIOException;
	
	int decode(Appendable dest, int len) throws UncheckedIOException;
	
	// num chars of current code point still waiting to be emitted
	int pendingOutput();
	
	Decoder feedInput(byte[] src);
	
	Decoder feedInput(byte[] src, int off, int len);
	
	public Encoding encoding();
}

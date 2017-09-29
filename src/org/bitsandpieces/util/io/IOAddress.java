/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.io;

/**
 * Represents the location of a file in a phyiscal or virtual file system.
 *
 * @author pp
 */
@FunctionalInterface
public interface IOAddress {

	/**
	 * Creates a new {@code IOBuffer} with a size appropriate for most tasks.
	 *
	 * @return a new {@code IOBuffer} for this address.
	 */
	default IOBuffer open() throws IOException {
		return open(BufferCache.DEFAULT_SIZE);
	}

	/**
	 * Creates a new {@code IOBuffer} with the specified size.
	 *
	 * @param bufferSize the internal buffer capactiy of the returned buffer.
	 * @return a new {@code IOBuffer} for this address.
	 */
	IOBuffer open(int bufferSize) throws IOException;
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 * This package provides a more flexible, more efficient and more powerful,
 * alternative to the JDK CharSet API, for a number of common encodings.
 * <p>
 * Errors can be reacted to on a case-by-case basis, Encoders and Decoders do
 * not handle their own input or output allocation and are thus far more
 * light-weight and re-usable, while providing actually useful statistics (and
 * limits) with regards to errors, processed input/output and resolved unicode
 * code points.
 * <p>
 * All implemented character mappings are derived directly from the unicode
 * homepage.
 */
package org.bitsandpieces.util.encoding;

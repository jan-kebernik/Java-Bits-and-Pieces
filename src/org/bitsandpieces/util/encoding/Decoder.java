/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.encoding;

import java.io.UncheckedIOException;

/**
 * {@code Decoder}s are objects that decode {@code byte}s to UTF-16
 * {@code char}s.
 * <p>
 * Both input and output are provided purely by the calling classes, making
 * {@code Decoder}s very light-weight.
 * </p>
 *
 * @author Jan Kebernik
 */
public interface Decoder {

	/**
	 * Decodes the current input and stores the resulting {@code char}s in the
	 * provided array. The value returned is either the number of {@code char}s
	 * stored or a negative value indicating that more input is required to
	 * fully resolve the current code point or that its input is malformed or
	 * unmappable.
	 * <p>
	 * Calling this method is equivalent to calling
	 * {@link #decode(char[], int, int, int) decode(dest, 0, dest.length, Integer.MAX_VALUE)}.
	 * </p>
	 * <p>
	 * The method will terminate when all available input is processed, when no
	 * more output can be produced or when an error occurs. At most
	 * {@code numCodePoints} and at most {@code len} {@code char}s will be
	 * stored. As a result, partial code points may be stored; the remainder
	 * will be held by this {@code Decoder} until more output is made available
	 * in a sub-sequent {@code decode()} call. In practice, this simply means
	 * that if {@link #pendingOutput() pendingOutput()} returns {@code 1}, the
	 * last produced {@code char} is the high-surrogate of a two-{@code char}
	 * code point, and the next produced char will be the low surrogate.
	 * </p>
	 *
	 * @param dest the array in which to store decoded {@code char}s.
	 * @return <ul><li>&gt= 0: The number of {@code char}s stored.</li>
	 * <li>{@link Encoding#UNDERFLOW}: Indicates that more input is required to
	 * resolve the current code point.</li>
	 * <li>{@link Encoding#ERROR}: Indicates that the last code point could not
	 * be resolved due to malformed or unmappable input.</li></ul>
	 * A negative result always implies that no {@code char}s were stored.
	 * @throws NullPointerException if {@code dest} is {@code null}.
	 * @throws IndexOutOfBoundsException if the specified array range is
	 * illegal.
	 * @throws IllegalArgumentException if {@code numCodePoints} is negative.
	 */
	int decode(char[] dest);

	/**
	 * Decodes the current input and stores the resulting {@code char}s in the
	 * provided array. The value returned is either the number of {@code char}s
	 * stored or a negative value indicating that more input is required to
	 * fully resolve the current code point or that its input is malformed or
	 * unmappable.
	 * <p>
	 * The method will terminate when all available input is processed, when no
	 * more output can be produced or when an error occurs. At most
	 * {@code numCodePoints} and at most {@code len} {@code char}s will be
	 * stored. As a result, partial code points may be stored; the remainder
	 * will be held by this {@code Decoder} until more output is made available
	 * in a sub-sequent {@code decode()} call. In practice, this simply means
	 * that if {@link #pendingOutput() pendingOutput()} returns {@code 1}, the
	 * last produced {@code char} is the high-surrogate of a two-{@code char}
	 * code point, and the next produced char will be the low surrogate.
	 * </p>
	 *
	 * @param dest the array in which to store decoded {@code char}s.
	 * @param off the index at which to start storing {@code char}s.
	 * @param len the desired number of {@code char}s to decode.
	 * @param numCodePoints the desired number of code points to produce.
	 * @return <ul><li>&gt= 0: The number of {@code char}s stored.</li>
	 * <li>{@link Encoding#UNDERFLOW}: Indicates that more input is required to
	 * resolve the current code point.</li>
	 * <li>{@link Encoding#ERROR}: Indicates that the last code point could not
	 * be resolved due to malformed or unmappable input.</li></ul>
	 * A negative result always implies that no {@code char}s were stored.
	 * @throws NullPointerException if {@code dest} is {@code null}.
	 * @throws IndexOutOfBoundsException if the specified array range is
	 * illegal.
	 * @throws IllegalArgumentException if {@code numCodePoints} is negative.
	 */
	int decode(char[] dest, int off, int len, int numCodePoints);

	/**
	 * Decodes the current input and stores the resulting {@code char}s in the
	 * provided {@code Appendable}. The value returned is either the number of
	 * {@code char}s stored or a negative value indicating that more input is
	 * required to fully resolve the current code point or that its input is
	 * malformed or unmappable.
	 * <p>
	 * Calling this method is equivalent to calling
	 * {@link #decode(Appendable, int, int) decode(dest, Integer.MAX_VALUE, Integer.MAX_VALUE)}.
	 * </p>
	 * <p>
	 * The method will terminate when all available input is processed, when no
	 * more output can be produced or when an error occurs. At most
	 * {@code numCodePoints} and at most {@code len} {@code char}s will be
	 * stored. As a result, partial code points may be stored; the remainder
	 * will be held by this {@code Decoder} until more output is made available
	 * in a sub-sequent {@code decode()} call. In practice, this simply means
	 * that if {@link #pendingOutput() pendingOutput()} returns {@code 1}, the
	 * last produced {@code char} is the high-surrogate of a two-{@code char}
	 * code point, and the next produced char will be the low surrogate.
	 * </p>
	 *
	 * @param dest the {@code Appendable} in which to store decoded
	 * {@code char}s.
	 * @return <ul><li>&gt= 0: The number of {@code char}s stored.</li>
	 * <li>{@link Encoding#UNDERFLOW}: Indicates that more input is required to
	 * resolve the current code point.</li>
	 * <li>{@link Encoding#ERROR}: Indicates that the last code point could not
	 * be resolved due to malformed or unmappable input. Malformed code points
	 * are not included in {@link #codePoints() codePoints()}</li></ul>
	 * A negative result always implies that no {@code char}s were stored.
	 * @throws NullPointerException if {@code dest} is {@code null}.
	 * @throws IllegalArgumentException if {@code numCodePoints} or {@code len}
	 * is negative.
	 */
	int decode(Appendable dest) throws UncheckedIOException;

	/**
	 * Decodes the current input and stores the resulting {@code char}s in the
	 * provided {@code Appendable}. The value returned is either the number of
	 * {@code char}s stored or a negative value indicating that more input is
	 * required to fully resolve the current code point or that its input is
	 * malformed or unmappable.
	 * <p>
	 * The method will terminate when all available input is processed, when no
	 * more output can be produced or when an error occurs. At most
	 * {@code numCodePoints} and at most {@code len} {@code char}s will be
	 * stored. As a result, partial code points may be stored; the remainder
	 * will be held by this {@code Decoder} until more output is made available
	 * in a sub-sequent {@code decode()} call. In practice, this simply means
	 * that if {@link #pendingOutput() pendingOutput()} returns {@code 1}, the
	 * last produced {@code char} is the high-surrogate of a two-{@code char}
	 * code point, and the next produced char will be the low surrogate.
	 * </p>
	 *
	 * @param dest the {@code Appendable} in which to store decoded
	 * {@code char}s.
	 * @param len the desired number of {@code char}s to decode.
	 * @param numCodePoints the desired number of code points to produce.
	 * @return <ul><li>&gt= 0: The number of {@code char}s stored.</li>
	 * <li>{@link Encoding#UNDERFLOW}: Indicates that more input is required to
	 * resolve the current code point.</li>
	 * <li>{@link Encoding#ERROR}: Indicates that the last code point could not
	 * be resolved due to malformed or unmappable input. Malformed code points
	 * are not included in {@link #codePoints() codePoints()}</li></ul>
	 * A negative result always implies that no {@code char}s were stored.
	 * @throws NullPointerException if {@code dest} is {@code null}.
	 * @throws IllegalArgumentException if {@code numCodePoints} or {@code len}
	 * is negative.
	 */
	int decode(Appendable dest, int len, int numCodePoints) throws UncheckedIOException;

	/**
	 * Returns the number of output {@code char}s that are held by this
	 * {@code Decoder} as part of the code point currently being decoded.
	 *
	 * @return the number of output {@code char}s that are held by this
	 * {@code Decoder} as part of the code point currently being decoded.
	 */
	int pendingOutput();

	/**
	 * Returns the number of input {@code byte}s held by this {@code Decoder} as
	 * part of the code point currently being decoded.
	 *
	 * @return the number of input {@code byte}s held by this {@code Decoder} as
	 * part of the code point currently being decoded.
	 */
	int pendingInput();

	/**
	 * Returns the number of additional input {@code byte}s required to fully
	 * resolve the code point currently being decoded.
	 *
	 * @return the number of additional input {@code byte}s required to fully
	 * resolve the code point currently being decoded.
	 */
	int needsInput();

	/**
	 * Returns the number of input {@code byte}s this {@code Decoder} currently
	 * has access to. This does <em>NOT</em> include any
	 * {@link #pendingInput() pending input}.
	 *
	 * @return the number of input {@code byte}s this {@code Decoder} currently
	 * has access to.
	 */
	int inputRemaining();

	/**
	 * Instructs this {@code Decoder} to use the specified {@code byte}s as
	 * input.
	 * <p>
	 * Note that this {@code Decoder} must not have any input remaining at the
	 * time of invoking this method, and will throw an
	 * {@code IllegalStateException} unless
	 * {@link #inputRemaining() inputRemaining()} reports a value of {@code 0}
	 * or unless {@code decode()} returns {@link Encoding#UNDERFLOW UNDERFLOW}.
	 * Either continue decoding or {@link #reset() reset()} this {@code Decoder}
	 * to consume or discard any remaining input prior to calling this method.
	 * </p>
	 *
	 * @param src the array whose {@code byte}s to use as input
	 * @return this {@code Decoder}
	 * @throws IllegalStateException if this {@code Decoder} still has input
	 * available.
	 * @throws NullPointerException if {@code src} is {@code null}
	 */
	Decoder feedInput(byte[] src);

	/**
	 * Instructs this {@code Decoder} to use the specified {@code byte}s as
	 * input.
	 * <p>
	 * Note that this {@code Decoder} must not have any input remaining at the
	 * time of invoking this method, and will throw an
	 * {@code IllegalStateException} unless
	 * {@link #inputRemaining() inputRemaining()} reports a value of {@code 0}
	 * or unless {@code decode()} returns {@link Encoding#UNDERFLOW UNDERFLOW}.
	 * Either continue decoding or {@link #reset() reset()} this {@code Decoder}
	 * to consume or discard any remaining input prior to calling this method.
	 * </p>
	 *
	 * @param src the array whose {@code byte}s to use as input
	 * @param off the index of the first {@code byte}
	 * @param len the number of {@code byte}s to use
	 * @return this {@code Decoder}
	 * @throws IllegalStateException if this {@code Decoder} still has input
	 * available.
	 * @throws NullPointerException if {@code src} is {@code null}
	 * @throws IndexOutOfBoundsException if the specified array range is illegal
	 */
	Decoder feedInput(byte[] src, int off, int len) throws IllegalStateException;

	/**
	 * Returns this {@code Decoder}'s {@link Encoding}.
	 *
	 * @return this {@code Decoder}'s {@code Encoding}.
	 */
	Encoding encoding();

	/**
	 * Resets this {@code Decoder} to its initial state.
	 *
	 * @return this {@code Decoder}
	 */
	Decoder reset();

	/**
	 * Returns the number of <em>valid</em> code points produced since the last
	 * {@link #reset() reset}. Malformed or unmappable code points are not
	 * counted this way.
	 *
	 * @return the number of <em>valid</em> code points produced since the last
	 * {@code reset}.
	 */
	long codePoints();

	/**
	 * Returns the number of input {@code byte}s processed since the last
	 * {@link #reset() reset}.
	 *
	 * @return the number of input {@code byte}s processed since the last
	 * {@link #reset() reset}.
	 */
	long bytes();

	/**
	 * Causes this {@code Decoder} to discard its current input. Pending input
	 * is <em>unaffacted</em> by this operation.
	 *
	 * @return this {@code Decoder}
	 */
	Decoder dropInput();
}

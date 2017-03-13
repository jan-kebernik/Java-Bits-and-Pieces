/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util.encoding;

/**
 * {@code Encoder}s are objects that encode UTF-16 {@code char}s to
 * {@code byte}s.
 * <p>
 * Both input and output are provided purely by the calling classes, making
 * {@code Encoder}s very light-weight.
 * </p>
 *
 * @author Jan Kebernik
 */
public interface Encoder {

	/**
	 * Encodes the current input and stores the resulting {@code bytes}s in the
	 * provided array. The value returned is either the number of {@code byte}s
	 * stored or a negative value indicating that more input is required to
	 * fully resolve the current code point or that its input is malformed or
	 * unmappable.
	 * <p>
	 * Calling this method is equivalent to calling
	 * {@link #encode(byte[], int, int, int) encode(dest, 0, dest.length, Integer.MAX_VALUE)}.
	 * </p>
	 * <p>
	 * The method will terminate when all available input is processed, when no
	 * more output can be produced or when an error occurs. At most
	 * {@code numCodePoints} are encoded and at most {@code len} {@code bytes}s
	 * will be stored. As a result, partial code points may be stored; the
	 * remainder will be held by this {@code Encoder} until more output is made
	 * available in a sub-sequent {@code encode()} call. If
	 * {@link #pendingInput() pendingInput()} returns {@code 1}, the last
	 * {@code char} processed is the high-surrogate of a two-{@code char} code
	 * point, and the next char processed will likely be the low surrogate.
	 * </p>
	 *
	 * @param dest the array in which to store encoded {@code byte}s.
	 * @return <ul><li>&gt= 0: The number of {@code bytes}s stored.</li>
	 * <li>{@link Encoding#UNDERFLOW}: Indicates that more input is required to
	 * resolve the current code point.</li>
	 * <li>{@link Encoding#ERROR}: Indicates that the last code point could not
	 * be resolved due to malformed or unmappable input.</li></ul>
	 * A negative result always implies that no {@code bytes}s were stored.
	 * @throws NullPointerException if {@code dest} is {@code null}.
	 */
	int encode(byte[] dest);

	/**
	 * Encodes the current input and stores the resulting {@code bytes}s in the
	 * provided array. The value returned is either the number of {@code byte}s
	 * stored or a negative value indicating that more input is required to
	 * fully resolve the current code point or that its input is malformed or
	 * unmappable.
	 * <p>
	 * The method will terminate when all available input is processed, when no
	 * more output can be produced or when an error occurs. At most
	 * {@code numCodePoints} are encoded and at most {@code len} {@code bytes}s
	 * will be stored. As a result, partial code points may be stored; the
	 * remainder will be held by this {@code Encoder} until more output is made
	 * available in a sub-sequent {@code encode()} call. If
	 * {@link #pendingInput() pendingInput()} returns {@code 1}, the last
	 * {@code char} processed is the high-surrogate of a two-{@code char} code
	 * point, and the next char processed will likely be the low surrogate.
	 * </p>
	 *
	 * @param dest the array in which to store encoded {@code byte}s.
	 * @param off the index at which to start storing {@code byte}s.
	 * @param len the desired number of {@code byte}s to decode.
	 * @param numCodePoints the desired number of code points to decode.
	 * @return <ul><li>&gt= 0: The number of {@code bytes}s stored.</li>
	 * <li>{@link Encoding#UNDERFLOW}: Indicates that more input is required to
	 * resolve the current code point.</li>
	 * <li>{@link Encoding#ERROR}: Indicates that the last code point could not
	 * be resolved due to malformed or unmappable input.</li></ul>
	 * A negative result always implies that no {@code bytes}s were stored.
	 * @throws NullPointerException if {@code dest} is {@code null}.
	 * @throws IndexOutOfBoundsException if the specified array range is
	 * illegal.
	 * @throws IllegalArgumentException if {@code numCodePoints} is negative.
	 */
	int encode(byte[] dest, int off, int len, int numCodePoints);

	/**
	 * Returns the number of output {@code bytes}s that are held by this
	 * {@code Encoder} as part of the code point currently being encoded.
	 *
	 * @return the number of output {@code bytes}s that are held by this
	 * {@code Encoder} as part of the code point currently being encoded.
	 */
	int pendingOutput();

	/**
	 * Returns the number of input {@code chars}s held by this {@code Encoder}
	 * as part of the code point currently being encoded.
	 *
	 * @return the number of input {@code chars}s held by this {@code Encoder}
	 * as part of the code point currently being encoded.
	 */
	int pendingInput();

	/**
	 * Returns the number of additional input {@code chars}s required to fully
	 * resolve the code point currently being encoded.
	 *
	 * @return the number of additional input {@code chars}s required to fully
	 * resolve the code point currently being encoded.
	 */
	int needsInput();

	/**
	 * Returns the number of input {@code chars}s this {@code Decoder} currently
	 * has access to. This does <em>NOT</em> include any
	 * {@link #pendingInput() pending input}.
	 *
	 * @return the number of input {@code chars}s this {@code Decoder} currently
	 * has access to.
	 */
	int inputRemaining();

	/**
	 * Instructs this {@code Encoder} to use the specified {@code chars}s as
	 * input.
	 * <p>
	 * Note that this {@code Encoder} must not have any input remaining at the
	 * time of invoking this method, and will throw an
	 * {@code IllegalStateException} unless
	 * {@link #inputRemaining() inputRemaining()} reports a value of {@code 0}
	 * or unless {@code encode()} returns {@link Encoding#UNDERFLOW UNDERFLOW}.
	 * Either continue encoding or {@link #reset() reset()} this {@code Encoder}
	 * to consume or discard any remaining input prior to calling this method.
	 * </p>
	 *
	 * @param src the {@code CharSequence} whose {@code char}s to use as input
	 * @return this {@code Encoder}
	 * @throws IllegalStateException if this {@code Encoder} still has input
	 * available.
	 * @throws NullPointerException if {@code src} is {@code null}
	 */
	Encoder feedInput(CharSequence src);

	/**
	 * Instructs this {@code Encoder} to use the specified {@code chars}s as
	 * input.
	 * <p>
	 * Note that this {@code Encoder} must not have any input remaining at the
	 * time of invoking this method, and will throw an
	 * {@code IllegalStateException} unless
	 * {@link #inputRemaining() inputRemaining()} reports a value of {@code 0}
	 * or unless {@code encode()} returns {@link Encoding#UNDERFLOW UNDERFLOW}.
	 * Either continue encoding or {@link #reset() reset()} this {@code Encoder}
	 * to consume or discard any remaining input prior to calling this method.
	 * </p>
	 *
	 * @param src the {@code CharSequence} whose {@code char}s to use as input
	 * @param off the index of the first {@code char}
	 * @param len the number of {@code char}s to use
	 * @return this {@code Encoder}
	 * @throws IllegalStateException if this {@code Encoder} still has input
	 * available.
	 * @throws NullPointerException if {@code src} is {@code null}
	 * @throws IndexOutOfBoundsException if the specified range is illegal
	 */
	Encoder feedInput(CharSequence src, int off, int len);

	/**
	 * Instructs this {@code Encoder} to use the specified {@code chars}s as
	 * input.
	 * <p>
	 * Note that this {@code Encoder} must not have any input remaining at the
	 * time of invoking this method, and will throw an
	 * {@code IllegalStateException} unless
	 * {@link #inputRemaining() inputRemaining()} reports a value of {@code 0}
	 * or unless {@code encode()} returns {@link Encoding#UNDERFLOW UNDERFLOW}.
	 * Either continue encoding or {@link #reset() reset()} this {@code Encoder}
	 * to consume or discard any remaining input prior to calling this method.
	 * </p>
	 *
	 * @param src the array whose {@code char}s to use as input
	 * @return this {@code Encoder}
	 * @throws IllegalStateException if this {@code Encoder} still has input
	 * available.
	 * @throws NullPointerException if {@code src} is {@code null}
	 */
	Encoder feedInput(char[] src);

	/**
	 * Instructs this {@code Encoder} to use the specified {@code chars}s as
	 * input.
	 * <p>
	 * Note that this {@code Encoder} must not have any input remaining at the
	 * time of invoking this method, and will throw an
	 * {@code IllegalStateException} unless
	 * {@link #inputRemaining() inputRemaining()} reports a value of {@code 0}
	 * or unless {@code encode()} returns {@link Encoding#UNDERFLOW UNDERFLOW}.
	 * Either continue encoding or {@link #reset() reset()} this {@code Encoder}
	 * to consume or discard any remaining input prior to calling this method.
	 * </p>
	 *
	 * @param src the array whose {@code char}s to use as input
	 * @param off the index of the first {@code char}
	 * @param len the number of {@code char}s to use
	 * @return this {@code Encoder}
	 * @throws IllegalStateException if this {@code Encoder} still has input
	 * available.
	 * @throws NullPointerException if {@code src} is {@code null}
	 * @throws IndexOutOfBoundsException if the specified array range is illegal
	 */
	Encoder feedInput(char[] src, int off, int len);

	/**
	 * Returns this {@code Encoder}'s {@link Encoding}.
	 *
	 * @return this {@code Encoder}'s {@code Encoding}.
	 */
	Encoding encoding();

	/**
	 * Resets this {@code Encoder} to its initial state.
	 *
	 * @return this {@code Encoder}
	 */
	Encoder reset();

	/**
	 * Returns the number of <em>valid</em> code points resolved since the last
	 * {@link #reset() reset}. Malformed or unmappable code points are not
	 * counted this way.
	 *
	 * @return the number of <em>valid</em> code points resolved since the last
	 * {@code reset}.
	 */
	long codePoints();

	/**
	 * Causes this {@code Encoder} to discard its current input. Pending input
	 * is <em>unaffacted</em> by this operation.
	 *
	 * @return this {@code Encoder}
	 */
	Encoder dropInput();
}

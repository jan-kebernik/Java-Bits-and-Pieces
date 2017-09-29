/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util;

/**
 * {@code Format}s allow for fast and flexible conversion of integer primitives
 * into {@code String}s or {@code char}-arrays.
 * <p>
 * Each {@code Format} has an alphabet, is either signed or unsigned, truncated
 * or untruncated and has an optional prefix, infix and suffix. The standard
 * implementations use internal look-up tables in order to limit the amount of
 * divisions required to fully parse a value. Unless otherwise noted,
 * {@code Format}s should be considered immutable and thus safe for use by
 * multiple concurrent threads. Unless otherwise noted, {@code char} primitives
 * are handled as signed or unsigned depending on whether the {@code Format} is
 * signed.
 * </p><p>
 * Each {@code Format} serves as a builder for other {@code Format}s with the
 * same alphabet. The default implementations will alwas create instances that
 * share their internal look-up tables, and are thus very cheap to vary.
 * </p><p>
 * The first character in the provided alphabet is considered the alphabet's
 * "zero". All leading occurances of this character will be truncated by
 * {@code Format}s whose {@link #isTruncated() isTruncated()} method returns
 * {@code true}. Note that this does not apply to subsequent characters in the
 * alphabet with the same value as the "zero".
 * </p><p>
 * There are presets for commonly-used alphabets available
 * ({@link #BIN binary}, {@link #OCT octal}, {@link #DEC decimal} and
 * {@link #HEX hexadecimal}). {@code Format}s with arbitrary alphabets can be
 * created using {@link #get(String) get()}.
 * </p>
 *
 * @author Jan Kebernik
 */
public interface Format {

	/**
	 * Unsigned, truncated binary {@code Format} (letters '0' and '1') with
	 * prefix "0b".
	 */
	public static final Format BIN = Formats.get("01").unsigned().prefix("0b");

	/**
	 * Unsigned, truncated octal {@code Format} (letters '0' through '7').
	 */
	public static final Format OCT = Formats.get("01234567").unsigned();

	/**
	 * Signed, truncated decimal {@code Format} (letters '0' through '9').
	 */
	public static final Format DEC = Formats.get("0123456789");

	/**
	 * Unsigned, truncated, lower-case hexadecimal {@code Format} (letters '0'
	 * through 'f') with prefix "0x".
	 */
	public static final Format HEX = Formats.get("0123456789abcdef").unsigned().prefix("0x");

	/**
	 * Returns a signed, truncated {@code Format} for the specified alphabet.
	 *
	 * @param alphabet contains the alphabet used by the new {@code Format}.
	 * This {@code String} must have a length of at least 2 and at most 256.
	 * Note that each char in the string counts as a single letter of the
	 * alphabet, meaning that supplementary Unicode codepoints are not accounted
	 * for.
	 * @return a signed, truncated {@code Format} for the specified alphabet.
	 */
	public static Format get(String alphabet) {
		return Formats.get(alphabet);
	}

	/**
	 * Defines an array into which to copy. Responsible for performing its own
	 * bounds checks. Allows standard {@code Format}s to avoid unnecessary array
	 * allocation by delegating this task to an outside class. This is useful
	 * because the number of characters to be generated can often not be known
	 * before-hand. This way it would be trivial for an outside class to keep
	 * track of and resize its own array to pass to a {@code Format}.
	 */
	@FunctionalInterface
	public static interface ArrayTarget {

		/**
		 * Must return a char array with a length of at least {@code off + len}.
		 *
		 * @param off the offset into the returned array at which the
		 * {@code Format} will attempt to copy chars.
		 * @param len the number of chars the {@code Format} will attempt to
		 * copy into the returned array.
		 * @return a char array with a length of at least {@code off + len}.
		 */
		char[] getArray(int off, int len);
	}

	/**
	 * Copies the resulting characters into the array defined by the specified
	 * {@code ArrayTarget}.
	 *
	 * @param t the {@code ArrayTarget} defining the char array.
	 * @param off the offset to which to copy the resulting characters.
	 * @param b the value whose character representation to copy.
	 * @return the number of the characters copied into the array.
	 */
	int copy(ArrayTarget t, int off, byte b);

	/**
	 * Returns a new char array representing the specified value.
	 *
	 * @param b the value whose character representation to calculate.
	 * @return a new char array representing the specified value
	 */
	char[] toCharArray(byte b);

	/**
	 * Returns a new {@code String} representing the specified value.
	 *
	 * @param b the value whose character representation to calculate.
	 * @return a new {@code String} representing the specified value
	 */
	String toString(byte b);

	/**
	 * Copies the resulting characters into the array defined by the specified
	 * {@code ArrayTarget}.
	 *
	 * @param t the {@code ArrayTarget} defining the char array.
	 * @param off the offset to which to copy the resulting characters.
	 * @param c the value whose character representation to copy.
	 * @return the number of the characters copied into the array.
	 */
	int copy(ArrayTarget t, int off, char c);

	/**
	 * Returns a new char array representing the specified value.
	 *
	 * @param c the value whose character representation to calculate.
	 * @return a new char array representing the specified value
	 */
	char[] toCharArray(char c);

	/**
	 * Returns a new {@code String} representing the specified value.
	 *
	 * @param c the value whose character representation to calculate.
	 * @return a new {@code String} representing the specified value
	 */
	String toString(char c);

	/**
	 * Copies the resulting characters into the array defined by the specified
	 * {@code ArrayTarget}.
	 *
	 * @param t the {@code ArrayTarget} defining the char array.
	 * @param off the offset to which to copy the resulting characters.
	 * @param s the value whose character representation to copy.
	 * @return the number of the characters copied into the array.
	 */
	int copy(ArrayTarget t, int off, short s);

	/**
	 * Returns a new char array representing the specified value.
	 *
	 * @param s the value whose character representation to calculate.
	 * @return a new char array representing the specified value
	 */
	char[] toCharArray(short s);

	/**
	 * Returns a new {@code String} representing the specified value.
	 *
	 * @param s the value whose character representation to calculate.
	 * @return a new {@code String} representing the specified value
	 */
	String toString(short s);

	/**
	 * Copies the resulting characters into the array defined by the specified
	 * {@code ArrayTarget}.
	 *
	 * @param t the {@code ArrayTarget} defining the char array.
	 * @param off the offset to which to copy the resulting characters.
	 * @param i the value whose character representation to copy.
	 * @return the number of the characters copied into the array.
	 */
	int copy(ArrayTarget t, int off, int i);

	/**
	 * Returns a new char array representing the specified value.
	 *
	 * @param i the value whose character representation to calculate.
	 * @return a new char array representing the specified value
	 */
	char[] toCharArray(int i);

	/**
	 * Returns a new {@code String} representing the specified value.
	 *
	 * @param i the value whose character representation to calculate.
	 * @return a new {@code String} representing the specified value
	 */
	String toString(int i);

	/**
	 * Copies the resulting characters into the array defined by the specified
	 * {@code ArrayTarget}.
	 *
	 * @param t the {@code ArrayTarget} defining the char array.
	 * @param off the offset to which to copy the resulting characters.
	 * @param l the value whose character representation to copy.
	 * @return the number of the characters copied into the array.
	 */
	int copy(ArrayTarget t, int off, long l);

	/**
	 * Returns a new char array representing the specified value.
	 *
	 * @param l the value whose character representation to calculate.
	 * @return a new char array representing the specified value
	 */
	char[] toCharArray(long l);

	/**
	 * Returns a new {@code String} representing the specified value.
	 *
	 * @param l the value whose character representation to calculate.
	 * @return a new {@code String} representing the specified value
	 */
	String toString(long l);

	/**
	 * Return the alphabet used by this {@code Format}.
	 *
	 * @return the alphabet used by this {@code Format}
	 */
	String alphabet();

	/**
	 * Returns this {@code Format}'s prefix or null, if no prefix is set.
	 *
	 * @return this {@code Format}'s prefix or null, if no prefix is set.
	 */
	String prefix();

	/**
	 * Returns a copy of this {@code Format} with the specified value as its
	 * prefix, or potentially this {@code Format} if its current prefix already
	 * equals the specified value.
	 *
	 * @param prefix the value the returned {@code Format} should use as its
	 * prefix, or {@code null} if no prefix should be used.
	 * @return a copy of this {@code Format} with the specified value as its
	 * prefix, or potentially this {@code Format} if its current prefix already
	 * equals the specified value.
	 */
	Format prefix(String prefix);

	/**
	 * Returns this {@code Format}'s suffix or null, if no suffix is set.
	 *
	 * @return this {@code Format}'s suffix or null, if no suffix is set.
	 */
	String suffix();

	/**
	 * Returns a copy of this {@code Format} with the specified value as its
	 * suffix, or potentially this {@code Format} if its current suffix already
	 * equals the specified value.
	 *
	 * @param suffix the value the returned {@code Format} should use as its
	 * suffix, or {@code null} if no suffix should be used.
	 * @return a copy of this {@code Format} with the specified value as its
	 * suffix, or potentially this {@code Format} if its current suffix already
	 * equals the specified value.
	 */
	Format suffix(String suffix);

	/**
	 * Returns this {@code Format}'s infix or null, if no infix is set.
	 *
	 * @return this {@code Format}'s infix or null, if no infix is set.
	 */
	Character infix();

	/**
	 * Returns a copy of this {@code Format} with the specified
	 * {@code Character}'s value as its infix, or potentially this
	 * {@code Format} if its current infix already equals the specified value.
	 *
	 * @param infix the {@code Character} whose value the returned
	 * {@code Format} should use as its infix, or {@code null} if no infix
	 * should be used.
	 * @return a copy of this {@code Format} with the specified
	 * {@code Character}'s value as its infix, or potentially this
	 * {@code Format} if its current infix already equals the specified value.
	 */
	Format infix(Character infix);

	/**
	 * Returns the interval at which this {@code Format} places its infix, if
	 * any.
	 *
	 * @return the interval at which this {@code Format} places its infix, if
	 * any.
	 */
	int interval();

	/**
	 * Returns a copy of this {@code Format} that places its infix at the
	 * specified value or potentially this {@code Format} it its current
	 * interval already equals the specified value.
	 *
	 * @param interval the interval at which the returned {@code Format} should
	 * place its infix.
	 * @return a copy of this {@code Format} that places its infix at the
	 * specified value or potentially this {@code Format} it its current
	 * interval already equals the specified value.
	 * @throws IllegalArgumentException if interval is not positive.
	 */
	Format interval(int interval);

	/**
	 * Returns {@code true} if this {@code Format} differentiates between
	 * negative and non-negative values.
	 *
	 * @return {@code true} if this {@code Format} differentiates between
	 * negative and non-negative values.
	 */
	boolean isSigned();

	/**
	 * Returns a copy of this {@code Format} for which
	 * {@link #isSigned() isSigned()} returns {@code true} or potentially this
	 * {@code Format} if such is already the case.
	 *
	 * @return a copy of this {@code Format} for which
	 * {@link #isSigned() isSigned()} returns {@code true} or potentially this
	 * {@code Format} if such is already the case.
	 */
	Format signed();

	/**
	 * Returns a copy of this {@code Format} for which
	 * {@link #isSigned() isSigned()} returns {@code false} or potentially this
	 * {@code Format} if such is already the case.
	 *
	 * @return a copy of this {@code Format} for which
	 * {@link #isSigned() isSigned()} returns {@code false} or potentially this
	 * {@code Format} if such is already the case.
	 */
	Format unsigned();

	/**
	 * Returns a copy of this {@code Format} for which
	 * {@link #isSigned() isSigned()} returns the specified value or potentially
	 * this {@code Format} if such is already the case.
	 *
	 * @param signed wether the returned {@code Format} should differentiate
	 * between negative and non-negative values.
	 * @return a copy of this {@code Format} for which
	 * {@link #isSigned() isSigned()} returns the specified value or potentially
	 * this {@code Format} if such is already the case.
	 */
	default Format signed(boolean signed) {
		return signed ? signed() : unsigned();
	}

	/**
	 * Returns {@code true} if this {@code Format} omits leading zeros.
	 *
	 * @return {@code true} if this {@code Format} omits leading zeros.
	 */
	boolean isTruncated();

	/**
	 * Returns a copy of this {@code Format} for which
	 * {@link #isTruncated() isTruncated()} returns {@code true} or potentially
	 * this {@code Format} if such is already the case.
	 *
	 * @return a copy of this {@code Format} for which
	 * {@link #isTruncated() isTruncated()} returns {@code true} or potentially
	 * this {@code Format} if such is already the case.
	 */
	Format truncated();

	/**
	 * Returns a copy of this {@code Format} for which
	 * {@link #isTruncated() isTruncated()} returns {@code false} or potentially
	 * this {@code Format} if such is already the case.
	 *
	 * @return a copy of this {@code Format} for which
	 * {@link #isTruncated() isTruncated()} returns {@code false} or potentially
	 * this {@code Format} if such is already the case.
	 */
	Format untruncated();

	/**
	 * Returns a copy of this {@code Format} for which
	 * {@link #isTruncated() isTruncated()} returns the specified value or
	 * potentially this {@code Format} if such is already the case.
	 *
	 * @param truncated wether the returned {@code Format} should omit leading
	 * zeros.
	 * @return a copy of this {@code Format} for which
	 * {@link #isTruncated() isTruncated()} returns the specified value or
	 * potentially this {@code Format} if such is already the case.
	 */
	default Format truncated(boolean truncated) {
		return truncated ? truncated() : untruncated();
	}

	/**
	 * Creates a copy of this {@code Format} with the same settings as the
	 * specified {@code Format}, or potentially this {@code Format} if all
	 * settings are already equal.
	 *
	 * @param f the {@code Format} whose settings to assume.
	 * @return a copy of this {@code Format} with the same settings as the
	 * specified {@code Format}, or potentially this {@code Format} if all
	 * settings are already equal
	 */
	default Format copySettings(Format f) {
		return infix(f.infix())
				.interval(f.interval())
				.prefix(f.prefix())
				.suffix(f.suffix())
				.signed(f.isSigned())
				.truncated(f.isTruncated());
	}

	// impl provided
	@Override
	public boolean equals(Object obj);

	// impl provided
	@Override
	public int hashCode();
}

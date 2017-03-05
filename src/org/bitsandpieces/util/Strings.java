/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitsandpieces.util;

import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Purely performance-oriented class that allows for reflective access to a
 * String's char array. If Reflection is disabled, the provided methods will
 * fall-back on non-reflective solutions, and log an approriate failure message.
 *
 * @author Jan Kebernik
 */
public final class Strings {

	// test
	private static final String FAIL_MESSAGE = "Reflection failed. Falling back on general solution.";

	public static final char NUL = 0;

	// Must be a BiFunction instead of a custom interface, because the 
	// Lookup would not have access to classes outside the standard (bootstrap) scope.
	private static final BiFunction<char[], Boolean, String> CONSTRUCTOR = construcor();
	private static final Function<String, char[]> GETTER = getter();

	private static MethodHandles.Lookup trustedLookup() throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		MethodHandles.Lookup caller = MethodHandles.lookup().in(String.class);
		Field modes = MethodHandles.Lookup.class.getDeclaredField("allowedModes");
		modes.setAccessible(true);
		modes.setInt(caller, -1);
		return caller;
	}

	// returns a lambda that has direct access to String's shared
	// constructor if reflection is allowed. 
	// otherwise returns a function that creates new Strings conventionally.
	// thread-safety is not a concern, given that each thread is allowed to 
	// fail (recoverably) independently
	private static BiFunction<char[], Boolean, String> construcor() {
		try {
			// create trusted Lookup with access to String internals
			MethodHandles.Lookup lookup = trustedLookup();
			// create handle for shared String constructor
			MethodHandle handle = lookup.findConstructor(
					String.class, MethodType.methodType(void.class, char[].class, boolean.class)
			);
			// create lambda using trusted Lookup and Handle
			return (BiFunction) LambdaMetafactory.metafactory(
					lookup,
					"apply",
					MethodType.methodType(BiFunction.class),
					handle.type().generic(),
					handle,
					handle.type()
			).getTarget().invokeExact();
		} catch (Error err) {
			// re-throw serious errors.
			throw err;
		} catch (Throwable ex) {
			// log recoverable exceptions.
			Logger.getLogger(Formats.class.getName()).log(Level.WARNING, FAIL_MESSAGE, ex);
			return (char[] a, Boolean b) -> String.valueOf(a);
		}
	}

	public static Supplier getter(Object obj, Field f) throws Throwable {
		MethodHandles.Lookup caller = MethodHandles.lookup();
		MethodHandle handle = caller.findVirtual(Field.class, "get", MethodType.genericMethodType(1));
		return (Supplier) LambdaMetafactory.metafactory(
				caller,
				"get",
				handle.type().changeReturnType(Supplier.class),
				MethodType.genericMethodType(0),
				handle,
				MethodType.genericMethodType(0)
		).getTarget().invoke(f, Modifier.isStatic(f.getModifiers()) ? null : obj);
	}

	private static Function<String, char[]> getter() {
		// Reflection API seems fastest here.
		// Cannot use lambdas, because private fields cannot be accessed that 
		// way (in the absence of a getter method).
		try {
			Field field = String.class.getDeclaredField("value");
			field.setAccessible(true);
			return (String s) -> {
				try {
					return (char[]) field.get(s);
				} catch (IllegalArgumentException | IllegalAccessException ex) {
					Logger.getLogger(Formats.class.getName()).log(Level.WARNING, FAIL_MESSAGE, ex);
					return s.toCharArray();
				}
			};
		} catch (NoSuchFieldException | SecurityException ex) {
			Logger.getLogger(Formats.class.getName()).log(Level.WARNING, FAIL_MESSAGE, ex);
			return String::toCharArray;
		}
	}

	private Strings() {
	}

	/**
	 * Attempts to create a new String instance sharing the specified char
	 * array. If Reflection is disabled, will fall back on regular String
	 * instantiation. This method is not safe, as there is potential for
	 * String's immutability contract to be broken due to leaked internals. Use
	 * at your own risk.
	 *
	 * @param a the char array to be shared by the new String
	 * @return a new String sharing the supplied char array or at least its
	 * contents.
	 */
	public static final String newString(char[] a) {
		return CONSTRUCTOR.apply(a, true);
	}

	/**
	 * Attempts to return the specified String's internal char array. If
	 * Reflection is disabled, will return a new char array with equal contents.
	 * This method is not safe, as there is potential for String's immutability
	 * contract to be broken due to leaked internals. Use at your own risk.
	 *
	 * @param s the String whose char array to return
	 * @return the char array used by the supplied String or a new char array
	 * with the String's contents
	 */
	public static final char[] getValue(String s) {
		return GETTER.apply(s);
	}
}

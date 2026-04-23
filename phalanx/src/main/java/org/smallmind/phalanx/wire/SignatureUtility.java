/*
 * Copyright (c) 2007 through 2026 David Berkman
 *
 * This file is part of the SmallMind Code Project.
 *
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under either, at your discretion...
 *
 * 1) The terms of GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * ...or...
 *
 * 2) The terms of the Apache License, Version 2.0.
 *
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or Apache License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and the Apache License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://www.apache.org/licenses/LICENSE-2.0>.
 *
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.phalanx.wire;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import org.smallmind.web.json.scaffold.fault.Fault;

/**
 * Encodes and decodes Java types to and from wire-level signature strings.
 *
 * <p>Two signature formats are supported:
 * <ul>
 *   <li><b>Neutral encoding</b> — a transport-portable format based on {@link BuiltInType}
 *       codes, used for comparing signatures across JVM boundaries.</li>
 *   <li><b>Native encoding/decoding</b> — standard JVM descriptor notation (e.g., {@code I},
 *       {@code [Ljava/lang/String;}), suitable for use with reflection and bytecode tooling.</li>
 * </ul>
 * Decoded class lookups are cached in a {@link ConcurrentHashMap} to avoid repeated class loading.</p>
 */
public class SignatureUtility {

  private static final ConcurrentHashMap<String, Class<?>> SIGNATURE_MAP = new ConcurrentHashMap<>();

  /**
   * Encodes a Java class into the transport-portable neutral signature format.
   *
   * <p>Primitives, the well-known reference types {@link String}, {@link LocalDateTime},
   * {@link Fault}, and {@link Object}, and array types each map to a specific
   * {@link BuiltInType} code. All other reference types encode as {@code !SimpleClassName}.
   * Array dimensionality is represented by bracket notation prepended to the element-type code.
   * {@code null}, {@code void}, and {@link Void} all encode as the void code.</p>
   *
   * @param clazz the class to encode; {@code null}, {@code void}, and {@link Void} map to
   *              the void neutral code
   * @return the neutral signature string for {@code clazz}; never {@code null}
   */
  public static String neutralEncode (Class<?> clazz) {

    if ((clazz == null) || void.class.equals(clazz) || Void.class.equals(clazz)) {

      return BuiltInType.VOID.getCode();
    } else {

      StringBuilder codeBuilder = new StringBuilder();

      if (clazz.isArray()) {

        int dimensions = 0;

        codeBuilder.append('[');
        while (clazz.isArray()) {
          dimensions++;
          clazz = clazz.getComponentType();
        }

        codeBuilder.append(",".repeat(Math.max(0, (dimensions - 1))));
        codeBuilder.append(']');
      }

      if (clazz.isPrimitive()) {
        if (clazz.equals(boolean.class)) {
          codeBuilder.insert(0, BuiltInType.BOOLEAN.getCode());
        } else if (clazz.equals(byte.class)) {
          codeBuilder.insert(0, BuiltInType.BYTE.getCode());
        } else if (clazz.equals(short.class)) {
          codeBuilder.insert(0, BuiltInType.SHORT.getCode());
        } else if (clazz.equals(int.class)) {
          codeBuilder.insert(0, BuiltInType.INTEGER.getCode());
        } else if (clazz.equals(long.class)) {
          codeBuilder.insert(0, BuiltInType.LONG.getCode());
        } else if (clazz.equals(float.class)) {
          codeBuilder.insert(0, BuiltInType.FLOAT.getCode());
        } else if (clazz.equals(double.class)) {
          codeBuilder.insert(0, BuiltInType.DOUBLE.getCode());
        } else if (clazz.equals(char.class)) {
          codeBuilder.insert(0, BuiltInType.CHARACTER.getCode());
        }
      } else if (clazz.equals(String.class)) {
        codeBuilder.insert(0, BuiltInType.STRING.getCode());
      } else if (clazz.equals(LocalDateTime.class)) {
        codeBuilder.insert(0, BuiltInType.DATE.getCode());
      } else if (clazz.equals(Fault.class)) {
        codeBuilder.insert(0, BuiltInType.FAULT.getCode());
      } else if (clazz.equals(Object.class)) {
        codeBuilder.insert(0, BuiltInType.OBJECT.getCode());
      } else {
        codeBuilder.insert(0, clazz.getSimpleName()).insert(0, '!');
      }

      return codeBuilder.toString();
    }
  }

  /**
   * Encodes a Java class into JVM descriptor notation.
   *
   * <p>Array classes are prefixed with one {@code [} per dimension. Primitive types map to their
   * single-character JVM descriptors ({@code Z}, {@code B}, {@code S}, {@code I}, {@code J},
   * {@code F}, {@code D}, {@code C}). Reference types encode as {@code L<binary-name>;}.
   * {@code null}, {@code void}, and {@link Void} encode as {@code "V"}.</p>
   *
   * @param clazz the class to encode; {@code null}, {@code void}, and {@link Void} map to
   *              {@code "V"}
   * @return the JVM descriptor string for {@code clazz} (e.g., {@code "I"} or
   * {@code "[Ljava/lang/String;"}); never {@code null}
   */
  public static String nativeEncode (Class<?> clazz) {

    if ((clazz == null) || void.class.equals(clazz) || Void.class.equals(clazz)) {

      return "V";
    } else {

      StringBuilder codeBuilder = new StringBuilder();

      while (clazz.isArray()) {
        codeBuilder.append("[");
        clazz = clazz.getComponentType();
      }

      if (clazz.isPrimitive()) {
        if (clazz.equals(boolean.class)) {
          codeBuilder.append("Z");
        } else if (clazz.equals(byte.class)) {
          codeBuilder.append("B");
        } else if (clazz.equals(short.class)) {
          codeBuilder.append("S");
        } else if (clazz.equals(int.class)) {
          codeBuilder.append("I");
        } else if (clazz.equals(long.class)) {
          codeBuilder.append("J");
        } else if (clazz.equals(float.class)) {
          codeBuilder.append("F");
        } else if (clazz.equals(double.class)) {
          codeBuilder.append("D");
        } else if (clazz.equals(char.class)) {
          codeBuilder.append("C");
        }
      } else {
        codeBuilder.append('L').append(clazz.getName().replace('.', '/')).append(';');
      }

      return codeBuilder.toString();
    }
  }

  /**
   * Resolves a JVM descriptor string produced by {@link #nativeEncode(Class)} back to its
   * {@link Class}.
   *
   * <p>Single-character primitive descriptors ({@code V}, {@code Z}, {@code B}, {@code C},
   * {@code S}, {@code I}, {@code J}, {@code F}, {@code D}) resolve directly to their Java
   * primitive or wrapper types. Object descriptors ({@code L...;}) and array descriptors
   * ({@code [}) are resolved via {@link #getObjectType(String)} with caching.</p>
   *
   * @param type the JVM descriptor string to decode
   * @return the {@link Class} corresponding to {@code type}; never {@code null}
   * @throws ClassNotFoundException if the descriptor refers to an unknown reference type or
   *                                its first character is not a recognized JVM type code
   */
  public static Class<?> nativeDecode (String type)
    throws ClassNotFoundException {

    return switch (type.charAt(0)) {
      case 'V' -> Void.class;
      case 'Z' -> boolean.class;
      case 'B' -> byte.class;
      case 'C' -> char.class;
      case 'S' -> short.class;
      case 'I' -> int.class;
      case 'J' -> long.class;
      case 'F' -> float.class;
      case 'D' -> (double.class);
      case 'L' -> getObjectType(type.substring(1, type.length() - 1).replace('/', '.'));
      case '[' -> getObjectType(type.replace('/', '.'));
      default -> throw new ClassNotFoundException("Unknown format for parameter signature(" + type + ")");
    };
  }

  /**
   * Resolves a class by its fully qualified name or array descriptor, caching the result.
   *
   * <p>The first call for a given {@code type} string invokes {@link Class#forName(String)}
   * and caches the result; subsequent calls return the cached value without triggering
   * class loading.</p>
   *
   * @param type the fully qualified class name or JVM array descriptor (with {@code /}
   *             already replaced by {@code .})
   * @return the resolved {@link Class}; never {@code null}
   * @throws ClassNotFoundException if the class cannot be found by the current class loader
   */
  private static Class<?> getObjectType (String type)
    throws ClassNotFoundException {

    Class<?> objectType;

    if ((objectType = SIGNATURE_MAP.get(type)) == null) {
      synchronized (SIGNATURE_MAP) {
        if ((objectType = SIGNATURE_MAP.get(type)) == null) {
          SIGNATURE_MAP.put(type, objectType = Class.forName(type));
        }
      }
    }

    return objectType;
  }
}

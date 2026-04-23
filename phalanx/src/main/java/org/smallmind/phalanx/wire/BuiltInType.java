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

/**
 * Canonical set of primitive and common types recognized by the wire protocol, each
 * identified by a single-character signature code used in compact type encoding. The codes
 * are intentionally short to minimize on-wire overhead in method signatures.
 */
public enum BuiltInType {

  /**
   * Signed boolean value; wire code {@code Z}.
   */
  BOOLEAN("Z"),
  /**
   * Signed 8-bit integer; wire code {@code B}.
   */
  BYTE("B"),
  /**
   * Unsigned 8-bit integer; wire code {@code Y}.
   */
  U_BYTE("Y"),
  /**
   * Signed 16-bit integer; wire code {@code S}.
   */
  SHORT("S"),
  /**
   * Unsigned 16-bit integer; wire code {@code H}.
   */
  U_SHORT("H"),
  /**
   * Signed 32-bit integer; wire code {@code I}.
   */
  INTEGER("I"),
  /**
   * Unsigned 32-bit integer; wire code {@code N}.
   */
  U_INTEGER("N"),
  /**
   * Signed 64-bit integer; wire code {@code L}.
   */
  LONG("L"),
  /**
   * Unsigned 64-bit integer; wire code {@code U}.
   */
  U_LONG("U"),
  /**
   * 32-bit IEEE 754 floating-point value; wire code {@code F}.
   */
  FLOAT("F"),
  /**
   * 64-bit IEEE 754 floating-point value; wire code {@code D}.
   */
  DOUBLE("D"),
  /**
   * Single Unicode character; wire code {@code C}.
   */
  CHARACTER("C"),
  /**
   * UTF-8 string; wire code {@code G}.
   */
  STRING("G"),
  /**
   * Date/time instant; wire code {@code T}.
   */
  DATE("T"),
  /**
   * Absent return value; wire code {@code V}.
   */
  VOID("V"),
  /**
   * Encoded fault/exception payload; wire code {@code A}.
   */
  FAULT("A"),
  /**
   * Opaque object whose concrete type is determined at runtime; wire code {@code O}.
   */
  OBJECT("O");

  private final String code;

  /**
   * Associates the single-character signature code with this type constant.
   *
   * @param code the wire-level type code; must be exactly one character
   */
  BuiltInType (String code) {

    this.code = code;
  }

  /**
   * Returns the single-character code that identifies this type in a wire signature.
   *
   * @return the type's wire encoding code
   */
  public String getCode () {

    return code;
  }
}

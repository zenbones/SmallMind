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
package org.smallmind.web.websocket;

/**
 * WebSocket frame operation codes.
 */
public enum OpCode {

  CONTINUATION((byte)0x0), TEXT((byte)0x1), BINARY((byte)0x2), CLOSE((byte)0x8), PING((byte)0x9), PONG((byte)0xA);
  private final byte code;

  /**
   * Assigns the numeric code associated with this opcode.
   *
   * @param code the opcode value
   */
  OpCode (byte code) {

    this.code = code;
  }

  /**
   * Converts a raw frame byte into a matching opcode.
   *
   * @param singleByte the byte containing the opcode
   * @return the matching opcode or {@code null} if none found
   */
  public static OpCode convert (byte singleByte) {

    byte maskedValue = (byte)(singleByte & 0xF);

    for (OpCode opCode : OpCode.values()) {
      if (opCode.getCode() == maskedValue) {

        return opCode;
      }
    }

    return null;
  }

  /**
   * Returns the numeric opcode value.
   *
   * @return the opcode byte
   */
  public byte getCode () {

    return code;
  }
}

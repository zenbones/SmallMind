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

import org.smallmind.nutsnbolts.util.Bytes;

/**
 * Enumerates the standard WebSocket close codes and provides helpers to convert between numeric and binary forms.
 */
public enum CloseCode {

  NORMAL(1000), GOING_AWAY(1001), PROTOCOL_ERROR(1002), UNKNOWN_DATA_TYPE(1003), RESERVED(1004), NO_STATUS_CODE(1005), CLOSED_ABNORMALLY(1006), DATA_TYPE_CONVERSION_ERROR(1007), POLICY_VIOLATION(1008),
  MESSAGE_TOO_LARGE(1009), MISSING_EXTENSION(1010), SERVER_ERROR(1011), SERVICE_RESTART(1012), TRY_AGAIN_LATER(1013), TLS_HANDSHAKE_FAILURE(1015);

  private final int code;

  /**
   * Creates a close code wrapper for the supplied numeric value.
   *
   * @param code the numeric close code
   */
  CloseCode (int code) {

    this.code = code;
  }

  /**
   * Resolves a {@link CloseCode} from its numeric representation.
   *
   * @param code the close code value
   * @return the matching enum value, or {@link #NO_STATUS_CODE} if not found
   */
  public static CloseCode fromCode (int code) {

    for (CloseCode closeCode : CloseCode.values()) {
      if (closeCode.getCode() == code) {
        return closeCode;
      }
    }

    return CloseCode.NO_STATUS_CODE;
  }

  /**
   * Resolves a {@link CloseCode} from a two-byte network-order representation.
   *
   * @param bytes the two-byte close payload
   * @return the matching enum value, or {@link #NO_STATUS_CODE} if not found
   */
  public static CloseCode fromBytes (byte[] bytes) {

    return fromCode(Bytes.getShort(bytes));
  }

  /**
   * Numeric representation of the close code.
   *
   * @return the close code value
   */
  public int getCode () {

    return code;
  }

  /**
   * Returns the close code encoded into a two-byte network-order array.
   *
   * @return the two-byte representation
   */
  public byte[] getCodeAsBytes () {

    byte[] out = new byte[2];

    out[0] = (byte)(code >>> 8);
    out[1] = (byte)(code & 0xFF);

    return out;
  }
}

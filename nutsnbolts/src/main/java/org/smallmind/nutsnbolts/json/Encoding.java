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
package org.smallmind.nutsnbolts.json;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import org.smallmind.nutsnbolts.http.Base64Codec;
import org.smallmind.nutsnbolts.security.HexCodec;

/**
 * Text encoding strategies for converting binary payloads to and from a string representation suitable for transport or storage.
 */
public enum Encoding {

  /**
   * Encodes bytes as a lowercase hexadecimal string.
   */
  HEX {
    /**
     * Encodes the supplied bytes as a hexadecimal string.
     *
     * @param bytes the bytes to encode
     * @return the hexadecimal string representation
     * @throws Exception if encoding fails
     */
    @Override
    public String encode (byte[] bytes)
      throws Exception {

      return HexCodec.hexEncode(bytes);
    }

    /**
     * Decodes a hexadecimal string back into bytes.
     *
     * @param encoded the hexadecimal string to decode
     * @return the decoded bytes
     * @throws UnsupportedEncodingException if decoding fails
     */
    @Override
    public byte[] decode (String encoded)
      throws UnsupportedEncodingException {

      return HexCodec.hexDecode(encoded);
    }
  },

  /**
   * Encodes bytes using Base64.
   */
  BASE_64 {
    /**
     * Encodes the supplied bytes using Base64.
     *
     * @param bytes the bytes to encode
     * @return the Base64 string representation
     * @throws IOException if encoding fails
     */
    @Override
    public String encode (byte[] bytes)
      throws IOException {

      return Base64Codec.encode(bytes);
    }

    /**
     * Decodes a Base64 string back into bytes.
     *
     * @param encoded the Base64 string to decode
     * @return the decoded bytes
     * @throws IOException if decoding fails
     */
    @Override
    public byte[] decode (String encoded)
      throws IOException {

      return Base64Codec.decode(encoded);
    }
  };

  /**
   * Converts raw bytes into a text string using this encoding strategy.
   *
   * @param bytes the bytes to encode
   * @return the encoded string
   * @throws Exception if the encoding operation fails
   */
  public abstract String encode (byte[] bytes)
    throws Exception;

  /**
   * Reconstructs raw bytes from their encoded string representation.
   *
   * @param encoded the encoded string to decode
   * @return the original bytes
   * @throws Exception if the decoding operation fails
   */
  public abstract byte[] decode (String encoded)
    throws Exception;
}

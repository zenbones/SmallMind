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
package org.smallmind.memcached.cubby.response;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.smallmind.memcached.cubby.IncomprehensibleRequestException;
import org.smallmind.memcached.cubby.IncomprehensibleResponseException;

/**
 * Stateless parser that converts a raw memcached meta-protocol response line into a {@link Response}.
 *
 * <p>The parser reads from a {@link JoinedBuffer} that spans both previously accumulated bytes and a
 * newly received read buffer, enabling correct handling of responses that straddle read boundaries.
 * All methods are static; no instances of this class need to be created.</p>
 *
 * <p>The supported response codes and their optional flag characters follow the memcached meta
 * protocol specification. An unrecognised response causes an {@link IncomprehensibleResponseException}
 * to be thrown, while a bare {@code ERROR} line causes an {@link IncomprehensibleRequestException}.</p>
 */
public class ResponseParser {

  /**
   * Parses a single response line from the joined buffer into a structured {@link Response}.
   *
   * <p>The buffer's mark is set at entry so that error-reporting helpers can rewind and capture
   * the offending bytes. After the two-character response code is identified the method delegates
   * to {@link #parseFlags} to consume any remaining space-separated flags on the same line.</p>
   *
   * @param joinedBuffer the buffer spanning accumulated and newly read bytes
   * @param offset       the absolute position within {@code joinedBuffer} where the response line begins
   * @param length       the number of bytes in the response line (excluding the trailing {@code \r\n})
   * @return a fully populated {@link Response} representing the parsed server reply
   * @throws IOException if the line is malformed, too short, or contains an unrecognised code
   */
  public static Response parse (JoinedBuffer joinedBuffer, int offset, int length)
    throws IOException {

    joinedBuffer.mark();

    if (length < 2) {
      throw createIncomprehensibleResponseException(joinedBuffer, length);
    } else if (isError(joinedBuffer, length)) {
      throw new IncomprehensibleRequestException();
    } else {

      Response response;
      byte first = joinedBuffer.get();
      byte second = joinedBuffer.get();

      if (ResponseCode.MN.begins(first, second)) {
        response = new Response(ResponseCode.MN);
      } else if (ResponseCode.HD.begins(first, second)) {
        response = new Response(ResponseCode.HD);
      } else if (ResponseCode.VA.begins(first, second)) {
        response = new Response(ResponseCode.VA);

        if ((length < 4) || joinedBuffer.get() != ' ') {
          throw createIncomprehensibleResponseException(joinedBuffer, length);
        } else {
          response.setValueLength(accumulateInt(joinedBuffer, offset, length));
        }
      } else if (ResponseCode.EN.begins(first, second)) {
        response = new Response(ResponseCode.EN);
      } else if (ResponseCode.EX.begins(first, second)) {
        response = new Response(ResponseCode.EX);
      } else if (ResponseCode.NF.begins(first, second)) {
        response = new Response(ResponseCode.NF);
      } else if (ResponseCode.NS.begins(first, second)) {
        response = new Response(ResponseCode.NS);
      } else {
        throw createIncomprehensibleResponseException(joinedBuffer, length);
      }

      parseFlags(response, joinedBuffer, offset, length);

      return response;
    }
  }

  /**
   * Determines whether the current response line is the literal text {@code ERROR}.
   *
   * <p>The buffer position is reset to the mark before returning so that subsequent
   * reads begin from the same starting point regardless of the outcome.</p>
   *
   * @param joinedBuffer the buffer positioned at the beginning of the response line
   * @param length       the byte length of the response line
   * @return {@code true} if the line consists exactly of the five bytes {@code ERROR}
   */
  private static boolean isError (JoinedBuffer joinedBuffer, int length) {

    try {
      return (length == 5)
               && (joinedBuffer.get() == 'E')
               && (joinedBuffer.get() == 'R')
               && (joinedBuffer.get() == 'R')
               && (joinedBuffer.get() == 'O')
               && (joinedBuffer.get() == 'R');
    } finally {
      joinedBuffer.reset();
    }
  }

  /**
   * Parses the space-separated flag tokens that follow the response code and populates the response.
   *
   * <p>Recognised flag characters:</p>
   * <ul>
   *   <li>{@code O} &ndash; opaque client token (string)</li>
   *   <li>{@code c} &ndash; CAS token (long)</li>
   *   <li>{@code s} &ndash; stored object size (int)</li>
   *   <li>{@code W} &ndash; won flag (boolean)</li>
   *   <li>{@code Z} &ndash; also-won flag (boolean)</li>
   *   <li>{@code X} &ndash; stale flag (boolean)</li>
   * </ul>
   *
   * @param response     the response to be updated with flag values
   * @param joinedBuffer the buffer positioned immediately after the response code (and value length for VA)
   * @param offset       the absolute start position of the response line in {@code joinedBuffer}
   * @param length       the total byte length of the response line
   * @throws IOException if an unexpected byte is encountered where a flag character is expected
   */
  private static void parseFlags (Response response, JoinedBuffer joinedBuffer, int offset, int length)
    throws IOException {

    while ((joinedBuffer.position() - offset) < length) {
      if (joinedBuffer.get() != ' ') {
        throw createIncomprehensibleResponseException(joinedBuffer, length);
      } else if ((joinedBuffer.position() - offset) < length) {
        switch (joinedBuffer.get()) {
          case 'O':
            response.setToken(accumulateToken(joinedBuffer, offset, length));
            break;
          case 'c':
            response.setCas(accumulateLong(joinedBuffer, offset, length));
            break;
          case 's':
            response.setSize(accumulateInt(joinedBuffer, offset, length));
            break;
          case 'W':
            response.setWon(true);
            break;
          case 'Z':
            response.setAlsoWon(true);
            break;
          case 'X':
            response.setStale(true);
            break;
          default:
            throw createIncomprehensibleResponseException(joinedBuffer, length);
        }
      }
    }
  }

  /**
   * Reads the next whitespace-delimited token from the buffer and parses it as an {@code int}.
   *
   * @param joinedBuffer the buffer positioned at the start of the numeric token
   * @param offset       the absolute start of the response line
   * @param length       the total byte length of the response line
   * @return the integer value of the token
   * @throws IOException if the token is not a valid integer, or the line format is invalid
   */
  private static int accumulateInt (JoinedBuffer joinedBuffer, int offset, int length)
    throws IOException {

    try {
      return Integer.parseInt(accumulateToken(joinedBuffer, offset, length));
    } catch (NumberFormatException numberFormatException) {
      throw createIncomprehensibleResponseException(joinedBuffer, length);
    }
  }

  /**
   * Reads the next whitespace-delimited token from the buffer and parses it as a {@code long}.
   *
   * @param joinedBuffer the buffer positioned at the start of the numeric token
   * @param offset       the absolute start of the response line
   * @param length       the total byte length of the response line
   * @return the long value of the token
   * @throws IOException if the token is not a valid long, or the line format is invalid
   */
  private static long accumulateLong (JoinedBuffer joinedBuffer, int offset, int length)
    throws IOException {

    try {
      return Long.parseLong(accumulateToken(joinedBuffer, offset, length));
    } catch (NumberFormatException numberFormatException) {
      throw createIncomprehensibleResponseException(joinedBuffer, length);
    }
  }

  /**
   * Extracts the next whitespace-delimited or end-of-line token from the buffer as a UTF-8 string.
   *
   * <p>The method scans forward from the current position to find the next space character or the
   * end of the response line, then bulk-reads those bytes into a new string without consuming
   * the terminating space.</p>
   *
   * @param joinedBuffer the buffer positioned at the first character of the token
   * @param offset       the absolute start of the response line
   * @param length       the total byte length of the response line
   * @return the token as a UTF-8 string
   */
  private static String accumulateToken (JoinedBuffer joinedBuffer, int offset, int length) {

    int index = 0;

    while ((joinedBuffer.position() + index) < (offset + length)) {

      if (joinedBuffer.peek(index) == ' ') {
        return new String(joinedBuffer.get(new byte[index]), StandardCharsets.UTF_8);
      }

      index++;
    }

    return new String(joinedBuffer.get(new byte[index]), StandardCharsets.UTF_8);
  }

  /**
   * Constructs an {@link IncomprehensibleResponseException} that includes the offending response text.
   *
   * <p>The buffer is reset to its mark and {@code length} bytes are read to capture the raw line
   * for inclusion in the exception message.</p>
   *
   * @param joinedBuffer the buffer whose mark points to the start of the response line
   * @param length       the byte length of the response line
   * @return an exception populated with the raw response text
   */
  private static IncomprehensibleResponseException createIncomprehensibleResponseException (JoinedBuffer joinedBuffer, int length) {

    byte[] slice = new byte[length];

    joinedBuffer.reset();
    joinedBuffer.get(slice);

    return new IncomprehensibleResponseException(new String(slice, StandardCharsets.UTF_8));
  }
}
